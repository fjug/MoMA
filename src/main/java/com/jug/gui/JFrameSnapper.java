package com.jug.gui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

import javax.swing.JFrame;

public class JFrameSnapper extends ComponentAdapter implements WindowListener {

	public final static int NORTH = 100;
	public final static int WEST = 101;
	public final static int EAST = 102;
	public final static int SOUTH = 103;
	public final static int NORTH_ONLY = 104;
	public final static int WEST_ONLY = 105;
	public final static int EAST_ONLY = 106;
	public final static int SOUTH_ONLY = 107;

	private final Vector< JFrame > frames;
	private JFrame currentlyMovingFrame;
	private int snappingDistance = 25;
	private boolean snappingPolicy = true;
	private Rectangle recOne, recTwo;

	private final HashMap< JFrame, Set< JFrame >> activeSnappings;
	private final HashMap< JFrame, Point > currentFramePositions;

	public JFrameSnapper() {
		frames = new Vector< JFrame >();
		activeSnappings = new HashMap< JFrame, Set< JFrame >>();
		currentFramePositions = new HashMap< JFrame, Point >();
	}

	public void addFrame( final JFrame frame ) {
		frame.addComponentListener( this );
		frame.addWindowListener( this );

		activeSnappings.put( frame, new HashSet< JFrame >() );
		currentFramePositions.put( frame, frame.getLocation() );

		frames.add( frame );
	}

	public void removeFrame( final JFrame frame ) {
		frame.removeComponentListener( this );
		frame.removeWindowListener( this );

		activeSnappings.remove( frame );
		currentFramePositions.remove( frame );

		frames.remove( frame );
	}

	public boolean getSnappingPolicy() {
		return snappingPolicy;
	}

	public void setSnappingPolicy( final boolean bool ) {
		snappingPolicy = bool;
	}

	public void setSnappingDistance( final int i ) {
		snappingDistance = i;
	}

	public int getSnappingDistance() {
		return snappingDistance;
	}

	@Override
	public void componentMoved( final ComponentEvent e ) {
//		System.out.println( "componentMoved -- " + ( ( JFrame ) e.getComponent() ).getName() );
		try {
			if ( currentlyMovingFrame == null ) return;

			if ( currentlyMovingFrame == e.getComponent() ) {
//				System.out.println( "  is currently moving" );
				final Point newPosition = e.getComponent().getLocation();
				final LinkedList<JFrame> fifo = new LinkedList<JFrame>(activeSnappings.get( currentlyMovingFrame ));
				int emergencyKill = 10;
				while ( emergencyKill > 0 && !fifo.isEmpty() ) {
					emergencyKill--;
					final JFrame snappedFrame = fifo.removeFirst();
					fifo.addAll( activeSnappings.get( snappedFrame ) );
					final Point p = snappedFrame.getLocation();
					final int dx = newPosition.x - currentFramePositions.get( currentlyMovingFrame ).x;
					final int dy = newPosition.y - currentFramePositions.get( currentlyMovingFrame ).y;
					snappedFrame.setLocation( p.x + dx, p.y + dy );
					currentFramePositions.put( snappedFrame, snappedFrame.getLocation() );
				}
				currentFramePositions.put( currentlyMovingFrame, newPosition );

				for ( final JFrame frame : frames ) {
					if ( frame != currentlyMovingFrame ) {
						// release current mover from being snapped elsewhere
						activeSnappings.get( frame ).remove( currentlyMovingFrame );

						// check for new snappings
						if ( touching( currentlyMovingFrame, frame ) && !activeSnappings.get( currentlyMovingFrame ).contains( frame ) ) {
							snapFrames( currentlyMovingFrame, frame );
						}
					}
				}
			}
		}
		catch ( final NullPointerException ex ) {
			//do nothing -- dunno why this happens every now and then...
		}
	}

	private boolean touching( final JFrame anchorFrame, final JFrame snappingFrame ) {
		recOne = new Rectangle( anchorFrame.getX(), anchorFrame.getY(), anchorFrame.getWidth(), anchorFrame.getHeight() );
		recTwo = new Rectangle( snappingFrame.getX(), snappingFrame.getY(), snappingFrame.getWidth(), snappingFrame.getHeight() );
//		recOne = anchorFrame.getBounds();
//		recTwo = snappingFrame.getBounds();

		return ( recTwo.intersects( recOne ) ) || ( touchingEdges( anchorFrame, snappingFrame ) );
	}

	private boolean touchingEdges( final JFrame anchorFrame, final JFrame snappingFrame ) {
		recOne = new Rectangle( anchorFrame.getX(), anchorFrame.getY(), anchorFrame.getWidth(), anchorFrame.getHeight() );
		recTwo = new Rectangle( snappingFrame.getX(), snappingFrame.getY(), snappingFrame.getWidth(), snappingFrame.getHeight() );
//		recOne = anchorFrame.getBounds();
//		recTwo = snappingFrame.getBounds();
		return ( recOne.intersects( recTwo ) );
	}

