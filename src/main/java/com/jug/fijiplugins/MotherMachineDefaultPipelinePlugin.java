package com.jug.fijiplugins;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.io.OpenDialog;
import ij.plugin.HyperStackConverter;
import ij.plugin.PlugIn;

import java.io.File;

/**
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
 * Date: October 2016
 */
public class MotherMachineDefaultPipelinePlugin implements PlugIn {


    @Override
    public void run(String s) {
        String currentDir = Prefs.getDefaultDirectory();

        GenericDialogPlus gd = new GenericDialogPlus("MoMA configuration");
        gd.addDirectoryField("Input folder", currentDir);
        gd.addNumericField("Number of Channels", 2, 0);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }
        String inputFolder = gd.getNextString();
        if (!inputFolder.endsWith("/")) {
            inputFolder = inputFolder + "/";
        }
        int numberOfChannels = (int) gd.getNextNumber();

        File inputFolderFile = new File(inputFolder);
        if (!inputFolderFile.exists() || !inputFolderFile.isDirectory()) {
            IJ.log("The input folder does not exist. Aborting...");
            return;
        }

        String registeredFolder = inputFolder + "1_registered/";
        String splitFolder = inputFolder + "2_splitted/";
        String analysisResultsFolder = inputFolder + "3_analysed/";

        ensureFolderExists(registeredFolder);
        ensureFolderExists(splitFolder);
        ensureFolderExists(analysisResultsFolder);

        boolean executeRegistration = true;
        boolean executeSplitting = true;

        if (countFilesInFolder(registeredFolder) == countFilesInFolder(inputFolder)) {
            executeRegistration = false;
        }

        if (countFilesInFolder(splitFolder) > 0) {
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
            numberOfTimePoints = countFilesInFolder(registeredFolder) / numberOfChannels;
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
                            " time_points_start_with=1";

            IJ.run("MoMA pre-processing", parameters);
        } else {
            IJ.log("Skipping splitting (MMPreprocess)...");
        }

        // -------------------------------------------------------------------------------
        // Dataset selection

        String[] datasets = listSubFolderNames(splitFolder);

        if (datasets.length == 0) {
            IJ.log("No data sets found. Consider removing the 2_spitted subfolder to rerun splitting (MMPreprocess).");
            return;
        }

        String[] dataSetDescriptions = new String[datasets.length];

        int nextIndexToAnalyse = -1;
        for (int i = 0; i < datasets.length; i++) {
            dataSetDescriptions[i] = datasets[i];
            if (countFilesInFolder(analysisResultsFolder + datasets[i] + "/") > 0) {
                dataSetDescriptions[i] += " *";
            } else {
                if (nextIndexToAnalyse < 0)
                {
                    nextIndexToAnalyse = i;
                }
            }
        }


        GenericDialogPlus gdDataSetSelection = new GenericDialogPlus("MoMA dataset selection");
        gdDataSetSelection.addChoice("Dataset", dataSetDescriptions, dataSetDescriptions[nextIndexToAnalyse]);
        gdDataSetSelection.addMessage("Datasets marked with a * were analysed already.");
        gdDataSetSelection.showDialog();
        if (gdDataSetSelection.wasCanceled()) {
            return;
        }

        String selectedDataSet = datasets[gdDataSetSelection.getNextChoiceIndex()];

        String momaInputFolder = splitFolder + selectedDataSet + "/";
        String momaOutputFolder = analysisResultsFolder + selectedDataSet + "/";

        ensureFolderExists(momaOutputFolder);

        // -------------------------------------------------------------------------------
        // Running actual MoMA
        String momaParameters =
                "input_folder=" + momaInputFolder +
                "output_folder=" + momaOutputFolder +
                "number_of_channels=" + numberOfChannels;

        IJ.run("MoMA", momaParameters);


        // -------------------------------------------------------------------------------
    }

    private void ensureFolderExists(String folder) {
        File file = new File(folder);
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
        }
    }

    private int countFilesInFolder(String folder) {
        File file = new File(folder);
        if (!file.exists() || !file.isDirectory())
        {
            return 0;
        }
        int count = 0;
        for (File subfile : file.listFiles()) {
            if (!subfile.isDirectory())
            {
                IJ.log(subfile.getName());
                count++;
            }
        }
        IJ.log("" + count + " files in folder " + folder);
        return count;
    }

    private String[] listSubFolderNames(String folder)
    {
        File file = new File(folder);
        if (!file.exists() || !file.isDirectory())
        {
            return new String[] {};
        }

        int numberOfFolders = file.listFiles().length - countFilesInFolder(folder);

        String[] listOfFolderNames = new String[numberOfFolders];

        int count = 0;
        for (File subfile : file.listFiles()) {
            if (subfile.isDirectory()) {
                listOfFolderNames[count] = subfile.getName();
                count++;
            }
        }
        return listOfFolderNames;
    }
}