/**
 * 
 */
package com.jug.gui;

import java.awt.Component;
import java.awt.FileDialog;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import com.jug.util.OSValidator;

/**
 * @author jug
 */
public class OsDependentFolderChooser {

	public static File showFolderChooser( final Component parent, final String path, final String title ) {

		JFrame frame = null;
		try {
			frame = ( JFrame ) SwingUtilities.getWindowAncestor( parent );
		} catch ( final ClassCastException e ) {
			frame = null;
		}

		if ( OSValidator.isMac() && frame != null ) {

			System.setProperty( "apple.awt.fileDialogForDirectories", "true" );
			final FileDialog fd = new FileDialog( frame, title, FileDialog.LOAD );
			fd.setDirectory( path );
			fd.setVisible( true );
			final File selectedFile = new File( fd.getDirectory() + "/" + fd.getFile() );
			if ( fd.getFile() == null ) {
				System.exit( 0 );
				return null;
			}
			System.setProperty( "apple.awt.fileDialogForDirectories", "false" );
			return selectedFile;

		} else {
			return showSwingFolderChooser( parent, path, title );
		}
	}

	private static File showSwingFolderChooser( final Component parent, final String path, final String title ) {
		final JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory( new java.io.File( path ) );
		chooser.setDialogTitle( title );
		chooser.setFileFilter( new FileFilter() {

			@Override
			public final boolean accept( final File file ) {
				return file.isDirectory();
			}

			@Override
			public String getDescription() {
				return "We only take directories";
			}
		} );
		chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
		chooser.setAcceptAllFileFilterUsed( false );

		if ( chooser.showOpenDialog( parent ) == JFileChooser.APPROVE_OPTION ) {
			return chooser.getSelectedFile();
		} else {
			return null;
		}
	}
}
