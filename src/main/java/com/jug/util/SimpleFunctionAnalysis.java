/**
 *
 */
package com.jug.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.imglib2.util.ValuePair;

/**
 * @author jug
 * 
 */
public class SimpleFunctionAnalysis {

	/**
	 * Takes float function values and returns the location of all maxima.
	 * that have some minimal specified lateral extent (see monotonicity
	 * parameters '*FlankWidth').
	 * 
	 * @param fktValues
	 *            - discrete float function values.
	 * @param minLeftFlankWidth
	 *            - a local maximum will only be considered if at least
	 *            the given number of lefthand values are strict monotone
	 *            increasing towards the center.
	 * @param minRightFlankWidth
	 *            - a local maximum will only be considered if at least
	 *            the given number of righthand values are strict monotone
	 *            decreasing.
	 * @return
	 */
	public static int[] getMaxima( final float[] fktValues, final int minLeftFlankWidth, final int minRightFlankWidth ) {
		final List< Integer > retInt = new ArrayList< Integer >();
		for ( int center = minLeftFlankWidth; center < fktValues.length - minRightFlankWidth; center++ ) {
			boolean maximum = true;
			int cmp;
			for ( cmp = center - minLeftFlankWidth; cmp <= center + minRightFlankWidth; cmp++ ) {
				if ( center == cmp ) {
					continue;
				}
				if ( fktValues[ cmp ] > fktValues[ center ] ) {
					maximum = false;
					break;
				}
			}
			if ( maximum ) {
				retInt.add( new Integer( center ) );
				center += minRightFlankWidth;
			}
		}

		// build int array to return it
		final int[] ret = new int[ retInt.size() ];
		for ( int i = 0; i < retInt.size(); i++ ) {
			ret[ i ] = retInt.get( i ).intValue();
		}
		return ret;

	}

	/**
	 * Takes float function values and returns the heights of all maxima and
	 * all minima
	 * that have some minimal specified lateral extent (see monotonicity
	 * parameters '*FlankWidth').
	 * 
	 * @param fktValues
	 *            - discrete float function values.
	 * @param minLeftFlankWidth
	 *            - a local maximum will only be considered if at least
	 *            the given number of lefthand values are strict monotone
	 *            increasing towards the center.
	 * @param minRightFlankWidth
	 *            - a local maximum will only be considered if at least
	 *            the given number of righthand values are strict monotone
	 *            decreasing.
	 * @return
	 */
	public static float[] getExteremalPointHeights( final float[] fktValues, final int minLeftFlankWidth, final int minRightFlankWidth ) {

		final List< Float > retFloat = new ArrayList< Float >();
		for ( int center = minLeftFlankWidth; center < fktValues.length - minRightFlankWidth; center++ ) {
			boolean maximum = true;
			int cmp;
			for ( cmp = center - minLeftFlankWidth; cmp <= center + minRightFlankWidth; cmp++ ) {
				if ( center == cmp ) {
					continue;
				}
				if ( fktValues[ cmp ] > fktValues[ center ] ) {
					maximum = false;
					break;
				}
			}
			if ( maximum ) {
				retFloat.add( new Float( -fktValues[ center ] ) ); // tricky '-' for decreasing sort oder
				center += minRightFlankWidth;
			}
		}
		for ( int center = minLeftFlankWidth; center < fktValues.length - minRightFlankWidth; center++ ) {
			boolean minimum = true;
			int cmp;
			for ( cmp = center - minLeftFlankWidth; cmp <= center + minRightFlankWidth; cmp++ ) {
				if ( center == cmp ) {
					continue;
				}
				if ( fktValues[ cmp ] < fktValues[ center ] ) {
					minimum = false;
					break;
				}
			}
			if ( minimum ) {
				retFloat.add( new Float( -fktValues[ center ] ) ); // tricky '-' for decreasing sort oder
				center += minRightFlankWidth;
			}
		}
		Collections.sort( retFloat );

		// build float array to return it
		final float[] ret = new float[ retFloat.size() ];
		for ( int i = 0; i < retFloat.size(); i++ ) {
			ret[ i ] = -retFloat.get( i ).floatValue(); // undo the decreasing sort oder trick (see above)
		}
		return ret;
	}

