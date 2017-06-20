/**
 * 
 */
package com.jug.ops.numerictype;

import com.jug.ops.rai.RaiMeanSubtractor;
import com.jug.ops.rai.RaiSquare;

import net.imagej.ops.Op;
import net.imagej.ops.special.hybrid.AbstractUnaryHybridCF;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

import org.scijava.plugin.Plugin;

/**
 * @author jug
 *
 */
@Plugin(type = Op.class)
public class VarOfRai<T extends NumericType<T> & NativeType<T> > 
extends AbstractUnaryHybridCF<RandomAccessibleInterval<T>, T> {

	@Override
	public void compute(RandomAccessibleInterval<T> input, T output) {
			
		output.setZero();
		
		// Var(X) = < < X - <X> >^2 >
		RandomAccessibleInterval<T> tmp;
		tmp = (RandomAccessibleInterval<T>) ops().run(RaiMeanSubtractor.class, input);
		tmp = (RandomAccessibleInterval<T>) ops().run(RaiSquare.class, tmp); 
		output = (T) ops().run(MeanOfRai.class, tmp);
		
	}

	@Override
	public T createOutput(RandomAccessibleInterval<T> input) {
		return input.randomAccess().get().createVariable();
	}
}
