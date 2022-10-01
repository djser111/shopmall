package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import com.atguigu.gulimall.product.vo.Catalog3Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        List<CategoryEntity> entities = baseMapper.selectList(null);
        List<CategoryEntity> level1Menu = new ArrayList<>();
        for (CategoryEntity entity : entities) {
            if (entity.getParentCid() == 0) {
                level1Menu.add(entity);
            }
            entity.setChildren(getChildren(entity, entities));
        }
        level1Menu.sort(new Comparator<CategoryEntity>() {
            @Override
            public int compare(CategoryEntity o1, CategoryEntity o2) {
                return o1.getSort() - o2.getSort();
            }
        });
        return level1Menu;
    }

    private List<CategoryEntity> getChildren(CategoryEntity menu, List<CategoryEntity> categoryEntities) {
        List<CategoryEntity> result = new ArrayList<>();
        for (CategoryEntity categoryEntity : categoryEntities) {
            if (categoryEntity.getParentCid().equals(menu.getCatId())) {
                result.add(categoryEntity);
            }
        }
        return result;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 检查当前删除的菜单，是否被其他地方引用
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatalogPath(Long catalogId) {
        List<Long> path = new ArrayList<>();
        List<Long> parentPath = findParentPath(catalogId, path);
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    @Override
    @Cacheable(value = "category", key = "#root.methodName", sync = true)
    public List<CategoryEntity> getLevel1Category() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("cat_level", 1));
    }

    @Override
    @Cacheable(value = "category", key = "#root.methodName", sync = true)
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        //1、一次查出所有数据
        return getDataFromDb();
    }

    //TODO 产生堆外内存溢出 OutOfDirectMemoryError
    public Map<String, List<Catalog2Vo>> getCatalogJson2() {
        /**
         * 1、空结果缓存，解决缓存穿透
         * 2、设置过期时间，解决缓存雪崩
         * 3、加锁，解决缓存击穿
         */
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            //redis中没有数据，查询数据库，放入redis在返回
            return getCatalogJsonDBWithRedissonLock();

        }
        //redis中有数据，将json字符串转为对象后返回
        return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {
        });

    }

    public Map<String, List<Catalog2Vo>> getCatalogJsonDBWithRedissonLock() {
        RLock lock = redissonClient.getLock("catalogJson-lock");
        lock.lock();
        Map<String, List<Catalog2Vo>> dataFromDb = null;

        try {
            //加锁成功...执行业务
            dataFromDb = getDataFromDb2();
        } finally {
            lock.unlock();
        }
        return dataFromDb;
    }

    public Map<String, List<Catalog2Vo>> getCatalogJsonDBWithRedisLock() {
        //1、占分布式锁，去redis占坑
        String uuid = UUID.randomUUID().toString();
        //设置过期时间必须和加锁同时进行，避免在设置过期时间时服务器断电而没有设置过期时间，这样的话redis中的lock会一直存在，没有抢到锁的线程会一直循环，造成死锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lock)) {
            Map<String, List<Catalog2Vo>> dataFromDb = null;
            try {
                //加锁成功...执行业务
                dataFromDb = getDataFromDb2();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                //执行完业务后需要删除redis中lock
                /**
                 * 1、在程序执行期间，设置的lock过期，其他线程设置新的lock进入，程序执行完后删除lock，造成误删
                 * String lock2 = redisTemplate.opsForValue().get("lock");
                 * if (uuid.equals(lock2)) {
                 *    redisTemplate.delete("lock");
                 * }
                 * 2、在uuid与lock校验完成之后，数据回传时，lock失效，程序判断uuid和lock2相同后，删除key，
                 * 但此时redis中lock已经改变，此时删除，其他线程进入，锁失效
                 * 3、需要保证在redis中完成判断并删除锁，可以使用lua脚本进行判断删除
                 */
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                Integer lock1 = redisTemplate.execute(new DefaultRedisScript<Integer>(script), Collections.singletonList("lock"), uuid);
            }
            return dataFromDb;
        } else {
            //没有抢到锁的线程重新执行这个方法抢锁
            try {
                //等待200毫秒后再次抢锁
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return getCatalogJsonDBWithRedisLock();
        }
    }

    private Map<String, List<Catalog2Vo>> getDataFromDb() {

        //1、一次查出所有数据
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        //2、查出所有一级分类
        List<CategoryEntity> level1Category = getParentCid(categoryEntities, 0L);

        return level1Category.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), obj -> {
            //3、每一个一级分类查出他的二级分类
            List<CategoryEntity> level2Category = getParentCid(categoryEntities, obj.getCatId());

            return level2Category.stream().map(catalog2 -> {

                Catalog2Vo catalog2Vo = new Catalog2Vo();
                catalog2Vo.setCatalog1Id(catalog2.getParentCid());
                catalog2Vo.setId(catalog2.getCatId());
                catalog2Vo.setName(catalog2.getName());

                List<CategoryEntity> level3Category = getParentCid(categoryEntities, catalog2.getCatId());

                List<Catalog3Vo> catalog3VoList = level3Category.stream().map(catalog3 -> {
                    Catalog3Vo catalog3Vo = new Catalog3Vo();
                    catalog3Vo.setCatalog2Id(catalog3.getParentCid());
                    catalog3Vo.setName(catalog3.getName());
                    catalog3Vo.setId(catalog3.getCatId());
                    return catalog3Vo;
                }).collect(Collectors.toList());

                catalog2Vo.setCatalog3List(catalog3VoList);

                return catalog2Vo;
            }).collect(Collectors.toList());
        }));

    }

    private Map<String, List<Catalog2Vo>> getDataFromDb2() {
        //得到锁以后，需要再去缓存中查一遍，确认没有才查数据库
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (!StringUtils.isEmpty(catalogJson)) {
            return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {
            });
        }

        Map<String, List<Catalog2Vo>> dataFromDb = getDataFromDb();
        //释放锁之前，应先将数据保存到redis中
        String s = JSON.toJSONString(dataFromDb);
        redisTemplate.opsForValue().set("catalogJson", s, 1, TimeUnit.DAYS);

        return dataFromDb;
    }

    public Map<String, List<Catalog2Vo>> getCatalogJsonDBWithLocalLock() {

        //只要是同一把锁，就能锁住需要这个锁的所有线程
        //1、synchronized(this)：springboot所有组件在容器中都是单例的
        //TODO 本地锁: synchronized,JUC(lock),在分布式情况下，想要锁住所有，必须使用分布式锁
        synchronized (this) {
            //得到锁以后，需要再去缓存中查一遍，确认没有才查数据库
            return getDataFromDb2();
        }
    }

    public List<CategoryEntity> getParentCid(List<CategoryEntity> categoryEntities, Long parentCid) {
        return categoryEntities.stream().filter(item -> {
            return item.getParentCid().equals(parentCid);
        }).collect(Collectors.toList());
    }

    private List<Long> findParentPath(Long catalogId, List<Long> path) {
        path.add(catalogId);
        CategoryEntity categoryEntity = baseMapper.selectById(catalogId);
        if (categoryEntity.getParentCid() != 0) {
            findParentPath(categoryEntity.getParentCid(), path);
        }
        return path;
    }
}