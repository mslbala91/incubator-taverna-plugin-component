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

package io.github.taverna_extras.component.ui.menu.component;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.WEST;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static org.apache.log4j.Logger.getLogger;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;
import io.github.taverna_extras.component.api.Component;
import io.github.taverna_extras.component.api.ComponentException;
import io.github.taverna_extras.component.api.Family;
import io.github.taverna_extras.component.api.Version;
import io.github.taverna_extras.component.api.profile.Profile;
import io.github.taverna_extras.component.ui.panel.ComponentChoiceMessage;
import io.github.taverna_extras.component.ui.panel.ComponentChooserPanel;
import io.github.taverna_extras.component.ui.panel.ProfileChoiceMessage;
import io.github.taverna_extras.component.ui.panel.RegistryAndFamilyChooserPanel;
import io.github.taverna_extras.component.ui.preference.ComponentPreference;
import io.github.taverna_extras.component.ui.serviceprovider.ComponentServiceIcon;
import io.github.taverna_extras.component.ui.serviceprovider.ComponentServiceProviderConfig;
import io.github.taverna_extras.component.ui.util.Utils;
import org.apache.taverna.lang.observer.Observable;
import org.apache.taverna.lang.observer.Observer;

/**
 * @author alanrw
 */
public class ComponentCopyAction extends AbstractAction {
	private static final long serialVersionUID = -4440978712410081685L;
	private static final Logger logger = getLogger(ComponentCopyAction.class);
	private static final String COPY_COMPONENT = "Copy component...";

	private final ComponentPreference prefs;
	private final Utils utils;

	public ComponentCopyAction(ComponentPreference pref, ComponentServiceIcon icon, Utils utils) {
		super(COPY_COMPONENT, icon.getIcon());
		this.prefs = pref;
		this.utils = utils;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		JPanel overallPanel = new JPanel();
		overallPanel.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		ComponentChooserPanel source = new ComponentChooserPanel(prefs);
		source.setBorder(new TitledBorder("Source component"));

		gbc.insets = new Insets(0, 5, 0, 5);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = WEST;
		gbc.fill = BOTH;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		overallPanel.add(source, gbc);

		final RegistryAndFamilyChooserPanel target = new RegistryAndFamilyChooserPanel(prefs);
		target.setBorder(new TitledBorder("Target family"));
		gbc.gridy++;
		overallPanel.add(target, gbc);

		source.addObserver(new Observer<ComponentChoiceMessage>() {
			@Override
			public void notify(Observable<ComponentChoiceMessage> sender,
					ComponentChoiceMessage message) throws Exception {
				Profile componentProfile = null;
				Family componentFamily = message.getComponentFamily();
				if (componentFamily != null)
					componentProfile = componentFamily.getComponentProfile();
				ProfileChoiceMessage profileMessage = new ProfileChoiceMessage(
						componentProfile);
				target.notify(null, profileMessage);
			}
		});

		int answer = showConfirmDialog(null, overallPanel, "Copy Component",
				OK_CANCEL_OPTION);
		if (answer == OK_OPTION)
			doCopy(source.getChosenComponent(), target.getChosenFamily());
	}

	private void doCopy(Component sourceComponent, Family targetFamily) {
		if (sourceComponent == null) {
			showMessageDialog(null, "Unable to determine source component",
					"Component Copy Problem", ERROR_MESSAGE);
			return;
		} else if (targetFamily == null) {
			showMessageDialog(null, "Unable to determine target family",
					"Component Copy Problem", ERROR_MESSAGE);
			return;
		}

		try {
			String componentName = sourceComponent.getName();
			boolean alreadyUsed = targetFamily.getComponent(componentName) != null;
			if (alreadyUsed)
				showMessageDialog(null, componentName + " is already used",
						"Duplicate component name", ERROR_MESSAGE);
			else {
				Version targetVersion = doCopy(sourceComponent, targetFamily,
						componentName);
				try {
					utils.refreshComponentServiceProvider(new ComponentServiceProviderConfig(
							targetVersion.getID()).getConfiguration());
				} catch (Exception e) {
					logger.error(e);
				}
			}
		} catch (ComponentException e) {
			logger.error("failed to copy component", e);
			showMessageDialog(null,
					"Unable to create component: " + e.getMessage(),
					"Component Copy Problem", ERROR_MESSAGE);
		}
	}

	private Version doCopy(Component sourceComponent, Family targetFamily,
			String componentName) throws ComponentException {
		return targetFamily
				.createComponentBasedOn(
						componentName,
						sourceComponent.getDescription(),
						sourceComponent
								.getComponentVersionMap()
								.get(sourceComponent.getComponentVersionMap()
										.lastKey()).getImplementation());
	}
}
