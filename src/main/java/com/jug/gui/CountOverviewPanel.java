/**
 *
 */
package com.jug.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import net.imglib2.algorithm.componenttree.Component;
import net.imglib2.type.numeric.real.DoubleType;

import com.jug.GrowthLine;
import com.jug.GrowthLineFrame;
import com.jug.lp.AbstractAssignment;
import com.jug.lp.GrowthLineTrackingILP;
import com.jug.lp.Hypothesis;

/**
 * @author jug
 *
 */
public class CountOverviewPanel extends JPanel {

	private static final long serialVersionUID = -7527124790229560330L;

	private JTable table;
	private Vector< Vector< String > > data;
	private Vector< String > columnNames;
	private JLabel totalCellCount;

	public CountOverviewPanel() {
		buildGui();
	}

	private void buildGui() {
		columnNames = new Vector< String >();
		columnNames.add( String.format( " #C" ) );
		columnNames.add( String.format( " #D" ) );
		columnNames.add( String.format( " #E" ) );
		columnNames.add( String.format( " CCC" ) );

		totalCellCount = new JLabel();
		totalCellCount.setFont( totalCellCount.getFont().deriveFont( Font.BOLD ) );
		totalCellCount.setText( String.format( "TOTAL CELL COUNT:    %d", 0 ) );
		totalCellCount.setBorder( BorderFactory.createEmptyBorder( 10, 50, 10, 50 ) );

		this.setLayout( new BorderLayout() );
		final JPanel helper = new JPanel( new BorderLayout() );
		final DefaultTableModel model = new DefaultTableModel( data, columnNames );
		table = new JTable( model ) {

			private static final long serialVersionUID = -5757310501730411649L;
			DefaultTableCellRenderer renderRight = new DefaultTableCellRenderer();

			{ //initializer block
				renderRight.setHorizontalAlignment( SwingConstants.CENTER );
			}

			@Override
			public TableCellRenderer getCellRenderer( final int arg0, final int arg1 ) {
				return renderRight;

			}

			@Override
			public boolean isCellEditable( final int rowIndex, final int colIndex ) {
				return false; // disable edit for ALL cells
			}

			@Override
			public void changeSelection( final int rowIndex, final int columnIndex, final boolean toggle, final boolean extend ) {
				try {
					// content?
				}
				catch ( final Exception e ) {}
				// make the selection change
				super.changeSelection( rowIndex, columnIndex, toggle, extend );
			}
		};
		this.add( table.getTableHeader(), BorderLayout.PAGE_START );
		final JScrollPane scrollPane = new JScrollPane( table );
		helper.add( scrollPane, BorderLayout.CENTER );
		this.add( helper, BorderLayout.CENTER );

		this.add( totalCellCount, BorderLayout.SOUTH );
	}

	/**
	 * @param currentGL
	 */
	public void showData( final GrowthLine currentGL ) {
		data = new Vector< Vector< String > >();

		int sumOfCells = 0;
		if ( currentGL != null && currentGL.getIlp() != null ) {
			// collect data
			for ( final GrowthLineFrame glf : currentGL.getFrames() ) {
				final Vector< String > row = new Vector< String >();

				int cells = 0;
				int exits = 0;
				int divisions = 0;

				for ( final Set< AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> > set : currentGL.getIlp().getOptimalRightAssignments( glf.getTime() ).values() ) {
					for ( final AbstractAssignment< Hypothesis< Component< DoubleType, ? >>> ora : set ) {
						cells++;
						if ( ora.getType() == GrowthLineTrackingILP.ASSIGNMENT_DIVISION )
							divisions++;
						if ( ora.getType() == GrowthLineTrackingILP.ASSIGNMENT_EXIT )
							exits++;
					}
				}
				if ( sumOfCells == 0 ) {
					sumOfCells = cells;
				} else {
					sumOfCells += divisions;
				}

				// fill new rows and update rowSum
				row.add( "" + cells );
				if ( divisions > 0 ) {
					row.add( "" + divisions );
				} else {
					row.add( "" );
				}
				if ( exits > 0 ) {
					row.add( "" + exits );
				} else {
					row.add( "" );
				}
				row.add( "" + sumOfCells );

				data.add( row );
			}
		}

		// set data
		final DefaultTableModel model = new DefaultTableModel( data, columnNames );
		table.setModel( model );
		table.setCellSelectionEnabled( true );
		totalCellCount.setText( String.format( "TOTAL CELL COUNT:    %d", sumOfCells ) );
	}

}
