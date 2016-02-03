package com.jug.sbmrm;

public class InitialRequest
{

	private final int dims; // number of features

	private final double[] initialX; // initial parameter values

	public InitialRequest( final int dims ) {
		this.dims = dims;
		this.initialX = null;
	}

	public InitialRequest( final double[] initialX ) {
		this.dims = initialX.length;
		this.initialX = initialX;
	}

	public double[] getInitialX() {
		return initialX;
	}

	public int getNumDimensions() {
		return dims;
	}
}
