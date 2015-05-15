/**
 *
 */
package com.jug.fg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.imglib2.type.numeric.real.FloatType;

import com.indago.fg.domain.BooleanDomain;
import com.indago.fg.factor.Factor;
import com.indago.fg.function.BooleanTensorTable;
import com.indago.fg.variable.ComponentVariable;
import com.jug.util.ComponentTreeUtils;
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

	/**
	 * Builds and returns the set Hup (given as a List). Hup is defines as the
	 * set up all Hypothesis in hyps that strictly above the Hypothesis hyp (in
	 * image space).
	 *
	 * @param seghyp
	 *            the reference segmentation hypothesis.
	 * @param roots
	 *            roots of component trees containing all segments relevant to
	 *            assemple Hup.
	 * @return Hup, a List of all segment hypothesis that lie above the
	 *         <code>seghyp</code>.
	 */
	public static List< FilteredComponent< FloatType >> getHup(
			final FilteredComponent< FloatType > seghyp,
			final Collection< FilteredComponent< FloatType >> roots ) {
		final List< FilteredComponent< FloatType >> Hup =
				new ArrayList< FilteredComponent< FloatType >>();

		for ( final FilteredComponent< FloatType > root : roots ) {
			final LinkedList< FilteredComponent< FloatType >> queue = new LinkedList<>();
			queue.add( root );

			while ( !queue.isEmpty() ) {
				final FilteredComponent< FloatType > node = queue.removeFirst();

				if ( ComponentTreeUtils.isAbove( node, seghyp ) ) {
					Hup.add( node );
				}

				for ( final FilteredComponent< FloatType > child : node.getChildren() ) {
					queue.push( child );
				}
			}
		}
		return Hup;
	}

}
