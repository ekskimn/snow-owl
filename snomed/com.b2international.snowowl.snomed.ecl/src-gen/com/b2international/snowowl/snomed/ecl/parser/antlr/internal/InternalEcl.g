/*
 * generated by Xtext
 */
grammar InternalEcl;

options {
	superClass=AbstractInternalAntlrParser;
	
}

@lexer::header {
package com.b2international.snowowl.snomed.ecl.parser.antlr.internal;

// Hack: Use our own Lexer superclass by means of import. 
// Currently there is no other way to specify the superclass for the lexer.
import org.eclipse.xtext.parser.antlr.Lexer;
}

@parser::header {
package com.b2international.snowowl.snomed.ecl.parser.antlr.internal; 

import org.eclipse.xtext.*;
import org.eclipse.xtext.parser.*;
import org.eclipse.xtext.parser.impl.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.parser.antlr.AbstractInternalAntlrParser;
import org.eclipse.xtext.parser.antlr.XtextTokenStream;
import org.eclipse.xtext.parser.antlr.XtextTokenStream.HiddenTokens;
import org.eclipse.xtext.parser.antlr.AntlrDatatypeRuleToken;
import com.b2international.snowowl.snomed.ecl.services.EclGrammarAccess;

}

@parser::members {

 	private EclGrammarAccess grammarAccess;
 	
    public InternalEclParser(TokenStream input, EclGrammarAccess grammarAccess) {
        this(input);
        this.grammarAccess = grammarAccess;
        registerRules(grammarAccess.getGrammar());
    }
    
    @Override
    protected String getFirstRuleName() {
    	return "ExpressionConstraint";	
   	}
   	
   	@Override
   	protected EclGrammarAccess getGrammarAccess() {
   		return grammarAccess;
   	}
}

@rulecatch { 
    catch (RecognitionException re) { 
        recover(input,re); 
        appendSkippedTokens();
    } 
}




// Entry rule entryRuleExpressionConstraint
entryRuleExpressionConstraint returns [EObject current=null] 
	@init { 
		HiddenTokens myHiddenTokenState = ((XtextTokenStream)input).setHiddenTokens("RULE_WS", "RULE_SL_COMMENT", "RULE_ML_COMMENT");
	}
	:
	{ newCompositeNode(grammarAccess.getExpressionConstraintRule()); }
	 iv_ruleExpressionConstraint=ruleExpressionConstraint 
	 { $current=$iv_ruleExpressionConstraint.current; } 
	 EOF 
;
finally {
	myHiddenTokenState.restore();
}

// Rule ExpressionConstraint
ruleExpressionConstraint returns [EObject current=null] 
    @init { enterRule(); 
		HiddenTokens myHiddenTokenState = ((XtextTokenStream)input).setHiddenTokens("RULE_WS", "RULE_SL_COMMENT", "RULE_ML_COMMENT");
    }
    @after { leaveRule(); }:

    { 
        newCompositeNode(grammarAccess.getExpressionConstraintAccess().getOrExpressionConstraintParserRuleCall()); 
    }
    this_OrExpressionConstraint_0=ruleOrExpressionConstraint
    { 
        $current = $this_OrExpressionConstraint_0.current; 
        afterParserOrEnumRuleCall();
    }

;
finally {
	myHiddenTokenState.restore();
}





// Entry rule entryRuleOrExpressionConstraint
entryRuleOrExpressionConstraint returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getOrExpressionConstraintRule()); }
	 iv_ruleOrExpressionConstraint=ruleOrExpressionConstraint 
	 { $current=$iv_ruleOrExpressionConstraint.current; } 
	 EOF 
;

// Rule OrExpressionConstraint
ruleOrExpressionConstraint returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(
    { 
        newCompositeNode(grammarAccess.getOrExpressionConstraintAccess().getAndExpressionConstraintParserRuleCall_0()); 
    }
    this_AndExpressionConstraint_0=ruleAndExpressionConstraint
    { 
        $current = $this_AndExpressionConstraint_0.current; 
        afterParserOrEnumRuleCall();
    }
((
    {
        $current = forceCreateModelElementAndSet(
            grammarAccess.getOrExpressionConstraintAccess().getOrExpressionConstraintLeftAction_1_0(),
            $current);
    }
)this_OR_2=RULE_OR
    { 
    newLeafNode(this_OR_2, grammarAccess.getOrExpressionConstraintAccess().getORTerminalRuleCall_1_1()); 
    }
(
(
		{ 
	        newCompositeNode(grammarAccess.getOrExpressionConstraintAccess().getRightAndExpressionConstraintParserRuleCall_1_2_0()); 
	    }
		lv_right_3_0=ruleAndExpressionConstraint		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getOrExpressionConstraintRule());
	        }
       		set(
       			$current, 
       			"right",
        		lv_right_3_0, 
        		"AndExpressionConstraint");
	        afterParserOrEnumRuleCall();
	    }

)
))*)
;





