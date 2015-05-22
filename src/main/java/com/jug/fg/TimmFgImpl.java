/**
 *
 */
package com.jug.fg;

import gurobi.GRBException;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;

import net.imglib2.algorithm.componenttree.ComponentForest;
import net.imglib2.type.numeric.real.FloatType;

import com.indago.fg.Assignment;
import com.indago.fg.FactorGraph;
import com.indago.fg.domain.BooleanFunctionDomain;
import com.indago.fg.factor.BooleanFactor;
import com.indago.fg.factor.Factor;
import com.indago.fg.function.BooleanAssignmentConstraint;
import com.indago.fg.function.BooleanConflictConstraint;
import com.indago.fg.function.BooleanTensorTable;
import com.indago.fg.function.BooleanWeightedIndexSumConstraint;
import com.indago.fg.function.Function;
import com.indago.fg.function.WeightedIndexSumConstraint.Relation;
import com.indago.fg.gui.FgPanel;
import com.indago.fg.variable.AssignmentVariable;
import com.indago.fg.variable.BooleanVariable;
import com.indago.fg.variable.ComponentVariable;
import com.indago.ilp.SolveBooleanFGGurobi;
import com.jug.GrowthLine;
import com.jug.GrowthLineFrame;
import com.jug.MotherMachine;
import com.jug.tracking.costs.CostFactory;
import com.jug.util.ComponentTreeUtils;
import com.jug.util.filteredcomponents.FilteredComponent;

/**
 * @author jug
 */
public class TimmFgImpl {

	private final GrowthLine gl;

	private int functionId = 0;
	private int factorId = 0;
	public static BooleanFunctionDomain unaryDomain = new BooleanFunctionDomain( 1 );
	public static BooleanFunctionDomain mappingDomain = new BooleanFunctionDomain( 3 );
	public static BooleanFunctionDomain divisionDomain = new BooleanFunctionDomain( 4 );

	final ArrayList< Function< ?, ? > > functions = new ArrayList<>();
	final ArrayList< Factor< ?, ?, ? > > factors = new ArrayList<>();
	final HashMap< FilteredComponent< FloatType >, ComponentVariable< FilteredComponent< FloatType > >> varSegHyps =
			new HashMap<>();
	final List< AssignmentVariable< FilteredComponent< FloatType > >> varMappings =
			new ArrayList<>();
	final List< AssignmentVariable< FilteredComponent< FloatType > >> varDivisions =
			new ArrayList<>();
	final List< AssignmentVariable< FilteredComponent< FloatType > >> varExits =
			new ArrayList<>();

	final HashMap< FilteredComponent< FloatType >, HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType > > > > rightNeighbors =
			new HashMap<>();
	final HashMap< FilteredComponent< FloatType >, HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType > > > > leftNeighbors =
			new HashMap<>();

	private FactorGraph fg;
	private Assignment solution;

	/**
	 * @param growthLine
	 */
	public TimmFgImpl( final GrowthLine gl ) {
		this.gl = gl;

		// ---------------------------------------------------------------------------------------------------------------
		// UNARIES UNARIES UNARIES UNARIES UNARIES UNARIES UNARIES UNARIES UNARIES UNARIES UNARIES UNARIES UNARIES UNARIES
		// ---------------------------------------------------------------------------------------------------------------

		for ( final GrowthLineFrame glf : gl.getFrames() ) {
			final float[] gapSepFkt =
					glf.getSimpleGapSeparationValues( MotherMachine.instance.getImgTemp() );

			for ( final FilteredComponent< FloatType > ctRoot : glf.getComponentTree().roots() ) {
				addSegmentBranch( ctRoot, gapSepFkt );
				addTreePathConstraints( ctRoot );
			}
		}


		// ---------------------------------------------------------------------------------------------------------------
		// HIGHER ORDER POTENTIALS HIGHER ORDER POTENTIALS HIGHER ORDER POTENTIALS HIGHER ORDER POTENTIALS HIGHER ORDER
		// ---------------------------------------------------------------------------------------------------------------

		for ( int t = 0; t < gl.size(); t++ ) {
			int tp1 = t + 1;
			if ( tp1 == gl.size() ) tp1 = t; // We double the last GLF to avoid border effects! This is not stupid! ;)

			final GrowthLineFrame glfT = gl.get( t );
			final GrowthLineFrame glfTp1 = gl.get( tp1 );

			addMappings( glfT, glfTp1 );
			addDivisions( glfT, glfTp1 );
			addExits( glfT );
		}

		for ( final GrowthLineFrame glf : gl.getFrames() ) {
			if ( glf.getTime() != 0 && glf.getTime() != gl.getFrames().size() - 1 ) {
				addTrackContinuationConstraints( glf.getComponentTree().roots() );
			}
		}

//		show();
	}

