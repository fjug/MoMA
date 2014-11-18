/**
 * 
 */
package com.jug;

import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.ExtendedRandomAccessibleInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform2D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.jug.loops.Loops;
import com.jug.ops.cursor.FindLocalMaxima;
import com.jug.ops.cursor.FindLocationAboveThreshold;
import com.jug.ops.numerictype.VarOfRai;
import com.jug.util.DataMover;
import com.jug.util.DoubleTypeImgLoader;

/**
 * Takes all files from within a given folder, loads the entire time-laps,
 * detects growth lines, and extracts them.
 * Extracted growth lines are preprocessed (rotated, background subtracted,
 * cropped) and saved as indivisual tiff-sequences in newly created folder
 * structure.
 * 
 * @author jug
 */
public class MM_MovieToDatasets {

	private static class CropArea {

		public long top;
		public long left;
		public long bottom;
		public long right;

		public CropArea( final long top, final long left, final long bottom, final long right ) {
			this.top = top;
			this.left = left;
			this.bottom = bottom;
			this.right = right;
		}
	}

	public static double SIGMA_GL_DETECTION_X = 15.0;
	public static double SIGMA_GL_DETECTION_Y = 3.0;

	public static int GL_WIDTH_TO_EXTRACT = 50;
	public static int GL_OFFSET_LATERAL = 5;
	public static int GL_OFFSET_TOP = 40;
	public static int GL_OFFSET_BOTTOM = 10;

	private static int BGREM_TEMPLATE_XMAX = 35;
	private static int BGREM_TEMPLATE_XMIN = 20;
	private static int BGREM_X_OFFSET = 35;

	/**
	 * Holds the entire movie sequence as loaded from given input folder.
	 */
	private static Img< DoubleType > data;

	/**
	 * The slope of the growth lines in raw data (used to rotate raw images).
	 */
	private static double dCorrectedSlope;

	/**
	 * Contains all detected growth line center points. The structure goes in
	 * line with image data: Outermost list: one element per frame (image in
	 * stack). 2nd list: one element per detected growth-line. 3rd list: one
	 * element (Point) per location downwards along the growth line.
	 */
	private static List< List< List< Point >>> glCenterPoints;

	/**
	 * Contains all GrowthLines found in the given data.
	 */
	private static List< GrowthLine > growthLines;

