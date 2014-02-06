/**
 *
 */
package com.jug.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;


/**
 * @author jug
 */
public class DialogAssignmentViewSetup extends JDialog implements ActionListener {

	private static final long serialVersionUID = -3564705333556776780L;

	private final AssignmentView model;

	private JTextField tfMin;
	private JTextField tfMax;
	private JButton bOk;
	private JButton bCancel;

	public DialogAssignmentViewSetup( final AssignmentView av, final int x, final int y ) {
		super( SwingUtilities.windowForComponent( av ), "Cost-Filter Setup" );
		this.dialogInit();
		this.setModal( true );
		this.setBounds( x - 100, y - 50, 200, 125 );

		this.model = av;
		buildGui();
		setKeySetup();
	}

	private void buildGui() {
		this.rootPane.setLayout( new BorderLayout() );

		final JLabel labelMin = new JLabel( "min. cost: " );
		tfMin = new JTextField( String.format( "%.4f", model.getCostFilterMin() ), 7 );
		tfMin.setHorizontalAlignment( JTextField.RIGHT );
		final JLabel labelMax = new JLabel( "max. cost: " );
		tfMax = new JTextField( String.format( "%.4f", model.getCostFilterMax() ), 7 );
		tfMax.setHorizontalAlignment( JTextField.RIGHT );

		final JPanel verticalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 5 ) );

		JPanel horizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		horizontalHelper.add( labelMin );
		horizontalHelper.add( tfMin );
		verticalHelper.add( horizontalHelper );

		horizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		horizontalHelper.add( labelMax );
		horizontalHelper.add( tfMax );
		verticalHelper.add( horizontalHelper );

		bOk = new JButton("OK");
		bOk.addActionListener( this );
		this.rootPane.setDefaultButton( bOk );
		bCancel = new JButton( "Cancel" );
		bCancel.addActionListener( this );
		horizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		horizontalHelper.add( bCancel );
		horizontalHelper.add( bOk );

		this.rootPane.add( verticalHelper, BorderLayout.CENTER );
		this.rootPane.add( horizontalHelper, BorderLayout.SOUTH );
	}

	private void setKeySetup() {
		this.rootPane.getInputMap( rootPane.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( "ESCAPE" ), "closeAction" );

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
		double valueMin, valueMax;
		if ( e.getSource().equals( bOk ) ) {
			try {
				valueMin = Double.parseDouble( tfMin.getText() );
				valueMax = Double.parseDouble( tfMax.getText() );
				model.setCostFilterMin( valueMin );
				model.setCostFilterMax( valueMax );
				this.setVisible( false );
				this.dispose();
			}
			catch ( final NumberFormatException nfe ) {
				this.getContentPane().add( new JLabel( "Input format not valid!" ), BorderLayout.NORTH );
			}
		}
		if ( e.getSource().equals( bCancel ) ) {
			this.setVisible( false );
			this.dispose();
		}
	}
}
