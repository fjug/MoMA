package com.jug.gurobi;

import ij.IJ;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * Spawn execution process of "grbgetkey"
 *
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: October 2016
 */
public class Exec
{
	static public void runGrbgetkey( String... commands )
	{
		String path = System.getProperty( "user.dir" ) + File.separator + "lib" + File.separator;

		if(IJ.isMacOSX())
		{
			path += "macosx" + File.separator + "grbgetkey";
		}
		else if(IJ.isWindows())
		{
			if(IJ.is64Bit())
			{
				path += "win64" + File.separator + "grbgetkey";
			}
			else
			{
				path += "win32" + File.separator + "grbgetkey";
			}
		}
		else if(IJ.isLinux())
		{
			if(IJ.is64Bit())
			{
				path += "linux64" + File.separator + "grbgetkey";
			}
		}

		final File grbgetkey = new File(path);
		grbgetkey.setExecutable( true );

		Process process = null;
		commands[0] = path;

		ProcessBuilder pb = new ProcessBuilder( commands );

		pb.redirectErrorStream( true );

		try
		{
			process = pb.start();
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		BufferedWriter bw = new BufferedWriter( new OutputStreamWriter( process.getOutputStream() ) );

		try
		{
			bw.write( System.getProperty( "line.separator" ) );
			bw.flush();
			if(IJ.isWindows()) {
				bw.write(System.getProperty("line.separator"));
				bw.flush();
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		InputStream is = process.getInputStream();
		InputStreamReader isr = new InputStreamReader( is );
		BufferedReader br = new BufferedReader( isr );
		String line;

		try
		{
			while ( ( line = br.readLine() ) != null )
			{
				IJ.log( line );
			}
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}
	}
}
