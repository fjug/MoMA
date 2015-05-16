/**
 *
 */
package com.jug;

import java.util.ArrayList;
import java.util.List;

import com.indago.fg.Assignment;
import com.jug.fg.TimmFgImpl;
import com.jug.gui.progress.DialogProgress;

/**
 * @author jug
 */
public class GrowthLine {

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private final List< GrowthLineFrame > frames;
	private TimmFgImpl timmFg;
	private Assignment timmFgSolution;

	// Hypothesis< Component< FloatType, ? > >,
	// AbstractAssignment< Hypothesis< Component< FloatType, ? > > > > ilp;

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
	 * @param f
	 * @return
	 */
	public GrowthLineFrame get( final int i ) {
		return this.getFrames().get( i );
	}

	/**
	 * @return the timmFg
	 */
	public TimmFgImpl getTimmFg() {
		return timmFg;
	}

	/**
	 * @return the timmFgSolution
	 */
	public Assignment getTimmFgSolution() {
		return timmFgSolution;
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
	 * Generates the tracking FactorGraph for this GL.
	 *
	 * @param dialogProgress
	 * @return
	 */
	public TimmFgImpl generateFG( final DialogProgress dialogProgress ) {
		timmFg = new TimmFgImpl( this );
		return timmFg;
	}

	/**
	 * Solves the tracking built FactorGraph for this GL.
	 * If it was not previously built using <code>generateFG</code>, that method
	 * will be called here.
	 *
	 * @param dialogProgress
	 * @return
	 */
	public Assignment solveFG( final DialogProgress dialogProgress ) {
		if ( timmFg == null ) {
			generateFG( dialogProgress );
		}
		timmFgSolution = timmFg.solve();
		return timmFgSolution;
	}
}
