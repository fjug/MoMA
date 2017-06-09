/**
 * 
 */
package com.jug.ops.numerictype;


import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.jug.ops.rai.RaiMeanSubtractor;
import com.jug.ops.rai.RaiSquare;

import net.imagej.ImageJ;
import net.imagej.ops.AbstractOp;
import net.imagej.ops.Op;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;

/**
 * @author jug
 *
 */
@Plugin(type = Op.class, name = "var of rai")
public class VarOfRai<T extends NumericType<T> & NativeType<T> > extends AbstractOp {
	
	@Parameter
	private RandomAccessibleInterval<T> input;

	@Parameter(type = ItemIO.OUTPUT)
	private T output;

	@Override
	public void run() {
		
	final ImageJ ij = new ImageJ();
		
	output.setZero();
	
	// Var(X) = < < X - <X> >^2 >
	RandomAccessibleInterval<T> tmp;
	tmp = (RandomAccessibleInterval<T>) ij.op().run(new RaiMeanSubtractor<T>(), input);
	tmp = (RandomAccessibleInterval<T>) ij.op().run(new RaiSquare<T>(), tmp); 
	output = (T) ij.op().run(new MeanOfRai<T>(), tmp);
    }

    public T createEmptyOutput(RandomAccessibleInterval<T> in) {
	return in.randomAccess().get().createVariable();
    }
}
