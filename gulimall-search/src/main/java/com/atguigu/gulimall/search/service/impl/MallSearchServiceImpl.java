package com.atguigu.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.config.GulimallElasticSearchConfig;
import com.atguigu.gulimall.search.constant.EsConstant;
import com.atguigu.gulimall.search.service.MallSearchService;
import com.atguigu.gulimall.search.vo.SearchParamVo;
import com.atguigu.gulimall.search.vo.SearchResponseVo;
import org.apache.lucene.search.join.ScoreMode;
import org.bouncycastle.util.encoders.UTF8;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public SearchResponseVo search(SearchParamVo searchParamVo) {
        SearchRequest searchRequest = buildSearchRequest(searchParamVo);
        SearchResponseVo searchResponseVo = null;

        try {
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
            searchResponseVo = buildSearchResponse(searchResponse, searchParamVo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return searchResponseVo;
    }

    /**
     * ??????????????????
     *
     * @param searchResponse
     * @return
     */
    private SearchResponseVo buildSearchResponse(SearchResponse searchResponse, SearchParamVo searchParamVo) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        SearchHits hits = searchResponse.getHits();
        //??????????????????
        List<SkuEsModel> products = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if (!StringUtils.isEmpty(searchParamVo.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    skuEsModel.setSkuTitle(skuTitle.getFragments()[0].string());
                }
                products.add(skuEsModel);
            }
            searchResponseVo.setProducts(products);
        }

        //???????????????
        searchResponseVo.setPageNum(searchParamVo.getPageNum());
        //??????????????????
        searchResponseVo.setTotal(hits.getTotalHits().value);
        //???????????????
        int totalPages = (int) hits.getTotalHits().value % EsConstant.PRODUCT_PAGESIZE == 0 ? (int) hits.getTotalHits().value / EsConstant.PRODUCT_PAGESIZE : ((int) hits.getTotalHits().value / EsConstant.PRODUCT_PAGESIZE + 1);
        searchResponseVo.setTotalPages(totalPages);

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i < totalPages; i++) {
            pageNavs.add(i);
        }
        searchResponseVo.setPageNavs(pageNavs);

        //??????????????????
        ParsedLongTerms brand_agg = searchResponse.getAggregations().get("brand_agg");
        List<SearchResponseVo.BrandVo> brandVos = new ArrayList<>();
        List<? extends Terms.Bucket> brandAggBuckets = brand_agg.getBuckets();
        for (Terms.Bucket bucket : brandAggBuckets) {
            SearchResponseVo.BrandVo brandVo = new SearchResponseVo.BrandVo();
            String keyAsString = bucket.getKeyAsString();
            brandVo.setBrandId(Long.parseLong(keyAsString));

            ParsedStringTerms brand_name_agg = bucket.getAggregations().get("brand_name_agg");
            String brand_name = brand_name_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brand_name);

            ParsedStringTerms brand_img_agg = bucket.getAggregations().get("brand_img_agg");
            String brand_img = brand_img_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brand_img);

            brandVos.add(brandVo);
        }
        searchResponseVo.setBrands(brandVos);

        //??????????????????
        ParsedLongTerms catalog_agg = searchResponse.getAggregations().get("catalog_agg");
        List<? extends Terms.Bucket> catalogAggBuckets = catalog_agg.getBuckets();
        List<SearchResponseVo.CatalogVo> catalogVos = new ArrayList<>();
        for (Terms.Bucket bucket : catalogAggBuckets) {
            SearchResponseVo.CatalogVo catalogVo = new SearchResponseVo.CatalogVo();
            catalogVo.setCatalogId(Long.parseLong(bucket.getKeyAsString()));

            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            catalogVo.setCatalogName(catalog_name_agg.getBuckets().get(0).getKeyAsString());

            catalogVos.add(catalogVo);
        }
        searchResponseVo.setCatalogs(catalogVos);

        //??????????????????
        ParsedNested attr_agg = searchResponse.getAggregations().get("attr_agg");
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        List<? extends Terms.Bucket> attrAggBuckets = attr_id_agg.getBuckets();
        List<SearchResponseVo.AttrVo> attrVos = new ArrayList<>();

        for (Terms.Bucket bucket : attrAggBuckets) {
            SearchResponseVo.AttrVo attrVo = new SearchResponseVo.AttrVo();
            attrVo.setAttrId(Long.parseLong(bucket.getKeyAsString()));

            ParsedStringTerms attr_value_agg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValueList = new ArrayList<>();
            List<? extends Terms.Bucket> attrValueAggBuckets = attr_value_agg.getBuckets();
            for (Terms.Bucket attrValueAggBucket : attrValueAggBuckets) {
                attrValueList.add(attrValueAggBucket.getKeyAsString());
            }
            attrVo.setAttrValue(attrValueList);

            ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
            attrVo.setAttrName(attr_name_agg.getBuckets().get(0).getKeyAsString());


            attrVos.add(attrVo);
        }
        searchResponseVo.setAttrs(attrVos);


        /**
         * ?????????
         */
        //???????????????
        if (searchParamVo.getAttrs() != null && searchParamVo.getAttrs().size() > 0) {
            List<SearchResponseVo.NavVo> navVos = searchParamVo.getAttrs().stream().map(attr -> {
                SearchResponseVo.NavVo navVo = new SearchResponseVo.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                for (SearchResponseVo.AttrVo searchResponseVoAttr : searchResponseVo.getAttrs()) {
                    if (Long.parseLong(s[0]) == searchResponseVoAttr.getAttrId()) {
                        navVo.setNavName(searchResponseVoAttr.getAttrName());
                    }
                }
                String encode = null;
                try {
                    encode = URLEncoder.encode(attr, "UTF-8");
                    encode = encode.replace("+", "%20");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                String replace = searchParamVo.getQueryString().replace("&attrs=" + encode, "");
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                return navVo;
            }).collect(Collectors.toList());

            searchResponseVo.setNavs(navVos);
        }

        //???????????????
        if (searchParamVo.getBrandId() != null && searchParamVo.getBrandId().size() > 0) {
            List<SearchResponseVo.NavVo> navVos = searchParamVo.getBrandId().stream().map(brandId -> {
                SearchResponseVo.NavVo navVo = new SearchResponseVo.NavVo();
                navVo.setNavName("??????");

                for (SearchResponseVo.BrandVo brand : searchResponseVo.getBrands()) {
                    if (brand.getBrandId().equals(brandId)) {
                        navVo.setNavValue(brand.getBrandName());
                    }
                }

                String replace = searchParamVo.getQueryString().replace("&brandId=" + brandId, "");
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                return navVo;
            }).collect(Collectors.toList());

            searchResponseVo.setNavs(navVos);
        }


        System.out.println(searchResponseVo.toString());

        return searchResponseVo;
    }

    /**
     * ??????????????????
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParamVo searchParamVo) {
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        /**
         * ?????????????????????????????????????????????????????????????????????????????????
         */
        //1?????????bool-query
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //1.1???????????????
        if (!StringUtils.isEmpty(searchParamVo.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", searchParamVo.getKeyword()));
        }
        //1.2??????????????????id???
        if (searchParamVo.getCatalog3Id() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId", searchParamVo.getCatalog3Id()));
        }
        //1.3?????????????????????
        if (searchParamVo.getBrandId() != null && searchParamVo.getBrandId().size() > 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", searchParamVo.getBrandId()));
        }
        //1.4?????????????????????
        if (searchParamVo.getHasStock() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("hasStock", searchParamVo.getHasStock() == 1));
        }
        //1.5???????????????????????????
        //1_500/_500/500_
        if (!StringUtils.isEmpty(searchParamVo.getSkuPrice())) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuPrice");
            String[] s = searchParamVo.getSkuPrice().split("_");
            if (s.length == 2) {
                if (!StringUtils.isEmpty(s[0]) && !StringUtils.isEmpty(s[1])) {
                    rangeQueryBuilder.gte(Long.parseLong(s[0]));
                    rangeQueryBuilder.lte(Long.parseLong(s[1]));
                } else {
                    if (searchParamVo.getSkuPrice().startsWith("_")) {
                        rangeQueryBuilder.lte(Long.parseLong(s[1]));
                    }
                }
            } else {
                if (searchParamVo.getSkuPrice().endsWith("_")) {
                    rangeQueryBuilder.gte(Long.parseLong(s[0]));
                }
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }
        //1.6?????????????????????
        if (!StringUtils.isEmpty(searchParamVo.getAttrs())) {
            //attrs=1_5???:8???&attrs=2_16G:8G
            List<String> attrs = searchParamVo.getAttrs();
            for (String attr : attrs) {
                String[] s = attr.split("_");
                String attrId = s[0];
                String[] attrValues = s[1].split(":");
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                boolQueryBuilder.filter(QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None));
            }
        }

        searchSourceBuilder.query(boolQueryBuilder);
        /**
         * ????????????????????????
         */
        //2?????????
        if (!StringUtils.isEmpty(searchParamVo.getSort())) {
            //sort=hotScore_desc/asc
            String[] s = searchParamVo.getSort().split("_");

            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            searchSourceBuilder.sort(s[0], order);
        }

        //3?????????
        searchSourceBuilder.from((searchParamVo.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        //4?????????
        if (!StringUtils.isEmpty(searchParamVo.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        /**
         * ????????????
         */
        //????????????
        //1??????????????????id???????????????????????????
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        //2??????????????????????????????id????????????????????????????????????
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        searchSourceBuilder.aggregation(brand_agg);

        //????????????
        //1??????????????????id???????????????????????????
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        //2??????????????????????????????id?????????????????????
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        searchSourceBuilder.aggregation(catalog_agg);

        //????????????
        //1??????????????????id???????????????????????????
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(50);
        //2??????????????????????????????id?????????????????????????????????
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);

        searchSourceBuilder.aggregation(attr_agg);

        System.out.println(searchSourceBuilder.toString());

        return new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);
    }
}
