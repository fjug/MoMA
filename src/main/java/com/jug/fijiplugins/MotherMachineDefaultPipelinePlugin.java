package com.jug.fijiplugins;

import com.jug.gurobi.GurobiInstaller;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.io.OpenDialog;
import ij.plugin.HyperStackConverter;
import ij.plugin.PlugIn;

import java.io.File;
import java.util.ArrayList;

/**
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
 * Date: October 2016
 */
public class MotherMachineDefaultPipelinePlugin implements PlugIn {

    private static String currentDir = Prefs.getDefaultDirectory();

    public MotherMachineDefaultPipelinePlugin()
    {
        //currentDir = Prefs.getDefaultDirectory();
        //IJ.log("current dir: " + currentDir);

        IJ.register(this.getClass());
    }


    @Override
    public void run(String s) {

        if(!new GurobiInstaller().checkInstallation()) {
            IJ.log("Gurobi appears not properly installed. Please check your installation!");
            return;
        }

        // -------------------------------------------------------------------------------
        // plugin configuration
        GenericDialogPlus gd = new GenericDialogPlus("MoMA configuration");
        gd.addDirectoryField("Input folder", currentDir);
        gd.addNumericField("Number of Channels", 2, 0);


        gd.addMessage("Advanced splitting parameters (MMPreprocess)");
        gd.addNumericField("Variance threshold", 0.001, 8);
        gd.addNumericField("Lateral offset", 40, 0);
        gd.addNumericField("Crop width", 100, 0);

        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        String inputFolder = gd.getNextString();
        if (!inputFolder.endsWith("/")) {
            inputFolder = inputFolder + "/";
        }
        int numberOfChannels = (int) gd.getNextNumber();


        double varianceThreshold = gd.getNextNumber();
        int lateralOffset = (int)gd.getNextNumber();
        int cropWidth = (int)gd.getNextNumber();

        currentDir = inputFolder;

        File inputFolderFile = new File(inputFolder);
        if (!inputFolderFile.exists() || !inputFolderFile.isDirectory()) {
            IJ.log("The input folder does not exist. Aborting...");
            return;
        }

        // -------------------------------------------------------------------------------
        // create subfolders for intermediate and final analysis results
        String registeredFolder = inputFolder + "1_registered/";
        String splitFolder = inputFolder + "2_split/";
        String analysisResultsFolder = inputFolder + "3_analysed/";

        Utilities.ensureFolderExists(registeredFolder);
        Utilities.ensureFolderExists(splitFolder);
        Utilities.ensureFolderExists(analysisResultsFolder);

        boolean executeRegistration = true;
        boolean executeSplitting = true;

        if (Utilities.countFilesInFolder(registeredFolder) == Utilities.countFilesInFolder(inputFolder)) {
            executeRegistration = false;
        }

        if (Utilities.countFilesInFolder(splitFolder) > 0) {
            executeSplitting = false;
        }

        int numberOfTimePoints = 0;
        if (executeRegistration) {

            // -------------------------------------------------------------------------------
            // Importing
            IJ.run("Image Sequence...", "open=" + inputFolder + " sort");

            ImagePlus imp = IJ.getImage();
            int numberOfSlices = imp.getNSlices();
            numberOfTimePoints = numberOfSlices / numberOfChannels;

            ImagePlus hyperStackImp = HyperStackConverter.toHyperStack(imp, numberOfChannels, 1, numberOfTimePoints, "default", "Color");
            hyperStackImp.show();

            // -------------------------------------------------------------------------------
            // Registration
            IJ.run(hyperStackImp, "HyperStackReg", "transformation=[Rigid Body] sliding");
            ImagePlus registeredStackImp = IJ.getImage();

            // -------------------------------------------------------------------------------
            // Save intermediate results
            IJ.run(registeredStackImp, "Image Sequence... ", "format=TIFF digits=4 save=[" + registeredFolder + "]");

            // cleanup
            registeredStackImp.close();
            hyperStackImp.close();
            imp.close();
        } else {
            IJ.log("Skipping registration...");
            numberOfTimePoints = Utilities.countFilesInFolder(registeredFolder) / numberOfChannels;
        }

        if (executeSplitting) {
            // -------------------------------------------------------------------------------
            // Run MMPreprocess

            String parameters =
                    "input_folder=[" + registeredFolder + "]" +
                            " output_folder=[" + splitFolder + "]" +
                            " number_of_Channels=" + numberOfChannels +
                            " channels_start_with=1" +
                            " number_of_Time_points=" + numberOfTimePoints +
                            " time_points_start_with=1" +
                            " variance_threshold=" + varianceThreshold +
                            " lateral_offset=" + lateralOffset +
                            " crop_width=" + cropWidth;




            IJ.run("MoMA pre-processing", parameters);
        } else {
            IJ.log("Skipping splitting (MMPreprocess)...");
        }

        // -------------------------------------------------------------------------------
        // Dataset selection

        String[] datasets = Utilities.listSubFolderNames(splitFolder);

        if (datasets.length == 0) {
            IJ.log("No data sets found. Consider removing the 2_split subfolder to rerun splitting (MMPreprocess).");
            return;
        }
        String[] dataSetDescriptions = new String[datasets.length];
        int nextIndexToAnalyse = -1;
        for (int i = 0; i < datasets.length; i++) {
            dataSetDescriptions[i] = datasets[i];
            if (Utilities.countFilesInFolder(analysisResultsFolder + datasets[i] + "/") > 0) {
                dataSetDescriptions[i] += " *";
            } else {
                if (nextIndexToAnalyse < 0)
                {
                    nextIndexToAnalyse = i;
                }
            }
        }
        String selectedDataset = "";
        if (nextIndexToAnalyse >= 0)
        {
            selectedDataset = dataSetDescriptions[nextIndexToAnalyse];
        }

        GenericDialogPlus gdDataSetSelection = new GenericDialogPlus("MoMA dataset selection");
        gdDataSetSelection.addChoice("Dataset", dataSetDescriptions, selectedDataset);
        gdDataSetSelection.addMessage("Datasets marked with a * were analysed already.");
        gdDataSetSelection.showDialog();
        if (gdDataSetSelection.wasCanceled()) {
            return;
        }
        String selectedDataSet = datasets[gdDataSetSelection.getNextChoiceIndex()];

        String momaInputFolder = splitFolder + selectedDataSet + "/";
        String momaOutputFolder = analysisResultsFolder + selectedDataSet + "/";

        // -------------------------------------------------------------------------------
        // create MoMA output folder; it would exit if it not exists
        Utilities.ensureFolderExists(momaOutputFolder);

        // -------------------------------------------------------------------------------
        // Running actual MoMA
        String momaParameters =
                "input_folder=[" + momaInputFolder + "]" +
                " output_folder=[" + momaOutputFolder + "]" +
                " number_of_channels=" + numberOfChannels;

        IJ.log("MoMA params: " + momaParameters);

        IJ.run("MoMA", momaParameters);


        // -------------------------------------------------------------------------------
    }

}
