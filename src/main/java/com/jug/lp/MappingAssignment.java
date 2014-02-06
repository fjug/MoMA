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
public class MappingAssignment extends AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > {

	@SuppressWarnings( "unused" )
	private final AssignmentsAndHypotheses< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > >, Hypothesis< Component< DoubleType, ? > > > nodes;
	@SuppressWarnings( "unused" )
	private final HypothesisNeighborhoods< Hypothesis< Component< DoubleType, ? > >, AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > edges;
	private final Hypothesis< Component< DoubleType, ? >> from;
	private final Hypothesis< Component< DoubleType, ? >> to;

	/**
	 * Creates an MappingAssignment.
	 *
	 * @param nodes
	 * @param edges
	 * @param from
	 * @param to
	 * @throws GRBException
	 */
	public MappingAssignment( final int t, final GRBVar ilpVariable, final GRBModel model,
 final AssignmentsAndHypotheses< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > >, Hypothesis< Component< DoubleType, ? > > > nodes, final HypothesisNeighborhoods< Hypothesis< Component< DoubleType, ? > >, AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > edges, final Hypothesis< Component< DoubleType, ? >> from, final Hypothesis< Component< DoubleType, ? >> to ) throws GRBException {
		super( GrowthLineTrackingILP.ASSIGNMENT_MAPPING, ilpVariable, model );
		this.from = from;
		this.to = to;
		this.edges = edges;
		this.nodes = nodes;
	}

	/**
	 * This method is void. MAPPING assignments do not come with assignment
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
	 * Mapping assignments do not come with constraints.
	 *
	 * @see com.jug.lp.AbstractAssignment#getConstraint()
	 */
	@Override
	public void addFunctionsAndFactors( final FactorGraphFileBuilder fgFile, final List< Integer > regionIds ) {
	}

	/**
	 * Returns the segmentation hypothesis this mapping-assignment comes from
	 * (the one at the earlier time-point t).
	 *
	 * @return the associated segmentation-hypothesis.
	 */
	public Hypothesis< Component< DoubleType, ? >> getSourceHypothesis() {
		return from;
	}

	/**
	 * Returns the segmentation hypothesis this mapping-assignment links to
	 * (the one at the later time-point t+1).
	 *
	 * @return the associated segmentation-hypothesis.
	 */
	public Hypothesis< Component< DoubleType, ? >> getDestinationHypothesis() {
		return to;
	}

}
