/**
 *
 */
package com.jug.gui;

import gurobi.GRBException;
import ij.ImageJ;
import ij.Prefs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import loci.formats.gui.ExtensionFileFilter;
import net.imglib2.Localizable;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.algorithm.componenttree.ComponentForest;
import net.imglib2.converter.Converters;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.miginfocom.swing.MigLayout;

import org.math.plot.Plot2DPanel;

import com.jug.GrowthLine;
import com.jug.GrowthLineFrame;
import com.jug.MotherMachine;
import com.jug.export.CellStatsExporter;
import com.jug.export.HtmlOverviewExporter;
import com.jug.gui.progress.DialogProgress;
import com.jug.gui.slider.RangeSlider;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.lp.Hypothesis;
import com.jug.util.ComponentTreeUtils;
import com.jug.util.SimpleFunctionAnalysis;
import com.jug.util.Util;
import com.jug.util.converter.RealFloatNormalizeConverter;
import com.jug.util.filteredcomponents.FilteredComponent;

/**
 * @author jug
 */
public class MotherMachineGui extends JPanel implements ChangeListener, ActionListener {

	private static final long serialVersionUID = -1008974839249784873L;

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	public MotherMachineModel model;

	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (left one in active assignments view).
	 */
	IntervalView< FloatType > viewImgLeftActive;
	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (center one in active assignments view).
	 */
	IntervalView< FloatType > viewImgCenterActive;
	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (right one in active assignments view).
	 */
	IntervalView< FloatType > viewImgRightActive;

	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (left one in inactive assignments view).
	 */
	IntervalView< FloatType > viewImgLeftInactive;
	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (center one in inactive assignments view).
	 */
	IntervalView< FloatType > viewImgCenterInactive;
	/**
	 * The view onto <code>imgRaw</code> that is supposed to be shown on screen
	 * (right one in inactive assignments view).
	 */
	IntervalView< FloatType > viewImgRightInactive;

	// -------------------------------------------------------------------------------------
	// gui-fields
	// -------------------------------------------------------------------------------------
	public Viewer2DCanvas imgCanvasActiveLeft;
	public Viewer2DCanvas imgCanvasActiveCenter;
	public Viewer2DCanvas imgCanvasActiveRight;

	public JSlider sliderGL;
	public JSlider sliderTime;
	public RangeSlider sliderTrackingRange;
	private JLabel lblCurrentTime;

	private JTabbedPane tabsViews;
	private CountOverviewPanel panelCountingView;
	private JPanel panelSegmentationAndAssignmentView;
	private JPanel panelDetailedDataView;
	private Plot2DPanel plot;

	public AssignmentViewer leftAssignmentViewer;
	public AssignmentViewer rightAssignmentViewer;

//	private JButton btnRedoAllHypotheses;
//	private JButton btnExchangeSegHyps;
	private JButton btnReoptimize;
	private JButton btnOptimizeMore;
	private JButton btnExportHtml;
	private JButton btnExportData;
//	private JButton btnSaveFG;

	String itemChannel0BGSubtr = "BG-subtr. Ch.0";
	String itemChannel0 = "Raw Channel 0";
	String itemChannel1 = "Raw Channel 1";
	String itemChannel2 = "Raw Channel 2";
//	String itemPMFRF = "PMFRF Sum Image";
//	String itemClassified = "RF BG Probability";
//	String itemSegmented = "RF Cell Segmentation";
	private JComboBox cbWhichImgToShow;

	// REMOVED because load/save does not go easy with this shit!
//	private JLabel lActiveHyps;

	private JTextField txtNumCells;

	// Batch interaction panels
	private JCheckBox cbSegmentationOkLeft;
	private JCheckBox cbSegmentationOkCenter;
	private JCheckBox cbSegmentationOkRight;

	private JCheckBox cbAssignmentsOkLeft;
	private JCheckBox cbAssignmentsOkRight;

	private JButton bCheckBoxLineFixHistory;
	private JButton bCheckBoxLineSet;
	private JButton bCheckBoxLineReset;

	// Menu-items
	private MenuItem menuViewShowConsole;
	private MenuItem menuShowImgRaw;
	private MenuItem menuShowImgTemp;

	private MenuItem menuProps;
	private MenuItem menuLoad;
	private MenuItem menuSave;

	// -------------------------------------------------------------------------------------
	// construction & gui creation
	// -------------------------------------------------------------------------------------
	/**
	 * Construction
	 *
	 * @param mmm
	 *            the MotherMachineModel to show
	 */
	public MotherMachineGui( final MotherMachineModel mmm ) {
		super( new BorderLayout() );

		this.model = mmm;

		buildGui();
		dataToDisplayChanged();
	}

