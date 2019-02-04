package conf;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.lang.Math.cos;
import static java.lang.Math.sin;


public final class ConfSandBox {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        double start = -10000000;
        double end = 10000000;
        double step = 5;
        Function<Double, Double> func = x -> sin(x) * sin(x) + cos(x) * cos(x);
        int chunks = (int) ((end - start) / step + 1.0) / 500;
        System.out.println(String.format("chunks: %d", chunks));

        long startNano = System.nanoTime();
        double result = calculateSequentially(func, start, end, step);
        System.out.println(String.format("Sequentially: %,.2f; %,d", result, System.nanoTime() - startNano));

        startNano = System.nanoTime();
        result = calculateThreads(func, start, end, step, chunks);
        System.out.println(String.format("Threads: %,.2f; %,d", result, System.nanoTime() - startNano));

        startNano = System.nanoTime();
        result = calculateExecutor(func, start, end, step, chunks);
        System.out.println(String.format("ServiceExecutor: %,.2f; %,d", result, System.nanoTime() - startNano));

        startNano = System.nanoTime();
        result = calculateForkJoin(new RecursiveTaskCalculate(start, end, step, func));
        System.out.println(String.format("ForkJoinPool with RecursiveTask: %,.2f; %,d", result, System.nanoTime() - startNano));

        startNano = System.nanoTime();
        result = calculateForkJoin(new CountedCompleterCalculate(null, start, end, step, func));
        System.out.println(String.format("ForkJoinPool with CountedCompleter: %,.2f; %,d", result, System.nanoTime() - startNano));

        startNano = System.nanoTime();
        result = calculateCompletableFuture(func, start, end, step, chunks);
        System.out.println(String.format("CompletableFuture: %,.2f; %,d", result, System.nanoTime() - startNano));

        startNano = System.nanoTime();
        result = calculateStreams(func, start, end, step, chunks);
        System.out.println(String.format("Streams: %,.2f; %,d", result, System.nanoTime() - startNano));

    }

    private static double calculateSequentially(Function<Double, Double> func, double start, double end, double step) {
        SequentialCalculate sc = new SequentialCalculate(func);
        return sc.calculate(start, end, step);
    }

    public static double calculateThreads(Function<Double, Double> func, double start, double end, double step, int chunks)
            throws InterruptedException {
        int parallelism = getParallelism();
        if (chunks > parallelism) {
            chunks = parallelism;
        }
        CalcThread[] calcThreads = new CalcThread[chunks];
        double interval = (end - start) / chunks;
        double st = start;
        for (int i = 0; i < chunks; i++) {
            calcThreads[i] = new CalcThread(st, st + interval, step, func);
            calcThreads[i].start();
            st += interval;
        }
        double result = 0.0;
        for (CalcThread calcThread : calcThreads) {
            calcThread.join();
            result += calcThread.getPartialResult();
        }
        return result;
    }

    public static double calculateExecutor(Function<Double, Double> func, double start, double end, double step, int chunks)
            throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(getParallelism());
        Future<Double>[] futures = new Future[chunks];
        double interval = (end - start) / chunks;
        double st = start;
        for (int i = 0; i < chunks; i++) {
            futures[i] = executorService.submit(new CalcCallable(st, st + interval, step, func));
            st += interval;
        }
        executorService.shutdown();

        double result = 0.0;
        for (Future<Double> partRes : futures) {
            result += partRes.get();
        }
        return result;
    }

    public static <T> double calculateForkJoin(ForkJoinTask<T> task) {
//        ForkJoinPool pool = new ForkJoinPool(getParallelism());
        ForkJoinPool pool = ForkJoinPool.commonPool();
        T res = pool.invoke(task);
        double sum = 0.0;
        if (CountedCompleterCalculate.class.isAssignableFrom(task.getClass())) {
            for (double value : CountedCompleterCalculate.RESULT.values()) {
                sum += value;
            }
        } else if (RecursiveTaskCalculate.class.isAssignableFrom(task.getClass())) {
            sum = (Double) res;
        }
        return sum;
    }

    public static double calculateCompletableFuture(Function<Double, Double> func,
                                                    double start, double end, double step, int chunks)
            throws ExecutionException, InterruptedException {
//        ForkJoinPool pool = new ForkJoinPool(getParallelism());
        ForkJoinPool pool = ForkJoinPool.commonPool();
        double interval = (end - start) / chunks;
        CompletableFuture<Double>[] allFutures = new CompletableFuture[chunks];
        double st = start;
        for (int i = 0; i < chunks; i++) {
            final double st_ = st;
            allFutures[i] = CompletableFuture.supplyAsync(() ->
                    calculateSequentially(func, st_, st_ + interval, step), pool);
            st += interval;
        }

        CompletableFuture<List<Double>> allDoneFuture =
                CompletableFuture.allOf(allFutures)
                        .thenApply(v -> Stream.of(allFutures)
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList()));

        double sum = 0.0;
        for (double partialResult : allDoneFuture.get()) {
            sum += partialResult;
        }
        return sum;
    }

    public static double calculateStreams(Function<Double, Double> func, double start, double end, double step, int chunks) {
        double interval = (end - start) / chunks;
        double sum = LongStream.range(0, chunks)
                .parallel()
                .mapToDouble(i -> start + interval * i)
                .map(st -> calculateSequentially(func, st, st + interval, step))
                .sum();
        return sum;
    }

    private static int getParallelism() {
        int parallelism = Runtime.getRuntime().availableProcessors() - 1;
        if (parallelism <= 1) {
            parallelism = 2;
        }
        return parallelism;
    }
}

