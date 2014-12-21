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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * @author jug
 */
public class DialogGurobiProgress extends JDialog implements ActionListener {

	private static final long serialVersionUID = -2399693327913856740L;

	private JButton bHide;

	private JLabel progressMessage = null;

	public DialogGurobiProgress( final JFrame jFrame ) {
		super( jFrame, "Optimization Progress..." );
		this.dialogInit();
		this.setModal( false );

		buildGui();
		this.pack();

		setKeySetup();
	}

	@Override
	public void setVisible( final boolean show ) {
		final int width = 350;
		final int x = super.getParent().getX() + super.getParent().getWidth() / 2 - width / 2;
		final int y = super.getParent().getY() + super.getParent().getHeight() / 2 - this.getHeight() / 2;
		this.setBounds( x, y, width, this.getHeight() );

		super.setVisible( true );
	}

	private void buildGui() {
		this.rootPane.setLayout( new BorderLayout() );

		progressMessage = new JLabel( "Starting optimization..." );
		progressMessage.setBorder( BorderFactory.createEmptyBorder( 5, 15, 5, 15 ) );

		bHide = new JButton( "hide" );
		bHide.addActionListener( this );
		this.rootPane.setDefaultButton( bHide );
		final JPanel buttonHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 15, 0 ) );
		buttonHelper.add( bHide );

		this.rootPane.add( progressMessage, BorderLayout.CENTER );
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
	 * @param string
	 */
	public void pushStatus( final String string ) {
		progressMessage.setText( string );
	}

	/**
	 * 
	 */
	public void notifyGurobiTermination() {
		bHide.setText( "done" );
	}
}
