/**
 *
 */
package com.jug.sbmrm;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTextArea;

import com.jug.MotherMachine;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.lp.costs.CostManager;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;

/**
 * @author jug
 */
public class MMTrainer {

	private final MotherMachine mm;
	private final GRBModel model;
	private final GrowthLineTrackingILP ilp;

	private Map< GRBVar, Boolean > assmntGT;
	private Map< GRBVar, Boolean > assmnt;

	private SbmrmClient sbmrm;

	double[] params; // the weights to optimize

	private JTextArea console;

	public MMTrainer( final MotherMachine mm ) {
		this.mm = mm;
		this.ilp = mm.getGui().model.getCurrentGL().getIlp();
		this.model = ilp.model;
		params = ilp.getCostManager().getWeights();
		sbmrm = null;
	}

	public void run( final JTextArea console ) {
		this.console = console;

		this.assmnt = new HashMap< GRBVar, Boolean >();
		this.assmntGT = new HashMap< GRBVar, Boolean >();

		log( "Reading and storing GT assignment..." );
		buildAssmnt( assmntGT );

		log( "Removing leveraged editing constraints..." );
		for ( int t = 0; t < MotherMachine.getMaxTime(); t++ ) {
			ilp.removeAllAssignmentConstraints( t );
			ilp.removeAllSegmentConstraints( t );
			ilp.removeSegmentsInFrameCountConstraint( t );
		}

		log( "Starting training..." );
		sbmrm = new SbmrmClient(this);
		sbmrm.run();
	}

	/**
	 * @param assmntGT2
	 */
	private void buildAssmnt( final Map< GRBVar, Boolean > assmnt ) {
		assmnt.clear();
		final GRBVar[] vars = model.getVars();
		for ( final GRBVar var : vars ) {
			try {
				assmnt.put( var, ( var.get( GRB.DoubleAttr.X ) > .5 ) ? true : false );
			} catch ( final GRBException e ) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Receives new parameters to try.
	 * Starts complete inference run with the updated parameters.
	 *
	 * @param params
	 */
	public void updateParametrization( final double[] params ) {
		log( "Updating parameters to " + Arrays.toString( params ) );

		// update cost manager
		final CostManager cm = ilp.getCostManager();
		cm.setWeights( params );

		// set new LAP costs as objective coefficients
		final GRBVar[] vars = model.getVars();
		for ( final GRBVar var : vars ) {
			final double newCost = cm.getCurrentCost( var ) + ( assmntGT.get( var ) ? 1 : -1 );
			try {
				var.set( GRB.DoubleAttr.Obj, newCost );
			} catch ( final GRBException e ) {
				e.printStackTrace();
			}
		}

		// solve
		log( " >> Resolving Loss Augmented Problem..." );
		ilp.run();

		log( " >> Reading and storing current assignment..." );
		buildAssmnt( assmnt );
	}

	/**
	 * @return the dimensionality of the learning task at hand.
	 *         (This is the number of parameters in our cost functions.)
	 */
	public int getDimensionality() {
		return params.length;
	}

	/**
	 * Returns the the maximized LAP-energy (Loss Augmented Problem Energy)
	 * the MMTrainer computed after the latest parameter update.
	 *
	 * @return
	 */
	public double getValue() {
		log( "Computing LAP energy value..." );

		final CostManager cm = ilp.getCostManager();
		double energy = 0;
		try {
			energy = -model.get( GRB.DoubleAttr.ObjVal );
		} catch ( final GRBException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// compute GT energy (to be added)
		final GRBVar[] vars = model.getVars();
		for ( final GRBVar var : vars ) {
			if ( assmntGT.get( var ) ) {
				energy += cm.getCurrentCost( var ); // E(y')
				energy -= 1; 						// |y'|
			}
		}
		return energy;
	}

	/**
	 * Returns the LAP-gradient the MMTrainer computed after the latest
	 * parameter update.
	 *
	 * @return
	 */
	public double[] getGradient() {
		log( "Computing gradient..." );

		final CostManager cm = ilp.getCostManager();
		final double[] gradient = new double[ cm.getDimensions() ];

		final GRBVar[] vars = model.getVars();
		for ( final GRBVar var : vars ) {
			final boolean valGT = assmntGT.get( var );
			final boolean val = assmnt.get( var );
			if ( val != valGT ) {
				for ( int i = 0; i < cm.getDimensions(); ++i ) {
					gradient[ i ] += cm.getRow( var )[ i ] * ( ( valGT ? 1 : 0 ) - ( val ? 1 : 0 ) );
				}
			}
		}

		return gradient;
	}

	/**
	 * Logs the given message to the set console (JTextArea) or to STDOUT if
	 * console is null.
	 *
	 * @param message
	 */
	private void log( final String message ) {
		final String str = now() + message + "\n";
		if ( this.console != null ) {
			console.append( str );
		} else {
			System.out.println( str );
		}
	}

	/**
	 * @return
	 */
	private String now() {
		final SimpleDateFormat sdfDate = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss >> " );
		final Date now = new Date();
		final String strDate = sdfDate.format( now );
		return strDate;
	}

	/**
	 * @return
	 */
	public double[] getParams() {
		return this.params;
	}

}
