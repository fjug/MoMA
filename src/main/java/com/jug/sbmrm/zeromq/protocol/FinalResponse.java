package com.jug.sbmrm.zeromq.protocol;

public class FinalResponse
{

	double[] x; // next weight vector to play with

	double value; // latest value of solver

	double eps; // latest eps of solver

	String status; // status of solver: "reached_min_eps", "reached_max_steps", "error"

	public FinalResponse( final double[] x, final double value, final double eps, final String status ) {
		this.x = x;
		this.value = value;
		this.eps = eps;
		this.status = status;
	}

	/**
	 * @return
	 */
	public double[] getFinalX() {
		return x;
	}

	public double getValue() {
		return value;
	}

	public double getEps() {
		return eps;
	}

	public String getStatus() {
		return status;
	}

}
