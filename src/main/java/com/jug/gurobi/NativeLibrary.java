package com.jug.gurobi;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: October 2016
 */
public class NativeLibrary
{
	public static void copyLibraries( final URL pluginUrl ) throws URISyntaxException, MalformedURLException
	{
		//		IJ.log( System.getProperty( "user.dir" ) );

		final String osName = System.getProperty( "os.name" ).toLowerCase();
		final String archName = System.getProperty( "os.arch" ).toLowerCase();

		final String path = System.getProperty( "user.dir" );

		if ( osName.startsWith( "mac" ) )
		{
			if ( archName.startsWith( "x86_64" ) )
			{
				final URL url = new URL( pluginUrl + "macosx" );

				final File dest = new File( path + File.separator + "lib" + File.separator + "macosx" );
				dest.mkdirs();

				FileUtils.copyResourcesRecursively( url, new File( path + File.separator + "lib" ), dest );
			}
			else
			{
				throw new UnsupportedOperationException( "32bit MacOSX is not supported." );
			}
		}
		else if ( osName.startsWith( "windows" ) )
		{
			if ( archName.startsWith( "amd64" ) )
			{
				final URL url = new URL( pluginUrl + "win64" );

				final File dest = new File( path + File.separator + "lib" + File.separator + "win64" );
				dest.mkdirs();

				FileUtils.copyResourcesRecursively( url, new File( path + File.separator + "lib" ), dest );
			}
			else if ( archName.startsWith( "x86" ) )
			{
				final URL url = new URL( pluginUrl + "win32" );

				final File dest = new File( path + File.separator + "lib" + File.separator + "win32" );
				dest.mkdirs();

				FileUtils.copyResourcesRecursively( url, new File( path + File.separator + "lib" ), dest );
			}
		}
		else if ( osName.startsWith( "linux" ) )
		{
			if ( archName.startsWith( "amd64" ) )
			{
				final URL url = new URL( pluginUrl + "linux64" );

				final File dest = new File( path + File.separator + "lib" + File.separator + "linux64" );
				dest.mkdirs();

				FileUtils.copyResourcesRecursively( url, new File( path + File.separator + "lib" ), dest );
			}
			else if ( archName.startsWith( "i386" ) )
			{
				throw new UnsupportedOperationException( "32bit Linux is not supported." );
			}
		}
		else
		{
			throw new UnsupportedOperationException( "Your OS is not supported." );
		}
	}
}
