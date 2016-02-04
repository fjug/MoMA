/**
 *
 */
package com.jug.sbmrm;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.swing.JTextArea;

import com.jug.MotherMachine;

/**
 * @author jug
 */
public class MMTrainer {

	private final MotherMachine mm;
	private SbmrmClient sbmrm;

	double[] params; // the weights to optimize

	private JTextArea console;

	public MMTrainer( final MotherMachine mm ) {
		this.mm = mm;
		params = mm.getCostFunctionWeights();
		sbmrm = null;
	}

	public void run( final JTextArea console ) {
		this.console = console;
		log( "Starting training..." );
		sbmrm = new SbmrmClient(this);
		sbmrm.run();
	}

	/**
	 * Receives new parameters to try.
	 * Starts complete inference run with the updated parameters.
	 *
	 * @param params
	 */
	public void updateParametrization( final double[] params ) {
		log( "Updating parameters to " + Arrays.toString( params ) );

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
		return 0;
	}

	/**
	 * Returns the LAP-gradient the MMTrainer computed after the latest
	 * parameter update.
	 *
	 * @return
	 */
	public double[] getGradient() {
		return null;
	}

	/**
	 * Logs the given message to the set console (JTextArea) or to STDOUT if
	 * console is null.
	 *
	 * @param message
	 */
	private void log( final String message ) {
		final String str = now() + message;
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

}
