/**
 *
 */
package com.jug.gui;

import gurobi.GRBException;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import net.imglib2.Pair;
import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.DoubleType;

import com.jug.MotherMachine;
import com.jug.lp.AbstractAssignment;
import com.jug.lp.DivisionAssignment;
import com.jug.lp.ExitAssignment;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.lp.Hypothesis;
import com.jug.lp.MappingAssignment;
import com.jug.util.ComponentTreeUtils;


/**
 * @author jug
 */
public class AssignmentView extends JComponent implements MouseInputListener {

	/**
	 *
	 */
	private static final int DISPLAY_COSTS_ABSOLUTE_X = 10;

	/**
	 *
	 */
	private static final int LINEHEIGHT_DISPLAY_COSTS = 20;

	/**
	 *
	 */
	private static final int OFFSET_DISPLAY_COSTS = 10;

	private static final int HEIGHT_OFFSET = 35;

	// -------------------------------------------------------------------------------------
	// statics
	// -------------------------------------------------------------------------------------
	private static final long serialVersionUID = -2920396224787446598L;

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private final int width;
	private final int height;

	private final int offsetY;

	private boolean doFilterDataByType = false;
	private int filterAssignmentType;

	private boolean doFilterDataByCost = false;
	private double filterMinCost = -100.0;
	private double filterMaxCost = 100.0;

	private boolean doFilterDataByIdentity = false;
	private boolean doAddToFilter = false; // if 'true' all assignments at the mouse location will be added to the filter next time repaint is called...
	private final Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > filteredAssignments;

	private HashMap< Hypothesis< Component< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> >> data;

	private boolean isMouseOver = false;
	private int mousePosX;
	private int mousePosY;
	private int currentCostLine;

	private boolean isDragging = false;
	private int dragX;
	private int dragY;
	private double dragStepWeight = 0;

	private boolean doAddAsGroundTruth;
	private boolean doAddAsGroundUntruth;

	private MotherMachineGui gui;

	private boolean doFilterGroundTruth = false;


	// -------------------------------------------------------------------------------------
	// construction
	// -------------------------------------------------------------------------------------
	public AssignmentView( final int height, final MotherMachineGui callbackGui ) {
		this( height, -GrowthLineTrackingILP.CUTOFF_COST, GrowthLineTrackingILP.CUTOFF_COST );
		this.doFilterDataByCost = false;
		this.gui = callbackGui;
	}

	/**
	 * @param height
	 * @param filterMinCost
	 * @param filterMaxCost
	 */
	public AssignmentView( final int height, final double filterMinCost, final double filterMaxCost ) {
		this.offsetY = MotherMachine.GL_OFFSET_TOP;
		this.width = 90;
		this.height = height;
		this.setPreferredSize( new Dimension( width, height - HEIGHT_OFFSET ) );

		this.addMouseListener( this );
		this.addMouseMotionListener( this );

		this.doFilterDataByCost = true;
		this.setCostFilterMin( filterMinCost );
		this.setCostFilterMax( filterMaxCost );

		this.filteredAssignments = new HashSet< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> >();
	}

	// -------------------------------------------------------------------------------------
	// getters and setters
	// -------------------------------------------------------------------------------------
	/**
	 * @return the filterMinCost
	 */
	public double getCostFilterMin() {
		return filterMinCost;
	}

	/**
	 * @param filterMinCost
	 *            the filterMinCost to set
	 */
	public void setCostFilterMin( final double filterMinCost ) {
		this.filterMinCost = filterMinCost;
	}

	/**
	 * @return the filterMaxCost
	 */
	public double getCostFilterMax() {
		return filterMaxCost;
	}

	/**
	 * @param filterMaxCost
	 *            the filterMaxCost to set
	 */
	public void setCostFilterMax( final double filterMaxCost ) {
		this.filterMaxCost = filterMaxCost;
	}

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * Turns of filtering and shows all the given data.
	 *
	 * @param data
	 *            a <code>HashMap</code> containing pairs of segmentation
	 *            hypothesis at some time-point t and assignments towards t+1.
	 */
	public void display( final HashMap< Hypothesis< Component< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> >> data, final boolean doFilterActive ) {
		doFilterDataByType = false;
		doFilterDataByCost = false;
		setData( data, doFilterActive );

		this.repaint();
	}

