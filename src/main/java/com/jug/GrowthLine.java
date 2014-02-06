/**
 *
 */
package com.jug;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.DoubleType;

import com.jug.lp.AbstractAssignment;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.lp.Hypothesis;

/**
 * @author jug
 */
public class GrowthLine {

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private final List< GrowthLineFrame > frames;
	private GrowthLineTrackingILP ilp; //<

	// Hypothesis< Component< DoubleType, ? > >,
	// AbstractAssignment< Hypothesis< Component< DoubleType, ? > > > > ilp;

	// -------------------------------------------------------------------------------------
	// setters and getters
	// -------------------------------------------------------------------------------------
	/**
	 * @return the frames
	 */
	public List< GrowthLineFrame > getFrames() {
		return frames;
	}

	/**
	 * @return the ILP
	 */
	public GrowthLineTrackingILP getIlp() {
		return ilp;
	}

	// -------------------------------------------------------------------------------------
	// constructors
	// -------------------------------------------------------------------------------------
	public GrowthLine() {
		this.frames = new ArrayList< GrowthLineFrame >();
	}

	public GrowthLine( final List< GrowthLineFrame > frames ) {
		this.frames = frames;
	}

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * @return the number of frames (time-steps) in this <code>GrowthLine</code>
	 */
	public int size() {
		return frames.size();
	}

	/**
	 * @param frame
	 *            the GrowthLineFrame to be appended as last frame
	 * @return true, if add was successful.
	 */
	public boolean add( final GrowthLineFrame frame ) {
		frame.setParent( this );
		return frames.add( frame );
	}

	/**
	 * @param frame
	 *            the GrowthLineFrame to be prepended as first frame
	 * @return true, if add was successful.
	 */
	public void prepand( final GrowthLineFrame frame ) {
		frame.setParent( this );
		frames.add( 0, frame );
	}

	/**
	 * @param f
	 * @return
	 */
	public GrowthLineFrame get( final int i ) {
		return this.getFrames().get( i );
	}

	/**
	 * Builds up the ILP used to find the MAP-mapping.
	 */
	public void generateILP() {
		ilp = new GrowthLineTrackingILP( this );
	}

	/**
	 * Runs the ILP.
	 */
	public void runILP() {
		getIlp().run();
	}

	/**
	 * @return a <code>Vector<String></code> object containing the summary of
	 *         divisions and exits for this GL. This data is eventually exported
	 *         to a CSV-file.
	 */
	public Vector< String > getDataVector() {
		final Vector<String> dataVector = new Vector<String>();

		int sumOfCells = 0;
		if ( getIlp() != null ) {

			// collect data
			for ( final GrowthLineFrame glf : getFrames() ) {

				int cells = 0;
				int exits = 0;
				int divisions = 0;

				for ( final Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > set : getIlp().getOptimalRightAssignments( glf.getTime() ).values() ) {
					for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> ora : set ) {
						cells++;
						if ( ora.getType() == GrowthLineTrackingILP.ASSIGNMENT_DIVISION )
							divisions++;
						if ( ora.getType() == GrowthLineTrackingILP.ASSIGNMENT_EXIT )
							exits++;
					}
				}
				if ( sumOfCells == 0 ) {
					sumOfCells = cells;
				} else {
					sumOfCells += divisions;
				}

				dataVector.add( ""+sumOfCells );
			}
		} else {
			for ( final GrowthLineFrame glf : getFrames() ) {
				dataVector.add( "?" );
			}
		}

		return dataVector;
	}

}
