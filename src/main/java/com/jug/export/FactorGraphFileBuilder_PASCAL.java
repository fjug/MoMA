/**
 *
 */
package com.jug.export;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author jug
 */
public class FactorGraphFileBuilder_PASCAL {

	int next_var_id = 0;
	int next_fkt_id = 0;
	int next_fac_id = 0;
	int next_con_id = 0;

	List< String > var_comment_lines = new ArrayList< String >();
	String  var_line = "";
	List< String > fkt_lines = new ArrayList< String >();
	List< String > fac_lines = new ArrayList< String >();
	List< String > constraint_lines = new ArrayList< String >();

	/**
	 * Returns the number of variables added so far.
	 *
	 * @return surprise! ;)
	 */
	public int getNumVars() {
		return next_var_id;
	}

	/**
	 * Returns the number of functions added so far.
	 *
	 * @return surprise! ;)
	 */
	public int getNumFunctions() {
		return next_fkt_id;
	}

	/**
	 * Returns the number of factors added so far.
	 *
	 * @return surprise! ;)
	 */
	public int getNumFactors() {
		return next_fac_id;
	}

	/**
	 * Returns the number of constraints added so far.
	 *
	 * @return surprise! ;)
	 */
	public int getNumConstraints() {
		return next_con_id;
	}

	/**
	 * Adds a comment line in the variable-section.
	 *
	 * @param comment
	 *            the String that should be added as a comment.
	 */
	public void addVarComment( final String comment ) {
		var_comment_lines.add( "# " + comment );
	}

	/**
	 * Adds a variable.
	 *
	 * @param cardinality
	 *            number of states this discrete variable can have.
	 * @return the id of the variable just added.
	 */
	public int addVar( final int cardinality ) {
		var_line += Integer.toString( cardinality ) + " ";
		return next_var_id++;
	}

	/**
	 * Adds a comment line in the function-section.
	 *
	 * @param comment
	 *            the String that should be added as a comment.
	 */
	public void addFktComment( final String comment ) {
		fkt_lines.add( "# " + comment );
	}

	/**
	 * Adds a pre-assembled String that fully describes a function.
	 *
	 * @param line
	 *            the string to be added.
	 * @return the id of the function just added.
	 */
	public int addFkt( final String line ) {
		fkt_lines.add( line );
		return next_fkt_id++;
	}

	/**
	 * Adds a function given a list of variable indices.
	 *
	 * @param varIdx
	 * @return
	 */
	public int addFkt( final int... varIdx ) {
		String line = "" + varIdx.length + " ";
		for ( final int idx : varIdx ) {
			line += "" + idx + " ";
		}
		return addFkt( line );
	}

	/**
	 * Adds a comment line in the factor-section.
	 *
	 * @param comment
	 *            the String that should be added as a comment.
	 */
	public void addFactorComment( final String comment ) {
		fac_lines.add( "# " + comment );
	}

	/**
	 * Adds a pre-assembled String that fully describes a factor.
	 *
	 * @param line
	 *            the string to be added.
	 * @return the id of the factor just added.
	 */
	public int addFactor( final String line ) {
		fac_lines.add( line );
		return next_fac_id++;
	}

	/**
	 * Adds a unary factor given by a list of tensor values.
	 *
	 * @param unaries
	 * @return
	 */
	public int addFactor( final float... unaries ) {
		String line = "" + unaries.length + "\n\t";
		for ( final float c : unaries ) {
			line += "" + c + " ";
		}
		return addFactor( line );
	}

	/**
	 * Adds a comment line in a constraint.
	 *
	 * @param comment
	 *            the String that should be added as a comment.
	 */
	public void addConstraintComment( final String comment ) {
		constraint_lines.add( "# " + comment );
	}

	/**
	 * Adds a pre-assembled String that fully describes a factor.
	 *
	 * @param line
	 *            the string to be added.
	 * @return the id of the factor just added.
	 */
	public int addConstraint( final String line ) {
		constraint_lines.add( line );
		return next_con_id++;
	}

	/**
	 * Adds a pre-assembled list of Strings that fully describe some
	 * constraints.
	 *
	 * @param lines
	 *            the strings to be added.
	 * @return the id of the last factor added, or -1 in case the given list was
	 *         empty.
	 */
	public int addConstraints( final List< String > lines ) {
		int last_id = -1;
		for ( final String line : lines ) {
			last_id = addConstraint( line );
		}
		return last_id;
	}

	/**
	 * @param file
	 */
	public void write( final File file ) {
		BufferedWriter out;
		try {
			out = new BufferedWriter( new FileWriter( file ) );
			out.write( "# EXPORTED MM-TRACKING WITH CONSTRAINTS (jug@mpi-cbg.de)" );
			out.newLine();
			out.write( "MARKOV" );
			out.newLine();
			out.newLine();

			out.write( "# #### VARIABLE SECTION ###################################" );
			out.newLine();
			for ( final String line : var_comment_lines ) {
				out.write( line );
				out.newLine();
			}
			out.write( "" + getNumVars() );
			out.newLine();
			out.write( var_line );
			out.newLine();

			out.write( "# #### FUNCTION SECTION ###################################" );
			out.newLine();
			for ( final String line : fkt_lines ) {
				out.write( line );
				out.newLine();
			}
			out.write( "# #### FACTOR SECTION #####################################" );
			out.newLine();
			for ( final String line : fac_lines ) {
				out.write( line );
				out.newLine();
			}
			out.write( "# #### CONSTRAINT SECTION #################################" );
			out.newLine();
			for ( final String line : constraint_lines ) {
				out.write( line );
				out.newLine();
			}
			out.close();
		}
		catch ( final IOException e ) {
			e.printStackTrace();
		}
	}
}
