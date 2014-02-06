package com.jug;

/**
 * Main class for the MotherMachine project.
 */

import ij.ImageJ;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import net.imglib2.Cursor;
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
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.jug.gui.JFrameSnapper;
import com.jug.gui.MotherMachineGui;
import com.jug.gui.MotherMachineModel;
import com.jug.loops.Loops;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.ops.cursor.FindLocalMaxima;
import com.jug.ops.cursor.FindLocationAboveThreshold;
import com.jug.ops.numerictype.VarOfRai;
import com.jug.util.DataMover;
import com.jug.util.DoubleTypeImgLoader;


/**
 * @author jug
 */
public class MotherMachine {

	// -------------------------------------------------------------------------------------
	// statics
	// -------------------------------------------------------------------------------------
	/**
	 * Parameter: sigma for gaussian blurring in x-direction of the raw image
	 * data. Used while searching the growth line centers.
	 */
	public static double SIGMA_GL_DETECTION_X = 15.0;
	public static double SIGMA_GL_DETECTION_Y = 3.0;
	/**
	 * Parameter: sigma for gaussian blurring in x-direction of the raw image
	 * data. Used while searching the gaps between bacteria.
	 */
	private static double SIGMA_PRE_SEGMENTATION_X = 0.0; // 3.5;
	private static double SIGMA_PRE_SEGMENTATION_Y = 0.0; // 0.5;
	/**
	 * Parameter: later border in pixels - well centers detected too close to
	 * the left and right image border will be neglected. Reason: detection not
	 * reliable if well is truncated.
	 */
	public static int GL_OFFSET_LATERAL = 5;
	/**
	 * Prior knowledge: hard offset in detected well center lines - will be cut
	 * of from top.
	 */
	public static int GL_OFFSET_TOP = 40;
	/**
	 * Prior knowledge: hard offset in detected well center lines - will be cut
	 * of from bottom.
	 */
	public static int GL_OFFSET_BOTTOM = 10;
	/**
	 * Maximum offset in x direction (with respect to growth line center) to
	 * take the background intensities from that will be subtracted from the
	 * growth line.
	 */
	private static int BGREM_TEMPLATE_XMAX = 35;
	/**
	 * Minimum offset in x direction (with respect to growth line center) to
	 * take the background intensities from that will be subtracted from the
	 * growth line.
	 */
	private static int BGREM_TEMPLATE_XMIN = 20;
	/**
	 * Offsets in +- x direction (with respect to growth line center) where the
	 * measured background values will be subtracted from.
	 */
	private static int BGREM_X_OFFSET = 35;
	/**
	 * Prior knowledge: minimal length of detected cells
	 */
	public static int MIN_CELL_LENGTH = 25;
	/**
	 * Prior knowledge: minimal contrast of an gap (also used for MSERs)
	 */
	public static double MIN_GAP_CONTRAST = 0.02; // This is set to a very low value that will basically not filter anything...

	// - - - - - - - - - - - - - -
	// GUI-WINDOW RELATED STATICS
	// - - - - - - - - - - - - - -
	/**
	 * The <code>JFrame</code> containing the main GUI.
	 */
	private static JFrame guiFrame;
	/**
	 * Properties to configure app (loaded and saved to properties file!).
	 */
	private static Properties props;
	/**
	 * Default x-position of the main GUI-window.
	 * This value will be used if the values in the properties file are not
	 * fitting on any of the currently attached screens.
	 */
	private static int DEFAULT_GUI_POS_X = 100;
	/**
	 * X-position of the main GUI-window. This value will be loaded from and
	 * stored in the properties file!
	 */
	private static int GUI_POS_X;
	/**
	 * Default y-position of the main GUI-window.
	 * This value will be used if the values in the properties file are not
	 * fitting on any of the currently attached screens.
	 */
	private static int DEFAULT_GUI_POS_Y = 100;
	/**
	 * Y-position of the main GUI-window. This value will be loaded from and
	 * stored in the properties file!
	 */
	private static int GUI_POS_Y;
	/**
	 * Width (in pixels) of the main GUI-window. This value will be loaded from
	 * and stored in the properties file!
	 */
	private static int GUI_WIDTH = 800;
	/**
	 * Width (in pixels) of the main GUI-window. This value will be loaded from
	 * and stored in the properties file!
	 */
	private static int GUI_HEIGHT = 630;
	/**
	 * Width (in pixels) of the console window. This value will be loaded from
	 * and stored in the properties file!
	 */
	private static int GUI_CONSOLE_WIDTH = 600;
	/**
	 * The path to usually open JFileChoosers at (except for initial load
	 * dialog).
	 */
	public static String DEFAULT_PATH = System.getProperty( "user.home" );

	// ====================================================================================================================

