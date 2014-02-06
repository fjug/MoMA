/**
 *
 */
package com.jug.lp;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.imglib2.Pair;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.algorithm.componenttree.ComponentForest;
import net.imglib2.type.numeric.real.DoubleType;

import com.jug.GrowthLine;
import com.jug.GrowthLineFrame;
import com.jug.lp.costs.CostFactory;
import com.jug.util.ComponentTreeUtils;


/**
 * @author jug
 */
public class GrowthLineTrackingILP {

	// < H extends Hypothesis< Component< DoubleType, ? > >, A extends AbstractAssignment< H > >

	// -------------------------------------------------------------------------------------
	// statics
	// -------------------------------------------------------------------------------------
	public static boolean FAST_MODE = false;

	public static int OPTIMIZATION_NEVER_PERFORMED = 0;
	public static int OPTIMAL = 1;
	public static int INFEASIBLE = 2;
	public static int UNBOUNDED = 3;
	public static int SUBOPTIMAL = 4;
	public static int NUMERIC = 5;
	public static int LIMIT_REACHED = 6;

	public static int ASSIGNMENT_EXIT = 0;
	public static int ASSIGNMENT_MAPPING = 1;
	public static int ASSIGNMENT_DIVISION = 2;

	public static final double CUTOFF_COST = 1.0;

	public static GRBEnv env;

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private final GrowthLine gl;

	public GRBModel model;
	private int status;

	public final AssignmentsAndHypotheses<
 AbstractAssignment< Hypothesis< Component< DoubleType, ? > > >, Hypothesis< Component< DoubleType, ? > > > nodes =
				new AssignmentsAndHypotheses<
 AbstractAssignment< Hypothesis< Component< DoubleType, ? > > >, Hypothesis< Component< DoubleType, ? > > >();
	public final HypothesisNeighborhoods<
 Hypothesis< Component< DoubleType, ? > >, AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > edgeSets = new HypothesisNeighborhoods< Hypothesis< Component< DoubleType, ? > >, AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > >();

	private int pbcId = 0;


	// -------------------------------------------------------------------------------------
	// construction
	// -------------------------------------------------------------------------------------
	public GrowthLineTrackingILP(final GrowthLine gl) {
		this.gl = gl;

		// Setting static stuff (this IS ugly!)
		if ( env == null ) {
			try {
				env = new GRBEnv( "MotherMachineILPs.log" );
			}
			catch ( final GRBException e ) {
				System.out.println( "GrowthLineTrackingILP::env could not be initialized!" );
				e.printStackTrace();
			}
		}

		try {
			model = new GRBModel( env );
		}
		catch ( final GRBException e ) {
			System.out.println( "GrowthLineTrackingILP::model could not be initialized!" );
			e.printStackTrace();
		}

		buildILP();
	}

