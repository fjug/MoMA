package com.jug.gurobi;

import com.jug.fijiplugins.MotherMachineAnalyserPlugin;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Author: HongKee Moon (moon@mpi-cbg.de), Robert Haase(rhaase@mpi-cbg.de) Scientific Computing Facility
 * Organization: MPI-CBG Dresden
 * Date: October 2016
 */
public class GurobiInstaller {
    public boolean checkInstallation()
    {
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
            gurobiDialog.addMessage( "There was no gurobi license file found in your users home directory." );
            gurobiDialog.addMessage( "Please acquire a license at " );
            gurobiDialog.addMessage( "http://www.gurobi.com/downloads/licenses/license-center" );
            gurobiDialog.addMessage( "Afterwards, please copy and paste the string starting with \"grbgetkey\":" );
            gurobiDialog.addStringField( "", "", 45 );
            gurobiDialog.showDialog();

            if(gurobiDialog.wasCanceled())
                return false;

            final String grbkeygetString = gurobiDialog.getNextString();

            Exec.runGrbgetkey( grbkeygetString.split( " " ) );
        }

        return gurobiLicFile.exists();
    }
}
