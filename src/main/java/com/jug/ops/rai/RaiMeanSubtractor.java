/**
 * 
 */
package com.jug.ops.rai;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

import com.jug.ops.numerictype.MeanOfRai;
import com.jug.util.DataMover;

/**
 * @author jug
 *
 */
public class RaiMeanSubtractor<T extends NumericType<T> & NativeType<T> > implements
	UnaryOutputOperation< RandomAccessibleInterval<T>, RandomAccessibleInterval<T> > {

    /**
     * @see net.imglib2.ops.operation.UnaryOperation#compute(java.lang.Object, java.lang.Object)
     */
    @Override
    public RandomAccessibleInterval<T> compute(
	    RandomAccessibleInterval<T> input,
	    RandomAccessibleInterval<T> output) {
	T mean = new MeanOfRai<T>().compute(input);
	DataMover.copy(input, output);
	
	for (T pixel : Views.iterable(output)) {
	    pixel.sub(mean);
	}
	return output;
    }

    /**
     * @see net.imglib2.ops.operation.UnaryOutputOperation#createEmptyOutput(java.lang.Object)
     */
    @Override
    public RandomAccessibleInterval<T> createEmptyOutput(
	    RandomAccessibleInterval<T> in) {
	return DataMover.createEmptyArrayImgLike(in, in.randomAccess().get()); 
    }

    /**
     * @see net.imglib2.ops.operation.UnaryOutputOperation#compute(java.lang.Object)
     */
    @Override
    public RandomAccessibleInterval<T> compute(RandomAccessibleInterval<T> in) {
	return compute( in, createEmptyOutput(in) );
    }

    /**
     * @see net.imglib2.ops.operation.UnaryOutputOperation#copy()
     */
    @Override
    public UnaryOutputOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> copy() {
	return new RaiMeanSubtractor<T>();
    }


}