	public static ValuePair< Integer, Float > getMin( final float[] fktValues ) {
		return getMin( fktValues, 0, fktValues.length - 1 );
	}

	public static ValuePair< Integer, Float > getMin( final float[] fktValues, final int from, final int to ) {
		int minPos = from;
		float min = fktValues[ from ];
		for ( int i = from; i <= to; i++ ) {
			if ( min > fktValues[ i ] ) {
				minPos = i;
				min = fktValues[ i ];
			}
		}
		return new ValuePair< Integer, Float >( Integer.valueOf( minPos ), Float.valueOf( min ) );
	}

	public static ValuePair< Integer, Float > getMax( final float[] fktValues ) {
		return getMax( fktValues, 0, fktValues.length - 1 );
	}

	public static ValuePair< Integer, Float > getMax( final float[] fktValues, final int from, final int to ) {
		int maxPos = from;
		float max = fktValues[ from ];
		for ( int i = from; i <= to; i++ ) {
			if ( max < fktValues[ i ] ) {
				maxPos = i;
				max = fktValues[ i ];
			}
		}
		return new ValuePair< Integer, Float >( Integer.valueOf( maxPos ), Float.valueOf( max ) );
	}

	public static ValuePair< Integer, Float > getLefthandLocalMin( final float[] fktValues, final int idx ) {
		return getLefthandLocalMinOrPlateau( fktValues, idx, 0.0f );
	}

	public static ValuePair< Integer, Float > getLefthandLocalMinOrPlateau( final float[] fktValues, final int idx, final float plateauDerivativeThreshold ) {
		int i = idx;
		while ( i > 0 && fktValues[ i - 1 ] + plateauDerivativeThreshold <= fktValues[ i ] ) {
			i--;
		}
		if ( i > 0 ) i--; // since we really want the min
		return new ValuePair< Integer, Float >( Integer.valueOf( i ), Float.valueOf( fktValues[ i ] ) );
	}

	public static ValuePair< Integer, Float > getLefthandLocalMax( final float[] fktValues, final int idx ) {
		int i = idx;
		while ( i > 0 && fktValues[ i - 1 ] >= fktValues[ i ] ) {
			i--;
		}
		if ( i > 0 ) i--; // since we really want the max
		return new ValuePair< Integer, Float >( Integer.valueOf( i ), Float.valueOf( fktValues[ i ] ) );
	}

	public static ValuePair< Integer, Float > getRighthandLocalMin( final float[] fktValues, final int idx ) {
		return getRighthandLocalMinOrPlateau( fktValues, idx, 0.0f );
	}

	public static ValuePair< Integer, Float > getRighthandLocalMinOrPlateau( final float[] fktValues, final int idx, final float plateauDerivativeThreshold ) {
		int i = idx;
		while ( i + 1 < fktValues.length && fktValues[ i ] >= fktValues[ i + 1 ] + plateauDerivativeThreshold ) {
			i++;
		}
		if ( i + 1 < fktValues.length ) i++; // since we really want the min
		return new ValuePair< Integer, Float >( Integer.valueOf( i ), Float.valueOf( fktValues[ i ] ) );
	}

	public static ValuePair< Integer, Float > getRighthandLocalMax( final float[] fktValues, final int idx ) {
		int i = idx;
		while ( i + 1 < fktValues.length && fktValues[ i ] <= fktValues[ i + 1 ] ) {
			i++;
		}
		if ( i + 1 < fktValues.length ) i++; // since we really want the max
		return new ValuePair< Integer, Float >( Integer.valueOf( i ), Float.valueOf( fktValues[ i ] ) );
	}

	public static float[] normalizeFloatArray( final float[] array, final float min, final float max ) {
		final float valMin = getMin( array, 0, array.length - 1 ).b;
		final float valMax = getMax( array, 0, array.length - 1 ).b;

		final float[] ret = new float[ array.length ];
		for ( int i = 0; i < array.length; i++ ) {
			ret[ i ] = ( array[ i ] - valMin ) / ( valMax - valMin ) * ( max - min ); // normalized in [0,max-min]
			ret[ i ] += min; // normalized in [min,max];
			i++;
		}

		return ret;
	}

	public static float[] differentiateFloatArray( final float[] array ) {
		return differentiateFloatArray( array, 1 );
	}

