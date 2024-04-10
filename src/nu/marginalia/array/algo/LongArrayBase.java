package nu.marginalia.array.algo;

import java.io.IOException;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;

public interface LongArrayBase extends BulkTransferArray<LongBuffer> {
    long get(long pos);

    void set(long pos, long value);

    long size();

    void transferFrom(FileChannel source, long sourceStart, long arrayStart, long arrayEnd) throws IOException;
}