	/**
	 * Builds the GUI.
	 */
	private void buildGui() {

		final MenuBar menuBar = new MenuBar();
		final Menu menuFile = new Menu( "File" );
		menuProps = new MenuItem( "Preferences..." );
		menuProps.addActionListener( this );
		menuLoad = new MenuItem( "Load tracking..." );
		menuLoad.addActionListener( this );
		menuSave = new MenuItem( "Save tracking..." );
		menuSave.addActionListener( this );
		menuFile.add( menuProps );
		menuFile.addSeparator();
		menuFile.add( menuLoad );
		menuFile.add( menuSave );
		menuBar.add( menuFile );

		final Menu menuView = new Menu( "View" );
		menuViewShowConsole = new MenuItem( "Show/hide Console" );
		menuViewShowConsole.addActionListener( this );
		menuShowImgRaw = new MenuItem( "Show raw imges..." );
		menuShowImgRaw.addActionListener( this );
		menuShowImgTemp = new MenuItem( "Show BG-subtrackted imges..." );
		menuShowImgTemp.addActionListener( this );
		menuView.add( menuViewShowConsole );
		menuView.add( menuShowImgRaw );
		menuView.add( menuShowImgTemp );
		menuBar.add( menuView );
		if ( !MotherMachine.HEADLESS ) {
			MotherMachine.getGuiFrame().setMenuBar( menuBar );
		}

		final JPanel panelContent = new JPanel( new BorderLayout() );
		JPanel panelVerticalHelper;
		JPanel panelHorizontalHelper;

		// --- Slider for time and GL -------------

		sliderTime = new JSlider( JSlider.HORIZONTAL, 0, model.getCurrentGL().size() - 2, 0 );
		sliderTime.setValue( 1 );
		model.setCurrentGLF( sliderTime.getValue() );
		sliderTime.addChangeListener( this );
		if ( sliderTime.getMaximum() < 200 ) {
			sliderTime.setMajorTickSpacing( 10 );
			sliderTime.setMinorTickSpacing( 2 );
		} else {
			sliderTime.setMajorTickSpacing( 100 );
			sliderTime.setMinorTickSpacing( 10 );
		}
		sliderTime.setPaintTicks( true );
		sliderTime.setPaintLabels( true );
		sliderTime.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 3 ) );
		panelHorizontalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper.setBorder( BorderFactory.createEmptyBorder( 5, 10, 0, 5 ) );
		lblCurrentTime = new JLabel( String.format( " t = %4d", sliderTime.getValue() ) );
		panelHorizontalHelper.add( lblCurrentTime, BorderLayout.WEST );
		panelHorizontalHelper.add( sliderTime, BorderLayout.CENTER );
		panelVerticalHelper = new JPanel( new BorderLayout() );
		panelVerticalHelper.add( panelHorizontalHelper, BorderLayout.CENTER );

		// --- Slider for TrackingRage ----------
		sliderTrackingRange =
				new RangeSlider( 0, model.getCurrentGL().size() - 2 );
		sliderTrackingRange.setValue( 0 );
		sliderTrackingRange.setUpperValue( model.getCurrentGL().size() - 2 );
		sliderTrackingRange.addChangeListener( this );
		panelHorizontalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper.setBorder( BorderFactory.createEmptyBorder( 0, 10, 15, 5 ) );
		final JLabel lblIgnoreBeyond =
				new JLabel( String.format( "opt. range:", sliderTrackingRange.getValue() ) );
		lblIgnoreBeyond.setToolTipText( "correct up to left slider / ignore data beyond right slider" );
		panelHorizontalHelper.add( lblIgnoreBeyond, BorderLayout.WEST );
		panelHorizontalHelper.add( sliderTrackingRange, BorderLayout.CENTER );
		panelVerticalHelper.add( panelHorizontalHelper, BorderLayout.SOUTH );

		panelContent.add( panelVerticalHelper, BorderLayout.SOUTH );

		sliderGL = new JSlider( JSlider.VERTICAL, 0, model.mm.getGrowthLines().size() - 1, 0 );
		sliderGL.setValue( 0 );
		sliderGL.addChangeListener( this );
		sliderGL.setMajorTickSpacing( 5 );
		sliderGL.setMinorTickSpacing( 1 );
		sliderGL.setPaintTicks( true );
		sliderGL.setPaintLabels( true );
		sliderGL.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 3 ) );
		panelVerticalHelper = new JPanel( new BorderLayout() );
		panelVerticalHelper.setBorder( BorderFactory.createEmptyBorder( 10, 10, 0, 5 ) );
		panelVerticalHelper.add( new JLabel( "GL#" ), BorderLayout.NORTH );
		panelVerticalHelper.add( sliderGL, BorderLayout.CENTER );
		// show the slider only if it actually has a purpose...
		if ( sliderGL.getMaximum() > 1 ) {
			add( panelVerticalHelper, BorderLayout.WEST );
		}

		// --- All the TABs -------------

		tabsViews = new JTabbedPane();
		tabsViews.addChangeListener( this );

		panelCountingView = new CountOverviewPanel();
		panelSegmentationAndAssignmentView = buildSegmentationAndAssignmentView();
		panelDetailedDataView = buildDetailedDataView();

		tabsViews.add( "Cell Counting", panelCountingView );
		tabsViews.add( "Segm. & Assingments", panelSegmentationAndAssignmentView );
		tabsViews.add( "Detailed Data View", panelDetailedDataView );

		tabsViews.setSelectedComponent( panelSegmentationAndAssignmentView );

		// --- Controls ----------------------------------
//		btnRedoAllHypotheses = new JButton( "Resegment" );
//		btnRedoAllHypotheses.addActionListener( this );
		btnReoptimize = new JButton( "Restart" );
		btnReoptimize.addActionListener( this );
		btnOptimizeMore = new JButton( "Optimize" );
		btnOptimizeMore.addActionListener( this );
		btnExportHtml = new JButton( "Export HTML" );
		btnExportHtml.addActionListener( this );
		btnExportData = new JButton( "Export Data" );
		btnExportData.addActionListener( this );
//		btnSaveFG = new JButton( "Save FG" );
//		btnSaveFG.addActionListener( this );
		panelHorizontalHelper = new JPanel( new FlowLayout( FlowLayout.RIGHT, 5, 0 ) );
//		panelHorizontalHelper.add( btnRedoAllHypotheses );
		panelHorizontalHelper.add( btnReoptimize );
		panelHorizontalHelper.add( btnOptimizeMore );
		panelHorizontalHelper.add( btnExportHtml );
		panelHorizontalHelper.add( btnExportData );
