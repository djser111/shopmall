package com.atguigu.gulimall.search.thread;

import java.util.concurrent.*;

public class ThreadTest {
    public static Executor executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        /**
         * 创建线程的四种方式：
         * 1、继承Thread类，重写run方法
         *      new Thread01().start();
         * 2、实现Runnable接口，重写run方法
         *      new Thread(new Runnable01()).start();
         * 3、实现Callable接口，重写call方法
         *      FutureTask<Long> futureTask = new FutureTask<>(new Callable01());
         *      new Thread(futureTask).start();
         * 4、线程池
         *      创建线程池的方法：
         *      （1）使用Executor工具类直接创建
         *      （2）new ThreadPoolExecutor();
         *      七大参数：
         *      1）int corePoolSize：核心线程数，不管线程是否空闲，永远不会释放
         *      2）int maximumPoolSize：线程池允许的最大线程数，超过核心线程数的线程空闲时，根据设置的存活时间释放线程
         *      3）long keepAliveTime：存活时间，超过核心线程数的线程最大空闲时间，超过将会被释放
         *      4）TimeUnit unit：时间单位
         *      5）BlockingQueue<Runnable> workQueue：阻塞队列，线程池中除了核心线程直接分配任务外，其他任务存放在队列中，只要有线程空闲，就会去队列中去除任务继续执行
         *      6）ThreadFactory threadFactory：线程的创建工厂
         *      7）RejectedExecutionHandler handler：拒绝策略，如果线程池满了，将会按照我们指定的拒绝策略拒绝执行任务
         *
         * 工作流程：
         * 1，线程池创建，准备好corePoolSize数量的核心线程，准备接受任务
         * 2，corePoolSize满了，多余的任务进入阻塞队列，空闲的核心线程自己去阻塞队列获取任务
         * 3，阻塞队列满了就开始创建新线程，最大线程数不能超过maximumPoolSize
         * 4，maximumPoolSize满了，就采用RejectedExecutionHandler handler拒绝策略，拒绝接受任务
         * 5，maximumPoolSize执行完了，所有空闲的非核心线程在keepAliveTime时间之后，释放这些线程
         *
         *      new LinkedBlockingDeque<>()默认是Integer的最大值，内存不够
         *
         * 面试：
         * 一个线程池core：7，max：20，queue：50，100个线程同时访问
         * 7个任务直接执行，剩余93个任务
         * 50个任务进入队列，剩余43个任务
         * 新开13个线程去队列中领取任务，队列中剩余37个任务
         * 43个任务在进入队列13个，此时最大线程与最大队列存储数量已满，采用拒绝策略，抛弃剩下的30个任务
         *
         * 区别：
         * 1、2不能得到返回值，3可以有返回值
         * 1、2、3消耗资源不可控
         * 4可以控制资源
         */
    }

    public static class Thread01 extends Thread {
        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
        }
    }

    public static class Runnable01 implements Runnable {

        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getId());
        }
    }

    public static class Callable01 implements Callable {

        @Override
        public Long call() throws Exception {
            System.out.println("当前线程：" + Thread.currentThread().getId());
            return Thread.currentThread().getId();
        }
    }
}
