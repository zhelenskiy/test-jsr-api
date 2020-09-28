import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class SafeExecutor {
    public <T> T execute(@NotNull Supplier<T> function) {
        return function.get();
    }
}
