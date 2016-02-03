/**
 *
 */
package com.jug.sbmrm;

import java.util.Arrays;

import org.zeromq.ZMQ;

import com.jug.sbmrm.TypedJsonBytes.TypedObject;

/**
 * @author jug
 */
public class SbmrmClient {

	public static void main( final String[] args ) {
		final ZMQ.Context context = ZMQ.context( 1 );

		//  Socket to talk to server
		System.out.println( "Connecting to SBMRM server…" );

		final ZMQ.Socket requester = context.socket( ZMQ.REQ );
		requester.connect( "tcp://192.168.0.20:4711" );

		final TypedJsonBytes json = new TypedJsonBytes( new SbmrmMessageTypes() );

		final InitialRequest ir = new InitialRequest( 100 );
		requester.send( json.toJson( ir ), 0 );

		double[] finalX;
		a: while ( true ) {
			final byte[] reply = requester.recv( 0 );
			final TypedObject to = json.fromJson( reply );

			switch ( to.type() ) {
			case SbmrmMessageTypes.EVALUATE_RESPONSE:
				final EvaluateResponse qr = ( EvaluateResponse ) to.object();
				final double[] x = qr.getX();
				System.out.println( String.format( "current x: %s", Arrays.toString( x ) ) );
				final ContinuationRequest cr = new ContinuationRequest( 0.0, new double[ x.length ] );
				requester.send( json.toJson( cr ), 0 );
				break;
			case SbmrmMessageTypes.FINAL_RESPONSE:
				final FinalResponse finalResponse = ( FinalResponse ) to.object();
				finalX = finalResponse.getFinalX();
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
