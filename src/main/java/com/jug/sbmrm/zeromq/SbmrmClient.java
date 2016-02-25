/**
 *
 */
package com.jug.sbmrm.zeromq;

import java.util.Arrays;

import org.zeromq.ZMQ;

import com.jug.sbmrm.MMTrainer;
import com.jug.sbmrm.zeromq.TypedJsonBytes.TypedObject;
import com.jug.sbmrm.zeromq.protocol.ContinuationRequest;
import com.jug.sbmrm.zeromq.protocol.EvaluateResponse;
import com.jug.sbmrm.zeromq.protocol.FinalResponse;
import com.jug.sbmrm.zeromq.protocol.InitialRequest;
import com.jug.sbmrm.zeromq.protocol.SbmrmMessageTypes;

/**
 * @author jug
 */
public class SbmrmClient implements Runnable {

	private final MMTrainer trainer;

	public static void main( final String[] args ) {
		final SbmrmClient client = new SbmrmClient( null );
		client.run();
	}

	public SbmrmClient( final MMTrainer trainer ) {
		this.trainer = trainer;
	}

	@Override
	public void run() {
		int iterationCounter = 0;
		final ZMQ.Context context = ZMQ.context( 1 );

		//  Socket to talk to server
		System.out.println( "Connecting to SBMRM server..." );

		final ZMQ.Socket requester = context.socket( ZMQ.REQ );
		requester.connect( "tcp://10.1.202.26:4711" );

		final TypedJsonBytes json = new TypedJsonBytes( new SbmrmMessageTypes() );

		InitialRequest ir = null;
		if ( trainer != null ) {
			if ( trainer.getParams() != null ) {
				ir = new InitialRequest( trainer.getParams() );
			} else {
				ir = new InitialRequest( trainer.getDimensionality() );
			}
		} else {
			// without trainer being set I assume you simply want to test
			// the communication to the server, right?
			ir = new InitialRequest( 100 );
		}
		requester.send( json.toJson( ir ), 0 );

		final double[] finalX = new double[ 1 ];
		a: while ( true ) {
			final byte[] reply = requester.recv( 0 );
			final TypedObject to = json.fromJson( reply );

			switch ( to.type() ) {
			case SbmrmMessageTypes.EVALUATE_P_RESPONSE:
				iterationCounter++;
				final EvaluateResponse epr = ( EvaluateResponse ) to.object();
				final double[] params_p = epr.getX();
				System.out.println( String.format( "current x: %s", Arrays.toString( params_p ) ) );

				ContinuationRequest cr1 = null;
				if ( trainer != null ) {
					trainer.setStatus( iterationCounter, epr.getX(), epr.getEps() );
					trainer.updateParametrization( params_p );
					cr1 = new ContinuationRequest( trainer.getValue(), trainer.getGradient() );
				} else {
					// without trainer being set I assume you simply want to test
					// the communication to the server, right?
					cr1 = new ContinuationRequest( 0.0, new double[ params_p.length ] );
				}
				requester.send( json.toJson( cr1 ), 0 );
				break;
			case SbmrmMessageTypes.EVALUATE_R_RESPONSE: // so far just 0 + horizontal gradient
				final EvaluateResponse err = ( EvaluateResponse ) to.object();
				final double[] params_r = err.getX();
				System.out.println( String.format( "current x: %s", Arrays.toString( params_r ) ) );

				ContinuationRequest cr2 = null;
				cr2 = new ContinuationRequest( 0.0, new double[ params_r.length ] );
				requester.send( json.toJson( cr2 ), 0 );
				break;
			case SbmrmMessageTypes.FINAL_RESPONSE:
				final FinalResponse finalResponse = ( FinalResponse ) to.object();
				if ( trainer != null ) {
					trainer.setFinalParameters(
							finalResponse.getFinalX(),
							finalResponse.getValue(),
							finalResponse.getEps(),
							finalResponse.getStatus() );
				}
				break a;
			default:
				throw new IllegalArgumentException( "Received illegal message type!" );
			}
		}

		System.out.println( String.format( "final x: %s", Arrays.toString( finalX ) ) );

		requester.close();
		context.term();
	}

}