	public static float[] differentiateFloatArray( final float[] array, final int span ) {
		final float[] ret = new float[ array.length - 2 * span ];
		for ( int i = span; i < array.length - span; i++ ) {
			ret[ i - span ] = .5f * ( ( array[ i - span ] - array[ i ] ) + ( array[ i ] - array[ i + span ] ) );
		}
		return ret;
	}

	public static float[] filterAbove( final float[] array, final float threshold ) {
		final float[] ret = new float[ array.length ];
		for ( int i = 0; i < array.length; i++ ) {
			ret[ i ] = ( array[ i ] > threshold ) ? array[ i ] : threshold;
		}
		return ret;
	}

	/**
	 * @param intensities
	 * @return
	 */
	public static float getSum( final float[] fktValues ) {
		return getSum( fktValues, 0, fktValues.length - 1 );
	}

	public static float getSum( final float[] fktValues, final int from, final int to ) {
		float sum = 0;
		for ( int i = from; i <= to; i++ ) {
			sum += fktValues[ i ];
		}
		return sum;
	}

	/**
	 * @param fktValues
	 * @return
	 */
	public static float getAvg( final float[] fktValues ) {
		return getSum( fktValues, 0, fktValues.length - 1 ) / fktValues.length;
	}

	/**
	 * @param fktValues
	 * @param from
	 * @param to
	 * @return
	 */
	public static float getAvg( final float[] fktValues, final int from, final int to ) {
		return getSum( fktValues, from, to ) / ( to - from + 1 );
	}

	/**
	 * @param fkt
	 * @return
	 */
	public static int[] findMarbleValleySegmentation( final float[] fkt ) {
		// border case
		if ( fkt.length == 0 ) return new int[ 0 ];

		final ArrayList< Integer > valleyBorders = new ArrayList< Integer >();

		final float energyLeakageFactor = 0.33f;
		final float initialEnergy = 0.02f;

		final int RUN_RIGHT = 0;
		final int RUN_LEFT = 1;
		final int CLIMB_RIGHT = 2;
		final int CLIMB_LEFT = 3;
		int state = RUN_RIGHT;

		final int[] valleys = new int[ fkt.length ];
		int valleyId = 1;
		float energy = initialEnergy;

		int idx = 0;
//	System.out.print("0>");
		int rightBorder = 0;
		while ( idx < fkt.length - 1 ) {
			final float deltaH = getHeightDifference( fkt, idx, idx + 1 );

			if ( state == RUN_RIGHT ) {
				if ( deltaH <= energy ) {
					valleys[ idx ] = valleyId;
					if ( deltaH <= 0 ) {
						energy += -deltaH * ( 1.0 - energyLeakageFactor ); // downwards we accumulate some energy
					} else {
						energy -= deltaH; // that we loose again going upwards!
					}
					idx++;
					continue;
				} else {
					state = CLIMB_RIGHT;
//		    System.out.print(""+idx+">>");
					continue;
				}
			}

			if ( state == CLIMB_RIGHT ) {
				if ( deltaH >= 0 ) {
					valleys[ idx ] = valleyId;
					idx++;
					continue;
				} else {
					valleys[ idx ] = valleyId;
					rightBorder = idx;
					idx--;
					state = RUN_LEFT;
//		    System.out.print(""+idx+"<");
					energy = 0;
					continue;
				}
			}

			if ( state == RUN_LEFT ) {
				if ( -deltaH <= energy && idx > 0 ) {
					valleys[ idx ] = valleyId;
					if ( deltaH >= 0 ) {
						energy += deltaH * ( 1.0 - energyLeakageFactor ); // downwards we accumulate some energy
					} else {
						energy -= -deltaH; // that we loose again going upwards!
					}
					idx--;
					continue;
				} else {
					if ( idx == 0 ) {
						valleys[ idx ] = valleyId;
						valleyId++;
						idx = rightBorder;
						state = RUN_RIGHT;
//			System.out.print(" ; "+idx+">");
						energy = initialEnergy;
					} else {
						state = CLIMB_LEFT;
//			System.out.print(""+idx+"<<");
					}
					continue;
				}
			}

			if ( state == CLIMB_LEFT ) {
				if ( deltaH <= 0 && idx > 0 ) {
					valleys[ idx ] = valleyId;
					idx--;
					continue;
				} else {
					if ( idx == 0 ) {
						valleys[ 0 ] = valleyId;
					}
					valleyId++;
					idx = rightBorder;
					state = RUN_RIGHT;
//		    System.out.print(" ; "+idx+">");
					energy = initialEnergy;
					continue;
				}
			}
		}
//	System.out.println("");

		// find the marble-value borders
		valleyBorders.add( new Integer( 0 ) );
		valleyId = valleys[ 0 ];
		for ( int i = 0; i < valleys.length - 1; i++ ) { // -1 is just because above I was to lazy to make a meaningful entry at the last position
			if ( valleyId != valleys[ i ] ) {
				valleyId = valleys[ i ];
				valleyBorders.add( new Integer( i ) );
			}
		}
		valleyBorders.add( new Integer( valleys.length - 1 ) );

		// convert to int[]
		final int[] ret = new int[ valleyBorders.size() ];
		for ( int i = 0; i < ret.length; i++ ) {
			ret[ i ] = valleyBorders.get( i ).intValue();
		}
		return ret;
	}

