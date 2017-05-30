package com.b2international.snowowl.snomed.api.impl.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.ihtsdo.drools.RuleExecutor;
import org.ihtsdo.drools.exception.BadRequestRuleExecutorException;
import org.ihtsdo.drools.response.InvalidContent;

import com.b2international.commons.http.ExtendedLocale;
import com.b2international.snowowl.core.branch.Branch;
import com.b2international.snowowl.core.exceptions.BadRequestException;
import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.snomed.api.domain.browser.ISnomedBrowserConcept;
import com.b2international.snowowl.snomed.api.impl.DescriptionService;
import com.b2international.snowowl.snomed.api.impl.validation.domain.ValidationConcept;
import com.b2international.snowowl.snomed.api.impl.validation.service.ValidationConceptService;
import com.b2international.snowowl.snomed.api.impl.validation.service.ValidationDescriptionService;
import com.b2international.snowowl.snomed.api.impl.validation.service.ValidationRelationshipService;
import com.b2international.snowowl.snomed.api.validation.ISnomedBrowserValidationService;
import com.b2international.snowowl.snomed.api.validation.ISnomedInvalidContent;
import com.b2international.snowowl.snomed.core.domain.BranchMetadataResolver;
import com.b2international.snowowl.snomed.datastore.config.SnomedCoreConfiguration;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class SnomedBrowserValidationService implements ISnomedBrowserValidationService {

	@Resource
	private IEventBus bus;

	private RuleExecutor ruleExecutor;

	public SnomedBrowserValidationService() {
		ruleExecutor = newRuleExecutor();
	}

	@Override
	public List<ISnomedInvalidContent> validateConcept(String branchPath, ISnomedBrowserConcept browserConcept, List<ExtendedLocale> locales) {
		final Branch branch = SnomedRequests.branching().prepareGet(branchPath).executeSync(bus);
		final String assertionGroupNamesString = BranchMetadataResolver.getEffectiveBranchMetadataValue(branch, SnomedCoreConfiguration.BRANCH_ASSERTION_GROUP_NAMES_KEY);
		final Set<String> assertionGroupNames = new HashSet<String>();
		if (Strings.isNullOrEmpty(assertionGroupNamesString)) {
			throw new BadRequestException("No assertion groups configured for this branch.");
		}
		assertionGroupNames.addAll(Arrays.asList(assertionGroupNamesString.split("\\,")));
		DescriptionService descriptionService = new DescriptionService(bus, branchPath);
		
		ValidationConceptService validationConceptService = new ValidationConceptService(branchPath, bus);
		ValidationDescriptionService validationDescriptionService = new ValidationDescriptionService(descriptionService, branchPath, bus);
		ValidationRelationshipService validationRelationshipService = new ValidationRelationshipService(branchPath, bus);
		try {
			List<InvalidContent> list = ruleExecutor.execute(assertionGroupNames, new ValidationConcept(browserConcept), 
					validationConceptService, validationDescriptionService, validationRelationshipService, false, false);
			List<ISnomedInvalidContent> invalidContent = Lists.transform(list, new Function<InvalidContent, ISnomedInvalidContent>() {
				@Override
				public ISnomedInvalidContent apply(InvalidContent input) {
					return new SnomedInvalidContent(input);
				}
			});
			return invalidContent;
		} catch (BadRequestRuleExecutorException e) {
			throw new BadRequestException(e.getMessage());
		}
	}

	@Override
	public int reloadRules() {
		ruleExecutor = newRuleExecutor();
		return ruleExecutor.getTotalRulesLoaded();
	}

	private RuleExecutor newRuleExecutor() {
		// TODO: Move path to configuration
		return new RuleExecutor("/opt/termserver/snomed-drools-rules");
	}

}
