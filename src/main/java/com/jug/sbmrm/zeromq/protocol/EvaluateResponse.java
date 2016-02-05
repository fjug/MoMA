package com.jug.sbmrm.zeromq.protocol;

public class EvaluateResponse
{

	double[] x; // next weight vector to play with

	double eps; // latest eps of solver

	public EvaluateResponse( final double[] x, final double eps ) {
		this.x = x;
		this.eps = eps;
	}

	public double[] getX() {
		return x;
	}

	public double getEps() {
		return eps;
	}
}
