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

import com.jug.MoMA;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.lp.costs.CostManager;
import com.jug.sbmrm.zeromq.SbmrmClient;

import gurobi.GRB;
import gurobi.GRBException;
import gurobi.GRBModel;
import gurobi.GRBVar;

/**
 * @author jug
 */
public class MMTrainer implements Runnable {

	private final MoMA mm;
	private final GRBModel model;
	private final GrowthLineTrackingILP ilp;

	private Map< GRBVar, Boolean > assmntGT;
	private Map< GRBVar, Boolean > assmnt;

	private SbmrmClient sbmrm;

	double[] params; // the weights to optimize

	private final JTextArea console;

	public MMTrainer( final MoMA mm, final JTextArea console ) {
		this.mm = mm;
		this.ilp = mm.getGui().model.getCurrentGL().getIlp();
		this.model = ilp.model;
		params = ilp.getCostManager().getWeights();
		sbmrm = null;
		this.console = console;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		this.assmnt = new HashMap< GRBVar, Boolean >();
		this.assmntGT = new HashMap< GRBVar, Boolean >();

		log( "Reading and storing GT assignment..." );
		buildAssmnt( assmntGT );

		log( "Removing leveraged editing constraints..." );
		for ( int t = 0; t < MoMA.getMaxTime(); t++ ) {
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
		log( "Updating parameters..." );

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
		log( "\tResolving Loss Augmented Problem..." );
		ilp.run();

		log( "\tReading and storing current assignment..." );
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

	/**
	 * @param iteration
	 *            number of iteration that starts now.
	 * @param newParams
	 *            parameter values to test now.
	 * @param eps
	 *            current gap.
	 */
	public void setStatus( final int iteration, final double[] newParams, final double eps ) {
		log(
				String.format(
						"Initiating iteration %d with following stats:\n\tx     = %s\n\teps   = %.7f",
						iteration,
						Arrays.toString( params ),
						eps ) );
	}

	/**
	 * @param finalParams
	 *            final parameters.
	 * @param value
	 *            final value.
	 * @param eps
	 *            final gap.
	 */
	public void setFinalParameters( final double[] finalParams, final double value, final double eps, final String status ) {
		log(
				String.format(
						"Optimization successfully terminated!\n\tfinal_x = %s\n\tvalue   = %.7f\n\teps     = %.7f\n\tstatus  = %s",
						Arrays.toString( finalParams ),
						value,
						eps,
						status ) );

		// set final results!
		this.params = finalParams;

		// update cost manager
		final CostManager cm = ilp.getCostManager();
		cm.setWeights( params );

		// remove LAP costs from objective coefficients
		final GRBVar[] vars = model.getVars();
		for ( final GRBVar var : vars ) {
			final double newCost = cm.getCurrentCost( var );
			try {
				var.set( GRB.DoubleAttr.Obj, newCost );
			} catch ( final GRBException e ) {
				e.printStackTrace();
			}
		}

		// solve
		log( "\tComputing MAP solution using new parameters..." );
		ilp.run();

		log( "\tEND" );
	}

}