	public void snapFrames( final JFrame snappingFrame, final JFrame anchorFrame, final int edge ) {
		currentlyMovingFrame = null;

		switch ( edge ) {
		case NORTH_ONLY:
			if ( !getSnappingPolicy() ) break;
		case NORTH:
			snappingFrame.setLocation( anchorFrame.getX(), anchorFrame.getY() - anchorFrame.getHeight() );
			break;
		case SOUTH_ONLY:
			if ( !getSnappingPolicy() ) break;
		case SOUTH:
			snappingFrame.setLocation( anchorFrame.getX(), anchorFrame.getY() + anchorFrame.getHeight() );
			break;
		case WEST_ONLY:
			if ( !getSnappingPolicy() ) break;
		case WEST:
			snappingFrame.setLocation( anchorFrame.getX() - snappingFrame.getWidth(), anchorFrame.getY() );
			break;
		case EAST_ONLY:
			if ( !getSnappingPolicy() ) break;
		case EAST:
			snappingFrame.setLocation( anchorFrame.getX() + anchorFrame.getWidth(), anchorFrame.getY() );
			break;
		}

		activeSnappings.get( anchorFrame ).add( snappingFrame );
		anchorFrame.setVisible( true );
		anchorFrame.toFront();
		currentlyMovingFrame = anchorFrame;

		currentFramePositions.put( anchorFrame, anchorFrame.getLocation() );
//		currentFramePositions.put( snappingFrame, snappingFrame.getLocation() );
	}

	private void snapFrames( final JFrame snappingFrame, final JFrame anchorFrame ) {
		final int edge = whichEdgeIsTouching( snappingFrame, anchorFrame );
		snapFrames( snappingFrame, anchorFrame, edge );
	}

	/*
	 * figured out which side of the Moving Frame is hitting the non-moving
	 * frame.
	 */
	private int whichEdgeIsTouching( final JFrame frame1, final JFrame movingFrame ) {
		final int z1 = frame1.getX(), w1 = frame1.getY(), z2 = z1 + frame1.getWidth(), w2 = w1 + frame1.getHeight();
		final int x1 = movingFrame.getX(), y1 = movingFrame.getY(), x2 = x1 + movingFrame.getWidth(), y2 = y1 + movingFrame.getHeight();

		if ( Math.abs( z1 - x1 ) < this.snappingDistance && Math.abs( w2 - y1 ) < this.snappingDistance )
			return NORTH;
		if ( Math.abs( z1 - x1 ) < this.snappingDistance && Math.abs( w1 - y2 ) < this.snappingDistance )
			return SOUTH;
		if ( Math.abs( z2 - x1 ) < this.snappingDistance && Math.abs( w1 - y1 ) < this.snappingDistance )
			return WEST;
		if ( Math.abs( z1 - x2 ) < this.snappingDistance && Math.abs( w2 - y2 ) < this.snappingDistance )
			return EAST;

		if ( Math.abs( z2 - x1 ) < this.snappingDistance ) return WEST_ONLY;
		if ( Math.abs( z1 - x2 ) < this.snappingDistance ) return EAST_ONLY;
		if ( Math.abs( w2 - y1 ) < this.snappingDistance ) return NORTH_ONLY;
		if ( Math.abs( w1 - y2 ) < this.snappingDistance ) return SOUTH_ONLY;
		return -1;
	}

	@Override
	public void windowOpened( final WindowEvent e ) {}

	@Override
	public void windowClosing( final WindowEvent e ) {}

	@Override
	public void windowClosed( final WindowEvent e ) {}

	@Override
	public void windowIconified( final WindowEvent e ) {}

	@Override
	public void windowDeiconified( final WindowEvent e ) {}

	@Override
	public void windowActivated( final WindowEvent e ) {
//		System.out.println( "windowActivated -- " + ( ( JFrame ) e.getComponent() ).getName() );
		this.currentlyMovingFrame = ( JFrame ) e.getComponent();
	}

	@Override
	public void windowDeactivated( final WindowEvent e ) {}

	public static void main( final String args[] ) {
		final JFrameSnapper snapper = new JFrameSnapper();

		final JFrame frame = new JFrame( "frame 0" );
		frame.setSize( 100, 200 );
		frame.setLocation( 0, 100 );
		frame.setVisible( true );

		snapper.addFrame( frame );

		final JFrame frame2 = new JFrame( "frame 1" );
		frame2.setSize( 200, 100 );
		frame2.setLocation( 0, 0 );
		frame2.setVisible( true );
		snapper.addFrame( frame2 );

		final JFrame frame3 = new JFrame( "frame 2" );
		frame3.setSize( 200, 200 );
		frame3.setLocation( 200, 100 );
		frame3.setVisible( true );
		snapper.addFrame( frame3 );

		snapper.snapFrames( frame2, frame, JFrameSnapper.EAST );
	}
}