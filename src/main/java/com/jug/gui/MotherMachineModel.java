/**
 *
 */
package com.jug.gui;

import com.jug.GrowthLine;
import com.jug.GrowthLineFrame;
import com.jug.MotherMachine;


/**
 * This class wraps an MotherMachine-instance and makes a GUI model out of it.
 *
 * @author jug
 */
public class MotherMachineModel {

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	/**
	 * The MotherMachine instance we wrap here
	 */
	protected MotherMachine mm;

	private int currentGLidx;
	private int currentGLFidx;

	// -------------------------------------------------------------------------------------
	// construction
	// -------------------------------------------------------------------------------------
	/**
	 * Construction
	 *
	 * @param mm
	 *            the instance of MotherMachine to be wrapped by this GUI model
	 */
	public MotherMachineModel( final MotherMachine mm ) {
		this.mm = mm;
		currentGLidx = 0;
		currentGLFidx = 0;
	}

	// -------------------------------------------------------------------------------------
	// getters and setters
	// -------------------------------------------------------------------------------------
	public GrowthLine getCurrentGL() {
		return mm.getGrowthLines().get( currentGLidx );
	}

	public void setCurrentGL( final int idx ) {
		setCurrentGL( idx, 0 );
	}

	public void setCurrentGL( final int idx, final int glfIdx ) {
		assert ( idx >= 0 );
		assert ( idx < mm.getGrowthLines().size() );
		currentGLidx = idx;
		currentGLFidx = glfIdx;
	}

	public GrowthLineFrame getCurrentGLF() {
		return getCurrentGL().get( currentGLFidx );
	}
	public GrowthLineFrame getCurrentGLFsPredecessor() {
		if ( currentGLFidx - 1 >= 0 ) {
			return getCurrentGL().get( currentGLFidx - 1 );
		} else {
			return null;
		}
	}
	public GrowthLineFrame getCurrentGLFsSuccessor() {
		if ( currentGLFidx + 1 < getCurrentGL().size() ) {
			return getCurrentGL().get( currentGLFidx + 1 );
		} else {
			return null;
		}
	}

	public void setCurrentGLF( final int idx ) {
		assert ( idx >= 0 );
		assert ( idx <= getCurrentGL().size() );
		currentGLFidx = idx;
	}

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	public GrowthLine switchToNextGL() {
		currentGLidx++;
		if ( currentGLidx >= mm.getGrowthLines().size() ) {
			currentGLidx = 0;
		}
		return getCurrentGL();
	}

	public GrowthLine switchToPrevGL() {
		currentGLidx--;
		if ( currentGLidx < 0 ) {
			currentGLidx = mm.getGrowthLines().size() - 1;
		}
		return getCurrentGL();
	}

	public GrowthLineFrame switchToNextGLF() {
		currentGLFidx++;
		if ( currentGLFidx >= getCurrentGL().size() ) {
			currentGLidx = 0;
		}
		return getCurrentGLF();
	}

	public GrowthLineFrame switchToPrevGLF() {
		currentGLFidx--;
		if ( currentGLFidx < 0 ) {
			currentGLFidx = getCurrentGL().size() - 1;
		}
		return getCurrentGLF();
	}

	/**
	 * @return the time-point of the current GLF within the current GL.
	 */
	public int getCurrentTime() {
		return getCurrentGL().getFrames().indexOf( getCurrentGLF() );
	}
}
