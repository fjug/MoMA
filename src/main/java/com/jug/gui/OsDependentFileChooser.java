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
public class OsDependentFileChooser {

	public static File showLoadFolderChooser(
			final Component parent,
			final String path,
			final String title ) {

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
			if ( fd.getFile() == null ) { return null; }
			System.setProperty( "apple.awt.fileDialogForDirectories", "false" );
			return selectedFile;

		} else {
			return showSwingLoadFolderChooser( parent, path, title );
		}
	}

	private static File showSwingLoadFolderChooser(
			final Component parent,
			final String path,
			final String title ) {
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

	public static File showSaveFolderChooser( final Component parent, final String path, final String title ) {

		JFrame frame = null;
		try {
			frame = ( JFrame ) SwingUtilities.getWindowAncestor( parent );
		} catch ( final ClassCastException e ) {
			frame = null;
		}

		if ( OSValidator.isMac() && frame != null ) {

			System.setProperty( "apple.awt.fileDialogForDirectories", "true" );
			final FileDialog fd = new FileDialog( frame, title, FileDialog.SAVE );
			fd.setDirectory( path );
			fd.setVisible( true );
			final File selectedFile = new File( fd.getDirectory() + "/" + fd.getFile() );
			if ( fd.getFile() == null ) {
				return null;
			}
			System.setProperty( "apple.awt.fileDialogForDirectories", "false" );
			return selectedFile;

		} else {
			return showSwingSaveFolderChooser( parent, path, title );
		}
	}

	private static File showSwingSaveFolderChooser( final Component parent, final String path, final String title ) {
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

		if ( chooser.showSaveDialog( parent ) == JFileChooser.APPROVE_OPTION ) {
			return chooser.getSelectedFile();
		} else {
			return null;
		}
	}

	public static File showSaveFileChooser(
			final Component parent,
			final String path,
			final String title,
			final FileFilter fileFilter ) {

		JFrame frame = null;
		try {
			frame = ( JFrame ) SwingUtilities.getWindowAncestor( parent );
		} catch ( final ClassCastException e ) {
			frame = null;
		}

		if ( OSValidator.isMac() && frame != null ) {

			final FileDialog fd = new FileDialog( frame, title, FileDialog.SAVE );
			fd.setDirectory( path );
			fd.setVisible( true );
			final File selectedFile = new File( fd.getDirectory() + "/" + fd.getFile() );
			if ( fd.getFile() == null ) { return null; }
			return selectedFile;

		} else {
			return showSwingSaveFileChooser( parent, path, title, fileFilter );
		}
	}

	private static File showSwingSaveFileChooser(
			final Component parent,
			final String path,
			final String title,
			final FileFilter fileFilter ) {
		final JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory( new java.io.File( path ) );
		chooser.setDialogTitle( title );
		if ( fileFilter != null ) {
			chooser.setFileFilter( fileFilter );
			chooser.setAcceptAllFileFilterUsed( false );
		} else {
			chooser.setAcceptAllFileFilterUsed( true );
		}
		chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );

		if ( chooser.showSaveDialog( parent ) == JFileChooser.APPROVE_OPTION ) {
			return chooser.getSelectedFile();
		} else {
			return null;
		}
	}

	public static File showLoadFileChooser(
			final Component parent,
			final String path,
			final String title,
			final FileFilter fileFilter ) {

		JFrame frame = null;
		try {
			frame = ( JFrame ) SwingUtilities.getWindowAncestor( parent );
		} catch ( final ClassCastException e ) {
			frame = null;
		}

		if ( OSValidator.isMac() && frame != null ) {

			final FileDialog fd = new FileDialog( frame, title, FileDialog.LOAD );
			fd.setDirectory( path );
			fd.setVisible( true );
			final File selectedFile = new File( fd.getDirectory() + "/" + fd.getFile() );
			if ( fd.getFile() == null ) { return null; }
			return selectedFile;

		} else {
			return showSwingLoadFileChooser( parent, path, title, fileFilter );
		}
	}

	private static File showSwingLoadFileChooser(
			final Component parent,
			final String path,
			final String title,
			final FileFilter fileFilter ) {
		final JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory( new java.io.File( path ) );
		chooser.setDialogTitle( title );
		if ( fileFilter != null ) {
			chooser.setFileFilter( fileFilter );
			chooser.setAcceptAllFileFilterUsed( false );
		} else {
			chooser.setAcceptAllFileFilterUsed( true );
		}
		chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );

		if ( chooser.showOpenDialog( parent ) == JFileChooser.APPROVE_OPTION ) {
			return chooser.getSelectedFile();
		} else {
			return null;
		}
	}
}
