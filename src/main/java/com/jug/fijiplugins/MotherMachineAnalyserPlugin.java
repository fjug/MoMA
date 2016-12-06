package com.jug.fijiplugins;

import com.jug.MoMA;
import com.jug.gurobi.GurobiInstaller;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.Prefs;
import ij.plugin.PlugIn;

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

		if(!new GurobiInstaller().checkInstallation()) {
			IJ.log("Gurobi appears not properly installed. Please check your installation!");
			return;
		}

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

    public static void main(String... args)
    {
        new ImageJ();

        new MotherMachineAnalyserPlugin().run("");
    }

}
