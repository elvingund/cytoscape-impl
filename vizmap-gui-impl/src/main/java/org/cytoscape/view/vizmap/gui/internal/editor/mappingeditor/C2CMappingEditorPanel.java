/*
 Copyright (c) 2006, 2007, The Cytoscape Consortium (www.cytoscape.org)

 The Cytoscape Consortium is:
 - Institute for Systems Biology
 - University of California San Diego
 - Memorial Sloan-Kettering Cancer Center
 - Institut Pasteur
 - Agilent Technologies

 This library is free software; you can redistribute it and/or modify it
 under the terms of the GNU Lesser General Public License as published
 by the Free Software Foundation; either version 2.1 of the License, or
 any later version.

 This library is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 documentation provided hereunder is on an "as is" basis, and the
 Institute for Systems Biology and the Whitehead Institute
 have no obligations to provide maintenance, support,
 updates, enhancements or modifications.  In no event shall the
 Institute for Systems Biology and the Whitehead Institute
 be liable to any party for direct, indirect, special,
 incidental or consequential damages, including lost profits, arising
 out of the use of this software and its documentation, even if the
 Institute for Systems Biology and the Whitehead Institute
 have been advised of the possibility of such damage.  See
 the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package org.cytoscape.view.vizmap.gui.internal.editor.mappingeditor;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.gui.internal.NumberConverter;
import org.cytoscape.view.vizmap.mappings.BoundaryRangeValues;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.cytoscape.view.vizmap.mappings.ContinuousMappingPoint;
import org.jdesktop.swingx.multislider.Thumb;
import org.jdesktop.swingx.multislider.TrackRenderer;

/**
 * Continuous-Continuous mapping editor.<br>
 * 
 * This editor will be used for Number-to-Number mapping.
 * 
 * <p>
 * This is a editor for continuous values, i.e., numbers.
 * </p>
 * 
 */
public class C2CMappingEditorPanel<K extends Number, V extends Number> extends ContinuousMappingEditorPanel<K, V> implements PropertyChangeListener {

	private final static long serialVersionUID = 1213748836613718L;

	// Default value for below and above.
	private static final Number DEF_BELOW_AND_ABOVE = 1d;
	private static final Number FIRST_LOCATION = 10d;
	private static final Number SECOND_LOCATION = 30d;

	

	public C2CMappingEditorPanel(final VisualStyle style, final ContinuousMapping<K, V> mapping, final CyTable attr,
			final CyApplicationManager appManager, final VisualMappingManager vmm, final VisualMappingFunctionFactory continuousMappingFactory) {
		super(style, mapping, attr, appManager, vmm, continuousMappingFactory);
		
		abovePanel.setVisible(false);
		belowPanel.setVisible(false);

		initSlider();
		if(mapping.getAllPoints().isEmpty()) {
			addSlider(0d, FIRST_LOCATION);
			addSlider(100d, SECOND_LOCATION);
		}
		
		setPropertySpinner();
	}
	
	private V convertToValue(final Number value) {
		return NumberConverter.convert(vpValueType, value);
	}
	
	private K convertToColumnValue(final Number value) {
		return NumberConverter.convert(columnType, value);
	}
	
	
	public ImageIcon getIcon(final int iconWidth, final int iconHeight) {
		final TrackRenderer rend = slider.getTrackRenderer();

		if (rend instanceof ContinuousTrackRenderer) {
			rend.getRendererComponent(slider);

			return ((ContinuousTrackRenderer) rend).getTrackGraphicIcon(iconWidth, iconHeight);
		} else {
			return null;
		}
	}

	public ImageIcon getLegend(final int width, final int height) {
		final ContinuousTrackRenderer<K, V> rend = (ContinuousTrackRenderer) slider.getTrackRenderer();
		rend.getRendererComponent(slider);

		return rend.getLegend(width, height);
	}

	
	/**
	 * Add New Slider to editor
	 * 
	 * @param position
	 * @param value
	 */
	private void addSlider(Number position, Number value) {
		final K maxValue = convertToColumnValue(tracer.getMax(type));
		BoundaryRangeValues<V> newRange;	
		
		if (mapping.getPointCount() == 0) {
			slider.getModel().addThumb(position.floatValue(), convertToValue(value));

			newRange = new BoundaryRangeValues<V>(convertToValue(below), convertToValue(5d), convertToValue(above));
			final K newKey = convertToColumnValue((maxValue.doubleValue() / 2));
			
			mapping.addPoint(newKey, newRange);

			updateMap();

			return;
		}

		slider.getModel().addThumb(position.floatValue(), convertToValue(value));
		
		final SortedMap<Double, ContinuousMappingPoint<K,V>> sortedPoints = new TreeMap<Double, ContinuousMappingPoint<K,V>>();
		for (final ContinuousMappingPoint<K, V> point : mapping.getAllPoints()) {
			final Number val =point.getValue();
			sortedPoints.put(val.doubleValue(), point);
		}

		final ContinuousMappingPoint<K, V> previousPoint = sortedPoints.get(sortedPoints.lastKey());
		final BoundaryRangeValues<V> previousRange = previousPoint.getRange();

		V lesserVal = previousPoint.getRange().greaterValue;
		V equalVal = previousPoint.getRange().greaterValue;
		V greaterVal = previousRange.greaterValue;

		newRange = new BoundaryRangeValues<V>(convertToValue(lesserVal), convertToValue(equalVal), convertToValue(greaterVal));
		mapping.addPoint(maxValue, newRange);

		updateMap();
	}