// Entry rule entryRuleAndExpressionConstraint
entryRuleAndExpressionConstraint returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getAndExpressionConstraintRule()); }
	 iv_ruleAndExpressionConstraint=ruleAndExpressionConstraint 
	 { $current=$iv_ruleAndExpressionConstraint.current; } 
	 EOF 
;

// Rule AndExpressionConstraint
ruleAndExpressionConstraint returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(
    { 
        newCompositeNode(grammarAccess.getAndExpressionConstraintAccess().getExclusionExpressionConstraintParserRuleCall_0()); 
    }
    this_ExclusionExpressionConstraint_0=ruleExclusionExpressionConstraint
    { 
        $current = $this_ExclusionExpressionConstraint_0.current; 
        afterParserOrEnumRuleCall();
    }
((
    {
        $current = forceCreateModelElementAndSet(
            grammarAccess.getAndExpressionConstraintAccess().getAndExpressionConstraintLeftAction_1_0(),
            $current);
    }
)this_AND_2=RULE_AND
    { 
    newLeafNode(this_AND_2, grammarAccess.getAndExpressionConstraintAccess().getANDTerminalRuleCall_1_1()); 
    }
(
(
		{ 
	        newCompositeNode(grammarAccess.getAndExpressionConstraintAccess().getRightExclusionExpressionConstraintParserRuleCall_1_2_0()); 
	    }
		lv_right_3_0=ruleExclusionExpressionConstraint		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getAndExpressionConstraintRule());
	        }
       		set(
       			$current, 
       			"right",
        		lv_right_3_0, 
        		"ExclusionExpressionConstraint");
	        afterParserOrEnumRuleCall();
	    }

)
))*)
;





// Entry rule entryRuleExclusionExpressionConstraint
entryRuleExclusionExpressionConstraint returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getExclusionExpressionConstraintRule()); }
	 iv_ruleExclusionExpressionConstraint=ruleExclusionExpressionConstraint 
	 { $current=$iv_ruleExclusionExpressionConstraint.current; } 
	 EOF 
;

// Rule ExclusionExpressionConstraint
ruleExclusionExpressionConstraint returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(
    { 
        newCompositeNode(grammarAccess.getExclusionExpressionConstraintAccess().getSimpleExpressionConstraintParserRuleCall_0()); 
    }
    this_SimpleExpressionConstraint_0=ruleSimpleExpressionConstraint
    { 
        $current = $this_SimpleExpressionConstraint_0.current; 
        afterParserOrEnumRuleCall();
    }
((
    {
        $current = forceCreateModelElementAndSet(
            grammarAccess.getExclusionExpressionConstraintAccess().getExclusionExpressionConstraintLeftAction_1_0(),
            $current);
    }
)this_MINUS_2=RULE_MINUS
    { 
    newLeafNode(this_MINUS_2, grammarAccess.getExclusionExpressionConstraintAccess().getMINUSTerminalRuleCall_1_1()); 
    }
(
(
		{ 
	        newCompositeNode(grammarAccess.getExclusionExpressionConstraintAccess().getRightSimpleExpressionConstraintParserRuleCall_1_2_0()); 
	    }
		lv_right_3_0=ruleSimpleExpressionConstraint		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getExclusionExpressionConstraintRule());
	        }
       		set(
       			$current, 
       			"right",
        		lv_right_3_0, 
        		"SimpleExpressionConstraint");
	        afterParserOrEnumRuleCall();
	    }

)
))?)
;





// Entry rule entryRuleSimpleExpressionConstraint
entryRuleSimpleExpressionConstraint returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getSimpleExpressionConstraintRule()); }
	 iv_ruleSimpleExpressionConstraint=ruleSimpleExpressionConstraint 
	 { $current=$iv_ruleSimpleExpressionConstraint.current; } 
	 EOF 
;

// Rule SimpleExpressionConstraint
ruleSimpleExpressionConstraint returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(
    { 
        newCompositeNode(grammarAccess.getSimpleExpressionConstraintAccess().getChildOfParserRuleCall_0()); 
    }
    this_ChildOf_0=ruleChildOf
    { 
        $current = $this_ChildOf_0.current; 
        afterParserOrEnumRuleCall();
    }

    |
    { 
        newCompositeNode(grammarAccess.getSimpleExpressionConstraintAccess().getDescendantOfParserRuleCall_1()); 
    }
    this_DescendantOf_1=ruleDescendantOf
    { 
        $current = $this_DescendantOf_1.current; 
        afterParserOrEnumRuleCall();
    }

    |
    { 
        newCompositeNode(grammarAccess.getSimpleExpressionConstraintAccess().getDescendantOrSelfOfParserRuleCall_2()); 
    }
    this_DescendantOrSelfOf_2=ruleDescendantOrSelfOf
    { 
        $current = $this_DescendantOrSelfOf_2.current; 
        afterParserOrEnumRuleCall();
    }

    |
    { 
        newCompositeNode(grammarAccess.getSimpleExpressionConstraintAccess().getParentOfParserRuleCall_3()); 
    }
    this_ParentOf_3=ruleParentOf
    { 
        $current = $this_ParentOf_3.current; 
        afterParserOrEnumRuleCall();
    }

    |
    { 
        newCompositeNode(grammarAccess.getSimpleExpressionConstraintAccess().getAncestorOfParserRuleCall_4()); 
    }
    this_AncestorOf_4=ruleAncestorOf
    { 
        $current = $this_AncestorOf_4.current; 
        afterParserOrEnumRuleCall();
    }

    |
    { 
        newCompositeNode(grammarAccess.getSimpleExpressionConstraintAccess().getAncestorOrSelfOfParserRuleCall_5()); 
    }
    this_AncestorOrSelfOf_5=ruleAncestorOrSelfOf
    { 
        $current = $this_AncestorOrSelfOf_5.current; 
        afterParserOrEnumRuleCall();
    }

    |
    { 
        newCompositeNode(grammarAccess.getSimpleExpressionConstraintAccess().getFocusConceptParserRuleCall_6()); 
    }
    this_FocusConcept_6=ruleFocusConcept
    { 
        $current = $this_FocusConcept_6.current; 
        afterParserOrEnumRuleCall();
    }
)
;





