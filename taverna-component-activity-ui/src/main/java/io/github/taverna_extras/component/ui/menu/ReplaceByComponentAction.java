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

package io.github.taverna_extras.component.ui.menu;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.SOUTH;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static io.github.taverna_extras.component.api.config.ComponentPropertyNames.COMPONENT_NAME;
import static io.github.taverna_extras.component.ui.ComponentActivityConfigurationBean.ignorableNames;
import static io.github.taverna_extras.component.ui.util.Utils.uniqueName;
import static org.apache.taverna.scufl2.api.common.Scufl2Tools.NESTED_WORKFLOW;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import io.github.taverna_extras.component.api.Component;
import io.github.taverna_extras.component.api.ComponentFactory;
import io.github.taverna_extras.component.api.Family;
import io.github.taverna_extras.component.api.Registry;
import io.github.taverna_extras.component.api.Version;
import io.github.taverna_extras.component.ui.ComponentActivityConfigurationBean;
import io.github.taverna_extras.component.ui.panel.ComponentChooserPanel;
import io.github.taverna_extras.component.ui.preference.ComponentPreference;
import io.github.taverna_extras.component.ui.serviceprovider.ComponentServiceIcon;
import io.github.taverna_extras.component.ui.util.Utils;

import org.apache.taverna.scufl2.api.activity.Activity;
import org.apache.taverna.scufl2.api.common.Scufl2Tools;
import org.apache.taverna.scufl2.api.core.Processor;
import org.apache.taverna.scufl2.api.core.Workflow;
import org.apache.taverna.scufl2.api.port.InputActivityPort;
import org.apache.taverna.scufl2.api.port.InputProcessorPort;
import org.apache.taverna.scufl2.api.port.OutputActivityPort;
import org.apache.taverna.scufl2.api.port.OutputProcessorPort;
import org.apache.taverna.workbench.edits.CompoundEdit;
import org.apache.taverna.workbench.edits.Edit;
import org.apache.taverna.workbench.edits.EditException;
import org.apache.taverna.workbench.edits.EditManager;
import org.apache.taverna.workbench.selection.SelectionManager;
import org.apache.taverna.workflow.edits.AddActivityEdit;
import org.apache.taverna.workflow.edits.AddActivityInputPortMappingEdit;
import org.apache.taverna.workflow.edits.AddActivityOutputPortMappingEdit;
import org.apache.taverna.workflow.edits.RemoveActivityEdit;
import org.apache.taverna.workflow.edits.RenameEdit;

/**
 * @author alanrw
 */
public class ReplaceByComponentAction extends AbstractAction {
	private static final long serialVersionUID = 7364648399658711574L;

	private final EditManager em;
	private final ComponentPreference prefs;
	private final SelectionManager sm;
	private final ComponentFactory factory;
	private final Scufl2Tools tools = new Scufl2Tools();

	private Processor selection;

	public ReplaceByComponentAction(ComponentPreference prefs,
			ComponentFactory factory, EditManager em, SelectionManager sm,
			ComponentServiceIcon icon) {
		super("Replace by component...", icon.getIcon());
		this.prefs = prefs;
		this.em = em;
		this.sm = sm;
		this.factory = factory;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JPanel overallPanel = new JPanel(new BorderLayout());
		ComponentChooserPanel panel = new ComponentChooserPanel(prefs);
		overallPanel.add(panel, CENTER);
		JPanel checkBoxPanel = new JPanel(new FlowLayout());
		JCheckBox replaceAllCheckBox = new JCheckBox(
				"Replace all matching services");
		checkBoxPanel.add(replaceAllCheckBox);
		checkBoxPanel.add(new JSeparator());
		JCheckBox renameServicesCheckBox = new JCheckBox("Rename service(s)");
		checkBoxPanel.add(renameServicesCheckBox);
		renameServicesCheckBox.setSelected(true);
		overallPanel.add(checkBoxPanel, SOUTH);
		int answer = showConfirmDialog(null, overallPanel, "Component choice",
				OK_CANCEL_OPTION);
		if (answer == OK_OPTION)
			doReplace(panel.getChosenRegistry(), panel.getChosenFamily(),
					replaceAllCheckBox.isSelected(),
					renameServicesCheckBox.isSelected(),
					panel.getChosenComponent());
	}

	private void doReplace(Registry chosenRegistry, Family chosenFamily,
			boolean replaceAll, boolean rename, Component chosenComponent) {
		Version chosenVersion = chosenComponent.getComponentVersionMap().get(
				chosenComponent.getComponentVersionMap().lastKey());
		Version.ID ident = new Version.Identifier(
				chosenRegistry.getRegistryBase(), chosenFamily.getName(),
				chosenComponent.getName(), chosenVersion.getVersionNumber());

		ComponentActivityConfigurationBean cacb = new ComponentActivityConfigurationBean(
				ident, factory);

		try {
			if (replaceAll) {
				Activity baseActivity = selection.getActivity(sm
						.getSelectedProfile());
				URI activityType = baseActivity.getType();
				String configString = getConfigString(baseActivity);

				replaceAllMatchingActivities(activityType, cacb, configString,
						rename, sm.getSelectedWorkflow());
			} else
				replaceActivity(cacb, selection, rename,
						sm.getSelectedWorkflow());
		} catch (Exception e) {
			showMessageDialog(
					null,
					"Failed to replace nested workflow with component: "
							+ e.getMessage(), "Component Problem",
					ERROR_MESSAGE);
		}
	}

