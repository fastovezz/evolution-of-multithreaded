package conf;

import java.util.function.Function;

public class SequentialCalculate {
    private Function<Double, Double> func;

    public SequentialCalculate(Function<Double, Double> func) {
        this.func = func;
    }

    public double calculate(double start, double end, double step) {
        double result = 0.0;
        double x = start;
        while (x < end) {
            result += step * func.apply(x);
            x += step;
        }
        return result;
    }
}


