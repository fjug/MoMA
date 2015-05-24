/**
 *
 */
package com.jug.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.l2fprod.common.beans.editor.DoublePropertyEditor;
import com.l2fprod.common.beans.editor.FloatPropertyEditor;
import com.l2fprod.common.beans.editor.IntegerPropertyEditor;
import com.l2fprod.common.beans.editor.StringPropertyEditor;
import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;

/**
 * @author jug
 */
public class DialogPropertiesEditor extends JDialog implements ActionListener {

	private static class PropFactory {

		private static String BGREM = "Background removal";
		private static String GL = "GrowthLine props";
		private static String TRA = "Tracking props";
		private static String SEG = "Segmentation props";
		private static String GRB = "GUROBI props";

		public static Property buildFor(final String key, final Object value) {
			final DefaultProperty property = new DefaultProperty();
			property.setDisplayName( key );
			property.setValue( value.toString() );

			if (key.equals( "BGREM_TEMPLATE_XMIN" )) {
				property.setCategory( BGREM );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new IntegerPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "BGREM_TEMPLATE_XMAX" )) {
				property.setCategory( BGREM );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new IntegerPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "BGREM_X_OFFSET" )) {
				property.setCategory( BGREM );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new IntegerPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "GL_WIDTH_IN_PIXELS" )) {
				property.setCategory( GL );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new IntegerPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "GL_FLUORESCENCE_COLLECTION_WIDTH_IN_PIXELS" )) {
				property.setCategory( GL );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new IntegerPropertyEditor() );
				property.setShortDescription( key );

			} else
			if (key.equals( "GL_OFFSET_BOTTOM" )) {
				property.setCategory( GL );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new IntegerPropertyEditor() );
				property.setShortDescription( key );

			} else
			if (key.equals( "GL_OFFSET_TOP" )) {
				property.setCategory( GL );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new IntegerPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "GL_OFFSET_LATERAL" )) {
				property.setCategory( GL );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new IntegerPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "MOTHER_CELL_BOTTOM_TRICK_MAX_PIXELS" )) {
				property.setCategory( GL );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new IntegerPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "GL_FLUORESCENCE_COLLECTION_WIDTH_IN_PIXELS" )) {
				property.setCategory( GL );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new IntegerPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "MIN_CELL_LENGTH" )) {
				property.setCategory( TRA );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new IntegerPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "MIN_GAP_CONTRAST" )) {
				property.setCategory( GL );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new FloatPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "SIGMA_PRE_SEGMENTATION_X" )) {
				property.setCategory( SEG );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new FloatPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "SIGMA_PRE_SEGMENTATION_Y" )) {
				property.setCategory( SEG );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new FloatPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "SIGMA_GL_DETECTION_X" )) {
				property.setCategory( SEG );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new FloatPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "SIGMA_GL_DETECTION_Y" )) {
				property.setCategory( SEG );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new FloatPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "SEGMENTATION_MIX_CT_INTO_PMFRF" )) {
				property.setCategory( SEG );
//				PropertyEditorRegistry.Instance.registerEditor(
//						property,
//						new FloatPropertyEditor() );
//				property.setShortDescription( key );
				property.setEditable( false );
			} else
			if (key.equals( "SEGMENTATION_CLASSIFIER_MODEL_FILE" )) {
//				property.setCategory( SEG );
//				PropertyEditorRegistry.Instance.registerEditor(
//						property,
//						new StringPropertyEditor() );
//				property.setShortDescription( key );
				property.setEditable( false );
			} else
			if (key.equals( "SEGMENTATION_CLASSIFIER_MODEL_FILE" )) {
//				property.setCategory( SEG );
//				PropertyEditorRegistry.Instance.registerEditor(
//						property,
//						new StringPropertyEditor() );
//				property.setShortDescription( key );
				property.setEditable( false );
			} else
			if (key.equals( "DEFAULT_PATH" )) {
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new StringPropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "GUROBI_TIME_LIMIT" )) {
				property.setCategory( GRB );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new DoublePropertyEditor() );
				property.setShortDescription( key );
			} else
			if (key.equals( "GUROBI_MAX_OPTIMALITY_GAP" )) {
				property.setCategory( GRB );
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new DoublePropertyEditor() );
				property.setShortDescription( key );
			} else {
				PropertyEditorRegistry.Instance.registerEditor(
						property,
						new StringPropertyEditor() );
				property.setShortDescription( key );
				property.setEditable( false );
			}
			return property;
		}
	}

	private JButton bOk;
	private JButton bCancel;
	private final Properties props;

	public DialogPropertiesEditor( final Component parent, final Properties props ) {
		super( SwingUtilities.windowForComponent( parent ), "TIMM Properties Editor" );
		this.dialogInit();
		this.setModal( true );

		final int width = 800;
		final int height = 400;

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final int screenWidth = ( int ) screenSize.getWidth();
		final int screenHeight = ( int ) screenSize.getHeight();
		this.setBounds( ( screenWidth - width ) / 2, ( screenHeight - height ) / 2, width, height );

		this.props = props;

		buildGui();
		setKeySetup();
	}

	private void buildGui() {
		this.rootPane.setLayout( new BorderLayout() );

		final PropertySheetPanel sheet = new PropertySheetPanel();
		sheet.setMode( PropertySheet.VIEW_AS_CATEGORIES );
		sheet.setDescriptionVisible( false );
		sheet.setSortingCategories( false );
		sheet.setSortingProperties( false );
		sheet.setRestoreToggleStates( false );
		sheet.setEditorFactory( PropertyEditorRegistry.Instance );

		for ( final Object propKey : this.props.keySet() ) {

			final String key = propKey.toString();
			sheet.addProperty( PropFactory.buildFor( key, props.getProperty( key ) ) );
		}

		bOk = new JButton( "OK" );
		bOk.addActionListener( this );
		this.rootPane.setDefaultButton( bOk );
		bCancel = new JButton( "Cancel" );
		bCancel.addActionListener( this );
		final JPanel horizontalHelper = new JPanel( new FlowLayout( FlowLayout.CENTER, 5, 0 ) );
		horizontalHelper.add( bCancel );
		horizontalHelper.add( bOk );

		this.rootPane.add( sheet, BorderLayout.CENTER );
		this.rootPane.add( horizontalHelper, BorderLayout.SOUTH );
	}

	private void setKeySetup() {
		this.rootPane.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( KeyStroke.getKeyStroke( "ESCAPE" ), "closeAction" );

		this.rootPane.getActionMap().put( "closeAction", new AbstractAction() {

			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed( final ActionEvent e ) {
				setVisible( false );
				dispose();
			}
		} );

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed( final ActionEvent e ) {
		if ( e.getSource().equals( bOk ) ) {
			this.setVisible( false );
			this.dispose();
		}
		if ( e.getSource().equals( bCancel ) ) {
			this.setVisible( false );
			this.dispose();
		}
	}
}