//		panelHorizontalHelper.add( btnSaveFG );
		add( panelHorizontalHelper, BorderLayout.SOUTH );

		// --- Final adding and layout steps -------------

		panelContent.add( tabsViews, BorderLayout.CENTER );
		add( panelContent, BorderLayout.CENTER );

		// - - - - - - - - - - - - - - - - - - - - - - - -
		//  KEYSTROKE SETUP (usingInput- and ActionMaps)
		// - - - - - - - - - - - - - - - - - - - - - - - -
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 't' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 'g' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 'a' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 's' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 'd' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 'r' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 'o' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( 'e' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( ' ' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( '?' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( '0' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( '1' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( '2' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( '3' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( '4' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( '5' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( '6' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( '7' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( '8' ), "MMGUI_bindings" );
		this.getInputMap( WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( '9' ), "MMGUI_bindings" );

		this.getActionMap().put( "MMGUI_bindings", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e ) {
				if ( e.getActionCommand().equals( "t" ) ) {
					sliderTime.requestFocus();
					dataToDisplayChanged();
				}
				if ( e.getActionCommand().equals( "g" ) ) {
					sliderGL.requestFocus();
					dataToDisplayChanged();
				}
				if ( e.getActionCommand().equals( "a" ) ) {
					if ( !tabsViews.getComponent( tabsViews.getSelectedIndex() ).equals( panelCountingView ) ) {
						tabsViews.setSelectedComponent( panelCountingView );
					}
					dataToDisplayChanged();
				}
				if ( e.getActionCommand().equals( "s" ) ) {
					if ( !tabsViews.getComponent( tabsViews.getSelectedIndex() ).equals( panelSegmentationAndAssignmentView ) ) {
						tabsViews.setSelectedComponent( panelSegmentationAndAssignmentView );
					}
					dataToDisplayChanged();
				}
				if ( e.getActionCommand().equals( "d" ) ) {
					if ( !tabsViews.getComponent( tabsViews.getSelectedIndex() ).equals( panelDetailedDataView ) ) {
						tabsViews.setSelectedComponent( panelDetailedDataView );
					}
					dataToDisplayChanged();
				}
				if ( e.getActionCommand().equals( "e" ) ) {
					btnExportData.doClick();
				}
				if ( e.getActionCommand().equals( "r" ) ) {
					btnReoptimize.doClick();
				}
				if ( e.getActionCommand().equals( "o" ) ) {
					btnOptimizeMore.doClick();
				}
				if ( e.getActionCommand().equals( "v" ) ) {
					int selIdx = cbWhichImgToShow.getSelectedIndex();
					selIdx++;
					if ( selIdx == cbWhichImgToShow.getItemCount() ) {
						selIdx = 0;
					}
					cbWhichImgToShow.setSelectedIndex( selIdx );
				}
				if ( e.getActionCommand().equals( "?" ) || e.getActionCommand().equals( "0" ) || e.getActionCommand().equals( "1" ) || e.getActionCommand().equals( "2" ) || e.getActionCommand().equals( "3" ) || e.getActionCommand().equals( "4" ) || e.getActionCommand().equals( "5" ) || e.getActionCommand().equals( "6" ) || e.getActionCommand().equals( "7" ) || e.getActionCommand().equals( "8" ) || e.getActionCommand().equals( "9" ) ) {
					txtNumCells.requestFocus();
					txtNumCells.setText( e.getActionCommand() );
				}
			}
		} );
	}

	/**
	 * @param panelCurationViewHelper
	 * @return
	 */
	private JPanel buildSegmentationAndAssignmentView() {
		final JPanel panelContent = new JPanel( new BorderLayout() );

		final JPanel panelViewCenterHelper =
				new JPanel( new FlowLayout( FlowLayout.CENTER, 0, 10 ) );
		final JPanel panelView =
				new JPanel( new MigLayout( "wrap 7", "[]0[]0[]0[]0[]0[]0[]", "[]0[]" ) );

		// =============== panelIsee-part ===================
		final JPanel panelIsee = new JPanel();
		panelIsee.setLayout( new BoxLayout( panelIsee, BoxLayout.LINE_AXIS ) );

		final JLabel labelNumCells1 = new JLabel( "I see" );
		final JLabel labelNumCells2 = new JLabel( "cells!" );
		txtNumCells = new JTextField( "?", 2 );
		txtNumCells.setHorizontalAlignment( SwingConstants.CENTER );
		txtNumCells.setMaximumSize( txtNumCells.getPreferredSize() );
		txtNumCells.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				int numCells = 0;
				final GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();
				try {
					numCells = Integer.parseInt( txtNumCells.getText() );
				} catch ( final NumberFormatException nfe ) {
					numCells = -1;
					txtNumCells.setText( "?" );
					ilp.removeSegmentsInFrameCountConstraint( model.getCurrentTime() );
				}
				if ( numCells != -1 ) {
					try {
						ilp.removeSegmentsInFrameCountConstraint( model.getCurrentTime() );
						ilp.addSegmentsInFrameCountConstraint( model.getCurrentTime(), numCells );
					} catch ( final GRBException e1 ) {
						e1.printStackTrace();
					}
				}

				final Thread t = new Thread( new Runnable() {

					@Override
					public void run() {
						model.getCurrentGL().runILP();
						dataToDisplayChanged();
						sliderTime.requestFocus();
					}
				} );
				t.start();
			}
		} );

		panelIsee.add( Box.createHorizontalGlue() );
		panelIsee.add( labelNumCells1 );
		panelIsee.add( txtNumCells );
		panelIsee.add( labelNumCells2 );
		panelIsee.add( Box.createHorizontalGlue() );

		// =============== panelDropdown-part ===================
		final JPanel panelDropdown = new JPanel();
		panelDropdown.setLayout( new BoxLayout( panelDropdown, BoxLayout.LINE_AXIS ) );
		cbWhichImgToShow = new JComboBox();
		cbWhichImgToShow.addItem( itemChannel0BGSubtr );
		cbWhichImgToShow.addItem( itemChannel0 );
		if ( model.mm.getRawChannelImgs().size() > 1 ) {
			cbWhichImgToShow.addItem( itemChannel1 );
		}
		if ( model.mm.getRawChannelImgs().size() > 2 ) {
			cbWhichImgToShow.addItem( itemChannel2 );
		}
//		cbWhichImgToShow.addItem( itemPMFRF );
//		cbWhichImgToShow.addItem( itemClassified );
//		cbWhichImgToShow.addItem( itemSegmented );
		cbWhichImgToShow.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				dataToDisplayChanged();
			}
		} );

		panelDropdown.add( Box.createHorizontalGlue() );
		panelDropdown.add( cbWhichImgToShow );
		panelDropdown.add( Box.createHorizontalGlue() );

//		btnExchangeSegHyps = new JButton( "switch" );
//		btnExchangeSegHyps.addActionListener( new ActionListener() {
//
//			@Override
//			public void actionPerformed( final ActionEvent e ) {
//				final GrowthLineFrame glf = model.getCurrentGLF();
//				if ( !glf.isParaMaxFlowComponentTree() ) {
//					glf.generateAwesomeSegmentationHypotheses( model.mm.getImgTemp() );
//				} else {
//					glf.generateSimpleSegmentationHypotheses( model.mm.getImgTemp() );
//				}
//				dataToDisplayChanged();
//			}
//		} );
//		lActiveHyps = new JLabel( "CT" );
//		lActiveHyps.setHorizontalAlignment( SwingConstants.CENTER );
//		lActiveHyps.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder( 2, 5, 2, 5 ) ) );
//		lActiveHyps.setPreferredSize( new Dimension( 65, lActiveHyps.getPreferredSize().height ) );

//		panelOptions.add( btnExchangeSegHyps );
//		panelOptions.add( lActiveHyps );
//		panelOptions.add( Box.createHorizontalGlue() );

		// =============== panelView-part ===================

		JPanel panelVerticalHelper;
		JPanel panelHorizontalHelper;
		JLabel labelHelper;

		final GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();

		// --- Left data viewer (t-1) -------------

		panelView.add( new JPanel() );

		panelVerticalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		labelHelper = new JLabel( "t-1" );
		panelHorizontalHelper.add( labelHelper );
		panelVerticalHelper.add( panelHorizontalHelper, BorderLayout.NORTH );
		// - - - - - -
		imgCanvasActiveLeft = new Viewer2DCanvas( this, MotherMachine.GL_WIDTH_IN_PIXELS + 2 * MotherMachine.GL_PIXEL_PADDING_IN_VIEWS, ( int ) model.mm.getImgRaw().dimension( 1 ) );
		panelVerticalHelper.add( imgCanvasActiveLeft, BorderLayout.CENTER );
		panelVerticalHelper.setBorder( BorderFactory.createMatteBorder( 2, 2, 2, 2, Color.GRAY ) );
		panelVerticalHelper.setBackground( Color.BLACK );
		panelView.add( panelVerticalHelper );

		// --- Left assignment viewer (t-1 -> t) -------------
		panelVerticalHelper = new JPanel( new BorderLayout() );
		// - - - - - -
		leftAssignmentViewer = new AssignmentViewer( ( int ) model.mm.getImgRaw().dimension( 1 ), this );
		if ( ilp != null )
			leftAssignmentViewer.display( ilp.getAllCompatibleRightAssignments( model.getCurrentTime() - 1 ) );
		// - - - - - -
		panelVerticalHelper.add( leftAssignmentViewer, BorderLayout.CENTER );
		panelView.add( panelVerticalHelper );

		// --- Center data viewer (t) -------------

		panelVerticalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		labelHelper = new JLabel( "t" );
		panelHorizontalHelper.add( labelHelper );
		panelVerticalHelper.add( panelHorizontalHelper, BorderLayout.NORTH );
		// - - - - - -
		imgCanvasActiveCenter = new Viewer2DCanvas( this, MotherMachine.GL_WIDTH_IN_PIXELS + 2 * MotherMachine.GL_PIXEL_PADDING_IN_VIEWS, ( int ) model.mm.getImgRaw().dimension( 1 ) );
		panelVerticalHelper.add( imgCanvasActiveCenter, BorderLayout.CENTER );
		panelVerticalHelper.setBorder( BorderFactory.createMatteBorder( 3, 3, 3, 3, Color.RED ) );
		panelVerticalHelper.setBackground( Color.BLACK );
		panelView.add( panelVerticalHelper );

		// --- Right assignment viewer (t -> t+1) -------------
		panelVerticalHelper = new JPanel( new BorderLayout() );
		// - - - - - -
		rightAssignmentViewer = new AssignmentViewer( ( int ) model.mm.getImgRaw().dimension( 1 ), this );
		if ( ilp != null )
			rightAssignmentViewer.display( ilp.getAllCompatibleRightAssignments( model.getCurrentTime() ) );
		panelVerticalHelper.add( rightAssignmentViewer, BorderLayout.CENTER );
		panelView.add( panelVerticalHelper );

		// ---  Right data viewer (t+1) -------------

		panelVerticalHelper = new JPanel( new BorderLayout() );
		panelHorizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		labelHelper = new JLabel( "t+1" );
		panelHorizontalHelper.add( labelHelper );
		panelVerticalHelper.add( panelHorizontalHelper, BorderLayout.NORTH );
		// - - - - - -
		imgCanvasActiveRight = new Viewer2DCanvas( this, MotherMachine.GL_WIDTH_IN_PIXELS + 2 * MotherMachine.GL_PIXEL_PADDING_IN_VIEWS, ( int ) model.mm.getImgRaw().dimension( 1 ) );
		panelVerticalHelper.add( imgCanvasActiveRight, BorderLayout.CENTER );
		panelVerticalHelper.setBorder( BorderFactory.createMatteBorder( 2, 2, 2, 2, Color.GRAY ) );
		panelVerticalHelper.setBackground( Color.BLACK );
		panelView.add( panelVerticalHelper );

		panelView.add( new JPanel() );


		// ---  ROW OF CHECKBOXES -------------

		final JLabel lblCheckBoxLine = new JLabel( "Correct are:" );
		panelView.add( lblCheckBoxLine, "align center" );
		// - - - - - -
		cbSegmentationOkLeft = new JCheckBox();
		panelView.add( cbSegmentationOkLeft, "align center" );
		// - - - - - -
		cbAssignmentsOkLeft = new JCheckBox();
		panelView.add( cbAssignmentsOkLeft, "align center" );
		// - - - - - -
		cbSegmentationOkCenter = new JCheckBox();
		panelView.add( cbSegmentationOkCenter, "align center" );
		// - - - - - -
		cbAssignmentsOkRight = new JCheckBox();
		panelView.add( cbAssignmentsOkRight, "align center" );
		// - - - - - -
		cbSegmentationOkRight = new JCheckBox();
		panelView.add( cbSegmentationOkRight, "align center" );
		// - - - - - -
		bCheckBoxLineFixHistory = new JButton( "<-all" );
		bCheckBoxLineFixHistory.addActionListener( this );
		bCheckBoxLineSet = new JButton( "set" );
		bCheckBoxLineSet.addActionListener( this );
		panelView.add( bCheckBoxLineSet, "align center" );
		bCheckBoxLineReset = new JButton( "reset" );
		bCheckBoxLineReset.addActionListener( this );

		// - - - - - -

		panelView.add( bCheckBoxLineFixHistory, "align center" );
		panelView.add( panelIsee, "cell 1 2 5 1, align center" );
		panelView.add( bCheckBoxLineReset, "align center, wrap" );

		panelDropdown.setBorder( BorderFactory.createEmptyBorder( 15, 0, 0, 0 ) );
		panelView.add( panelDropdown, "cell 1 3 5 1, align center, wrap" );

		panelViewCenterHelper.add( panelView );
		panelContent.add( panelViewCenterHelper, BorderLayout.CENTER );

		return panelContent;
	}

	/**
	 * @return
	 */
	private JPanel buildDetailedDataView() {
		final JPanel panelDataView = new JPanel( new BorderLayout() );

		plot = new Plot2DPanel();
		updatePlotPanels();
		plot.setPreferredSize( new Dimension( 500, 500 ) );
		panelDataView.add( plot, BorderLayout.CENTER );

		return panelDataView;
	}

	/**
	 * Removes all plots from the plot panel and adds new ones showing the data
	 * corresponding to the current slider setting.
	 */
	private void updatePlotPanels() {

		final GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();

		// Intensity plot
		// --------------
		plot.removeAllPlots();

		final float[] yMidline = model.getCurrentGLF().getMirroredCenterLineValues( model.mm.getImgTemp() );
		float[] ySegmentationData;
//		if ( cbWhichImgToShow.getSelectedItem().equals( itemPMFRF ) ) {
//			ySegmentationData = model.getCurrentGLF().getAwesomeGapSeparationValues( model.mm.getImgTemp() );
//		} else {
			ySegmentationData = model.getCurrentGLF().getSimpleGapSeparationValues( model.mm.getImgTemp() );
//		}
		final float[] yAvg = new float[ yMidline.length ];
		final float constY = SimpleFunctionAnalysis.getSum( ySegmentationData ) / ySegmentationData.length;
		for ( int i = 0; i < yAvg.length; i++ ) {
			yAvg[ i ] = constY;
		}
		plot.addLinePlot( "Midline Intensities", new Color( 127, 127, 255 ), Util.makeDoubleArray( yMidline ) );
		plot.addLinePlot( "Segmentation data", new Color( 80, 255, 80 ), Util.makeDoubleArray( ySegmentationData ) );
		plot.addLinePlot( "avg. fkt-value", new Color( 200, 64, 64 ), Util.makeDoubleArray( yAvg ) );

		plot.setFixedBounds( 1, 0.0, 1.0 );

		// ComponentTreeNodes
		// ------------------
		if ( ilp != null ) {
			dumpCosts( model.getCurrentGLF().getComponentTree(), ySegmentationData, ilp );
		}
	}

	private < C extends Component< FloatType, C > > void dumpCosts( final ComponentForest< C > ct, final float[] ySegmentationData, final GrowthLineTrackingILP ilp ) {
		final int numCTNs = ComponentTreeUtils.countNodes( ct );
		final float[][] xydxdyCTNBorders = new float[ numCTNs ][ 4 ];
		final int t = sliderTime.getValue();
		final float[][] xydxdyCTNBordersActive = new float[ ilp.getOptimalSegmentation( t ).size() ][ 4 ];

		int i = 0;
		for ( final C root : ct.roots() ) {
			System.out.println( "" );
			int level = 0;
			ArrayList< C > ctnLevel = new ArrayList< C >();
			ctnLevel.add( root );
			while ( ctnLevel.size() > 0 ) {
				for ( final Component< ?, ? > ctn : ctnLevel ) {
					addBoxAtIndex( i, ctn, xydxdyCTNBorders, ySegmentationData, level );
//					if ( cbWhichImgToShow.getSelectedItem().equals( itemPMFRF ) ) {
//						System.out.print( String.format( "%8.4f;\t", ilp.localParamaxflowBasedCost( t, ctn ) ) );
//					} else {
					System.out.print( String.format(
							"%8.4f;\t",
							ilp.localIntensityBasedCost( t, ctn ) ) );
//					}
					i++;
				}
				ctnLevel = ComponentTreeUtils.getAllChildren( ctnLevel );
				level++;
				System.out.println( "" );
			}

			i = 0;
			for ( final Hypothesis< Component< FloatType, ? >> hyp : ilp.getOptimalSegmentation( t ) ) {
				final Component< FloatType, ? > ctn = hyp.getWrappedHypothesis();
				addBoxAtIndex( i, ctn, xydxdyCTNBordersActive, ySegmentationData, ComponentTreeUtils.getLevelInTree( ctn ) );
				i++;
			}
		}
		plot.addBoxPlot( "Seg. Hypothesis", new Color( 127, 127, 127, 255 ), Util.makeDoubleArray2d( xydxdyCTNBorders ) );
		if ( ilp.getOptimalSegmentation( t ).size() > 0 ) {
			plot.addBoxPlot( "Active Seg. Hypothesis", new Color( 255, 0, 0, 255 ), Util.makeDoubleArray2d( xydxdyCTNBordersActive ) );
		}
	}

	/**
	 * @param index
	 * @param ctn
	 * @param boxDataArray
	 * @param ydata
	 * @param level
	 */
	@SuppressWarnings( "unchecked" )
	private void addBoxAtIndex( final int index, final Component< ?, ? > ctn, final float[][] boxDataArray, final float[] ydata, final int level ) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		Iterator< Localizable > componentIterator = ctn.iterator();
		if ( ctn instanceof FilteredComponent ) {
			componentIterator = ( ( FilteredComponent< FloatType > ) ctn ).iteratorExtended();
		}
		while ( componentIterator.hasNext() ) {
			final int pos = componentIterator.next().getIntPosition( 0 );
			min = Math.min( min, pos );
			max = Math.max( max, pos );
		}
		final int leftLocation = min;
		final int rightLocation = max;
		boxDataArray[ index ] = new float[] { 0.5f * ( leftLocation + rightLocation ) + 1, 1.0f - level * 0.05f - 0.02f, rightLocation - leftLocation, 0.02f };
	}

	// -------------------------------------------------------------------------------------
	// getters and setters
	// -------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * Picks the right hyperslice in Z direction in imgRaw and sets an
	 * View.offset according to the current offset settings. Note: this method
	 * does not and should not invoke a repaint!
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public void dataToDisplayChanged() {

		final GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();

		// IF 'COUNTING VIEW' VIEW IS ACTIVE
		// =================================
		if ( tabsViews.getComponent( tabsViews.getSelectedIndex() ).equals( panelCountingView ) ) {
			if ( ilp != null ) {
				panelCountingView.showData( model.getCurrentGL() );
			} else {
				panelCountingView.showData( null );
			}
		}

		// IF SEGMENTATION AND ASSIGNMENT VIEW IS ACTIVE
		// =============================================
		if ( tabsViews.getComponent( tabsViews.getSelectedIndex() ).equals( panelSegmentationAndAssignmentView ) ) {
			// - - t-1 - - - - - -

			if ( model.getCurrentGLFsPredecessor() != null ) {
				final GrowthLineFrame glf = model.getCurrentGLFsPredecessor();
				viewImgLeftActive = Views.offset( Views.hyperSlice( model.mm.getImgRaw(), 2, glf.getOffsetF() ), glf.getOffsetX() - MotherMachine.GL_WIDTH_IN_PIXELS / 2 - MotherMachine.GL_PIXEL_PADDING_IN_VIEWS, glf.getOffsetY() );
				imgCanvasActiveLeft.setScreenImage( glf, viewImgLeftActive );
			} else {
				// show something empty
				imgCanvasActiveLeft.setEmptyScreenImage();
			}

			// - - t+1 - - - - - -

			if ( model.getCurrentGLFsSuccessor() != null && sliderTime.getValue() < sliderTime.getMaximum() ) { // hence copy of last frame for border-problem avoidance
				final GrowthLineFrame glf = model.getCurrentGLFsSuccessor();
				viewImgRightActive = Views.offset( Views.hyperSlice( model.mm.getImgRaw(), 2, glf.getOffsetF() ), glf.getOffsetX() - MotherMachine.GL_WIDTH_IN_PIXELS / 2 - MotherMachine.GL_PIXEL_PADDING_IN_VIEWS, glf.getOffsetY() );
				imgCanvasActiveRight.setScreenImage( glf, viewImgRightActive );
			} else {
				// show something empty
				imgCanvasActiveRight.setEmptyScreenImage();
			}

			// - -  t  - - - - - -

			final GrowthLineFrame glf = model.getCurrentGLF();
			final IntervalView< FloatType > paramaxflowSumImageFloatTyped = model.getCurrentGLF().getParamaxflowSumImageFloatTyped( null );
			final FloatType min = new FloatType();
			final FloatType max = new FloatType();

//			if ( paramaxflowSumImageFloatTyped != null && cbWhichImgToShow.getSelectedItem().equals( itemPMFRF ) ) {
//				imgCanvasActiveCenter.setScreenImage( glf, paramaxflowSumImageFloatTyped );
//			} else
			if ( cbWhichImgToShow.getSelectedItem().equals( itemChannel0 ) ) {
				viewImgCenterActive = Views.offset( Views.hyperSlice( model.mm.getImgRaw(), 2, glf.getOffsetF() ), glf.getOffsetX() - MotherMachine.GL_WIDTH_IN_PIXELS / 2 - MotherMachine.GL_PIXEL_PADDING_IN_VIEWS, glf.getOffsetY() );
				imgCanvasActiveCenter.setScreenImage( glf, viewImgCenterActive );
			} else if ( cbWhichImgToShow.getSelectedItem().equals( itemChannel1 ) ) {
				final IntervalView< FloatType > viewToShow = Views.hyperSlice( model.mm.getRawChannelImgs().get( 1 ), 2, glf.getOffsetF() );
				Util.computeMinMax( Views.iterable( viewToShow ), min, max );
				viewImgCenterActive = Views.offset( Converters.convert( viewToShow, new RealFloatNormalizeConverter( max.get() ), new FloatType() ), glf.getOffsetX() - MotherMachine.GL_WIDTH_IN_PIXELS / 2 - MotherMachine.GL_PIXEL_PADDING_IN_VIEWS, glf.getOffsetY() );
				imgCanvasActiveCenter.setScreenImage( glf, viewImgCenterActive );
			} else if ( cbWhichImgToShow.getSelectedItem().equals( itemChannel2 ) ) {
				final IntervalView< FloatType > viewToShow = Views.hyperSlice( model.mm.getRawChannelImgs().get( 2 ), 2, glf.getOffsetF() );
				Util.computeMinMax( Views.iterable( viewToShow ), min, max );
				viewImgCenterActive = Views.offset( Converters.convert( viewToShow, new RealFloatNormalizeConverter( max.get() ), new FloatType() ), glf.getOffsetX() - MotherMachine.GL_WIDTH_IN_PIXELS / 2 - MotherMachine.GL_PIXEL_PADDING_IN_VIEWS, glf.getOffsetY() );
				imgCanvasActiveCenter.setScreenImage( glf, viewImgCenterActive );
//			} else if ( cbWhichImgToShow.getSelectedItem().equals( itemClassified ) ) {
//				final Thread t = new Thread() {
//
//					@Override
//					public void run() {
//						final IntervalView< FloatType > sizeEstimationImageFloatTyped = Views.offset( Views.hyperSlice( model.mm.getCellClassificationImgs(), 2, glf.getOffsetF() ), glf.getOffsetX() - MotherMachine.GL_WIDTH_IN_PIXELS / 2 - MotherMachine.GL_PIXEL_PADDING_IN_VIEWS, glf.getOffsetY() );
//						imgCanvasActiveCenter.setScreenImage( glf, sizeEstimationImageFloatTyped );
//					}
//				};
//				t.start();
//			} else if ( cbWhichImgToShow.getSelectedItem().equals( itemSegmented ) ) {
//				final Thread t = new Thread() {
//
//					@Override
//					public void run() {
//						final IntervalView< FloatType > sizeEstimationImageFloatTyped = Views.offset( Converters.convert( Views.hyperSlice( model.mm.getCellSegmentedChannelImgs(), 2, glf.getOffsetF() ), new RealFloatNormalizeConverter( 1.0f ), new FloatType() ), glf.getOffsetX() - MotherMachine.GL_WIDTH_IN_PIXELS / 2 - MotherMachine.GL_PIXEL_PADDING_IN_VIEWS, glf.getOffsetY() );
//						imgCanvasActiveCenter.setScreenImage( glf, sizeEstimationImageFloatTyped );
//					}
//				};
//				t.start();
			} else { // BG-subtracted Channel 0 selected or PMFRF not available
				viewImgCenterActive = Views.offset( Views.hyperSlice( model.mm.getImgTemp(), 2, glf.getOffsetF() ), glf.getOffsetX() - MotherMachine.GL_WIDTH_IN_PIXELS / 2 - MotherMachine.GL_PIXEL_PADDING_IN_VIEWS, glf.getOffsetY() );
				imgCanvasActiveCenter.setScreenImage( glf, viewImgCenterActive );
			}

//			if ( glf.isParaMaxFlowComponentTree() ) {
//				lActiveHyps.setText( "PMFRF" );
//				lActiveHyps.setForeground( Color.red );
//			} else {
//				lActiveHyps.setText( "CT " );
//				lActiveHyps.setForeground( Color.black );
//			}

			// - -  assignment-views  - - - - - -

			if ( ilp != null ) {
				final int t = sliderTime.getValue();
				if ( t == 0 ) {
					leftAssignmentViewer.display( null );
				} else {
					leftAssignmentViewer.display( ilp.getAllCompatibleRightAssignments( t - 1 ) );
				}
				if ( t == sliderTime.getMaximum() ) {
					rightAssignmentViewer.display( null );
				} else {
					rightAssignmentViewer.display( ilp.getAllCompatibleRightAssignments( t ) );
				}
			} else {
				leftAssignmentViewer.display( null );
				rightAssignmentViewer.display( null );
			}

			// - -  i see ? cells  - - - - - -
			updateNumCellsField();
		}

		// IF DETAILED DATA VIEW IS ACTIVE
		// ===============================
		if ( tabsViews.getComponent( tabsViews.getSelectedIndex() ).equals( panelDetailedDataView ) ) {
			updatePlotPanels();
		}
	}

	/**
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged( final ChangeEvent e ) {

		if ( e.getSource().equals( sliderGL ) ) {
			model.setCurrentGL( sliderGL.getValue(), sliderTime.getValue() );
		}

		if ( e.getSource().equals( sliderTime ) ) {
			updateNumCellsField();
		}

		if ( e.getSource().equals( sliderTrackingRange ) ) {
			if ( model.getCurrentGL().getIlp() != null ) {
				model.getCurrentGL().getIlp().ignoreBeyond( sliderTrackingRange.getUpperValue() );
			}
		}

		dataToDisplayChanged();
		this.repaint();
	}

	/**
	 *
	 */
	private void updateNumCellsField() {
		this.lblCurrentTime.setText( String.format( " t = %4d", sliderTime.getValue() ) );
		this.model.setCurrentGLF( sliderTime.getValue() );
		if ( model.getCurrentGL().getIlp() != null ) {
			final int rhs =
					model.getCurrentGL().getIlp().getSegmentsInFrameCountConstraintRHS(
							sliderTime.getValue() );
			if ( rhs == -1 ) {
				txtNumCells.setText( "?" );
			} else {
				txtNumCells.setText( "" + rhs );
			}
		}
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {

		if ( e.getSource().equals( menuProps ) ) {
			final DialogPropertiesEditor propsEditor =
					new DialogPropertiesEditor( this, MotherMachine.props );
			propsEditor.setVisible( true );
		}
		if ( e.getSource().equals( menuLoad ) ) {

			final MotherMachineGui self = this;

			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();

					if ( ilp == null ) {
						prepareOptimization();
						ilp = model.getCurrentGL().getIlp();
					}
					final File file = OsDependentFileChooser.showLoadFileChooser(
							self,
							MotherMachine.STATS_OUTPUT_PATH,
							"Choose tracking to load...",
							null );
					System.out.println( "File to load tracking from: " + file.getAbsolutePath() );
					try {
						ilp.loadState( file );
					} catch ( final IOException e1 ) {
						e1.printStackTrace();
					}
				}

			} );
			t.start();
		}
		if ( e.getSource().equals( menuSave ) ) {

			final GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();

			if ( ilp != null ) { // && ilp.getStatus() != GrowthLineTrackingILP.OPTIMIZATION_NEVER_PERFORMED
				final File file = OsDependentFileChooser.showSaveFileChooser(
						this,
						MotherMachine.STATS_OUTPUT_PATH,
						"Save current tracking to...",
						null );
				System.out.println( "File to save tracking to: " + file.getAbsolutePath() );
				ilp.saveState( file );
			} else {
				JOptionPane.showMessageDialog(
						this,
						"Loaded data must be optimized before tracking can be saved!",
						"Error",
						JOptionPane.ERROR_MESSAGE );
			}
		}
		if ( e.getSource().equals( menuViewShowConsole ) ) {
			MotherMachine.instance.showConsoleWindow( !MotherMachine.instance.isConsoleVisible() );
			MotherMachine.getGuiFrame().setVisible( true );
		}
		if ( e.getSource().equals( menuShowImgTemp ) ) {
			new ImageJ();
			ImageJFunctions.show( MotherMachine.instance.getImgTemp(), "BG-subtracted data" );
		}
		if ( e.getSource().equals( menuShowImgRaw ) ) {
			new ImageJ();
			ImageJFunctions.show( MotherMachine.instance.getRawChannelImgs().get( 0 ), "raw data (ch.0)" );
		}
