/*
 * generated by Xtext
 */
package com.b2international.snowowl.snomed.ecl.validation;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.emf.ecore.EPackage;

public class AbstractEclValidator extends org.eclipse.xtext.validation.AbstractDeclarativeValidator {

	@Override
	protected List<EPackage> getEPackages() {
	    List<EPackage> result = new ArrayList<EPackage>();
	    result.add(com.b2international.snowowl.snomed.ecl.ecl.EclPackage.eINSTANCE);
		return result;
	}
}
