/**
 *
 */
package com.jug.util;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import io.scif.img.ImgIOException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.imglib2.*;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.apache.commons.lang3.StringUtils;

/**
 * @author jug
 * 
 */
public class FloatTypeImgLoader {

	public static ArrayList <Img<FloatType>> loadTiffsFromFileOrFolder(String fileOrPathName, int minTime, int maxTime, int minChannel, int maxChannel) throws FileNotFoundException
	{
		File file = new File(fileOrPathName);

		if (!file.exists()) {
			throw new FileNotFoundException();
		}

		File[] list = file.listFiles(tifFilter);

		if (file.isDirectory() && list.length > 1) {
			return loadTiffsFromFolder( fileOrPathName, minTime, maxTime, minChannel, maxChannel);
		} else if (file.isDirectory() && list.length == 1) {
			return loadTiffsFromFile( list[0].getAbsolutePath(), minTime, maxTime, minChannel, maxChannel);
		} else {
			return loadTiffsFromFile( fileOrPathName, minTime, maxTime, minChannel, maxChannel);
		}
	}

	private static ArrayList<Img<FloatType>> loadTiffsFromFile(String filename, int minTime, int maxTime, int minChannel, int maxChannel) {

		ArrayList<Img<FloatType>> rawChannelImgs = new ArrayList< Img< FloatType >>();

		ImagePlus imp = IJ.openImage(filename);

		int channelCount = imp.getNChannels();
		int frameCount = imp.getNFrames();
		if (channelCount == maxChannel - minChannel + 1 && (frameCount == maxTime - minTime + 1 || (minTime == -1 && maxTime == -1 )))
		{
			ImagePlus[] channelImps = ChannelSplitter.split(imp);
			for (ImagePlus channelImp : channelImps)
			{
				Img<FloatType> img = ImageJFunctions.convertFloat(channelImp);
				System.out.println("size before dupl "  + img.max(2));

				// dirty workaround, see FloatTypeImgLoader.loadMMTiffSequence
				img = duplicateLastSlice(img);

				System.out.println("size after dupl "  + img.max(2));
				rawChannelImgs.add(img);
			}
		} else { // this way is not very memory efficient, but I see no other easy alternative
			int sliceCount = imp.getNSlices();

			for (int c = minChannel; c <= maxChannel; c++) {
				ImagePlus dupl = new Duplicator().run(imp, minChannel, maxChannel, 1, sliceCount, minTime, maxTime);
				Img<FloatType> img = ImageJFunctions.convertFloat(dupl);

				// dirty workaround, see FloatTypeImgLoader.loadMMTiffSequence
				img = duplicateLastSlice(img);
				rawChannelImgs.add(img);
			}
		}

		System.out.println("size before norm  "  + rawChannelImgs.get(0).max(2));
		// Normalise first channel
		ArrayList<IntervalView<FloatType>> firstChannelSlices = Util.slice(rawChannelImgs.get( 0 ));
		for (IntervalView<FloatType> slice : firstChannelSlices)
		{
			Normalize.normalize(slice, new FloatType( 0.0f ), new FloatType( 1.0f ) );
		}
		rawChannelImgs.set(0, Util.stack(firstChannelSlices));

		System.out.println("size after norm  "  + rawChannelImgs.get(0).max(2));
		return rawChannelImgs;
	}

	private static Img<FloatType> duplicateLastSlice(Img<FloatType> inImg) {
		ArrayList<IntervalView<FloatType>> slices = Util.slice(inImg);

		// duplicate last slice
		slices.add(slices.get(slices.size() - 1));



		return Util.stack(slices);
	}

	private static ArrayList<Img<FloatType>> loadTiffsFromFolder(String path, int minTime, int maxTime, int minChannel, int maxChannel) {

		ArrayList<Img<FloatType>> rawChannelImgs = new ArrayList< Img< FloatType >>();
		for ( int cIdx = minChannel; cIdx <= maxChannel; cIdx++ ) {

			// load tiffs from folder
			final String filter = String.format( "_c%04d", cIdx );
			System.out.println( String.format( "Loading tiff sequence for channel, identified by '%s', from '%s'...", filter, path ) );
			try {
				if ( cIdx == minChannel ) {
					rawChannelImgs.add( FloatTypeImgLoader.loadMMPathAsStack( path, minTime, maxTime, true, filter ) );
				} else {
					rawChannelImgs.add( FloatTypeImgLoader.loadMMPathAsStack( path, minTime, maxTime, false, filter ) );
				}
			} catch ( final Exception e ) {
				e.printStackTrace();
				System.exit( 10 );
			}
			System.out.println( "Done loading tiffs!" );
		}

		return rawChannelImgs;
	}



