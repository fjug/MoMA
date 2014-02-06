/**
 *
 */
package com.jug.lp;

import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.util.List;

import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * @author jug
 */
public class DivisionAssignment extends AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > {

	@SuppressWarnings( "unused" )
	private final AssignmentsAndHypotheses< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > >, Hypothesis< Component< DoubleType, ? > > > nodes;
	@SuppressWarnings( "unused" )
	private final HypothesisNeighborhoods< Hypothesis< Component< DoubleType, ? > >, AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > edges;
	private final Hypothesis< Component< DoubleType, ? >> from;
	private final Hypothesis< Component< DoubleType, ? >> toUpper;
	private final Hypothesis< Component< DoubleType, ? >> toLower;

	/**
	 * Creates an DivisionAssignment.
	 *
	 * @param nodes
	 * @param edges
	 * @param from
	 * @param to1
	 * @param to2
	 * @throws GRBException
	 */
	public DivisionAssignment( final int t, final GRBVar ilpVariable, final GRBModel model,
 final AssignmentsAndHypotheses< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > >, Hypothesis< Component< DoubleType, ? > > > nodes, final HypothesisNeighborhoods< Hypothesis< Component< DoubleType, ? > >, AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > edges, final Hypothesis< Component< DoubleType, ? >> from, final Hypothesis< Component< DoubleType, ? >> toUpper, final Hypothesis< Component< DoubleType, ? >> toLower ) throws GRBException {
		super( GrowthLineTrackingILP.ASSIGNMENT_DIVISION, ilpVariable, model );
		this.from = from;
		this.toUpper = toUpper;
		this.toLower = toLower;
		this.edges = edges;
		this.nodes = nodes;
	}

	/**
	 * This method is void. DIVISION assignments do not come with assignment
	 * specific constrains...
	 *
	 * @throws GRBException
	 * @see com.jug.lp.AbstractAssignment#addConstraintsToLP(gurobi.GRBModel,
	 *      com.jug.lp.AssignmentsAndHypotheses,
	 *      com.jug.lp.HypothesisNeighborhoods)
	 */
	@Override
	public void addConstraintsToLP() throws GRBException {
	}

	/**
	 * Division assignments do not come with constraints.
	 *
	 * @see com.jug.lp.AbstractAssignment#getConstraint()
	 */
	@Override
	public void addFunctionsAndFactors( final FactorGraphFileBuilder fgFile, final List< Integer > regionIds ) {
	}

	/**
	 * Returns the segmentation hypothesis this division-assignment comes from
	 * (the one at the earlier time-point t).
	 *
	 * @return the associated segmentation-hypothesis.
	 */
	public Hypothesis< Component< DoubleType, ? >> getSourceHypothesis() {
		return from;
	}

	/**
	 * Returns the upper of the two segmentation hypothesis this
	 * division-assignment links to (the upper of the two at the later
	 * time-point t+1).
	 *
	 * @return the associated segmentation-hypothesis.
	 */
	public Hypothesis< Component< DoubleType, ? >> getUpperDesinationHypothesis() {
		return toUpper;
	}

	/**
	 * Returns the upper of the two segmentation hypothesis this
	 * division-assignment links to (the upper of the two at the later
	 * time-point t+1).
	 *
	 * @return the associated segmentation-hypothesis.
	 */
	public Hypothesis< Component< DoubleType, ? >> getLowerDesinationHypothesis() {
		return toLower;
	}
}
