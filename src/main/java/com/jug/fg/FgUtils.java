/**
 *
 */
package com.jug.fg;

import net.imglib2.type.numeric.real.FloatType;

import com.indago.fg.domain.BooleanDomain;
import com.indago.fg.factor.Factor;
import com.indago.fg.function.BooleanTensorTable;
import com.indago.fg.variable.ComponentVariable;
import com.jug.util.filteredcomponents.FilteredComponent;


/**
 * @author jug
 */
public class FgUtils {

	/**
	 * @param componentVariable
	 * @return
	 */
	public static BooleanTensorTable getUnaryFactor(
			final ComponentVariable< FilteredComponent< FloatType >> componentVariable ) {
		for ( final Factor< BooleanDomain, ?, ? > factor : componentVariable.getFactors() ) {
			if (factor.getFunction() instanceof BooleanTensorTable) {
				return ( BooleanTensorTable ) factor.getFunction();
			}
		}
		return null;
	}

}
