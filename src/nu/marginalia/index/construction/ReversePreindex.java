package nu.marginalia.index.construction;

import nu.marginalia.array.LongArray;
import nu.marginalia.array.LongArrayFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static nu.marginalia.array.algo.TwoArrayOperations.*;

/** Contains the data that would go into a reverse index,
 * that is, a mapping from words to documents, minus the actual
 * index structure that makes the data quick to access while
 * searching.
 * <p>
 * Two preindexes can be merged into a third preindex containing
 * the union of their data.  This operation requires no additional
 * RAM.
 */
public class ReversePreindex {
    final ReversePreindexWordSegments segments;
    final ReversePreindexDocuments documents;


    public ReversePreindex(ReversePreindexWordSegments segments, ReversePreindexDocuments documents) {
        this.segments = segments;
        this.documents = documents;
    }


    /** Delete all files associated with this pre-index */
    public void delete() throws IOException {
        segments.delete();
        documents.delete();
    }

    public static ReversePreindex merge(Path destDir,
                                        ReversePreindex left,
                                        ReversePreindex right) throws IOException {

        ReversePreindexWordSegments mergingSegment =
                createMergedSegmentWordFile(destDir, left.segments, right.segments);

        var mergingIter = mergingSegment.constructionIterator(2);
        var leftIter = left.segments.iterator(2);
        var rightIter = right.segments.iterator(2);

        Path docsFile = Files.createTempFile(destDir, "docs", ".dat");

        LongArray mergedDocuments = LongArrayFactory.mmapForWritingConfined(
                docsFile,
                left.documents.size() + right.documents.size()
        );

        leftIter.next();
        rightIter.next();

        try (FileChannel leftChannel = left.documents.createDocumentsFileChannel();
             FileChannel rightChannel = right.documents.createDocumentsFileChannel())
        {

            while (mergingIter.canPutMore()
                    && leftIter.isPositionBeforeEnd()
                    && rightIter.isPositionBeforeEnd())
            {
                final long currentWord = mergingIter.wordId();

                if (leftIter.wordId == currentWord && rightIter.wordId == currentWord)
                {
                    // both inputs have documents for the current word
                    mergeSegments(leftIter, rightIter,
                            left.documents, right.documents,
                            mergedDocuments, mergingIter);
                }
                else if (leftIter.wordId == currentWord) {
                    if (!copySegment(leftIter, mergedDocuments, leftChannel, mergingIter))
                        break;
                }
                else if (rightIter.wordId == currentWord) {
                    if (!copySegment(rightIter, mergedDocuments, rightChannel, mergingIter))
                        break;
                }
                else assert false : "This should never happen"; // the helvetica scenario
            }

            if (leftIter.isPositionBeforeEnd()) {
                while (copySegment(leftIter, mergedDocuments, leftChannel, mergingIter));
            }

            if (rightIter.isPositionBeforeEnd()) {
                while (copySegment(rightIter, mergedDocuments, rightChannel, mergingIter));
            }

        }

        if (leftIter.isPositionBeforeEnd())
            throw new IllegalStateException("Left has more to go");
        if (rightIter.isPositionBeforeEnd())
            throw new IllegalStateException("Right has more to go");
        if (mergingIter.canPutMore())
            throw new IllegalStateException("Source iters ran dry before merging iter");


        mergingSegment.force();

        // We may have overestimated the size of the merged docs size in the case there were
        // duplicates in the data, so we need to shrink it to the actual size we wrote.

        long end = mergingIter.startOffset();
        long sizeLongs = 2 * mergingSegment.totalSize();
        if (end != sizeLongs) {
            throw new IllegalStateException();
        }
        mergedDocuments = shrinkMergedDocuments(mergedDocuments,
                docsFile, sizeLongs);

        return new ReversePreindex(
                mergingSegment,
                new ReversePreindexDocuments(mergedDocuments, docsFile)
        );
    }

    /** Create a segment word file with each word from both inputs, with zero counts for all the data.
     * This is an intermediate product in merging.
     */
    static ReversePreindexWordSegments createMergedSegmentWordFile(Path destDir,
                                                                   ReversePreindexWordSegments left,
                                                                   ReversePreindexWordSegments right) throws IOException {
        Path segmentWordsFile = Files.createTempFile(destDir, "segment_words", ".dat");
        Path segmentCountsFile = Files.createTempFile(destDir, "segment_counts", ".dat");

        // We need total size to request a direct LongArray range.  Seems slower, but is faster.
        // ... see LongArray.directRangeIfPossible(long start, long end)
        long segmentsSize = countDistinctElements(left.wordIds, right.wordIds,
                0,  left.wordIds.size(),
                0,  right.wordIds.size());

        LongArray wordIdsFile = LongArrayFactory.mmapForWritingConfined(segmentWordsFile, segmentsSize);

        mergeArrays(wordIdsFile, left.wordIds, right.wordIds,
                0,
                0, left.wordIds.size(),
                0, right.wordIds.size());

        LongArray counts = LongArrayFactory.mmapForWritingConfined(segmentCountsFile, segmentsSize);

        return new ReversePreindexWordSegments(wordIdsFile, counts, segmentWordsFile, segmentCountsFile);
    }

    /** It's possible we overestimated the necessary size of the documents file,
     * this will permit us to shrink it down to the smallest necessary size.
     */
    private static LongArray shrinkMergedDocuments(LongArray mergedDocuments, Path docsFile, long sizeLongs) throws IOException {

        mergedDocuments.force();

        long beforeSize = mergedDocuments.size() * 8;
        long afterSize = sizeLongs * 8;

        if (beforeSize > afterSize) {
            mergedDocuments.close();
            try (var bc = Files.newByteChannel(docsFile, StandardOpenOption.WRITE)) {
                bc.truncate(afterSize);
            }

            mergedDocuments = LongArrayFactory.mmapForWritingConfined(docsFile, sizeLongs);
        }

        return mergedDocuments;
    }

    /** Merge contents of the segments indicated by leftIter and rightIter into the destionation
     * segment, and advance the construction iterator with the appropriate size.
     */
    private static void mergeSegments(ReversePreindexWordSegments.SegmentIterator leftIter,
                                      ReversePreindexWordSegments.SegmentIterator rightIter,
                                      ReversePreindexDocuments left,
                                      ReversePreindexDocuments right,
                                      LongArray dest,
                                      ReversePreindexWordSegments.SegmentConstructionIterator destIter)
    {
        long segSize = mergeArrays2(dest,
                left.documents,
                right.documents,
                destIter.startOffset(),
                leftIter.startOffset, leftIter.endOffset,
                rightIter.startOffset, rightIter.endOffset);

        long distinct = segSize / 2;
        destIter.putNext(distinct);
        leftIter.next();
        rightIter.next();
    }

    /** Copy the data from the source segment at the position and length indicated by sourceIter,
     * into the destination segment, and advance the construction iterator.
     */
    private static boolean copySegment(ReversePreindexWordSegments.SegmentIterator sourceIter,
                                    LongArray dest,
                                    FileChannel sourceChannel,
                                    ReversePreindexWordSegments.SegmentConstructionIterator mergingIter) throws IOException {

        long size = sourceIter.endOffset - sourceIter.startOffset;
        long start = mergingIter.startOffset();
        long end = start + size;

        dest.transferFrom(sourceChannel,
                sourceIter.startOffset,
                mergingIter.startOffset(),
                end);

        boolean putNext = mergingIter.putNext(size / 2);
        boolean iterNext = sourceIter.next();

        if (!putNext && iterNext)
            throw new IllegalStateException("Source iterator ran out before dest iterator?!");

        return iterNext;
    }


}
