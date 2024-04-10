package nu.marginalia.array.algo;

import nu.marginalia.array.functional.*;

import java.io.IOException;

public interface LongArrayTransformations extends LongArrayBase {

    default long fold(long zero, long start, long end, LongBinaryOperation operator) {
        long accumulator = zero;

        for (long i = start; i < end; i++) {
            accumulator = operator.apply(accumulator, get(i));
        }

        return accumulator;
    }

}