	private String getConfigString(Activity baseActivity) {
		return baseActivity.getConfiguration().getJsonAsString();
	}

	private void replaceAllMatchingActivities(URI activityType,
			ComponentActivityConfigurationBean cacb, String configString,
			boolean rename, Workflow d) throws IntermediateException {
		for (Processor p : d.getProcessors()) {
			Activity a = p.getActivity(sm.getSelectedProfile());
			if (a.getType().equals(activityType)
					&& getConfigString(a).equals(configString))
				replaceActivity(cacb, p, rename, d);
			else if (a.getType().equals(NESTED_WORKFLOW))
				replaceAllMatchingActivities(activityType, cacb, configString,
						rename,
						tools.nestedWorkflowForProcessor(p, a.getParent()));
		}
	}

	private void replaceActivity(ComponentActivityConfigurationBean cacb,
			Processor p, boolean rename, Workflow d) throws IntermediateException {
		final Activity originalActivity = p.getActivity(sm.getSelectedProfile());
		final List<Edit<?>> currentWorkflowEditList = new ArrayList<>();
				
		Activity replacementActivity = new Activity();
		try {
			URI configType;
			replacementActivity.createConfiguration(configType);
			
			replacementActivity.configure(cacb);
			//FIXME
		} catch (Exception e) {
			throw new IntermediateException(
					"Unable to configure component", e);
		}
		if (originalActivity.getInputPorts().size() != replacementActivity
				.getInputPorts().size())
			throw new IntermediateException(
					"Component does not have matching ports", null);

		int replacementOutputSize = replacementActivity.getOutputPorts().size();
		int originalOutputSize = originalActivity.getOutputPorts().size();
		for (String name : ignorableNames) {
			if (originalActivity.getOutputPorts().getByName(name) != null)
				originalOutputSize--;
			if (replacementActivity.getOutputPorts().getByName(name) != null)
				replacementOutputSize--;
		}

		int sizeDifference = replacementOutputSize - originalOutputSize;
		if (sizeDifference != 0)
			throw new IntermediateException(
					"Component does not have matching ports", null);

		for (InputActivityPort aip : originalActivity.getInputPorts()) {
			String aipName = aip.getName();
			int aipDepth = aip.getDepth();
			InputActivityPort caip = replacementActivity.getInputPorts().getByName(aipName);
			if ((caip == null) || (caip.getDepth() != aipDepth))
				throw new RuntimeException("Original input port "
						+ aipName + " is not matched");
		}
		for (OutputActivityPort aop : originalActivity.getOutputPorts()) {
			String aopName = aop.getName();
			int aopDepth = aop.getDepth();
			OutputActivityPort caop = replacementActivity.getOutputPorts().getByName(aopName);
			if ((caop == null || aopDepth != caop.getDepth())
					&& !ignorableNames.contains(aopName))
				throw new IntermediateException(
						"Original output port " + aopName + " is not matched", null);
		}

		for (InputProcessorPort pip : p.getInputPorts()) {
			InputActivityPort iap = replacementActivity.getInputPorts()
					.getByName(pip.getName());
			if (iap == null)
				iap = new InputActivityPort(replacementActivity, pip.getName());
			currentWorkflowEditList.add(new AddActivityInputPortMappingEdit(
					replacementActivity, pip, iap));
		}

		for (OutputProcessorPort pop : p.getOutputPorts()) {
			OutputActivityPort oap = replacementActivity.getOutputPorts()
					.getByName(pop.getName());
			if (oap == null)
				oap = new OutputActivityPort(replacementActivity, pop.getName());
			currentWorkflowEditList.add(new AddActivityOutputPortMappingEdit(
					replacementActivity, pop, oap));
		}

		currentWorkflowEditList
				.add(new AddActivityEdit(p, replacementActivity));
		currentWorkflowEditList
				.add(new RemoveActivityEdit(p, originalActivity));
		
		if (rename) {
			String possibleName = replacementActivity.getConfiguration()
					.getJsonAsObjectNode().get(COMPONENT_NAME).textValue();
			currentWorkflowEditList.add(new RenameEdit<>(p, uniqueName(
					possibleName, d.getProcessors())));
		}
		try {
			em.doDataflowEdit(d.getParent(), new CompoundEdit(
					currentWorkflowEditList));
		} catch (EditException e) {
			throw new IntermediateException(
					"Unable to replace with component", e);
		}
	}

	public void setSelection(Processor selection) {
		this.selection = selection;
	}

	@SuppressWarnings("serial")
	private static class IntermediateException extends Exception {
		IntermediateException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}
}
