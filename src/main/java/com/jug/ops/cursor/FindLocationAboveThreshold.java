/**
 * 
 */
package com.jug.ops.cursor;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.AbstractOp;
import net.imagej.ops.Op;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;
import net.imglib2.view.Views;

/**
 * @author jug
 *
 */
@Plugin(type = Op.class, name = "find location above threshold")
public class FindLocationAboveThreshold<IMG_T extends Type< IMG_T > & Comparable< IMG_T >>  extends AbstractOp {
		
	@Parameter
	private RandomAccessibleInterval<IMG_T> input;

	@Parameter(type = ItemIO.OUTPUT)
	private Cursor<IMG_T> output;

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
     */
    public Cursor<IMG_T> createEmptyOutput(RandomAccessibleInterval<IMG_T> in) {
	return Views.iterable(in).cursor().copyCursor();
    }

    /**
     * Iterates input and returns the position of the first occurrence of an pixel value
     * larger the compare value given at instantiation time.
     */
    @Override
	public void run() {
	while (output.hasNext()) {
	    IMG_T el = output.next();

	    if (el.compareTo(cmpVal) > 0) {
		break;
	    }
	}
    }

}
