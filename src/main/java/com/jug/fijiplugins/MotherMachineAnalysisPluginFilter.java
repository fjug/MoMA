package com.jug.fijiplugins;

import com.jug.MoMA;
import com.jug.gurobi.GurobiInstaller;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

import java.io.File;

/**
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
 * Date: October 2016
 */
public class MotherMachineAnalysisPluginFilter implements PlugInFilter {

    @Override
    public int setup(String s, ImagePlus imagePlus) {
        return DOES_8G + DOES_16 + DOES_32;
    }

    @Override
    public void run(ImageProcessor imageProcessor) {

        if(!new GurobiInstaller().checkInstallation()) {
            IJ.log("Gurobi appears not properly installed. Please check your installation!");
            return;
        }

        ImagePlus imp = IJ.getImage();

        String tempFolder = IJ.getDirectory("temp");
        String suffix = "moma";

        int count = 0;
        while (new File(tempFolder + suffix).exists()) {
            count++;
            suffix = "moma" + count;
        }

        String targetFolder = tempFolder + suffix + "/";

        Utilities.ensureFolderExists(targetFolder);



        IJ.run(imp, "Image Sequence... ", "format=TIFF digits=4 save=[" + targetFolder + "]");

        String[] args = {
                "moma",
                "-i",
                targetFolder,
                "-c",
                "" + imp.getNChannels()
        };


        MoMA.running_as_Fiji_plugin = true;
        MoMA.main(args);


    }
}