	/**
	 * PROJECT MAIN
	 * ============
	 *
	 * @param args
	 *            muh!
	 */
	public static void main( final String[] args ) {
		try {

			final MotherMachine main = new MotherMachine();
			guiFrame = new JFrame( "Interactive MotherMachine" );
			main.initMainWindow( guiFrame );

			props = main.loadParams();
			BGREM_TEMPLATE_XMIN = Integer.parseInt( props.getProperty( "BGREM_TEMPLATE_XMIN", Integer.toString( BGREM_TEMPLATE_XMIN ) ) );
			BGREM_TEMPLATE_XMAX = Integer.parseInt( props.getProperty( "BGREM_TEMPLATE_XMAX", Integer.toString( BGREM_TEMPLATE_XMAX ) ) );
			BGREM_X_OFFSET = Integer.parseInt( props.getProperty( "BGREM_X_OFFSET", Integer.toString( BGREM_X_OFFSET ) ) );
			GL_OFFSET_BOTTOM = Integer.parseInt( props.getProperty( "GL_OFFSET_BOTTOM", Integer.toString( GL_OFFSET_BOTTOM ) ) );
			GL_OFFSET_TOP = Integer.parseInt( props.getProperty( "GL_OFFSET_TOP", Integer.toString( GL_OFFSET_TOP ) ) );
			GL_OFFSET_LATERAL = Integer.parseInt( props.getProperty( "GL_OFFSET_LATERAL", Integer.toString( GL_OFFSET_LATERAL ) ) );
			MIN_CELL_LENGTH = Integer.parseInt( props.getProperty( "MIN_CELL_LENGTH", Integer.toString( MIN_CELL_LENGTH ) ) );
			MIN_GAP_CONTRAST = Double.parseDouble( props.getProperty( "MIN_GAP_CONTRAST", Double.toString( MIN_GAP_CONTRAST ) ) );
			SIGMA_PRE_SEGMENTATION_X = Double.parseDouble( props.getProperty( "SIGMA_PRE_SEGMENTATION_X", Double.toString( SIGMA_PRE_SEGMENTATION_X ) ) );
			SIGMA_PRE_SEGMENTATION_Y = Double.parseDouble( props.getProperty( "SIGMA_PRE_SEGMENTATION_Y", Double.toString( SIGMA_PRE_SEGMENTATION_Y ) ) );
			SIGMA_GL_DETECTION_X = Double.parseDouble( props.getProperty( "SIGMA_GL_DETECTION_X", Double.toString( SIGMA_GL_DETECTION_X ) ) );
			SIGMA_GL_DETECTION_Y = Double.parseDouble( props.getProperty( "SIGMA_GL_DETECTION_Y", Double.toString( SIGMA_GL_DETECTION_Y ) ) );
			DEFAULT_PATH = props.getProperty( "DEFAULT_PATH", DEFAULT_PATH );

			GUI_POS_X = Integer.parseInt( props.getProperty( "GUI_POS_X", Integer.toString( DEFAULT_GUI_POS_X ) ) );
			GUI_POS_Y = Integer.parseInt( props.getProperty( "GUI_POS_Y", Integer.toString( DEFAULT_GUI_POS_X ) ) );
			GUI_WIDTH = Integer.parseInt( props.getProperty( "GUI_WIDTH", Integer.toString( GUI_WIDTH ) ) );
			GUI_HEIGHT = Integer.parseInt( props.getProperty( "GUI_HEIGHT", Integer.toString( GUI_HEIGHT ) ) );
			GUI_CONSOLE_WIDTH = Integer.parseInt( props.getProperty( "GUI_CONSOLE_WIDTH", Integer.toString( GUI_CONSOLE_WIDTH ) ) );
			// Iterate over all currently attached monitors and check if sceen position is actually possible,
			// otherwise fall back to the DEFAULT values and ignore the ones coming from the properties-file.
			boolean pos_ok = false;
			final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			final GraphicsDevice[] gs = ge.getScreenDevices();
			for ( int i = 0; i < gs.length; i++ ) {
				final DisplayMode dm = gs[ i ].getDisplayMode();
				if ( gs[ i ].getDefaultConfiguration().getBounds().contains( new java.awt.Point( GUI_POS_X, GUI_POS_Y ) ) ) {
					pos_ok = true;
				}
			}
			// None of the screens contained the top-left window coordinates --> fall back onto default values...
			if ( !pos_ok ) {
				GUI_POS_X = DEFAULT_GUI_POS_X;
				GUI_POS_Y = DEFAULT_GUI_POS_Y;
			}

			String path = props.getProperty( "import_path", System.getProperty( "user.home" ) );
			final File fPath = main.showStartupDialog( guiFrame, path );
			path = fPath.getAbsolutePath();
			props.setProperty( "import_path", fPath.getAbsolutePath() );

			// Setting up console window and window snapper...
			main.initConsoleWindow();
			main.showConsoleWindow();
			final JFrameSnapper snapper = new JFrameSnapper();
			snapper.addFrame( main.frameConsoleWindow );
			snapper.addFrame( guiFrame );

			// ---------------------------------------------------
			main.processDataFromFolder( path );
			// ---------------------------------------------------

			System.out.print( "Build and show GUI..." );
			// show loaded and annotated data
			ImageJFunctions.show( main.imgRaw, "Rotated & cropped raw data" );
			ImageJFunctions.show( main.imgTemp, "Temporary" );
			ImageJFunctions.show( main.imgAnnotated, "Annotated ARGB data" );

			final MotherMachineGui gui = new MotherMachineGui( new MotherMachineModel( main ) );
			gui.setVisible( true );

			main.ij = new ImageJ();
			guiFrame.add( gui );
			guiFrame.setSize( GUI_WIDTH, GUI_HEIGHT );
			guiFrame.setLocation( GUI_POS_X, GUI_POS_Y );
			guiFrame.setVisible( true );

			SwingUtilities.invokeLater( new Runnable() {

				@Override
				public void run() {
					snapper.snapFrames( main.frameConsoleWindow, guiFrame, JFrameSnapper.EAST );
				}
			} );

			System.out.println( " done!" );
		}
		catch ( final UnsatisfiedLinkError ulr ) {
			JOptionPane.showMessageDialog( MotherMachine.guiFrame,
					"Could initialize Gurobi.\n" +
					"You might not have installed Gurobi properly or you miss a valid license.\n" +
					"Please visit 'www.gurobi.com' for further information.\n\n" +
					ulr.getMessage(),
					"Gurobi Error?", JOptionPane.ERROR_MESSAGE );
		}
	}

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------

