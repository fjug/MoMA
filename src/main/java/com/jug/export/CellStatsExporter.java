/**
 *
 */
package com.jug.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.swing.JOptionPane;

import com.jug.GrowthLineFrame;
import com.jug.MoMA;
import com.jug.gui.DialogCellStatsExportSetup;
import com.jug.gui.MoMAGui;
import com.jug.gui.OsDependentFileChooser;
import com.jug.gui.progress.DialogProgress;
import com.jug.lp.AbstractAssignment;
import com.jug.lp.DivisionAssignment;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.lp.Hypothesis;
import com.jug.lp.MappingAssignment;
import com.jug.util.ComponentTreeUtils;
import com.jug.util.Util;
import com.jug.util.filteredcomponents.FilteredComponent;

import gurobi.GRBException;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * @author jug
 */
public class CellStatsExporter {

	final class SegmentRecord {

		private static final int ENDOFTRACKING = 1234;
		private static final int USER_PRUNING = 4321;

		// Note: if daughterTypeOrPosition is set to a positive value $i$ -- the given cell is the i-th cell in the growth line (with the mother cell being i=1.
		private static final int UNKNOWN = 0;
		private static final int LOWER = -1;
		private static final int UPPER = -2;

		public boolean exists = true;
		public int id = -1;
		public int pid = -1;
		public int tbirth = -1;
		public int daughterTypeOrPosition = SegmentRecord.UNKNOWN;
		public int frame = 0;

		public List< Integer > genealogy;

		public Hypothesis< Component< FloatType, ? >> hyp;
		private int terminated_by = Integer.MIN_VALUE;

		public SegmentRecord(
				final Hypothesis< Component< FloatType, ? >> hyp,
				final int id,
				final int pid,
				final int tbirth,
				final int daughterTypeOrPosition,
				final List< Integer > genealogy ) {
			this.hyp = hyp;
			this.id = id;
			this.pid = pid;
			this.tbirth = tbirth;
			this.daughterTypeOrPosition = daughterTypeOrPosition;
			this.genealogy = genealogy;
			this.frame = 0;
		}

		public SegmentRecord(
				final Hypothesis< Component< FloatType, ? >> hyp,
				final int id,
				final int pid,
				final int tbirth,
				final int daughterTypeOrPosition ) {
			this.hyp = hyp;
			this.id = id;
			this.pid = pid;
			this.tbirth = tbirth;
			this.daughterTypeOrPosition = daughterTypeOrPosition;
			this.genealogy = new ArrayList< Integer >();
			genealogy.add( daughterTypeOrPosition );
			this.frame = 0;
		}

		public SegmentRecord( final SegmentRecord point, final int frameOffset ) {
			this.hyp = point.hyp;
			this.id = point.id;
			this.pid = point.pid;
			this.tbirth = point.tbirth;
			this.daughterTypeOrPosition = point.daughterTypeOrPosition;
			this.frame = point.frame + frameOffset;
			this.genealogy = new ArrayList< Integer >( point.genealogy );
		}

		@Override
		public SegmentRecord clone() {
			final SegmentRecord ret =
					new SegmentRecord( this.hyp, this.id, this.pid, this.tbirth, this.daughterTypeOrPosition, this.genealogy );
			ret.exists = this.exists;
			ret.frame = this.frame;
			ret.terminated_by = this.terminated_by;
			return ret;
		}

		@Override
		public String toString() {
			String dt = "UNKNOWN";
			if ( daughterTypeOrPosition == SegmentRecord.UPPER ) dt = "TOP";
			if ( daughterTypeOrPosition == SegmentRecord.LOWER ) dt = "BOTTOM";
			if ( daughterTypeOrPosition > 0 ) dt = "CELL#" + daughterTypeOrPosition;
			return String.format( "id=%d; pid=%d; birth_frame=%d; daughter_type=%s", id, pid, tbirth, dt );
		}

