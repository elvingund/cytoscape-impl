package org.cytoscape.internal.view.help;

import org.cytoscape.application.CyVersion;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

/*
 * #%L
 * Cytoscape Swing Application Impl (swing-application-impl)
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

public class HelpUserManualTask extends AbstractTask {
	
	private static final String MANUAL_URL = "http://manual.cytoscape.org/en/";
	
	private final CyServiceRegistrar serviceRegistrar;

	public HelpUserManualTask(final CyServiceRegistrar serviceRegistrar) {
		this.serviceRegistrar = serviceRegistrar;
	}

	@Override
	public void run(TaskMonitor tm) {
		final OpenBrowser openBrowser = serviceRegistrar.getService(OpenBrowser.class);
		final CyVersion cyVersion = serviceRegistrar.getService(CyVersion.class);
		// final int fixVersion = cyVersion.getBugFixVersion();
		final int fixVersion = 0; // Overriden for hotfix (impl only) release
		
		openBrowser.openURL(MANUAL_URL + 
				cyVersion.getMajorVersion() + "." +
				cyVersion.getMinorVersion() + "." +
				fixVersion);
	}
}
