/**
 *
 */
package com.jug.gui;

import gurobi.GRBException;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.Pair;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.algorithm.componenttree.ComponentForest;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.math.plot.Plot2DPanel;

import com.jug.GrowthLine;
import com.jug.GrowthLineFrame;
import com.jug.MotherMachine;
import com.jug.gui.progress.DialogProgress;
import com.jug.lp.AbstractAssignment;
import com.jug.lp.DivisionAssignment;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.lp.Hypothesis;
import com.jug.lp.MappingAssignment;
import com.jug.util.ComponentTreeUtils;
import com.jug.util.SimpleFunctionAnalysis;
import com.jug.util.Util;

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
	private Viewer2DCanvas imgCanvasActiveLeft;
	private Viewer2DCanvas imgCanvasActiveCenter;
	private Viewer2DCanvas imgCanvasActiveRight;

	private JSlider sliderGL;
	private JSlider sliderTime;
	private JLabel lblCurrentTime;

	private JTabbedPane tabsViews;
	private CountOverviewPanel panelCountingView;
	private JPanel panelSegmentationAndAssignmentView;
	private JPanel panelDetailedDataView;
	private Plot2DPanel plot;

	private AssignmentViewer leftAssignmentViewer;
	private AssignmentViewer rightAssignmentViewer;

	private JButton btnRedoAllHypotheses;
	private JButton btnExchangeSegHyps;
	private JButton btnOptimize;
	private JButton btnExportAllStats;
	private JButton btnSaveFG;

	private JCheckBox cbShowParaMaxFlowData;

	private JLabel lActiveHyps;

	private JTextField txtNumCells;

	// Menu-items
	private MenuItem menuViewShowConsole;

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
		final Menu menuView = new Menu( "View" );
		menuViewShowConsole = new MenuItem( "Show/hide Console" );
		menuViewShowConsole.addActionListener( this );
		menuView.add( menuViewShowConsole );
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
		panelContent.add( panelHorizontalHelper, BorderLayout.SOUTH );

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
		btnRedoAllHypotheses = new JButton( "Resegment" );
		btnRedoAllHypotheses.addActionListener( this );
		btnOptimize = new JButton( "(re)Optimize" );
		btnOptimize.addActionListener( this );
		btnExportAllStats = new JButton( "Export" );
		btnExportAllStats.addActionListener( this );
		btnSaveFG = new JButton( "Save FG" );
		btnSaveFG.addActionListener( this );
		panelHorizontalHelper = new JPanel( new FlowLayout( FlowLayout.RIGHT, 5, 0 ) );
		panelHorizontalHelper.add( btnRedoAllHypotheses );
		panelHorizontalHelper.add( btnOptimize );
		panelHorizontalHelper.add( btnExportAllStats );
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
				if ( e.getActionCommand().equals( "o" ) ) {
					btnOptimize.doClick();
				}
				if ( e.getActionCommand().equals( "e" ) ) {
					btnExportAllStats.doClick();
				}
				if ( e.getActionCommand().equals( "r" ) ) {
					btnRedoAllHypotheses.doClick();
				}
				if ( e.getActionCommand().equals( " " ) ) {
					cbShowParaMaxFlowData.doClick();
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

		final JPanel panelView = new JPanel( new FlowLayout( FlowLayout.CENTER, 0, 10 ) );
		final JPanel panelOptions = new JPanel();
		panelOptions.setLayout( new BoxLayout( panelOptions, BoxLayout.LINE_AXIS ) );

		// =============== panelOptions-part ===================
		cbShowParaMaxFlowData = new JCheckBox( "show PMFRF if avlbl", false );
		cbShowParaMaxFlowData.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				dataToDisplayChanged();
			}
		} );
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
				ilp.run();
				dataToDisplayChanged();
				sliderTime.requestFocus();
			}
		} );

		btnExchangeSegHyps = new JButton( "switch" );
		btnExchangeSegHyps.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( final ActionEvent e ) {
				final GrowthLineFrame glf = model.getCurrentGLF();
				if ( !glf.isParaMaxFlowComponentTree() ) {
					glf.generateAwesomeSegmentationHypotheses( model.mm.getImgTemp() );
				} else {
					glf.generateSimpleSegmentationHypotheses( model.mm.getImgTemp() );
				}
				dataToDisplayChanged();
			}
		} );
		lActiveHyps = new JLabel( "CT" );
		lActiveHyps.setHorizontalAlignment( SwingConstants.CENTER );
		lActiveHyps.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder( 2, 5, 2, 5 ) ) );
		lActiveHyps.setPreferredSize( new Dimension( 65, lActiveHyps.getPreferredSize().height ) );

		panelOptions.add( Box.createHorizontalGlue() );
		panelOptions.add( cbShowParaMaxFlowData );
		panelOptions.add( Box.createHorizontalGlue() );
		panelOptions.add( labelNumCells1 );
		panelOptions.add( txtNumCells );
		panelOptions.add( labelNumCells2 );
		panelOptions.add( Box.createHorizontalGlue() );
		panelOptions.add( btnExchangeSegHyps );
		panelOptions.add( lActiveHyps );
		panelOptions.add( Box.createHorizontalGlue() );

		// =============== panelView-part ===================

		JPanel panelVerticalHelper;
		JPanel panelHorizontalHelper;
		JLabel labelHelper;

		final GrowthLineTrackingILP ilp = model.getCurrentGL().getIlp();

		// --- Left data viewer (t-1) -------------

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

		panelContent.add( panelView, BorderLayout.CENTER );
		panelContent.add( panelOptions, BorderLayout.SOUTH );

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
		if ( cbShowParaMaxFlowData.isSelected() ) {
			ySegmentationData = model.getCurrentGLF().getAwesomeGapSeparationValues( model.mm.getImgTemp() );
		} else {
			ySegmentationData = model.getCurrentGLF().getSimpleGapSeparationValues( model.mm.getImgTemp() );
		}
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
					if ( cbShowParaMaxFlowData.isSelected() ) {
						System.out.print( String.format( "%.4f;\t", ilp.localParamaxflowBasedCost( t, ctn ) ) );
					} else {
						System.out.print( String.format( "%.4f;\t", ilp.localIntensityBasedCost( t, ctn ) ) );
					}
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
	private void addBoxAtIndex( final int index, final Component< ?, ? > ctn, final float[][] boxDataArray, final float[] ydata, final int level ) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		final Iterator< Localizable > componentIterator = ctn.iterator();
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
			viewImgCenterActive = Views.offset( Views.hyperSlice( model.mm.getImgRaw(), 2, glf.getOffsetF() ), glf.getOffsetX() - MotherMachine.GL_WIDTH_IN_PIXELS / 2 - MotherMachine.GL_PIXEL_PADDING_IN_VIEWS, glf.getOffsetY() );

			final IntervalView< FloatType > paramaxflowSumImageFloatTyped = model.getCurrentGLF().getParamaxflowSumImageFloatTyped( null );
			if ( paramaxflowSumImageFloatTyped != null && cbShowParaMaxFlowData.isSelected() ) {
				imgCanvasActiveCenter.setScreenImage( glf, paramaxflowSumImageFloatTyped );
			} else {
				imgCanvasActiveCenter.setScreenImage( glf, viewImgCenterActive );
			}

			if ( glf.isParaMaxFlowComponentTree() ) {
				lActiveHyps.setText( "PMFRF" );
				lActiveHyps.setForeground( Color.red );
			} else {
				lActiveHyps.setText( "CT " );
				lActiveHyps.setForeground( Color.black );
			}

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
			this.lblCurrentTime.setText( String.format( " t = %4d", sliderTime.getValue() ) );
			this.model.setCurrentGLF( sliderTime.getValue() );
			if ( model.getCurrentGL().getIlp() != null ) {
				final int rhs = model.getCurrentGL().getIlp().getSegmentsInFrameCountConstraintRHS( sliderTime.getValue() );
				if ( rhs == -1 ) {
					txtNumCells.setText( "?" );
				} else {
					txtNumCells.setText( "" + rhs );
				}
			}
		}

		dataToDisplayChanged();
		this.repaint();
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {

		if ( e.getSource().equals( menuViewShowConsole ) ) {
			MotherMachine.instance.showConsoleWindow( !MotherMachine.instance.isConsoleVisible() );
			MotherMachine.getGuiFrame().setVisible( true );
		}
		if ( e.getSource().equals( btnSaveFG ) ) {
			final MotherMachineGui self = this;
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					final JFileChooser fc = new JFileChooser( MotherMachine.DEFAULT_PATH );
					fc.addChoosableFileFilter( new ExtensionFileFilter( new String[] { "txt", "TXT" }, "TXT-file" ) );

					if ( fc.showSaveDialog( self ) == JFileChooser.APPROVE_OPTION ) {
						File file = fc.getSelectedFile();
						if ( !file.getAbsolutePath().endsWith( ".txt" ) && !file.getAbsolutePath().endsWith( ".TXT" ) ) {
							file = new File( file.getAbsolutePath() + ".txt" );
						}
						MotherMachine.DEFAULT_PATH = file.getParent();

						if ( model.getCurrentGL().getIlp() == null ) {
							System.out.println( "Generating ILP..." );
							model.getCurrentGL().generateILP( new DialogProgress( self, "Building tracking model...", ( model.getCurrentGL().size() - 1 ) * 2 ) );
						} else {
							System.out.println( "Using existing ILP (possibly containing user-defined ground-truth bits)..." );
						}
						System.out.println( "Saving ILP as FactorGraph..." );
						model.getCurrentGL().getIlp().exportFG( file );
						System.out.println( "...done!" );
					}

				}
			} );
			t.start();
		}
		if ( e.getSource().equals( btnRedoAllHypotheses ) ) {

//			final int choiceAwesome = JOptionPane.showOptionDialog( this, "Do you want to reset to PMFRF segmentations?\n(Otherwise fast CT segments will be built.)", "PMFRF or CT?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null );
			final DialogResegmentSetup dSetup = new DialogResegmentSetup( this, true, true );
			dSetup.ask();
			if ( !dSetup.wasCanceled() ) {
				final JSlider sliderGL = this.sliderGL;

				final Thread t = new Thread( new Runnable() {

					@Override
					public void run() {
						if ( dSetup.allFrames() ) {
							for ( int i = sliderGL.getMinimum(); i <= sliderGL.getMaximum(); i++ ) {
								sliderGL.setValue( i );
								dataToDisplayChanged();
								if ( dSetup.doPMFRF() ) {
									activateAwesomeHypothesesForCurrentGL();
								} else {
									activateSimpleHypotheses();
								}
							}
						} else {
							if ( dSetup.doPMFRF() ) {
								activateAwesomeHypothesesForCurrentGL();
							} else {
								activateSimpleHypotheses();
							}
						}
						dataToDisplayChanged();
					}
				} );
				t.start();
			}
		}
		if ( e.getSource().equals( btnOptimize ) ) {
			final MotherMachineGui self = this;
			final Thread t = new Thread( new Runnable() {

				@Override
				public void run() {
					System.out.println( "Filling in CT hypotheses where needed..." );
					for ( final GrowthLineFrame glf : model.getCurrentGL().getFrames() ) {
						if ( glf.getComponentTree() == null ) {
							glf.generateSimpleSegmentationHypotheses( MotherMachine.instance.getImgTemp() );
						}
					}

					System.out.println( "Generating ILP..." );
					model.getCurrentGL().generateILP( new DialogProgress( self, "Building tracking model...", ( model.getCurrentGL().size() - 1 ) * 2 ) );

					System.out.println( "Finding optimal result..." );
					model.getCurrentGL().runILP();
					System.out.println( "...done!" );
					dataToDisplayChanged();
				}
			} );
			t.start();
		}
		if ( e.getSource().equals( btnExportAllStats ) ) {
			exportAllStats();
		}
	}

	/**
	 * 
	 */
	public void exportAllStats() {
		final MotherMachineGui self = this;

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
			exportTrackingImagesAndHtml( file, startFrame, endFrame );
			exportTracks( new File( file.getPath().substring( 0, file.getPath().length() - 5 ) + ".csv" ) );
			try {
				exportCellStats( new File( file.getPath().substring( 0, file.getPath().length() - 5 ) + "_CellStats.csv" ) );
			} catch ( final GRBException e ) {
				e.printStackTrace();
			}
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

		final int numProcessors = 16; //Prefs.getThreads();
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
	public void exportTrackingImagesAndHtml( final File htmlFileToSaveTo, final int startFrame, final int endFrame ) {
		System.out.println( "Exporting tracks as images + html..." );

		final String path = htmlFileToSaveTo.getParent();
		final String imgpath = path + "/imgs";
		final File fImgpath = new File( imgpath );

		String basename = htmlFileToSaveTo.getName();
		final int pos = basename.lastIndexOf( "." );
		if ( pos > 0 ) {
			basename = basename.substring( 0, pos );
		}

		// create folders to imgs if not exists
		if ( !fImgpath.exists() && !fImgpath.mkdirs() ) {
			JOptionPane.showMessageDialog( this, "Saving of HTML canceled! Couldn't create dir: " + fImgpath, "Saving canceled...", JOptionPane.ERROR_MESSAGE );
			return;
		}

		Writer out = null;
		try {
			out = new OutputStreamWriter( new FileOutputStream( htmlFileToSaveTo ) );

			out.write( "<html>\n" );
			out.write( "<body>\n" );
			out.write( "	<table border='0' cellspacing='1' cellpadding='0'>\n" );
			out.write( "		<tr>\n" );

			String row1 = "";
			final String nextrow = "		</tr>\n			<tr>\n";
			String row2 = "";

			for ( int i = startFrame; i <= endFrame; i++ ) {
				this.sliderTime.setValue( i );
				try {
					String fn = String.format( "/" + basename + "_gl_%02d_glf_%03d.png", sliderGL.getValue(), i );
					this.imgCanvasActiveCenter.exportScreenImage( imgpath + fn );
					row1 += "			<th><font size='+2'>t=" + i + "</font></th>\n";
					row2 += "			<td><img src='./imgs" + fn + "'></td>\n";

					if ( i < endFrame ) {
						fn = String.format( "/" + basename + "_gl_%02d_assmnts_%03d.png", sliderGL.getValue(), i );
						Util.saveImage( Util.getImageOf( this.rightAssignmentViewer.getActiveAssignments(), imgCanvasActiveCenter.getWidth(), imgCanvasActiveCenter.getHeight() ), imgpath + fn );
						row1 += "			<th></th>\n";
						row2 += "			<td><img src='./imgs" + fn + "'></td>\n"; // + "' width='10' height='" + this.imgCanvasActiveCenter.getHeight() 
					}
				} catch ( final IOException e ) {
					JOptionPane.showMessageDialog( this, "Tracking imagery could not be saved entirely!", "Export Error", JOptionPane.ERROR_MESSAGE );
					e.printStackTrace();
					out.close();
					return;
				}
			}

			out.write( row1 );
			out.write( nextrow );
			out.write( row2 );

			out.write( "		</tr>\n" );
			out.write( "	</table>\n" );
			out.write( "</body>\n" );
			out.write( "</html>\n" );

			out.close();
		} catch ( final FileNotFoundException e1 ) {
			JOptionPane.showMessageDialog( this, "File not found!", "Error!", JOptionPane.ERROR_MESSAGE );
			e1.printStackTrace();
		} catch ( final IOException e1 ) {
			JOptionPane.showMessageDialog( this, "Selected file could not be written!", "Error!", JOptionPane.ERROR_MESSAGE );
			e1.printStackTrace();
		}
		System.out.println( "...done!" );

	}

	/**
	 * @param self
	 * @param file
	 * @throws GRBException
	 */
	public void exportCellStats( final File file ) throws GRBException {

		final class SegmentRecord {

			private static final int ENDOFTRACKING = 1234;
			public boolean exists = true;
			public int id = -1;
			public int pid = -1;
			public int tbirth = -1;
			public int frame = 0;

			public Hypothesis< Component< FloatType, ? >> hyp;
			private int terminated_by = Integer.MIN_VALUE;

			public SegmentRecord( final Hypothesis< Component< FloatType, ? >> hyp, final int id, final int pid, final int tbirth ) {
				this.hyp = hyp;
				this.id = id;
				this.pid = pid;
				this.tbirth = tbirth;
				this.frame = 0;
			}

			public SegmentRecord( final SegmentRecord point ) {
				this.hyp = point.hyp;
				this.id = point.id;
				this.pid = point.pid;
				this.tbirth = point.tbirth;
				this.frame = point.frame + 1;
			}

			@Override
			public SegmentRecord clone() {
				final SegmentRecord ret = new SegmentRecord( this.hyp, this.id, this.pid, this.tbirth );
				ret.exists = this.exists;
				ret.frame = this.frame;
				ret.terminated_by = this.terminated_by;
				return ret;
			}

			@Override
			public String toString() {
				return String.format( "id=%d; pid=%d; birth-frame=%d", id, pid, tbirth );
			}

			/**
			 * @return
			 */
			public SegmentRecord nextSegmentInTime( final GrowthLineTrackingILP ilp ) {
				SegmentRecord ret = this;

				try {
					final AbstractAssignment< Hypothesis< Component< FloatType, ? >>> rightAssmt = ilp.getOptimalRightAssignment( this.hyp );
					if ( rightAssmt == null ) {
						exists = false;
						terminated_by = SegmentRecord.ENDOFTRACKING;
					} else if ( rightAssmt.getType() == GrowthLineTrackingILP.ASSIGNMENT_MAPPING ) {
						final MappingAssignment ma = ( MappingAssignment ) rightAssmt;
						ret = new SegmentRecord( this );
						ret.hyp = ma.getDestinationHypothesis();
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
		}

		final String loadedDataFolder = MotherMachine.props.getProperty( "import_path", "BUG -- could not get property 'import_path' while exporting cell statistics..." );
		final int numCurrGL = sliderGL.getValue();
		final Vector< String > linesToExport = new Vector< String >();

		final GrowthLineFrame firstGLF = model.getCurrentGL().getFrames().get( 0 );
		final GrowthLineTrackingILP ilp = firstGLF.getParent().getIlp();
		final Vector< ValuePair< Integer, Hypothesis< Component< FloatType, ? >>> > segmentsInFirstFrame = firstGLF.getSortedActiveHypsAndPos();
		final List< SegmentRecord > startingPoints = new ArrayList< SegmentRecord >();

		int nextCellId = 0;
		final LinkedList< SegmentRecord > queue = new LinkedList< SegmentRecord >();

		for ( final ValuePair< Integer, Hypothesis< Component< FloatType, ? >>> valuePair : segmentsInFirstFrame ) {

			final SegmentRecord point = new SegmentRecord( valuePair.b, nextCellId++, -1, -1 );
			startingPoints.add( point );

			final SegmentRecord prepPoint = new SegmentRecord( point );
			prepPoint.hyp = point.hyp;
			queue.add( prepPoint );
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
				final SegmentRecord next = new SegmentRecord( prepPoint );
				next.hyp = ma.getDestinationHypothesis();
				queue.add( next );
			}
			// DIVISON -- NEW CELLS ARE BORN CURRENT ONE ENDS
			if ( rightAssmt.getType() == GrowthLineTrackingILP.ASSIGNMENT_DIVISION ) {
				final DivisionAssignment da = ( DivisionAssignment ) rightAssmt;

				prepPoint.pid = prepPoint.id;
				prepPoint.tbirth = prepPoint.frame;

				prepPoint.id = nextCellId;
				prepPoint.hyp = da.getLowerDesinationHypothesis();
				startingPoints.add( prepPoint.clone() );
				queue.add( new SegmentRecord( prepPoint ) );
				nextCellId++;

				prepPoint.id = nextCellId;
				prepPoint.hyp = da.getLowerDesinationHypothesis();
				startingPoints.add( prepPoint.clone() );
				queue.add( new SegmentRecord( prepPoint ) );
				nextCellId++;
			}
		}

		// Line 1: import folder
		linesToExport.add( loadedDataFolder );

		// Line 2: GL-id
		linesToExport.add( "GLidx = " + numCurrGL );

		// Line 3: #cells
		linesToExport.add( "numCells = " + startingPoints.size() );

		// Line 4: #channels
		linesToExport.add( "numChannels = " + MotherMachine.instance.getRawChannelImgs().size() + "\n" );

		// Export all cells (we found all their starting segments above)
		for ( int cid = 0; cid < startingPoints.size(); cid++ ) {
			SegmentRecord segmentRecord = startingPoints.get( cid );

			linesToExport.add( segmentRecord.toString() );
			do {
				final Pair< Integer, Integer > limits = ComponentTreeUtils.getTreeNodeInterval( segmentRecord.hyp.getWrappedHypothesis() );
				final int height = limits.getB() - limits.getA();
				linesToExport.add( String.format( "\tframe=%d; cell_height=%d\t", segmentRecord.frame, height ) );

				// export info per image channel
				for ( int c = 0; c < MotherMachine.instance.getRawChannelImgs().size(); c++ ) {
					final IntervalView< FloatType > channelFrame = Views.hyperSlice( MotherMachine.instance.getRawChannelImgs().get( c ), 2, segmentRecord.frame );
					final IterableInterval< FloatType > segmentBoxInChannel = Util.getSegmentBoxInImg( channelFrame, segmentRecord.hyp, firstGLF.getAvgXpos() );

					final FloatType min = new FloatType();
					final FloatType max = new FloatType();
					Util.computeMinMax( segmentBoxInChannel, min, max );
					final long[] hist = segmentRecord.computeChannelHistogram( segmentBoxInChannel, min.get(), max.get() );

					String histStr = "\t\tch=" + c;
					histStr += String.format( "; min=%8.3f; max=%8.3f", min.get(), max.get() );
					for ( final long value : hist ) {
						histStr += String.format( "; %5d", value );
					}
					linesToExport.add( histStr );
				}
				segmentRecord = segmentRecord.nextSegmentInTime( ilp );
			}
			while ( segmentRecord.exists() );

			if ( segmentRecord.terminated_by == GrowthLineTrackingILP.ASSIGNMENT_EXIT ) {
				linesToExport.add( "\tEXIT\n" );
			} else if ( segmentRecord.terminated_by == GrowthLineTrackingILP.ASSIGNMENT_DIVISION ) {
				linesToExport.add( "\tDIVISION\n" );
			} else if ( segmentRecord.terminated_by == SegmentRecord.ENDOFTRACKING ) {
				// UGLY TRICK ALERT: remember the trick to fix the tracking towards the last frame?
				// Yes, we double the last frame. This also means that we should not export this fake frame, ergo we remove it here!
				for ( int i = 0; i < MotherMachine.instance.getRawChannelImgs().size() + 1; i++ ) {
					linesToExport.remove( linesToExport.size() - 1 );
				}
				linesToExport.add( "\tENDOFDATA\n" );
			} else {
				linesToExport.add( "\tGUROBI_EXCEPTION\n" );
			}
		}

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
			JOptionPane.showMessageDialog( this, "File not found!", "Error!", JOptionPane.ERROR_MESSAGE );
			e1.printStackTrace();
		} catch ( final IOException e1 ) {
			JOptionPane.showMessageDialog( this, "Selected file could not be written!", "Error!", JOptionPane.ERROR_MESSAGE );
			e1.printStackTrace();
		}
		System.out.println( "...done!" );
	}

	/**
	 * @param file
	 */
	private void exportTracks( final File file ) {
		final String loadedDataFolder = MotherMachine.props.getProperty( "import_path", "BUG -- could not get property 'import_path' while exporting tracks..." );
		final int numCurrGL = sliderGL.getValue();
		final int numGLFs = model.getCurrentGL().getFrames().size();
		final Vector< Vector< String >> dataToExport = new Vector< Vector< String >>();

		final Vector< String > firstLine = new Vector< String >();
		firstLine.add( loadedDataFolder );
		dataToExport.add( firstLine );
		final Vector< String > secondLine = new Vector< String >();
		secondLine.add( "" + numCurrGL );
		secondLine.add( "" + numGLFs );
		dataToExport.add( secondLine );

		int i = 0;
		for ( final GrowthLineFrame glf : model.getCurrentGL().getFrames() ) {
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
			JOptionPane.showMessageDialog( this, "File not found!", "Error!", JOptionPane.ERROR_MESSAGE );
			e1.printStackTrace();
		} catch ( final IOException e1 ) {
			JOptionPane.showMessageDialog( this, "Selected file could not be written!", "Error!", JOptionPane.ERROR_MESSAGE );
			e1.printStackTrace();
		}
		System.out.println( "...done!" );
	}

	/**
	 * Requests the focus on the slider controlling the time (frame).
	 */
	public void focusOnSliderTime() {
		sliderTime.requestFocus();
	}
}
