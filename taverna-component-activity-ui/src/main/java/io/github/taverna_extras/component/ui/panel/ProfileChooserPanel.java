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

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.WEST;
import static java.awt.event.ItemEvent.SELECTED;
import static org.apache.log4j.Logger.getLogger;
import static io.github.taverna_extras.component.ui.util.Utils.LONG_STRING;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.apache.log4j.Logger;
import io.github.taverna_extras.component.api.ComponentException;
import io.github.taverna_extras.component.api.Registry;
import io.github.taverna_extras.component.api.profile.Profile;
import org.apache.taverna.lang.observer.Observable;
import org.apache.taverna.lang.observer.Observer;

/**
 * @author alanrw
 */
public class ProfileChooserPanel extends JPanel implements
		Observer<RegistryChoiceMessage>, Observable<ProfileChoiceMessage> {
	private static final String READING_MSG = "Reading profiles";
	private static final String PROFILE_LABEL = "Profile:";
	private static final long serialVersionUID = 2175274929391537032L;
	private static Logger logger = getLogger(ProfileChooserPanel.class);

	private final List<Observer<ProfileChoiceMessage>> observers = new ArrayList<>();
	private final JComboBox<String> profileBox = new JComboBox<>();
	private final SortedMap<String, Profile> profileMap = new TreeMap<>();

	private Registry registry;

	public ProfileChooserPanel(RegistryChooserPanel registryPanel) {
		this();
		registryPanel.addObserver(new Observer<RegistryChoiceMessage>() {
			@Override
			public void notify(Observable<RegistryChoiceMessage> sender,
					RegistryChoiceMessage message) throws Exception {
				try {
					registry = message.getChosenRegistry();
					updateProfileModel();
				} catch (Exception e) {
					logger.error("failure when notifying about chosen registry", e);
				}
			}
		});
	}

	public ProfileChooserPanel() {
		super();
		profileBox.setPrototypeDisplayValue(LONG_STRING);
		this.setLayout(new GridBagLayout());

		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = WEST;
		gbc.fill = NONE;
		this.add(new JLabel(PROFILE_LABEL), gbc);
		gbc.gridx = 1;
		gbc.weightx = 1;
		gbc.fill = BOTH;
		this.add(profileBox, gbc);
		profileBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange() == SELECTED)
					setProfile(profileMap.get(profileBox.getSelectedItem()));
			}
		});

		profileBox.setEditable(false);
	}

	@Override
	public void notify(Observable<RegistryChoiceMessage> sender,
			RegistryChoiceMessage message) throws Exception {
		try {
			registry = message.getChosenRegistry();
			updateProfileModel();
		} catch (Exception e) {
			logger.error("failure when notifying about chosen registry", e);
		}
	}

	private void updateProfileModel() {
		profileMap.clear();
		profileBox.removeAllItems();
		profileBox.setToolTipText(null);
		profileBox.addItem(READING_MSG);
		profileBox.setEnabled(false);
		new ProfileUpdater().execute();
	}

	private void setProfile(Profile componentProfile) {
		if (componentProfile != null)
			profileBox.setToolTipText(componentProfile.getDescription());
		else
			profileBox.setToolTipText(null);

		Profile chosenProfile = getChosenProfile();
		ProfileChoiceMessage message = new ProfileChoiceMessage(chosenProfile);
		for (Observer<ProfileChoiceMessage> o : getObservers())
			try {
				o.notify(this, message);
			} catch (Exception e) {
				logger.error("failure when notifying about profile choice", e);
			}
	}

	public Profile getChosenProfile() {
		if (profileBox.getSelectedIndex() < 0)
			return null;

		return profileMap.get(profileBox.getSelectedItem());
	}

	private class ProfileUpdater extends SwingWorker<String, Object> {
		@Override
		protected String doInBackground() throws Exception {
			if (registry == null)
				return null;
			List<Profile> componentProfiles;
			try {
				componentProfiles = registry.getComponentProfiles();
			} catch (ComponentException e) {
				logger.error("failed to get profiles", e);
				throw e;
			} catch (NullPointerException e) {
				logger.error("failed to get profiles", e);
				throw e;
			}
			for (Profile profile : componentProfiles)
				try {
					profileMap.put(profile.getName(), profile);
				} catch (NullPointerException e) {
					logger.error("failure getting profile name", e);
				}

			return null;
		}

		@Override
		protected void done() {
			profileBox.removeAllItems();
			try {
				get();
				for (String name : profileMap.keySet())
					profileBox.addItem(name);
				if (!profileMap.isEmpty()) {
					String firstKey = profileMap.firstKey();
					profileBox.setSelectedItem(firstKey);
					setProfile(profileMap.get(firstKey));
				} else
					profileBox.addItem("No profiles available");
			} catch (InterruptedException | ExecutionException e) {
				logger.error(e);
				profileBox.addItem("Unable to read profiles");
			}
			profileBox.setEnabled(!profileMap.isEmpty());
		}
	}

	@Override
	public void addObserver(Observer<ProfileChoiceMessage> observer) {
		observers.add(observer);
		ProfileChoiceMessage message = new ProfileChoiceMessage(getChosenProfile());
		try {
			observer.notify(this, message);
		} catch (Exception e) {
			logger.error("failure when notifying about profile choice", e);
		}
	}

	@Override
	public void removeObserver(Observer<ProfileChoiceMessage> observer) {
		observers.remove(observer);
	}

	@Override
	public List<Observer<ProfileChoiceMessage>> getObservers() {
		return observers;
	}
}
