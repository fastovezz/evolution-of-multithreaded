package conf;


import java.util.concurrent.RecursiveTask;
import java.util.function.Function;

public class RecursiveTaskCalculate extends RecursiveTask<Double> {
    private final double start;
    private final double end;
    private final double step;
    private final Function<Double, Double> func;
    static final long SEQUENTIAL_THRESHOLD = 500;

    @Override
    protected Double compute() {
        if ((end - start) / step < SEQUENTIAL_THRESHOLD) {
            return sequentialCompute();
        }
        double mid = start + (end - start) / 2.0;
        RecursiveTaskCalculate left = new RecursiveTaskCalculate(start, mid, step, func);
        RecursiveTaskCalculate right = new RecursiveTaskCalculate(mid, end, step, func);
        left.fork();
        right.fork();
        double rightRes = right.join();
        double leftRes = left.join();
        return leftRes + rightRes;
    }

    private Double sequentialCompute() {
        SequentialCalculate sequentialCalculate = new SequentialCalculate(func);
        return sequentialCalculate.calculate(start, end, step);
    }

    public RecursiveTaskCalculate(double start, double end, double step, Function<Double, Double> func) {
        this.start = start;
        this.end = end;
        this.step = step;
        this.func = func;
    }
}
