package com.jug.sbmrm.zeromq.protocol;

public class ContinuationRequest
{

	private final double value;

	private final double[] gradient;

	public ContinuationRequest( final double value, final double[] gradient ) {
		this.value = value;
		this.gradient = gradient;
	}

	public double getValue() {
		return value;
	}

	public double[] getGradien() {
		return gradient;
	}
}
