package nu.marginalia.index.construction;

import nu.marginalia.array.LongArray;
import nu.marginalia.array.LongArrayFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** A pair of file-backed arrays of sorted wordIds
 * and the count of documents associated with each wordId.
 */
public class ReversePreindexWordSegments {
    public final LongArray wordIds;
    public final LongArray counts;

    final Path wordsFile;
    final Path countsFile;

    public ReversePreindexWordSegments(LongArray wordIds,
                                       LongArray counts,
                                       Path wordsFile,
                                       Path countsFile)
    {
        assert wordIds.size() == counts.size();

        this.wordIds = wordIds;
        this.counts = counts;
        this.wordsFile = wordsFile;
        this.countsFile = countsFile;
    }

    public ReversePreindexWordSegments(Path wordsFile,
                                       Path countsFile) throws IOException {
        this.wordIds = LongArrayFactory.mmapForReadingConfined(wordsFile);
        this.counts = LongArrayFactory.mmapForReadingConfined(countsFile);

        assert wordIds.size() == counts.size();

        this.wordsFile = wordsFile;
        this.countsFile = countsFile;
    }

    public SegmentIterator iterator(int recordSize) {
        return new SegmentIterator(recordSize);
    }

    SegmentConstructionIterator constructionIterator(int recordSize) {
        return new SegmentConstructionIterator(recordSize);
    }

    public long totalSize() {
        return counts.fold(0, 0, counts.size(), Long::sum);
    }

    public void delete() throws IOException {
        counts.close();
        wordIds.close();

        Files.delete(countsFile);
        Files.delete(wordsFile);
    }

    public void force() {
        counts.force();
        wordIds.force();
    }

    public void close() {
        wordIds.close();
        counts.close();
    }

    public class SegmentIterator {
        private final int recordSize;
        private final long fileSize;
        long wordId;
        long startOffset = 0;
        long endOffset = 0;

        private SegmentIterator(int recordSize) {
            this.recordSize = recordSize;
            this.fileSize = wordIds.size();
        }

        private long i = -1;
        public long idx() {
            return i;
        }
        public boolean next() {
            if (++i >= fileSize) {
                wordId = Long.MIN_VALUE;
                return false;
            }

            wordId = wordIds.get(i);
            startOffset = endOffset;
            endOffset = startOffset + recordSize * counts.get(i);

            return true;
        }

        public boolean isPositionBeforeEnd() {
            return i < wordIds.size();
        }

        public long size() {
            return endOffset - startOffset;
        }
    }

    class SegmentConstructionIterator {
        private final int recordSize;
        private final long fileSize;
        private long wordId;
        private long startOffset = 0;

        public long wordId() {
            return wordId;
        }

        public long startOffset() {
            return startOffset;
        }

        private SegmentConstructionIterator(int recordSize) {
            this.recordSize = recordSize;
            this.fileSize = wordIds.size();
            if (fileSize == 0) {
                throw new IllegalArgumentException("Cannot construct zero-length word segment file");
            }
            this.wordId = wordIds.get(0);
        }

        private long i = 0;
        public long idx() {
            return i;
        }

        public boolean putNext(long size) {

            if (i >= fileSize)
                return false;

            counts.set(i++, size);
            startOffset += recordSize * size;

            if (i == fileSize) {
                // We've reached the end of the iteration and there is no
                // "next" wordId to fetch
                wordId = Long.MIN_VALUE;
                return false;
            }
            else {
                wordId = wordIds.get(i);
                return true;
            }
        }

        public boolean canPutMore() {
            return i < wordIds.size();
        }
    }
}