	/**
	 * @param varSegHyps
	 * @param ctRoot
	 * @param gapSepFkt
	 */
	public void addSegmentBranch(
			final FilteredComponent< FloatType > ctNode,
			final float[] gapSepFkt ) {

		final ComponentVariable< FilteredComponent< FloatType >> newVar =
				new ComponentVariable< FilteredComponent< FloatType > >( ctNode );
		newVar.setAnnotatedCost( CostFactory.getIntensitySegmentationCost( ctNode, gapSepFkt ) );

		varSegHyps.put( ctNode, newVar );
		addUnaryTo( ctNode, newVar, newVar.getAnnotatedCost() );

		for ( final FilteredComponent< FloatType > child : ctNode.getChildren() ) {
			addSegmentBranch( child, gapSepFkt );
		}
	}

	/**
	 * @param ctRoot
	 */
	private void addTreePathConstraints( final FilteredComponent< FloatType > ctRoot ) {
		final LinkedList< FilteredComponent< FloatType >> leaves = new LinkedList<>();

		// --- find leaves ------------------------------------------------------------------------
		final LinkedList< FilteredComponent< FloatType >> queue = new LinkedList<>();
		queue.add( ctRoot );
		while ( !queue.isEmpty() ) {
			final FilteredComponent< FloatType > node = queue.removeFirst();

			if ( node.getChildren().size() == 0 ) {
				leaves.push( node );
			} else {
				for ( final FilteredComponent< FloatType > child : node.getChildren() ) {
					queue.push( child );
				}
			}
		}

		// --- per leave compose path constraint --------------------------------------------------
		while ( !leaves.isEmpty() ) {
			final FilteredComponent< FloatType > leave = leaves.removeFirst();

			final List< ComponentVariable< FilteredComponent< FloatType >> > path =
					new ArrayList<>();
			FilteredComponent< FloatType > runningNode = leave;
			path.add( varSegHyps.get( runningNode ) );
			while ( runningNode.getParent() != null ) {
				runningNode = runningNode.getParent();
				path.add( varSegHyps.get( runningNode ) );
			}

			// --- add only if path is not only a root --------------------------------------------
			if ( path.size() > 1 ) {
				// --- connect variables collected along path by a path constraint ----------------
				final BooleanFactor factor =
						new BooleanFactor( new BooleanFunctionDomain( path.size() ), factorId++ );
				final BooleanConflictConstraint bcc = new BooleanConflictConstraint();
				factor.setFunction( bcc );
				int i = 0;
				for ( final ComponentVariable< FilteredComponent< FloatType >> var : path ) {
					factor.setVariable( i, var );
					i++;
				}
				functions.add( bcc );
				factors.add( factor );
			}
		}
	}

	/**
	 * @param fs
	 * @param ctRoot
	 * @param varSegHyps
	 * @param functions
	 * @param factors
	 */
	private void addUnaryTo(
			final FilteredComponent< FloatType > ctNode,
			final ComponentVariable< FilteredComponent< FloatType >> newVar,
			final float cost ) {
		final double[] entries =
				new double[] { 0.0, 0.0 }; //cost };  // TODO unary fuckup
		final BooleanTensorTable btt = new BooleanTensorTable( unaryDomain, entries, functionId++ );
		final BooleanFactor factor = new BooleanFactor( unaryDomain, factorId++ );
		factor.setFunction( btt );
		factor.setVariable( 0, varSegHyps.get( ctNode ) );

		functions.add( btt );
		factors.add( factor );
	}

