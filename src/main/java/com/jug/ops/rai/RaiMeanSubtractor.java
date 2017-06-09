/**
 * 
 */
package com.jug.ops.rai;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import com.jug.ops.numerictype.MeanOfRai;
import com.jug.util.DataMover;

import net.imagej.ImageJ;
import net.imagej.ops.AbstractOp;
import net.imagej.ops.Op;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

/**
 * @author jug
 *
 */
@Plugin(type = Op.class, name = "rai mean subtractor")
public class RaiMeanSubtractor<T extends NumericType<T> & NativeType<T> > extends AbstractOp {
	
	@Parameter
	private RandomAccessibleInterval<T> input;

	@Parameter(type = ItemIO.OUTPUT)
	private RandomAccessibleInterval<T> output;

	@Override
    public void run() {
	final ImageJ ij = new ImageJ();
	T mean = (T) ij.op().run(new MeanOfRai<T>(), input);
	DataMover.copy(input, output);
	
	for (T pixel : Views.iterable(output)) {
	    pixel.sub(mean);
	}
    }

    public RandomAccessibleInterval<T> createEmptyOutput(
	    RandomAccessibleInterval<T> in) {
	return DataMover.createEmptyArrayImgLike(in, in.randomAccess().get()); 
    }

}