		/**
		 * @return
		 */
		public SegmentRecord nextSegmentInTime( final GrowthLineTrackingILP ilp ) {
			SegmentRecord ret = this;

			exists = true;
			try {
				final AbstractAssignment< Hypothesis< Component< FloatType, ? >>> rightAssmt = ilp.getOptimalRightAssignment( this.hyp );
				if ( rightAssmt == null ) {
					exists = false;
					terminated_by = SegmentRecord.ENDOFTRACKING;
				} else if ( rightAssmt.getType() == GrowthLineTrackingILP.ASSIGNMENT_MAPPING ) {
					final MappingAssignment ma = ( MappingAssignment ) rightAssmt;
					if ( !ma.isPruned() ) {
						ret = new SegmentRecord( this, 1 );
						ret.hyp = ma.getDestinationHypothesis();
					} else {
						terminated_by = SegmentRecord.USER_PRUNING;
						exists = false;
					}
				} else {
					terminated_by = rightAssmt.getType();
					exists = false;
				}
			} catch ( final GRBException ge ) {
				exists = false;
				System.err.println( ge.getMessage() );
			}
			return ret;
		}

		/**
		 * @return true if the current segment is valid.
		 */
		public boolean exists() {
			return exists;
		}

		/**
		 * @param channel
		 * @return
		 */
		public long[] computeChannelHistogram( final IterableInterval< FloatType > view, final float min, final float max ) {
			final Histogram1d< FloatType > histogram = new Histogram1d< FloatType >( view, new Real1dBinMapper< FloatType >( min, max, 20, false ) );
			return histogram.toLongArray();
		}

		/**
		 * @param channel
		 * @return
		 */
		public float[] computeChannelPercentile( final IterableInterval< FloatType > channel ) {
			final List< Float > pixelVals = new ArrayList< Float >();
			for ( final FloatType ftPixel : channel ) {
				pixelVals.add( ftPixel.get() );
			}
			Collections.sort( pixelVals );

			final int numPercentiles = 20;
			final float ret[] = new float[ numPercentiles - 1 ];
			for ( int i = 1; i < numPercentiles; i++ ) {
				final int index = ( i * pixelVals.size() / numPercentiles ) - 1;
				ret[ i - 1 ] = pixelVals.get( index );
			}
			return ret;
		}

		/**
		 * @param segmentBoxInChannel
		 * @return
		 */
		public float[] computeChannelColumnIntensities( final IntervalView< FloatType > columnBoxInChannel ) {
			if ( MoMA.GL_FLUORESCENCE_COLLECTION_WIDTH_IN_PIXELS != columnBoxInChannel.dimension( 0 ) ) {
				System.out.println( "EXPORT WARNING: intensity columns to be exported are " + columnBoxInChannel.dimension( 0 ) + " instead of " + MoMA.GL_FLUORESCENCE_COLLECTION_WIDTH_IN_PIXELS );
			}

			final float ret[] = new float[ ( int ) columnBoxInChannel.dimension( 0 ) ];
			int idx = 0;
			for ( int i = ( int ) columnBoxInChannel.min( 0 ); i <= columnBoxInChannel.max( 0 ); i++ ) {
				final IntervalView< FloatType > column = Views.hyperSlice( columnBoxInChannel, 0, i );
				ret[ idx ] = 0f;
				for ( final FloatType ftPixel : Views.iterable( column ) ) {
					ret[ idx ] += ftPixel.get();
				}
				idx++;
			}
			return ret;
		}

		/**
		 * @param columnBoxInChannel
		 * @return
		 */
		public float[][] getIntensities( final IntervalView< FloatType > columnBoxInChannel ) {
			final float ret[][] = new float[ ( int ) columnBoxInChannel.dimension( 0 ) ][ ( int ) columnBoxInChannel.dimension( 1 ) ];
			int y = 0;
			for ( int j = ( int ) columnBoxInChannel.min( 1 ); j <= columnBoxInChannel.max( 1 ); j++ ) {
				final IntervalView< FloatType > row = Views.hyperSlice( columnBoxInChannel, 1, j );
				int x = 0;
				for ( final FloatType ftPixel : Views.iterable( row ) ) {
					ret[ x ][ y ] = ftPixel.get();
					x++;
				}
				y++;
			}
			return ret;
		}