	/**
	 * @param glfT
	 * @param glfTp1
	 */
	private void addMappings( final GrowthLineFrame glfT, final GrowthLineFrame glfTp1 ) {

		final ComponentForest< FilteredComponent< FloatType >> seghypsT =
				glfT.getComponentTree();
		final ComponentForest< FilteredComponent< FloatType >> seghypsTp1 =
				glfTp1.getComponentTree();

		for ( final FilteredComponent< FloatType > ctRootT : seghypsT.roots() ) {
			for ( final FilteredComponent< FloatType > ctRootTp1 : seghypsTp1.roots() ) {
				addMappingsNtoM( glfT.size(), ctRootT, ctRootTp1 );
			}
		}
	}

	/**
	 * @param glTLength
	 * @param ctRootT
	 * @param ctRootTp1
	 */
	private void addMappingsNtoM(
			final int glTLength, final FilteredComponent< FloatType > ctRootT,
			final FilteredComponent< FloatType > ctRootTp1 ) {

		final LinkedList<FilteredComponent< FloatType >> queue = new LinkedList<>();
		queue.add( ctRootT );
		while (!queue.isEmpty()) {
			final FilteredComponent< FloatType > node = queue.removeFirst();

			addMappings1toM( glTLength, node, ctRootTp1 );

			for ( final FilteredComponent< FloatType > child : node.getChildren() ) {
				queue.push( child );
			}
		}
	}

	/**
	 * @param glTLength
	 * @param ctNodeT
	 * @param ctRootTp1
	 */
	private void addMappings1toM(
			final int glTLength,
			final FilteredComponent< FloatType > ctNodeT,
			final FilteredComponent< FloatType > ctRootTp1 ) {

		// Establish one connection from ctNode to ctNodeTp1 (if not cut off)
		final LinkedList< FilteredComponent< FloatType >> queue = new LinkedList<>();
		queue.add( ctRootTp1 );
		while ( !queue.isEmpty() ) {
			final FilteredComponent< FloatType > nodeTp1 = queue.removeFirst();

			final float fromCost = varSegHyps.get( ctNodeT ).getAnnotatedCost();
			final float toCost = varSegHyps.get( nodeTp1 ).getAnnotatedCost();
			final float costMapping = 0.1f * fromCost + 0.9f * toCost +
					CostFactory.compatibilityCostOfMapping( ctNodeT, nodeTp1, glTLength );

			if ( costMapping < MotherMachine.ENUMERATION_CUTOFF_COST ) {
				addMappingBetween( ctNodeT, nodeTp1, costMapping );
			}

			for ( final FilteredComponent< FloatType > child : nodeTp1.getChildren() ) {
				queue.push( child );
			}
		}
	}

	/**
	 * @param ctNodeT
	 * @param ctNodeTp1
	 * @param costMapping
	 */
	private void addMappingBetween(
			final FilteredComponent< FloatType > ctNodeT,
			final FilteredComponent< FloatType > ctNodeTp1,
			final float cost ) {

		// --- add new variable -----------------------------------------------------------------------------
		final AssignmentVariable< FilteredComponent< FloatType >> newVar =
				new AssignmentVariable< FilteredComponent< FloatType > >( ctNodeT, AssignmentVariable.ASSIGNMENT_MAPPING );
		newVar.setAnnotatedCost( cost );
		varMappings.add( newVar );

		// --- add new variable into left- and right neighborhoods ------------------------------------------
		HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType > > > ln =
				leftNeighbors.get( ctNodeTp1 );
		if ( ln == null ) {
			ln = new HashMap<>();
			leftNeighbors.put( ctNodeTp1, ln );
		}
		final ArrayList< FilteredComponent< FloatType >> leftSegmentList =
				new ArrayList< FilteredComponent< FloatType >>();
		leftSegmentList.add( ctNodeT );
		ln.put( newVar, leftSegmentList );

		HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType > > > rn =
				rightNeighbors.get( ctNodeT );
		if ( rn == null ) {
			rn = new HashMap<>();
			rightNeighbors.put( ctNodeT, rn );
		}
		final ArrayList< FilteredComponent< FloatType >> rightSegmentList =
				new ArrayList< FilteredComponent< FloatType >>();
		rightSegmentList.add( ctNodeTp1 );
		rn.put( newVar, rightSegmentList );

		// --- connect variables by a constraint factor -----------------------------------------------------
		final BooleanFactor factor = new BooleanFactor( mappingDomain, factorId++ );
		final BooleanAssignmentConstraint bac = new BooleanAssignmentConstraint();
		factor.setFunction( bac );
		factor.setVariable( 0, newVar );
		factor.setVariable( 1, varSegHyps.get( ctNodeT ) );
		factor.setVariable( 2, varSegHyps.get( ctNodeTp1 ) );

		functions.add( bac );
		factors.add( factor );

		// --- unary costs to new assignment variable -------------------------------------------------------
		final double[] entries =
				new double[] { 0.0, cost };
		final BooleanTensorTable btt = new BooleanTensorTable( unaryDomain, entries, functionId++ );
		final BooleanFactor unaryFactor = new BooleanFactor( unaryDomain, factorId++ );
		unaryFactor.setFunction( btt );
		unaryFactor.setVariable( 0, newVar );

		functions.add( btt );
		factors.add( unaryFactor );
	}