	/**
	 * The singleton instance of ImageJ.
	 */
	public ImageJ ij;

	private double dCorrectedSlope;

	private Img< DoubleType > imgRaw;
	private Img< DoubleType > imgTemp;
	private Img< ARGBType > imgAnnotated;

	/**
	 * Contains all detected growth line center points.
	 * The structure goes in line with image data:
	 * Outermost list: one element per frame (image in stack).
	 * 2nd list: one element per detected growth-line.
	 * 3rd list: one element (Point) per location downwards along the growth
	 * line.
	 */
	private List< List< List< Point >>> glCenterPoints;

	/**
	 * Contains all GrowthLines found in the given data.
	 */
	private List< GrowthLine > growthLines;

	/**
	 * All ILP-related structures are within mmILP.
	 */
	private GrowthLineTrackingILP mmILP;

	/**
	 * Frame hosting the console output.
	 */
	private JFrame frameConsoleWindow;
	/**
	 * TextArea hosting the console output within the JFrame frameConsoleWindow.
	 */
	private JTextArea consoleWindowTextArea;

	// -------------------------------------------------------------------------------------
	// setters and getters
	// -------------------------------------------------------------------------------------
	/**
	 * @return the imgRaw
	 */
	public Img< DoubleType > getImgRaw() {
		return imgRaw;
	}

	/**
	 * @param imgRaw
	 *            the imgRaw to set
	 */
	public void setImgRaw( final Img< DoubleType > imgRaw ) {
		this.imgRaw = imgRaw;
	}

	/**
	 * @return the imgTemp
	 */
	public Img< DoubleType > getImgTemp() {
		return imgTemp;
	}

	/**
	 * @param imgTemp
	 *            the imgTemp to set
	 */
	public void setImgTemp( final Img< DoubleType > imgTemp ) {
		this.imgTemp = imgTemp;
	}

	/**
	 * @return the imgRendered
	 */
	public Img< ARGBType > getImgAnnotated() {
		return imgAnnotated;
	}

	/**
	 * @param imgRendered
	 *            the imgRendered to set
	 */
	public void setImgRendered( final Img< ARGBType > imgRendered ) {
		this.imgAnnotated = imgRendered;
	}

	/**
	 * @return the growthLines
	 */
	public List< GrowthLine > getGrowthLines() {
		return growthLines;
	}

	/**
	 * @param growthLines
	 *            the growthLines to set
	 */
	public void setGrowthLines( final List< GrowthLine > growthLines ) {
		this.growthLines = growthLines;
	}
	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------

	/**
	 * Created and shows the console window and redirects System.out and
	 * System.err to it.
	 */
	private void initConsoleWindow() {
		frameConsoleWindow = new JFrame( "MotherMachine Console Window" );
//		frameConsoleWindow.setResizable( false );
		consoleWindowTextArea = new JTextArea();

		final int centerX = ( int ) Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2;
		final int centerY = ( int ) Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2;
		frameConsoleWindow.setBounds( centerX - GUI_CONSOLE_WIDTH / 2, centerY - GUI_HEIGHT / 2, GUI_CONSOLE_WIDTH, GUI_HEIGHT );
		final JScrollPane scrollPane = new JScrollPane( consoleWindowTextArea );
		scrollPane.setBorder( BorderFactory.createEmptyBorder( 0, 15, 0, 0 ) );
		frameConsoleWindow.getContentPane().add( scrollPane );

		final OutputStream out = new OutputStream() {

			private final PrintStream original = new PrintStream( System.out );

			@Override
			public void write( final int b ) throws IOException {
				updateConsoleTextArea( String.valueOf( ( char ) b ) );
				original.print( String.valueOf( ( char ) b ) );
			}

			@Override
			public void write( final byte[] b, final int off, final int len ) throws IOException {
				updateConsoleTextArea( new String( b, off, len ) );
				original.print( new String( b, off, len ) );
			}

			@Override
			public void write( final byte[] b ) throws IOException {
				write( b, 0, b.length );
			}
		};

		final OutputStream err = new OutputStream() {

			private final PrintStream original = new PrintStream( System.out );

			@Override
			public void write( final int b ) throws IOException {
				updateConsoleTextArea( String.valueOf( ( char ) b ) );
				original.print( String.valueOf( ( char ) b ) );
			}

			@Override
			public void write( final byte[] b, final int off, final int len ) throws IOException {
				updateConsoleTextArea( new String( b, off, len ) );
				original.print( new String( b, off, len ) );
			}

			@Override
			public void write( final byte[] b ) throws IOException {
				write( b, 0, b.length );
			}
		};

		System.setOut( new PrintStream( out, true ) );
		System.setErr( new PrintStream( err, true ) );
	}

	private void updateConsoleTextArea( final String text ) {
		SwingUtilities.invokeLater( new Runnable() {

			@Override
			public void run() {
				consoleWindowTextArea.append( text );
			}
		} );
	}

	/**
	 * Shows the ConsoleWindow
	 */
	public void showConsoleWindow() {
		frameConsoleWindow.setVisible( true );
	}

