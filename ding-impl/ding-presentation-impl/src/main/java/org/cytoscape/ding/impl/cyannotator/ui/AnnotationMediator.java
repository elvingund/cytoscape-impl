package org.cytoscape.ding.impl.cyannotator.ui;

import static org.cytoscape.ding.internal.util.ViewUtil.invokeOnEDT;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.events.CyShutdownEvent;
import org.cytoscape.application.events.CyShutdownListener;
import org.cytoscape.application.events.CyStartEvent;
import org.cytoscape.application.events.CyStartListener;
import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;
import org.cytoscape.ding.impl.DGraphView;
import org.cytoscape.ding.impl.cyannotator.CyAnnotator;
import org.cytoscape.ding.impl.cyannotator.annotations.DingAnnotation;
import org.cytoscape.ding.impl.cyannotator.create.AbstractDingAnnotationFactory;
import org.cytoscape.ding.impl.cyannotator.create.GroupAnnotationFactory;
import org.cytoscape.ding.impl.cyannotator.tasks.AddAnnotationTask;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.events.SessionAboutToBeLoadedEvent;
import org.cytoscape.session.events.SessionAboutToBeLoadedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.presentation.annotations.Annotation;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.events.AnnotationsAddedEvent;
import org.cytoscape.view.presentation.events.AnnotationsAddedListener;
import org.cytoscape.view.presentation.events.AnnotationsRemovedEvent;
import org.cytoscape.view.presentation.events.AnnotationsRemovedListener;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.swing.DialogTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2018 The Cytoscape Consortium
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

/**
 * This class mediates the communication between the Annotations UI and the rest of Cytoscape.
 */