	/**
	 * @param glfT
	 * @param glfTp1
	 */
	private void addDivisions( final GrowthLineFrame glfT, final GrowthLineFrame glfTp1 ) {

		final ComponentForest< FilteredComponent< FloatType >> seghypsT =
				glfT.getComponentTree();
		final ComponentForest< FilteredComponent< FloatType >> seghypsTp1 =
				glfTp1.getComponentTree();

		for ( final FilteredComponent< FloatType > ctRootT : seghypsT.roots() ) {
			for ( final FilteredComponent< FloatType > ctRootTp1 : seghypsTp1.roots() ) {
				addDivisionsNtoM( glfT.size(), ctRootT, ctRootTp1 );
			}
		}
	}

	/**
	 * @param glTLength
	 * @param ctRootT
	 * @param ctRootTp1
	 */
	private void addDivisionsNtoM(
			final int glTLength, final FilteredComponent< FloatType > ctRootT,
			final FilteredComponent< FloatType > ctRootTp1 ) {

		final LinkedList< FilteredComponent< FloatType >> queue = new LinkedList<>();
		queue.add( ctRootT );
		while ( !queue.isEmpty() ) {
			final FilteredComponent< FloatType > node = queue.removeFirst();

			addDivisions1toM( glTLength, node, ctRootTp1 );

			for ( final FilteredComponent< FloatType > child : node.getChildren() ) {
				queue.push( child );
			}
		}
	}

	/**
	 * @param glTLength
	 * @param ctNodeT
	 * @param ctRootTp1
	 */
	private void addDivisions1toM(
			final int glTLength,
			final FilteredComponent< FloatType > ctNodeT,
			final FilteredComponent< FloatType > ctRootTp1 ) {

		// Establish one connection from ctNode to ctNodeTp1 (if not cut off)
		final LinkedList< FilteredComponent< FloatType >> queue = new LinkedList<>();
		queue.add( ctRootTp1 );
		while ( !queue.isEmpty() ) {
			final FilteredComponent< FloatType > nodeTp1_upper = queue.removeFirst();

			for ( final FilteredComponent< FloatType > nodeTp1_lower : ComponentTreeUtils.getRightNeighbors( nodeTp1_upper ) ) {

				final float fromCost = varSegHyps.get( ctNodeT ).getAnnotatedCost();
				final float toCostUpper = varSegHyps.get( nodeTp1_upper ).getAnnotatedCost();
				final float toCostLower = varSegHyps.get( nodeTp1_lower ).getAnnotatedCost();
				final float costDivision = 0.1f * fromCost + 0.9f * ( toCostUpper + toCostLower ) +
						CostFactory.compatibilityCostOfDivision(
								ctNodeT,
								nodeTp1_upper,
								nodeTp1_lower,
								glTLength );
				if ( costDivision < MotherMachine.ENUMERATION_CUTOFF_COST ) {
					addDivisionBetween( ctNodeT, nodeTp1_upper, nodeTp1_lower, costDivision );
				}

				for ( final FilteredComponent< FloatType > child : nodeTp1_upper.getChildren() ) {
					queue.push( child );
				}
			}
		}
	}

