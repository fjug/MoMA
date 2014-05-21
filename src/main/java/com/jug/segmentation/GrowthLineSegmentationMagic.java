/**
 *
 */
package com.jug.segmentation;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.SubsampleIntervalView;
import net.imglib2.view.Views;

/**
 * @author jug
 */
public class GrowthLineSegmentationMagic {

	static SilentWekaSegmenter< DoubleType > gapClassifier;
	private static long numSolutions;

	public static void setClassifier( final String folder, final String file ) {
		gapClassifier = new SilentWekaSegmenter< DoubleType >( folder, file );
	}

	public static RandomAccessibleInterval< DoubleType > returnClassification( final RandomAccessibleInterval< DoubleType > rai ) {
		final RandomAccessibleInterval< DoubleType > classified = gapClassifier.classifyPixels( rai, true );

//		ImageJFunctions.show( classified );

		final long[] min = new long[ classified.numDimensions() ];
		classified.min( min );
		// Depending on the class order in our random forest you might need the next line...
//		min[ 2 ]++; 
		final long[] max = new long[ classified.numDimensions() ];
		classified.max( max );
		// TODO: FIXES A BUG IN THE IMGLIB... NEEDS TO BE REMOVED AFTER THE BUG IS REMOVED!!!
		if ( ( max[ 2 ] - min[ 2 ] + 1 ) % 2 == 1 ) {
			max[ 2 ]++;
		}

		final SubsampleIntervalView< DoubleType > subsampleGapClass = ( SubsampleIntervalView< DoubleType > ) Views.subsample( Views.interval( classified, min, max ), 1, 1, 2 );

//		ImageJFunctions.show( subsampleGapClass );

		return subsampleGapClass;
	}

	private static RandomAccessibleInterval< LongType > returnParamaxflowBaby( final RandomAccessibleInterval< DoubleType > rai, final boolean withClassificationOfGaps ) {
		final ParaMaxFlow< DoubleType > paramaxflow = new ParaMaxFlow< DoubleType >( rai, ( withClassificationOfGaps ) ? returnClassification( rai ) : null, false, -1.0, 0.45, 0.15, 1.0, 1.0, 1.0, 0.10, 0.0, 0.5, 10.0, 0.0, 0.5, 10.0 );

		numSolutions = paramaxflow.solve( -1000000, 1000000 );

		final Img< LongType > sumRegions = paramaxflow.getRegionsImg();

//		ImageJFunctions.show( paramaxflow.getUnariesImg() );
//		ImageJFunctions.show( paramaxflow.getBinariesInXImg() );
//		ImageJFunctions.show( paramaxflow.getBinariesInYImg() );
//		ImageJFunctions.show( sumRegions );

		return sumRegions;
	}

	public static RandomAccessibleInterval< LongType > returnParamaxflowRegionSums( final RandomAccessibleInterval< DoubleType > rai ) {
		return returnParamaxflowBaby( rai, false );
	}

	public static RandomAccessibleInterval< LongType > returnClassificationBoostedParamaxflowRegionSums( final RandomAccessibleInterval< DoubleType > rai ) {
		return returnParamaxflowBaby( rai, true );
	}

	public static long getNumSolutions() {
		return numSolutions;
	}

}