	/**
	 * Turns of filtering by type, turns on filtering by cost, and shows all the
	 * given data.
	 *
	 * @param data
	 *            a <code>HashMap</code> containing pairs of segmentation
	 *            hypothesis at some time-point t and assignments towards t+1.
	 */
	public void display( final HashMap< Hypothesis< Component< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> >> data, final boolean doFilterActive, final double minCostToShow, final double maxCostToShow ) {
		doFilterDataByType = false;
		setData( data, doFilterActive );

		doFilterDataByCost = true;
		this.setCostFilterMin( minCostToShow );
		this.setCostFilterMax( maxCostToShow );

		this.repaint();
	}

	/**
	 * Turns on filtering by type and shows only the filtered data.
	 *
	 * @param data
	 *            a <code>HashMap</code> containing pairs of segmentation
	 *            hypothesis at some time-point t and assignments towards t+1.
	 * @param typeToFilter
	 *            must be one of the values
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_MAPPING</code>,
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_DIVISION</code>, or
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_EXIT</code>.
	 */
	public void display( final HashMap< Hypothesis< Component< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> >> data, final boolean doFilterActive, final int typeToFilter ) {
		assert ( typeToFilter == GrowthLineTrackingILP.ASSIGNMENT_EXIT ||
				 typeToFilter == GrowthLineTrackingILP.ASSIGNMENT_MAPPING ||
				 typeToFilter == GrowthLineTrackingILP.ASSIGNMENT_DIVISION );
		this.display( data, doFilterActive, typeToFilter, this.getCostFilterMin(), this.getCostFilterMax() );
	}

	/**
	 * Turns on filtering by type and by cost and shows only the filtered data.
	 *
	 * @param data
	 *            a <code>HashMap</code> containing pairs of segmentation
	 *            hypothesis at some time-point t and assignments towards t+1.
	 * @param typeToFilter
	 *            must be one of the values
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_MAPPING</code>,
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_DIVISION</code>, or
	 *            <code>GrowthLineTrackingILP.ASSIGNMENT_EXIT</code>.
	 */
	public void display( final HashMap< Hypothesis< Component< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> >> data, final boolean doFilterActive, final int typeToFilter, final double minCostToShow, final double maxCostToShow ) {
		assert ( typeToFilter == GrowthLineTrackingILP.ASSIGNMENT_EXIT ||
				 typeToFilter == GrowthLineTrackingILP.ASSIGNMENT_MAPPING ||
				 typeToFilter == GrowthLineTrackingILP.ASSIGNMENT_DIVISION );
		doFilterDataByType = true;
		this.filterAssignmentType = typeToFilter;

		doFilterDataByCost = true;
		this.setCostFilterMin( minCostToShow );
		this.setCostFilterMax( maxCostToShow );
		setData( data, doFilterActive );

		this.repaint();
	}

	/**
	 * In this overwritten method we added filtering and calling
	 * <code>drawAssignment(...)</code>.
	 *
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	@Override
	public void paint( final Graphics g ) {
		if ( data == null ) return;

		this.currentCostLine = 0;
		for ( final Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > setOfAssignments : data.values() ) {
			for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> assignment : setOfAssignments ) {
				if ( doFilterDataByType && assignment.getType() != filterAssignmentType ) {
					continue;
				}
				try {
					if ( doFilterDataByCost && ( assignment.getCost() < this.getCostFilterMin() || assignment.getCost() > this.getCostFilterMax() ) ) {
						continue;
					}
				}
				catch ( final GRBException e ) {
					e.printStackTrace();
				}
				drawAssignment( g, assignment );
			}
		}

		if ( this.isDragging ) {
			g.setColor( Color.GREEN.darker() );
			g.drawString( String.format( "min: %.4f", this.getCostFilterMin() ), 0, 10 );
			g.setColor( Color.RED.darker() );
			g.drawString( String.format( "max: %.4f", this.getCostFilterMax() ), 0, 30 );
			g.setColor( Color.GRAY );
			g.drawString( String.format( "dlta %.4f", this.dragStepWeight ), 0, 50 );
		}

		// in case we where adding assignments - stop now!
		this.doAddToFilter = false;
	}

	/**
	 * Checks the type of assignment we have and call the corresponding drawing
	 * method.
	 *
	 * @param g
	 * @param assignment
	 */
	private void drawAssignment( final Graphics g, final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> assignment ) {

		// Just return in case the given component is in the
		// set of filtered assignments.
		if ( this.doFilterDataByIdentity && this.filteredAssignments.contains( assignment ) ) { return; }
		if ( this.doFilterGroundTruth && !( assignment.isGroundTruth() || assignment.isGroundUntruth() ) ) { return; }

		final int type = assignment.getType();

		final Graphics2D g2 = ( Graphics2D ) g;
		final Dimension size = getSize();

		if ( type == GrowthLineTrackingILP.ASSIGNMENT_EXIT ) {
			drawExitAssignment( g, g2, ( ExitAssignment ) assignment, size );
		} else if ( type == GrowthLineTrackingILP.ASSIGNMENT_MAPPING ) {
			drawMappingAssignment( g, g2, ( MappingAssignment ) assignment, size );
		} else if ( type == GrowthLineTrackingILP.ASSIGNMENT_DIVISION ) {
			drawDivisionAssignment( g, g2, ( DivisionAssignment ) assignment, size );
		}
	}

