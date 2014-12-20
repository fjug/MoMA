/**
 * 
 */
package com.jug.export;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.swing.JOptionPane;

import com.jug.gui.MotherMachineGui;
import com.jug.util.Util;

/**
 * @author jug
 */
public class HtmlOverviewExporter {

	private final MotherMachineGui gui;
	private final File htmlFile;
	private final String imgpath;
	private final int startFrame;
	private final int endFrame;

	public HtmlOverviewExporter( final MotherMachineGui gui, final File htmlFile, final String imgpath, final int startFrame, final int endFrame ) {
		this.gui = gui;
		this.htmlFile = htmlFile;
		this.imgpath = imgpath;
		this.startFrame = startFrame;
		this.endFrame = endFrame;
	}

	public void run() {

		String basename = htmlFile.getName();
		final int pos = basename.lastIndexOf( "." );
		if ( pos > 0 ) {
			basename = basename.substring( 0, pos );
		}

		// create folders to imgs if not exists
		final File fImgpath = new File( imgpath );
		if ( !fImgpath.exists() && !fImgpath.mkdirs() ) {
			JOptionPane.showMessageDialog( gui, "Saving of HTML canceled! Couldn't create dir: " + fImgpath, "Saving canceled...", JOptionPane.ERROR_MESSAGE );
			return;
		}

		Writer out = null;
		try {
			out = new OutputStreamWriter( new FileOutputStream( htmlFile ) );

			out.write( "<html>\n" );
			out.write( "<body>\n" );
			out.write( "	<table border='0' cellspacing='1' cellpadding='0'>\n" );
			out.write( "		<tr>\n" );

			String row1 = "";
			final String nextrow = "		</tr>\n			<tr>\n";
			String row2 = "";

			for ( int i = startFrame; i <= endFrame; i++ ) {
				gui.sliderTime.setValue( i );
				try {
					String fn = String.format( "/" + basename + "_gl_%02d_glf_%03d.png", gui.sliderGL.getValue(), i );
					gui.imgCanvasActiveCenter.exportScreenImage( imgpath + fn );
					row1 += "			<th><font size='+2'>t=" + i + "</font></th>\n";
					row2 += "			<td><img src='./imgs" + fn + "'></td>\n";

					if ( i < endFrame ) {
						fn = String.format( "/" + basename + "_gl_%02d_assmnts_%03d.png", gui.sliderGL.getValue(), i );
						Util.saveImage( Util.getImageOf( gui.rightAssignmentViewer.getActiveAssignments(), gui.imgCanvasActiveCenter.getWidth(), gui.imgCanvasActiveCenter.getHeight() ), imgpath + fn );
						row1 += "			<th></th>\n";
						row2 += "			<td><img src='./imgs" + fn + "'></td>\n"; // + "' width='10' height='" + this.imgCanvasActiveCenter.getHeight() 
					}
				} catch ( final IOException e ) {
					JOptionPane.showMessageDialog( gui, "Tracking imagery could not be saved entirely!", "Export Error", JOptionPane.ERROR_MESSAGE );
					e.printStackTrace();
					out.close();
					return;
				}
			}

			out.write( row1 );
			out.write( nextrow );
			out.write( row2 );

			out.write( "		</tr>\n" );
			out.write( "	</table>\n" );
			out.write( "</body>\n" );
			out.write( "</html>\n" );

			out.close();
		} catch ( final FileNotFoundException e1 ) {
			JOptionPane.showMessageDialog( gui, "File not found!", "Error!", JOptionPane.ERROR_MESSAGE );
			e1.printStackTrace();
		} catch ( final IOException e1 ) {
			JOptionPane.showMessageDialog( gui, "Selected file could not be written!", "Error!", JOptionPane.ERROR_MESSAGE );
			e1.printStackTrace();
		}

	}

}
