package com.jug.fijiplugins;

import ij.IJ;

import java.io.File;
import java.util.ArrayList;

/**
 * Author: Robert Haase, Scientific Computing Facility, MPI-CBG Dresden, rhaase@mpi-cbg.de
 * Date: October 2016
 */
public class Utilities {

    /**
     * Checks if a given folder location exists and creates the folder if not.
     *
     * @param folder to be created
     */
    public static void ensureFolderExists(String folder) {
        File file = new File(folder);
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
        }
    }

    /**
     * Count files in a folder which are neighter hidden nor a directory
     * @param folder folder to count files in
     * @return number of files, zero if folder does not exist
     */
    static int countFilesInFolder(String folder) {
        File file = new File(folder);
        if (!file.exists() || !file.isDirectory())
        {
            return 0;
        }
        int count = 0;
        for (File subfile : file.listFiles()) {
            if (!subfile.isDirectory() && !subfile.isHidden()) {
                count++;
            }
        }
        IJ.log("" + count + " files in folder " + folder);
        return count;
    }

    /**
     * Creates a list of sub folder names
     * @param folder main folder to look through
     * @return String array containing all sub folder names
     */
    static String[] listSubFolderNames(String folder)
    {
        File file = new File(folder);
        if (!file.exists() || !file.isDirectory()) {
            return new String[] {};
        }

        ArrayList<String> folderNameList = new ArrayList<String>();

        for (File subfile : file.listFiles()) {
            if (subfile.isDirectory() && !subfile.isHidden()) {
                folderNameList.add(subfile.getName());
            }
        }

        String[] listOfFolderNames = new String[folderNameList.size()];
        folderNameList.toArray(listOfFolderNames);
        return listOfFolderNames;
    }
}