	/**
	 * This methods draws the given mapping-assignment into the component.
	 *
	 * @param g
	 * @param g2
	 * @param ma
	 *            a mapping-assignment that should be visualized.
	 * @param size
	 */
	private void drawMappingAssignment( final Graphics g, final Graphics2D g2, final MappingAssignment ma, final Dimension size ) {
		final Hypothesis< Component< DoubleType, ? >> leftHyp = ma.getSourceHypothesis();
		final Hypothesis< Component< DoubleType, ? >> rightHyp = ma.getDestinationHypothesis();

		final Pair< Integer, Integer > limitsLeft = ComponentTreeUtils.getTreeNodeInterval( leftHyp.getWrappedHypothesis() );
		final Pair< Integer, Integer > limitsRight = ComponentTreeUtils.getTreeNodeInterval( rightHyp.getWrappedHypothesis() );

		final int x1 = 0;
		final int y1 = offsetY + limitsLeft.getA().intValue();
		final int x2 = 0;
		final int y2 = offsetY + limitsLeft.getB().intValue();
		final int x3 = this.width;
		final int y3 = offsetY + limitsRight.getB().intValue();
		final int x4 = this.width;
		final int y4 = offsetY + limitsRight.getA().intValue();

		final GeneralPath polygon = new GeneralPath();
		polygon.moveTo( x1, y1 );
		polygon.lineTo( x2, y2 );
		polygon.lineTo( x3, y3 );
		polygon.lineTo( x4, y4 );
		polygon.closePath();

		// Interaction with mouse:
		if ( !this.isDragging && this.isMouseOver && polygon.contains( this.mousePosX, this.mousePosY ) ) {
			if ( doAddToFilter ) {
				// this case happens after shift-click
				this.filteredAssignments.add( ma );
			} else if ( this.doAddAsGroundTruth ) {
				this.doAddAsGroundTruth = false;
				ma.setGroundTruth( !ma.isGroundTruth() );
				SwingUtilities.invokeLater( new Runnable() {
					@Override
					public void run() {
						gui.dataToDisplayChanged();
					}
				} );
			} else if ( this.doAddAsGroundUntruth ) {
				this.doAddAsGroundUntruth = false;
				ma.setGroundUntruth( !ma.isGroundUntruth() );
				SwingUtilities.invokeLater( new Runnable() {

					@Override
					public void run() {
						gui.dataToDisplayChanged();
					}
				} );
			} else {
				// otherwise we show the costs by hovering over
				try {
					final double cost = ma.getCost();
					if ( ma.isGroundTruth() ) {
						g2.setPaint( Color.GREEN.darker() );
					} else if ( ma.isGroundUntruth() ) {
						g2.setPaint( Color.RED.darker() );
					} else {
						g2.setPaint( new Color( 25 / 256f, 65 / 256f, 165 / 256f, 1.0f ).darker().darker() );
					}
					g2.drawString( String.format( "c=%.4f", cost ), DISPLAY_COSTS_ABSOLUTE_X, this.mousePosY - OFFSET_DISPLAY_COSTS - this.currentCostLine * LINEHEIGHT_DISPLAY_COSTS );
					this.currentCostLine++;
				}
				catch ( final GRBException e ) {
					e.printStackTrace();
				}
			}
		}

		// draw it!
		g2.setStroke( new BasicStroke( 1 ) );
		if ( ma.isGroundTruth() ) {
			g2.setPaint( new Color( 160 / 256f, 200 / 256f, 180 / 256f, 0.6f ) );
		} else if ( ma.isGroundUntruth() ) {
			g2.setPaint( new Color( 256 / 256f, 50 / 256f, 50 / 256f, 0.6f ) );
		} else {
			g2.setPaint( new Color( 25 / 256f, 65 / 256f, 165 / 256f, 0.2f ) );
		}
		g2.fill( polygon );
		if ( ma.isGroundTruth() ) {
			g2.setPaint( Color.GREEN.darker() );
			g2.setStroke( new BasicStroke( 3 ) );
		} else if ( ma.isGroundUntruth() ) {
			g2.setPaint( Color.RED.darker() );
			g2.setStroke( new BasicStroke( 3 ) );
		} else {
			g2.setPaint( new Color( 25 / 256f, 65 / 256f, 165 / 256f, 1.0f ) );
		}
		g2.draw( polygon );
	}