	/**
	 * @param ctNodeT
	 * @param ctNodeTp1
	 * @param nodeTp1_lower
	 * @param costMapping
	 */
	private void addDivisionBetween(
			final FilteredComponent< FloatType > ctNodeT,
			final FilteredComponent< FloatType > ctNodeTp1_upper,
			final FilteredComponent< FloatType > ctNodeTp1_lower,
			final float cost ) {

		// --- add new variable -----------------------------------------------------------------------------
		final AssignmentVariable< FilteredComponent< FloatType >> newVar =
				new AssignmentVariable< FilteredComponent< FloatType > >( ctNodeT, AssignmentVariable.ASSIGNMENT_DIVISION );
		newVar.setAnnotatedCost( cost );
		varDivisions.add( newVar );

		// --- add new variable into left- and right neighborhoods ------------------------------------------
		HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType > > > ln_upper =
				leftNeighbors.get( ctNodeTp1_upper );
		if ( ln_upper == null ) {
			ln_upper = new HashMap<>();
			leftNeighbors.put( ctNodeTp1_upper, ln_upper );
		}
		HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType > > > ln_lower =
				leftNeighbors.get( ctNodeTp1_lower );
		if ( ln_lower == null ) {
			ln_lower = new HashMap<>();
			leftNeighbors.put( ctNodeTp1_lower, ln_lower );
		}
		final ArrayList< FilteredComponent< FloatType >> leftSegmentList =
				new ArrayList< FilteredComponent< FloatType >>();
		leftSegmentList.add( ctNodeT );
		ln_upper.put( newVar, leftSegmentList );
		ln_lower.put( newVar, leftSegmentList );

		HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType > > > rn =
				rightNeighbors.get( ctNodeT );
		if ( rn == null ) {
			rn = new HashMap<>();
			rightNeighbors.put( ctNodeT, rn );
		}
		final ArrayList< FilteredComponent< FloatType >> rightSegmentList =
				new ArrayList< FilteredComponent< FloatType >>();
		rightSegmentList.add( ctNodeTp1_upper );
		rightSegmentList.add( ctNodeTp1_lower );
		rn.put( newVar, rightSegmentList );

		// --- connect variables by a constraint factor -----------------------------------------------------
		final BooleanFactor factor = new BooleanFactor( divisionDomain, factorId++ );
		final BooleanAssignmentConstraint bac = new BooleanAssignmentConstraint();
		factor.setFunction( bac );
		factor.setVariable( 0, newVar );
		factor.setVariable( 1, varSegHyps.get( ctNodeT ) );
		factor.setVariable( 2, varSegHyps.get( ctNodeTp1_upper ) );
		factor.setVariable( 3, varSegHyps.get( ctNodeTp1_lower ) );

		functions.add( bac );
		factors.add( factor );

		// --- unary costs to new assignment variable -------------------------------------------------------
		final double[] entries =
				new double[] { 0.0, cost };
		final BooleanTensorTable btt = new BooleanTensorTable( unaryDomain, entries, functionId++ );
		final BooleanFactor unaryFactor = new BooleanFactor( unaryDomain, factorId++ );
		unaryFactor.setFunction( btt );
		unaryFactor.setVariable( 0, newVar );

		functions.add( btt );
		factors.add( unaryFactor );

	}

	/**
	 * @param glf
	 */
	private void addExits( final GrowthLineFrame glf ) {

		for ( final FilteredComponent< FloatType > root : glf.getComponentTree().roots() ) {
			final LinkedList< FilteredComponent< FloatType >> queue = new LinkedList<>();
			queue.add( root );
			while ( !queue.isEmpty() ) {
				final FilteredComponent< FloatType > node = queue.removeFirst();
				final float fromCost = varSegHyps.get( node ).getAnnotatedCost();

				// TODO: cost-fuckup - folding of segment costs into assignment costs not needed any longer -- remove!
				final float cost = Math.min( 0.0f, fromCost / 2.0f ); // NOTE: 0 or negative but only hyp/2 to prefer map or div if exists...
				addExitTo( node, glf.getComponentTree().roots(), cost );

				for ( final FilteredComponent< FloatType > child : node.getChildren() ) {
					queue.push( child );
				}
			}
		}
	}

