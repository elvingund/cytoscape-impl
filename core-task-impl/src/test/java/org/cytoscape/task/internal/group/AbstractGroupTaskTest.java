package org.cytoscape.task.internal.group;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.group.CyGroup;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.group.internal.CyGroupFactoryImpl;
import org.cytoscape.group.internal.CyGroupManagerImpl;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.model.internal.CyRootNetworkManagerImpl;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskMonitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AbstractGroupTaskTest {
	AbstractGroupTask task;
	CyGroup group1;
	CyGroup group2;
	
	CyGroupManagerImpl groupManager;
	CyNetwork network;
	CyRootNetwork rootNetwork;
	
	NetworkTestSupport support;
	CyGroupFactory groupFactory;
	
	@Mock
	CyServiceRegistrar registrar;
	
	@Mock
	private CyEventHelper eventHelper;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		
		support = new NetworkTestSupport();
		network = support.getNetwork();
		
		CyRootNetworkManager rootNetworkManager = new CyRootNetworkManagerImpl();
		rootNetwork = rootNetworkManager.getRootNetwork(network);
		
		groupManager = new CyGroupManagerImpl(eventHelper);
		groupFactory = new CyGroupFactoryImpl(eventHelper, groupManager, registrar);
		
		group1 = groupFactory.createGroup(network, true);
		rootNetwork.getRow(group1.getGroupNode(), CyRootNetwork.SHARED_ATTRS).set(CyRootNetwork.SHARED_NAME, "group1");
		
		group2 = groupFactory.createGroup(network, true);
		rootNetwork.getRow(group2.getGroupNode(), CyRootNetwork.SHARED_ATTRS).set(CyRootNetwork.SHARED_NAME, "group2");
		
		task = new AbstractGroupTask() {
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
			}
		};
		task.net = network;
		task.groupMgr = groupManager;
	}
	
	@Test
	public void testGetGroup() {
		Assert.assertEquals(group1, task.getGroup("group1"));
		Assert.assertEquals(group2, task.getGroup("group2"));
	}
}