	/**
	 * This methods draws the given division-assignment into the component.
	 *
	 * @param g
	 * @param g2
	 * @param da
	 *            a division-assignment that should be visualized.
	 * @param size
	 */
	private void drawDivisionAssignment( final Graphics g, final Graphics2D g2, final DivisionAssignment da, final Dimension size ) {
		final Hypothesis< Component< DoubleType, ? >> leftHyp = da.getSourceHypothesis();
		final Hypothesis< Component< DoubleType, ? >> rightHypUpper = da.getUpperDesinationHypothesis();
		final Hypothesis< Component< DoubleType, ? >> rightHypLower = da.getLowerDesinationHypothesis();

		final Pair< Integer, Integer > limitsLeft = ComponentTreeUtils.getTreeNodeInterval( leftHyp.getWrappedHypothesis() );
		final Pair< Integer, Integer > limitsRightUpper = ComponentTreeUtils.getTreeNodeInterval( rightHypUpper.getWrappedHypothesis() );
		final Pair< Integer, Integer > limitsRightLower = ComponentTreeUtils.getTreeNodeInterval( rightHypLower.getWrappedHypothesis() );

		final int x1 = 0;
		final int y1 = offsetY + limitsLeft.getA().intValue();
		final int x2 = 0;
		final int y2 = offsetY + limitsLeft.getB().intValue();
		final int x3 = this.width;
		final int y3 = offsetY + limitsRightLower.getB().intValue();
		final int x4 = this.width;
		final int y4 = offsetY + limitsRightLower.getA().intValue();
		final int x5 = this.width / 3;
		final int y5 = offsetY + ( 2 * ( limitsLeft.getA().intValue() + limitsLeft.getB().intValue() ) / 2 + 1 * ( limitsRightUpper.getB().intValue() + limitsRightLower.getA().intValue() ) / 2 ) / 3;
		final int x6 = this.width;
		final int y6 = offsetY + limitsRightUpper.getB().intValue();
		final int x7 = this.width;
		final int y7 = offsetY + limitsRightUpper.getA().intValue();

		final GeneralPath polygon = new GeneralPath();
		polygon.moveTo( x1, y1 );
		polygon.lineTo( x2, y2 );
		polygon.lineTo( x3, y3 );
		polygon.lineTo( x4, y4 );
		polygon.lineTo( x5, y5 );
		polygon.lineTo( x6, y6 );
		polygon.lineTo( x7, y7 );
		polygon.closePath();

		// Interaction with mouse:
		if ( !this.isDragging && this.isMouseOver && polygon.contains( this.mousePosX, this.mousePosY ) ) {
			if ( doAddToFilter ) {
				// this case happens after shift-click
				this.filteredAssignments.add( da );
			} else if ( this.doAddAsGroundTruth ) {
				this.doAddAsGroundTruth = false;
				da.setGroundTruth( !da.isGroundTruth() );
				SwingUtilities.invokeLater( new Runnable() {

					@Override
					public void run() {
						gui.dataToDisplayChanged();
					}
				} );
			} else if ( this.doAddAsGroundUntruth ) {
				this.doAddAsGroundUntruth = false;
				da.setGroundUntruth( !da.isGroundUntruth() );
				SwingUtilities.invokeLater( new Runnable() {

					@Override
					public void run() {
						gui.dataToDisplayChanged();
					}
				} );
			} else {
				// otherwise we show the costs by hovering over
				try {
					final double cost = da.getCost();
					if ( da.isGroundTruth() ) {
						g2.setPaint( Color.GREEN.darker() );
					} else if ( da.isGroundUntruth() ) {
						g2.setPaint( Color.RED.darker() );
					} else {
						g2.setPaint( new Color( 250 / 256f, 150 / 256f, 40 / 256f, 1.0f ).darker().darker() );
					}
					g2.drawString( String.format( "c=%.4f", cost ), DISPLAY_COSTS_ABSOLUTE_X, this.mousePosY - OFFSET_DISPLAY_COSTS - this.currentCostLine * LINEHEIGHT_DISPLAY_COSTS );
					this.currentCostLine++;
				}
				catch ( final GRBException e ) {
					e.printStackTrace();
				}
			}
		}

		// draw it!
		g2.setStroke( new BasicStroke( 1 ) );
		if ( da.isGroundTruth() ) {
			g2.setPaint( new Color( 160 / 256f, 200 / 256f, 180 / 256f, 0.6f ) );
		} else if ( da.isGroundUntruth() ) {
			g2.setPaint( new Color( 256 / 256f, 50 / 256f, 50 / 256f, 0.6f ) );
		} else {
			g2.setPaint( new Color( 250 / 256f, 150 / 256f, 40 / 256f, 0.2f ) );
		}
		g2.fill( polygon );
		if ( da.isGroundTruth() ) {
			g2.setPaint( Color.GREEN.darker() );
			g2.setStroke( new BasicStroke( 3 ) );
		} else if ( da.isGroundUntruth() ) {
			g2.setPaint( Color.RED.darker() );
			g2.setStroke( new BasicStroke( 3 ) );
		} else {
			g2.setPaint( new Color( 250 / 256f, 150 / 256f, 40 / 256f, 1.0f ) );
		}
		g2.draw( polygon );
	}

