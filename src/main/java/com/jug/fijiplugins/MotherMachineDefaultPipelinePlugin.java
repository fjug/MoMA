package com.jug.fijiplugins;

import com.jug.gurobi.GurobiInstaller;
import com.jug.util.FloatTypeImgLoader;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.io.OpenDialog;
import ij.plugin.HyperStackConverter;
import ij.plugin.PlugIn;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * This plugin represents the full pipeline of the MoMA analysis
 * - Registration of images (motion correction)
 * - Splitting the different growth channels from the original huge images
 * - Analyse one particular growth channel. If the user wants to analyse another Growth channel, he just need to restart this plugin.
 *
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

        if(!GurobiInstaller.checkInstallation()) {
            IJ.log("Gurobi appears not properly installed. Please check your installation!");
            return;
        }

        // -------------------------------------------------------------------------------
        // plugin configuration
        GenericDialogPlus gd = new GenericDialogPlus("MoMA configuration");
        if (s.equals("file")) {
            gd.addFileField("Input_file", currentDir);
        } else {
            gd.addDirectoryField("Input_folder", currentDir);
        }
        //gd.addNumericField("Number of Channels", 2, 0);


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
        //int numberOfChannels = (int) gd.getNextNumber();


        double varianceThreshold = gd.getNextNumber();
        int lateralOffset = (int)gd.getNextNumber();
        int cropWidth = (int)gd.getNextNumber();

        currentDir = inputFolder;

        File inputFolderFile = new File(inputFolder);
        if (!inputFolderFile.exists()) {
            IJ.log("The input folder does not exist. Aborting...");
            return;
        }
        String dataSetName;
        if (inputFolderFile.isDirectory()) {
            dataSetName = inputFolderFile.getName();
        }
        else
        {
            dataSetName = inputFolderFile.getName().replace(".tiff","").replace(".tif","");
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

        if (Utilities.countFilesInFolder(registeredFolder) > 0) {
            executeRegistration = false;
        }

        if (Utilities.countFilesInFolder(splitFolder) > 0) {
            executeSplitting = false;
        }

        int numberOfTimePoints = 0;
        int numberOfChannels = 0;
        if (executeRegistration) {

            ImagePlus imp;
            ImagePlus hyperStackImp;

            // -------------------------------------------------------------------------------
            // Importing
            if (new File(inputFolder).isDirectory()) {
                IJ.run("Image Sequence...", "open=" + inputFolder + " sort");

                int min_c = Integer.MAX_VALUE;
                int max_c = Integer.MIN_VALUE;

                for (File image : inputFolderFile.listFiles(tifFilter)) {
                    int c = FloatTypeImgLoader.getChannelFromFilename(image.getName());
                    if (c < min_c) {
                        min_c = c;
                    }
                    if (c > max_c) {
                        max_c = c;
                    }
                }

                numberOfChannels = max_c - min_c + 1;


                imp = IJ.getImage();
                int numberOfSlices = imp.getNSlices();
                numberOfTimePoints = numberOfSlices / numberOfChannels;

                hyperStackImp = HyperStackConverter.toHyperStack(imp, numberOfChannels, 1, numberOfTimePoints, "default", "Color");
                hyperStackImp.show();

                // -------------------------------------------------------------------------------
                // Registration
                IJ.run(hyperStackImp, "HyperStackReg", "transformation=[Rigid Body] sliding");
            } else {
                // if it's a file:
                imp = IJ.openImage(inputFolder);
                numberOfChannels = imp.getNChannels();
                imp.show();
                hyperStackImp = imp;
            }

            ImagePlus registeredStackImp = IJ.getImage();

            // -------------------------------------------------------------------------------
            // Save intermediate results
            //IJ.run(registeredStackImp, "Image Sequence... ", "format=TIFF digits=4 save=[" + registeredFolder + "]");
            IJ.saveAsTiff(registeredStackImp, registeredFolder + dataSetName + ".tif");

            // cleanup
            registeredStackImp.close();
            hyperStackImp.close();
            if (hyperStackImp != imp ) {
                imp.close();
            }
        } else {
            IJ.log("Skipping registration...");
            //numberOfTimePoints = Utilities.countFilesInFolder(registeredFolder) / numberOfChannels;

            File registeredFolderFile = new File(registeredFolder);
            File[] filelist = registeredFolderFile.listFiles(tifFilter);
            if (filelist.length == 1) { // registration result saved as single stack file
                ImagePlus imp = IJ.openImage(filelist[0].getAbsolutePath());
                numberOfChannels = imp.getNChannels();
                numberOfTimePoints = imp.getNFrames();
            } else {
                int min_t = Integer.MAX_VALUE;
                int max_t = Integer.MIN_VALUE;
                for (File image : filelist) {

                    int t = FloatTypeImgLoader.getChannelFromFilename(image.getName());
                    if (t < min_t) {
                        min_t = t;
                    }
                    if (t > max_t) {
                        max_t = t;
                    }
                }
                numberOfTimePoints = max_t - min_t + 1;
            }
        }

        if (executeSplitting) {
            // -------------------------------------------------------------------------------
            // Run MMPreprocess

            String parameters =
                    "input_file=[" + registeredFolder + dataSetName + ".tif" + "]" +
                            " output_folder=[" + splitFolder + "]" +
                            " number_of_time_points=" + numberOfTimePoints +
                            " time_points_start_with=1" +
                            " variance_threshold=" + varianceThreshold +
                            " lateral_offset=" + lateralOffset +
                            " crop_width=" + cropWidth;




            IJ.run("MoMA pre-processing a single file", parameters);
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

    static FileFilter tifFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            return pathname.getName().endsWith(".tif");
        }
    };

}