		/**
		 * @return
		 */
		public String getGenealogyString() {
			String ret = "";
			for ( final int dt : genealogy ) {
				if ( dt == SegmentRecord.UPPER ) {
					ret = ret + "T";
				} else
				if ( dt == SegmentRecord.LOWER ) {
					ret = ret + "B";
				} else
				if ( dt == SegmentRecord.UNKNOWN ) {
					ret = ret + "U";
				} else {
					ret = ret + dt;
				}
			}
			return ret;
		}
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final MoMAGui gui;
	private boolean doTrackExport = false;
	private boolean doExportUserInputs = true;
	private boolean includeHistograms = true;
	private boolean includeQuantiles = true;
	private boolean includeColIntensitySums = true;
	private boolean includePixelIntensities = false;

	public CellStatsExporter( final MoMAGui gui ) {
		this.gui = gui;
	}

	public boolean showConfigDialog() {
		final DialogCellStatsExportSetup dialog =
				new DialogCellStatsExportSetup( gui, doExportUserInputs, doTrackExport, includeHistograms, includeQuantiles, includeColIntensitySums, includePixelIntensities );
		dialog.ask();
		if ( !dialog.wasCanceled() ) {
			this.doTrackExport = dialog.doExportTracks;
			this.doExportUserInputs = dialog.doExportUserInputs;
			this.includeHistograms = dialog.includeHistograms;
			this.includeQuantiles = dialog.includeQuantiles;
			this.includeColIntensitySums = dialog.includeColIntensitySums;
			this.includePixelIntensities = dialog.includePixelIntensities;
			return true;
		} else {
			return false;
		}
	}

