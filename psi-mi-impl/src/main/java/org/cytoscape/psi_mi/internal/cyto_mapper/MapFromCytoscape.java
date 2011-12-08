/*
  Copyright (c) 2006, The Cytoscape Consortium (www.cytoscape.org)

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
package org.cytoscape.psi_mi.internal.cyto_mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.psi_mi.internal.data_mapper.Mapper;
import org.cytoscape.psi_mi.internal.model.AttributeBag;
import org.cytoscape.psi_mi.internal.model.ExternalReference;
import org.cytoscape.psi_mi.internal.model.Interaction;
import org.cytoscape.psi_mi.internal.model.Interactor;
import org.cytoscape.psi_mi.internal.model.vocab.CommonVocab;
import org.cytoscape.psi_mi.internal.util.AttributeUtil;


/**
 * Maps Cytoscape Graph Objects to Data Service Interaction objects.
 * This class performs the inverse mapping of the MapToCytoscape
 * class.
 *
 * @author Ethan Cerami
 */
public class MapFromCytoscape implements Mapper {
	private CyNetwork cyNetwork;

	/**
	 * All new interactions.
	 */
	private ArrayList<Interaction> interactions;

	/**
	 * Constructor.
	 *
	 * @param cyNetwork GraphPerspective Object.
	 */
	public MapFromCytoscape(CyNetwork cyNetwork) {
		this.cyNetwork = cyNetwork;
	}

	/**
	 * Perform Mapping.
	 */
	public void doMapping() {
		interactions = new ArrayList<Interaction>();

		for (CyEdge edge : cyNetwork.getEdgeList()) {
			Interaction interaction = new Interaction();

			if (edge != null) {
				CyNode sourceNode = edge.getSource();
				CyNode targetNode = edge.getTarget();
				Interactor sourceInteractor = new Interactor();
				Interactor targetInteractor = new Interactor();
				transferNodeAttributes(cyNetwork, sourceNode, sourceInteractor);
				transferNodeAttributes(cyNetwork, targetNode, targetInteractor);

				ArrayList<Interactor> interactors = new ArrayList<Interactor>();
				interactors.add(sourceInteractor);
				interactors.add(targetInteractor);
				interaction.setInteractors(interactors);
				transferEdgeAttributes(cyNetwork, edge, interaction);
				interactions.add(interaction);
			}
		}
	}

	/**
	 * Gets an ArrayList of Interaction objects.
	 *
	 * @return ArrayList of Interaction objects.
	 */
	public ArrayList<Interaction> getInteractions() {
		return this.interactions;
	}

	/**
	 * Transfers all Edge Attributes from Cytoscape to Data Service Objects.
	 *
	 * @param edge        Cytoscape Edge.
	 * @param interaction Data Service Interaction Object.
	 */
	private void transferEdgeAttributes(CyNetwork net, CyEdge edge, Interaction interaction) {
		transferAllAttributes(net.getCyRow(edge), interaction);
	}

	/**
	 * Transfers all Node Attributes from Cytoscape to Data Service objects.
	 *
	 * @param node       Cytoscape Node.
	 * @param interactor Data Service Interactor object.
	 */
	private void transferNodeAttributes(CyNetwork net, CyNode node, Interactor interactor) {
		CyRow attributes = net.getCyRow(node);
		interactor.setName(attributes.get(AttributeUtil.NODE_NAME_ATTR_LABEL, String.class));
		transferAllAttributes(attributes, interactor);
	}

	/**
	 * Transfers all Node / Edge Attributes.
	 */
	@SuppressWarnings("unchecked")
	private void transferAllAttributes(CyRow attrs, AttributeBag bag) {
		List<String> dbNames = null;
		List<String> dbIds = null;

		for (Entry<String, Object> entry : attrs.getAllValues().entrySet()) {
			String attributeName = entry.getKey();
			Object value = entry.getValue();
			if (attributeName.equals(CommonVocab.XREF_DB_NAME)) {
				dbNames = (List<String>) value;
			} else if (attributeName.equals(CommonVocab.XREF_DB_ID)) {
				dbIds = (List<String>) value;
			} else {
				if (value instanceof String) {
					bag.addAttribute(attributeName, value);
				} // skip non-string attributes
			}
		}

		addExternalReferences(dbNames, dbIds, bag);
	}

	/**
	 * Adds External References.
	 */
	private void addExternalReferences(List<String> dbNames, List<String> dbIds, AttributeBag bag) {
		if ((dbNames != null) && (dbIds != null)) {
			ExternalReference[] refs = new ExternalReference[dbNames.size()];

			for (int i = 0; i < dbNames.size(); i++) {
				String dbName = dbNames.get(i);
				String dbId = dbIds.get(i);
				refs[i] = new ExternalReference(dbName, dbId);
			}

			bag.setExternalRefs(refs);
		}
	}
}
