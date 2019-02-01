package conf;


import java.util.function.Function;

public class CalcThread extends Thread {
    private final double start;
    private final double end;
    private final double step;
    private double partialResult;
    private final Function<Double, Double> func;

    @Override
    public void run() {
        double x = start;
        while (x < end) {
            partialResult += step * func.apply(x);
            x += step;
        }
    }

    public double getPartialResult() {
        return partialResult;
    }

    public CalcThread(double start, double end, double step, Function<Double, Double> func) {
        this.start = start;
        this.end = end;
        this.step = step;
        this.func = func;
    }
}

