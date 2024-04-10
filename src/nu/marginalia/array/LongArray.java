package nu.marginalia.array;

import nu.marginalia.array.algo.LongArrayBase;
import nu.marginalia.array.algo.LongArrayTransformations;
import nu.marginalia.array.page.SegmentLongArray;

import java.lang.foreign.Arena;


public interface LongArray extends LongArrayBase, LongArrayTransformations, AutoCloseable {
    int WORD_SIZE = 8;

    @Deprecated
    static LongArray allocate(long size) {
        return SegmentLongArray.onHeap(Arena.ofShared(), size);
    }

    /** Translate the range into the equivalent range in the underlying array if they are in the same page */
    ArrayRangeReference<LongArray> directRangeIfPossible(long start, long end);

    void force();
    void close();
}
