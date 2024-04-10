package nu.marginalia.array.algo;

import nu.marginalia.array.LongArray;


/** Functions for operating on pairs of arrays.
 */
public class TwoArrayOperations {

    /**
     * Merge two sorted arrays into a third array, removing duplicates.
     */
    public static long mergeArrays(LongArray out, LongArray a, LongArray b, long outStart, long aStart, long aEnd, long bStart, long bEnd) {
        long aPos = aStart;
        long bPos = bStart;
        long outPos = outStart;

        long lastValue = 0;

        while (aPos < aEnd && bPos < bEnd) {
            final long aVal = a.get(aPos);
            final long bVal = b.get(bPos);
            final long setVal;

            if (aVal < bVal) {
                setVal = aVal;
                aPos++;
            } else if (bVal < aVal) {
                setVal = bVal;
                bPos++;
            } else {
                setVal = aVal;
                aPos++;
                bPos++;
            }

            if (outPos == outStart || setVal != lastValue) {
                out.set(outPos++, setVal);
            }

            lastValue = setVal;
        }

        while (aPos < aEnd) {
            long val = a.get(aPos++);

            if (val != lastValue || outPos == outStart) {
                out.set(outPos++, val);
            }

            lastValue = val;
        }

        while (bPos < bEnd) {
            long val = b.get(bPos++);

            if (val != lastValue || outPos == outStart) {
                out.set(outPos++, val);
            }

            lastValue = val;
        }

        return outPos - outStart;
    }

    /**
     * Merge two sorted arrays into a third array, removing duplicates.
     * <p>
     * The operation is performed with a step size of 2. For each pair of values,
     * only the first is considered to signify a key. The second value is retained along
     * with the first.  In the case of a duplicate, the value associated with array 'a'
     * is retained, the other is discarded.
     *
     */
    public static long mergeArrays2(LongArray out, LongArray a, LongArray b,
                                    long outStart,
                                    long aStart, long aEnd,
                                    long bStart, long bEnd)
    {
        long aPos = aStart;
        long bPos = bStart;
        long outPos = outStart;

        long lastValue = 0;

        while (aPos < aEnd && bPos < bEnd) {
            final long aVal = a.get(aPos);
            final long bVal = b.get(bPos);

            final long setVal;
            final long setArg;

            if (aVal < bVal) {
                setVal = aVal;
                setArg = a.get(aPos + 1);

                aPos+=2;
            } else if (bVal < aVal) {
                setVal = bVal;
                setArg = b.get(bPos + 1);

                bPos+=2;
            } else {
                setVal = aVal;
                setArg = a.get(aPos + 1);

                aPos+=2;
                bPos+=2;
            }

            if (setVal != lastValue || outPos == outStart) {
                out.set(outPos++, setVal);
                out.set(outPos++, setArg);

                lastValue = setVal;
            }
        }

        while (aPos < aEnd) {
            long val = a.get(aPos++);
            long arg = a.get(aPos++);

            if (val != lastValue || outPos == outStart) {
                out.set(outPos++, val);
                out.set(outPos++, arg);
                lastValue = val;
            }
        }

        while (bPos < bEnd) {
            long val = b.get(bPos++);
            long arg = b.get(bPos++);

            if (val != lastValue || outPos == outStart) {
                out.set(outPos++, val);
                out.set(outPos++, arg);

                lastValue = val;
            }
        }

        return outPos - outStart;
    }

    /**
     * Count the number of distinct elements in two sorted arrays.
     */
    public static long countDistinctElements(LongArray a, LongArray b, long aStart, long aEnd, long bStart, long bEnd) {
        var directRangeA = a.directRangeIfPossible(aStart, aEnd);
        var directRangeB = b.directRangeIfPossible(bStart, bEnd);

        a = directRangeA.array();
        aStart = directRangeA.start();
        aEnd = directRangeA.end();

        b = directRangeB.array();
        bStart = directRangeB.start();
        bEnd = directRangeB.end();

        return countDistinctElementsDirect(a, b, aStart, aEnd, bStart, bEnd);
    }

    private static long countDistinctElementsDirect(LongArray a, LongArray b, long aStart, long aEnd, long bStart, long bEnd) {
        long aPos = aStart;
        long bPos = bStart;

        long distinct = 0;
        long lastValue = 0;

        while (aPos < aEnd && bPos < bEnd) {
            final long aVal = a.get(aPos);
            final long bVal = b.get(bPos);
            final long setVal;

            if (aVal < bVal) {
                setVal = aVal;
                aPos++;
            } else if (bVal < aVal) {
                setVal = bVal;
                bPos++;
            } else {
                setVal = aVal;
                aPos++;
                bPos++;
            }

            if (distinct == 0 || (setVal != lastValue)) {
                distinct++;
            }

            lastValue = setVal;
        }

        while (aPos < aEnd) {
            long val = a.get(aPos++);

            if (distinct == 0 || (val != lastValue)) {
                distinct++;
            }
            lastValue = val;
        }

        while (bPos < bEnd) {
            long val = b.get(bPos++);

            if (distinct == 0 || (val != lastValue)) {
                distinct++;
            }
            lastValue = val;
        }

        return distinct;
    }

}
