package com.jug.fijiplugins;

import com.jug.MoMA;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
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

        IJ.log("Hello Mother!");

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
                /*new String[5];
        args[0] = "moma";
        args[1] = "-i";
        args[2] = "/Users/rhaase/Projects/Florian_Jug_Myers_MoMA_Deployment/data/MoMA_example";
        args[3] = "-c";
        args[4] = "2";*/

        MoMA.running_as_Fiji_plugin = true;
        MoMA.main(args);
    }

    public static void main(String... args)
    {
        new ImageJ();

        new MotherMachineAnalyserPlugin().run(String.join( " ", args ));
    }

}