// Entry rule entryRuleFocusConcept
entryRuleFocusConcept returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getFocusConceptRule()); }
	 iv_ruleFocusConcept=ruleFocusConcept 
	 { $current=$iv_ruleFocusConcept.current; } 
	 EOF 
;

// Rule FocusConcept
ruleFocusConcept returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(
    { 
        newCompositeNode(grammarAccess.getFocusConceptAccess().getMemberOfParserRuleCall_0()); 
    }
    this_MemberOf_0=ruleMemberOf
    { 
        $current = $this_MemberOf_0.current; 
        afterParserOrEnumRuleCall();
    }

    |
    { 
        newCompositeNode(grammarAccess.getFocusConceptAccess().getConceptReferenceParserRuleCall_1()); 
    }
    this_ConceptReference_1=ruleConceptReference
    { 
        $current = $this_ConceptReference_1.current; 
        afterParserOrEnumRuleCall();
    }

    |
    { 
        newCompositeNode(grammarAccess.getFocusConceptAccess().getAnyParserRuleCall_2()); 
    }
    this_Any_2=ruleAny
    { 
        $current = $this_Any_2.current; 
        afterParserOrEnumRuleCall();
    }
)
;





// Entry rule entryRuleChildOf
entryRuleChildOf returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getChildOfRule()); }
	 iv_ruleChildOf=ruleChildOf 
	 { $current=$iv_ruleChildOf.current; } 
	 EOF 
;

// Rule ChildOf
ruleChildOf returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(this_LT_EM_0=RULE_LT_EM
    { 
    newLeafNode(this_LT_EM_0, grammarAccess.getChildOfAccess().getLT_EMTerminalRuleCall_0()); 
    }
(
(
(
		{ 
	        newCompositeNode(grammarAccess.getChildOfAccess().getConstraintFocusConceptParserRuleCall_1_0_0()); 
	    }
		lv_constraint_1_1=ruleFocusConcept		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getChildOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_1, 
        		"FocusConcept");
	        afterParserOrEnumRuleCall();
	    }

    |		{ 
	        newCompositeNode(grammarAccess.getChildOfAccess().getConstraintNestableExpressionParserRuleCall_1_0_1()); 
	    }
		lv_constraint_1_2=ruleNestableExpression		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getChildOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_2, 
        		"NestableExpression");
	        afterParserOrEnumRuleCall();
	    }

)

)
))
;





// Entry rule entryRuleDescendantOf
entryRuleDescendantOf returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getDescendantOfRule()); }
	 iv_ruleDescendantOf=ruleDescendantOf 
	 { $current=$iv_ruleDescendantOf.current; } 
	 EOF 
;

// Rule DescendantOf
ruleDescendantOf returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(this_LT_0=RULE_LT
    { 
    newLeafNode(this_LT_0, grammarAccess.getDescendantOfAccess().getLTTerminalRuleCall_0()); 
    }
(
(
(
		{ 
	        newCompositeNode(grammarAccess.getDescendantOfAccess().getConstraintFocusConceptParserRuleCall_1_0_0()); 
	    }
		lv_constraint_1_1=ruleFocusConcept		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getDescendantOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_1, 
        		"FocusConcept");
	        afterParserOrEnumRuleCall();
	    }

    |		{ 
	        newCompositeNode(grammarAccess.getDescendantOfAccess().getConstraintNestableExpressionParserRuleCall_1_0_1()); 
	    }
		lv_constraint_1_2=ruleNestableExpression		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getDescendantOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_2, 
        		"NestableExpression");
	        afterParserOrEnumRuleCall();
	    }

)

)
))
;





// Entry rule entryRuleDescendantOrSelfOf
entryRuleDescendantOrSelfOf returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getDescendantOrSelfOfRule()); }
	 iv_ruleDescendantOrSelfOf=ruleDescendantOrSelfOf 
	 { $current=$iv_ruleDescendantOrSelfOf.current; } 
	 EOF 
