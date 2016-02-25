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
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.jug.MoMA;
import com.jug.sbmrm.MMTrainer;

/**
 * @author jug
 */
public class MMTrainerGui extends JFrame implements ActionListener {

	private final MoMAGui mmGui;
	private final MMTrainer trainer;

	private JTextArea loggingTextArea;
	private JButton bRun;

	public MMTrainerGui( final MoMAGui mmGui ) {
		this.mmGui = mmGui;

		buildGui();

		this.trainer = new MMTrainer( MoMA.instance, loggingTextArea );
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

		final JScrollPane scrollPane = new JScrollPane( loggingTextArea );
		scrollPane.setBorder( BorderFactory.createEmptyBorder( 0, 15, 0, 0 ) );
		// make textarea autoscroll
		scrollPane.getVerticalScrollBar().addAdjustmentListener( new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged( final AdjustmentEvent e ) {
				e.getAdjustable().setValue( e.getAdjustable().getMaximum() );
			}
		} );

		bRun = new JButton( "start training" );
		bRun.addActionListener( this );

		cp.add( scrollPane, BorderLayout.CENTER );
		cp.add( bRun, BorderLayout.SOUTH );
	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( bRun ) ) {
			final Thread t = new Thread( trainer );
			t.start();
		}
	}
}
