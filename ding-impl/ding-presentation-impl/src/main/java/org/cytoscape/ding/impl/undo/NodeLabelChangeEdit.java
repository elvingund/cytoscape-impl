package org.cytoscape.ding.impl.undo;

import org.cytoscape.ding.DVisualLexicon;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.undo.AbstractCyEdit;
import org.cytoscape.work.undo.UndoSupport;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.values.ObjectPosition;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;

/**
 * Class to undo or redo Label Move. 
 * @author jingchen
 *
 */
public class NodeLabelChangeEdit extends AbstractCyEdit {
	
	
	private ObjectPosition oldValue;
	private ObjectPosition newValue;
	private CyServiceRegistrar serviceRegistrar;
	private CyNetworkView netView;
	private Long  nodeId;

	public NodeLabelChangeEdit(CyServiceRegistrar serviceRegistrar, ObjectPosition previousValue,
			CyNetworkView netview, Long nodeId) {
		super("Move Label");
		this.serviceRegistrar = serviceRegistrar;
		
		this.oldValue = previousValue;
		this.netView = netview;
		this.nodeId = nodeId;
	}
	

	
	public void post(ObjectPosition newPosition) {
		this.newValue = newPosition;
		serviceRegistrar.getService(UndoSupport.class).postEdit(this);
	}

	
	private boolean isNetworkViewRegistered() {
		final CyNetworkViewManager netViewMgr = serviceRegistrar.getService(CyNetworkViewManager.class);
		
		return netViewMgr.getNetworkViews(netView.getModel()).contains(netView);
	}
	
	
	private void updateView() {
		final VisualStyle style = serviceRegistrar.getService(VisualMappingManager.class).getVisualStyle(netView);
		style.apply(netView);
		netView.updateView();
	}
	
	@Override
	public void undo() {
		if (isNetworkViewRegistered()) { // Make sure the network view still exists!
			
			CyNode cyN = netView.getModel().getNode(nodeId.longValue());
			View<CyNode> node = netView.getNodeView(cyN);
			if ( oldValue != null)
				node.setLockedValue(DVisualLexicon.NODE_LABEL_POSITION, oldValue);
			else
				node.clearValueLock(DVisualLexicon.NODE_LABEL_POSITION);
			
			updateView();
		}		
	}

	@Override
	public void redo() {
		if (isNetworkViewRegistered()) {
			View<CyNode> node = netView.getNodeView(netView.getModel().getNode(nodeId.longValue()));
			node.setLockedValue(DVisualLexicon.NODE_LABEL_POSITION, newValue);
			updateView();
		}	
	}

}