;

// Rule DescendantOrSelfOf
ruleDescendantOrSelfOf returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(this_DBL_LT_0=RULE_DBL_LT
    { 
    newLeafNode(this_DBL_LT_0, grammarAccess.getDescendantOrSelfOfAccess().getDBL_LTTerminalRuleCall_0()); 
    }
(
(
(
		{ 
	        newCompositeNode(grammarAccess.getDescendantOrSelfOfAccess().getConstraintFocusConceptParserRuleCall_1_0_0()); 
	    }
		lv_constraint_1_1=ruleFocusConcept		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getDescendantOrSelfOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_1, 
        		"FocusConcept");
	        afterParserOrEnumRuleCall();
	    }

    |		{ 
	        newCompositeNode(grammarAccess.getDescendantOrSelfOfAccess().getConstraintNestableExpressionParserRuleCall_1_0_1()); 
	    }
		lv_constraint_1_2=ruleNestableExpression		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getDescendantOrSelfOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_2, 
        		"NestableExpression");
	        afterParserOrEnumRuleCall();
	    }

)

)
))
;





// Entry rule entryRuleParentOf
entryRuleParentOf returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getParentOfRule()); }
	 iv_ruleParentOf=ruleParentOf 
	 { $current=$iv_ruleParentOf.current; } 
	 EOF 
;

// Rule ParentOf
ruleParentOf returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(this_GT_EM_0=RULE_GT_EM
    { 
    newLeafNode(this_GT_EM_0, grammarAccess.getParentOfAccess().getGT_EMTerminalRuleCall_0()); 
    }
(
(
(
		{ 
	        newCompositeNode(grammarAccess.getParentOfAccess().getConstraintFocusConceptParserRuleCall_1_0_0()); 
	    }
		lv_constraint_1_1=ruleFocusConcept		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getParentOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_1, 
        		"FocusConcept");
	        afterParserOrEnumRuleCall();
	    }

    |		{ 
	        newCompositeNode(grammarAccess.getParentOfAccess().getConstraintNestableExpressionParserRuleCall_1_0_1()); 
	    }
		lv_constraint_1_2=ruleNestableExpression		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getParentOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_2, 
        		"NestableExpression");
	        afterParserOrEnumRuleCall();
	    }

)

)
))
;





// Entry rule entryRuleAncestorOf
entryRuleAncestorOf returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getAncestorOfRule()); }
	 iv_ruleAncestorOf=ruleAncestorOf 
	 { $current=$iv_ruleAncestorOf.current; } 
	 EOF 
;

// Rule AncestorOf
ruleAncestorOf returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(this_GT_0=RULE_GT
    { 
    newLeafNode(this_GT_0, grammarAccess.getAncestorOfAccess().getGTTerminalRuleCall_0()); 
    }
(
(
(
		{ 
	        newCompositeNode(grammarAccess.getAncestorOfAccess().getConstraintFocusConceptParserRuleCall_1_0_0()); 
	    }
		lv_constraint_1_1=ruleFocusConcept		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getAncestorOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_1, 
        		"FocusConcept");
	        afterParserOrEnumRuleCall();
	    }

    |		{ 
	        newCompositeNode(grammarAccess.getAncestorOfAccess().getConstraintNestableExpressionParserRuleCall_1_0_1()); 
	    }
		lv_constraint_1_2=ruleNestableExpression		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getAncestorOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_2, 
        		"NestableExpression");
	        afterParserOrEnumRuleCall();
	    }

)

)
))
;





// Entry rule entryRuleAncestorOrSelfOf
entryRuleAncestorOrSelfOf returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getAncestorOrSelfOfRule()); }
	 iv_ruleAncestorOrSelfOf=ruleAncestorOrSelfOf 
	 { $current=$iv_ruleAncestorOrSelfOf.current; } 
	 EOF 
;

// Rule AncestorOrSelfOf
ruleAncestorOrSelfOf returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(this_DBL_GT_0=RULE_DBL_GT
    { 
    newLeafNode(this_DBL_GT_0, grammarAccess.getAncestorOrSelfOfAccess().getDBL_GTTerminalRuleCall_0()); 
    }
(
(
(
		{ 
	        newCompositeNode(grammarAccess.getAncestorOrSelfOfAccess().getConstraintFocusConceptParserRuleCall_1_0_0()); 
	    }
		lv_constraint_1_1=ruleFocusConcept		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getAncestorOrSelfOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_1, 
        		"FocusConcept");
	        afterParserOrEnumRuleCall();
	    }

    |		{ 
	        newCompositeNode(grammarAccess.getAncestorOrSelfOfAccess().getConstraintNestableExpressionParserRuleCall_1_0_1()); 
	    }
		lv_constraint_1_2=ruleNestableExpression		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getAncestorOrSelfOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_2, 
        		"NestableExpression");
	        afterParserOrEnumRuleCall();
	    }

)

)
))
;