	/**
	 * @param ctNode
	 * @param roots
	 *            The roots from which all segment hypotheses of this time-point
	 *            can be found. We need this here in order to assemble the exit
	 *            constraints!
	 * @param cost
	 */
	private void addExitTo(
			final FilteredComponent< FloatType > ctNode,
			final Set< FilteredComponent< FloatType >> roots,
			final float cost ) {
		// --- add new variable -----------------------------------------------------------------------------
		final AssignmentVariable< FilteredComponent< FloatType >> newVar =
				new AssignmentVariable< FilteredComponent< FloatType > >( ctNode, AssignmentVariable.ASSIGNMENT_EXIT );
		newVar.setAnnotatedCost( cost );
		varExits.add( newVar );

		// --- add new variable into right neighborhood -----------------------------------------------------
		HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType > > > rn =
				rightNeighbors.get( ctNode );
		if ( rn == null ) {
			rn = new HashMap<>();
			rightNeighbors.put( ctNode, rn );
		}
		final ArrayList< FilteredComponent< FloatType >> rightSegmentList =
				new ArrayList< FilteredComponent< FloatType >>(); // EMPTY LIST!!!
		rn.put( newVar, rightSegmentList );

		// --- collect the segment hyps above ctNode (in Hup) ---------------------------------
		final List< FilteredComponent< FloatType >> Hup = FgUtils.getHup( ctNode, roots );

		// --- add constraint connecting Hup --------------------------------------------------
		if ( Hup.size() > 0 ) {
			// --- connect variables collected along path by a path constraint ----------------
			final int[] coefficients = new int[ Hup.size() + 1 ];
			for ( int i = 0; i < coefficients.length; i++ )
				coefficients[ i ] = 1;
			coefficients[ 0 ] = Hup.size();
			final BooleanWeightedIndexSumConstraint bwisc =
					new BooleanWeightedIndexSumConstraint( coefficients, Relation.LE, Hup.size() );

			final BooleanFactor factor = new BooleanFactor( bwisc.getDomain(), factorId++ );

			factor.setFunction( bwisc );
			factor.setVariable( 0, varSegHyps.get( ctNode ) );
			int i = 1;
			for ( final FilteredComponent< FloatType > seghyp : Hup ) {
				factor.setVariable( i, varSegHyps.get( seghyp ) );
				i++;
			}
			functions.add( bwisc );
			factors.add( factor );
		}

		// --- unary costs to new assignment variable -------------------------------------------------------
		final double[] entries =
				new double[] { 0.0, cost };
		final BooleanTensorTable btt = new BooleanTensorTable( unaryDomain, entries, functionId++ );
		final BooleanFactor unaryFactor = new BooleanFactor( unaryDomain, factorId++ );
		unaryFactor.setFunction( btt );
		unaryFactor.setVariable( 0, newVar );

		functions.add( btt );
		factors.add( unaryFactor );
	}

	/**
	 * @param roots
	 */
	private void addTrackContinuationConstraints( final Set< FilteredComponent< FloatType >> roots ) {
		for ( final FilteredComponent< FloatType > root : roots ) {
			final LinkedList< FilteredComponent< FloatType >> queue = new LinkedList<>();
			queue.add( root );
			while ( !queue.isEmpty() ) {
				final FilteredComponent< FloatType > node = queue.removeFirst();

				// add constraint: sum left neighborhood vars == sum right neighborhood vars
				final ComponentVariable< FilteredComponent< FloatType >> varSeg =
						varSegHyps.get( node );
				final HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> ln =
						leftNeighbors.get( varSeg );
				final HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> rn =
						rightNeighbors.get( varSeg );
				addPathContinuationConstraint( ln, rn );

				for ( final FilteredComponent< FloatType > child : node.getChildren() ) {
					queue.push( child );
				}
			}
		}
	}

