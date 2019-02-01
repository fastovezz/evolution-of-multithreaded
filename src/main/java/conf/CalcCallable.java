package conf;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class CalcCallable implements Callable<Double> {
    private final double start;
    private final double end;
    private final double step;
    private final Function<Double, Double> func;

    @Override
    public Double call() throws Exception {
        double partialResult = 0.0;
        double x = start;
        while (x < end) {
            partialResult += step * func.apply(x);
            x += step;
        }
        return partialResult;
    }

    public CalcCallable(double start, double end, double step, Function<Double, Double> func) {
        this.start = start;
        this.end = end;
        this.step = step;
        this.func = func;
    }
}
