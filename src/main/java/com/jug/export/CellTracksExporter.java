/**
 * 
 */
package com.jug.export;

import java.util.Locale;
import java.util.Vector;

import net.imglib2.util.ValuePair;

import com.jug.GrowthLineFrame;
import com.jug.MoMA;
import com.jug.gui.MoMAGui;

/**
 * @author jug
 */
public class CellTracksExporter {

	private final MoMAGui gui;

	public CellTracksExporter( final MoMAGui gui ) {
		this.gui = gui;
	}

	public Vector< Vector< String >> getExportData() {

		// use US-style number formats! (e.g. '.' as decimal point)
		Locale.setDefault( new Locale( "en", "US" ) );

		final String loadedDataFolder = MoMA.props.getProperty( "import_path", "BUG -- could not get property 'import_path' while exporting tracks..." );
		final int numCurrGL = gui.sliderGL.getValue();
		final int numGLFs = gui.model.getCurrentGL().getFrames().size();
		final Vector< Vector< String >> dataToExport = new Vector< Vector< String >>();

		final Vector< String > firstLine = new Vector< String >();
		firstLine.add( loadedDataFolder );
		dataToExport.add( firstLine );
		final Vector< String > secondLine = new Vector< String >();
		secondLine.add( "" + numCurrGL );
		secondLine.add( "" + numGLFs );
		dataToExport.add( secondLine );

		int i = 0;
		for ( final GrowthLineFrame glf : gui.model.getCurrentGL().getFrames() ) {
			final Vector< String > newRow = new Vector< String >();
			newRow.add( "" + i );

			final int numCells = glf.getSolutionStats_numCells();
			final Vector< ValuePair< ValuePair< Integer, Integer >, ValuePair< Integer, Integer > >> data = glf.getSolutionStats_limitsAndRightAssType();

			newRow.add( "" + numCells );
			for ( final ValuePair< ValuePair< Integer, Integer >, ValuePair< Integer, Integer > > elem : data ) {
				final int min = elem.a.a.intValue();
				final int max = elem.a.b.intValue();
				final int type = elem.b.a.intValue();
				final int user_touched = elem.b.b.intValue();
				newRow.add( String.format( "%3d, %3d, %3d, %3d", min, max, type, user_touched ) );
			}

			dataToExport.add( newRow );
			i++;
		}

		return dataToExport;
	}
}