	/**
	 * Loads all files containing ".tif" from a folder given by foldername.
	 * 
	 * @param strFolder
	 *            String pointing to folder containing images (ending with
	 *            '.tif')
	 * @return list containing all loaded tiff files
	 * @throws ImgIOException
	 * @throws IncompatibleTypeException
	 * @throws Exception
	 */
	public static List< Img< FloatType >> loadTiffsFromFolder( final String strFolder ) throws ImgIOException, IncompatibleTypeException, Exception {
		return loadTiffsFromFolder( strFolder, -1, -1, ( String[] ) null );
	}

	public static List< Img< FloatType >> loadTiffsFromFolder( final String strFolder, final int minTime, final int maxTime, final String... filterStrings ) throws ImgIOException, IncompatibleTypeException, Exception {

		final File folder = new File( strFolder );
		final FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept( final File dir, final String name ) {
				boolean isMatching = name.contains( ".tif" );
				for ( final String filter : filterStrings ) {
					isMatching = isMatching && name.contains( filter );
				}
				if ( isMatching == true ) {
					final int time = getTimeFromFilename(name);
					if ( ( minTime != -1 && time < minTime ) || ( maxTime != -1 && time > maxTime ) ) {
						isMatching = false;
					}
				}
				return isMatching;
			}
		};
		final File[] listOfFiles = folder.listFiles( filter );
		if ( listOfFiles == null ) { throw new Exception( "Given argument is not a valid folder!" ); }

