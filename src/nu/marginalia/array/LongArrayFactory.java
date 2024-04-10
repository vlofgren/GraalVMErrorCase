package nu.marginalia.array;

import nu.marginalia.array.page.SegmentLongArray;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.file.Files;
import java.nio.file.Path;

public class LongArrayFactory {
    public static LongArray mmapForReadingConfined(Path filename) throws IOException  {
        return SegmentLongArray.fromMmapReadOnly(Arena.ofConfined(), filename, 0, Files.size(filename) / LongArray.WORD_SIZE);
    }

    public static LongArray mmapForWritingConfined(Path filename, long size) throws IOException  {
        return SegmentLongArray.fromMmapReadWrite(Arena.ofConfined(), filename, 0, size);
    }
}