	/**
	 * This methods draws the given exit-assignment into the component.
	 *
	 * @param g
	 * @param g2
	 * @param ea
	 *            a exit-assignment that should be visualized.
	 * @param size
	 */
	private void drawExitAssignment( final Graphics g, final Graphics2D g2, final ExitAssignment ea, final Dimension size ) {
		final Hypothesis< Component< DoubleType, ? >> hyp = ea.getAssociatedHypothesis();
		final Pair< Integer, Integer > limits = ComponentTreeUtils.getTreeNodeInterval( hyp.getWrappedHypothesis() );

		final int x1 = 0;
		final int x2 = this.getWidth() / 5;
		final int y1 = offsetY + limits.getA().intValue();
		final int y2 = y1 + limits.getB().intValue() - limits.getA().intValue();

		if ( !this.isDragging && this.isMouseOver && this.mousePosX > x1 && this.mousePosX < x2 && this.mousePosY > y1 && this.mousePosY < y2 ) {
			if ( doAddToFilter ) {
				// this case happens after shift-click
				this.filteredAssignments.add( ea );
			} else if ( this.doAddAsGroundTruth ) {
				this.doAddAsGroundTruth = false;
				ea.setGroundTruth( !ea.isGroundTruth() );
				SwingUtilities.invokeLater( new Runnable() {

					@Override
					public void run() {
						gui.dataToDisplayChanged();
					}
				} );
			} else if ( this.doAddAsGroundUntruth ) {
				this.doAddAsGroundUntruth = false;
				ea.setGroundUntruth( !ea.isGroundUntruth() );
				SwingUtilities.invokeLater( new Runnable() {

					@Override
					public void run() {
						gui.dataToDisplayChanged();
					}
				} );
			} else {
				// otherwise we show the costs by hovering over
				try {
					final double cost = ea.getCost();
					g2.drawString( String.format( "c=%.4f", cost ), 10, this.mousePosY - 10 - this.currentCostLine * 20 );
					this.currentCostLine++;
				}
				catch ( final GRBException e ) {
					e.printStackTrace();
				}
			}
		}

		// draw it!
		g2.setStroke( new BasicStroke( 1 ) );
		if ( ea.isGroundTruth() ) {
			g2.setPaint( new Color( 160 / 256f, 200 / 256f, 180 / 256f, 0.6f ) );
		} else if ( ea.isGroundUntruth() ) {
			g2.setPaint( new Color( 256 / 256f, 50 / 256f, 50 / 256f, 0.6f ) );
		} else {
			g2.setPaint( new Color( 1f, 0f, 0f, 0.2f ) );
		}
		g2.fillRect( x1, y1, x2 - x1, y2 - y1 );
		if ( ea.isGroundTruth() ) {
			g2.setPaint( Color.GREEN.darker() );
			g2.setStroke( new BasicStroke( 3 ) );
		} else if ( ea.isGroundUntruth() ) {
			g2.setPaint( Color.RED.darker() );
			g2.setStroke( new BasicStroke( 3 ) );
		} else {
			g2.setPaint( Color.RED );
		}
		g2.drawRect( x1, y1, x2 - x1, y2 - y1 );
	}

