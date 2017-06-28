/**
 * 
 */
package com.jug.ops.cursor;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.AbstractOp;
import net.imagej.ops.Op;
import net.imagej.ops.special.hybrid.AbstractUnaryHybridCF;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.Type;
import net.imglib2.view.Views;

/**
 * @author jug
 *
 */
@Plugin(type = Op.class)
public class FindLocationAboveThreshold<IMG_T extends Type< IMG_T > & Comparable< IMG_T >>  
extends AbstractUnaryHybridCF<RandomAccessibleInterval<IMG_T>, Cursor<IMG_T>> {
		
    private IMG_T cmpVal;
    
    public IMG_T getdCmpVal() {
        return cmpVal;
    }

    public void setdCmpVal(IMG_T cmpVal) {
        this.cmpVal = cmpVal;
    }

    /**
     * Constructor setting the compare value.
     * @param compareValue - the value the input will be compared to
     */
    public FindLocationAboveThreshold(IMG_T compareValue) {
    	cmpVal = compareValue;
    }

    /**
     * Iterates input and returns the position of the first occurrence of an pixel value
     * larger the compare value given at instantiation time.
     */
	@Override
	public void compute(final RandomAccessibleInterval<IMG_T> input, final Cursor<IMG_T> output) {
		while (output.hasNext()) {
		    IMG_T el = output.next();

		    if (el.compareTo(cmpVal) > 0) {
			break;
		    }
		}		
	}

	@Override
	public Cursor<IMG_T> createOutput(RandomAccessibleInterval<IMG_T> input) {
		return Views.iterable(input).cursor().copyCursor();
	}

}