		final List< Img< FloatType >> images = loadTiffs( listOfFiles );
		return images;
	}

	/**
	 * 
	 * @param strFolder
	 * @param minTime
	 * @param maxTime
	 * @param normalize
	 * @param filterStrings
	 * @return
	 * @throws ImgIOException
	 * @throws IncompatibleTypeException
	 * @throws Exception
	 */
	public static List< Img< FloatType >> loadMMTiffsFromFolder( final String strFolder, final int minTime, final int maxTime, final boolean normalize, final String... filterStrings ) throws ImgIOException, IncompatibleTypeException, Exception {

		final File folder = new File( strFolder );
		final FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept( final File dir, final String name ) {
				boolean isMatching = name.contains( ".tif" );
				for ( final String filter : filterStrings ) {
					isMatching = isMatching && name.contains( filter );
				}
				if ( isMatching == true ) {
					final int time = getTimeFromFilename(name);
					if ( ( minTime != -1 && time < minTime ) || ( maxTime != -1 && time > maxTime ) ) {
						isMatching = false;
					}
				}
				return isMatching;
			}
		};
		final File[] listOfFiles = folder.listFiles( filter );
		Arrays.sort( listOfFiles ); // LINUX does not do that by default!
		if ( listOfFiles == null ) { throw new Exception( "Given argument is not a valid folder!" ); }

		final List< Img< FloatType >> images = loadMMTiffSequence( listOfFiles, normalize );
		return images;
	}

	/**
	 * @param listOfFiles
	 * @return
	 * @throws ImgIOException
	 */
	public static List< Img< FloatType >> loadTiffs( final File[] listOfFiles ) throws ImgIOException {
		final List< Img< FloatType > > images = new ArrayList< Img< FloatType > >( listOfFiles.length );
		for ( int i = 0; i < listOfFiles.length; i++ ) {
			images.add( null );
		}

		final int numProcessors = Prefs.getThreads();
		final int numThreads = Math.min( listOfFiles.length, numProcessors );

		final ImgIOException ioe = new ImgIOException( "One of the image loading threads had a problem reading from file." );

		final Thread[] threads = new Thread[ numThreads ];

		class ImageProcessingThread extends Thread {

			final int numThread;
			final int numThreads;

			public ImageProcessingThread( final int numThread, final int numThreads ) {
				this.numThread = numThread;
				this.numThreads = numThreads;
			}

			@Override
			public void run() {

				for ( int t = numThread; t < listOfFiles.length; t += numThreads ) {
					try {
						images.set( t, loadTiff( listOfFiles[ t ] ) );
					} catch ( final ImgIOException e ) {
						ioe.setStackTrace( e.getStackTrace() );
					}
				}
			}
		}

		// start threads
		for ( int i = 0; i < numThreads; i++ ) {
			threads[ i ] = new ImageProcessingThread( i, numThreads );
			threads[ i ].start();
		}

		// wait for all threads to terminate
		for ( final Thread thread : threads ) {
			try {
				thread.join();
			} catch ( final InterruptedException e ) {
				System.out.println( "Thread.join was interrupted in FloatTypeImgLoader.loadTiffs - be aware of leaking Threads!" );
				e.printStackTrace();
			}
		}

		// SINGLE THREADED ALTERNATIVE
//		for ( int i = 0; i < listOfFiles.length; i++ ) {
//			try {
//				images.set( i, loadTiff( listOfFiles[ i ] ) );
//			} catch ( final ImgIOException e ) {
//				e.printStackTrace();
//			}
//		}

		return images;
	}

	/**
	 * Load and selectively normalize channels.
	 * Assumptions: filename contains channel info in format "_c%04d".
	 * 
	 * @param listOfFiles
	 * @param normalizationFilterString
	 *            if filename contains this string (not case sensitive), then
	 *            the loaded image will be normalized to [0,1].
	 * @return
	 * @throws ImgIOException
	 */
	public static List< Img< FloatType >> loadMMTiffSequence( final File[] listOfFiles, final boolean normalize ) throws ImgIOException {
		final List< Img< FloatType > > images = new ArrayList< Img< FloatType > >( listOfFiles.length );

		for ( int i = 0; i < listOfFiles.length; i++ ) {
			images.add( null );
		}

		final int numProcessors = Prefs.getThreads();
		final int numThreads = Math.min( listOfFiles.length, numProcessors );

		final ImgIOException ioe = new ImgIOException( "One of the image loading threads had a problem reading from file." );

		final Thread[] threads = new Thread[ numThreads ];

		class ImageProcessingThread extends Thread {

			final int numThread;
			final int numThreads;

			public ImageProcessingThread( final int numThread, final int numThreads ) {
				this.numThread = numThread;
				this.numThreads = numThreads;
			}

			@Override
			public void run() {

				for ( int t = numThread; t < listOfFiles.length; t += numThreads ) {
					try {
						images.set( t, loadTiff( listOfFiles[ t ] ) );
					} catch ( final ImgIOException e ) {
						ioe.setStackTrace( e.getStackTrace() );
					}
					// Selective Normalization!
					if ( normalize ) {
						Normalize.normalize( images.get( t ), new FloatType( 0.0f ), new FloatType( 1.0f ) );
					}
				}
			}
		}

		// start threads
		for ( int i = 0; i < numThreads; i++ ) {
			threads[ i ] = new ImageProcessingThread( i, numThreads );
			threads[ i ].start();
		}

		// wait for all threads to terminate
		for ( final Thread thread : threads ) {
			try {
				thread.join();
			} catch ( final InterruptedException e ) {
				System.out.println( "Thread.join was interrupted in FloatTypeImgLoader.loadTiffs - be aware of leaking Threads!" );
				e.printStackTrace();
			}
		}

		// SINGLE THREADED ALTERNATIVE
		// ---------------------------
//		for ( int i = 0; i < listOfFiles.length; i++ ) {
//			try {
//				images.set( i, loadTiff( listOfFiles[ i ] ) );
//			} catch ( final ImgIOException e ) {
//				e.printStackTrace();
//			}
//			// Selective Normalization!
//			if ( normalize ) {
//				Normalize.normalize( images.get( i ), new FloatType( 0f ), new FloatType( 1f ) );
//			}
//		}

		// Add the last image twice. This is to trick the MM to not having tracking problems towards the last frame.
		// Note that this also means that the GUI always has to show one frame less!!!
		try {
			images.add( loadTiff( listOfFiles[ listOfFiles.length - 1 ] ) );
			if ( normalize ) {
				Normalize.normalize( images.get( listOfFiles.length ), new FloatType( 0.0f ), new FloatType( 1.0f ) );
			}
		} catch ( final ImgIOException e ) {
			e.printStackTrace();
		}

		return images;
	}

	/**
	 * @param listOfFiles
	 * @param imgFactory
	 * @param images
	 * @param imageOpener
	 * @param i
	 * @return
	 * @throws ImgIOException
	 */
	public static Img< FloatType > loadTiff( final File file ) throws ImgIOException {
//	    ALERT: THOSE FOLLOWING TWO LINES CAUSE THREAD LEAK!!!!
//		final ImgFactory< FloatType > imgFactory = new ArrayImgFactory< FloatType >();
//		final ImgOpener imageOpener = new ImgOpener();

		System.out.print( "\n >> Loading file '" + file.getName() + "' ..." );
//		final List< SCIFIOImgPlus< FloatType >> imgs = imageOpener.openImgs( file.getAbsolutePath(), imgFactory, new FloatType() );
//		final Img< FloatType > img = imgs.get( 0 ).getImg();

		//alert! this does not always work, just try with the test images or FloatTypeImgLoaderTest class
		//final Img< FloatType > img = ImagePlusAdapter.wrapReal( IJ.openImage( file.getAbsolutePath() ) );
		final Img< FloatType > img = ImagePlusAdapter.convertFloat( IJ.openImage( file.getAbsolutePath() ) );
		return img;
	}

	/**
	 * Loads all files containing "*.tif" from a given folder.
	 * 
	 * @param inFolder
	 *            Folder containing images (ending with '*.tif')
	 * @return 3d Img, normalized to [0,1]
	 * @throws ImgIOException
	 * @throws IncompatibleTypeException
	 * @throws Exception
	 */
	public static < T extends RealType< T > & NativeType< T > > Img< FloatType > loadFolderAsStack( final File inFolder, final boolean normalize ) throws ImgIOException, IncompatibleTypeException, Exception {
		return loadPathAsStack( inFolder.getAbsolutePath(), normalize );
	}

	/**
	 * Loads all files containing "*.tif" from a given folder.
	 * 
	 * @param inFolder
	 *            Folder containing images (ending with '*.tif')
	 * @return 3d Img, normalized to [0,1]
	 * @throws ImgIOException
	 * @throws IncompatibleTypeException
	 * @throws Exception
	 */
	public static < T extends RealType< T > & NativeType< T > > Img< FloatType > loadFolderAsChannelStack( final File inFolder ) throws ImgIOException, IncompatibleTypeException, Exception {
		return loadPathAsChannelStack( inFolder.getAbsolutePath() );
	}

	/**
	 * Loads all files containing ".tif" from a folder given by foldername.
	 * 
	 * @param strFolder
	 *            String pointing to folder containing images (ending with
	 *            '.tif')
	 * @return 3d Img, normalized to [0,1]
	 * @throws ImgIOException
	 * @throws IncompatibleTypeException
	 * @throws Exception
	 */
	public static < T extends RealType< T > & NativeType< T > > Img< FloatType > loadPathAsChannelStack( final String strFolder ) throws ImgIOException, IncompatibleTypeException, Exception {

		final List< Img< FloatType >> imageList = loadTiffsFromFolder( strFolder );

		Img< FloatType > stack = null;
		final long width = imageList.get( 0 ).dimension( 0 );
		final long height = imageList.get( 0 ).dimension( 1 );
		final long channels = imageList.get( 0 ).dimension( 2 );
		final long frames = imageList.size();

		stack = new ArrayImgFactory< FloatType >().create( new long[] { width, height, channels, frames }, new FloatType() );

		// Add images to stack...
		int i = 0;
		for ( final RandomAccessible< FloatType > image : imageList ) {
			final RandomAccessibleInterval< FloatType > viewZSlize = Views.hyperSlice( stack, 3, i );

			for ( int c = 0; c < channels; c++ ) {
				final RandomAccessibleInterval< FloatType > viewChannel = Views.hyperSlice( viewZSlize, 2, c );
				final IterableInterval< FloatType > iterChannel = Views.iterable( viewChannel );

				if ( image.numDimensions() < 3 ) {
					if ( c > 0 ) { throw new ImgIOException( "Not all images to be loaded contain the same number of color channels!" ); }
					DataMover.copy( image, iterChannel );
				} else {
					DataMover.copy( Views.hyperSlice( image, 2, c ), iterChannel );
				}
				Normalize.normalize( iterChannel, new FloatType( 0.0f ), new FloatType( 1.0f ) );
			}
			i++;
		}

//		ImageJFunctions.show( stack, "muh" );
		return stack;
	}

	/**
	 * Loads all files containing ".tif" from a folder given by foldername.
	 * 
	 * @param strFolder
	 *            String pointing to folder containing images (ending with
	 *            '.tif')
	 * @return 3d Img, normalized to [0,1]
	 * @throws ImgIOException
	 * @throws IncompatibleTypeException
	 * @throws Exception
	 */
	public static < T extends RealType< T > & NativeType< T > > Img< FloatType > loadPathAsStack( final String strFolder, final boolean normalize ) throws ImgIOException, IncompatibleTypeException, Exception {
		return loadPathAsStack( strFolder, -1, -1, normalize, ( String[] ) null );
	}

	public static < T extends RealType< T > & NativeType< T > > Img< FloatType > loadPathAsStack( final String strFolder, final int minTime, final int maxTime, final boolean normalize, final String... filter ) throws ImgIOException, IncompatibleTypeException, Exception {

		final List< Img< FloatType >> imageList = loadTiffsFromFolder( strFolder, minTime, maxTime, filter );
		if ( imageList.size() == 0 ) return null;

		Img< FloatType > stack = null;
		final long width = imageList.get( 0 ).dimension( 0 );
		final long height = imageList.get( 0 ).dimension( 1 );
		final long frames = imageList.size();

		stack = new ArrayImgFactory< FloatType >().create( new long[] { width, height, frames }, new FloatType() );

		// Add images to stack...
		int i = 0;
		for ( final Img< FloatType > image : imageList ) {
			final RandomAccessibleInterval< FloatType > viewZSlize = Views.hyperSlice( stack, 2, i );
			final IterableInterval< FloatType > iterZSlize = Views.iterable( viewZSlize );

			DataMover.copy( Views.extendZero( image ), iterZSlize );
			if ( normalize ) {
				Normalize.normalize( iterZSlize, new FloatType( 0.0f ), new FloatType( 1.0f ) );
			}
			i++;
		}

		return stack;
	}

	public static < T extends RealType< T > & NativeType< T > > Img< FloatType > loadMMPathAsStack( final String strFolder, final int minTime, final int maxTime, final boolean normalize, final String... filter ) throws ImgIOException, IncompatibleTypeException, Exception {

		final List< Img< FloatType >> imageList = loadMMTiffsFromFolder( strFolder, minTime, maxTime, normalize, filter );
		if ( imageList.size() == 0 ) return null;

		Img< FloatType > stack = null;
		final long width = imageList.get( 0 ).dimension( 0 );
		final long height = imageList.get( 0 ).dimension( 1 );
		final long frames = imageList.size();

		stack = new ArrayImgFactory< FloatType >().create( new long[] { width, height, frames }, new FloatType() );

		// Add images to stack...
		int i = 0;
		for ( final Img< FloatType > image : imageList ) {
			final RandomAccessibleInterval< FloatType > viewZSlize = Views.hyperSlice( stack, 2, i );
			final IterableInterval< FloatType > iterZSlize = Views.iterable( viewZSlize );
			DataMover.copy( Views.extendZero( image ), iterZSlize );
			i++;
		}

		return stack;
	}

	/**
	 * Loads a tiff sequence from the given folder. Only those files that
	 * contain <code>filterString</code> as substring in their filename will be
	 * considered. This function tries to automatically determine the number of
	 * time-points and channels to be loaded.
	 * Note: the filename is expected to encode numbers with four digits, like
	 * t=13 would be "_t0013".
	 * 
	 * @param strFolder
	 * @param filterString
	 * @return a <code>List</code> of multi-channel images of type
	 *         <code>Img<FloatType></code>.
	 * @throws Exception
	 */
	public static < T extends RealType< T > & NativeType< T > > List< Img< FloatType >> load2DTiffSequenceAsListOfMultiChannelImgs( final String strFolder, final String filterString ) throws Exception {
		int t, c;
		try {
			t = figureMaxCounterFromFolder( strFolder, filterString, "_t" );
			c = figureMaxCounterFromFolder( strFolder, filterString, "_c" );
		} catch ( final Exception e ) {
			throw new Exception( "Files in this folder seem not to comply to required format... could not determine number of time-points and number of channels!" );
		}
		return load2DTiffSequenceAsListOfMultiChannelImgs( strFolder, filterString, 0, t - 1, 1, c, 4 );
	}

	/**
	 * Loads a tiff sequence from the given folder. Only those files that
	 * contain <code>filterString</code> as substring in their filename will be
	 * considered.
	 * Naming convention: "<some_name>_t####_c####.tif", where # are digits and
	 * <some_name> does NOT contain "_t" or "_c".
	 * 
	 * @param strFolder
	 * @param filterString
	 * @param tmin
	 *            lowest time-index to be loaded.
	 * @param tmax
	 *            highest time-index to be loaded.
	 * @param cmin
	 *            lowest channel-index to be loaded.
	 * @param cmax
	 *            highest channel-index to be loaded.
	 * @param numDigits
	 *            the number of digits used to express the time and channel
	 *            indices, e.g. 0013 would be 4.
	 * @return a <code>List</code> of multi-channel images of type
	 *         <code>Img<FloatType></code>.
	 * @throws Exception
	 */
	public static < T extends RealType< T > & NativeType< T > > List< Img< FloatType >> load2DTiffSequenceAsListOfMultiChannelImgs( final String strFolder, final String filterString, final int tmin, final int tmax, final int cmin, final int cmax, final int numDigits ) throws ImgIOException, IncompatibleTypeException, Exception {
		final List< Img< FloatType >> ret = new ArrayList< Img< FloatType >>();

		final File folder = new File( strFolder );

		for ( int t = tmin; t <= tmax; t++ ) {
			final String tString = String.format( "_t%0" + numDigits + "d", t );

			final List< Img< FloatType > > channelImgs = new ArrayList< Img< FloatType > >();
			for ( int c = cmin; c <= cmax; c++ ) {
				final String cString = String.format( "_c%0" + numDigits + "d", c );

				final FilenameFilter filter = new FilenameFilter() {

					@Override
					public boolean accept( final File dir, final String name ) {
						return name.contains( ".tif" ) && ( ( filterString != null ) ? name.contains( filterString ) : true ) && name.contains( tString ) && name.contains( cString );
					}
				};
				final File[] listOfFiles = folder.listFiles( filter );
				if ( listOfFiles.length == 0 || listOfFiles == null ) { throw new Exception( String.format( "Missing file for t=%d and c=%d", t, c ) ); }
				if ( listOfFiles.length > 1 ) { throw new Exception( String.format( "Multiple matching files for t=%d and c=%d", t, c ) ); }

				channelImgs.add( loadTiff( listOfFiles[ 0 ] ) );
			}
			ret.add( makeMultiChannelImage( channelImgs ) );
		}

		return ret;
	}

	/**
	 * @param channelImgs
	 * @return
	 * @throws ImgIOException
	 */
	public static Img< FloatType > makeMultiChannelImage( final List< Img< FloatType >> imageList ) throws ImgIOException {

		if ( imageList.get( 0 ).numDimensions() != 2 ) { throw new ImgIOException( "MultiChannel image can only be composed out of 2d images (so far)." ); }

		Img< FloatType > retImage = null;
		final long width = imageList.get( 0 ).dimension( 0 );
		final long height = imageList.get( 0 ).dimension( 1 );
		final long channels = imageList.size();

		retImage = new ArrayImgFactory< FloatType >().create( new long[] { width, height, channels }, new FloatType() );

		// Add channels to images to to be returned...
		for ( int c = 0; c < channels; c++ ) {
			final Img< FloatType > image = imageList.get( c );

			final RandomAccessibleInterval< FloatType > viewChannel = Views.hyperSlice( retImage, 2, c );
			final IterableInterval< FloatType > iterChannel = Views.iterable( viewChannel );

			if ( image.numDimensions() == 2 ) {
				DataMover.copy( image, iterChannel );
			} else {
				throw new ImgIOException( "MultiChannel image can only be composed out of non 2d images." );
			}
			Normalize.normalize( iterChannel, new FloatType( 0.0f ), new FloatType( 1.0f ) );
		}

//		ImageJFunctions.show( retImage, "muh" );
		return retImage;
	}

	/**
	 * @param frameList
	 * @return
	 * @throws ImgIOException
	 */
	public static Img< FloatType > makeMultiFrameFromChannelImages( final List< Img< FloatType >> frameList ) throws ImgIOException {

		if ( frameList.get( 0 ).numDimensions() != 3 ) { throw new ImgIOException( "MultiChannel image can only be composed out of 2d images (so far)." ); }

		Img< FloatType > retImage = null;
		final long width = frameList.get( 0 ).dimension( 0 );
		final long height = frameList.get( 0 ).dimension( 1 );
		final long channels = frameList.get( 0 ).dimension( 2 );
		final long frames = frameList.size();

		retImage = new ArrayImgFactory< FloatType >().create( new long[] { width, height, channels, frames }, new FloatType() );

		// Add frames to images to to be returned...
		for ( int f = ( int ) ( frames - 1 ); f >= 0; f-- ) {
			final Img< FloatType > image = frameList.get( f );

			if ( image.numDimensions() == 3 ) {
				if ( channels != image.dimension( 2 ) ) { throw new ImgIOException( "Not all images to be loaded contain the same number of color channels!" ); }

				for ( int c = 0; c < channels; c++ ) {
					final RandomAccessibleInterval< FloatType > sourceChannel = Views.hyperSlice( image, 2, c );
					final RandomAccessibleInterval< FloatType > viewChannel = Views.hyperSlice( Views.hyperSlice( retImage, 3, f ), 2, c );
					final IterableInterval< FloatType > iterChannel = Views.iterable( viewChannel );

					Normalize.normalize( iterChannel, new FloatType( 0.0f ), new FloatType( 1.0f ) );
					DataMover.copy( sourceChannel, iterChannel );
				}
				frameList.remove( f );
			} else {
				throw new ImgIOException( "MultiFrame image can only be composed out of non 3d images." );
			}
		}

//		ImageJFunctions.show( stack, "muh" );
		return retImage;
	}

	/**
	 * Parses all filenames found in given folder, filters them by given filter,
	 * and extracts the maximum int value that can be parsed after the given
	 * prefix. Note: the substring to be parsed an int must start right after
	 * the given prefix and MUST be terminated by either '_' or '.'!
	 * 
	 * @param strFolder
	 *            the folder to look into.
	 * @param filterString
	 *            only files containing this string as substring in their
	 *            filename will be considered.
	 * @param prefix
	 *            indicates the starting position to start parsing for an
	 *            int-value. (Note: the int must terminate with either '_' or
	 *            '.'!)
	 * @return The maximum int-value found.
	 * @throws Exception
	 */
	public static int figureMaxCounterFromFolder( final String strFolder, final String filterString, final String prefix ) throws Exception {
		int max = -1;

		final File folder = new File( strFolder );
		final FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept( final File dir, final String name ) {
				return name.contains( ".tif" ) && ( ( filterString != null ) ? name.contains( filterString ) : true ) && name.contains( prefix );
			}
		};
		final File[] listOfFiles = folder.listFiles( filter );
		if ( listOfFiles == null ) return max;

		for ( int i = 0; i < listOfFiles.length; i++ ) {
			String str = listOfFiles[ i ].getName();

			final int num = getParameterFromFilename(str, prefix);
			/*
			str = str.substring( str.indexOf( prefix ) + prefix.length() );
			int muh = str.indexOf( "_" );
			int mah = str.indexOf( "." );
			if ( muh == -1 ) muh = Integer.MAX_VALUE;
			if ( mah == -1 ) mah = Integer.MAX_VALUE;
			if ( muh == Integer.MAX_VALUE && mah == Integer.MAX_VALUE ) { throw new NumberFormatException(); }
			str = str.substring( 0, Math.min( muh, mah ) );

			int num = -1;
			try {
				num = Integer.parseInt( str );
			} catch ( final NumberFormatException nfe ) {
				throw new Exception( "Naming convention in given folder do not comply to rules... Bad user! ;)" );
			}*/

			if ( max < num ) max = num;
		}

		return max;
	}

	static int getTimeFromFilename(String filename) {
		return getParameterFromFilename(filename, "t");
	}

	public static int getChannelFromFilename(String filename) {
		return getParameterFromFilename(filename, "c");
	}

	/**
	 * This function looks for the last appearing _x0003 part in a filename like
	 * /path_x3/test_xyz/filename_x33.tif and will return 33 in this case.
	 *
	 * @param filename
	 * @param startsWith
	 * @return
	 */
	private static int getParameterFromFilename(String filename, String startsWith) {
		String[] arr = filename.split("_");

		boolean resultValid = false;
		int result = 0;
		for (String item : arr) {
			if (item.startsWith(startsWith)) {

				for (int i = 1; i < item.length(); i++) {
					String substr = item.substring(1, i+1);
					if (isNumeric(substr))
					{
						result = Integer.parseInt(substr);
						resultValid = true;
					}
				}
			}
		}
		if (resultValid)
		{
			return result;
		}
		return 0;
	}


	private static boolean isNumeric(String text) {
		return StringUtils.isNumeric(text);
	}


	public static FileFilter tifFilter = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return pathname.getName().endsWith(".tif");
		}
	};

}
