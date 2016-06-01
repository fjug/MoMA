/**
 *
 */
package com.jug.gui;

import java.util.HashMap;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jug.lp.AbstractAssignment;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.lp.Hypothesis;
import com.jug.util.OSValidator;

import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.FloatType;

/**
 * @author jug
 */
public class AssignmentViewer extends JTabbedPane implements ChangeListener {

	// -------------------------------------------------------------------------------------
	// statics
	// -------------------------------------------------------------------------------------
	private static final long serialVersionUID = 6588846114839723373L;

	// -------------------------------------------------------------------------------------
	// fields
	// -------------------------------------------------------------------------------------
	private AssignmentView activeAssignments;
	private AssignmentView inactiveMappingAssignments;
	private AssignmentView inactiveDivisionAssignments;
	private AssignmentView inactiveExitAssignments;
	private AssignmentView fixedAssignments;

	private HashMap< Hypothesis< Component< FloatType, ? >>, Set< AbstractAssignment< Hypothesis< Component< FloatType, ? >>> >> data;

	private final MoMAGui gui;

	private int curTabIdx = 0;
	private JPanel nextHackTab;

	// -------------------------------------------------------------------------------------
	// construction
	// -------------------------------------------------------------------------------------
	/**
	 * @param dimension
	 */
	public AssignmentViewer( final int height, final MoMAGui callbackGui ) {
		this.gui = callbackGui;
		this.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 0 ) );
		buildGui( height );
	}

	// -------------------------------------------------------------------------------------
	// getters and setters
	// -------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------
	// methods
	// -------------------------------------------------------------------------------------
	/**
	 * Builds the user interface.
	 */
	private void buildGui( final int height ) {
		activeAssignments = new AssignmentView( height, gui );
		inactiveMappingAssignments = new AssignmentView( height, gui );
		inactiveDivisionAssignments = new AssignmentView( height, gui );
		inactiveExitAssignments = new AssignmentView( height, gui );
		fixedAssignments = new AssignmentView( height, gui );

		// Hack to enable non-Mac MoMA to only use one row of tabs
		nextHackTab = new JPanel();
		final JComponent[] tabsToRoll =
				{ activeAssignments, inactiveMappingAssignments, inactiveDivisionAssignments, inactiveExitAssignments, fixedAssignments };
		final String[] namesToRoll =
				{ "OPT", "M", "D", "E", "GT" };
		final AssignmentViewer me = this;
		final ChangeListener changeListener = new ChangeListener() {

			@Override
			public void stateChanged( final ChangeEvent changeEvent ) {
				final JTabbedPane sourceTabbedPane = ( JTabbedPane ) changeEvent.getSource();
				if ( sourceTabbedPane.getSelectedComponent().equals( nextHackTab ) ) {
					final int oldIdx = curTabIdx;
					curTabIdx++;
					if ( curTabIdx >= tabsToRoll.length ) curTabIdx = 0;
					me.add( namesToRoll[ curTabIdx ], tabsToRoll[ curTabIdx ] );
					me.remove( tabsToRoll[ oldIdx ] );
					me.setSelectedIndex( 1 );
				}
			}
		};
		this.addChangeListener( changeListener );

		activeAssignments.display( data, true );
		inactiveMappingAssignments.display( data, false, GrowthLineTrackingILP.ASSIGNMENT_MAPPING );
		inactiveDivisionAssignments.display( data, false, GrowthLineTrackingILP.ASSIGNMENT_DIVISION );
		inactiveExitAssignments.display( data, false, GrowthLineTrackingILP.ASSIGNMENT_EXIT );
		fixedAssignments.display( data, false );
		fixedAssignments.setFilterGroundTruth( true );

		if ( !OSValidator.isMac() ) {
			this.add( ">", nextHackTab );
			this.add( namesToRoll[ curTabIdx ], tabsToRoll[ curTabIdx ] );
		} else {
			for ( int i = 0; i < tabsToRoll.length; i++ ) {
				this.add( namesToRoll[ i ], tabsToRoll[ i ] );
			}
		}
	}

	/**
	 * Receives and visualizes a new HashMap of assignments.
	 *
	 * @param hashMap
	 *            a <code>HashMap</code> containing pairs of segmentation
	 *            hypothesis at some time-point t and assignments towards t+1.
	 */
	public void display( final HashMap< Hypothesis< Component< FloatType, ? >>, Set< AbstractAssignment< Hypothesis< Component< FloatType, ? >>> >> hashMap ) {
		this.data = hashMap;
		activeAssignments.setData( data, true );
		inactiveMappingAssignments.setData( data, false );
		inactiveDivisionAssignments.setData( data, false );
		inactiveExitAssignments.setData( data, false );
		fixedAssignments.setData( data, false );
	}

	/**
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged( final ChangeEvent e ) {
		if ( this.getSelectedComponent().equals( activeAssignments ) ) {
			activeAssignments.setData( data, true );
		} else if ( this.getSelectedComponent().equals( inactiveMappingAssignments ) ) {
			inactiveMappingAssignments.setData( data, false );
		} else if ( this.getSelectedComponent().equals( inactiveDivisionAssignments ) ) {
			inactiveDivisionAssignments.setData( data, false );
		} else if ( this.getSelectedComponent().equals( inactiveExitAssignments ) ) {
			inactiveExitAssignments.setData( data, false );
		} else {
			fixedAssignments.setData( data, false );
		}
	}

	/**
	 * Returns the <code>AssignmentView</code> that holds all active
	 * assignments.
	 *
	 * @return
	 */
	public AssignmentView getActiveAssignments() {
		return this.activeAssignments;
	}

}