	/**
	 * PROJECT MAIN
	 * 
	 * @param arga
	 *            muh!
	 */
	public static void main( final String[] args ) {

		// ===== command line parsing ======================================================================

		// create Options object & the parser
		final Options options = new Options();
		final CommandLineParser parser = new BasicParser();
		// defining command line options
		final Option help = new Option( "help", "print this message" );

		final Option channels = new Option( "c", "channels", true, "number of channels (will be determined if not given)" );
		channels.setRequired( false );

		final Option filenameFilterStringOption = new Option( "f", "fnfilter", true, "string that must be contained in loaded filenames" );
		filenameFilterStringOption.setRequired( false );

		final Option doFrameByFrame = new Option( "s", "serial", false, "does data preprocessing frame by frame (saves memory, might cause trouble)" );
		doFrameByFrame.setRequired( false );

		final Option timeFirst = new Option( "tmin", "min_time", true, "first time-point to be processed" );
		timeFirst.setRequired( false );

		final Option timeLast = new Option( "tmax", "max_time", true, "last time-point to be processed" );
		timeFirst.setRequired( false );

		final Option infolder = new Option( "i", "infolder", true, "folder to read data from" );
		infolder.setRequired( true );

		final Option outfolder = new Option( "o", "outfolder", true, "folder to write preprocessed data to (equals infolder if not given)" );
		outfolder.setRequired( false );

		options.addOption( help );
		options.addOption( channels );
		options.addOption( filenameFilterStringOption );
		options.addOption( doFrameByFrame );
		options.addOption( timeFirst );
		options.addOption( timeLast );
		options.addOption( infolder );
		options.addOption( outfolder );
		// get the commands parsed
		CommandLine cmd = null;
		try {
			cmd = parser.parse( options, args );
		} catch ( final ParseException e1 ) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "... -c <# of channels> -i <in-folder> -o [out-folder]", "", options, "Error: " + e1.getMessage() );
			System.exit( 0 );
		}

		if ( cmd.hasOption( "help" ) ) {
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "... -c <# of channels> -i <in-folder> -o [out-folder]", options );
			System.exit( 0 );
		}

		boolean doSerial = false;
		if ( cmd.hasOption( "s" ) ) {
			doSerial = true;
		}

		final File inFolder = new File( cmd.getOptionValue( "i" ) );
		if ( !inFolder.isDirectory() ) {
			System.out.println( "Error: Input folder is not a directory!" );
			System.exit( 2 );
		}
		if ( !inFolder.canRead() ) {
			System.out.println( "Error: Input folder cannot be read!" );
			System.exit( 2 );
		}

		File outFolder = null;
		if ( !cmd.hasOption( "o" ) ) {
			outFolder = inFolder;
		} else {
			outFolder = new File( cmd.getOptionValue( "o" ) );

			if ( !outFolder.isDirectory() ) {
				System.out.println( "Error: Output folder is not a directory!" );
				System.exit( 3 );
			}
			if ( !inFolder.canWrite() ) {
				System.out.println( "Error: Output folder cannot be written to!" );
				System.exit( 3 );
			}
		}

		// get filter string
		String filenameFilterString = "";
		if ( cmd.hasOption( "f" ) ) {
			filenameFilterString = cmd.getOptionValue( "f" );
		}

		// receive number of channels to be loaded
		int maxTime = -1;
		int minTime = 0;
		int numChannelsToLoad = -1;
		try {
			maxTime = DoubleTypeImgLoader.figureMaxCounterFromFolder( inFolder.getAbsolutePath(), filenameFilterString, "_t" );
			numChannelsToLoad = DoubleTypeImgLoader.figureMaxCounterFromFolder( inFolder.getAbsolutePath(), filenameFilterString, "_c" );
		} catch ( final Exception e ) {
			e.printStackTrace();
			System.exit( 4 );
		}
		if ( cmd.hasOption( "c" ) ) {
			numChannelsToLoad = Integer.parseInt( cmd.getOptionValue( "c" ) );
		}

		if ( cmd.hasOption( "tmin" ) ) {
			minTime = Integer.parseInt( cmd.getOptionValue( "tmin" ) );
		}
		if ( cmd.hasOption( "tmax" ) ) {
			maxTime = Integer.parseInt( cmd.getOptionValue( "tmax" ) );
		}

		System.out.print( String.format( "Loading tiff sequence (%d time points, %d channels):", maxTime - minTime + 1, numChannelsToLoad ) );

		// TODO remove!
		CropArea crop = null;
		for ( int t = minTime; t <= ( ( doSerial ) ? maxTime : minTime ); t++ ) {

			// ===== load tiffs from folder ======================================================================

			try {
				List< Img< DoubleType >> frameList = null;
				if ( doSerial ) {
					System.out.print( String.format( "Loading time point %d  (%d channels) ...", t, numChannelsToLoad ) );
					frameList = DoubleTypeImgLoader.load2DTiffSequenceAsListOfMultiChannelImgs( inFolder.getAbsolutePath(), "_c", t, t, 1, numChannelsToLoad, 4 );
				} else {
					System.out.print( String.format( "Loading data  (%d time-point with %d channels each) ...", maxTime, numChannelsToLoad ) );
					frameList = DoubleTypeImgLoader.load2DTiffSequenceAsListOfMultiChannelImgs( inFolder.getAbsolutePath(), "_c", minTime, maxTime, 1, numChannelsToLoad, 4 );
				}
				System.out.print( String.format( "    ...glue time-point multi-channel images together...", maxTime - minTime + 1, numChannelsToLoad ) );
				data = DoubleTypeImgLoader.makeMultiFrameFromChannelImages( frameList ); // ALERT: this empties the list in fact!
			} catch ( final Exception e ) {
				e.printStackTrace();
				System.exit( 4 );
			}
			System.out.println( " done!" );

//			ImageJFunctions.show( data, "Loaded data..." );

			// ===== start preprocessing pipeline ======================================================================

			// straighten loaded images
			System.out.print( "Straighten loaded images..." );
			data = straightenRawImg( data );
			System.out.println( " done!" );

//			ImageJFunctions.show( data, "Straightened data..." );

			// cropping loaded images
			System.out.print( "Cropping to ROI..." );
			// if we are in serial mode we want to reuse the crop area
			// this makes drift being a problem, but that is better then having GLs jumping around like hell!
			if ( crop == null ) {
				crop = determineCropCoordinates( data );
			}
			data = cropRawImgToROI( data, crop );
			System.out.println( " done!" );

//			ImageJFunctions.show( data, "Cropped data..." );

			// searching for GLs
			System.out.print( "Searching for GrowthLines..." );
			findGrowthLines( data );
			System.out.println( " done!" );

			// subtracting BG in RAW image...
			System.out.print( "Subtracting background..." );
			subtractBackground( data );
			System.out.println( " done!" );

//			ImageJFunctions.show( data, "BG-Subtracted data..." );

			// exporting individual GLs as image sequence...
			System.out.print( "Exporting individual GLs..." );
			for ( int i = 0; i < growthLines.size(); i++ ) {
				System.out.print( " " + ( i + 1 ) );
				final String newFileName = String.format( "%s%sGL%02d", outFolder.getAbsolutePath(), File.separator, i );
				exportGrowthLineToFolder( data, i, new File( newFileName ), t );
			}
			System.out.println( " done!" );
		}

		System.out.println( "Individual growth-lines written to " + outFolder.getAbsolutePath() + " !" );
		System.exit( 0 );
	}

	/**
	 * Rotates the whole stack (each Z-slize) in order to vertically align the
	 * wells seen in the micrographs come from the MotherMachine. Note: This is
	 * NOT done in-place! The returned <code>Img</code> is newly created!
	 * 
	 * @param data
	 *            - the 3d <code>Img</code> to be straightened.
	 * @return the straightened <code>Img</code>. (Might be larger to avoid
	 *         loosing data!)
	 */
	private static Img< DoubleType > straightenRawImg( final Img< DoubleType > data ) {
		assert ( data.numDimensions() == 4 );

		// new raw image
		Img< DoubleType > rawNew;

		// find out how slanted the given stack is...
		final List< Cursor< DoubleType >> points = new Loops< DoubleType, Cursor< DoubleType >>().forEachHyperslice( Views.hyperSlice( Views.hyperSlice( data, 3, 0 ), 2, 0 ), 0, new FindLocationAboveThreshold< DoubleType >( new DoubleType( 0.33 ) ) );

		final SimpleRegression regression = new SimpleRegression();
		final long[] pos = new long[ 2 ];
		int i = 0;
		final double[] plotData = new double[ points.size() ];
		for ( final Cursor< DoubleType > c : points ) {
			c.localize( pos );
			regression.addData( i, -pos[ 0 ] );
			plotData[ i ] = -pos[ 0 ];
			// System.out.println("Regression.addData ( " + i + ", " + (-pos[0])
			// + " )");
			i++;
		}
		// Plot2d.simpleLinePlot("Global positioning regression data",
		// plotData);

		dCorrectedSlope = regression.getSlope();
		final double radSlant = Math.atan( regression.getSlope() );
		// System.out.println("slope = " + regression.getSlope());
		// System.out.println("intercept = " + regression.getIntercept());
		final double[] dCenter2d = new double[] { data.dimension( 0 ) * 0.5, -regression.getIntercept() + points.size() * regression.getSlope() };

		// ...and inversely rotate the whole stack in XY
		final AffineTransform2D affine = new AffineTransform2D();
		affine.translate( -dCenter2d[ 0 ], -dCenter2d[ 1 ] );
		affine.rotate( radSlant );
		affine.translate( dCenter2d[ 0 ], dCenter2d[ 1 ] );

		long minX = data.min( 0 ), maxX = data.max( 0 );
		long minY = data.min( 1 );
		final long maxY = data.max( 1 );
		final double[][] corners = { new double[] { minX, minY }, new double[] { maxX, minY }, new double[] { minX, maxY }, new double[] { maxX, maxY } };
		final double[] tmp = new double[ 2 ];
		for ( final double[] corner : corners ) {
			affine.apply( corner, tmp );
			minX = Math.min( minX, ( long ) tmp[ 0 ] );
			maxX = Math.max( maxX, ( long ) tmp[ 0 ] );
			minY = Math.min( minY, ( long ) tmp[ 1 ] );
			// maxY = Math.max(maxY, (long)tmp[1]); // if this line is active
			// also the bottom would be extenden while rotating (currently not
			// wanted!)
		}

		rawNew = data.factory().create( new long[] { maxX - minX, maxY - minY, data.dimension( 2 ), data.dimension( 3 ) }, data.firstElement() );

		for ( i = 0; i < data.dimension( 3 ); i++ ) {
			final RandomAccessibleInterval< DoubleType > viewZSlize = Views.hyperSlice( data, 3, i );

			for ( int c = 0; c < data.dimension( 2 ); c++ ) {
				final RandomAccessibleInterval< DoubleType > viewCZSlize = Views.hyperSlice( viewZSlize, 2, c );

				final RandomAccessible< DoubleType > raInfCZSlize = Views.extendValue( viewCZSlize, new DoubleType( 0.0 ) );
				final RealRandomAccessible< DoubleType > rraInterpolatedCZSlize = Views.interpolate( raInfCZSlize, new NLinearInterpolatorFactory< DoubleType >() );
				final RandomAccessible< DoubleType > raRotatedCZSlize = RealViews.affine( rraInterpolatedCZSlize, affine );

				final RandomAccessibleInterval< DoubleType > raiRotatedAndTruncatedCZSlize = Views.zeroMin( Views.interval( raRotatedCZSlize, new long[] { minX, minY }, new long[] { maxX, maxY } ) );

				DataMover.copy( raiRotatedAndTruncatedCZSlize, Views.iterable( Views.hyperSlice( Views.hyperSlice( rawNew, 3, i ), 2, c ) ) );
			}
		}

//		ImageJFunctions.show( rawNew );
		return rawNew;
	}

	/**
	 * Finds the region of interest in the given 3d image stack and crops it.
	 * Note: each cropped z-slize will be renormalized to [0,1]. Precondition:
	 * given <code>Img</code> should be rotated such that the seen wells are
	 * axis parallel.
	 * 
	 * @param data
	 *            - the streightened 3d <code>Img</code> that should be cropped
	 *            down to contain only the ROI.
	 */
	private static Img< DoubleType > cropRawImgToROI( final Img< DoubleType > data ) {
		assert ( data.numDimensions() == 4 );

		final Long top, left, bottom, right;
		final CropArea c = determineCropCoordinates( data );

		return cropRawImgToROI( data, c.top, c.left, c.bottom, c.right );
	}

	/**
	 * @param top
	 * @param left
	 * @param bottom
	 * @param right
	 */
	private static CropArea determineCropCoordinates( final Img< DoubleType > data ) {

		// crop positions to be evaluated
		long top = 0, bottom = data.dimension( 1 );
		long left, right;

		// check for possible crop in first and last image
		final long[] lFPositions = new long[] { 0, data.dimension( 3 ) - 1 };
		for ( final long lFPos : lFPositions ) {
			// find out how slanted the given stack is...
			final List< DoubleType > points = new Loops< DoubleType, DoubleType >().forEachHyperslice( Views.hyperSlice( data, 3, lFPos ), 1, new VarOfRai< DoubleType >() );

			final double[] y = new double[ points.size() ];
			int i = 0;
			for ( final DoubleType dtPoint : points ) {
				y[ i ] = dtPoint.get();
				i++;
			}

			// Plot2d.simpleLinePlot("Variance in image rows", y, "Variance");
			final double threshold = 0.005;

			// looking for longest interval above threshold
			int curMaxLen = 0;
			int curLen = 0;
			long topCandidate = 0;
			boolean below = true;
			for ( int j = 0; j < y.length; j++ ) {
				if ( below && y[ j ] > threshold ) {
					topCandidate = j;
					below = false;
					curLen = 1;
				}
				if ( !below && y[ j ] > threshold ) {
					curLen++;
				}
				if ( !below && ( y[ j ] <= threshold || j == y.length - 1 ) ) {
					below = true;
					if ( curLen > curMaxLen ) {
						curMaxLen = curLen;
						top = topCandidate;
						bottom = j;
					}
					curLen = 0;
				}
			}
			// System.out.println(">> Top/bottom: " + top + " / " + bottom);
		}
		left = Math.round( Math.floor( 0 - dCorrectedSlope * bottom ) );
		right = Math.round( Math.ceil( data.dimension( 0 ) + dCorrectedSlope * ( data.dimension( 1 ) - top ) ) );

		final CropArea ret = new CropArea( top, left, bottom, right );
		return ret;
	}

	/**
	 * If you know how to crop... go ahead!
	 * 
	 * @param data
	 * @param c
	 * @return
	 */
	private static Img< DoubleType > cropRawImgToROI( final Img< DoubleType > data, final CropArea c ) {
		return cropRawImgToROI( data, c.top, c.left, c.bottom, c.right );
	}

	/**
	 * If you know how to crop... go ahead!
	 * 
	 * @param data
	 * @param left
	 * @param top
	 * @param bottom
	 * @param right
	 * @return
	 */
	private static Img< DoubleType > cropRawImgToROI( final Img< DoubleType > data, final long top, final long left, final long bottom, final long right ) {
		// return image
		Img< DoubleType > rawNew = null;

		// create image that can host cropped data
		rawNew = data.factory().create( new long[] { right - left, bottom - top, data.dimension( 2 ), data.dimension( 3 ) }, data.firstElement() );

		// and copy it there
		for ( int f = 0; f < data.dimension( 3 ); f++ ) {
			final RandomAccessibleInterval< DoubleType > viewZSlize = Views.hyperSlice( data, 3, f );

			for ( int c = 0; c < data.dimension( 2 ); c++ ) {
				final RandomAccessibleInterval< DoubleType > viewCZSlize = Views.hyperSlice( viewZSlize, 2, c );
				final RandomAccessibleInterval< DoubleType > viewCroppedCZSlize = Views.zeroMin( Views.interval( viewCZSlize, new long[] { left, top }, new long[] { bottom, right } ) );

				DataMover.copy( viewCroppedCZSlize, Views.iterable( Views.hyperSlice( Views.hyperSlice( rawNew, 3, f ), 2, c ) ) );
				// Normalize.normalize(Views.iterable( Views.hyperSlice(ret, 2, i)
				// ), new DoubleType(0.0), new DoubleType(1.0));
			}
		}

//		ImageJFunctions.show( rawNew );
		return rawNew;
	}

	/**
	 * Estimates the centers of the growth lines given in 'data'. The found
	 * center lines are computed by a linear regression of growth line center
	 * estimates. Those estimates are obtained by convolving the image with a
	 * Gaussian (parameterized by SIGMA_GL_DETECTION_*) and looking for local
	 * maxima in that image.
	 * 
	 * This function operates on 'imgTemp' and sets 'glCenterPoints' as well as
	 * 'growthLines'.
	 */
	private static void findGrowthLines( final Img< DoubleType > data ) {

		growthLines = new ArrayList< GrowthLine >();
		glCenterPoints = new ArrayList< List< List< Point >>>();

		// movie copy for smoothing
		final Img< DoubleType > movieCopy = data.copy();

		List< List< Point > > frameWellCenters;

		// ------ GAUSS -----------------------------

		final int n = data.numDimensions();
		final double[] sigmas = new double[ n ];
		sigmas[ 0 ] = SIGMA_GL_DETECTION_X;
		sigmas[ 1 ] = SIGMA_GL_DETECTION_Y;
		try {
			Gauss3.gauss( sigmas, Views.extendMirrorDouble( data ), movieCopy );
		} catch ( final IncompatibleTypeException e ) {
			e.printStackTrace();
		}

		// ------ FIND AND FILTER MAXIMA -------------

		final List< List< GrowthLineFrame >> collectionOfFrames = new ArrayList< List< GrowthLineFrame >>();

		for ( long frameIdx = 0; frameIdx < movieCopy.dimension( 3 ); frameIdx++ ) {
			// first color channel, current frame
			final IntervalView< DoubleType > ivFrame = Views.hyperSlice( Views.hyperSlice( movieCopy, 3, frameIdx ), 2, 0 );

			// Find maxima per image row (per frame)
			frameWellCenters = new Loops< DoubleType, List< Point >>().forEachHyperslice( ivFrame, 1, new FindLocalMaxima< DoubleType >() );

			// Delete detected points that are too lateral
			for ( int y = 0; y < frameWellCenters.size(); y++ ) {
				final List< Point > lstPoints = frameWellCenters.get( y );
				for ( int x = lstPoints.size() - 1; x >= 0; x-- ) {
					if ( lstPoints.get( x ).getIntPosition( 0 ) < GL_OFFSET_LATERAL || lstPoints.get( x ).getIntPosition( 0 ) > movieCopy.dimension( 0 ) - GL_OFFSET_LATERAL ) {
						lstPoints.remove( x );
					}
				}
				frameWellCenters.set( y, lstPoints );
			}

			// Delete detected points that are too high or too low
			// (and use this sweep to compute 'maxWellCenterIdx' and
			// 'maxWellCenters')
			int maxWellCenters = 0;
			int maxWellCentersIdx = 0;
			for ( int y = 0; y < frameWellCenters.size(); y++ ) {
				if ( y < GL_OFFSET_TOP || y > frameWellCenters.size() - 1 - GL_OFFSET_BOTTOM ) {
					frameWellCenters.get( y ).clear();
				} else {
					if ( maxWellCenters < frameWellCenters.get( y ).size() ) {
						maxWellCenters = frameWellCenters.get( y ).size();
						maxWellCentersIdx = y;
					}
				}
			}

			// add all the remaining points to 'glCenterPoints'
			glCenterPoints.add( frameWellCenters );

			// ------ DISTRIBUTE POINTS TO CORRESPONDING GROWTH LINES -------

			final List< GrowthLineFrame > glFrames = new ArrayList< GrowthLineFrame >();

			final Point pOrig = new Point( 4 );
			pOrig.setPosition( frameIdx, 3 ); // location in original Img (will
												// be recovered step by step)
			pOrig.setPosition( 0, 2 );

			// start at the row containing the maximum number of well centers
			// (see above for the code that found maxWellCenter*)
			pOrig.setPosition( maxWellCentersIdx, 1 );
			for ( int x = 0; x < maxWellCenters; x++ ) {
				glFrames.add( new GrowthLineFrame() ); // add one GLF for each
														// found column
				final Point p = frameWellCenters.get( maxWellCentersIdx ).get( x );
				pOrig.setPosition( p.getLongPosition( 0 ), 0 );
				glFrames.get( x ).addPoint( new Point( pOrig ) );
			}
			// now go backwards from 'maxWellCenterIdx' and find the right
			// assignment in case
			// a different number of wells was found (going forwards comes
			// below!)
			for ( int y = maxWellCentersIdx - 1; y >= 0; y-- ) {
				pOrig.setPosition( y, 1 ); // location in orig. Img (2nd of 3
											// steps)

				final List< Point > maximaPerImgRow = frameWellCenters.get( y );
				if ( maximaPerImgRow.size() == 0 ) {
					break;
				}
				// find best matching well for first point
				final int posX = frameWellCenters.get( y ).get( 0 ).getIntPosition( 0 );
				int mindist = ( int ) movieCopy.dimension( 0 );
				int offset = 0;
				for ( int x = 0; x < maxWellCenters; x++ ) {
					final int wellPosX = glFrames.get( x ).getFirstPoint().getIntPosition( 0 );
					if ( mindist > Math.abs( wellPosX - posX ) ) {
						mindist = Math.abs( wellPosX - posX );
						offset = x;
					}
				}
				// move points into detected wells
				for ( int x = offset; x < maximaPerImgRow.size(); x++ ) {
					final Point p = maximaPerImgRow.get( x );
					pOrig.setPosition( p.getLongPosition( 0 ), 0 );
					glFrames.get( x ).addPoint( new Point( pOrig ) );
				}
			}
			// now go forward from 'maxWellCenterIdx' and find the right
			// assignment in case
			// a different number of wells was found
			for ( int y = maxWellCentersIdx + 1; y < frameWellCenters.size(); y++ ) {
				pOrig.setPosition( y, 1 ); // location in original Img (2nd of 3
				// steps)

				final List< Point > maximaPerImgRow = frameWellCenters.get( y );
				if ( maximaPerImgRow.size() == 0 ) {
					break;
				}
				// find best matching well for first point
				final int posX = frameWellCenters.get( y ).get( 0 ).getIntPosition( 0 );
				int mindist = ( int ) movieCopy.dimension( 0 );
				int offset = 0;
				for ( int x = 0; x < maxWellCenters; x++ ) {
					final int wellPosX = glFrames.get( x ).getLastPoint().getIntPosition( 0 );
					if ( mindist > Math.abs( wellPosX - posX ) ) {
						mindist = Math.abs( wellPosX - posX );
						offset = x;
					}
				}
				// move points into GLFs
				for ( int x = offset; x < maximaPerImgRow.size(); x++ ) {
					final Point p = maximaPerImgRow.get( x );
					pOrig.setPosition( p.getLongPosition( 0 ), 0 );
					glFrames.get( x ).addPoint( new Point( pOrig ) );
				}
			}

			// sort points
			for ( final GrowthLineFrame glf : glFrames ) {
				glf.sortPoints();
			}

			// add this list of GrowhtLIneFrames to the collection
			collectionOfFrames.add( glFrames );
		}

		// ------ SORT GrowthLineFrames FROM collectionOfFrames INTO
		// this.growthLines -------------
		int maxGLsPerFrame = 0;
		int maxGLsPerFrameIdx = 0;
		for ( int i = 0; i < collectionOfFrames.size(); i++ ) {
			if ( maxGLsPerFrame < collectionOfFrames.get( i ).size() ) {
				maxGLsPerFrame = collectionOfFrames.get( i ).size();
				maxGLsPerFrameIdx = i;
			}
		}
		// copy the max-GLs frame into this.growthLines
		growthLines = new ArrayList< GrowthLine >( maxGLsPerFrame );
		for ( int i = 0; i < maxGLsPerFrame; i++ ) {
			growthLines.add( new GrowthLine() );
			growthLines.get( i ).add( collectionOfFrames.get( maxGLsPerFrameIdx ).get( i ) );
		}
		// go backwards from there and prepand into GL
		for ( int j = maxGLsPerFrameIdx - 1; j >= 0; j-- ) {
			final int deltaL = maxGLsPerFrame - collectionOfFrames.get( j ).size();
			int offset = 0; // here we would like to have the shift to consider
							// when copying GLFrames into GLs
			double minDist = Double.MAX_VALUE;
			for ( int i = 0; i <= deltaL; i++ ) {
				double dist = collectionOfFrames.get( maxGLsPerFrameIdx ).get( i ).getAvgXpos();
				dist -= collectionOfFrames.get( j ).get( 0 ).getAvgXpos();
				if ( dist < minDist ) {
					minDist = dist;
					offset = i;
				}
			}
			for ( int i = 0; i < collectionOfFrames.get( j ).size(); i++ ) {
				growthLines.get( offset + i ).prepand( collectionOfFrames.get( j ).get( i ) );
			}
		}
		// go forwards and append into GL
		for ( int j = maxGLsPerFrameIdx + 1; j < collectionOfFrames.size(); j++ ) {
			final int deltaL = maxGLsPerFrame - collectionOfFrames.get( j ).size();
			int offset = 0; // here we would like to have the shift to consider
							// when copying GLFrames into GLs
			double minDist = Double.MAX_VALUE;
			for ( int i = 0; i <= deltaL; i++ ) {
				double dist = collectionOfFrames.get( maxGLsPerFrameIdx ).get( i ).getAvgXpos();
				dist -= collectionOfFrames.get( j ).get( 0 ).getAvgXpos();
				if ( dist < minDist ) {
					minDist = dist;
					offset = i;
				}
			}
			for ( int i = 0; i < collectionOfFrames.get( j ).size(); i++ ) {
				growthLines.get( offset + i ).add( collectionOfFrames.get( j ).get( i ) );
			}
		}

	}

	/**
	 * Simple but effective method to subtract uneven illumination from the
	 * growth-line data.
	 * 
	 * @param data
	 *            DoubleType image stack.
	 */
	private static void subtractBackground( final Img< DoubleType > data ) {

		for ( int i = 0; i < growthLines.size(); i++ ) {
			for ( int f = 0; f < growthLines.get( i ).size(); f++ ) {
				final GrowthLineFrame glf = growthLines.get( i ).get( f );

				final int glfX = glf.getAvgXpos();
				if ( glfX == -1 ) continue; // do not do anything with empty GLFs

				final int glfY1 = 0; // gl.getFirstPoint().getIntPosition(1);
				final int glfY2 = ( int ) data.dimension( 1 ) - 1;

				final IntervalView< DoubleType > frame = Views.hyperSlice( Views.hyperSlice( data, 3, f ), 2, 0 );

				double rowAvgs[] = new double[ glfY2 - glfY1 + 1 ];
				int colCount = 0;
				// Look to the left if you are not the first GLF
				if ( glfX > BGREM_TEMPLATE_XMAX ) {
					final IntervalView< DoubleType > leftBackgroundWindow = Views.interval( frame, new long[] { glfX - BGREM_TEMPLATE_XMAX, glfY1 }, new long[] { glfX - BGREM_TEMPLATE_XMIN, glfY2 } );
					rowAvgs = addRowSumsFromInterval( leftBackgroundWindow, rowAvgs );
					colCount += ( BGREM_TEMPLATE_XMAX - BGREM_TEMPLATE_XMIN );
				}
				// Look to the right if you are not the last GLF
				if ( glfX < data.dimension( 0 ) - BGREM_TEMPLATE_XMAX ) {
					final IntervalView< DoubleType > rightBackgroundWindow = Views.interval( frame, new long[] { glfX + BGREM_TEMPLATE_XMIN, glfY1 }, new long[] { glfX + BGREM_TEMPLATE_XMAX, glfY2 } );
					rowAvgs = addRowSumsFromInterval( rightBackgroundWindow, rowAvgs );
					colCount += ( BGREM_TEMPLATE_XMAX - BGREM_TEMPLATE_XMIN );
				}
				// compute averages
				for ( int j = 0; j < rowAvgs.length; j++ ) {
					rowAvgs[ j ] /= colCount;
				}

				// Subtract averages you've seen to your left and/or to your
				// right
				final long x1 = Math.max( 0, glfX - BGREM_X_OFFSET );
				final long x2 = Math.min( frame.dimension( 0 ) - 1, glfX + BGREM_X_OFFSET );
				final IntervalView< DoubleType > growthLineArea = Views.interval( frame, new long[] { x1, glfY1 }, new long[] { x2, glfY2 } );
				removeValuesFromRows( growthLineArea, rowAvgs );
				// Normalize the zone we removed the background from...
				Normalize.normalize( Views.iterable( growthLineArea ), new DoubleType( 0.0 ), new DoubleType( 1.0 ) );
			}
		}
	}

	/**
	 * Adds all intensity values of row i in view to rowSums[i].
	 * 
	 * @param view
	 * @param rowSums
	 */
	private static double[] addRowSumsFromInterval( final IntervalView< DoubleType > view, final double[] rowSums ) {
		for ( int i = 0; i < view.dimension( 1 ); i++ ) {
			final IntervalView< DoubleType > row = Views.hyperSlice( view, 1, i );
			final Cursor< DoubleType > cursor = Views.iterable( row ).cursor();
			while ( cursor.hasNext() ) {
				rowSums[ i ] += cursor.next().get();
			}
		}
		return rowSums;
	}

	/**
	 * Removes the value values[i] from all columns in row i of the given view.
	 * 
	 * @param view
	 * @param values
	 */
	private static void removeValuesFromRows( final IntervalView< DoubleType > view, final double[] values ) {
		for ( int i = 0; i < view.dimension( 1 ); i++ ) {
			final Cursor< DoubleType > cursor = Views.iterable( Views.hyperSlice( view, 1, i ) ).cursor();
			while ( cursor.hasNext() ) {
				cursor.next().set( new DoubleType( Math.max( 0, cursor.get().get() - values[ i ] ) ) );
			}
		}
	}

	/**
	 * Write GL i into folder <code>newFolder</code> as sequence of cropped tiff
	 * files.
	 * 
	 * @param idxGL
	 *            index of the growth line that should be exported.
	 * @param outputFolder
	 *            existing folder that can be written to.
	 */
	private static void exportGrowthLineToFolder( final Img< DoubleType > data, final int idxGL, final File outputFolder, final int tOffset ) {
		IntervalView< DoubleType > extractedView;
		RandomAccessibleInterval< DoubleType > extractedRai;

		// If output folder does not exist -- create it!
		if ( !outputFolder.exists() ) {
			outputFolder.mkdir();
		}

		// Figure out which sub-image should final be exported and final export it!
		int t = 0;
		final List< GrowthLineFrame > frames = growthLines.get( idxGL ).getFrames();
		for ( final GrowthLineFrame frame : frames ) {
			for ( int c = 0; c < data.dimension( 2 ); c++ ) {
				final long offsetX = frame.getOffsetX();
				final long offsetY = frame.getOffsetY();
				final long offsetF = frame.getOffsetF();
				final long width = GL_WIDTH_TO_EXTRACT;
				final long absoluteHeight = data.max( 1 );

				extractedView = Views.offset( Views.hyperSlice( Views.hyperSlice( data, 3, t ), 2, c ), offsetX - width / 2, offsetY );
				final ExtendedRandomAccessibleInterval< DoubleType, IntervalView< DoubleType >> extendedView = Views.extendMirrorSingle( extractedView );
				extractedRai = Views.interval( extendedView, new long[] { 0, 0 }, new long[] { width, absoluteHeight - offsetY } );
				final ImagePlus iPlus = ImageJFunctions.wrapFloat( extractedRai, "temp" );

				final String fn = String.format( outputFolder.getAbsolutePath() + File.separator + "gl%02d_t%04d_c%02d.tif", idxGL, tOffset + t, c );
				IJ.save( iPlus.duplicate(), fn );
			}
			t++;
		}
	}
}
