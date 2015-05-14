/**
 *
 */
package com.jug.fg;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;

import net.imglib2.algorithm.componenttree.ComponentForest;
import net.imglib2.type.numeric.real.FloatType;

import com.indago.fg.FactorGraph;
import com.indago.fg.domain.BooleanFunctionDomain;
import com.indago.fg.factor.BooleanFactor;
import com.indago.fg.factor.Factor;
import com.indago.fg.function.BooleanAssignmentConstraint;
import com.indago.fg.function.BooleanTensorTable;
import com.indago.fg.function.Function;
import com.indago.fg.gui.FgPanel;
import com.indago.fg.variable.AssignmentVariable;
import com.indago.fg.variable.BooleanVariable;
import com.indago.fg.variable.ComponentVariable;
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

	final HashMap< FilteredComponent< FloatType >, HashMap< FilteredComponent< FloatType >, AssignmentVariable< FilteredComponent< FloatType > >> > rightNeighbors =
			new HashMap<>();
	final HashMap< FilteredComponent< FloatType >, HashMap< FilteredComponent< FloatType >, AssignmentVariable< FilteredComponent< FloatType > >> > leftNeighbors =
			new HashMap<>();

	/**
	 * @param growthLine
	 */
	public TimmFgImpl( final GrowthLine gl ) {

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

		// Mappings & Divisions
		// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
		for ( int t = 0; t < gl.size(); t++ ) {
			int tp1 = t + 1;
			if ( tp1 == gl.size() ) tp1 = t; // We double the last GLF to avoid border effects! This is not stupid! ;)

			final GrowthLineFrame glfT = gl.get( t );
			final GrowthLineFrame glfTp1 = gl.get( tp1 );

			addMappings( glfT, glfTp1 );
			addDivisions( glfT, glfTp1 );
			addExits( glfT );
		}

		// Divisions
		// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

		// Exits
		// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

		show();
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
		addUnaryTo( ctNode, newVar );

		for ( final FilteredComponent< FloatType > child : ctNode.getChildren() ) {
			addSegmentBranch( child, gapSepFkt );
		}
	}

	/**
	 * @param ctRoot
	 */
	private void addTreePathConstraints( final FilteredComponent< FloatType > ctRoot ) {
		// TODO Find all leaves -- then go upwards to parent and collect for monster-constraints
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
			final ComponentVariable< FilteredComponent< FloatType >> newVar ) {
		final double[] entries =
				new double[] { 0.0, newVar.getAnnotatedCost() };
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
				new AssignmentVariable< FilteredComponent< FloatType > >( ctNodeT );
		varMappings.add( newVar );

		// --- add new variable into left- and right neighborhoods ------------------------------------------
		HashMap< FilteredComponent< FloatType >, AssignmentVariable< FilteredComponent< FloatType >>> ln =
				leftNeighbors.get( ctNodeTp1 );
		if ( ln == null ) {
			ln = new HashMap<>();
			leftNeighbors.put( ctNodeTp1, ln );
		}
		ln.put( ctNodeTp1, newVar );

		HashMap< FilteredComponent< FloatType >, AssignmentVariable< FilteredComponent< FloatType >>> rn =
				rightNeighbors.get( ctNodeT );
		if ( rn == null ) {
			rn = new HashMap<>();
			rightNeighbors.put( ctNodeT, rn );
		}
		rn.put( ctNodeT, newVar );

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
				new AssignmentVariable< FilteredComponent< FloatType > >( ctNodeT );
		varDivisions.add( newVar );

		// --- add new variable into left- and right neighborhoods ------------------------------------------
		HashMap< FilteredComponent< FloatType >, AssignmentVariable< FilteredComponent< FloatType >>> ln =
				leftNeighbors.get( ctNodeTp1_upper );
		if ( ln == null ) {
			ln = new HashMap<>();
			leftNeighbors.put( ctNodeTp1_upper, ln );
		}
		ln.put( ctNodeTp1_upper, newVar );

		ln = leftNeighbors.get( ctNodeTp1_lower );
		if ( ln == null ) {
			ln = new HashMap<>();
			leftNeighbors.put( ctNodeTp1_lower, ln );
		}
		ln.put( ctNodeTp1_lower, newVar );

		HashMap< FilteredComponent< FloatType >, AssignmentVariable< FilteredComponent< FloatType >>> rn =
				rightNeighbors.get( ctNodeT );
		if ( rn == null ) {
			rn = new HashMap<>();
			rightNeighbors.put( ctNodeT, rn );
		}
		rn.put( ctNodeT, newVar );

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

				final double cost = Math.min( 0.0f, fromCost / 2.0f ); // NOTE: 0 or negative but only hyp/2 to prefer map or div if exists...
				addExitTo( node, cost );

				for ( final FilteredComponent< FloatType > child : node.getChildren() ) {
					queue.push( child );
				}
			}
		}
	}

	/**
	 * @param node
	 * @param cost
	 */
	private void addExitTo( final FilteredComponent< FloatType > ctNode, final double cost ) {
		// --- add new variable -----------------------------------------------------------------------------
		final AssignmentVariable< FilteredComponent< FloatType >> newVar =
				new AssignmentVariable< FilteredComponent< FloatType > >( ctNode );
		varExits.add( newVar );

		// --- add new variable into right neighborhood -----------------------------------------------------
		HashMap< FilteredComponent< FloatType >, AssignmentVariable< FilteredComponent< FloatType >>> rn =
				rightNeighbors.get( ctNode );
		if ( rn == null ) {
			rn = new HashMap<>();
			rightNeighbors.put( ctNode, rn );
		}
		rn.put( ctNode, newVar );

		// --- connect variables by a constraint factor -----------------------------------------------------
		//TODO missing exit constraints!!!

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
	 *
	 */
	public void show() {
		final Collection< BooleanVariable > vars = new ArrayList< BooleanVariable >();
		for ( final ComponentVariable< FilteredComponent< FloatType >> var : varSegHyps.values() ) {
			vars.add( var );
		}
//		for ( final AssignmentVariable< FilteredComponent< FloatType > > var : varMappings ) {
//			vars.add( var );
//		}
//		for ( final AssignmentVariable< FilteredComponent< FloatType > > var : varDivisions ) {
//			vars.add( var );
//		}
		for ( final AssignmentVariable< FilteredComponent< FloatType > > var : varExits ) {
			vars.add( var );
		}

		final FactorGraph fg = new FactorGraph( vars, factors, functions );

		final FgPanel panel = new FgPanel( fg );
		final JFrame frame = new JFrame( "TimmFgImpl - dump" );
		frame.setBounds( 50, 50, 1200, 900 );
		frame.getContentPane().setLayout( new BorderLayout() );
		frame.getContentPane().add( panel );
		frame.setVisible( true );
	}

}
