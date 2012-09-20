package org.cytoscape.view.vizmap.gui.internal.editor;

import org.cytoscape.view.vizmap.gui.editor.ContinuousEditorType;
import org.cytoscape.view.vizmap.gui.internal.editor.propertyeditor.CyDiscreteValuePropertyEditor;

/**
 * Editor object for all kinds of discrete values such as Node Shape, Line
 * Stroke, etc.
 * 
 * 
 * @param <T>
 */
public class DiscreteValueVisualPropertyEditor<T> extends BasicVisualPropertyEditor<T> {

	public DiscreteValueVisualPropertyEditor(final Class<T> type, final CyDiscreteValuePropertyEditor<T> propEditor) {
		super(type, propEditor, ContinuousEditorType.DISCRETE);

		discreteTableCellRenderer = REG.getRenderer(type);
	}
}
