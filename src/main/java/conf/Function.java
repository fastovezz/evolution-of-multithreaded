package conf;

@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);
}


