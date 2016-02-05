package com.jug.sbmrm.zeromq.protocol;

public class InitialRequest
{

	private final int dims; // number of features

	private final double[] initial_x; // initial parameter values

	private final double lambda; // regularizer weight

	private final int steps; // number of iterations (0 == unlimited)

	private final double min_eps; // convergence threshold, meaning depends on eps_strategy

	private final String eps_strategy; // can be "eps_from_gap" or "eps_from_change"

	/**
	 * Builds an InitialRequest message.
	 * This will create an initial request message using the zero vector, a
	 * lambda of 1, an unlimited number of steps, 10^-1 as min_eps, and the
	 * "eps_from_gap" termination criterion.
	 *
	 * @param dims
	 *            length of the weights vector to use for optimization.
	 */
	public InitialRequest( final int dims ) {
		this( new double[ dims ], 1, 0, 0.0001, true );
	}

	/**
	 * Builds an InitialRequest message.
	 * This will create an initial request message using a lambda of 1, an
	 * unlimited number of steps, 10^-1 as min_eps, and the "eps_from_gap"
	 * termination criterion.
	 *
	 * @param initialX
	 *            the initial weight vector.
	 */
	public InitialRequest( final double[] initialX ) {
		this( initialX, 1, 0, 0.0001, true );
	}

	/**
	 * Builds an InitialRequest message.
	 *
	 * @param initialX
	 *            the initial weight vector.
	 * @param lambda
	 *            regularizer weight
	 * @param steps
	 *            number of iterations
	 * @param min_eps
	 *            convergence threshold, meaning depends on eps_strategy
	 * @param eps_from_gap
	 *            if true we use the "eps_from_gap", otherwise the
	 *            "eps_from_change" termination strategy.
	 */
	public InitialRequest( final double[] initialX, final double lambda, final int steps, final double min_eps, final boolean eps_from_gap ) {
		this.dims = initialX.length;
		this.initial_x = initialX;
		this.lambda = lambda;
		this.steps = steps;
		this.min_eps = min_eps;
		if ( eps_from_gap ) {
			this.eps_strategy = "eps_from_gap";
		} else {
			this.eps_strategy = "eps_from_change";
		}

	}

	public double[] getInitialX() {
		return initial_x;
	}

	public int getNumDimensions() {
		return dims;
	}
}
