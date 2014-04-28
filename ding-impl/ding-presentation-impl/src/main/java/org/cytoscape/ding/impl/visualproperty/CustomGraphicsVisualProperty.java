package org.cytoscape.ding.impl.visualproperty;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import org.cytoscape.ding.customgraphics.CustomGraphicsRange;
import org.cytoscape.ding.customgraphics.NullCustomGraphics;
import org.cytoscape.ding.customgraphicsmgr.internal.CustomGraphicsManagerImpl;
import org.cytoscape.ding.internal.charts.CyChartFactoryManagerImpl;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.view.model.AbstractVisualProperty;
import org.cytoscape.view.presentation.charts.CyChart;
import org.cytoscape.view.presentation.charts.CyChartFactory;
import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphicsFactory;

public class CustomGraphicsVisualProperty extends AbstractVisualProperty<CyCustomGraphics> {
	
	public CustomGraphicsVisualProperty(final CyCustomGraphics<CustomGraphicLayer> defaultValue,
										final CustomGraphicsRange customGraphicsRange, 
	                                    final String id,
	                                    final String displayName, 
	                                    final Class<? extends CyIdentifiable> targetObjectDataType) {
		super(defaultValue, customGraphicsRange, id, displayName, targetObjectDataType);
	}

	@Override
	public String toSerializableString(final CyCustomGraphics value) {
		if (value instanceof CyChart)
			return value.toSerializableString();
		
		return value.getClass().getCanonicalName()+","+value.toSerializableString();
	}

	// Parse the string associated with our visual property.  Note that we depend on the first
	// part of the string being the class name that was registered with the CyCustomGraphicsManager
	@Override
	public CyCustomGraphics<CustomGraphicLayer> parseSerializableString(String value) {
		// Return dummy if something is assigned.  This should be replaced after loading session.
		if (NullCustomGraphics.getNullObject().toString().equals(value) || value.contains("NullCustomGraphics")) {
			// System.out.println("CustomGraphicsVisualProperty: returning NullCustomGraphics");
			return NullCustomGraphics.getNullObject();
		} else {
			// First check if it's a CyChart
			// ------------------------------
			// This is hack, but we've got no other way to get our hands on the
			// CyChartFactoryManager this way, because the DVisualLexicon is created statically
			final CyChartFactoryManagerImpl cfMgr = CyChartFactoryManagerImpl.getInstance();
			
			String[] parts = value.split(":");
			final CyChartFactory<? extends CustomGraphicLayer> chartFactory = cfMgr.getCyChartFactory(parts[0]);
			
			if (chartFactory != null) {
				// Skip over the chart factory id
				int offset = value.indexOf(":");
				
				return (CyCustomGraphics<CustomGraphicLayer>) chartFactory.getInstance(value.substring(offset + 1));
			} else {
				// Try to parse it as a regular custom graphics then
				// -------------------------------------------------
				// This is hack, but we've got no other way to get our hands on the
				// CyCustomGraphicsManager since the DVisualLexicon is created statically
				final CustomGraphicsManagerImpl cgMgr = CustomGraphicsManagerImpl.getInstance();
				
				parts = value.split(",");
				final CyCustomGraphicsFactory factory = cgMgr.getCustomGraphicsFactory(parts[0]);
				
				if (factory != null) {
					// Skip over the class name
					int offset = value.indexOf(",");
					
					return factory.parseSerializableString(value.substring(offset + 1));
				}
			}
		}
		
		return NullCustomGraphics.getNullObject();
	}
}
