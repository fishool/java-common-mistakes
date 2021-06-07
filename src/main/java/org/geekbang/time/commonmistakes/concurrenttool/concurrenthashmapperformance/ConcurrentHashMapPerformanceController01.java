package org.geekbang.time.commonmistakes.concurrenttool.concurrenthashmapperformance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
@RequestMapping("concurrenthashmapperformance")
@Slf4j
public class ConcurrentHashMapPerformanceController01 {

    private static int LOOP_COUNT = 10000000;
    private static int THREAD_COUNT = 10;
    private static int ITEM_COUNT = 10;
    
    /**
     * 我们可以举一个形象的例子。ConcurrentHashMap 就像是一个大篮子，
     * 现在这个篮子里有 900 个桔子，我们期望把这个篮子装满 1000 个桔子，也就是再装 100
     * 个桔子。有 10 个工人来干这件事儿，大家先后到岗后会计算还需要补多少个桔子进去，最
     * 后把桔子装入篮子。
     *
     *ConcurrentHashMap 这个篮子本身，可以确保多个工人在装东西进去时，不会相互影响
     *干扰，但无法确保工人 A 看到还需要装 100 个桔子但是还未装的时候，工人 B 就看不到篮
     *子中的桔子数量。更值得注意的是，你往这个篮子装 100 个桔子的操作不是原子性的，在
     *别人看来可能会有一个瞬间篮子里有 964 个桔子，还需要补 36 个桔子。
     *
     * 1. 使用了 ConcurrentHashMap，不代表对它的多个操作之间的状态是一致的，是没有其
     * 他线程在操作它的，如果需要确保需要手动加锁。
     * 2. 诸如 size、isEmpty 和 containsValue 等聚合方法，在并发情况下可能会反映
     * ConcurrentHashMap 的中间状态。因此在并发情况下，这些方法的返回值只能用作参
     * 考，而不能用于流程控制。显然，利用 size 方法计算差异值，是一个流程控制。
     * 3. 诸如 putAll 这样的聚合方法也不能确保原子性，在 putAll 的过程中去获取数据可能会
     * 获取到部分数据。
     *
     **/

    @GetMapping("good")
    public String good() throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("normaluse");
        Map<String, Long> normaluse = normaluse();
        stopWatch.stop();
        Assert.isTrue(normaluse.size() == ITEM_COUNT, "normaluse size error");
        Assert.isTrue(normaluse.entrySet().stream()
                        .mapToLong(item -> item.getValue()).reduce(0, Long::sum) == LOOP_COUNT
                , "normaluse count error");
        stopWatch.start("gooduse");
        Map<String, Long> gooduse = gooduse();
        stopWatch.stop();
        Assert.isTrue(gooduse.size() == ITEM_COUNT, "gooduse size error");
        Assert.isTrue(gooduse.entrySet().stream()
                        .mapToLong(item -> item.getValue())
                        .reduce(0, Long::sum) == LOOP_COUNT
                , "gooduse count error");
        log.info(stopWatch.prettyPrint());
        return "OK";
    }

    private Map<String, Long> normaluse() throws InterruptedException {
        ConcurrentHashMap<String, Long> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, LOOP_COUNT).parallel().forEach(i -> {
                    String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
                    synchronized (freqs) {
                        if (freqs.containsKey(key)) {
                            freqs.put(key, freqs.get(key) + 1);
                        } else {
                            freqs.put(key, 1L);
                        }
                    }
                }
        ));
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        return freqs;
    }

    private Map<String, Long> gooduse() throws InterruptedException {
        ConcurrentHashMap<String, LongAdder> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);
        // parallelStream默认使用了共享的ForkJoinPool，其默认线程数是CPU核心数-1。
        // 或者显示初始化 forkJoinPool , 指定线程数
        forkJoinPool.execute(() -> IntStream.rangeClosed(1, LOOP_COUNT).parallel().forEach(i -> {
                    String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
                    freqs.computeIfAbsent(key, k -> new LongAdder()).increment();
                }
        ));
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        return freqs.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue().longValue())
                );
    }
}
