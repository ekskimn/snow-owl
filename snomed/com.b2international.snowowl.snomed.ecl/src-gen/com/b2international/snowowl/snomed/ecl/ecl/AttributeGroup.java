/**
 */
package com.b2international.snowowl.snomed.ecl.ecl;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Attribute Group</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.b2international.snowowl.snomed.ecl.ecl.AttributeGroup#getCardinality <em>Cardinality</em>}</li>
 *   <li>{@link com.b2international.snowowl.snomed.ecl.ecl.AttributeGroup#getRefinement <em>Refinement</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.b2international.snowowl.snomed.ecl.ecl.EclPackage#getAttributeGroup()
 * @model
 * @generated
 */
public interface AttributeGroup extends Refinement
{
  /**
   * Returns the value of the '<em><b>Cardinality</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Cardinality</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Cardinality</em>' containment reference.
   * @see #setCardinality(Cardinality)
   * @see com.b2international.snowowl.snomed.ecl.ecl.EclPackage#getAttributeGroup_Cardinality()
   * @model containment="true"
   * @generated
   */
  Cardinality getCardinality();

  /**
   * Sets the value of the '{@link com.b2international.snowowl.snomed.ecl.ecl.AttributeGroup#getCardinality <em>Cardinality</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Cardinality</em>' containment reference.
   * @see #getCardinality()
   * @generated
   */
  void setCardinality(Cardinality value);

  /**
   * Returns the value of the '<em><b>Refinement</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Refinement</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Refinement</em>' containment reference.
   * @see #setRefinement(Refinement)
   * @see com.b2international.snowowl.snomed.ecl.ecl.EclPackage#getAttributeGroup_Refinement()
   * @model containment="true"
   * @generated
   */
  Refinement getRefinement();

  /**
   * Sets the value of the '{@link com.b2international.snowowl.snomed.ecl.ecl.AttributeGroup#getRefinement <em>Refinement</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Refinement</em>' containment reference.
   * @see #getRefinement()
   * @generated
   */
  void setRefinement(Refinement value);

} // AttributeGroup
