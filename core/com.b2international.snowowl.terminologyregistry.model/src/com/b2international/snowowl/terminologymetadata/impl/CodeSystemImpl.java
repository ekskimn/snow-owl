/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.terminologymetadata.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.internal.cdo.CDOObjectImpl;

import com.b2international.snowowl.terminologymetadata.CodeSystem;
import com.b2international.snowowl.terminologymetadata.CodeSystemVersionGroup;
import com.b2international.snowowl.terminologymetadata.TerminologymetadataPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Code System</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.b2international.snowowl.terminologymetadata.impl.CodeSystemImpl#getShortName <em>Short Name</em>}</li>
 *   <li>{@link com.b2international.snowowl.terminologymetadata.impl.CodeSystemImpl#getCodeSystemOID <em>Code System OID</em>}</li>
 *   <li>{@link com.b2international.snowowl.terminologymetadata.impl.CodeSystemImpl#getName <em>Name</em>}</li>
 *   <li>{@link com.b2international.snowowl.terminologymetadata.impl.CodeSystemImpl#getMaintainingOrganizationLink <em>Maintaining Organization Link</em>}</li>
 *   <li>{@link com.b2international.snowowl.terminologymetadata.impl.CodeSystemImpl#getLanguage <em>Language</em>}</li>
 *   <li>{@link com.b2international.snowowl.terminologymetadata.impl.CodeSystemImpl#getCitation <em>Citation</em>}</li>
 *   <li>{@link com.b2international.snowowl.terminologymetadata.impl.CodeSystemImpl#getIconPath <em>Icon Path</em>}</li>
 *   <li>{@link com.b2international.snowowl.terminologymetadata.impl.CodeSystemImpl#getTerminologyComponentId <em>Terminology Component Id</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public abstract class CodeSystemImpl extends CDOObjectImpl implements CodeSystem {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected CodeSystemImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return TerminologymetadataPackage.Literals.CODE_SYSTEM;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected int eStaticFeatureCount() {
		return 0;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getCodeSystemOID() {
		return (String)eGet(TerminologymetadataPackage.Literals.CODE_SYSTEM__CODE_SYSTEM_OID, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setCodeSystemOID(String newCodeSystemOID) {
		eSet(TerminologymetadataPackage.Literals.CODE_SYSTEM__CODE_SYSTEM_OID, newCodeSystemOID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getName() {
		return (String)eGet(TerminologymetadataPackage.Literals.CODE_SYSTEM__NAME, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setName(String newName) {
		eSet(TerminologymetadataPackage.Literals.CODE_SYSTEM__NAME, newName);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getShortName() {
		return (String)eGet(TerminologymetadataPackage.Literals.CODE_SYSTEM__SHORT_NAME, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setShortName(String newShortName) {
		eSet(TerminologymetadataPackage.Literals.CODE_SYSTEM__SHORT_NAME, newShortName);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getMaintainingOrganizationLink() {
		return (String)eGet(TerminologymetadataPackage.Literals.CODE_SYSTEM__MAINTAINING_ORGANIZATION_LINK, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setMaintainingOrganizationLink(String newMaintainingOrganizationLink) {
		eSet(TerminologymetadataPackage.Literals.CODE_SYSTEM__MAINTAINING_ORGANIZATION_LINK, newMaintainingOrganizationLink);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getLanguage() {
		return (String)eGet(TerminologymetadataPackage.Literals.CODE_SYSTEM__LANGUAGE, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setLanguage(String newLanguage) {
		eSet(TerminologymetadataPackage.Literals.CODE_SYSTEM__LANGUAGE, newLanguage);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getCitation() {
		return (String)eGet(TerminologymetadataPackage.Literals.CODE_SYSTEM__CITATION, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setCitation(String newCitation) {
		eSet(TerminologymetadataPackage.Literals.CODE_SYSTEM__CITATION, newCitation);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getIconPath() {
		return (String)eGet(TerminologymetadataPackage.Literals.CODE_SYSTEM__ICON_PATH, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setIconPath(String newIconPath) {
		eSet(TerminologymetadataPackage.Literals.CODE_SYSTEM__ICON_PATH, newIconPath);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getTerminologyComponentId() {
		return (String)eGet(TerminologymetadataPackage.Literals.CODE_SYSTEM__TERMINOLOGY_COMPONENT_ID, true);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTerminologyComponentId(String newTerminologyComponentId) {
		eSet(TerminologymetadataPackage.Literals.CODE_SYSTEM__TERMINOLOGY_COMPONENT_ID, newTerminologyComponentId);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated NOT
	 */
	public CodeSystemVersionGroup getCodeSystemVersionGroup() {
		return (CodeSystemVersionGroup) this.eContainer();
	}

} //CodeSystemImpl