	/**
	 * @param ln
	 * @param rn
	 */
	private void addPathContinuationConstraint(
			final HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> ln,
			final HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> rn ) {

		if ( ln == null && rn == null ) {
//			System.out.println( "Warning: isolated variable - no PathContinuationConstraint added!" );
			return;
		}
		if ( ln == null || rn == null ) {
//			System.out.println( "Variable at first or last time point not added PathContinuationConstraint for!" );
			return;
		}

		System.out.println( "PathContinuationConstraint added!" );
		final int dims = ln.keySet().size() + rn.keySet().size();

		final int[] coeffs = new int[ dims ];
		for ( int i = 0; i < ln.keySet().size(); i++ ) {
			coeffs[ i ] = 1;
		}
		for ( int i = 0; i < rn.keySet().size(); i++ ) {
			coeffs[ ln.keySet().size() + i ] = -1;
		}
		final BooleanFactor factor =
				new BooleanFactor( new BooleanFunctionDomain( dims ), factorId++ );
		final BooleanWeightedIndexSumConstraint bwisc =
				new BooleanWeightedIndexSumConstraint( coeffs, Relation.EQ, 0 );
		factor.setFunction( bwisc );
		int i = 0;
		for ( final AssignmentVariable< FilteredComponent< FloatType >> var : ln.keySet() ) {
			factor.setVariable( i, var );
			i++;
		}
		for ( final AssignmentVariable< FilteredComponent< FloatType >> var : rn.keySet() ) {
			factor.setVariable( i, var );
			i++;
		}
		functions.add( bwisc );
		factors.add( factor );
	}

	/**
	 *
	 */
	public void show() {
		final FactorGraph fg = assembleFactorGraph();

		final FgPanel panel = new FgPanel( fg );
		final JFrame frame = new JFrame( "TimmFgImpl - dump" );
		frame.setBounds( 50, 50, 1200, 900 );
		frame.getContentPane().setLayout( new BorderLayout() );
		frame.getContentPane().add( panel );
		frame.setVisible( true );
	}

	/**
	 * @return
	 */
	private FactorGraph assembleFactorGraph() {
		final Collection< BooleanVariable > vars = new ArrayList< BooleanVariable >();
		for ( final ComponentVariable< FilteredComponent< FloatType >> var : varSegHyps.values() ) {
			vars.add( var );
		}
		for ( final AssignmentVariable< FilteredComponent< FloatType > > var : varMappings ) {
			vars.add( var );
		}
		for ( final AssignmentVariable< FilteredComponent< FloatType > > var : varDivisions ) {
			vars.add( var );
		}
		for ( final AssignmentVariable< FilteredComponent< FloatType > > var : varExits ) {
			vars.add( var );
		}

		return new FactorGraph( vars, factors, functions );
	}

	public Assignment solve() {
		fg = assembleFactorGraph();

		solution = null;
		try {
			final SolveBooleanFGGurobi solver = new SolveBooleanFGGurobi();
			solution = solver.solve( fg );

			for ( final FilteredComponent< FloatType > seg : varSegHyps.keySet() ) {
				final ComponentVariable< FilteredComponent< FloatType >> var = varSegHyps.get( seg );
				if ( solution.getAssignment( var ).get() ) {
					System.out.println( "\n>>> Cost: " + var.getAnnotatedCost() );
				} else {
					System.out.print( "." );
				}
			}
		} catch ( @SuppressWarnings( "restriction" ) final GRBException e ) {
			System.err.println( "Gurobi trouble... Boolean FactorGraph could not be solved!" );
			e.printStackTrace();
		}
		return solution;
	}

	/**
	 * @return the solution to the built FactorGraph.
	 */
	public Assignment getSolution() {
		return solution;
	}

	/**
	 * @param t
	 *            the time point for which all active segments should be
	 *            returned. (A segment is active if it was choosen to be part of
	 *            the solution of this factor graph!)
	 * @return a <code>Collection</code> of segments that where choosen to be
	 *         part of the solution (by solving the factor graph at hand).
	 */
	private Collection< FilteredComponent< FloatType >> getSegmentsAt( final int t ) {
		final List< FilteredComponent< FloatType >> ret =
				new ArrayList< FilteredComponent< FloatType >>();

		// iterate all segments + collect
		for ( final FilteredComponent< FloatType > root : gl.get( t ).getComponentTree().roots() ) {
			final LinkedList< FilteredComponent< FloatType >> queue = new LinkedList<>();
			queue.add( root );
			while ( !queue.isEmpty() ) {
				final FilteredComponent< FloatType > node = queue.removeFirst();

				ret.add( node );

				for ( final FilteredComponent< FloatType > child : node.getChildren() ) {
					queue.push( child );
				}
			}
		}
		return ret;
	}

