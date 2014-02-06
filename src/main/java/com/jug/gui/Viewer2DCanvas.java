/**
 *
 */
package com.jug.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.converter.RealARGBConverter;
import net.imglib2.display.projector.Projector2D;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;

import com.jug.GrowthLineFrame;
import com.jug.lp.Hypothesis;


/**
 * @author jug
 */
public class Viewer2DCanvas extends JComponent implements MouseInputListener {

	private static final long serialVersionUID = 8284204775277266994L;

	private final int w;
	private final int h;
	private Projector2D projector;
	private ARGBScreenImage screenImage;
	private IntervalView< DoubleType > view;
	private GrowthLineFrame glf;

	// tracking the mouse (when over)
	private boolean isMouseOver;
	private int mousePosX;
	private int mousePosY;

	// tracking the mouse (when dragging)
	private boolean isDragging;
	private int dragX;
	private int dragY;

	public Viewer2DCanvas( final int w, final int h ) {
		super();

		addMouseListener( this );
		addMouseMotionListener( this );

		this.w = w;
		this.h = h;
		setPreferredSize( new Dimension( w, h ) );
		this.screenImage = new ARGBScreenImage( w, h );
		this.projector = null;
		this.view = null;
		this.glf = null;
	}

	/**
	 * Sets the image data to be displayed when paintComponent is called.
	 *
	 * @param glf
	 *            the GrowthLineFrameto be displayed
	 * @param viewImg
	 *            an IntervalView<DoubleType> containing the desired view
	 *            onto the raw image data
	 */
	public void setScreenImage( final GrowthLineFrame glf, final IntervalView< DoubleType > viewImg ) {
		setEmptyScreenImage();
		this.projector = new Projector2D< DoubleType, ARGBType >( 0, 1, viewImg, screenImage, new RealARGBConverter< DoubleType >( 0, 1 ) );
		this.view = viewImg;
		this.glf = glf;
		this.repaint();
	}

	/**
	 * Prepares to display an empty image.
	 */
	public void setEmptyScreenImage() {
		screenImage = new ARGBScreenImage( w, h );
		this.projector = null;
		this.view = null;
		this.glf = null;
	}


	@Override
	public void paintComponent( final Graphics g ) {
		try {
			if ( projector != null ) {
				projector.map();
			}
			glf.drawCenterLine( screenImage, view );
			//TODO NOT nice... do something against that, please!
			final int t = glf.getParent().getFrames().indexOf( glf );
			glf.drawOptimalSegmentation( screenImage, view, glf.getParent().getIlp().getOptimalSegmentation( t ) );
		}
		catch ( final ArrayIndexOutOfBoundsException e ) {
			// this can happen if a growth line, due to shift, exists in one
			// frame, and does not exist in others.
			// If for this growth line we want to visualize a time where the
			// GrowthLine is empty, the projector
			// throws a ArrayIndexOutOfBoundsException that I catch
			// hereby... ;)
			System.err.println( "ArrayIndexOutOfBoundsException in paintComponent of MMGUI!" );
			// e.printStackTrace();
		}
		catch ( final NullPointerException e ) {
			// System.err.println( "View or glf not yet set in MotherMachineGui!" );
			// e.printStackTrace();
		}
		g.drawImage( screenImage.image(), 0, 0, w, h, null );

		// Mouse-position related stuff...
		if ( !this.isDragging && this.isMouseOver && glf != null && glf.getParent().getIlp() != null ) {
			double cost = Double.NaN;
			//TODO NOT nice... do something against that, please!
			final int t = glf.getTime();
			final Hypothesis< Component< DoubleType, ? >> hyp = glf.getParent().getIlp().getOptimalSegmentationAtLocation( t, this.mousePosY );
			if ( hyp != null ) {
				cost = hyp.getCosts();
			}
			g.setColor( Color.DARK_GRAY );
			g.drawString( String.format( "c=%.4f", cost ), 2, this.mousePosY - 9 );
			g.setColor( Color.YELLOW.brighter() );
			g.drawString( String.format( "c=%.4f", cost ), 1, this.mousePosY - 10 );
		}
	}

	// -------------------------------------------------------------------------------------
	// MouseInputListener related methods
	// -------------------------------------------------------------------------------------

	/**
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseClicked( final MouseEvent e ) {
	}

	/**
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	@Override
	public void mousePressed( final MouseEvent e ) {
	}

	/**
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseEntered( final MouseEvent e ) {
		this.isMouseOver = true;
	}

	/**
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseExited( final MouseEvent e ) {
		this.isMouseOver = false;
		this.repaint();
	}

	/**
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseDragged( final MouseEvent e ) {
		if ( e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3 ) {
			this.isDragging = true;
			this.dragX = e.getX();
			this.dragY = e.getY();
		}
		repaint();
	}

	/**
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseReleased( final MouseEvent e ) {
		this.isDragging = false;
		repaint();
	}

	/**
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseMoved( final MouseEvent e ) {
		this.mousePosX = e.getX();
		this.mousePosY = e.getY() - 42;
		this.repaint();
	}
}