public class AnnotationMediator implements CyStartListener, CyShutdownListener, SessionAboutToBeLoadedListener,
		SessionLoadedListener, NetworkViewAddedListener, NetworkViewAboutToBeDestroyedListener,
		SetCurrentNetworkViewListener, AnnotationsAddedListener, AnnotationsRemovedListener, PropertyChangeListener,
		CytoPanelComponentSelectedListener {

	private final AnnotationMainPanel mainPanel;
	private final Map<String, AnnotationFactory<? extends Annotation>> factories = new LinkedHashMap<>();
	private boolean appStarted;
	private boolean loadingSession;
	
	private ClickToAddAnnotationListener clickToAddAnnotationListener;
	
	private final CyServiceRegistrar serviceRegistrar;
	private final Object lock = new Object();
	
	private static final Logger logger = LoggerFactory.getLogger(CyUserLog.NAME);
	
	public AnnotationMediator(AnnotationMainPanel mainPanel, CyServiceRegistrar serviceRegistrar) {
		super();
		this.mainPanel = mainPanel;
		this.serviceRegistrar = serviceRegistrar;
	}
	
	@Override
	public void handleEvent(CyStartEvent evt) {
		final HashSet<AnnotationFactory<? extends Annotation>> set = new LinkedHashSet<>(factories.values());
		
		invokeOnEDT(() -> {
			set.forEach(f -> addAnnotationButton(f));
			mainPanel.setEnabled(false);
			mainPanel.getRemoveAnnotationsButton().addActionListener(e -> removeSelectedAnnotations());
			mainPanel.getBackgroundTable().getSelectionModel().addListSelectionListener(e -> {
				mainPanel.updateSelectionButtons();
				
				if (!e.getValueIsAdjusting() && !mainPanel.getBackgroundTable().isEditing())
					selectAnnotationsFromSelectedRows();
			});
			mainPanel.getForegroundTable().getSelectionModel().addListSelectionListener(e -> {
				mainPanel.updateSelectionButtons();
				
				if (!e.getValueIsAdjusting() && !mainPanel.getForegroundTable().isEditing())
					selectAnnotationsFromSelectedRows();
			});
		});

		appStarted = true;
	}

	@Override
	public void handleEvent(CyShutdownEvent evt) {
		appStarted = false;
	}
	
	@Override
	public void handleEvent(SessionAboutToBeLoadedEvent evt) {
		loadingSession = true;
	}
	
	@Override
	public void handleEvent(SessionLoadedEvent evt) {
		loadingSession = false;
		disposeClickToAddAnnotationListener();
		
		final Set<CyNetworkView> allViews = serviceRegistrar.getService(CyNetworkViewManager.class).getNetworkViewSet();
		
		allViews.forEach(view -> {
			if (view instanceof DGraphView) {
				addPropertyListeners((DGraphView) view);
				addPropertyListeners(((DGraphView) view).getCyAnnotator().getAnnotations());
			}
		});
		
		final DGraphView view = getCurrentDGraphView();
		invokeOnEDT(() -> mainPanel.update(view));
	}

	@Override
	public void handleEvent(NetworkViewAddedEvent evt) {
		if (!appStarted || loadingSession)
			return;
		
		final CyNetworkView view = evt.getNetworkView() instanceof DGraphView ?
				(DGraphView) evt.getNetworkView() : null;
				
		if (view instanceof DGraphView) {
			addPropertyListeners((DGraphView) view);
			addPropertyListeners(((DGraphView) view).getCyAnnotator().getAnnotations());
		}
	}
	
	@Override
	public void handleEvent(NetworkViewAboutToBeDestroyedEvent evt) {
		CyNetworkView view = evt.getNetworkView();
		
		if (view instanceof DGraphView) {
			removePropertyListeners((DGraphView) view);
			removePropertyListeners(((DGraphView) view).getCyAnnotator().getAnnotations());
		}
	}
	
	@Override
	public void handleEvent(SetCurrentNetworkViewEvent evt) {
		if (appStarted && !loadingSession) {
			disposeClickToAddAnnotationListener();
			final DGraphView view = evt.getNetworkView() instanceof DGraphView ?
					(DGraphView) evt.getNetworkView() : null;

			invokeOnEDT(() -> mainPanel.update(view));
		}
	}
	
	@Override
	public void handleEvent(CytoPanelComponentSelectedEvent evt) {
		// When the panel component changes, disable all annotation buttons,
		// so the user doesn't add an annotation by accident
		if (appStarted && CytoPanelName.WEST == evt.getCytoPanel().getCytoPanelName())
			invokeOnEDT(() -> mainPanel.clearAnnotationButtonSelection());
	}

	@Override
	public void handleEvent(AnnotationsAddedEvent evt) {
//		if (appStarted && !loadingSession && evt.getSource().equals(getCurrentDGraphView()))
//			invokeOnEDT(() -> mainPanel.addAnnotations(evt.getPayloadCollection()));
	}
	
	@Override
	public void handleEvent(AnnotationsRemovedEvent evt) {
//		if (appStarted && !loadingSession && evt.getSource().equals(getCurrentDGraphView()))
//			invokeOnEDT(() -> mainPanel.removeAnnotations(evt.getPayloadCollection()));
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void propertyChange(PropertyChangeEvent evt) {
		if (!appStarted || loadingSession)
			return;
		
		DGraphView view = getCurrentDGraphView();
		CyAnnotator cyAnnotator = view != null ? view.getCyAnnotator() : null;
		
		Object source = evt.getSource();
		
		if (source.equals(cyAnnotator)) {
			if ("annotations".equals(evt.getPropertyName())) {
				// First remove property listeners from deleted annotations and add them to the new ones
				Set<Annotation> oldList = mainPanel.getAllAnnotations();
				List<Annotation> newList = cyAnnotator.getAnnotations();
				oldList.removeAll(newList);
				removePropertyListeners(oldList);
				addPropertyListeners((Collection<Annotation>) evt.getNewValue());
				// Now update the UI
				invokeOnEDT(() -> mainPanel.update(view));
			}
		} else if (source instanceof DingAnnotation) {
			if ("selected".equals(evt.getPropertyName())) {
				invokeOnEDT(() -> {
					if (view != null && view.equals(mainPanel.getDGraphView()))
						mainPanel.setSelected((DingAnnotation) source, (boolean) evt.getNewValue());
				});
			}
		}
	}
	
	public void addAnnotationFactory(AnnotationFactory<? extends Annotation> f, Map<?, ?> props) {
		if (f instanceof AbstractDingAnnotationFactory == false)
			return; // TODO For now, only DING annotations are supported!
		
		if (f instanceof GroupAnnotationFactory)
			return;
		
		synchronized (lock) {
			factories.put(f.getId(), f);
		}
		
		if (appStarted)
			invokeOnEDT(() -> addAnnotationButton(f));
	}
	
	public void removeAnnotationFactory(AnnotationFactory<? extends Annotation> f, Map<?, ?> props) {
		synchronized (lock) {
			if (factories.remove(f.getId()) != null && appStarted)
				invokeOnEDT(() -> mainPanel.removeAnnotationButton(f));
		}
	}
	
	private void addAnnotationButton(AnnotationFactory<? extends Annotation> f) {
		final JToggleButton btn = mainPanel.addAnnotationButton(f);
		btn.addActionListener(evt -> {
			disposeClickToAddAnnotationListener();
			
			if (btn.isSelected()) {
				DGraphView view = getCurrentDGraphView();
				
				if (view != null) {
					clickToAddAnnotationListener = new ClickToAddAnnotationListener(view, f);
					view.addMouseListener(clickToAddAnnotationListener);
				}
			}
		});
	}

	private void createAnnotation(DGraphView view, AnnotationFactory<? extends Annotation> f, Point point) {
		if (view == null || f instanceof AbstractDingAnnotationFactory == false)
			return; // TODO For now, only DING annotations are supported!
		
		TaskIterator iterator = new TaskIterator(new AddAnnotationTask(view, point, f));
		
		serviceRegistrar.getService(DialogTaskManager.class).execute(iterator, new TaskObserver() {
			@Override
			public void taskFinished(ObservableTask task) {
				// TODO
			}
			@Override
			public void allFinished(FinishStatus finishStatus) {
				// TODO Add to list?
			}
		});
	}

	private DGraphView getCurrentDGraphView() {
		CyNetworkView view = serviceRegistrar.getService(CyApplicationManager.class).getCurrentNetworkView();
		
		return view instanceof DGraphView ? (DGraphView) view : null;
	}
	
	private void selectAnnotationsFromSelectedRows() {
		final DGraphView view = mainPanel.getDGraphView();
		
		if (view == null || view.getCyAnnotator() == null)
			return;
		
		final List<Annotation> all = view.getCyAnnotator().getAnnotations();
		
		if (all != null && !all.isEmpty()) {
			final Collection<Annotation> selList = mainPanel.getSelectedAnnotations();
			all.forEach(a -> a.setSelected(selList.contains(a)));
		}
	}
	
	private void removeSelectedAnnotations() {
		final DGraphView view = mainPanel.getDGraphView();
		
		if (view == null)
			return;
		
		final Collection<Annotation> selList = mainPanel.getSelectedAnnotations();
		
		if (!selList.isEmpty())
			serviceRegistrar.getService(AnnotationManager.class).removeAnnotations(selList);
	}
	
	private void addPropertyListeners(DGraphView view) {
		if (view == null || view.getCyAnnotator() == null)
			return;
		
		removePropertyListeners(view);
		view.getCyAnnotator().addPropertyChangeListener("annotations", this);
	}
	
	private void removePropertyListeners(DGraphView view) {
		if (view == null || view.getCyAnnotator() == null)
			return;
		
		view.getCyAnnotator().removePropertyChangeListener("annotations", this);
	}
	
	private void addPropertyListeners(Collection<Annotation> list) {
		if (list != null)
			list.forEach(a -> {
				if (a instanceof DingAnnotation) {
					((DingAnnotation) a).removePropertyChangeListener("selected", this);
					((DingAnnotation) a).addPropertyChangeListener("selected", this);
				}
			});
	}
	
	private void removePropertyListeners(Collection<Annotation> list) {
		if (list != null)
			list.forEach(a -> {
				if (a instanceof DingAnnotation)
					((DingAnnotation) a).removePropertyChangeListener("selected", this);
			});
	}
	
	private void disposeClickToAddAnnotationListener() {
		invokeOnEDT(() -> {
			if (clickToAddAnnotationListener != null) {
				clickToAddAnnotationListener.getView().removeMouseListener(clickToAddAnnotationListener);
				clickToAddAnnotationListener = null;
			}
		});
	}
	
	private class ClickToAddAnnotationListener extends MouseAdapter {
		
		private final DGraphView view;
		private final AnnotationFactory<? extends Annotation> factory;

		public ClickToAddAnnotationListener(DGraphView view, AnnotationFactory<? extends Annotation> f) {
			this.view = view;
			this.factory = f;
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1)
				createAnnotation(view, factory, e.getPoint());
		}
		
		public DGraphView getView() {
			return view;
		}
		
		public AnnotationFactory<? extends Annotation> getFactory() {
			return factory;
		}
	}
}