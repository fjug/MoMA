/**
 *
 */
package com.jug.lp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;

/**
 * @author jug
 */
public class FactorGraphFileBuilder_PAUL {

	int next_t = 0;
	int next_hyp_id = 0;

	List< String > lines = new ArrayList< String >();

	final HashMap< Hypothesis< Component< FloatType, ? > >, Integer > mapHypId;

	public FactorGraphFileBuilder_PAUL() {
		mapHypId = new HashMap< Hypothesis< Component< FloatType, ? > >, Integer >();
		lines.add( "# EXPORTED MM-TRACKING (jug@mpi-cbg.de)\n" );
		lines.add( "# SEGMENTS" );
		lines.add( "# Note: ids must be given such that hypotheses are ordered from top to bottom" );
		lines.add( "# (in order to implicitly know about exit constraints)\n" );
	}

	/**
	 * writes a time-point tag into <code>lines</code>.
	 */
	public void markNextTimepoint() {
		if ( next_hyp_id == 0 ) {
			lines.add( "# #### SEGMENTS (HYPOTHESES) ###################################" );
		}

		lines.add( "\nt=" + next_t + "\n" );
		next_t++;
	}

	/**
	 * Adds a hypotheses.
	 *
	 * @param hyp
	 *
	 * @return the id of the added hypothesis.
	 */
	public int addHyp( final Hypothesis< Component< FloatType, ? > > hyp ) {
		mapHypId.put( hyp, next_hyp_id );
		lines.add( String.format( "H %d %f (%d,%d)", next_hyp_id, hyp.getCosts(), hyp.getLocation().a, hyp.getLocation().b ) );
		next_hyp_id++;
		return next_hyp_id - 1;
	}

	/**
	 * Adds an exclusion constraint (of hyps that cannot be turned on
	 * simultaneously)
	 *
	 * @param hyps
	 */
	public void addExclusionConstraint( final List< Hypothesis< Component< FloatType, ? > > > hyps ) {
		String str = "EC ";
		boolean first = true;
		for ( final Hypothesis< Component< FloatType, ? > > hyp : hyps ) {
			if ( first ) {
				first = false;
			} else {
				str += " + ";
			}
			str += "" + mapHypId.get( hyp );
		}
		str += " <= 1";
		lines.add( str );
	}

	/**
	 * @param file
	 */
	public void write( final File file ) {
		BufferedWriter out;
		try {
			out = new BufferedWriter( new FileWriter( file ) );

			for ( final String line : lines ) {
				out.write( line );
				out.newLine();
			}
			out.close();
		}
		catch ( final IOException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * @param line
	 */
	public void addLine( final String line ) {
		lines.add( line );
	}

	/**
	 * @param ilp
	 * @param t
	 * @param assmnt
	 */
	public void addMapping( final GrowthLineTrackingILP ilp, final int t, final MappingAssignment assmnt ) {
		final Hypothesis< Component< FloatType, ? > > sourceHypothesis = assmnt.getSourceHypothesis();
		final Hypothesis< Component< FloatType, ? > > destinationHypothesis = assmnt.getDestinationHypothesis();
		final Pair< Float, float[] > cost = ilp.compatibilityCostOfMapping( sourceHypothesis, destinationHypothesis );
		lines.add( String.format( "MA %d %d %d %f", t, mapHypId.get( sourceHypothesis ), mapHypId.get( destinationHypothesis ), cost.getA() ) );
	}

	/**
	 * @param ilp
	 * @param t
	 * @param assmnt
	 */
	public void addDivision( final GrowthLineTrackingILP ilp, final int t, final DivisionAssignment assmnt ) {
		final Hypothesis< Component< FloatType, ? > > sourceHypothesis = assmnt.getSourceHypothesis();
		final Hypothesis< Component< FloatType, ? > > destinationHypothesisUpper = assmnt.getUpperDesinationHypothesis();
		final Hypothesis< Component< FloatType, ? > > destinationHypothesisLower = assmnt.getLowerDesinationHypothesis();
		final Pair< Float, float[] > cost =
				ilp.compatibilityCostOfDivision( sourceHypothesis, destinationHypothesisUpper, destinationHypothesisLower );
		lines.add(
				String.format(
						"DA %d %d %d %d %f",
						t,
						mapHypId.get( sourceHypothesis ),
						mapHypId.get( destinationHypothesisUpper ),
						mapHypId.get( destinationHypothesisLower ),
						cost.getA() ) );
	}
}