	/**
	 * @param fkt
	 * @param i
	 * @param j
	 * @return
	 */
	private static float getHeightDifference( final float[] fkt, final int i, final int j ) {
		assert ( i <= j );
		return ( fkt[ j ] - fkt[ i ] );
	}

	/**
	 * @param fkt
	 * @param i
	 * @param j
	 * @return
	 */
	private static float getAvgSteepness( final float[] fkt, final int i, final int j ) {
		assert ( i <= j );
		return ( fkt[ j ] - fkt[ i ] ) / ( j - i );
	}

	/**
	 * @param fkt
	 * @return
	 */
	public static float[] flipSign( final float[] fkt ) {
		final float[] ret = new float[ fkt.length ];
		for ( int i = 0; i < fkt.length; i++ ) {
			ret[ i ] = fkt[ i ] * -1;
		}
		return ret;
	}

	/**
	 * @param i
	 * @param j
	 * @param fkt
	 * @return
	 */
	public static ValuePair< Integer, Integer > getHighestMonotoneIncreasingSegment( final float[] fkt, final int i, final int j ) {
		int idxStart = i;
		int idxEnd = j;

		int idxLatestStart = i;
		float maxHeight = 0;

		for ( int idx = i; idx < j; idx++ ) {
			final float deltaH = getHeightDifference( fkt, idx, idx + 1 );
			if ( deltaH < 0 || idx == j - 1 ) {
				final float height = getHeightDifference( fkt, idxLatestStart, idx );
				if ( maxHeight < height ) {
					maxHeight = height;
					idxStart = idxLatestStart;
					idxEnd = idx;
				}
				idxLatestStart = idx;
				continue;
			}
		}
		if ( idxEnd == j - 1 && getHeightDifference( fkt, j - 1, j ) >= 0 ) {
			idxEnd = j;
		}

		return new ValuePair< Integer, Integer >( new Integer( idxStart ), new Integer( idxEnd ) );
	}

	/**
	 * @param i
	 * @param j
	 * @param fkt
	 * @return
	 */
	public static ValuePair< Integer, Integer > getHighestMonotoneDecreasingSegment( final float[] fkt, final int i, final int j ) {
		final float[] inverseFkt = SimpleFunctionAnalysis.flipSign( fkt );
		return getHighestMonotoneIncreasingSegment( inverseFkt, i, j );
	}

	/**
	 * @param gapSepFkt
	 * @return
	 */
	public static float getMedian( final float[] fkt, final int i, final int j ) {
		final int len = j - i + 1;
		final float[] fktCopy = new float[ len ];
		System.arraycopy( fkt, i, fktCopy, 0, len );
		Arrays.sort( fktCopy );
		return fktCopy[ len / 2 ];
	}

	/**
	 * @param offset
	 *            the number that is added to each element of fkt
	 * @return
	 */
	public static float[] elementWiseAdd( final float[] fkt, final float offset ) {
		final float[] ret = new float[ fkt.length ];
		for ( int i = 0; i < fkt.length; i++ ) {
			ret[ i ] = fkt[ i ] + offset;
		}
		return ret;
	}

}
