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

package org.apache.taverna.component.ui.annotation;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createTypedLiteral;
import static java.lang.Integer.MIN_VALUE;
import static org.apache.taverna.component.ui.annotation.SemanticAnnotationUtils.getObjectName;

import javax.swing.JComponent;
import javax.swing.JTextArea;

import org.apache.taverna.component.api.profile.SemanticAnnotationProfile;

import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * 
 * 
 * @author Alan Williams
 */
public class AnnotationPropertyPanelFactory extends PropertyPanelFactorySPI {
	@Override
	public JComponent getInputComponent(
			SemanticAnnotationProfile semanticAnnotationProfile,
			Statement statement) {
		return getDefaultInputComponent(semanticAnnotationProfile, statement);
	}

	@Override
	public RDFNode getNewTargetNode(Statement originalStatement,
			JComponent component) {
		String newText = ((JTextArea) component).getText();
		if ((originalStatement == null)
				|| !getObjectName(originalStatement).equals(newText))
			return createTypedLiteral(newText);
		return null;
	}

	@Override
	public int getRatingForSemanticAnnotation(
			SemanticAnnotationProfile semanticAnnotationProfile) {
		OntProperty property = semanticAnnotationProfile.getPredicate();
		if ((property != null) && property.isAnnotationProperty())
			return 100;
		return MIN_VALUE;
	}

	@Override
	public JComponent getDisplayComponent(
			SemanticAnnotationProfile semanticAnnotationProfile,
			Statement statement) {
		return getDefaultDisplayComponent(semanticAnnotationProfile, statement);
	}
}
