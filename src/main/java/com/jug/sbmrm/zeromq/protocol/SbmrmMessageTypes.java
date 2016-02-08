/**
 *
 */
package com.jug.sbmrm.zeromq.protocol;

/**
 * @author jug
 */
public class SbmrmMessageTypes extends AbstractMessageTypes {

	public static final int INITIAL_REQUEST = 0;
	public static final int CONTINUATION_REQUEST = 1;
	public static final int EVALUATE_P_RESPONSE = 2;
	public static final int EVALUATE_R_RESPONSE = 4;
	public static final int FINAL_RESPONSE = 3;

	public SbmrmMessageTypes() {
		super();
		put( INITIAL_REQUEST, InitialRequest.class );
		put( CONTINUATION_REQUEST, ContinuationRequest.class );
		put( EVALUATE_P_RESPONSE, EvaluateResponse.class );
		put( EVALUATE_R_RESPONSE, EvaluateResponse.class );
		put( FINAL_RESPONSE, FinalResponse.class );
	}
}
