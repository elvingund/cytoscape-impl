package org.cytoscape.ding.impl;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2016 The Cytoscape Consortium
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

import static org.cytoscape.ding.DVisualLexicon.NODE_LABEL_POSITION;
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.cytoscape.ding.DNodeShape;
import org.cytoscape.ding.DVisualLexicon;
import org.cytoscape.graph.render.stateful.CustomGraphicsInfo;
import org.cytoscape.graph.render.stateful.NodeDetails;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.CyNetworkViewSnapshot;
import org.cytoscape.view.model.SnapshotNodeInfo;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.customgraphics.CustomGraphicLayer;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.Justification;
import org.cytoscape.view.presentation.property.values.LineType;
import org.cytoscape.view.presentation.property.values.ObjectPosition;
import org.cytoscape.view.presentation.property.values.Position;


public class DNodeDetails implements NodeDetails {

	private static final float NESTED_IMAGE_SCALE_FACTOR = 0.6f;
	
	private final DRenderingEngine re;
	private final CyServiceRegistrar registrar;
	
	// These images will be used when a view is not available for a nested network.
	private static BufferedImage DEFAULT_NESTED_NETWORK_IMAGE;
	private static BufferedImage RECURSIVE_NESTED_NETWORK_IMAGE;
	
	// Used to detect recursive rendering of nested networks.
	private static int nestedNetworkPaintingDepth = 0;
	
	static {
		// Initialize image icons for nested networks
		try {
			DEFAULT_NESTED_NETWORK_IMAGE   = ImageIO.read(DNodeDetails.class.getClassLoader().getResource("images/default_network.png"));
			RECURSIVE_NESTED_NETWORK_IMAGE = ImageIO.read(DNodeDetails.class.getClassLoader().getResource("images/recursive_network.png"));
		} catch (IOException e) {
			e.printStackTrace();
			DEFAULT_NESTED_NETWORK_IMAGE = null;
			RECURSIVE_NESTED_NETWORK_IMAGE = null;
		}
	}
	
	public DNodeDetails(DRenderingEngine re, CyServiceRegistrar registrar) {
		this.re = re;
		this.registrar = registrar;
	}
	
	
	@Override
	public boolean isSelected(View<CyNode> nodeView) {
		return Boolean.TRUE.equals(nodeView.getVisualProperty(BasicVisualLexicon.NODE_SELECTED));
	}
	
	static Paint getTransparentColor(Paint p, Integer trans) {
		if(trans == null)
			return p;
		int alpha = trans;
		if (p instanceof Color && ((Color)p).getAlpha() != alpha) {
			final Color c = (Color) p;
			return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
		} else {
			return p;
		}
	}
	
	@Override
	public Color getColorLowDetail(CyNetworkViewSnapshot netView, View<CyNode> nodeView) {
		if (isSelected(nodeView))
			return getSelectedColorLowDetail(netView, nodeView);
		else
			return getUnselectedColorLowDetail(netView, nodeView);
	}
	
	private Color getUnselectedColorLowDetail(CyNetworkViewSnapshot netView, View<CyNode> nodeView) {
		Paint paint = nodeView.getVisualProperty(NODE_FILL_COLOR);
		if(paint instanceof Color)
			return (Color) paint;
		
		paint = netView.getViewDefault(NODE_FILL_COLOR);
		if(paint instanceof Color)
			return (Color) paint;
		
		return (Color) NODE_FILL_COLOR.getDefault();
	}

	private Color getSelectedColorLowDetail(CyNetworkViewSnapshot netView, View<CyNode> nodeView) {
		Paint paint = nodeView.getVisualProperty(NODE_SELECTED_PAINT);
		if(paint instanceof Color)
			return (Color) paint;
		
		paint = netView.getViewDefault(NODE_SELECTED_PAINT);
		if(paint instanceof Color)
			return (Color) paint;
		
		return (Color) NODE_SELECTED_PAINT.getDefault();
	}

	@Override
	public Paint getFillPaint(View<CyNode> nodeView) {
		if (isSelected(nodeView))
			return getSelectedPaint(nodeView);
		else
			return getUnselectedPaint(nodeView);
	}

