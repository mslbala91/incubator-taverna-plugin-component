/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package io.github.taverna_extras.component.ui.panel;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.WEST;
import static java.awt.event.ItemEvent.SELECTED;
import static org.apache.log4j.Logger.getLogger;
import static io.github.taverna_extras.component.ui.util.Utils.SHORT_STRING;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.apache.log4j.Logger;
import io.github.taverna_extras.component.api.Component;
import io.github.taverna_extras.component.api.Family;
import io.github.taverna_extras.component.api.Registry;
import io.github.taverna_extras.component.api.Version;
import io.github.taverna_extras.component.ui.preference.ComponentPreference;
import org.apache.taverna.lang.observer.Observable;
import org.apache.taverna.lang.observer.Observer;

/**
 * @author alanrw
 */
public class ComponentVersionChooserPanel extends JPanel implements
		Observer<ComponentChoiceMessage> {
	private static final long serialVersionUID = 5125907010496468219L;
	private static Logger logger = getLogger(ComponentVersionChooserPanel.class);

	private final JComboBox<String> componentVersionChoice;
	private final SortedMap<Integer, Version> componentVersionMap;
	private final ComponentChooserPanel componentChooserPanel;

	public ComponentVersionChooserPanel(ComponentPreference prefs) {
		super(new GridBagLayout());
		componentVersionMap = new TreeMap<>();
		componentChooserPanel = new ComponentChooserPanel(prefs);
		componentVersionChoice = new JComboBox<>();
		componentVersionChoice.setPrototypeDisplayValue(SHORT_STRING);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.anchor = WEST;
		gbc.fill = HORIZONTAL;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		add(componentChooserPanel, gbc);
		componentChooserPanel.addObserver(this);

		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.fill = NONE;
		add(new JLabel("Component version:"), gbc);
		gbc.gridx = 1;
		gbc.weightx = 1;
		add(componentVersionChoice, gbc);
		componentVersionChoice.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange() == SELECTED)
					updateToolTipText();
			}
		});
	}

	protected void updateToolTipText() {
		Version chosenComponentVersion = getChosenComponentVersion();
		componentVersionChoice
				.setToolTipText(chosenComponentVersion == null ? null
						: chosenComponentVersion.getDescription());
	}

	private void updateComponentVersionModel() {
		componentVersionMap.clear();
		componentVersionChoice.removeAllItems();
		componentVersionChoice.setToolTipText(null);
		componentVersionChoice.addItem("Reading component versions");
		componentVersionChoice.setEnabled(false);
		new ComponentVersionUpdater().execute();
	}

	public Version getChosenComponentVersion() {
		if (componentVersionMap.isEmpty())
			return null;
		try {
			return componentVersionMap.get(new Integer(componentVersionChoice
					.getSelectedItem().toString()));
		} catch (NumberFormatException nfe) {
			// Not a number, no version chosen
			return null;
		}
	}

	@Override
	public void notify(Observable<ComponentChoiceMessage> sender,
			ComponentChoiceMessage message) {
		try {
			updateComponentVersionModel();
		} catch (RuntimeException e) {
			logger.error("problem updating view from component version", e);
		}
	}

	public Registry getChosenRegistry() {
		return componentChooserPanel.getChosenRegistry();
	}

	public Family getChosenFamily() {
		return componentChooserPanel.getChosenFamily();
	}

	public Component getChosenComponent() {
		return componentChooserPanel.getChosenComponent();
	}

	private class ComponentVersionUpdater extends SwingWorker<String, Object> {
		@Override
		protected String doInBackground() throws Exception {
			Component chosenComponent = componentChooserPanel
					.getChosenComponent();
			if (chosenComponent != null)
				for (Version version : chosenComponent.getComponentVersionMap()
						.values()) {
					Integer versionNumber = version.getVersionNumber();
					componentVersionMap.put(versionNumber, version);
				}
			return null;
		}

		@Override
		protected void done() {
			componentVersionChoice.removeAllItems();
			try {
				get();
				for (Integer versionNumber : componentVersionMap.keySet())
					componentVersionChoice.addItem(versionNumber.toString());

				if (!componentVersionMap.isEmpty()) {
					componentVersionChoice.setSelectedItem(componentVersionMap
							.lastKey());
					updateToolTipText();
				} else
					componentVersionChoice.addItem("No versions available");
			} catch (InterruptedException | ExecutionException e) {
				componentVersionChoice.addItem("Unable to read versions");
				logger.error(e);
			}

			componentVersionChoice.setEnabled(!componentVersionMap.isEmpty());
		}
	}

	public ComponentChooserPanel getComponentChooserPanel() {
		return componentChooserPanel;
	}
}
