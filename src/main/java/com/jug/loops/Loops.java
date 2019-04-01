/**
 * 
 */
package com.jug.loops;

import java.util.ArrayList;
import java.util.List;

import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * @author jug
 *
 */
public class Loops<IMG_T extends Type< IMG_T >, INNER_RET_T> {

    /** Hyperslices given <code>RandomAccessibleInterval</code> and hands individual slices to a 
     *  given UnaryOperation.
     * @param rai - <code>RandomAccessibleInterval</code> to be hypersliced.
     * @param d - dimension along which the hyperslices will be created.
     * @param opClass - the class of the Op to be called for each hyperslice.
     *                Results will be added to List and finally returned.
     * @param opService - the OpService instance to be used to call the Op
     * @return a List of all the individual return values.
     */
    public List<INNER_RET_T> forEachHyperslice(
        RandomAccessibleInterval<FloatType> rai, int d,
        Class<? extends Op> opClass, final OpService opService) {

        ArrayList<INNER_RET_T> ret = new ArrayList<>((int)rai.dimension(d));

        for (long i=0; i<rai.dimension(d); i++) {
            INNER_RET_T r = (INNER_RET_T) opService.run(opClass, Views.hyperSlice(rai, d, i));
            ret.add(r);
        }

        return ret;
    }
    
    /** Slices given <code>RandomAccessibleInterval</code> along given dimension and hands those to a 
     *  given UnaryOperation. The difference to <code>forEachHyperslice</code> is, 
     *  that the image dimensions of the given <code>RandomAccessibleInterval</code> 
     *  are preserved and pointers (like e.g. Cursors) created on the slices can be 
     *  used to locate the same places in the original <code>RandomAccessibleInterval</code>.
     * @param rai - <code>RandomAccessibleInterval</code> to be hypersliced.
     * @param d - dimension along which the hyperslices will be created.
     * @param opClass - the class of the Op to be called for each hyperslice.
     *                Results will be added to List and finally returned.
     * @param opService - the OpService instance to be used to call the Op
     * @return a List of all the individual return values.
     */
    public List<INNER_RET_T> forEachIntervalSlice(
        RandomAccessibleInterval<IMG_T> rai, int d,
        Class<? extends Op> opClass, final OpService opService) {

        ArrayList<INNER_RET_T> ret = new ArrayList<>((int)rai.dimension(d));

        final int n = rai.numDimensions();
        long[] min = new long[n];
        rai.min( min );
        long[] max = new long[n];
        rai.max( max );
        for (long i=0; i<rai.dimension(d); i++) {
            min[ d ] = i;
            max[ d ] = i;
            INNER_RET_T r = (INNER_RET_T) opService.run(opClass, Views.interval(rai, min, max));
            ret.add(r);
        }

        return ret;
    }
}