//		if ( e.getSource().equals( btnSaveFG ) ) {
//			final MotherMachineGui self = this;
//			final Thread t = new Thread( new Runnable() {
//
//				@Override
//				public void run() {
//					final JFileChooser fc = new JFileChooser( MotherMachine.DEFAULT_PATH );
//					fc.addChoosableFileFilter( new ExtensionFileFilter( new String[] { "txt", "TXT" }, "TXT-file" ) );
//
//					if ( fc.showSaveDialog( self ) == JFileChooser.APPROVE_OPTION ) {
//						File file = fc.getSelectedFile();
//						if ( !file.getAbsolutePath().endsWith( ".txt" ) && !file.getAbsolutePath().endsWith( ".TXT" ) ) {
//							file = new File( file.getAbsolutePath() + ".txt" );
//						}
//						MotherMachine.DEFAULT_PATH = file.getParent();
//
//						if ( model.getCurrentGL().getIlp() == null ) {
//							System.out.println( "Generating ILP..." );
//							model.getCurrentGL().generateILP( new DialogProgress( self, "Building tracking model...", ( model.getCurrentGL().size() - 1 ) * 2 ) );
//						} else {
//							System.out.println( "Using existing ILP (possibly containing user-defined ground-truth bits)..." );
//						}
//						System.out.println( "Saving ILP as FactorGraph..." );
//						model.getCurrentGL().getIlp().exportFG( file );
//						System.out.println( "...done!" );
//					}
//
//				}
//			} );
//			t.start();
//		}
//		if ( e.getSource().equals( btnRedoAllHypotheses ) ) {
//
////			final int choiceAwesome = JOptionPane.showOptionDialog( this, "Do you want to reset to PMFRF segmentations?\n(Otherwise fast CT segments will be built.)", "PMFRF or CT?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null );
//			final DialogResegmentSetup dSetup = new DialogResegmentSetup( this, true, true );
//			dSetup.ask();
//			if ( !dSetup.wasCanceled() ) {
//				final JSlider sliderGL = this.sliderGL;
//
//				final Thread t = new Thread( new Runnable() {
//
//					@Override
//					public void run() {
//						if ( dSetup.allFrames() ) {
//							for ( int i = sliderGL.getMinimum(); i <= sliderGL.getMaximum(); i++ ) {
//								sliderGL.setValue( i );
//								dataToDisplayChanged();
//								if ( dSetup.doPMFRF() ) {
//									activateAwesomeHypothesesForCurrentGL();
//								} else {
//									activateSimpleHypotheses();
//								}
//							}
//						} else {
//							if ( dSetup.doPMFRF() ) {
//								activateAwesomeHypothesesForCurrentGL();
//							} else {
//								activateSimpleHypotheses();
//							}
//						}
//						dataToDisplayChanged();
//					}
//				} );
//				t.start();
//			}
//		}
		if ( e.getSource().equals( bCheckBoxLineSet ) ) {
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					setAllVariablesFixedWhereChecked();

					System.out.println( "Finding optimal result..." );
					model.getCurrentGL().runILP();
					System.out.println( "...done!" );

					sliderTime.requestFocus();
					dataToDisplayChanged();
				}

			} );
			t.start();
		}
		if ( e.getSource().equals( bCheckBoxLineReset ) ) {
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					setAllVariablesFreeWhereUnchecked();

					System.out.println( "Finding optimal result..." );
					model.getCurrentGL().runILP();
					System.out.println( "...done!" );

					sliderTime.requestFocus();
					dataToDisplayChanged();
				}

			} );
			t.start();
		}
		if ( e.getSource().equals( bCheckBoxLineFixHistory ) ) {
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					final int t = sliderTime.getValue();
					final int extent =
							sliderTrackingRange.getUpperValue() - sliderTrackingRange.getValue();
					sliderTrackingRange.setValue( t );
					sliderTrackingRange.setExtent( extent );
					setAllVariablesFixedUpTo( t );

					System.out.println( "Finding optimal result..." );
					model.getCurrentGL().runILP();
					System.out.println( "...done!" );

					sliderTime.requestFocus();
					dataToDisplayChanged();
				}

			} );
			t.start();
		}
		if ( e.getSource().equals( btnReoptimize ) ) {
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					prepareOptimization();

					sliderTrackingRange.setValue( 0 );
					model.getCurrentGL().getIlp().ignoreBeyond( sliderTrackingRange.getUpperValue() );

					System.out.println( "Finding optimal result..." );
					model.getCurrentGL().runILP();
					System.out.println( "...done!" );

					sliderTime.requestFocus();
					dataToDisplayChanged();
				}

			} );
			t.start();
		}
		if ( e.getSource().equals( btnOptimizeMore ) ) {
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					if ( model.getCurrentGL().getIlp() == null ) {
						prepareOptimization();
					}

					model.getCurrentGL().getIlp().ignoreBeyond( sliderTrackingRange.getUpperValue() );

					System.out.println( "Finding optimal result..." );
					model.getCurrentGL().runILP();
					System.out.println( "...done!" );

					sliderTime.requestFocus();
					dataToDisplayChanged();
				}

			} );
			t.start();
		}
		if ( e.getSource().equals( btnExportHtml ) ) {
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					exportHtmlOverview();
				}
			} );
			t.start();
		}
		if ( e.getSource().equals( btnExportData ) ) {
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					exportDataFiles();
				}
			} );
			t.start();
		}
	}

	/**
	 * @param self
	 * @return
	 */
	public void prepareOptimization() {
		System.out.println( "Filling in CT hypotheses where needed..." );
		for ( final GrowthLineFrame glf : model.getCurrentGL().getFrames() ) {
			if ( glf.getComponentTree() == null ) {
				glf.generateSimpleSegmentationHypotheses( MotherMachine.instance.getImgTemp() );
			}
		}

		System.out.println( "Generating ILP..." );
		if ( MotherMachine.HEADLESS ) {
			model.getCurrentGL().generateILP( null );
		} else {
			model.getCurrentGL().generateILP(
					new DialogProgress( this, "Building tracking model...", ( model.getCurrentGL().size() - 1 ) * 2 ) );
		}
	}

	/**
	 * Depending on which checkboxes are checked, fix ALL respective
	 * segmentations and assignments to current ILP state.
	 */
	protected void setAllVariablesFixedWhereChecked() {
		final GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();
		final int t = sliderTime.getValue();
		if ( ilp != null ) {
			if ( cbSegmentationOkLeft.isSelected() ) {
				ilp.fixSegmentationAsIs( t - 1 );
			}
			if ( cbAssignmentsOkLeft.isSelected() ) {
				ilp.fixAssignmentsAsAre( t - 1 );
			}
			if ( cbSegmentationOkCenter.isSelected() ) {
				ilp.fixSegmentationAsIs( t );
			}
			if ( cbAssignmentsOkRight.isSelected() ) {
				ilp.fixAssignmentsAsAre( t );
			}
			if ( cbSegmentationOkRight.isSelected() ) {
				ilp.fixSegmentationAsIs( t + 1 );
			}
		}
	}

	/**
	 * Depending on which checkboxes are checked, fix ALL respective
	 * segmentations and assignments to current ILP state.
	 */
	protected void setAllVariablesFixedUpTo( final int t ) {
		final GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();
		if ( ilp != null ) {
			ilp.fixSegmentationAsIs( 0 );
			for ( int i = 1; i <= t; i++ ) {
				ilp.fixAssignmentsAsAre( i - 1 );
				ilp.fixSegmentationAsIs( i );
			}
		}
	}

	/**
	 * Depending on which checkboxes are UNchecked, free ALL respective
	 * segmentations and assignments if they are clamped to any value in the
	 * ILP.
	 */
	protected void setAllVariablesFreeWhereUnchecked() {
		final GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();
		final int t = sliderTime.getValue();
		if ( ilp != null ) {
			if ( cbSegmentationOkLeft.isSelected() ) {
				ilp.removeAllSegmentConstraints( t - 1 );
			}
			if ( cbAssignmentsOkLeft.isSelected() ) {
				ilp.removeAllAssignmentConstraints( t - 1 );
			}
			if ( cbSegmentationOkCenter.isSelected() ) {
				ilp.removeAllSegmentConstraints( t );
			}
			if ( cbAssignmentsOkRight.isSelected() ) {
				ilp.removeAllAssignmentConstraints( t );
			}
			if ( cbSegmentationOkRight.isSelected() ) {
				ilp.removeAllSegmentConstraints( t + 1 );
			}
		}
	}

	/**
	 *
	 */
	public void exportDataFiles() {
		if ( model.getCurrentGL().getIlp() == null ) {
			JOptionPane.showMessageDialog( this, "The current GL can only be exported after being tracked (optimized)!" );
			return;
		}

		final CellStatsExporter exporter = new CellStatsExporter( this );
		exporter.export();
	}

	/**
	 *
	 */
	public void exportHtmlOverview() {
		final MotherMachineGui self = this;

		if ( model.getCurrentGL().getIlp() == null ) {
			JOptionPane.showMessageDialog( this, "The current GL can only be exported after being tracked (optimized)!" );
			return;
		}

		boolean doExport = true;
		int startFrame = 0;
		int endFrame = sliderTime.getMaximum();

		File file = new File( String.format( MotherMachine.STATS_OUTPUT_PATH + String.format( "/index.html" ) ) );

		if ( !MotherMachine.HEADLESS ) {
			final JFileChooser fc = new JFileChooser();
			fc.setSelectedFile( file );
			fc.addChoosableFileFilter( new ExtensionFileFilter( new String[] { "html" }, "HTML-file" ) );

			if ( fc.showSaveDialog( self ) == JFileChooser.APPROVE_OPTION ) {
				file = fc.getSelectedFile();
				if ( !file.getAbsolutePath().endsWith( ".html" ) && !file.getAbsolutePath().endsWith( ".htm" ) ) {
					file = new File( file.getAbsolutePath() + ".html" );
				}
				MotherMachine.STATS_OUTPUT_PATH = file.getParent();

				boolean done = false;
				while ( !done ) {
					try {
						final String str = ( String ) JOptionPane.showInputDialog( self, "First frame to be exported:", "Start at...", JOptionPane.QUESTION_MESSAGE, null, null, "" + startFrame );
						if ( str == null ) return; // User decided to hit cancel!
						startFrame = Integer.parseInt( str );
						done = true;
					} catch ( final NumberFormatException nfe ) {
						done = false;
					}
				}
				done = false;
				while ( !done ) {
					try {
						final String str = ( String ) JOptionPane.showInputDialog( self, "Last frame to be exported:", "End with...", JOptionPane.QUESTION_MESSAGE, null, null, "" + endFrame );
						if ( str == null ) return; // User decided to hit cancel!
						endFrame = Integer.parseInt( str );
						done = true;
					} catch ( final NumberFormatException nfe ) {
						done = false;
					}
				}
			} else {
				doExport = false;
			}
		}

		// ----------------------------------------------------------------------------------------------------
		if ( doExport ) {
			exportHtmlTrackingOverview( file, startFrame, endFrame );
		}
		// ----------------------------------------------------------------------------------------------------

		if ( !MotherMachine.HEADLESS ) {
			dataToDisplayChanged();
		}
	}

	/**
	 * Goes over all glfs of the current gl and activates the simple, intensity
	 * + comp.tree hypotheses.
	 */
	private void activateSimpleHypotheses() {
		activateSimpleHypothesesForGL( model.getCurrentGL() );
	}

	private void activateSimpleHypothesesForGL( final GrowthLine gl ) {
		for ( final GrowthLineFrame glf : gl.getFrames() ) {
			System.out.print( "." );
			glf.generateSimpleSegmentationHypotheses( model.mm.getImgTemp() );
		}
		System.out.println( "" );
	}

	/**
	 * Goes over all glfs of the current gl and activates the awesome,
	 * RF-classified + paramaxflow hypotheses.
	 */
	private void activateAwesomeHypothesesForCurrentGL() {
		activateAwesomeHypothesesForGL( model.getCurrentGL() );
	}

	/**
	 * Goes over all glfs of the given gl and activates the awesome,
	 * RF-classified + paramaxflow hypotheses.
	 */
	private void activateAwesomeHypothesesForGL( final GrowthLine gl ) {
		// Since I am gonna mix CT and PMFRF, I have to also ensure to have the CT ones available
		if ( MotherMachine.SEGMENTATION_MIX_CT_INTO_PMFRF > 0.0001 ) {
			activateSimpleHypothesesForGL( gl );
		}

		final int numProcessors = Prefs.getThreads();
		final int numThreads = Math.min( model.getCurrentGL().getFrames().size(), numProcessors );
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

				for ( int i = numThread; i < model.getCurrentGL().getFrames().size(); i += numThreads ) {
					gl.getFrames().get( i ).generateAwesomeSegmentationHypotheses( model.mm.getImgTemp() );
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
			} catch ( final InterruptedException e ) {}
		}

		// OLD SINGLETHREADED VERSION
