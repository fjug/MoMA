/**
 * 
 */
package com.jug.ops.numerictype;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.AbstractOp;
import net.imagej.ops.Op;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.view.Views;

/**
 * @author jug
 *
 */
@Plugin(type = Op.class, name = "sum of rai")
public class SumOfRai<T extends NumericType< T >> extends AbstractOp {
	
	@Parameter
	private RandomAccessibleInterval<T> input;

	@Parameter(type = ItemIO.OUTPUT)
	private T output;

//    /**
//     * @see net.imglib2.ops.operation.UnaryOperation#compute(java.lang.Object, java.lang.Object)
//     */
//    @Override
    public T compute(RandomAccessibleInterval<T> input, T output) {
	output.setZero();
	for (T el : Views.iterable(input)) {
	    output.add(el);
	}
	return output;
    }


    public T createEmptyOutput(RandomAccessibleInterval<T> in) {
	return in.randomAccess().get().createVariable();
    }

    @Override
	public void run() {
	T ret = createEmptyOutput(input);
	ret = compute(input, ret);
	output = ret;
    }

}