// Entry rule entryRuleMemberOf
entryRuleMemberOf returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getMemberOfRule()); }
	 iv_ruleMemberOf=ruleMemberOf 
	 { $current=$iv_ruleMemberOf.current; } 
	 EOF 
;

// Rule MemberOf
ruleMemberOf returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(this_CARET_0=RULE_CARET
    { 
    newLeafNode(this_CARET_0, grammarAccess.getMemberOfAccess().getCARETTerminalRuleCall_0()); 
    }
(
(
(
		{ 
	        newCompositeNode(grammarAccess.getMemberOfAccess().getConstraintConceptReferenceParserRuleCall_1_0_0()); 
	    }
		lv_constraint_1_1=ruleConceptReference		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getMemberOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_1, 
        		"ConceptReference");
	        afterParserOrEnumRuleCall();
	    }

    |		{ 
	        newCompositeNode(grammarAccess.getMemberOfAccess().getConstraintAnyParserRuleCall_1_0_1()); 
	    }
		lv_constraint_1_2=ruleAny		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getMemberOfRule());
	        }
       		set(
       			$current, 
       			"constraint",
        		lv_constraint_1_2, 
        		"Any");
	        afterParserOrEnumRuleCall();
	    }

)

)
))
;





// Entry rule entryRuleConceptReference
entryRuleConceptReference returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getConceptReferenceRule()); }
	 iv_ruleConceptReference=ruleConceptReference 
	 { $current=$iv_ruleConceptReference.current; } 
	 EOF 
;

// Rule ConceptReference
ruleConceptReference returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
((
(
		{ 
	        newCompositeNode(grammarAccess.getConceptReferenceAccess().getIdSnomedIdentifierParserRuleCall_0_0()); 
	    }
		lv_id_0_0=ruleSnomedIdentifier		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getConceptReferenceRule());
	        }
       		set(
       			$current, 
       			"id",
        		lv_id_0_0, 
        		"SnomedIdentifier");
	        afterParserOrEnumRuleCall();
	    }

)
)(this_PIPE_1=RULE_PIPE
    { 
    newLeafNode(this_PIPE_1, grammarAccess.getConceptReferenceAccess().getPIPETerminalRuleCall_1_0()); 
    }
(
(
		{ 
	        newCompositeNode(grammarAccess.getConceptReferenceAccess().getTermTermParserRuleCall_1_1_0()); 
	    }
		lv_term_2_0=ruleTerm		{
	        if ($current==null) {
	            $current = createModelElementForParent(grammarAccess.getConceptReferenceRule());
	        }
       		set(
       			$current, 
       			"term",
        		lv_term_2_0, 
        		"Term");
	        afterParserOrEnumRuleCall();
	    }

)
)this_PIPE_3=RULE_PIPE
    { 
    newLeafNode(this_PIPE_3, grammarAccess.getConceptReferenceAccess().getPIPETerminalRuleCall_1_2()); 
    }
)?)
;





// Entry rule entryRuleAny
entryRuleAny returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getAnyRule()); }
	 iv_ruleAny=ruleAny 
	 { $current=$iv_ruleAny.current; } 
	 EOF 
;

// Rule Any
ruleAny returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(this_WILDCARD_0=RULE_WILDCARD
    { 
    newLeafNode(this_WILDCARD_0, grammarAccess.getAnyAccess().getWILDCARDTerminalRuleCall_0()); 
    }
(
    {
        $current = forceCreateModelElement(
            grammarAccess.getAnyAccess().getAnyAction_1(),
            $current);
    }
))
;





// Entry rule entryRuleNestableExpression
entryRuleNestableExpression returns [EObject current=null] 
	:
	{ newCompositeNode(grammarAccess.getNestableExpressionRule()); }
	 iv_ruleNestableExpression=ruleNestableExpression 
	 { $current=$iv_ruleNestableExpression.current; } 
	 EOF 
;

// Rule NestableExpression
ruleNestableExpression returns [EObject current=null] 
    @init { enterRule(); 
    }
    @after { leaveRule(); }:
(this_ROUND_OPEN_0=RULE_ROUND_OPEN
    { 
    newLeafNode(this_ROUND_OPEN_0, grammarAccess.getNestableExpressionAccess().getROUND_OPENTerminalRuleCall_0()); 
    }

    { 
        newCompositeNode(grammarAccess.getNestableExpressionAccess().getExpressionConstraintParserRuleCall_1()); 
    }
    this_ExpressionConstraint_1=ruleExpressionConstraint
    { 
        $current = $this_ExpressionConstraint_1.current; 
        afterParserOrEnumRuleCall();
    }
this_ROUND_CLOSE_2=RULE_ROUND_CLOSE
    { 
    newLeafNode(this_ROUND_CLOSE_2, grammarAccess.getNestableExpressionAccess().getROUND_CLOSETerminalRuleCall_2()); 
    }
)
;





