/**
 *
 */
package com.jug.util;

import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.Normalize;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.Views;

/**
 * @author jug
 * 
 */
public class DoubleTypeImgLoader {

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
	public static List< Img< DoubleType >> loadTiffsFromFolder( final String strFolder ) throws ImgIOException, IncompatibleTypeException, Exception {
		return loadTiffsFromFolder( strFolder, null );
	}

	public static List< Img< DoubleType >> loadTiffsFromFolder( final String strFolder, final String filterString ) throws ImgIOException, IncompatibleTypeException, Exception {

		final File folder = new File( strFolder );
		final FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept( final File dir, final String name ) {
				return name.contains( ".tif" ) && ( ( filterString != null ) ? name.contains( filterString ) : true );
			}
		};
		final File[] listOfFiles = folder.listFiles( filter );
		if ( listOfFiles == null )
			throw new Exception( "Given argument is not a valid folder!" );

		final ImgFactory< DoubleType > imgFactory = new ArrayImgFactory< DoubleType >();
		final List< Img< DoubleType > > images = new ArrayList< Img< DoubleType > >();
		final ImgOpener imageOpener = new ImgOpener();
		for ( int i = 0; i < listOfFiles.length; i++ ) {
			if ( listOfFiles[ i ].isFile() ) {
				System.out.println( ">> Loading file '" + listOfFiles[ i ].getName() + "' ..." );
				final List< SCIFIOImgPlus< DoubleType >> imgs = imageOpener.openImgs( listOfFiles[ i ].getAbsolutePath(), imgFactory, new DoubleType() );
				final Img< DoubleType > img = imgs.get( 0 ).getImg();
				images.add( img );
//				ImageJFunctions.show( img );
			}
		}
		return images;
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
	public static < T extends RealType< T > & NativeType< T > > Img< DoubleType > loadFolderAsStack( final File inFolder ) throws ImgIOException, IncompatibleTypeException, Exception {
		return loadPathAsStack( inFolder.getAbsolutePath() );
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
	public static < T extends RealType< T > & NativeType< T > > Img< DoubleType > loadFolderAsChannelStack( final File inFolder ) throws ImgIOException, IncompatibleTypeException, Exception {
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
	public static < T extends RealType< T > & NativeType< T > > Img< DoubleType > loadPathAsChannelStack( final String strFolder ) throws ImgIOException, IncompatibleTypeException, Exception {

		final List< Img< DoubleType >> imageList = loadTiffsFromFolder( strFolder );

		Img< DoubleType > stack = null;
		final long width = imageList.get( 0 ).dimension( 0 );
		final long height = imageList.get( 0 ).dimension( 1 );
		final long channels = imageList.get( 0 ).dimension( 2 );
		final long frames = imageList.size();

		stack = new ArrayImgFactory< DoubleType >().create( new long[] { width, height, channels, frames }, new DoubleType() );

		// Add images to stack...
		int i = 0;
		for ( final RandomAccessible< DoubleType > image : imageList ) {
			final RandomAccessibleInterval< DoubleType > viewZSlize = Views.hyperSlice( stack, 3, i );

			for ( int c = 0; c < channels; c++ ) {
				final RandomAccessibleInterval< DoubleType > viewChannel = Views.hyperSlice( viewZSlize, 2, c );
				final IterableInterval< DoubleType > iterChannel = Views.iterable( viewChannel );

				if ( image.numDimensions() < 3 ) {
					if ( c > 0 ) { throw new ImgIOException( "Not all images to be loaded contain the same number of color channels!" ); }
					DataMover.copy( image, iterChannel );
				} else {
					DataMover.copy( Views.hyperSlice( image, 2, c ), iterChannel );
				}
				Normalize.normalize( iterChannel, new DoubleType( 0.0 ), new DoubleType( 1.0 ) );
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
	public static < T extends RealType< T > & NativeType< T > > Img< DoubleType > loadPathAsStack( final String strFolder ) throws ImgIOException, IncompatibleTypeException, Exception {
		return loadPathAsStack( strFolder, null );
	}

	public static < T extends RealType< T > & NativeType< T > > Img< DoubleType > loadPathAsStack( final String strFolder, final String filter ) throws ImgIOException, IncompatibleTypeException, Exception {

		final List< Img< DoubleType >> imageList = loadTiffsFromFolder( strFolder, filter );

		Img< DoubleType > stack = null;
		final long width = imageList.get( 0 ).dimension( 0 );
		final long height = imageList.get( 0 ).dimension( 1 );
		final long frames = imageList.size();

		stack = new ArrayImgFactory< DoubleType >().create( new long[] { width, height, frames }, new DoubleType() );

		// Add images to stack...
		int i = 0;
		for ( final RandomAccessible< DoubleType > image : imageList ) {
			final RandomAccessibleInterval< DoubleType > viewZSlize = Views.hyperSlice( stack, 2, i );
			final IterableInterval< DoubleType > iterZSlize = Views.iterable( viewZSlize );

			DataMover.copy( image, iterZSlize );
			Normalize.normalize( iterZSlize, new DoubleType( 0.0 ), new DoubleType( 1.0 ) );
			i++;
		}

		return stack;
	}
}
