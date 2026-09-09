package johnsmith.enchantmentcore.api.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.function.Supplier;

/**
 * A proxy codec that defers the execution of a provided supplier until
 * the encoding or decoding phase. Prevents premature registry resolution.
 */
public final class LazyCodec<A> implements Codec<A> {
    private final Supplier<Codec<A>> delegate;
    private Codec<A> resolved;

    public static <A> Codec<A> of(Supplier<Codec<A>> delegate) {
        return new LazyCodec<>(delegate);
    }

    private LazyCodec(Supplier<Codec<A>> delegate) {
        this.delegate = delegate;
    }

    private Codec<A> getDelegate() {
        if (this.resolved == null) {
            this.resolved = this.delegate.get();
        }
        return this.resolved;
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        return getDelegate().decode(ops, input);
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        return getDelegate().encode(input, ops, prefix);
    }
}