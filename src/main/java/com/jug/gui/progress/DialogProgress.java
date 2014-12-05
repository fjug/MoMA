/**
 *
 */
package com.jug.gui.progress;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * @author jug
 */
public class DialogProgress extends JDialog implements ActionListener, ProgressListener {

	private static final long serialVersionUID = 2961170560625243194L;

	private JButton bHide;

	private final int maxProgress;
	private JProgressBar progressBar;

	private final String message;

	public DialogProgress( final JComponent parent, final String message, final int totalProgressNotificationsToCome ) {
		super( SwingUtilities.windowForComponent( parent ), "Segmentation-setup..." );
		this.dialogInit();
		this.setModal( false );

		this.message = message;
		this.maxProgress = totalProgressNotificationsToCome;

		buildGui();
		this.pack();

		setKeySetup();
	}

	@Override
	public void setVisible( final boolean show ) {
		final int width = 500;
		final int x = super.getParent().getX() + super.getParent().getWidth() / 2 - width / 2;
		final int y = super.getParent().getY() + super.getParent().getHeight() / 2 - this.getHeight() / 2;
		this.setBounds( x, y, width, this.getHeight() );

		super.setVisible( true );
	}

	private void buildGui() {
		this.rootPane.setLayout( new BorderLayout() );

		final JLabel lblMessage = new JLabel( this.message );
		lblMessage.setBorder( BorderFactory.createEmptyBorder( 5, 15, 0, 15 ) );
		progressBar = new JProgressBar( 0, this.maxProgress );
		progressBar.setBorder( BorderFactory.createEmptyBorder( 5, 15, 5, 15 ) );

		bHide = new JButton( "hide" );
		bHide.addActionListener( this );
		this.rootPane.setDefaultButton( bHide );
		final JPanel buttonHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 15, 0 ) );
		buttonHelper.add( bHide );

		this.rootPane.add( lblMessage, BorderLayout.NORTH );
		this.rootPane.add( progressBar, BorderLayout.CENTER );
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
		if ( e.getSource().equals( bHide ) ) {
			this.setVisible( false );
			this.dispose();
		}
	}

	/**
	 * @see com.jug.gui.progress.ProgressListener#hasProgressed()
	 */
	@Override
	public void hasProgressed() {
		final int newVal = progressBar.getValue() + 1;
		progressBar.setValue( newVal );
	}
}