	private Paint getSelectedPaint(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_SELECTED_PAINT);
	}

	public Paint getUnselectedPaint(View<CyNode> nodeView) {
		Paint paint = nodeView.getVisualProperty(NODE_FILL_COLOR);
		Integer trans = nodeView.getVisualProperty(NODE_TRANSPARENCY);
		return getTransparentColor(paint, trans);
	}

	@Override
	public byte getShape(View<CyNode> nodeView) {
		return DNodeShape.getDShape(nodeView.getVisualProperty(NODE_SHAPE)).getNativeShape();
	}

	@Override
	public double getWidth(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_WIDTH);
	}
	
	@Override
	public double getHeight(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_HEIGHT);
	}
	
	@Override
	public float getBorderWidth(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_BORDER_WIDTH).floatValue();
	}

	@Override
	public Stroke getBorderStroke(View<CyNode> nodeView) {
		float borderWidth = getBorderWidth(nodeView);
		LineType lineType = nodeView.getVisualProperty(NODE_BORDER_LINE_TYPE);
		return DLineType.getDLineType(lineType).getStroke(borderWidth);
	}
	
	@Override
	public Paint getBorderPaint(View<CyNode> nodeView) {
		Paint paint = nodeView.getVisualProperty(NODE_BORDER_PAINT);
		Integer trans = nodeView.getVisualProperty(NODE_BORDER_TRANSPARENCY);
		return getTransparentColor(paint, trans);
	}

	@Override
	public int getLabelCount(View<CyNode> nodeView) {
		String label = getLabelText(nodeView);
		return (label == null || label.isEmpty()) ? 0 : 1;
	}

	@Override
	public String getLabelText(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_LABEL);
	}

	@Override
	public String getTooltipText(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_TOOLTIP);
	}

	@Override
	public Font getLabelFont(View<CyNode> nodeView) {
		Number size = nodeView.getVisualProperty(NODE_LABEL_FONT_SIZE);
		Font   font = nodeView.getVisualProperty(NODE_LABEL_FONT_FACE);
		if (size != null && font != null)
			font = font.deriveFont(size.floatValue());
		return font;
	}

	@Override
	public Paint getLabelPaint(View<CyNode> nodeView) {
		Paint paint = nodeView.getVisualProperty(NODE_LABEL_COLOR);
		Integer trans = nodeView.getVisualProperty(NODE_LABEL_TRANSPARENCY);
		return getTransparentColor(paint, trans);
	}


	private CustomGraphicsInfo getCustomGraphicsInfo(VisualProperty<CyCustomGraphics> cgVP, View<CyNode> node) {
		CyCustomGraphics<CustomGraphicLayer> cg = (CyCustomGraphics<CustomGraphicLayer>) node.getVisualProperty(cgVP);
		if(cg == null)
			return null;
		
		DVisualLexicon lexicon = re.getVisualLexicon();
		VisualProperty<Double> sizeVP = lexicon.getAssociatedCustomGraphicsSizeVP(cgVP);
		Double size = node.getVisualProperty(sizeVP);
		
		VisualProperty<ObjectPosition> positionVP = lexicon.getAssociatedCustomGraphicsPositionVP(cgVP);
		ObjectPosition position = node.getVisualProperty(positionVP);
		
		CustomGraphicsInfo info = new CustomGraphicsInfo(cgVP);
		info.setCustomGraphics(cg);
		info.setSize(size);
		info.setPosition(position);
		return info;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<VisualProperty<CyCustomGraphics>, CustomGraphicsInfo> getCustomGraphics(View<CyNode> nodeView) {
		Map<VisualProperty<CyCustomGraphics>, CustomGraphicsInfo> cgInfoMap = new TreeMap<>(Comparator.comparing(VisualProperty::getIdString));
		
		DVisualLexicon lexicon = re.getVisualLexicon();
		for (VisualProperty<CyCustomGraphics> cgVP : lexicon.getCustomGraphicsVisualProperties()) {
			CustomGraphicsInfo info = getCustomGraphicsInfo(cgVP, nodeView);
			if(info != null)
				cgInfoMap.put(cgVP, info);
		}
		return cgInfoMap;
	}
	
	@Override
	public Position getLabelTextAnchor(View<CyNode> nodeView) {
		ObjectPosition pos = nodeView.getVisualProperty(NODE_LABEL_POSITION);
		return pos == null ? null : pos.getAnchor();
	}

	@Override
	public Position getLabelNodeAnchor(View<CyNode> nodeView) {
		ObjectPosition pos = nodeView.getVisualProperty(NODE_LABEL_POSITION);
		return pos == null ? null : pos.getTargetAnchor();
	}

	@Override
	public float getLabelOffsetVectorX(View<CyNode> nodeView) {
		ObjectPosition pos = nodeView.getVisualProperty(NODE_LABEL_POSITION);
		return pos == null ? 0.0f : (float) pos.getOffsetX();
	}

	@Override
	public float getLabelOffsetVectorY(View<CyNode> nodeView) {
		ObjectPosition pos = nodeView.getVisualProperty(NODE_LABEL_POSITION);
		return pos == null ? 0.0f : (float) pos.getOffsetY();
	}

	@Override
	public Justification getLabelJustify(View<CyNode> nodeView) {
		ObjectPosition pos = nodeView.getVisualProperty(NODE_LABEL_POSITION);
		return pos == null ? null : pos.getJustify();
	}
	
	@Override
	public double getLabelWidth(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_LABEL_WIDTH);
	}
	
	////// Transparencies /////////////
	
	public Integer getTransparency(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_TRANSPARENCY);
	}

	public Integer getLabelTransparency(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_LABEL_TRANSPARENCY);
	}

	@Override
	public Integer getBorderTransparency(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_BORDER_TRANSPARENCY);
	}

	public Boolean getNestedNetworkImgVisible(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_NESTED_NETWORK_IMAGE_VISIBLE);
	}

	public Double getNodeDepth(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_DEPTH);
	}

	@Override
	public double getXPosition(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_X_LOCATION);
	}

	@Override
	public double getYPosition(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_Y_LOCATION);
	}
	
	@Override
	public double getZPosition(View<CyNode> nodeView) {
		return nodeView.getVisualProperty(NODE_Z_LOCATION);
	}
	
	
	@Override
	public TexturePaint getNestedNetworkTexturePaint(CyNetworkViewSnapshot netView, View<CyNode> nodeView) {
		++nestedNetworkPaintingDepth;
		try {
			boolean nestedNetworkVisible = getNestedNetworkImgVisible(nodeView);
			if(!Boolean.TRUE.equals(nestedNetworkVisible)) {
				return null;
			}
			
			SnapshotNodeInfo nodeInfo = netView.getNodeInfo(nodeView);
			CyNode modelNode = netView.getMutableNetworkView().getModel().getNode(nodeInfo.getModelSUID());
			
			if (modelNode == null || nestedNetworkPaintingDepth > 1 ||  modelNode.getNetworkPointer() == null)
				return null;

			final double IMAGE_WIDTH  = getWidth(nodeView)  * NESTED_IMAGE_SCALE_FACTOR;
			final double IMAGE_HEIGHT = getHeight(nodeView) * NESTED_IMAGE_SCALE_FACTOR;

			CyNetworkView nestedNetworkView = getNestedNetworkView(netView, nodeView);

			// Do we have a node w/ a self-reference?
			if (netView.getMutableNetworkView() == nestedNetworkView) {
				if (RECURSIVE_NESTED_NETWORK_IMAGE == null)
					return null;

				final Rectangle2D rect = new Rectangle2D.Double(-IMAGE_WIDTH / 2, -IMAGE_HEIGHT / 2, IMAGE_WIDTH, IMAGE_HEIGHT);
				return new TexturePaint(RECURSIVE_NESTED_NETWORK_IMAGE, rect);
			}
			
			if (nestedNetworkView != null) {
				DingRenderer dingRenderer = registrar.getService(DingRenderer.class);
				
				DRenderingEngine re = dingRenderer.getRenderingEngine(netView.getMutableNetworkView());
				double scaleFactor = re.getGraphLOD().getNestedNetworkImageScaleFactor();
				
				DRenderingEngine nestedRe = dingRenderer.getRenderingEngine(nestedNetworkView);
				if(nestedRe == null)
					return null; // not a Ding network
				
				return nestedRe.getSnapshot(IMAGE_WIDTH * scaleFactor, IMAGE_HEIGHT * scaleFactor);
			} else {
				if (DEFAULT_NESTED_NETWORK_IMAGE == null || getWidth(nodeView) == -1 || getHeight(nodeView) == -1)
					return null;

				Rectangle2D rect = new Rectangle2D.Double(-IMAGE_WIDTH / 2, -IMAGE_HEIGHT / 2, IMAGE_WIDTH, IMAGE_HEIGHT);
				return new TexturePaint(DEFAULT_NESTED_NETWORK_IMAGE, rect);
			}
		} finally {
			--nestedNetworkPaintingDepth;
		}
	}

	public CyNetworkView getNestedNetworkView(CyNetworkViewSnapshot netView, View<CyNode> nodeView) {
		SnapshotNodeInfo nodeInfo = netView.getNodeInfo(nodeView);
		CyNode modelNode = netView.getMutableNetworkView().getModel().getNode(nodeInfo.getModelSUID());
		
		if(modelNode.getNetworkPointer() == null)
			return null;
		
		CyNetworkViewManager netViewMgr = registrar.getService(CyNetworkViewManager.class);
		Iterator<CyNetworkView> viewIterator = netViewMgr.getNetworkViews(modelNode.getNetworkPointer()).iterator();
		
		if(viewIterator.hasNext())
			return viewIterator.next();
		
		return null;
	}
}