//		for ( final GrowthLineFrame glf : model.getCurrentGL().getFrames() ) {
//			System.out.println( ">>>>> Generating PMFRF hypotheses for GLF #" + glf.getTime() );
//			glf.generateAwesomeSegmentationHypotheses( model.mm.getImgTemp() );
//		}
//		System.out.print( "" );

		// NEW SINGLETHREADED VERSION
//		for ( int i = this.sliderTime.getMinimum(); i <= this.sliderTime.getMaximum(); i++ ) {
//			sliderTime.setValue( i );
//			if ( model.getCurrentGLF().getAwesomeGapSeparationValues( null ) == null ) {
//				btnExchangeSegHyps.doClick();
//			}
//			dataToDisplayChanged();
//		}
	}

	/**
	 * Exports current tracking solution as individual PNG images in the given
	 * folder.
	 *
	 * @param endFrame
	 * @param startFrame
	 *
	 * @param folder
	 *            path to folder in which to store PNGs.
	 */
	private void exportHtmlTrackingOverview( final File htmlFileToSaveTo, final int startFrame, final int endFrame ) {
		System.out.println( "Exporting html tracking overview..." );

		final String path = htmlFileToSaveTo.getParent();
		final String imgpath = path + "/imgs";

		final HtmlOverviewExporter exporter = new HtmlOverviewExporter( this, htmlFileToSaveTo, imgpath, startFrame, endFrame );
		exporter.run();

		System.out.println( "...done!" );

	}

	/**
	 * Requests the focus on the slider controlling the time (frame).
	 */
	public void focusOnSliderTime() {
		sliderTime.requestFocus();
	}
}