	public void export() {
		if ( !MoMA.HEADLESS ) {
			if ( showConfigDialog() ) {
				final File folderToUse = OsDependentFileChooser.showSaveFolderChooser( gui, MoMA.STATS_OUTPUT_PATH, "Choose export folder..." );
				if ( folderToUse == null ) {
					JOptionPane.showMessageDialog(
							gui,
							"Illegal save location choosen!",
							"Error",
							JOptionPane.ERROR_MESSAGE );
					return;
				}
				if ( doTrackExport ) {
					exportTracks( new File( folderToUse, "ExportedTracks_" + MoMA.getDefaultFilenameDecoration() + ".csv" ) );
				}
				if ( doExportUserInputs ) {
					final int tmin = MoMA.getMinTime();
					final int tmax = MoMA.getMaxTime();
					final File file =
							new File( folderToUse, String.format(
									"--[%d-%d]_%s.timm",
									tmin,
									tmax,
									MoMA.getDefaultFilenameDecoration() ) );
					MoMA.getGui().model.getCurrentGL().getIlp().saveState( file );
				}
				try {
					exportCellStats( new File( folderToUse, "ExportedCellStats_" + MoMA.getDefaultFilenameDecoration() + ".csv" ) );
				} catch ( final GRBException e ) {
					e.printStackTrace();
				}
			}
		} else {
			if ( doTrackExport ) {
				exportTracks( new File( MoMA.STATS_OUTPUT_PATH, "ExportedTracks_" + MoMA.getDefaultFilenameDecoration() + ".csv" ) );
			}
			if ( doExportUserInputs ) {
				final int tmin = MoMA.getMinTime();
				final int tmax = MoMA.getMaxTime();
				final File file =
						new File( MoMA.STATS_OUTPUT_PATH, String.format(
								"--[%d-%d]_%s.timm",
								tmin,
								tmax,
								MoMA.getDefaultFilenameDecoration() ) );
				MoMA.getGui().model.getCurrentGL().getIlp().saveState( file );
			}

			try {
				exportCellStats( new File( MoMA.STATS_OUTPUT_PATH, "ExportedCellStats_" + MoMA.getDefaultFilenameDecoration() + ".csv" ) );
			} catch ( final GRBException e ) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param file
	 * @throws GRBException
	 */
	public void exportCellStats( final File file ) throws GRBException {

		// ------- THE MAGIC *** THE MAGIC *** THE MAGIC *** THE MAGIG -------
		final Vector< String > linesToExport = getCellStatsExportData();
		// -------------------------------------------------------------------

		System.out.println( "Exporting collected cell-statistics..." );
		Writer out = null;
		try {
			out = new OutputStreamWriter( new FileOutputStream( file ) );

			for ( final String line : linesToExport ) {
				out.write( line );
				out.write( "\n" );
			}
			out.close();
		} catch ( final FileNotFoundException e1 ) {
			JOptionPane.showMessageDialog( gui, "File not found!", "Error!", JOptionPane.ERROR_MESSAGE );
			e1.printStackTrace();
		} catch ( final IOException e1 ) {
			JOptionPane.showMessageDialog( gui, "Selected file could not be written!", "Error!", JOptionPane.ERROR_MESSAGE );
			e1.printStackTrace();
		}
		System.out.println( "...done!" );
	}

	private Vector< String > getCellStatsExportData() throws GRBException {
		// use US-style number formats! (e.g. '.' as decimal point)
		Locale.setDefault( new Locale( "en", "US" ) );

		final String loadedDataFolder = MoMA.props.getProperty( "import_path", "BUG -- could not get property 'import_path' while exporting cell statistics..." );
		final int numCurrGL = gui.sliderGL.getValue();
		final Vector< String > linesToExport = new Vector< String >();

		final GrowthLineFrame firstGLF = gui.model.getCurrentGL().getFrames().get( 0 );
		final GrowthLineTrackingILP ilp = firstGLF.getParent().getIlp();
		final Vector< ValuePair< Integer, Hypothesis< Component< FloatType, ? > > > > segmentsInFirstFrameSorted =
				firstGLF.getSortedActiveHypsAndPos();
		final List< SegmentRecord > startingPoints = new ArrayList< SegmentRecord >();

		int nextCellId = 0;
		final LinkedList< SegmentRecord > queue = new LinkedList< SegmentRecord >();

		int cellNum = 0;
		for ( final ValuePair< Integer, Hypothesis< Component< FloatType, ? > > > valuePair : segmentsInFirstFrameSorted ) {

			cellNum++;
			final SegmentRecord point =
					new SegmentRecord( valuePair.b, nextCellId++, -1, -1, cellNum );
			startingPoints.add( point );

			final SegmentRecord prepPoint = new SegmentRecord( point, 1 );
			prepPoint.hyp = point.hyp;

			if ( !prepPoint.hyp.isPruned() ) {
				queue.add( prepPoint );
			}
		}
		while ( !queue.isEmpty() ) {
			final SegmentRecord prepPoint = queue.poll();

			final AbstractAssignment< Hypothesis< Component< FloatType, ? >>> rightAssmt = ilp.getOptimalRightAssignment( prepPoint.hyp );

			if ( rightAssmt == null ) {
				continue;
			}
			// MAPPING -- JUST DROP SEGMENT STATS
			if ( rightAssmt.getType() == GrowthLineTrackingILP.ASSIGNMENT_MAPPING ) {
				final MappingAssignment ma = ( MappingAssignment ) rightAssmt;
				final SegmentRecord next = new SegmentRecord( prepPoint, 1 );
				next.hyp = ma.getDestinationHypothesis();
				if ( !prepPoint.hyp.isPruned() ) {
					queue.add( next );
				}
			}
			// DIVISON -- NEW CELLS ARE BORN CURRENT ONE ENDS
			if ( rightAssmt.getType() == GrowthLineTrackingILP.ASSIGNMENT_DIVISION ) {
				final DivisionAssignment da = ( DivisionAssignment ) rightAssmt;

				prepPoint.pid = prepPoint.id;
				prepPoint.tbirth = prepPoint.frame;

				prepPoint.id = nextCellId;
				prepPoint.hyp = da.getLowerDesinationHypothesis();
				prepPoint.daughterTypeOrPosition = SegmentRecord.LOWER;
				if ( !prepPoint.hyp.isPruned() && !( prepPoint.tbirth > gui.sliderTime.getMaximum() ) ) {
					final SegmentRecord newPoint = new SegmentRecord( prepPoint, 0 );
					newPoint.genealogy.add( SegmentRecord.LOWER );
					startingPoints.add( newPoint.clone() );
					newPoint.frame++;
					queue.add( newPoint );
					nextCellId++;
				}

				prepPoint.id = nextCellId;
				prepPoint.hyp = da.getUpperDesinationHypothesis();
				prepPoint.daughterTypeOrPosition = SegmentRecord.UPPER;
				if ( !prepPoint.hyp.isPruned() && !( prepPoint.tbirth > gui.sliderTime.getMaximum() ) ) {
					final SegmentRecord newPoint = new SegmentRecord( prepPoint, 0 );
					newPoint.genealogy.add( SegmentRecord.UPPER );
					startingPoints.add( newPoint.clone() );
					newPoint.frame++;
					queue.add( newPoint );
					nextCellId++;
				}
			}
		}

		// INITIALIZE PROGRESS-BAR if not run headless
		final DialogProgress dialogProgress = new DialogProgress( gui, "Exporting selected cell-statistics...", startingPoints.size() );
		if ( !MoMA.HEADLESS ) {
			dialogProgress.setVisible( true );
		}

		// Line 1: import folder
		linesToExport.add( loadedDataFolder );

		// Line 2: GL-id
		linesToExport.add( "GLidx = " + numCurrGL );

		// Line 3: #cells
		linesToExport.add( "numCells = " + startingPoints.size() );

		// Line 4: #channels
		linesToExport.add( "numChannels = " + MoMA.instance.getRawChannelImgs().size() );

		// Line 5: imageHeight
		final long h = MoMA.instance.getImgRaw().dimension( 1 );
		linesToExport.add( "imageHeight = " + h + "\n" );

		// Line 6: bottomOffset
		linesToExport.add(
				"glHeight = " + ( h - MoMA.GL_OFFSET_BOTTOM - MoMA.GL_OFFSET_TOP ) + "\n" );

		// Line 7: track region (pixel row interval we perform tracking within -- this is all but top and bottom offset areas)
		linesToExport.add( String.format("trackRegionInterval = [%d,%d]", MoMA.GL_OFFSET_TOP, h - 1 - MoMA.GL_OFFSET_BOTTOM ) );

		// Export all cells (we found all their starting segments above)
		for ( int cid = 0; cid < startingPoints.size(); cid++ ) {
			SegmentRecord segmentRecord = startingPoints.get( cid );

			linesToExport.add( segmentRecord.toString() );
			do {
				ValuePair< Integer, Integer > limits =
						ComponentTreeUtils.getTreeNodeInterval( segmentRecord.hyp.getWrappedHypothesis() );
				if ( segmentRecord.hyp.getWrappedHypothesis() instanceof FilteredComponent ) {
					limits = ComponentTreeUtils.getExtendedTreeNodeInterval( ( FilteredComponent< ? > ) segmentRecord.hyp.getWrappedHypothesis() );
				}

				final GrowthLineFrame glf = gui.model.getCurrentGL().getFrames().get( segmentRecord.frame );
				final List< Point > centerLine = glf.getImgLocations();
				final double height = Util.evaluatePolygonLength( centerLine, limits.getA(), limits.getB() );

				final int numCells = glf.getSolutionStats_numCells();
				final int cellPos = glf.getSolutionStats_cellPos( segmentRecord.hyp );

				final String genealogy = segmentRecord.getGenealogyString();

				// WARNING -- if you change substring 'frame' you need also to change the last-row-deletion procedure below for the ENDOFTRACKING case... yes, this is not clean... ;)
				linesToExport.add( String.format(
						"\tframe=%d; pos_in_GL=[%d,%d]; pixel_limits=[%d,%d]; cell_height=%.2f; num_pixels_in_box=%d; genealogy=%s",
						segmentRecord.frame,
						cellPos,
						numCells,
						limits.getA(),
						limits.getB(),
						height,
						Util.getSegmentBoxPixelCount( segmentRecord.hyp, firstGLF.getAvgXpos() ),
						genealogy ) );

				// export info per image channel
				for ( int c = 0; c < MoMA.instance.getRawChannelImgs().size(); c++ ) {
					final IntervalView< FloatType > channelFrame = Views.hyperSlice( MoMA.instance.getRawChannelImgs().get( c ), 2, segmentRecord.frame );
					final IterableInterval< FloatType > segmentBoxInChannel = Util.getSegmentBoxInImg( channelFrame, segmentRecord.hyp, firstGLF.getAvgXpos() );

					final FloatType min = new FloatType();
					final FloatType max = new FloatType();
					Util.computeMinMax( segmentBoxInChannel, min, max );

					if ( includeHistograms ) {
						final long[] hist = segmentRecord.computeChannelHistogram( segmentBoxInChannel, min.get(), max.get() );
						String histStr = String.format( "\t\tch=%d; output=HISTOGRAM", c );
						histStr += String.format( "; min=%8.3f; max=%8.3f", min.get(), max.get() );
						for ( final long value : hist ) {
							histStr += String.format( "; %5d", value );
						}
						linesToExport.add( histStr );
					}

					if ( includeQuantiles ) {
						final float[] percentile = segmentRecord.computeChannelPercentile( segmentBoxInChannel );
						String percentileStr = String.format( "\t\tch=%d; output=PERCENTILES", c );
						percentileStr += String.format( "; min=%8.3f; max=%8.3f", min.get(), max.get() );
						for ( final float value : percentile ) {
							percentileStr += String.format( "; %8.3f", value );
						}
						linesToExport.add( percentileStr );
					}

					if ( includeColIntensitySums ) {
						final IntervalView< FloatType > columnBoxInChannel = Util.getColumnBoxInImg( channelFrame, segmentRecord.hyp, firstGLF.getAvgXpos() );
						final float[] column_intensities = segmentRecord.computeChannelColumnIntensities( columnBoxInChannel );
						String colIntensityStr = String.format( "\t\tch=%d; output=COLUMN_INTENSITIES", c );
						for ( final float value : column_intensities ) {
							colIntensityStr += String.format( "; %.3f", value );
						}
						linesToExport.add( colIntensityStr );
					}

					if ( includePixelIntensities ) {
						final IntervalView< FloatType > intensityBoxInChannel = Util.getIntensityBoxInImg( channelFrame, segmentRecord.hyp, firstGLF.getAvgXpos() );
						final float[][] intensities = segmentRecord.getIntensities( intensityBoxInChannel );
						String intensityStr = String.format( "\t\tch=%d; output=PIXEL_INTENSITIES", c );
						for ( int y = 0; y < intensities[ 0 ].length; y++ ) {
							for ( int x = 0; x < intensities.length; x++ ) {
								intensityStr += String.format( ";%.3f", intensities[ x ][ y ] );
							}
							intensityStr += " ";
						}
						linesToExport.add( intensityStr );
					}
				}
				segmentRecord = segmentRecord.nextSegmentInTime( ilp );
			}
			while ( segmentRecord.exists() );

			if ( segmentRecord.terminated_by == GrowthLineTrackingILP.ASSIGNMENT_EXIT ) {
				linesToExport.add( "\tEXIT\n" );
			} else if ( segmentRecord.terminated_by == GrowthLineTrackingILP.ASSIGNMENT_DIVISION ) {
				linesToExport.add( "\tDIVISION\n" );
			} else if ( segmentRecord.terminated_by == SegmentRecord.USER_PRUNING ) {
				linesToExport.add( "\tUSER_PRUNING\n" );
			} else if ( segmentRecord.terminated_by == SegmentRecord.ENDOFTRACKING ) {
//				// UGLY TRICK ALERT: remember the trick to fix the tracking towards the last frame?
//				// Yes, we double the last frame. This also means that we should not export this fake frame, ergo we remove it here!
				String deleted = "";
				do {
					deleted = linesToExport.remove( linesToExport.size() - 1 );
				}
				while ( !deleted.trim().startsWith( "frame" ) );
				linesToExport.add( "\tENDOFDATA\n" );
			} else {
				linesToExport.add( "\tGUROBI_EXCEPTION\n" );
			}

			// REPORT PROGRESS if needbe
			if ( !MoMA.HEADLESS ) {
				dialogProgress.hasProgressed();
			}
		}

		// Dispose ProgressBar in needbe
		if ( !MoMA.HEADLESS ) {
			dialogProgress.setVisible( false );
			dialogProgress.dispose();
		}

		return linesToExport;
	}

	public void exportTracks( final File file ) {

		final Vector< Vector< String >> dataToExport = getTracksExportData();

		System.out.println( "Exporting data..." );
		Writer out = null;
		try {
			out = new OutputStreamWriter( new FileOutputStream( file ) );

			for ( final Vector< String > rowInData : dataToExport ) {
				for ( final String datum : rowInData ) {
					out.write( datum + ",\t " );
				}
				out.write( "\n" );
			}
			out.close();
		} catch ( final FileNotFoundException e1 ) {
			if ( !MoMA.HEADLESS )
				JOptionPane.showMessageDialog( gui, "File not found!", "Error!", JOptionPane.ERROR_MESSAGE );
			System.err.println( "Export Error: File not found!" );
			e1.printStackTrace();
		} catch ( final IOException e1 ) {
			if ( !MoMA.HEADLESS )
				JOptionPane.showMessageDialog( gui, "Selected file could not be written!", "Error!", JOptionPane.ERROR_MESSAGE );
			System.err.println( "Export Error: Selected file could not be written!" );
			e1.printStackTrace();
		}
		System.out.println( "...done!" );
	}

	private Vector< Vector< String >> getTracksExportData() {

		// use US-style number formats! (e.g. '.' as decimal point)
		Locale.setDefault( new Locale( "en", "US" ) );

		final String loadedDataFolder = MoMA.props.getProperty( "import_path", "BUG -- could not get property 'import_path' while exporting tracks..." );
		final int numCurrGL = gui.sliderGL.getValue();
		final int numGLFs = gui.model.getCurrentGL().getFrames().size();
		final Vector< Vector< String >> dataToExport = new Vector< Vector< String >>();

		final Vector< String > firstLine = new Vector< String >();
		firstLine.add( loadedDataFolder );
		dataToExport.add( firstLine );
		final Vector< String > secondLine = new Vector< String >();
		secondLine.add( "" + numCurrGL );
		secondLine.add( "" + numGLFs );
		dataToExport.add( secondLine );

		int i = 0;
		for ( final GrowthLineFrame glf : gui.model.getCurrentGL().getFrames() ) {
			final Vector< String > newRow = new Vector< String >();
			newRow.add( "" + i );

			final int numCells = glf.getSolutionStats_numCells();
			final Vector< ValuePair< ValuePair< Integer, Integer >, ValuePair< Integer, Integer > >> data = glf.getSolutionStats_limitsAndRightAssType();

			newRow.add( "" + numCells );
			for ( final ValuePair< ValuePair< Integer, Integer >, ValuePair< Integer, Integer > > elem : data ) {
				final int min = elem.a.a.intValue();
				final int max = elem.a.b.intValue();
				final int type = elem.b.a.intValue();
				final int user_touched = elem.b.b.intValue();
				newRow.add( String.format( "%3d, %3d, %3d, %3d", min, max, type, user_touched ) );
			}

			dataToExport.add( newRow );
			i++;
		}

		return dataToExport;
	}

}