	/**
	 * Sets new data without modifying the filter setting.
	 *
	 * @param data
	 * @param doFilterActive
	 */
	public void setData( final HashMap< Hypothesis< Component< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> >> data, final boolean doFilterActive ) {
		if ( data != null && doFilterActive ) {
			this.data = new HashMap< Hypothesis< Component< DoubleType, ? >>, Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> >>();
			for ( final Hypothesis< Component< DoubleType, ? >> hypo : data.keySet() ) {
				final Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > activeSet = new HashSet< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> >();
				for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> ass : data.get( hypo ) ) {
					try {
						if ( ass.isChoosen() || ass.isGroundTruth() ) {
							activeSet.add( ass );
						}
					}
					catch ( final GRBException e ) {
						e.printStackTrace();
					}
					this.data.put( hypo, activeSet );
				}
			}
		} else {
			this.data = data;
		}
		this.repaint();
	}

	/**
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseClicked( final MouseEvent e ) {
		if ( e.getClickCount() == 2 ) {
			new DialogAssignmentViewSetup( this, e.getXOnScreen(), e.getYOnScreen() ).setVisible( true );
		}
	}

	/**
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	@Override
	public void mousePressed( final MouseEvent e ) {
		// plain click to initiate dragging
		if ( !e.isShiftDown() && !e.isControlDown() && e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3 ) {
			this.isDragging = true;
			this.dragX = e.getX();
			this.dragY = e.getY();
		}

		// ctrl-click to filter some assignments
		if ( !e.isAltDown() && e.isControlDown() && e.getButton() == MouseEvent.BUTTON1 ) {
			this.doFilterDataByIdentity = true;
			this.doAddToFilter = true; // when repainting component next time...
		}

		// shift-click to filter some assignments
		if ( !e.isAltDown() && e.isShiftDown() && e.getButton() == MouseEvent.BUTTON1 ) {
			this.doFilterDataByIdentity = false;
			this.filteredAssignments.clear();
		}

		// alt-click to filter some assignments
		if ( e.isAltDown() && e.getButton() == MouseEvent.BUTTON1 ) {
			if ( e.isControlDown() ) {
				this.doAddAsGroundUntruth = true;
			} else {
				this.doAddAsGroundTruth = true;
			}
		}

		repaint();
	}

	/**
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseReleased( final MouseEvent e ) {
		this.isDragging = false;
		repaint();
	}

	/**
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseEntered( final MouseEvent e ) {
		this.isMouseOver = true;
	}

	/**
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseExited( final MouseEvent e ) {
		this.isMouseOver = false;
		this.repaint();
	}

	/**
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseDragged( final MouseEvent e ) {
		this.doFilterDataByCost = true;

		final double minstep = 0.1;
		final double xsensitivity = 15.0;
		final int dX = e.getX() - this.dragX;
		final int dY = this.dragY - e.getY();

		final double fac = Math.pow( 2, Math.abs( ( xsensitivity + dX ) / xsensitivity ) );
		if ( dX > 0 ) {
			this.dragStepWeight = minstep * fac;
		} else {
			this.dragStepWeight = minstep / fac;
		}

		if ( e.getButton() == MouseEvent.BUTTON1 ) {
			this.setCostFilterMax( this.getCostFilterMax() + dY * this.dragStepWeight );
		}
		if ( e.getButton() == MouseEvent.BUTTON3 ) {
			this.setCostFilterMin( this.getCostFilterMin() + dY * this.dragStepWeight );
		}

		this.dragY = e.getY();
		repaint();
	}

	/**
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	@Override
	public void mouseMoved( final MouseEvent e ) {
		this.mousePosX = e.getX();
		this.mousePosY = e.getY();
		this.repaint();
	}

	/**
	 * If set, this filter shows only assignments that are flagged as being
	 * ground-truth or ground-untruth.
	 *
	 * @param doIt
	 *            indicate whether of not to set this filter active.
	 */
	public void setFilterGroundTruth( final boolean doIt ) {
		this.doFilterGroundTruth = doIt;
	}

}