	@Override
	protected void addButtonActionPerformed(ActionEvent evt) {
		addSlider(100d, convertToValue(5d));
	}

	@Override
	protected void deleteButtonActionPerformed(ActionEvent evt) {
		final int selectedIndex = slider.getSelectedIndex();

		// TODO: Is this correct?
		if (0 <= selectedIndex) {
			slider.getModel().removeThumb(selectedIndex);
			mapping.removePoint(selectedIndex);
			initSlider();
		}
	}

	private void initSlider() {

		slider.updateUI();

		final Number minValue = tracer.getMin(type);
		Number actualRange = tracer.getRange(type);

		BoundaryRangeValues<V> bound;
		Number fraction;
		
		final List<Thumb<V>> sorted = slider.getModel().getSortedThumbs();
		for (Thumb<V> t : sorted)
			slider.getModel().removeThumb(slider.getModel().getThumbIndex(t));
		
		final SortedMap<Double, ContinuousMappingPoint<K,V>> sortedPoints = new TreeMap<Double, ContinuousMappingPoint<K,V>>();
		for (final ContinuousMappingPoint<K, V> point : mapping.getAllPoints()) {
			final Number val =point.getValue();
			sortedPoints.put(val.doubleValue(), point);
		}

		for (Double key : sortedPoints.keySet()) {
			ContinuousMappingPoint<K, V> point = sortedPoints.get(key);
			bound = point.getRange();
			fraction = ((Number) ((point.getValue().doubleValue() - minValue.doubleValue()) / actualRange.doubleValue())).floatValue() * 100d;
			slider.getModel().addThumb(fraction.floatValue(), bound.equalValue);
		}

		if (!sortedPoints.isEmpty()) {
			below = sortedPoints.get(sortedPoints.firstKey()).getRange().lesserValue;
			above = sortedPoints.get(sortedPoints.lastKey()).getRange().greaterValue;
		} else {
			below = convertToValue(DEF_BELOW_AND_ABOVE);
			above = convertToValue(DEF_BELOW_AND_ABOVE);
		}

		/*
		 * get min and max for the value object
		 */
		TriangleThumbRenderer thumbRend = new TriangleThumbRenderer();

		ContinuousTrackRenderer<K, V> cRend = new ContinuousTrackRenderer<K, V>(style, mapping, below, above,
				tracer, appManager);
		cRend.addPropertyChangeListener(this);

		slider.setThumbRenderer(thumbRend);
		slider.setTrackRenderer(cRend);
		slider.addMouseListener(new ThumbMouseListener());
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(ContinuousMappingEditorPanel.BELOW_VALUE_CHANGED)) {
			below = convertToValue((Number)evt.getNewValue());
		} else if (evt.getPropertyName().equals(ContinuousMappingEditorPanel.ABOVE_VALUE_CHANGED)) {
			above = convertToValue((Number)evt.getNewValue());
		}
	}

	@Override
	public ImageIcon drawIcon(int iconWidth, int iconHeight, boolean detail) {
		final TrackRenderer rend = slider.getTrackRenderer();

		if (rend instanceof ContinuousTrackRenderer) {
			rend.getRendererComponent(slider);

			return ((ContinuousTrackRenderer) rend).getTrackGraphicIcon(iconWidth, iconHeight);
		} else {
			return null;
		}
	}

	
	private void setPropertySpinner() {
		SpinnerNumberModel propertySpinnerModel = new SpinnerNumberModel(0.0d, Double.NEGATIVE_INFINITY,
				Double.POSITIVE_INFINITY, 0.01d);
		propertySpinnerModel.addChangeListener(new PropertyValueSpinnerChangeListener(propertySpinnerModel));
		propertySpinner.setModel(propertySpinnerModel);
	}

	private final class PropertyValueSpinnerChangeListener implements ChangeListener {

		private final SpinnerNumberModel spinnerModel;
		
		public PropertyValueSpinnerChangeListener(SpinnerNumberModel model) {
			this.spinnerModel = model;
		}

		public void stateChanged(ChangeEvent e) {
			
			final Number newVal = spinnerModel.getNumber().doubleValue();
			final int selectedIndex = slider.getSelectedIndex();

			slider.getModel().getThumbAt(selectedIndex).setObject(convertToValue(newVal));

			updateMap();
			slider.repaint();
		}
	}
}
