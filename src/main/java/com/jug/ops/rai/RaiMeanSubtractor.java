/**
 * 
 */
package com.jug.ops.rai;

import com.jug.ops.numerictype.MeanOfRai;
import com.jug.util.DataMover;

import net.imagej.ops.Op;
import net.imagej.ops.special.hybrid.AbstractUnaryHybridCF;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

import org.scijava.plugin.Plugin;

/**
 * @author jug
 *
 */
@Plugin(type = Op.class)
public class RaiMeanSubtractor<T extends NumericType<T> & NativeType<T> > 
extends AbstractUnaryHybridCF<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> {
	
	@Override
	public void compute(RandomAccessibleInterval<T> input, RandomAccessibleInterval<T> output) {
		T mean = (T) ops().run(MeanOfRai.class, input);
		DataMover.copy(input, output);
		
		for (T pixel : Views.iterable(output)) {
		    pixel.sub(mean);
		}
	}

	@Override
	public RandomAccessibleInterval<T> createOutput(RandomAccessibleInterval<T> input) {
		return DataMover.createEmptyArrayImgLike(input, input.randomAccess().get());
	}

}
