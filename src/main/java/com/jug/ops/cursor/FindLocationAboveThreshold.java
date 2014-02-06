/**
 * 
 */
package com.jug.ops.cursor;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.ops.operation.UnaryOutputOperation;
import net.imglib2.type.Type;
import net.imglib2.view.Views;

/**
 * @author jug
 *
 */
public class FindLocationAboveThreshold<IMG_T extends Type< IMG_T > & Comparable< IMG_T >> implements 
	UnaryOutputOperation< RandomAccessibleInterval<IMG_T>, Cursor<IMG_T> > {

    private IMG_T cmpVal;
    
    public IMG_T getdCmpVal() {
        return cmpVal;
    }

    public void setdCmpVal(IMG_T cmpVal) {
        this.cmpVal = cmpVal;
    }

    /**
     * Constructor setting the compare value.
     * @param d - the value the input will be compared to
     */
    public FindLocationAboveThreshold(IMG_T compareValue) {
	cmpVal = compareValue;
    }

    /** Returns an instance of the return type of function 'compute'.
     * @see net.imglib2.ops.operation.UnaryOutputOperation#createEmptyOutput(java.lang.Object)
     */
    @Override
    public Cursor<IMG_T> createEmptyOutput(RandomAccessibleInterval<IMG_T> in) {
	return Views.iterable(in).cursor().copyCursor();
    }

    /**
     * Iterates input and returns the position of the first occurrence of an pixel value
     * larger the compare value given at instantiation time.
     * @see net.imglib2.ops.operation.UnaryOutputOperation#compute(java.lang.Object)
     */
    @Override
    public Cursor<IMG_T> compute(RandomAccessibleInterval<IMG_T> in) {
	return compute(in, createEmptyOutput(in));
    }

    /**
     * Iterates input and returns the position of the first occurrence of an pixel value
     * larger the compare value given at instantiation time.
     * @see net.imglib2.ops.operation.UnaryOutputOperation#compute(java.lang.Object, java.lang.Object)
     */
    @Override
    public Cursor<IMG_T> compute(RandomAccessibleInterval<IMG_T> input, Cursor<IMG_T> output) {
	while (output.hasNext()) {
	    IMG_T el = output.next();

	    if (el.compareTo(cmpVal) > 0) {
		break;
	    }
	}
	return output;
}

    /**
     * @see net.imglib2.ops.operation.UnaryOperation#copy()
     */
    @Override
    public UnaryOutputOperation<RandomAccessibleInterval<IMG_T>, Cursor<IMG_T>> copy() {
	return new FindLocationAboveThreshold<IMG_T>(cmpVal);
    }
}