	/**
	 * @param t
	 *            the time point for which all active segments should be
	 *            returned. (A segment is active if it was choosen to be part of
	 *            the solution of this factor graph!)
	 * @return a <code>Collection</code> of segments that where choosen to be
	 *         part of the solution (by solving the factor graph at hand).
	 */
	private Collection< FilteredComponent< FloatType >> getActiveSegmentsAt( final int t ) {
		final List< FilteredComponent< FloatType >> ret = new ArrayList< FilteredComponent< FloatType >> ();

		if (solution == null) {
			return ret;
		}
		// iterate all segments + check for status in assignment (solution)
		for ( final FilteredComponent< FloatType > root : gl.get( t ).getComponentTree().roots() ) {
			final LinkedList< FilteredComponent< FloatType >> queue = new LinkedList<>();
			queue.add( root );
			while ( !queue.isEmpty() ) {
				final FilteredComponent< FloatType > node = queue.removeFirst();

				if ( solution.getAssignment( varSegHyps.get( node ) ).get() == true ) {
					ret.add( node );
				}

				for ( final FilteredComponent< FloatType > child : node.getChildren() ) {
					queue.push( child );
				}
			}
		}
		return ret;
	}

	/**
	 * @param segments
	 *            segments for which leaving assignments should be returned.
	 * @return
	 * @return
	 */
	public HashMap< FilteredComponent< FloatType >, HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> > getRightNeighborsOf(
			final Collection< FilteredComponent< FloatType >> segments ) {
		final HashMap< FilteredComponent< FloatType >, HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> > ret =
				new HashMap<>();

		// Collect variables leaving segments at time t
		for ( final FilteredComponent< FloatType > segment : segments ) {
			ret.put( segment, getRightNeighborsOf( segment ) );
		}
		return ret;
	}

	/**
	 * @param segment
	 *            segment for which leaving assignments should be returned.
	 * @return
	 */
	public HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> getRightNeighborsOf(
			final FilteredComponent< FloatType > segment ) {

		return rightNeighbors.get( segment );
	}

	/**
	 * @param segments
	 *            segments for which leaving active assignments should be
	 *            returned.
	 * @return
	 * @return
	 */
	public HashMap< FilteredComponent< FloatType >, HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> > getActiveRightNeighborsOf(
			final Collection< FilteredComponent< FloatType >> segments ) {
		final HashMap< FilteredComponent< FloatType >, HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> > ret =
				new HashMap<>();

		// Collect variables leaving segments at time t
		for ( final FilteredComponent< FloatType > segment : segments ) {
			ret.put( segment, getActiveRightNeighborsOf( segment ) );
		}
		return ret;
	}

	/**
	 * @param segment
	 *            segment for which active leaving assignments should be
	 *            returned.
	 */
	private HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> getActiveRightNeighborsOf(
			final FilteredComponent< FloatType > segment ) {
		final HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> ret =
				new HashMap<>();

		if ( solution == null ) { return ret; }

		for ( final AssignmentVariable< FilteredComponent< FloatType >> var : rightNeighbors.get(
				segment ).keySet() ) {
			if ( solution.getAssignment( var ).get() == true ) {
				ret.put( var, rightNeighbors.get( segment ).get( var ) );
			}
		}
		return ret;
	}

	/**
	 * @param t
	 *            the time point for which all right neighbors of aktivated
	 *            (contained in solution) segments should be returned. Returns
	 *            empty map in case the factor graph was not solved yet.
	 * @return
	 */
	public HashMap< FilteredComponent< FloatType >, HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> > getActiveRightNeighborsAt(
			final int t ) {
		return getActiveRightNeighborsOf( getActiveSegmentsAt( t ) );
	}

	/**
	 * @param t
	 *            the time point for which all right neighbors of all segments
	 *            should be returned.
	 * @return
	 */
	public HashMap< FilteredComponent< FloatType >, HashMap< AssignmentVariable< FilteredComponent< FloatType >>, List< FilteredComponent< FloatType >>> > getAllRightNeighborsAt(
			final int t ) {
		return getRightNeighborsOf( getSegmentsAt( t ) );
	}
}