// Entry rule entryRuleSnomedIdentifier
entryRuleSnomedIdentifier returns [String current=null] 
	@init { 
		HiddenTokens myHiddenTokenState = ((XtextTokenStream)input).setHiddenTokens();
	}
	:
	{ newCompositeNode(grammarAccess.getSnomedIdentifierRule()); } 
	 iv_ruleSnomedIdentifier=ruleSnomedIdentifier 
	 { $current=$iv_ruleSnomedIdentifier.current.getText(); }  
	 EOF 
;
finally {
	myHiddenTokenState.restore();
}

// Rule SnomedIdentifier
ruleSnomedIdentifier returns [AntlrDatatypeRuleToken current=new AntlrDatatypeRuleToken()] 
    @init { enterRule(); 
		HiddenTokens myHiddenTokenState = ((XtextTokenStream)input).setHiddenTokens();
    }
    @after { leaveRule(); }:
(    this_DIGIT_NONZERO_0=RULE_DIGIT_NONZERO    {
		$current.merge(this_DIGIT_NONZERO_0);
    }

    { 
    newLeafNode(this_DIGIT_NONZERO_0, grammarAccess.getSnomedIdentifierAccess().getDIGIT_NONZEROTerminalRuleCall_0()); 
    }
(    this_DIGIT_NONZERO_1=RULE_DIGIT_NONZERO    {
		$current.merge(this_DIGIT_NONZERO_1);
    }

    { 
    newLeafNode(this_DIGIT_NONZERO_1, grammarAccess.getSnomedIdentifierAccess().getDIGIT_NONZEROTerminalRuleCall_1_0()); 
    }

    |    this_ZERO_2=RULE_ZERO    {
		$current.merge(this_ZERO_2);
    }

    { 
    newLeafNode(this_ZERO_2, grammarAccess.getSnomedIdentifierAccess().getZEROTerminalRuleCall_1_1()); 
    }
)(    this_DIGIT_NONZERO_3=RULE_DIGIT_NONZERO    {
		$current.merge(this_DIGIT_NONZERO_3);
    }

    { 
    newLeafNode(this_DIGIT_NONZERO_3, grammarAccess.getSnomedIdentifierAccess().getDIGIT_NONZEROTerminalRuleCall_2_0()); 
    }

    |    this_ZERO_4=RULE_ZERO    {
		$current.merge(this_ZERO_4);
    }

    { 
    newLeafNode(this_ZERO_4, grammarAccess.getSnomedIdentifierAccess().getZEROTerminalRuleCall_2_1()); 
    }
)(    this_DIGIT_NONZERO_5=RULE_DIGIT_NONZERO    {
		$current.merge(this_DIGIT_NONZERO_5);
    }

    { 
    newLeafNode(this_DIGIT_NONZERO_5, grammarAccess.getSnomedIdentifierAccess().getDIGIT_NONZEROTerminalRuleCall_3_0()); 
    }

    |    this_ZERO_6=RULE_ZERO    {
		$current.merge(this_ZERO_6);
    }

    { 
    newLeafNode(this_ZERO_6, grammarAccess.getSnomedIdentifierAccess().getZEROTerminalRuleCall_3_1()); 
    }
)(    this_DIGIT_NONZERO_7=RULE_DIGIT_NONZERO    {
		$current.merge(this_DIGIT_NONZERO_7);
    }

    { 
    newLeafNode(this_DIGIT_NONZERO_7, grammarAccess.getSnomedIdentifierAccess().getDIGIT_NONZEROTerminalRuleCall_4_0()); 
    }

    |    this_ZERO_8=RULE_ZERO    {
		$current.merge(this_ZERO_8);
    }

    { 
    newLeafNode(this_ZERO_8, grammarAccess.getSnomedIdentifierAccess().getZEROTerminalRuleCall_4_1()); 
    }
)(    this_DIGIT_NONZERO_9=RULE_DIGIT_NONZERO    {
		$current.merge(this_DIGIT_NONZERO_9);
    }

    { 
    newLeafNode(this_DIGIT_NONZERO_9, grammarAccess.getSnomedIdentifierAccess().getDIGIT_NONZEROTerminalRuleCall_5_0()); 
    }

    |    this_ZERO_10=RULE_ZERO    {
		$current.merge(this_ZERO_10);
    }

    { 
    newLeafNode(this_ZERO_10, grammarAccess.getSnomedIdentifierAccess().getZEROTerminalRuleCall_5_1()); 
    }
)+)
    ;
finally {
	myHiddenTokenState.restore();
}





// Entry rule entryRuleTerm
entryRuleTerm returns [String current=null] 
	@init { 
		HiddenTokens myHiddenTokenState = ((XtextTokenStream)input).setHiddenTokens();
	}
	:
	{ newCompositeNode(grammarAccess.getTermRule()); } 
	 iv_ruleTerm=ruleTerm 
	 { $current=$iv_ruleTerm.current.getText(); }  
	 EOF 
;
finally {
	myHiddenTokenState.restore();
}

