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
 *///
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.5 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.03.01 at 10:42:55 AM CET 
//


package org.ihtsdo.sct;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * Complex type that represents information about the source of this set of
 * changes.
 * 
 * <p>Java class for SifSource complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SifSource">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="path" type="{urn:ihtsdo-org/sct}SifPathBase" minOccurs="0"/>
 *         &lt;element name="startPosition" type="{urn:ihtsdo-org/sct}SifPosition" minOccurs="0"/>
 *         &lt;element name="organization" type="{urn:ihtsdo-org/sct}SifOrganization" minOccurs="0"/>
 *         &lt;element name="person" type="{urn:ihtsdo-org/sct}SifPerson" minOccurs="0"/>
 *         &lt;element name="software" type="{urn:ihtsdo-org/sct}SifSoftware" minOccurs="0"/>
 *         &lt;element name="environment" type="{urn:ihtsdo-org/sct}SifEnvironment" minOccurs="0"/>
 *         &lt;element name="property" type="{urn:ihtsdo-org/sct}SifPropertyPlus" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SifSource", propOrder = {
    "path",
    "startPosition",
    "organization",
    "person",
    "software",
    "environment",
    "property"
})
public class SifSource {

    protected SifPathBase path;
    protected SifPosition startPosition;
    protected SifOrganization organization;
    protected SifPerson person;
    protected SifSoftware software;
    protected SifEnvironment environment;
    protected List<SifPropertyPlus> property;

    /**
     * Gets the value of the path property.
     * 
     * @return
     *     possible object is
     *     {@link SifPathBase }
     *     
     */
    public SifPathBase getPath() {
        return path;
    }

    /**
     * Sets the value of the path property.
     * 
     * @param value
     *     allowed object is
     *     {@link SifPathBase }
     *     
     */
    public void setPath(SifPathBase value) {
        this.path = value;
    }

    /**
     * Gets the value of the startPosition property.
     * 
     * @return
     *     possible object is
     *     {@link SifPosition }
     *     
     */
    public SifPosition getStartPosition() {
        return startPosition;
    }

    /**
     * Sets the value of the startPosition property.
     * 
     * @param value
     *     allowed object is
     *     {@link SifPosition }
     *     
     */
    public void setStartPosition(SifPosition value) {
        this.startPosition = value;
    }

    /**
     * Gets the value of the organization property.
     * 
     * @return
     *     possible object is
     *     {@link SifOrganization }
     *     
     */
    public SifOrganization getOrganization() {
        return organization;
    }

    /**
     * Sets the value of the organization property.
     * 
     * @param value
     *     allowed object is
     *     {@link SifOrganization }
     *     
     */
    public void setOrganization(SifOrganization value) {
        this.organization = value;
    }

    /**
     * Gets the value of the person property.
     * 
     * @return
     *     possible object is
     *     {@link SifPerson }
     *     
     */
    public SifPerson getPerson() {
        return person;
    }

    /**
     * Sets the value of the person property.
     * 
     * @param value
     *     allowed object is
     *     {@link SifPerson }
     *     
     */
    public void setPerson(SifPerson value) {
        this.person = value;
    }

    /**
     * Gets the value of the software property.
     * 
     * @return
     *     possible object is
     *     {@link SifSoftware }
     *     
     */
    public SifSoftware getSoftware() {
        return software;
    }

    /**
     * Sets the value of the software property.
     * 
     * @param value
     *     allowed object is
     *     {@link SifSoftware }
     *     
     */
    public void setSoftware(SifSoftware value) {
        this.software = value;
    }

    /**
     * Gets the value of the environment property.
     * 
     * @return
     *     possible object is
     *     {@link SifEnvironment }
     *     
     */
    public SifEnvironment getEnvironment() {
        return environment;
    }

    /**
     * Sets the value of the environment property.
     * 
     * @param value
     *     allowed object is
     *     {@link SifEnvironment }
     *     
     */
    public void setEnvironment(SifEnvironment value) {
        this.environment = value;
    }

    /**
     * Gets the value of the property property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the property property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProperty().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SifPropertyPlus }
     * 
     * 
     */
    public List<SifPropertyPlus> getProperty() {
        if (property == null) {
            property = new ArrayList<SifPropertyPlus>();
        }
        return this.property;
    }

}