package com.b2international.snowowl.snomed.api.impl;

import java.util.List;

import javax.annotation.Resource;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.ApplicationContext;
import com.b2international.snowowl.core.api.IBranchPath;
import com.b2international.snowowl.datastore.BranchPathUtils;
import com.b2international.snowowl.datastore.browser.BranchSpecificClientTerminologyBrowser;
import com.b2international.snowowl.dsl.SCGStandaloneSetup;
import com.b2international.snowowl.dsl.scg.Expression;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.semanticengine.normalform.ScgExpressionNormalFormGenerator;
import com.b2international.snowowl.snomed.api.ISnomedExpressionService;
import com.b2international.snowowl.snomed.api.domain.expression.ISnomedExpression;
import com.b2international.snowowl.snomed.datastore.RecursiveTerminologyBrowser;
import com.b2international.snowowl.snomed.datastore.SnomedClientStatementBrowser;
import com.b2international.snowowl.snomed.datastore.SnomedStatementBrowser;
import com.b2international.snowowl.snomed.datastore.SnomedTerminologyBrowser;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptIndexEntry;

public class SnomedExpressionService implements ISnomedExpressionService {

	@Resource
	private IEventBus bus;

	@Override
	public ISnomedExpression getConceptShortNormalForm(String conceptId, String branchPath, List<ExtendedLocale> extendedLocales, boolean normaliseAttributeValues) {
		
		IBranchPath iBranchPath = BranchPathUtils.createPath(branchPath);
		
		BranchSpecificClientTerminologyBrowser<SnomedConceptIndexEntry, String> browser = 
				new BranchSpecificClientTerminologyBrowser<>(getTerminologyBrowser(), iBranchPath);
		RecursiveTerminologyBrowser<SnomedConceptIndexEntry, String> recursiveTerminologyBrowser = RecursiveTerminologyBrowser.create(browser);
		final ScgExpressionNormalFormGenerator scgExpressionNormalFormGenerator = 
				new ScgExpressionNormalFormGenerator(recursiveTerminologyBrowser, new SnomedClientStatementBrowser(getStatementBrowser()));
		Expression expression = (Expression) SCGStandaloneSetup.parse(conceptId.toString());
		final Expression shortNormalForm = scgExpressionNormalFormGenerator.getShortNormalForm(expression, normaliseAttributeValues);
		
		return new SnomedExpresssionConverter(browser, extendedLocales, new DescriptionService(bus, branchPath)).convert(shortNormalForm);
	}

	private static SnomedTerminologyBrowser getTerminologyBrowser() {
		return ApplicationContext.getServiceForClass(SnomedTerminologyBrowser.class);
	}

	private static SnomedStatementBrowser getStatementBrowser() {
		return ApplicationContext.getServiceForClass(SnomedStatementBrowser.class);
	}
}
