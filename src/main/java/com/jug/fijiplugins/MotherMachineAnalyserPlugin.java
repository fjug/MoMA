package com.jug.fijiplugins;

import com.jug.MoMA;
import com.jug.util.Exec;
import com.jug.util.NativeLibrary;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.Prefs;
import ij.plugin.PlugIn;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * MotherMachine Analysis plugin
 *
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
 * Author: HongKee Moon (moon@mpi-cbg.de), Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: October 2016
 */
public class MotherMachineAnalyserPlugin implements PlugIn {

    private static String inputFolder;
    private static String outputFolder;

    public MotherMachineAnalyserPlugin()
    {
        String currentDir = Prefs.getDefaultDirectory();

        inputFolder = currentDir;
        outputFolder = currentDir;

        IJ.register(this.getClass());
    }

    @Override
    public void run(String s) {

		final Class<?> clazz = MotherMachineAnalyserPlugin.class;
		final String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		final String pluginsDir = url.substring(0, url.length() - clazz.getName().length() - 6);

		try
		{
			NativeLibrary.copyLibraries( new URL(pluginsDir) );
		}
		catch ( URISyntaxException e )
		{
			IJ.log( "Native library allocation for Fiji failed." );
			e.printStackTrace();
		}
		catch ( MalformedURLException e )
		{
			IJ.log( "The given class URL is wrong." );
			e.printStackTrace();
		}

		final String gurobiLicFilePath = System.getProperty( "user.home" ) + File.separator + "gurobi.lic";
		final File gurobiLicFile = new File( gurobiLicFilePath );

		if(!gurobiLicFile.exists())
		{
			GenericDialogPlus gurobiDialog = new GenericDialogPlus("Getting Gurobi License File");
			gurobiDialog.addMessage( "Paste the string starting with \"grbgetkey\"" );
			gurobiDialog.addStringField( "", "", 45 );
			gurobiDialog.showDialog();

			if(gurobiDialog.wasCanceled())
				return;

			final String grbkeygetString = gurobiDialog.getNextString();

			Exec.runGrbgetkey( grbkeygetString.split( " " ) );
		}

		if(gurobiLicFile.exists())
		{
			GenericDialogPlus gd = new GenericDialogPlus("MoMA configuration");
			gd.addDirectoryField("Input_folder", inputFolder);
			gd.addDirectoryField("Output_folder", outputFolder);
			gd.addNumericField("Number_of_Channels", 2, 0);
			gd.showDialog();
			if (gd.wasCanceled()) {
				return;
			}
			inputFolder = gd.getNextString();
			outputFolder = gd.getNextString();
			int numberOfChannels = (int)gd.getNextNumber();

			IJ.log("Starting MoMA..");

			String[] args = {
					"moma",
					"-i",
					inputFolder,
					"-o",
					outputFolder,
					"-c",
					"" + numberOfChannels
			};


			for (String param : args) {
				IJ.log("moma params " + param);
			}

			MoMA.running_as_Fiji_plugin = true;
			MoMA.main(args);
		}
    }

    public static void main(String... args)
    {
        new ImageJ();

        new MotherMachineAnalyserPlugin().run("");
    }

}
