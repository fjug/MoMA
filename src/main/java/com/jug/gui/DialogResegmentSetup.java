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
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * @author jug
 */
public class DialogResegmentSetup extends JDialog implements ActionListener {

	private static final long serialVersionUID = 6729479739513650107L;

	private JButton bOk;
	private JButton bCancel;

	private boolean wasCanceled;
	private final boolean allGLFs;
	private final boolean doPMFRF;

	public DialogResegmentSetup( final JComponent parent, final boolean allGLFs, final boolean doPMFRF ) {
		super( SwingUtilities.windowForComponent( parent ), "Segmentation-setup..." );
		this.dialogInit();
		this.setModal( true );

		this.wasCanceled = true;
		this.allGLFs = allGLFs;
		this.doPMFRF = doPMFRF;

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

		final ButtonGroup bgWhat = new ButtonGroup();
		final JRadioButton rbAll = new JRadioButton( "all", this.allGLFs );
		final JRadioButton rbCurrent = new JRadioButton( "only current", !this.allGLFs );
		bgWhat.add( rbAll );
		bgWhat.add( rbCurrent );

		final ButtonGroup bgHow = new ButtonGroup();
		final JRadioButton rbCT = new JRadioButton( "simple (fast)", !this.doPMFRF );
		final JRadioButton rbPMFRF = new JRadioButton( "elaborate", this.doPMFRF );
		bgHow.add( rbCT );
		bgHow.add( rbPMFRF );

		final JPanel horizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 15, 5 ) );

		JPanel verticalHelper = new JPanel( new GridLayout( 2, 1 ) );
		verticalHelper.add( rbAll );
		verticalHelper.add( rbCurrent );
		verticalHelper.setBorder( BorderFactory.createTitledBorder( "Which frames?" ) );
		horizontalHelper.add( verticalHelper );

		verticalHelper = new JPanel( new GridLayout( 2, 1 ) );
		verticalHelper.add( rbCT );
		verticalHelper.add( rbPMFRF );
		verticalHelper.setBorder( BorderFactory.createTitledBorder( "Which method?" ) );
		horizontalHelper.add( verticalHelper );

		bOk = new JButton( "OK" );
		bOk.addActionListener( this );
		this.rootPane.setDefaultButton( bOk );
		bCancel = new JButton( "Cancel" );
		bCancel.addActionListener( this );
		final JPanel buttonHelper = new JPanel( new FlowLayout( FlowLayout.RIGHT, 15, 0 ) );
		buttonHelper.add( bCancel );
		buttonHelper.add( bOk );

		this.rootPane.add( horizontalHelper, BorderLayout.CENTER );
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

	/**
	 * @return
	 */
	public boolean allFrames() {
		return this.allGLFs;
	}

	/**
	 * @return
	 */
	public boolean doPMFRF() {
		return this.doPMFRF;
	}
}