	/**
	 * Hide the ConsoleWindow
	 */
	public void hideConsoleWindow() {
		frameConsoleWindow.setVisible( false );
	}

	/**
	 * Initializes the MotherMachine main app. This method contains platform
	 * specific code like setting icons, etc.
	 *
	 * @param guiFrame
	 *            the JFrame containing the MotherMachine.
	 */
	private void initMainWindow( final JFrame guiFrame ) {
		guiFrame.addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(final WindowEvent we) {
				saveParams();
				System.exit(0);
			}
		} );
//		final java.net.URL url = MotherMachine.class.getResource( "gui/media/IconMotherMachine128.png" );
//		final Toolkit kit = Toolkit.getDefaultToolkit();
//		final Image img = kit.createImage( url );
//		if ( !OSValidator.isMac() ) {
//			guiFrame.setIconImage( img );
//		}
//		if ( OSValidator.isMac() ) {
//			Application.getApplication().setDockIconImage( img );
//		}
	}

	/**
	 *
	 * @param guiFrame
	 *            parent frame
	 * @param path
	 *            path to be suggested to open
	 * @return
	 */
	private File showStartupDialog( final JFrame guiFrame, final String path ) {

		final String parentFolder = path.substring( 0, path.lastIndexOf( File.separatorChar ) );

		int decision = 0;
		if ( path.equals( System.getProperty( "user.home" ) ) ) {
			decision = JOptionPane.NO_OPTION;
		} else {
			final String message = "Should the MotherMachine be opened with the data found in:\n" + path + "\n\nIn case you want to choose a folder please select 'No'...";
			final String title = "MotherMachine Start Dialog";
			decision = JOptionPane.showConfirmDialog( guiFrame, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE );
		}
		if (decision == JOptionPane.YES_OPTION) {
			return new File( path );
		} else {
			return showFolderChooser( guiFrame, parentFolder );
		}
	}

	/**
	 * Shows a JFileChooser set up to accept the selection of folders.
	 * If 'cancel' is pressed this method terminates the MotherMachine app.
	 *
	 * @param guiFrame
	 *            parent frame
	 * @param path
	 *            path to the folder to open initially
	 * @return an instance of {@link File} pointing at the selected folder.
	 */
	private File showFolderChooser( final JFrame guiFrame, final String path ) {
		final JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory( new java.io.File( path ) );
		chooser.setDialogTitle( "Select folder containing image sequence..." );
		chooser.setFileFilter( new FileFilter() {

			@Override
			public final boolean accept( final File file ) {
				return file.isDirectory();
			}

			@Override
			public String getDescription() {
				return "We only take directories";
			}
		} );
		chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		chooser.setAcceptAllFileFilterUsed( false );

		if ( chooser.showOpenDialog( guiFrame ) == JFileChooser.APPROVE_OPTION ) {
			return chooser.getSelectedFile();
		} else {
			System.exit( 0 );
			return null;
		}
	}

	/**
	 * Loads the file 'mm.properties' and returns an instance of
	 * {@link Properties} containing the key-value pairs found in that file.
	 *
	 * @return instance of {@link Properties} containing the key-value pairs
	 *         found in that file.
	 */
	@SuppressWarnings( "resource" )
	private Properties loadParams() {
		InputStream is = null;
		final Properties props = new Properties();

		// First try loading from the current directory
		try {
			final File f = new File( "mm.properties" );
			is = new FileInputStream( f );
		}
		catch ( final Exception e ) {
			is = null;
		}

		try {
			if ( is == null ) {
				// Try loading from classpath
				is = getClass().getResourceAsStream( "mm.properties" );
			}

			// Try loading properties from the file (if found)
			props.load( is );
		}
		catch ( final Exception e ) {
			System.out.println( "No properties file 'mm.properties' found in current path or classpath... I will create one!" );
		}

		return props;
	}

	/**
	 * Saves a file 'mm.properties' in the current folder.
	 * This file contains all MotherMachine specific properties as key-value
	 * pairs.
	 *
	 * @param props
	 *            an instance of {@link Properties} containing all key-value
	 *            pairs used by the MotherMachine.
	 */
	public void saveParams() {
		try {
			final File f = new File( "mm.properties" );
			final OutputStream out = new FileOutputStream( f );

			props.setProperty( "BGREM_TEMPLATE_XMIN", Integer.toString( BGREM_TEMPLATE_XMIN ) );
			props.setProperty( "BGREM_TEMPLATE_XMAX", Integer.toString( BGREM_TEMPLATE_XMAX ) );
			props.setProperty( "BGREM_X_OFFSET", Integer.toString( BGREM_X_OFFSET ) );
			props.setProperty( "GL_OFFSET_BOTTOM", Integer.toString( GL_OFFSET_BOTTOM ) );
			props.setProperty( "GL_OFFSET_TOP", Integer.toString( GL_OFFSET_TOP ) );
			props.setProperty( "GL_OFFSET_LATERAL", Integer.toString( GL_OFFSET_LATERAL ) );
			props.setProperty( "MIN_CELL_LENGTH", Integer.toString( MIN_CELL_LENGTH ) );
			props.setProperty( "MIN_GAP_CONTRAST", Double.toString( MIN_GAP_CONTRAST ) );
			props.setProperty( "SIGMA_PRE_SEGMENTATION_X", Double.toString( SIGMA_PRE_SEGMENTATION_X ) );
			props.setProperty( "SIGMA_PRE_SEGMENTATION_Y", Double.toString( SIGMA_PRE_SEGMENTATION_Y ) );
			props.setProperty( "SIGMA_GL_DETECTION_X", Double.toString( SIGMA_GL_DETECTION_X ) );
			props.setProperty( "SIGMA_GL_DETECTION_Y", Double.toString( SIGMA_GL_DETECTION_Y ) );
			props.setProperty( "DEFAULT_PATH", DEFAULT_PATH );

			final java.awt.Point loc = guiFrame.getLocation();
			GUI_POS_X = loc.x;
			GUI_POS_Y = loc.y;
			GUI_WIDTH = guiFrame.getWidth();
			GUI_HEIGHT = guiFrame.getHeight();

			props.setProperty( "GUI_POS_X", Integer.toString( GUI_POS_X ) );
			props.setProperty( "GUI_POS_Y", Integer.toString( GUI_POS_Y ) );
			props.setProperty( "GUI_WIDTH", Integer.toString( GUI_WIDTH ) );
			props.setProperty( "GUI_HEIGHT", Integer.toString( GUI_HEIGHT ) );
			props.setProperty( "GUI_CONSOLE_WIDTH", Integer.toString( GUI_CONSOLE_WIDTH ) );

			props.store( out, "MotherMachine properties" );
		}
		catch ( final Exception e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Opens all tiffs in the given folder, straightens and crops images,
	 * extracts growth lines, subtracts background, builds segmentation
	 * hypothesis and a Markov random field for tracking. Finally it even solves
	 * this model using Gurobi and reads out the MAP.
	 *
	 * @param path
	 *            the folder to be processed.
	 */
	private void processDataFromFolder( final String path ) {
		// load tiffs from folder
		System.out.print( "Loading tiff sequence..." );
		loadTiffSequence( path );
		System.out.println( " done!" );

		// straighten loaded images
		System.out.print( "Staighten loaded images..." );
		straightenRawImg();
		System.out.println( " done!" );

		// cropping loaded images
		System.out.print( "Cropping to ROI..." );
		cropRawImgToROI();
		System.out.println( " done!" );

		// setup ARGB image (that will eventually contain annotations)
		System.out.print( "Spawning off annotation image (ARGB)..." );
		resetImgAnnotatedLike( getImgRaw() );
		try {
			DataMover.convertAndCopy( getImgRaw(), getImgAnnotated() );
		}
		catch ( final Exception e ) {
			// conversion might not be supported
			e.printStackTrace();
		}
		System.out.println( " done!" );

		System.out.print( "Searching for GrowthLines..." );
		resetImgTempToRaw();
		findGrowthLines();
		annotateDetectedWellCenters();
		System.out.println( " done!" );

		// subtracting BG in RAW image...
		System.out.print( "Subtracting background..." );
		subtractBackgroundInRaw();
		// ...and make temp image be the same
		resetImgTempToRaw();
		System.out.println( " done!" );

		System.out.print( "Generating Segmentation Hypotheses..." );
		generateSegmentationHypotheses();
		System.out.println( " done!" );

//		System.out.println( "Generating Integer Linear Programs..." );
//		generateILPs();
//		System.out.println( " done!" );
//
//		System.out.println( "Running Integer Linear Programs..." );
//		runILPs();
//		System.out.println( " done!" );
	}

	/**
	 * Loads all files contained in the given folder that end on '.tif' into an
	 * image stack and assigns it to imgRaw.
	 *
	 * @param folder
	 *            string containing a sequence of '.tif' files.
	 */
	public void loadTiffSequence( final String folder ) {
		try {
			imgRaw = DoubleTypeImgLoader.loadStackOfTiffsFromFolder( folder );
		}
		catch ( final Exception e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Resets imgTemp to contain the raw data from imgRaw.
	 */
	public void resetImgTempToRaw() {
		setImgTemp( imgRaw.copy() );
	}

	/**
	 * Resets imgTemp to contain the raw data from imgRaw.
	 */
	public void resetImgAnnotatedLike( final Img< DoubleType > img ) {
		imgAnnotated = DataMover.createEmptyArrayImgLike( img, new ARGBType() );
	}

	/**
	 * Rotates the whole stack (each Z-slize) in order to vertically align the
	 * wells seen in the micrographs come from the MotherMachine. Note: This is
	 * NOT done in-place! The returned <code>Img</code> in newly created!
	 *
	 * @param img
	 *            - the 3d <code>Img</code> to be straightened.
	 * @return the straightened <code>Img</code>. (Might be larger to avoid
	 *         loosing data!)
	 */
	private void straightenRawImg() {
		assert ( imgRaw.numDimensions() == 3 );

		// new raw image
		Img< DoubleType > rawNew;

		// find out how slanted the given stack is...
		final List< Cursor< DoubleType >> points = new Loops< DoubleType, Cursor< DoubleType >>().forEachHyperslice( Views.hyperSlice( imgRaw, 2, 0 ), 0, new FindLocationAboveThreshold< DoubleType >( new DoubleType( 0.33 ) ) );

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

		this.dCorrectedSlope = regression.getSlope();
		final double radSlant = Math.atan( regression.getSlope() );
		// System.out.println("slope = " + regression.getSlope());
		// System.out.println("intercept = " + regression.getIntercept());
		final double[] dCenter2d = new double[] { imgRaw.dimension( 0 ) * 0.5, -regression.getIntercept() + points.size() * regression.getSlope() };

		// ...and inversely rotate the whole stack in XY
		final AffineTransform2D affine = new AffineTransform2D();
		affine.translate( -dCenter2d[ 0 ], -dCenter2d[ 1 ] );
		affine.rotate( radSlant );
		affine.translate( dCenter2d[ 0 ], dCenter2d[ 1 ] );

		long minX = 0, maxX = imgRaw.dimension( 0 );
		long minY = 0;
		final long maxY = imgRaw.dimension( 1 );
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

		rawNew = imgRaw.factory().create( new long[] { maxX - minX, maxY - minY, imgRaw.dimension( 2 ) }, imgRaw.firstElement() );

		for ( i = 0; i < imgRaw.dimension( 2 ); i++ ) {
			final RandomAccessibleInterval< DoubleType > viewZSlize = Views.hyperSlice( imgRaw, 2, i );
			final RandomAccessible< DoubleType > raInfZSlize = Views.extendValue( viewZSlize, new DoubleType( 0.0 ) );
			final RealRandomAccessible< DoubleType > rraInterpolatedZSlize = Views.interpolate( raInfZSlize, new NLinearInterpolatorFactory< DoubleType >() );
			final RandomAccessible< DoubleType > raRotatedZSlize = RealViews.affine( rraInterpolatedZSlize, affine );

			final RandomAccessibleInterval< DoubleType > raiRotatedAndTruncatedZSlize = Views.zeroMin( Views.interval( raRotatedZSlize, new long[] { minX, minY }, new long[] { maxX, maxY } ) );

			DataMover.copy( raiRotatedAndTruncatedZSlize, Views.iterable( Views.hyperSlice( rawNew, 2, i ) ) );
		}

		// set new, straightened image to be the new imgRaw
		imgRaw = rawNew;
	}

	/**
	 * Finds the region of interest in the given 3d image stack and crops it.
	 * Note: each cropped z-slize will be renormalized to [0,1]. Precondition:
	 * given <code>Img</code> should be rotated such that the seen wells are
	 * axis parallel.
	 *
	 * @param img
	 *            - the streightened 3d <code>Img</code> that should be cropped
	 *            down to contain only the ROI.
	 */
	private void cropRawImgToROI() {
		assert ( imgRaw.numDimensions() == 3 );

		// return image
		Img< DoubleType > rawNew = null;

		// crop positions to be evaluated
		long top = 0, bottom = imgRaw.dimension( 1 );
		long left, right;

		// check for possible crop in first and last image
		final long[] lZPositions = new long[] { 0, imgRaw.dimension( 2 ) - 1 };
		for ( final long lZPos : lZPositions ) {
			// find out how slanted the given stack is...
			final List< DoubleType > points = new Loops< DoubleType, DoubleType >().forEachHyperslice( Views.hyperSlice( imgRaw, 2, lZPos ), 1, new VarOfRai< DoubleType >() );

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
		left = Math.round( Math.floor( 0 - this.dCorrectedSlope * bottom ) );
		right = Math.round( Math.ceil( imgRaw.dimension( 0 ) + this.dCorrectedSlope * ( imgRaw.dimension( 1 ) - top ) ) );

		// create image that can host cropped data
		rawNew = imgRaw.factory().create( new long[] { right - left, bottom - top, imgRaw.dimension( 2 ) }, imgRaw.firstElement() );

		// and copy it there
		for ( int i = 0; i < imgRaw.dimension( 2 ); i++ ) {
			final RandomAccessibleInterval< DoubleType > viewZSlize = Views.hyperSlice( imgRaw, 2, i );
			final RandomAccessibleInterval< DoubleType > viewCroppedZSlize = Views.zeroMin( Views.interval( viewZSlize, new long[] { left, top }, new long[] { bottom, right } ) );

			DataMover.copy( viewCroppedZSlize, Views.iterable( Views.hyperSlice( rawNew, 2, i ) ) );
			// Normalize.normalize(Views.iterable( Views.hyperSlice(ret, 2, i)
			// ), new DoubleType(0.0), new DoubleType(1.0));
		}

		// set new, straightened image to be the new imgRaw
		imgRaw = rawNew;
	}

	/**
	 * Simple but effective method to subtract uneven illumination from the
	 * growth-line data.
	 *
	 * @param img
	 *            DoubleType image stack.
	 */
	private void subtractBackgroundInRaw() {

		for ( int i = 0; i < getGrowthLines().size(); i++ ) {
			for ( int f = 0; f < getGrowthLines().get( i ).size(); f++ ) {
				final GrowthLineFrame glf = getGrowthLines().get( i ).get( f );

				final int glfX = glf.getAvgXpos();
				if ( glfX == -1 ) continue; // do not do anything with empty GLFs

				final int glfY1 = 0; // gl.getFirstPoint().getIntPosition(1);
				final int glfY2 = ( int ) imgRaw.dimension( 1 ) - 1; // gl.getLastPoint().getIntPosition(1);

				final IntervalView< DoubleType > frame = Views.hyperSlice( imgRaw, 2, f );

				double rowAvgs[] = new double[ glfY2 - glfY1 + 1 ];
				int colCount = 0;
				// Look to the left if you are not the first GLF
				if ( glfX > MotherMachine.BGREM_TEMPLATE_XMAX ) {
					final IntervalView< DoubleType > leftBackgroundWindow = Views.interval( frame, new long[] { glfX - MotherMachine.BGREM_TEMPLATE_XMAX, glfY1 }, new long[] { glfX - MotherMachine.BGREM_TEMPLATE_XMIN, glfY2 } );
					rowAvgs = addRowSumsFromInterval( leftBackgroundWindow, rowAvgs );
					colCount += ( MotherMachine.BGREM_TEMPLATE_XMAX - MotherMachine.BGREM_TEMPLATE_XMIN );
				}
				// Look to the right if you are not the last GLF
				if ( glfX < imgRaw.dimension( 0 ) - MotherMachine.BGREM_TEMPLATE_XMAX ) {
					final IntervalView< DoubleType > rightBackgroundWindow = Views.interval( frame, new long[] { glfX + MotherMachine.BGREM_TEMPLATE_XMIN, glfY1 }, new long[] { glfX + MotherMachine.BGREM_TEMPLATE_XMAX, glfY2 } );
					rowAvgs = addRowSumsFromInterval( rightBackgroundWindow, rowAvgs );
					colCount += ( MotherMachine.BGREM_TEMPLATE_XMAX - MotherMachine.BGREM_TEMPLATE_XMIN );
				}
				// compute averages
				for ( int j = 0; j < rowAvgs.length; j++ ) {
					rowAvgs[ j ] /= colCount;
				}

				// Subtract averages you've seen to your left and/or to your
				// right
				final long x1 = Math.max( 0, glfX - MotherMachine.BGREM_X_OFFSET );
				final long x2 = Math.min( frame.dimension( 0 ) - 1, glfX + MotherMachine.BGREM_X_OFFSET );
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
	private double[] addRowSumsFromInterval( final IntervalView< DoubleType > view, final double[] rowSums ) {
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
	private void removeValuesFromRows( final IntervalView< DoubleType > view, final double[] values ) {
		for ( int i = 0; i < view.dimension( 1 ); i++ ) {
			final Cursor< DoubleType > cursor = Views.iterable( Views.hyperSlice( view, 1, i ) ).cursor();
			while ( cursor.hasNext() ) {
				cursor.next().set( new DoubleType( Math.max( 0, cursor.get().get() - values[ i ] ) ) );
			}
		}
	}

	/**
	 * Estimates the centers of the growth lines given in 'imgTemp'.
	 * The found center lines are computed by a linear regression of growth line
	 * center estimates.
	 * Those estimates are obtained by convolving the image with a Gaussian
	 * (parameterized by SIGMA_GL_DETECTION_*) and looking for local maxima in
	 * that image.
	 *
	 * This function operates on 'imgTemp' and sets 'glCenterPoints' as
	 * well as 'growthLines'.
	 */
	private void findGrowthLines() {

		this.setGrowthLines( new ArrayList< GrowthLine >() );
		this.glCenterPoints = new ArrayList< List<List<Point>>>();

		List< List< Point > > frameWellCenters;

		// ------ GAUSS -----------------------------

		final int n = imgTemp.numDimensions();
		final double[] sigmas = new double[ n ];
		sigmas[ 0 ] = SIGMA_GL_DETECTION_X;
		sigmas[ 1 ] = SIGMA_GL_DETECTION_Y;
		try {
			Gauss3.gauss( sigmas, Views.extendMirrorDouble( imgTemp ), imgTemp );
		}
		catch ( final IncompatibleTypeException e ) {
			e.printStackTrace();
		}


		// ------ FIND AND FILTER MAXIMA -------------

		final List< List< GrowthLineFrame >> collectionOfFrames = new ArrayList< List< GrowthLineFrame >>();

		for ( long frameIdx = 0; frameIdx < imgTemp.dimension( 2 ); frameIdx++ ) {
			final IntervalView< DoubleType > ivFrame = Views.hyperSlice( imgTemp, 2, frameIdx );

			// Find maxima per image row (per frame)
			frameWellCenters = new Loops< DoubleType, List< Point >>().forEachHyperslice( ivFrame, 1, new FindLocalMaxima< DoubleType >() );

			// Delete detected points that are too lateral
			for ( int y = 0; y < frameWellCenters.size(); y++ ) {
				final List< Point > lstPoints = frameWellCenters.get( y );
				for ( int x = lstPoints.size() - 1; x >= 0; x-- ) {
					if ( lstPoints.get( x ).getIntPosition( 0 ) < GL_OFFSET_LATERAL || lstPoints.get( x ).getIntPosition( 0 ) > imgTemp.dimension( 0 ) - GL_OFFSET_LATERAL ) {
						lstPoints.remove( x );
					}
				}
				frameWellCenters.set( y, lstPoints );
			}

			// Delete detected points that are too high or too low
			// (and use this sweep to compute 'maxWellCenterIdx' and 'maxWellCenters')
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
			this.glCenterPoints.add( frameWellCenters );

			// ------ DISTRIBUTE POINTS TO CORRESPONDING GROWTH LINES -------

			final List< GrowthLineFrame > glFrames = new ArrayList< GrowthLineFrame >();

			final Point pOrig = new Point( 3 );
			pOrig.setPosition( frameIdx, 2 ); 	// location in original Img (will
												// be recovered step by step)

			// start at the row containing the maximum number of well centers
			// (see above for the code that found maxWellCenter*)
			pOrig.setPosition( maxWellCentersIdx, 1 );
			for ( int x = 0; x < maxWellCenters; x++ ) {
				glFrames.add( new GrowthLineFrame() ); // add one GLF for each found column
				final Point p = frameWellCenters.get( maxWellCentersIdx ).get( x );
				pOrig.setPosition( p.getLongPosition( 0 ), 0 );
				glFrames.get( x ).addPoint( new Point( pOrig ) );
			}
			// now go backwards from 'maxWellCenterIdx' and find the right assignment in case
			// a different number of wells was found (going forwards comes below!)
			for ( int y = maxWellCentersIdx - 1; y >= 0; y-- ) {
				pOrig.setPosition( y, 1 ); // location in orig. Img (2nd of 3 steps)

				final List< Point > maximaPerImgRow = frameWellCenters.get( y );
				if ( maximaPerImgRow.size() == 0 ) {
					break;
				}
				// find best matching well for first point
				final int posX = frameWellCenters.get( y ).get( 0 ).getIntPosition( 0 );
				int mindist = ( int ) imgTemp.dimension( 0 );
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
			// now go forward from 'maxWellCenterIdx' and find the right assignment in case
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
				int mindist = ( int ) imgTemp.dimension( 0 );
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
			// add this list of GrowhtLIneFrames to the collection
			collectionOfFrames.add( glFrames );
		}

		// ------ SORT GrowthLineFrames FROM collectionOfFrames INTO this.growthLines -------------
		int maxGLsPerFrame = 0;
		int maxGLsPerFrameIdx = 0;
		for ( int i = 0; i < collectionOfFrames.size(); i++ ) {
			if ( maxGLsPerFrame < collectionOfFrames.get( i ).size() ) {
				maxGLsPerFrame = collectionOfFrames.get( i ).size();
				maxGLsPerFrameIdx = i;
			}
		}
		// copy the max-GLs frame into this.growthLines
		this.setGrowthLines( new ArrayList< GrowthLine >( maxGLsPerFrame ) );
		for ( int i = 0; i < maxGLsPerFrame; i++ ) {
			getGrowthLines().add( new GrowthLine() );
			getGrowthLines().get( i ).add( collectionOfFrames.get( maxGLsPerFrameIdx ).get( i ) );
		}
		// go backwards from there and prepand into GL
		for ( int j = maxGLsPerFrameIdx - 1; j >= 0; j-- ) {
			final int deltaL = maxGLsPerFrame - collectionOfFrames.get( j ).size();
			int offset = 0;  // here we would like to have the shift to consider when copying GLFrames into GLs
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
				getGrowthLines().get( offset + i ).prepand( collectionOfFrames.get( j ).get( i ) );
			}
		}
		// go forwards and append into GL
		for ( int j = maxGLsPerFrameIdx + 1; j < collectionOfFrames.size(); j++ ) {
			final int deltaL = maxGLsPerFrame - collectionOfFrames.get( j ).size();
			int offset = 0;  // here we would like to have the shift to consider when copying GLFrames into GLs
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
				getGrowthLines().get( offset + i ).add( collectionOfFrames.get( j ).get( i ) );
			}
		}

	}

	/**
	 * Draws the detected well centers, <code>detectedWellCenters</code>, into
	 * the annotation layer, <code>imgAnnotated</code>.
	 */
	private void annotateDetectedWellCenters() {
		for ( final GrowthLine gl : this.getGrowthLines() ) {
			for ( final GrowthLineFrame glf : gl.getFrames() ) {
				glf.drawCenterLine( imgAnnotated );
			}
		}
	}

	/**
	 * Iterates over all found GrowthLines and evokes
	 * GrowthLine.findGapHypotheses(Img).
	 * Note that this function always uses the image data in 'imgTemp'.
	 */
	public void generateSegmentationHypotheses() {

		// ------ GAUSS -----------------------------

		if ( SIGMA_PRE_SEGMENTATION_X + SIGMA_PRE_SEGMENTATION_Y > 0.000001 ) {
			System.out.print( " ...Note: smoothing performed before building GapHypotheses... " );
			final int n = imgTemp.numDimensions();
			final double[] sigmas = new double[ n ];
			sigmas[ 0 ] = SIGMA_PRE_SEGMENTATION_X;
			sigmas[ 1 ] = SIGMA_PRE_SEGMENTATION_Y;
			try {
				Gauss3.gauss( sigmas, Views.extendMirrorDouble( imgTemp ), imgTemp );
			}
			catch ( final IncompatibleTypeException e ) {
				e.printStackTrace();
			}
		}

		// ------ DETECTION --------------------------

		System.out.println( "" );
		int i = 0;
		for ( final GrowthLine gl : getGrowthLines() ) {
			i++;
			System.out.print( "   Working on GL#" + i + " of " + getGrowthLines().size() + "... " );
			for ( final GrowthLineFrame glf : gl.getFrames() ) {
				System.out.print( "." );
				glf.generateSegmentationHypotheses( imgTemp );
			}
			System.out.println( " ...done!" );
		}
	}

	/**
	 * Creates and triggers filling of mmILP, containing all
	 * optimization-related structures used to compute the optimal tracking.
	 */
	private void generateILPs() {
//		for ( final GrowthLine gl : getGrowthLines() ) {
//			gl.generateILP();
//		}
//		getGrowthLines().get( 0 ).generateILP();
	}

	/**
	 * Runs all the generated ILPs.
	 */
	private void runILPs() {
//		int i = 0;
//		for ( final GrowthLine gl : getGrowthLines() ) {
//			System.out.println( " > > > > > Starting LP for GL# " + i + " < < < < < " );
//			gl.runILP();
//			i++;
//		}
//		getGrowthLines().get( 0 ).runILP();
	}

}