	// -------------------------------------------------------------------------------------
	// getters & setters
	// -------------------------------------------------------------------------------------
	/**
	 * @return the status. This status returns one of the following values:
	 *         OPTIMIZATION_NEVER_PERFORMED, OPTIMAL, INFEASABLE, UNBOUNDED,
	 *         SUBOPTIMAL, NUMERIC, or LIMIT_REACHED. Values 2-6 correspond
	 *         directly to the ones from gurobi, the last one is set when none
	 *         of the others was actually returned by gurobi.
	 *         OPTIMIZATION_NEVER_PERFORMED shows, that the optimizer was never
	 *         started on this ILP setup.
	 */
	public int getStatus() {
		return status;
	}

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	private void buildILP() {
		try {
			// add Hypothesis
			createSegmentationHypotheses();

			// add Assignments
			enumerateAndAddAssignments();

			// UPDATE GUROBI-MODEL
			// - - - - - - - - - -
			model.update();

			// Iterate over all assignments and ask them to add their
			// constraints to the model
			// - - - - - - - - - - - - - - - - - - - - - - - - - - - -
			int numHyp = 0;
			for ( final List< Hypothesis< Component< DoubleType, ? >>> innerList : nodes.getAllHypotheses() ) {
				for ( final Hypothesis< Component< DoubleType, ? >> hypothesis : innerList ) {
					numHyp++;
				}
			}
			int numAss = 0;
			for ( final List< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > innerList : nodes.getAllAssignments() ) {
				for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> assignment : innerList ) {
					assignment.addConstraintsToLP();
					numAss++;
				}
			}
			System.out.println( "    Hypothesis count: " + numHyp );
			System.out.println( "    Assignment count: " + numAss );

			// Add the remaining ILP constraints
			// (those would be (i) and (ii) of 'Default Solution')
			// - - - - - - - - - - - - - - - - - - - - - - - - - -
			addPathBlockingConstraint();
			addExplainationContinuityConstraints();

			// UPDATE GUROBI-MODEL
			// - - - - - - - - - -
			model.update();
//			System.out.println( "Constraints added: " + model.getConstrs().length );

		}
		catch ( final GRBException e ) {
			System.out.println( "Could not fill data into GrowthLineTrackingILP!" );
			e.printStackTrace();
		}

	}

	/**
	 * Writes the FactorGraph corresponding to the optimization problem of the
	 * given growth-line into a file.
	 *
	 * @throws IOException
	 */
	public void exportFG( final File file ) {
		// Here I collect all the lines I will eventually write into the FG-file...
		final FactorGraphFileBuilder fgFile = new FactorGraphFileBuilder();

		// FIRST RUN: we export all variables and set varId's for second run...
		for ( int t = 0; t < nodes.getNumberOfTimeSteps(); t++ ) {
			// TODO puke!
			final int regionId = ( t + 1 ) / 2;

			fgFile.addVarComment( "=== VAR-SECTION :: TimePoint t=" + ( t + 1 ) + " ================" );
			fgFile.addVarComment( "--- VAR-SECTION :: Assignment-variables ---------------" );

			fgFile.addFktComment( "=== FKT-SECTION :: TimePoint t=" + ( t + 1 ) + " ================" );
			fgFile.addFktComment( "--- FKT-SECTION :: Unary (Segmentation) Costs ---------" );

			fgFile.addFactorComment( "=== FAC-SECTION :: TimePoint t=" + ( t + 1 ) + " ================" );
			fgFile.addFactorComment( "--- FAC-SECTION :: Unary (Segmentation) Factors -------" );

			final List< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > assmts_t = nodes.getAssignmentsAt( t );
			for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> assmt : assmts_t ) {
				final int var_id = fgFile.addVar( 2 );
				assmt.setVarId( var_id );

				double cost = 0.0;
				if (assmt.getType() == GrowthLineTrackingILP.ASSIGNMENT_MAPPING) {
					fgFile.addVarComment( "- - MAPPING (var: " + var_id + ") - - - - - " );
					fgFile.addFktComment( "- - MAPPING (var: " + var_id + ") - - - - - " );
					final MappingAssignment ma = ( MappingAssignment ) assmt;
					cost = ma.getSourceHypothesis().getCosts() + ma.getDestinationHypothesis().getCosts();
				} else
				if (assmt.getType() == GrowthLineTrackingILP.ASSIGNMENT_DIVISION) {
					fgFile.addVarComment( "- - DIVISION (var: " + var_id + ") - - - - - " );
					fgFile.addFktComment( "- - DIVISION (var: " + var_id + ") - - - - - " );
					final DivisionAssignment da = ( DivisionAssignment ) assmt;
					cost = da.getSourceHypothesis().getCosts() + da.getUpperDesinationHypothesis().getCosts() + da.getLowerDesinationHypothesis().getCosts();
				} else
				if (assmt.getType() == GrowthLineTrackingILP.ASSIGNMENT_EXIT) {
					fgFile.addVarComment( "- - EXIT (var: " + var_id + ") - - - - - " );
					fgFile.addFktComment( "- - EXIT (var: " + var_id + ") - - - - - " );
					final ExitAssignment ea = ( ExitAssignment ) assmt;
					cost = ea.getAssociatedHypothesis().getCosts();
				}

				final int fkt_id = fgFile.addFkt( String.format( "table 1 2 0 %f", cost ) );
				fgFile.addFactor( fkt_id, var_id, regionId );
			}
		}
		// SECOND RUN: export all the rest (now that we have the right varId's).
		for ( int t = 0; t < nodes.getNumberOfTimeSteps(); t++ ) {
			// TODO puke!
			final int regionId = ( t + 1 ) / 2;

			fgFile.addFktComment( "=== FKT-SECTION :: TimePoint t=" + ( t + 1 ) + " ================" );
			fgFile.addFktComment( "--- FKT-SECTION :: Assignment Constraints (HUP-stuff for EXITs) -------------" );

			fgFile.addFactorComment( "=== FAC-SECTION :: TimePoint t=" + ( t + 1 ) + " ================" );
			fgFile.addFactorComment( "--- FAC-SECTION :: Assignment Factors ----------------" );

			final List< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > assmts_t = nodes.getAssignmentsAt( t );
			for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> assmt : assmts_t ) {
				final List< Integer > regionIds = new ArrayList< Integer >();
				regionIds.add( new Integer( regionId ) );
				assmt.addFunctionsAndFactors( fgFile, regionIds );
			}

			// NOTE: last time-point does not get Path-Blocking or Explanation-Continuity-Constraints!
			if ( t == nodes.getNumberOfTimeSteps() - 1 ) continue;

			fgFile.addFktComment( "--- FKT-SECTION :: Path-Blocking Constraints ------------" );
			fgFile.addFactorComment( "--- FAC-SECTION :: Path-Blocking Constraints ------------" );

			final ComponentForest< ? > ct = gl.get( t ).getComponentTree();
			recursivelyAddPathBlockingConstraints( ct, t, fgFile );

			if ( t > 0 && t < nodes.getNumberOfTimeSteps() ) {
				fgFile.addFktComment( "--- FKT-SECTION :: Explanation-Continuity Constraints ------" );
				fgFile.addFactorComment( "--- FAC-SECTION :: Explanation-Continuity Constraints ------" );

				for ( final Hypothesis< Component< DoubleType, ? >> hyp : nodes.getHypothesesAt( t ) ) {
					final List< Integer > varIds = new ArrayList< Integer >();
					final List< Integer > coeffs = new ArrayList< Integer >();

					if ( edgeSets.getLeftNeighborhood( hyp ) != null ) {
						for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> a_j : edgeSets.getLeftNeighborhood( hyp ) ) {
							//expr.addTerm( 1.0, a_j.getGRBVar() );
							coeffs.add( new Integer( 1 ) );
							varIds.add( new Integer( a_j.getVarIdx() ) );
						}
					}
					if ( edgeSets.getRightNeighborhood( hyp ) != null ) {
						for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> a_j : edgeSets.getRightNeighborhood( hyp ) ) {
							//expr.addTerm( -1.0, a_j.getGRBVar() );
							coeffs.add( new Integer( -1 ) );
							varIds.add( new Integer( a_j.getVarIdx() ) );
						}
					}

					// add the constraint for this hypothesis
					//model.addConstr( expr, GRB.EQUAL, 0.0, "ecc_" + eccId );
					final int fkt_id = fgFile.addConstraintFkt( coeffs, "==", 0 );
					fgFile.addFactor( fkt_id, varIds, regionId );
				}
			}
		}

		// WRITE FILE
		fgFile.write( file );
	}

	private < C extends Component< ?, C > > void recursivelyAddPathBlockingConstraints( final ComponentForest< C > ct, final int t, final FactorGraphFileBuilder fgFile ) {
		for ( final C ctRoot : ct.roots() ) {
			// And call the function adding all the path-blocking-constraints...
			recursivelyAddPathBlockingConstraints( ctRoot, t, fgFile );
		}
	}

	/**
	 * Traverses all GL-frames in the given GL and adds all
	 * component-tree-nodes, wrapped in instances of <code>Hypothesis</code>, to
	 * the ILP.
	 * This method calls <code>recursivelyAddCTNsAsHypotheses(...)</code>.
	 */
	private void createSegmentationHypotheses() {
		for ( int t = 0; t < gl.size(); t++ ) {
			final GrowthLineFrame glf = gl.getFrames().get( t );

			for ( final Component< DoubleType, ? > ctRoot : glf.getComponentTree().roots() ) {
				recursivelyAddCTNsAsHypotheses( t, ctRoot );
			}
		}
	}

	/**
	 * Adds all hypothesis given by the nodes in the component tree to
	 * <code>nodes</code>.
	 *
	 * @param ctNode
	 *            a node in a <code>ComponentTree</code>.
	 * @param t
	 *            the time-index the ctNode comes from.
	 */
	private void recursivelyAddCTNsAsHypotheses( final int t, final Component< DoubleType, ? > ctNode ) {

		final double cost = localCost( t, ctNode );
		nodes.addHypothesis( t, new Hypothesis< Component< DoubleType, ? > >( ctNode, cost ) );

		// do the same for all children
		for ( final Component< DoubleType, ? > ctChild : ctNode.getChildren() ) {
			recursivelyAddCTNsAsHypotheses( t, ctChild );
		}
	}

	/**
	 * @param t
	 * @param ctNode
	 * @return
	 */
	public double localCost( final int t, final Component< ?, ? > ctNode ) {
		//TODO kotz
		final double[] gapSepFkt = gl.getFrames().get( t ).getGapSeparationValues( null );
		return CostFactory.getSegmentationCost( ctNode, gapSepFkt );
	}

	/**
	 * Takes all pairs of neighboring time-points, asks them for all the
	 * available segmentation-hypothesis, and enumerates all potentially
	 * interesting assignments using the <code>addXXXAsignment(...)</code>
	 * methods.
	 *
	 * @throws GRBException
	 */
	private void enumerateAndAddAssignments() throws GRBException {
		for ( int t = 0; t < gl.size() - 1; t++ ) {
			final List< Hypothesis< Component< DoubleType, ? >>> curHyps = nodes.getHypothesesAt( t );
			final List< Hypothesis< Component< DoubleType, ? >>> nxtHyps = nodes.getHypothesesAt( t + 1 );

			addExitAssignments( t, curHyps );
			addMappingAssignments( t, curHyps, nxtHyps );
			addDivisionAssignments( t, curHyps, nxtHyps );
		}
	}

	/**
	 * Add an exit-assignment at time t to a bunch of segmentation hypotheses.
	 * Note: exit-assignments cost <code>0</code>, but they come with a
	 * non-trivial construction to enforce, that an exit-assignment can only be
	 * assigned by the solver iff all active segmentation hypotheses above one
	 * that has an active exit-assignment are also assigned with an
	 * exit-assignment.
	 *
	 * @param t
	 *            the time-point.
	 * @param hyps
	 *            a list of hypothesis for which an <code>ExitAssignment</code>
	 *            should be added.
	 * @throws GRBException
	 */
	private void addExitAssignments( final int t, final List< Hypothesis< Component< DoubleType, ? >>> hyps ) throws GRBException {
		final double cost = 0.0;
		int i = 0;
		for ( final Hypothesis< Component< DoubleType, ? >> hyp : hyps ) {
//			cost = hyp.getCosts();
			final GRBVar newLPVar = model.addVar( 0.0, 1.0, cost, GRB.BINARY, String.format( "a_%d^EXIT--%d", t, i ) );
			final List< Hypothesis< Component< DoubleType, ? >>> Hup = LpUtils.getHup( hyp, hyps );
			final ExitAssignment ea = new ExitAssignment( t, newLPVar, model, nodes, edgeSets, Hup, hyp );
			nodes.addAssignment( t, ea );
			edgeSets.addToRightNeighborhood( hyp, ea );
			i++;
		}
	}

	/**
	 * Add a mapping-assignment to a bunch of segmentation hypotheses.
	 *
	 * @param t
	 *            the time-point from which the <code>curHyps</code> originate.
	 * @param curHyps
	 *            a list of hypothesis for which a
	 *            <code>MappingAssignment</code> should be added.
	 * @param nxtHyps
	 *            a list of hypothesis at the next time-point at which the newly
	 *            added <code>MappingAssignments</code> should end at.
	 * @throws GRBException
	 */
	private void addMappingAssignments( final int t, final List< Hypothesis< Component< DoubleType, ? >>> curHyps, final List< Hypothesis< Component< DoubleType, ? >>> nxtHyps ) throws GRBException {
		double cost = 0.0;

		int i = 0;
		for ( final Hypothesis< Component< DoubleType, ? >> from : curHyps ) {
			int j = 0;
			final double fromCost = from.getCosts();

			for ( final Hypothesis< Component< DoubleType, ? >> to : nxtHyps ) {
				final double toCost = to.getCosts();

				if ( !( ComponentTreeUtils.isBelow( to.getWrappedHypothesis(), from.getWrappedHypothesis() ) ) ) {

					if ( !FAST_MODE )
						cost = 1.0 * ( fromCost + toCost ) + compatibilityCostOfMapping( from, to );

					if ( cost <= CUTOFF_COST ) {
						final String name = String.format( "a_%d^MAPPING--(%d,%d)", t, i, j );
						final GRBVar newLPVar = model.addVar( 0.0, 1.0, cost, GRB.BINARY, name );
						final MappingAssignment ma = new MappingAssignment( t, newLPVar, model, nodes, edgeSets, from, to );
						nodes.addAssignment( t, ma );
						if ( edgeSets.addToRightNeighborhood( from, ma ) == false ) {
							System.err.println( "ERROR: Mapping-assignment could not be added to right neighborhood!" );
						}
						if ( edgeSets.addToLeftNeighborhood( to, ma ) == false ) {
							System.err.println( "ERROR: Mapping-assignment could not be added to left neighborhood!" );
						}
						j++;
					}
				}
			}
			i++;
		}
	}

	/**
	 * Computes the compatibility-mapping-costs between the two given
	 * hypothesis.
	 *
	 * @param from
	 *            the segmentation hypothesis from which the mapping originates.
	 * @param to
	 *            the segmentation hypothesis towards which the
	 *            mapping-assignment leads.
	 * @return the cost we want to set for the given combination of segmentation
	 *         hypothesis.
	 */
	private double compatibilityCostOfMapping( final Hypothesis< Component< DoubleType, ? >> from, final Hypothesis< Component< DoubleType, ? >> to ) {
		final long sizeFrom = from.getWrappedHypothesis().size();
		final long sizeTo = to.getWrappedHypothesis().size();

		final double valueFrom = from.getWrappedHypothesis().value().get();
		final double valueTo = to.getWrappedHypothesis().value().get();

		final Pair< Integer, Integer > intervalFrom = ComponentTreeUtils.getTreeNodeInterval( from.getWrappedHypothesis() );
		final Pair< Integer, Integer > intervalTo = ComponentTreeUtils.getTreeNodeInterval( to.getWrappedHypothesis() );

		final double oldPos = 0.5 * ( intervalFrom.getB().intValue() + intervalFrom.getA().intValue() );
		final double newPos = 0.5 * ( intervalTo.getB().intValue() + intervalTo.getA().intValue() );

		final double glLength = gl.get( 0 ).size();

		// Finally the costs are computed...
		final double costDeltaH = CostFactory.getMigrationCost( oldPos, newPos, glLength );
		final double costDeltaL = CostFactory.getGrowthCost( sizeFrom, sizeTo, glLength );
		final double costDeltaV = CostFactory.getIntensityMismatchCost( valueFrom, valueTo );

		final double cost = costDeltaL + costDeltaV + costDeltaH;
//		System.out.println( String.format( ">>> %f + %f + %f = %f", costDeltaL, costDeltaV, costDeltaH, cost ) );
		return cost;
	}

	/**
	 * Add a division-assignment to a bunch of segmentation hypotheses. Note
	 * that this function also looks for suitable pairs of hypothesis in
	 * nxtHyps, since division-assignments naturally need two right-neighbors.
	 *
	 * @param t
	 *            the time-point from which the <code>curHyps</code> originate.
	 * @param curHyps
	 *            a list of hypothesis for which a
	 *            <code>DivisionAssignment</code> should be added.
	 * @param nxtHyps
	 *            a list of hypothesis at the next time-point at which the newly
	 *            added <code>DivisionAssignments</code> should end at.
	 * @throws GRBException
	 */
	private void addDivisionAssignments( final int t, final List< Hypothesis< Component< DoubleType, ? >>> curHyps, final List< Hypothesis< Component< DoubleType, ? >>> nxtHyps ) throws GRBException {
		double cost = 0.0;

		int i = 0;
		for ( final Hypothesis< Component< DoubleType, ? >> from : curHyps ) {
			int j = 0;
			final double fromCost = from.getCosts();

			for ( final Hypothesis< Component< DoubleType, ? >> to : nxtHyps ) {
				if ( !( ComponentTreeUtils.isBelow( to.getWrappedHypothesis(), from.getWrappedHypothesis() ) ) ) {
					for ( final Component< DoubleType, ? > neighborCTN : ComponentTreeUtils.getRightNeighbors( to.getWrappedHypothesis() ) ) {
						@SuppressWarnings( "unchecked" )
						final Hypothesis< Component< DoubleType, ? > > lowerNeighbor = ( Hypothesis< Component< DoubleType, ? >> ) nodes.findHypothesisContaining( neighborCTN );
						if ( lowerNeighbor == null ) {
							System.out.println( "CRITICAL BUG!!!! Check GrowthLineTimeSeris::adDivisionAssignment(...)" );
						} else {
							final double toCost = to.getCosts() + lowerNeighbor.getCosts();

							if ( !FAST_MODE )
								cost = toCost + compatibilityCostOfDivision( from, to, lowerNeighbor );

							if ( cost <= CUTOFF_COST ) {
								final GRBVar newLPVar = model.addVar( 0.0, 1.0, cost, GRB.BINARY, String.format( "a_%d^DIVISION--(%d,%d)", t, i, j ) );
								final DivisionAssignment da = new DivisionAssignment( t, newLPVar, model, nodes, edgeSets, from, to, lowerNeighbor );
								nodes.addAssignment( t, da );
								edgeSets.addToRightNeighborhood( from, da );
								edgeSets.addToLeftNeighborhood( to, da );
								edgeSets.addToLeftNeighborhood( lowerNeighbor, da );
								j++;
							}
						}
					}
				}
			}
			i++;
		}
	}

	/**
	 * Computes the compatibility-mapping-costs between the two given
	 * hypothesis.
	 *
	 * @param from
	 *            the segmentation hypothesis from which the mapping originates.
	 * @param to
	 *            the upper (left) segmentation hypothesis towards which the
	 *            mapping-assignment leads.
	 * @param lowerNeighbor
	 *            the lower (right) segmentation hypothesis towards which the
	 *            mapping-assignment leads.
	 * @return the cost we want to set for the given combination of segmentation
	 *         hypothesis.
	 */
	private double compatibilityCostOfDivision( final Hypothesis< Component< DoubleType, ? >> from, final Hypothesis< Component< DoubleType, ? >> toUpper, final Hypothesis< Component< DoubleType, ? >> toLower ) {
		final long sizeFrom = from.getWrappedHypothesis().size();
		final long sizeToU = toUpper.getWrappedHypothesis().size();
		final long sizeToL = toLower.getWrappedHypothesis().size();
		final long sizeTo = sizeToU + sizeToL;

		final double valueFrom = from.getWrappedHypothesis().value().get();
		final double valueTo = 0.5 * ( toUpper.getWrappedHypothesis().value().get() + toLower.getWrappedHypothesis().value().get() );

		final Pair< Integer, Integer > intervalFrom = ComponentTreeUtils.getTreeNodeInterval( from.getWrappedHypothesis() );
		final Pair< Integer, Integer > intervalToU = ComponentTreeUtils.getTreeNodeInterval( toUpper.getWrappedHypothesis() );
		final Pair< Integer, Integer > intervalToL = ComponentTreeUtils.getTreeNodeInterval( toLower.getWrappedHypothesis() );

		final double oldPos = 0.5 * ( intervalFrom.getB().intValue() + intervalFrom.getA().intValue() );
		final double newPos = 0.5 * ( intervalToL.getB().intValue() + intervalToU.getA().intValue() );

		final double glLength = gl.get( 0 ).size();

		// Finally the costs are computed...
		final double costDeltaH = CostFactory.getMigrationCost( oldPos, newPos, glLength );
		final double costDeltaL = CostFactory.getGrowthCost( sizeFrom, sizeTo, glLength );
		final double costDeltaV = CostFactory.getIntensityMismatchCost( valueFrom, valueTo );
		final double costDeltaS = CostFactory.getUnevenDivisionCost( sizeToU, sizeToL );

		final double cost = costDeltaL + costDeltaV + costDeltaH + costDeltaS;
//		System.out.println( String.format( ">>> %f + %f + %f + %f = %f", costDeltaL, costDeltaV, costDeltaH, costDeltaS, cost ) );
		return cost;
	}

	/**
	 * This function traverses all time points of the growth-line
	 * <code>gl</code>, retrieves the full component tree that has to be built
	 * beforehand, and calls the private method
	 * <code>recursivelyAddPathBlockingConstraints</code> on all those root
	 * nodes. This function adds one constraint for each path starting at a leaf
	 * node in the tree up to the root node itself.
	 * Those path-blocking constraints ensure, that only 0 or 1 of the
	 * segmentation hypothesis along such a path can be chosen during the convex
	 * optimization.
	 *
	 * @throws GRBException
	 *
	 */
	public void addPathBlockingConstraint() throws GRBException {
		// For each time-point
		for ( int t = 0; t < gl.size(); t++ ) {
			// Get the full component tree
			final ComponentForest< ? > ct = gl.get( t ).getComponentTree();
			// And call the function adding all the path-blocking-constraints...
			recursivelyAddPathBlockingConstraints( ct, t );
		}
	}

	private < C extends Component< ?, C > > void recursivelyAddPathBlockingConstraints( final ComponentForest< C > ct, final int t ) throws GRBException {
		for ( final C ctRoot : ct.roots() ) {
			// And call the function adding all the path-blocking-constraints...
			recursivelyAddPathBlockingConstraints( ctRoot, t );
		}
	}

	/**
	 * Generates path-blocking constraints for each path from the given
	 * <code>ctNode</code> to a leaf in the tree.
	 * Those path-blocking constraints ensure, that only 0 or 1 of the
	 * segmentation hypothesis along such a path can be chosen during the convex
	 * optimization.
	 *
	 * @param ctRoot
	 * @param pbcId
	 * @param t
	 * @throws GRBException
	 */
	private < C extends Component< ?, C > > void recursivelyAddPathBlockingConstraints( final C ctNode, final int t ) throws GRBException {

		// if ctNode is a leave node -> add constraint (by going up the list of
		// parents and building up the constraint)
		if ( ctNode.getChildren().size() == 0 ) {
			C runnerNode = ctNode;

			final GRBLinExpr exprR = new GRBLinExpr();
			while ( runnerNode != null ) {
				@SuppressWarnings( "unchecked" )
				final Hypothesis< Component< DoubleType, ? > > hypothesis = ( Hypothesis< Component< DoubleType, ? >> ) nodes.findHypothesisContaining( runnerNode );
				if ( hypothesis == null ) {
					System.err.println( "WARNING: Hypothesis for a CTN was not found in GrowthLineTrackingILP -- this is an indication for some design problem of the system!" );
				}

				if ( edgeSets.getRightNeighborhood( hypothesis ) != null ) {
					for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> a : edgeSets.getRightNeighborhood( hypothesis ) ) {
						exprR.addTerm( 1.0, a.getGRBVar() );
					}
				}
				runnerNode = runnerNode.getParent();
			}
			pbcId++;
			final String name = "pbc_r_t_" + t + "_" + pbcId;
			model.addConstr( exprR, GRB.LESS_EQUAL, 1.0, name );
		} else {
			// if ctNode is a inner node -> recursion
			for ( final C ctChild : ctNode.getChildren() ) {
				recursivelyAddPathBlockingConstraints( ctChild, t );
			}
		}
	}

	/**
	 *
	 * @param ctNode
	 * @param t
	 * @param functions
	 * @param factors
	 */
	private < C extends Component< ?, C > > void recursivelyAddPathBlockingConstraints( final C ctNode, final int t, final FactorGraphFileBuilder fgFile ) {

		// if ctNode is a leave node -> add constraint (by going up the list of
		// parents and building up the constraint)
		if ( ctNode.getChildren().size() == 0 ) {
			final List< Integer > varIds = new ArrayList< Integer >();
			final List< Integer > coeffs = new ArrayList< Integer >();

			C runnerNode = ctNode;

			// final GRBLinExpr exprR = new GRBLinExpr();
			while ( runnerNode != null ) {
				@SuppressWarnings( "unchecked" )
				final Hypothesis< Component< DoubleType, ? > > hypothesis = ( Hypothesis< Component< DoubleType, ? >> ) nodes.findHypothesisContaining( runnerNode );
				if ( hypothesis == null ) {
					System.err.println( "WARNING: Hypothesis for a CTN was not found in GrowthLineTrackingILP -- this is an indication for some design problem of the system!" );
				}

				if ( edgeSets.getRightNeighborhood( hypothesis ) != null ) {
					for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> a : edgeSets.getRightNeighborhood( hypothesis ) ) {
						// exprR.addTerm( 1.0, a.getGRBVar() );
						coeffs.add( new Integer( 1 ) );
						varIds.add( new Integer( a.getVarIdx() ) );
					}
				}
				runnerNode = runnerNode.getParent();
			}
			// model.addConstr( exprR, GRB.LESS_EQUAL, 1.0, name );
			final int fkt_id = fgFile.addConstraintFkt( coeffs, "<=", 1 );
			// TODO puke!
			fgFile.addFactor( fkt_id, varIds, ( t + 1 ) / 2 );
		} else {
			// if ctNode is a inner node -> recursion
			for ( final C ctChild : ctNode.getChildren() ) {
				recursivelyAddPathBlockingConstraints( ctChild, t, fgFile );
			}
		}
	}

	/**
	 * This function generated and adds the explanation-continuity-constraints
	 * to the ILP model.
	 * Those constraints ensure that for each segmentation hypotheses at all
	 * time-points t we have the same number of active incoming and active
	 * outgoing edges from/to assignments.
	 * Intuitively speaking this means that each hypothesis that is chosen by an
	 * assignment coming from t-1 we need to continue its interpretation by
	 * finding an active assignment towards t+1.
	 */
	public void addExplainationContinuityConstraints() throws GRBException {
		int eccId = 0;

		// For each time-point
		for ( int t = 1; t < gl.size() - 1; t++ ) { // !!! sparing out the border !!!

			for ( final Hypothesis< Component< DoubleType, ? >> hyp : nodes.getHypothesesAt( t ) ) {
				final GRBLinExpr expr = new GRBLinExpr();

				if ( edgeSets.getLeftNeighborhood( hyp ) != null ) {
					for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> a_j : edgeSets.getLeftNeighborhood( hyp ) ) {
						expr.addTerm( 1.0, a_j.getGRBVar() );
					}
				}
				if ( edgeSets.getRightNeighborhood( hyp ) != null ) {
					for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> a_j : edgeSets.getRightNeighborhood( hyp ) ) {
						expr.addTerm( -1.0, a_j.getGRBVar() );
					}
				}

				// add the constraint for this hypothesis
				model.addConstr( expr, GRB.EQUAL, 0.0, "ecc_" + eccId );
				eccId++;
			}
		}
	}

	/**
	 * This function takes the ILP (hopefully) built up in <code>model</code>
	 * and starts the convex optimization procedure. This is actually the step
	 * that will find the MAP in the given model and hence the solution to our
	 * segmentation and tracking problem.
	 */
	public void run() {
		try {
			// RUN + return true if solution is feasible
			// - - - - - - - - - - - - - - - - - - - - -
			model.optimize();

			// Read solution and extract interpretation
			// - - - - - - - - - - - - - - - - - - - - -
			if ( model.get( GRB.IntAttr.Status ) == GRB.Status.OPTIMAL ) {
				status = OPTIMAL;
			} else
			if ( model.get( GRB.IntAttr.Status ) == GRB.Status.INFEASIBLE ) {
				status = INFEASIBLE;
			} else
			if ( model.get( GRB.IntAttr.Status ) == GRB.Status.UNBOUNDED ) {
				status = UNBOUNDED;
			} else
			if ( model.get( GRB.IntAttr.Status ) == GRB.Status.SUBOPTIMAL ) {
				status = SUBOPTIMAL;
			} else
			if ( model.get( GRB.IntAttr.Status ) == GRB.Status.NUMERIC ) {
				status = NUMERIC;
			} else {
				status = LIMIT_REACHED;
			}
		}
		catch ( final GRBException e ) {
			System.out.println( "Could not run the generated ILP!" );
			e.printStackTrace();
		}
	}

	/**
	 * Returns the optimal segmentation at time t, given by a list of non
	 * conflicting component-tree-nodes.
	 * Calling this function makes only sense if the <code>run</code>-method was
	 * called and the convex optimizer could find a optimal feasible solution.
	 *
	 * @param t
	 *            the time-point at which to look for the optimal segmentation.
	 * @return a list of <code>ComponentTreeNodes</code> that correspond to the
	 *         active segmentation hypothesis (chosen by the optimization
	 *         procedure).
	 */
	public List< Component< DoubleType, ? >> getOptimalSegmentation( final int t ) {
		final ArrayList< Component< DoubleType, ? >> ret = new ArrayList< Component< DoubleType, ? >>();

		final List< Hypothesis< Component< DoubleType, ? >>> hyps = getOptimalHypotheses( t );
		for ( final Hypothesis< Component< DoubleType, ? >> h : hyps ) {
			ret.add( h.getWrappedHypothesis() );
		}

		return ret;
	}

	/**
	 * Returns the active segmentation at time t and the given y-location along
	 * the gap-separation function of the corresponding GLF.
	 * Calling this function makes only sense if the <code>run</code>-method was
	 * called and the convex optimizer could find a optimal feasible solution.
	 *
	 * @param t
	 *            the time-point at which to look for the optimal segmentation.
	 * @param gapSepYPos
	 *            the position along the gap-separation-function you want to
	 *            receive the active segmentation hypothesis for.
	 * @return a <code>Hypothesis< Component< DoubleType, ? >></code> that
	 *         correspond to the active segmentation hypothesis at the
	 *         requested location.
	 *         Note: this function might return <code>null</code> since not all
	 *         y-locations are occupied by active segmentation hypotheses!
	 */
	public Hypothesis< Component< DoubleType, ? >> getOptimalSegmentationAtLocation( final int t, final int gapSepYPos ) {
		final List< Hypothesis< Component< DoubleType, ? >>> hyps = getOptimalHypotheses( t );
		for ( final Hypothesis< Component< DoubleType, ? >> h : hyps ) {
			final Pair< Integer, Integer > ctnLimits = ComponentTreeUtils.getTreeNodeInterval( h.getWrappedHypothesis() );
			if ( ctnLimits.getA().intValue() <= gapSepYPos && ctnLimits.getB().intValue() >= gapSepYPos ) { return h; }
		}
		return null;
	}

	/**
	 * Returns the optimal segmentation at time t, given by a list of non
	 * conflicting segmentation hypothesis.
	 * Calling this function makes only sense if the <code>run</code>-method was
	 * called and the convex optimizer could find a optimal feasible solution.
	 *
	 * @param t
	 *            the time-point at which to look for the optimal segmentation.
	 * @return a list of <code>Hypothesis< Component< DoubleType, ? > ></code>
	 *         that correspond to the active segmentation hypothesis (chosen by
	 *         the optimization procedure).
	 */
	public List< Hypothesis< Component< DoubleType, ? > > > getOptimalHypotheses( final int t ) {
		final ArrayList< Hypothesis< Component< DoubleType, ? > > > ret = new ArrayList< Hypothesis< Component< DoubleType, ? > > >();

		final List< Hypothesis< Component< DoubleType, ? >>> hyps = nodes.getHypothesesAt( t );

		for ( final Hypothesis< Component< DoubleType, ? >> hyp : hyps ) {
			Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > nh;
			if ( t > 0 ) {
				nh = edgeSets.getLeftNeighborhood( hyp );
			} else {
				nh = edgeSets.getRightNeighborhood( hyp );
			}

			try {
				final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> aa = findActiveAssignment( nh );
				if ( aa != null ) {
					ret.add( hyp );
				}
			}
			catch ( final GRBException e ) {
				System.err.println( "It could not be determined of a certain assignment was choosen during the convex optimization!" );
				e.printStackTrace();
			}
		}

		return ret;
	}

	/**
	 * Finds and returns the optimal left (to t-1) assignments at time-point t.
	 * For each segmentation hypothesis at t we collect all active assignments
	 * coming in from the left (from t-1).
	 * Calling this function makes only sense if the <code>run</code>-method was
	 * called and the convex optimizer could find a optimal feasible solution.
	 *
	 * @param t
	 *            the time at which to look for active left-assignments.
	 *            Values for t make only sense if <code>>=1</code> and
	 *            <code>< nodes.getNumberOfTimeSteps().</code>
	 * @return a hash-map that maps from segmentation hypothesis to sets
	 *         containing ONE assignment that (i) are active, and (ii) come in
	 *         from the left (from t-1).
	 *         Note that segmentation hypothesis that are not active will NOT be
	 *         included in the hash-map.
	 */
	public HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > > getOptimalLeftAssignments( final int t ) {
		assert ( t >= 1 );
		assert ( t < nodes.getNumberOfTimeSteps() );

		final HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > > ret = new HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > >();

		final List< Hypothesis< Component< DoubleType, ? >>> hyps = nodes.getHypothesesAt( t );

		for ( final Hypothesis< Component< DoubleType, ? >> hyp : hyps ) {
			try {
				final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> ola = getOptimalLeftAssignment( hyp );
				if ( ola != null ) {
					final HashSet< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > oneElemSet = new HashSet< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > >();
					oneElemSet.add( ola );
					ret.put( hyp, oneElemSet );
				}
			}
			catch ( final GRBException e ) {
				System.err.println( "An optimal left assignment could not be determined!" );
				e.printStackTrace();
			}
		}

		return ret;
	}

	/**
	 * Finds and returns the optimal left (to t-1) assignment given a
	 * segmentation hypothesis.
	 * For each segmentation hypothesis we know a set of outgoing edges
	 * (assignments) that describe the interpretation (fate) of this segmented
	 * cell. The ILP is set up such that only 1 such assignment can be chosen by
	 * the convex optimizer during the computation of the optimal MAP
	 * assignment.
	 *
	 * @return the optimal (choosen by the convex optimizer) assignment
	 *         describing the most likely data interpretation (MAP) towards the
	 *         previous time-point.
	 * @throws GRBException
	 */
	private AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > getOptimalLeftAssignment( final Hypothesis< Component< DoubleType, ? > > hypothesis ) throws GRBException {
		return findActiveAssignment( edgeSets.getLeftNeighborhood( hypothesis ) );
	}

	/**
	 * Finds and returns the optimal right (to t+1) assignments at time-point t.
	 * For each segmentation hypothesis at t we collect all active assignments
	 * going towards the right (to t+1).
	 * Calling this function makes only sense if the <code>run</code>-method was
	 * called and the convex optimizer could find a optimal feasible solution.
	 *
	 * @param t
	 *            the time at which to look for active right-assignments.
	 *            Values for t make only sense if <code>>=0</code> and
	 *            <code>< nodes.getNumberOfTimeSteps() - 1.</code>
	 * @return a hash-map that maps from segmentation hypothesis to a sets
	 *         containing ONE assignment that (i) are active, and (i) go towards
	 *         the right (to t+1).
	 *         Note that segmentation hypothesis that are not active will NOT be
	 *         included in the hash-map.
	 */
	public HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > > getOptimalRightAssignments( final int t ) {
		assert ( t >= 0 );
		assert ( t < nodes.getNumberOfTimeSteps() - 1 );

		final HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > > ret = new HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > >();

		final List< Hypothesis< Component< DoubleType, ? >>> hyps = nodes.getHypothesesAt( t );

		if ( hyps == null ) return ret;

		for ( final Hypothesis< Component< DoubleType, ? >> hyp : hyps ) {
			try {
				final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> ora = getOptimalRightAssignment( hyp );
				if ( ora != null ) {
					final HashSet< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > oneElemSet = new HashSet< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > >();
					oneElemSet.add( ora );
					ret.put( hyp, oneElemSet );
				}
			}
			catch ( final GRBException e ) {
				System.err.println( "An optimal right assignment could not be determined!" );
				e.printStackTrace();
			}
		}

		return ret;
	}

	/**
	 * Finds and returns the optimal right (to t+1) assignment given a
	 * segmentation hypothesis.
	 * For each segmentation hypothesis we know a set of outgoing edges
	 * (assignments) that describe the interpretation (fate) of this segmented
	 * cell. The ILP is set up such that only 1 such assignment can be chosen by
	 * the convex optimizer during the computation of the optimal MAP
	 * assignment.
	 *
	 * @return the optimal (choosen by the convex optimizer) assignment
	 *         describing the most likely data interpretation (MAP) towards the
	 *         next time-point.
	 * @throws GRBException
	 */
	private AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > getOptimalRightAssignment( final Hypothesis< Component< DoubleType, ? > > hypothesis ) throws GRBException {
		return findActiveAssignment( edgeSets.getRightNeighborhood( hypothesis ) );
	}

	/**
	 * Finds the active assignment in a set of assignments.
	 * This method is thought to be called given a set that can only contain at
	 * max 1 active assignment. (It will always and exclusively return the first
	 * active assignment in the iteration order of the given set!)
	 *
	 * @return the one (first) active assignment in the given set of
	 *         assignments. (An assignment is active iff the binary ILP variable
	 *         associated with the assignment was set to 1 by the convex
	 *         optimizer!)
	 * @throws GRBException
	 */
	private AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > findActiveAssignment( final Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > set ) throws GRBException {
		if ( set == null ) return null;

		for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > a : set ) {
			if ( a.isChoosen() ) { return a; }
		}
		return null;
	}

	/**
	 * Collects and returns all inactive left-assignments given the optimal
	 * segmentation.
	 * An assignment in inactive, when it was NOT chosen by the ILP.
	 * Only those assignments are collected that are left-edges from one of the
	 * currently chosen (optimal) segmentation-hypotheses.
	 *
	 * @param t
	 *            the time at which to look for inactive left-assignments.
	 *            Values for t make only sense if <code>>=1</code> and
	 *            <code>< nodes.getNumberOfTimeSteps().</code>
	 * @return a hash-map that maps from segmentation hypothesis to a set of
	 *         assignments that (i) are NOT active, and (ii) come in from the
	 *         left (from t-1).
	 */
	public HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > > getInactiveLeftAssignments( final int t ) {
		assert ( t >= 1 );
		assert ( t < nodes.getNumberOfTimeSteps() );

		final HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > >> ret = new HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > >();

		final List< Hypothesis< Component< DoubleType, ? >>> hyps = this.getOptimalHypotheses( t );

		for ( final Hypothesis< Component< DoubleType, ? >> hyp : hyps ) {
			try {
				final Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > set = edgeSets.getLeftNeighborhood( hyp );

				if ( set == null ) continue;

				for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > a : set ) {
					if ( !a.isChoosen() ) {
						Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > innerSet = ret.get( hyp );
						if ( innerSet == null ) {
							innerSet = new HashSet< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > >();
							innerSet.add( a );
							ret.put( hyp, innerSet );
						} else {
							innerSet.add( a );
						}
					}
				}
			}
			catch ( final GRBException e ) {
				System.err.println( "Gurobi problem at getInactiveLeftAssignments(t)!" );
				e.printStackTrace();
			}
		}

		return ret;
	}

	/**
	 * Collects and returns all inactive right-assignments given the optimal
	 * segmentation.
	 * An assignment in inactive, when it was NOT chosen by the ILP.
	 * Only those assignments are collected that are right-edges from one of the
	 * currently chosen (optimal) segmentation-hypotheses.
	 *
	 * @param t
	 *            the time at which to look for inactive right-assignments.
	 *            Values for t make only sense if <code>>=0</code> and
	 *            <code>< nodes.getNumberOfTimeSteps()-1.</code>
	 * @return a hash-map that maps from segmentation hypothesis to a set of
	 *         assignments that (i) are NOT active, and (ii) come in from the
	 *         right (from t+1).
	 */
	public HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > > getInactiveRightAssignments( final int t ) {
		assert ( t >= 0 );
		assert ( t < nodes.getNumberOfTimeSteps() - 1 );

		final HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > > ret = new HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > >();

		final List< Hypothesis< Component< DoubleType, ? >>> hyps = this.getOptimalHypotheses( t );

		for ( final Hypothesis< Component< DoubleType, ? >> hyp : hyps ) {
			try {
				final Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > set = edgeSets.getRightNeighborhood( hyp );

				if ( set == null ) continue;

				for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > a : set ) {
					if ( !a.isChoosen() ) {
						Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > innerSet = ret.get( hyp );
						if ( innerSet == null ) {
							innerSet = new HashSet< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > >();
							innerSet.add( a );
							ret.put( hyp, innerSet );
						} else {
							innerSet.add( a );
						}
					}
				}
			}
			catch ( final GRBException e ) {
				System.err.println( "Gurobi problem at getInactiveRightAssignments(t)!" );
				e.printStackTrace();
			}
		}

		return ret;
	}

	/**
	 * Collects and returns all left-assignments given the optimal segmentation.
	 * Only those assignments are collected that are left-edges from one of the
	 * currently chosen (optimal) segmentation-hypotheses.
	 *
	 * @param t
	 *            the time at which to look for inactive left-assignments.
	 *            Values for t make only sense if <code>>=1</code> and
	 *            <code>< nodes.getNumberOfTimeSteps().</code>
	 * @return a hash-map that maps from segmentation hypothesis to a set of
	 *         assignments that come in from the left (from t-1).
	 */
	public HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > > getAllCompatibleLeftAssignments( final int t ) {
		assert ( t >= 1 );
		assert ( t < nodes.getNumberOfTimeSteps() );

		final HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > >> ret = new HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > >();

		final List< Hypothesis< Component< DoubleType, ? >>> hyps = this.getOptimalHypotheses( t );

		for ( final Hypothesis< Component< DoubleType, ? >> hyp : hyps ) {
			final Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > set = edgeSets.getLeftNeighborhood( hyp );

			if ( set == null ) continue;

			for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > a : set ) {
				Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > innerSet = ret.get( hyp );
				if ( innerSet == null ) {
					innerSet = new HashSet< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > >();
					innerSet.add( a );
					ret.put( hyp, innerSet );
				} else {
					innerSet.add( a );
				}
			}
		}

		return ret;
	}

	/**
	 * Collects and returns all right-assignments given the optimal
	 * segmentation.
	 * Only those assignments are collected that are right-edges from one of the
	 * currently chosen (optimal) segmentation-hypotheses.
	 *
	 * @param t
	 *            the time at which to look for inactive right-assignments.
	 *            Values for t make only sense if <code>>=0</code> and
	 *            <code>< nodes.getNumberOfTimeSteps()-1.</code>
	 * @return a hash-map that maps from segmentation hypothesis to a set of
	 *         assignments that come in from the right (from t+1).
	 */
	public HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > > getAllCompatibleRightAssignments( final int t ) {
		assert ( t >= 0 );
		assert ( t < nodes.getNumberOfTimeSteps() - 1 );

		final HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > > ret = new HashMap< Hypothesis< Component< DoubleType, ? > >, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > >();

		final List< Hypothesis< Component< DoubleType, ? >>> hyps = this.getOptimalHypotheses( t );

		for ( final Hypothesis< Component< DoubleType, ? >> hyp : hyps ) {
			final Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > set = edgeSets.getRightNeighborhood( hyp );

			if ( set == null ) continue;

			for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > a : set ) {
				Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > innerSet = ret.get( hyp );
				if ( innerSet == null ) {
					innerSet = new HashSet< AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > >();
					innerSet.add( a );
					ret.put( hyp, innerSet );
				} else {
					innerSet.add( a );
				}
			}
		}

		return ret;
	}
}