// Rule Term
ruleTerm returns [AntlrDatatypeRuleToken current=new AntlrDatatypeRuleToken()] 
    @init { enterRule(); 
		HiddenTokens myHiddenTokenState = ((XtextTokenStream)input).setHiddenTokens();
    }
    @after { leaveRule(); }:
((
    { 
        newCompositeNode(grammarAccess.getTermAccess().getTermCharacterParserRuleCall_0()); 
    }
    this_TermCharacter_0=ruleTermCharacter    {
		$current.merge(this_TermCharacter_0);
    }

    { 
        afterParserOrEnumRuleCall();
    }
)+((    this_WS_1=RULE_WS    {
		$current.merge(this_WS_1);
    }

    { 
    newLeafNode(this_WS_1, grammarAccess.getTermAccess().getWSTerminalRuleCall_1_0()); 
    }
)+(
    { 
        newCompositeNode(grammarAccess.getTermAccess().getTermCharacterParserRuleCall_1_1()); 
    }
    this_TermCharacter_2=ruleTermCharacter    {
		$current.merge(this_TermCharacter_2);
    }

    { 
        afterParserOrEnumRuleCall();
    }
)+)*)
    ;
finally {
	myHiddenTokenState.restore();
}





// Entry rule entryRuleTermCharacter
entryRuleTermCharacter returns [String current=null] 
	@init { 
		HiddenTokens myHiddenTokenState = ((XtextTokenStream)input).setHiddenTokens();
	}
	:
	{ newCompositeNode(grammarAccess.getTermCharacterRule()); } 
	 iv_ruleTermCharacter=ruleTermCharacter 
	 { $current=$iv_ruleTermCharacter.current.getText(); }  
	 EOF 
;
finally {
	myHiddenTokenState.restore();
}

// Rule TermCharacter
ruleTermCharacter returns [AntlrDatatypeRuleToken current=new AntlrDatatypeRuleToken()] 
    @init { enterRule(); 
		HiddenTokens myHiddenTokenState = ((XtextTokenStream)input).setHiddenTokens();
    }
    @after { leaveRule(); }:
