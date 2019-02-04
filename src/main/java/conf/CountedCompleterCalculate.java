package conf;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountedCompleter;
import java.util.function.Function;

public class CountedCompleterCalculate extends CountedCompleter<Void> {
    private final double start;
    private final double end;
    private final double step;
    private final Function<Double, Double> func;
    static final long SEQUENTIAL_THRESHOLD = 500;
    static final Map<Integer, Double> RESULT = new ConcurrentHashMap<>();

    @Override
    public void compute() {
        if ((end - start) / step < SEQUENTIAL_THRESHOLD) {
            RESULT.put(this.hashCode(), sequentialCompute());
        } else {
            double mid = start + (end - start) / 2.0;
            setPendingCount(2);
            CountedCompleterCalculate left = new CountedCompleterCalculate(this, start, mid, step, func);
            CountedCompleterCalculate right = new CountedCompleterCalculate(this, mid, end, step, func);
            left.fork();
            right.fork();
        }
        tryComplete();
    }

    private Double sequentialCompute() {
        SequentialCalculate sequentialCalculate = new SequentialCalculate(func);
        return sequentialCalculate.calculate(start, end, step);
    }

    public CountedCompleterCalculate(CountedCompleter<?> parent, double start, double end, double step, Function<Double, Double> func) {
        super(parent);
        this.start = start;
        this.end = end;
        this.step = step;
        this.func = func;
    }
}
