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

package org.apache.taverna.component.ui.menu.profile;

import static org.apache.taverna.component.ui.menu.profile.ComponentProfileMenuSection.COMPONENT_PROFILE_SECTION;

import java.net.URI;

import javax.swing.Action;

import org.apache.taverna.component.api.ComponentFactory;
import org.apache.taverna.component.ui.preference.ComponentPreference;
import org.apache.taverna.component.ui.serviceprovider.ComponentServiceIcon;
import org.apache.taverna.ui.menu.AbstractMenuAction;

/**
 * @author alanrw
 */
public class ComponentProfileImportMenuAction extends AbstractMenuAction {
	private static final URI COMPONENT_PROFILE_IMPORT_URI = URI
			.create("http://taverna.sf.net/2008/t2workbench/menu#componentProfileImport");

	private ComponentFactory factory;
	private ComponentPreference prefs;
	private ComponentServiceIcon icon;

	public ComponentProfileImportMenuAction() {
		super(COMPONENT_PROFILE_SECTION, 200, COMPONENT_PROFILE_IMPORT_URI);
	}

	public void setComponentFactory(ComponentFactory factory) {
		this.factory = factory;
	}
	
	public void setIcon(ComponentServiceIcon icon) {
		this.icon = icon;
	}

	public void setPreferences(ComponentPreference prefs) {
		this.prefs = prefs;
	}

	@Override
	protected Action createAction() {
		return new ComponentProfileImportAction(factory, prefs, icon);
	}
}
