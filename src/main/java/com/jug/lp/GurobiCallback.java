package com.jug.lp;

import gurobi.GRB;
import gurobi.GRBCallback;
import gurobi.GRBException;

import com.jug.MoMA;
import com.jug.gui.progress.DialogGurobiProgress;

public class GurobiCallback extends GRBCallback {

//	private final double lastiter;
	private double lastnode;
//	private final GRBVar[] vars;

	private final DialogGurobiProgress dialog;
	private double latestGap;

	public GurobiCallback( final DialogGurobiProgress dialog ) { // final GRBVar[] xvars, 
//		lastiter = -GRB.INFINITY;
		lastnode = -GRB.INFINITY;
//		vars = xvars;
		this.dialog = dialog;
		this.latestGap = Double.POSITIVE_INFINITY;
	}

	@Override
	protected void callback() {
		try {
			if ( where == GRB.CB_POLLING ) {
				// Ignore polling callback
			} else if ( where == GRB.CB_PRESOLVE ) {
				// Presolve callback
//				final int cdels = getIntInfo( GRB.CB_PRE_COLDEL );
//				final int rdels = getIntInfo( GRB.CB_PRE_ROWDEL );
//				if ( cdels != 0 || rdels != 0 ) {
//					System.out.println( cdels + " columns and " + rdels + " rows are removed" );
//				}

			} else if ( where == GRB.CB_SIMPLEX ) {
				// Simplex callback
//				final double itcnt = getDoubleInfo( GRB.CB_SPX_ITRCNT );
//				if ( itcnt - lastiter >= 100 ) {
//					lastiter = itcnt;
//					final double obj = getDoubleInfo( GRB.CB_SPX_OBJVAL );
//					final int ispert = getIntInfo( GRB.CB_SPX_ISPERT );
//					final double pinf = getDoubleInfo( GRB.CB_SPX_PRIMINF );
//					final double dinf = getDoubleInfo( GRB.CB_SPX_DUALINF );
//					char ch;
//					if ( ispert == 0 )
//						ch = ' ';
//					else if ( ispert == 1 )
//						ch = 'S';
//					else
//						ch = 'P';
//					System.out.println( itcnt + " " + obj + ch + " " + pinf + " " + dinf );
//				}

			} else if ( where == GRB.CB_MIP ) {
				// General MIP callback
				final double nodecnt = getDoubleInfo( GRB.CB_MIP_NODCNT );
				final double objbst = getDoubleInfo( GRB.CB_MIP_OBJBST );
				final double objbnd = getDoubleInfo( GRB.CB_MIP_OBJBND );
				final int solcnt = getIntInfo( GRB.CB_MIP_SOLCNT );
				final double runtime = getDoubleInfo( GRB.CB_RUNTIME );

				this.latestGap = Math.abs( objbst - objbnd ) / ( 1.0 + Math.abs( objbst ) );

				if ( nodecnt - lastnode >= 100 ) {
					lastnode = nodecnt;
					final int actnodes = ( int ) getDoubleInfo( GRB.CB_MIP_NODLFT );
					final int itcnt = ( int ) getDoubleInfo( GRB.CB_MIP_ITRCNT );
					final int cutcnt = getIntInfo( GRB.CB_MIP_CUTCNT );
					System.out.println( nodecnt + " " + actnodes + " " + itcnt + " " + objbst + " " + objbnd + " " + solcnt + " " + cutcnt );
				}
				if ( runtime > MoMA.GUROBI_TIME_LIMIT ) {
					if ( Math.abs( objbst - objbnd ) < MoMA.GUROBI_MAX_OPTIMALITY_GAP * ( 1.0 + Math.abs( objbst ) ) ) {
						abort();
					}
				}
//				if ( nodecnt >= 10000 && solcnt > 0 ) {
//					System.out.println( "Stop early - 10000 nodes explored" );
//					abort();
//				}

			} else if ( where == GRB.CB_MIPSOL ) {
				// MIP solution callback
//				final int nodecnt = ( int ) getDoubleInfo( GRB.CB_MIPSOL_NODCNT );
//				final double obj = getDoubleInfo( GRB.CB_MIPSOL_OBJ );
//				final int solcnt = getIntInfo( GRB.CB_MIPSOL_SOLCNT );
//				final double[] x = getSolution( vars );
//				System.out.println( "**** New solution at node " + nodecnt + ", obj " + obj + ", sol " + solcnt + ", x[0] = " + x[ 0 ] + " ****" );

			} else if ( where == GRB.CB_MIPNODE ) {
				// MIP node callback
//				System.out.println( "**** New node ****" );
//				if ( getIntInfo( GRB.CB_MIPNODE_STATUS ) == GRB.OPTIMAL ) {
//					final double[] x = getNodeRel( vars );
//					setSolution( vars, x );
//				}

			} else if ( where == GRB.CB_BARRIER ) {
				// Barrier callback
//				final int itcnt = getIntInfo( GRB.CB_BARRIER_ITRCNT );
//				final double primobj = getDoubleInfo( GRB.CB_BARRIER_PRIMOBJ );
//				final double dualobj = getDoubleInfo( GRB.CB_BARRIER_DUALOBJ );
//				final double priminf = getDoubleInfo( GRB.CB_BARRIER_PRIMINF );
//				final double dualinf = getDoubleInfo( GRB.CB_BARRIER_DUALINF );
//				final double cmpl = getDoubleInfo( GRB.CB_BARRIER_COMPL );
//				System.out.println( itcnt + " " + primobj + " " + dualobj + " " + priminf + " " + dualinf + " " + cmpl );

			} else if ( where == GRB.CB_MESSAGE ) {
				// Message callback
				final String msg = getStringInfo( GRB.CB_MSG_STRING );
				final double runtime = getDoubleInfo( GRB.CB_RUNTIME );

				if ( msg != null ) {
					System.out.println( msg );
				}
				pushStatusToDialog( String.format( "Runtime: %.1f sec.; Current gap: %.2f%%", runtime, latestGap * 100.0 ) );
			}
		} catch ( final GRBException e ) {
			System.out.println( "Error code: " + e.getErrorCode() );
			System.out.println( e.getMessage() );
			e.printStackTrace();
		} catch ( final Exception e ) {
			System.out.println( "Error during callback" );
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	private void notifyTerminationToDialog() {
		if ( dialog != null ) dialog.notifyGurobiTermination();
	}

	/**
	 * @param string
	 */
	private void pushStatusToDialog( final String string ) {
//		System.out.println( ">>>>>>> " + string );
		if ( dialog != null ) dialog.pushStatus( string );
	}

	/**
	 * @return
	 */
	public double getLatestGap() {
		return this.latestGap;
	}
}
