/**
 *
 */
package com.jug.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;

import com.jug.MotherMachine;
import com.jug.sbmrm.MMTrainer;

/**
 * @author jug
 */
public class MMTrainerGui extends JFrame implements ActionListener {

	private final MotherMachineGui mmGui;
	private final MMTrainer trainer;

	private JTextArea loggingTextArea;
	private JButton bRun;

	public MMTrainerGui( final MotherMachineGui mmGui ) {
		this.mmGui = mmGui;
		this.trainer = new MMTrainer( MotherMachine.instance );
		buildGui();
	}

	private void buildGui() {
		final Container cp = this.getContentPane();
		cp.setLayout( new BorderLayout( 5, 5 ) );

		final int framewidth = 800;
		final int frameheight = 600;

		final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		final int screenwidth = gd.getDisplayMode().getWidth();
		final int screenheight = gd.getDisplayMode().getHeight();
		this.setBounds(
				( screenwidth - framewidth ) / 2,
				( screenheight - frameheight ) / 2,
				framewidth,
				frameheight );

		loggingTextArea = new JTextArea();
		loggingTextArea.setLineWrap( true );
		loggingTextArea.setWrapStyleWord( true );

		bRun = new JButton( "start training" );
		bRun.addActionListener( this );

		cp.add( loggingTextArea, BorderLayout.CENTER );
		cp.add( bRun, BorderLayout.SOUTH );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( bRun ) ) {
			trainer.run( loggingTextArea );
		}
	}
}
