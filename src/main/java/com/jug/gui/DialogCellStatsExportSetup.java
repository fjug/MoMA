/**
 *
 */
package com.jug.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * @author jug
 */
public class DialogCellStatsExportSetup extends JDialog implements ActionListener {

	private static final long serialVersionUID = 8900435259048694344L;

	private JButton bOk;
	private JButton bCancel;

	private boolean wasCanceled;

	public boolean doExportTracks;
	public boolean includeHistograms;
	public boolean includeQuantiles;
	public boolean includeColIntensitySums;
	public boolean includePixelIntensities;

	private JCheckBox cbHist;

	private JCheckBox cbQuant;

	private JCheckBox cbColInt;

	private JCheckBox cbPixelInt;

	private JCheckBox cbDoExportTracks;

	public DialogCellStatsExportSetup( final JComponent parent, final boolean doExportTracks, final boolean includeHistograms, final boolean includeQuantiles, final boolean includeColIntensitySums, final boolean includePixelIntensities ) {
		super( SwingUtilities.windowForComponent( parent ), "Data-export setup..." );
		this.dialogInit();
		this.setModal( true );

		this.wasCanceled = true;

		this.doExportTracks = doExportTracks;
		this.includeHistograms = includeHistograms;
		this.includeQuantiles = includeQuantiles;
		this.includeColIntensitySums = includeColIntensitySums;
		this.includePixelIntensities = includePixelIntensities;

		buildGui();
		this.pack();

		setKeySetup();
	}

	public void ask() {
		final int x = super.getParent().getX() + super.getParent().getWidth() / 2 - this.getWidth() / 2;
		final int y = super.getParent().getY() + super.getParent().getHeight() / 2 - this.getHeight() / 2;
		this.setBounds( x, y, this.getWidth(), this.getHeight() );

		this.setVisible( true );
	}

	private void buildGui() {
		this.rootPane.setLayout( new BorderLayout() );

		cbHist = new JCheckBox( "histogram data", includeHistograms );
		cbQuant = new JCheckBox( "intensity quantiles", includeQuantiles );
		cbColInt = new JCheckBox( "column intensities", includeColIntensitySums );
		cbPixelInt = new JCheckBox( "pixel intensities", includePixelIntensities );

		final JPanel toplevelHelper = new JPanel( new GridLayout( 2, 1, 10, 10 ) );
		toplevelHelper.setBorder( BorderFactory.createEmptyBorder( 10, 5, 5, 5 ) );

		JPanel verticalHelper = new JPanel( new GridLayout( 2, 1 ) );
		verticalHelper.add( cbHist );
		verticalHelper.add( cbQuant );
		verticalHelper.add( cbColInt );
		verticalHelper.add( cbPixelInt );
		verticalHelper.setBorder( BorderFactory.createTitledBorder( "Pick data to be exported..." ) );
		toplevelHelper.add( verticalHelper );

		cbDoExportTracks = new JCheckBox( "yes, do it!", doExportTracks );

		verticalHelper = new JPanel( new GridLayout( 1, 1 ) );
		verticalHelper.add( cbDoExportTracks );
		verticalHelper.setBorder( BorderFactory.createTitledBorder( "Also export tracks-file?" ) );
		toplevelHelper.add( verticalHelper );

		bOk = new JButton( "OK" );
		bOk.addActionListener( this );
		this.rootPane.setDefaultButton( bOk );
		bCancel = new JButton( "Cancel" );
		bCancel.addActionListener( this );
		final JPanel buttonHelper = new JPanel( new FlowLayout( FlowLayout.RIGHT, 15, 0 ) );
		buttonHelper.add( bCancel );
		buttonHelper.add( bOk );

		this.rootPane.add( toplevelHelper, BorderLayout.CENTER );
		this.rootPane.add( buttonHelper, BorderLayout.SOUTH );
	}

	private void setKeySetup() {
		this.rootPane.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( "ESCAPE" ), "closeAction" );

		this.rootPane.getActionMap().put( "closeAction", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e ) {
				setVisible( false );
				dispose();
			}
		} );

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( bOk ) ) {
			this.wasCanceled = false;

			this.doExportTracks = cbDoExportTracks.isSelected();
			this.includeHistograms = cbHist.isSelected();
			this.includeQuantiles = cbQuant.isSelected();
			this.includeColIntensitySums = cbColInt.isSelected();
			this.includePixelIntensities = cbPixelInt.isSelected();

			this.setVisible( false );
			this.dispose();
		}
		if ( e.getSource().equals( bCancel ) ) {
			this.setVisible( false );
			this.dispose();
		}
	}

	/**
	 * @return the wasCanceled
	 */
	public boolean wasCanceled() {
		return wasCanceled;
	}

}
