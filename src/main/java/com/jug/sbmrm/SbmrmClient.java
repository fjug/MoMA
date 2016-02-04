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

	private final MMTrainer trainer;

	public static void main( final String[] args ) {
		final SbmrmClient client = new SbmrmClient( null );
		client.run();
	}

	public SbmrmClient( final MMTrainer trainer ) {
		this.trainer = trainer;
	}

	public void run() {
		final ZMQ.Context context = ZMQ.context( 1 );

		//  Socket to talk to server
		System.out.println( "Connecting to SBMRM server..." );

		final ZMQ.Socket requester = context.socket( ZMQ.REQ );
		requester.connect( "tcp://192.168.1.162:4711" );

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

		double[] finalX;
		a: while ( true ) {
			final byte[] reply = requester.recv( 0 );
			final TypedObject to = json.fromJson( reply );

			switch ( to.type() ) {
			case SbmrmMessageTypes.EVALUATE_RESPONSE:
				final EvaluateResponse qr = ( EvaluateResponse ) to.object();
				final double[] params = qr.getX();
				System.out.println( String.format( "current x: %s", Arrays.toString( params ) ) );

				ContinuationRequest cr = null;
				if ( trainer != null ) {
					trainer.updateParametrization( params );
					cr = new ContinuationRequest( trainer.getValue(), trainer.getGradient() );
				} else {
					// without trainer being set I assume you simply want to test
					// the communication to the server, right?
					cr = new ContinuationRequest( 0.0, new double[ params.length ] );
				}
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
