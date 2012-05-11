package org.cytoscape.work.internal;

import java.util.Properties;

import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.work.TunableHandlerFactory;
import org.cytoscape.work.TunableSetter;
import org.cytoscape.work.internal.sync.SyncTaskManager;
import org.cytoscape.work.internal.sync.SyncTunableHandlerFactory;
import org.cytoscape.work.internal.sync.SyncTunableMutator;
import org.cytoscape.work.internal.sync.TunableRecorderManager;
import org.cytoscape.work.internal.sync.TunableSetterImpl;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}


	public void start(BundleContext bc) {
		
		SyncTunableMutator syncTunableMutator = new SyncTunableMutator();
		SyncTaskManager syncTaskManager = new SyncTaskManager(syncTunableMutator);
		
		registerAllServices(bc,syncTaskManager, new Properties());
		
		TunableRecorderManager trm = new TunableRecorderManager();

		TunableSetterImpl tsi = new TunableSetterImpl(syncTunableMutator,trm);
		registerService(bc,tsi,TunableSetter.class, new Properties());
		
		SyncTunableHandlerFactory syncTunableHandlerFactory = new SyncTunableHandlerFactory();
		Properties syncFactoryProp = new Properties();
		registerService(bc,syncTunableHandlerFactory, TunableHandlerFactory.class, syncFactoryProp);
		syncTunableMutator.addTunableHandlerFactory(syncTunableHandlerFactory, syncFactoryProp);
		
	}
}