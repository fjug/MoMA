/**
 *
 */
package com.jug.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.imglib2.Point;

/**
 * @author jug
 * 
 */
public class Util {

	/**
	 * <p>
	 * Create a long[] with the location of of an {@link Point}.
	 * </p>
	 * 
	 * <p>
	 * Keep in mind that creating arrays wildly is not good practice and
	 * consider using the point directly.
	 * </p>
	 * 
	 * @param point
	 * 
	 * @return location of the point as a new long[]
	 */
	final static public long[] pointLocation( final Point point ) {
		final long[] dimensions = new long[ point.numDimensions() ];
		for ( int i = 0; i < point.numDimensions(); i++ )
			dimensions[ i ] = point.getLongPosition( i );
		return dimensions;
	}

	/**
	 * Creates an image containing the given component (as is on screen).
	 * 
	 * @param component
	 *            the component to be captured
	 * @return a <code>BufferedImage</code> containing a screenshot of the given
	 *         component.
	 */
	public static BufferedImage getImageOf( final Component component ) {
		return getImageOf( component, component.getWidth(), component.getHeight() );
	}

	/**
	 * Creates an image containing the given component (as is on screen).
	 * 
	 * @param component
	 *            the component to be captured
	 * @param width
	 * @param height
	 * @return a <code>BufferedImage</code> containing a screenshot of the given
	 *         component.
	 */
	public static BufferedImage getImageOf( final Component component, final int width, final int height ) {
		final BufferedImage image = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
		final Graphics2D graphics = image.createGraphics();
		graphics.setPaint( Color.WHITE );
		graphics.fillRect( 0, 0, image.getWidth(), image.getHeight() );
		component.paint( image.getGraphics() );
//		component.printAll( image.getGraphics() );
		return image;
	}

	/**
	 * Saves a given image in the file specified by the given
	 * <code>filename</code>.
	 * 
	 * @param image
	 *            <code>BufferedImage</code> to be saved.
	 * @param filename
	 *            path to the file the image should be saved to.
	 * @throws IOException
	 */
	public static void saveImage( final BufferedImage image, String filename ) throws IOException {
		if ( !filename.endsWith( ".png" ) && !filename.endsWith( ".PNG" ) ) {
			filename += ".png";
		}
		// write the image as a PNG
		ImageIO.write( image, "png", new File( filename ) );
	}
}
