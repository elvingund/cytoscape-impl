package org.cytoscape.ding.internal.charts.heatmap;

import java.util.Map;

import javax.swing.Icon;

import org.cytoscape.ding.internal.charts.ViewUtils;
import org.cytoscape.view.presentation.charts.CyChart;
import org.cytoscape.view.presentation.charts.CyChartFactory;
import org.cytoscape.view.presentation.property.values.CyColumnIdentifierFactory;

public class HeatMapChartFactory implements CyChartFactory<HeatMapLayer> {
	
	private final CyColumnIdentifierFactory colIdFactory;
	
	public HeatMapChartFactory(final CyColumnIdentifierFactory colIdFactory) {
		this.colIdFactory = colIdFactory;
	}
	
	@Override
	public CyChart<HeatMapLayer> getInstance(final String input) {
		return new HeatMapChart(input, colIdFactory);
	}

	@Override
	public CyChart<HeatMapLayer> getInstance(final CyChart<HeatMapLayer> chart) {
		return new HeatMapChart((HeatMapChart)chart, colIdFactory);
	}
	
	@Override
	public CyChart<HeatMapLayer> getInstance(final Map<String, Object> properties) {
		return new HeatMapChart(properties, colIdFactory);
	}

	@Override
	public String getId() {
		return HeatMapChart.FACTORY_ID;
	}
	
	@Override
	public Class<? extends CyChart<HeatMapLayer>> getSupportedClass() {
		return HeatMapChart.class;
	}

	@Override
	public String getDisplayName() {
		return "Heat Map";
	}
	
	@Override
	public Icon getIcon(int width, int height) {
		return ViewUtils.resizeIcon(HeatMapChart.ICON, width, height);
	}
	
	@Override
	public String toString() {
		return getDisplayName();
	}
}