(    this_LT_0=RULE_LT    {
		$current.merge(this_LT_0);
    }

    { 
    newLeafNode(this_LT_0, grammarAccess.getTermCharacterAccess().getLTTerminalRuleCall_0()); 
    }

    |    this_GT_1=RULE_GT    {
		$current.merge(this_GT_1);
    }

    { 
    newLeafNode(this_GT_1, grammarAccess.getTermCharacterAccess().getGTTerminalRuleCall_1()); 
    }

    |    this_DBL_LT_2=RULE_DBL_LT    {
		$current.merge(this_DBL_LT_2);
    }

    { 
    newLeafNode(this_DBL_LT_2, grammarAccess.getTermCharacterAccess().getDBL_LTTerminalRuleCall_2()); 
    }

    |    this_DBL_GT_3=RULE_DBL_GT    {
		$current.merge(this_DBL_GT_3);
    }

    { 
    newLeafNode(this_DBL_GT_3, grammarAccess.getTermCharacterAccess().getDBL_GTTerminalRuleCall_3()); 
    }

    |    this_AND_4=RULE_AND    {
		$current.merge(this_AND_4);
    }

    { 
    newLeafNode(this_AND_4, grammarAccess.getTermCharacterAccess().getANDTerminalRuleCall_4()); 
    }

    |    this_OR_5=RULE_OR    {
		$current.merge(this_OR_5);
    }

    { 
    newLeafNode(this_OR_5, grammarAccess.getTermCharacterAccess().getORTerminalRuleCall_5()); 
    }

    |    this_NOT_6=RULE_NOT    {
		$current.merge(this_NOT_6);
    }

    { 
    newLeafNode(this_NOT_6, grammarAccess.getTermCharacterAccess().getNOTTerminalRuleCall_6()); 
    }

    |    this_ZERO_7=RULE_ZERO    {
		$current.merge(this_ZERO_7);
    }

    { 
    newLeafNode(this_ZERO_7, grammarAccess.getTermCharacterAccess().getZEROTerminalRuleCall_7()); 
    }

    |    this_DIGIT_NONZERO_8=RULE_DIGIT_NONZERO    {
		$current.merge(this_DIGIT_NONZERO_8);
    }

    { 
    newLeafNode(this_DIGIT_NONZERO_8, grammarAccess.getTermCharacterAccess().getDIGIT_NONZEROTerminalRuleCall_8()); 
    }

    |    this_LETTER_9=RULE_LETTER    {
		$current.merge(this_LETTER_9);
    }

    { 
    newLeafNode(this_LETTER_9, grammarAccess.getTermCharacterAccess().getLETTERTerminalRuleCall_9()); 
    }

    |    this_CARET_10=RULE_CARET    {
		$current.merge(this_CARET_10);
    }

    { 
    newLeafNode(this_CARET_10, grammarAccess.getTermCharacterAccess().getCARETTerminalRuleCall_10()); 
    }

    |    this_EQUAL_11=RULE_EQUAL    {
		$current.merge(this_EQUAL_11);
    }

    { 
    newLeafNode(this_EQUAL_11, grammarAccess.getTermCharacterAccess().getEQUALTerminalRuleCall_11()); 
    }

    |    this_PLUS_12=RULE_PLUS    {
		$current.merge(this_PLUS_12);
    }

    { 
    newLeafNode(this_PLUS_12, grammarAccess.getTermCharacterAccess().getPLUSTerminalRuleCall_12()); 
    }

    |    this_CURLY_OPEN_13=RULE_CURLY_OPEN    {
		$current.merge(this_CURLY_OPEN_13);
    }

    { 
    newLeafNode(this_CURLY_OPEN_13, grammarAccess.getTermCharacterAccess().getCURLY_OPENTerminalRuleCall_13()); 
    }

    |    this_CURLY_CLOSE_14=RULE_CURLY_CLOSE    {
		$current.merge(this_CURLY_CLOSE_14);
    }

    { 
    newLeafNode(this_CURLY_CLOSE_14, grammarAccess.getTermCharacterAccess().getCURLY_CLOSETerminalRuleCall_14()); 
    }

    |    this_ROUND_OPEN_15=RULE_ROUND_OPEN    {
		$current.merge(this_ROUND_OPEN_15);
    }

    { 
    newLeafNode(this_ROUND_OPEN_15, grammarAccess.getTermCharacterAccess().getROUND_OPENTerminalRuleCall_15()); 
    }

    |    this_ROUND_CLOSE_16=RULE_ROUND_CLOSE    {
		$current.merge(this_ROUND_CLOSE_16);
    }

    { 
    newLeafNode(this_ROUND_CLOSE_16, grammarAccess.getTermCharacterAccess().getROUND_CLOSETerminalRuleCall_16()); 
    }

    |    this_SQUARE_OPEN_17=RULE_SQUARE_OPEN    {
		$current.merge(this_SQUARE_OPEN_17);
    }

    { 
    newLeafNode(this_SQUARE_OPEN_17, grammarAccess.getTermCharacterAccess().getSQUARE_OPENTerminalRuleCall_17()); 
    }

    |    this_SQUARE_CLOSE_18=RULE_SQUARE_CLOSE    {
		$current.merge(this_SQUARE_CLOSE_18);
    }

    { 
    newLeafNode(this_SQUARE_CLOSE_18, grammarAccess.getTermCharacterAccess().getSQUARE_CLOSETerminalRuleCall_18()); 
    }

    |    this_DOT_19=RULE_DOT    {
		$current.merge(this_DOT_19);
    }

    { 
    newLeafNode(this_DOT_19, grammarAccess.getTermCharacterAccess().getDOTTerminalRuleCall_19()); 
    }

    |    this_COLON_20=RULE_COLON    {
		$current.merge(this_COLON_20);
    }

    { 
    newLeafNode(this_COLON_20, grammarAccess.getTermCharacterAccess().getCOLONTerminalRuleCall_20()); 
    }

    |    this_COMMA_21=RULE_COMMA    {
		$current.merge(this_COMMA_21);
    }

    { 
    newLeafNode(this_COMMA_21, grammarAccess.getTermCharacterAccess().getCOMMATerminalRuleCall_21()); 
    }

    |    this_OTHER_CHARACTER_22=RULE_OTHER_CHARACTER    {
		$current.merge(this_OTHER_CHARACTER_22);
    }

    { 
    newLeafNode(this_OTHER_CHARACTER_22, grammarAccess.getTermCharacterAccess().getOTHER_CHARACTERTerminalRuleCall_22()); 
    }
)
    ;
finally {
	myHiddenTokenState.restore();
}





RULE_AND : 'AND';

RULE_OR : 'OR';

RULE_MINUS : 'MINUS';

RULE_ZERO : '0';

RULE_DIGIT_NONZERO : '1'..'9';

RULE_LETTER : ('a'..'z'|'A'..'Z');

RULE_PIPE : '|';

RULE_COLON : ':';

RULE_CURLY_OPEN : '{';

RULE_CURLY_CLOSE : '}';

RULE_EQUAL : '=';

RULE_COMMA : ',';

RULE_ROUND_OPEN : '(';

RULE_ROUND_CLOSE : ')';

RULE_SQUARE_OPEN : '[';

RULE_SQUARE_CLOSE : ']';

RULE_PLUS : '+';

RULE_CARET : '^';

RULE_NOT : '!';

RULE_DOT : '.';

RULE_WILDCARD : '*';

RULE_LT : '<';

RULE_GT : '>';

RULE_DBL_LT : '<<';

RULE_DBL_GT : '>>';

RULE_LT_EM : '<!';

RULE_GT_EM : '>!';

RULE_WS : (' '|'\t'|'\n'|'\r');

RULE_ML_COMMENT : '/*' ( options {greedy=false;} : . )*'*/';

RULE_SL_COMMENT : '//' ~(('\n'|'\r'))* ('\r'? '\n')?;

RULE_OTHER_CHARACTER : .;


