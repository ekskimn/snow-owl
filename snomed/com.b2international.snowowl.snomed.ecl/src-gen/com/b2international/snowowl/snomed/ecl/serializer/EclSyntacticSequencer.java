/*
 * generated by Xtext
 */
package com.b2international.snowowl.snomed.ecl.serializer;

import com.b2international.snowowl.snomed.ecl.services.EclGrammarAccess;
import com.google.inject.Inject;
import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.IGrammarAccess;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.serializer.analysis.GrammarAlias.AbstractElementAlias;
import org.eclipse.xtext.serializer.analysis.GrammarAlias.AlternativeAlias;
import org.eclipse.xtext.serializer.analysis.GrammarAlias.TokenAlias;
import org.eclipse.xtext.serializer.analysis.ISyntacticSequencerPDAProvider.ISynNavigable;
import org.eclipse.xtext.serializer.analysis.ISyntacticSequencerPDAProvider.ISynTransition;
import org.eclipse.xtext.serializer.sequencer.AbstractSyntacticSequencer;

@SuppressWarnings("all")
public class EclSyntacticSequencer extends AbstractSyntacticSequencer {

	protected EclGrammarAccess grammarAccess;
	protected AbstractElementAlias match_AndExpressionConstraint_ANDTerminalRuleCall_1_1_0_or_COMMATerminalRuleCall_1_1_1;
	
	@Inject
	protected void init(IGrammarAccess access) {
		grammarAccess = (EclGrammarAccess) access;
		match_AndExpressionConstraint_ANDTerminalRuleCall_1_1_0_or_COMMATerminalRuleCall_1_1_1 = new AlternativeAlias(false, false, new TokenAlias(false, false, grammarAccess.getAndExpressionConstraintAccess().getANDTerminalRuleCall_1_1_0()), new TokenAlias(false, false, grammarAccess.getAndExpressionConstraintAccess().getCOMMATerminalRuleCall_1_1_1()));
	}
	
	@Override
	protected String getUnassignedRuleCallToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if(ruleCall.getRule() == grammarAccess.getANDRule())
			return getANDToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getCARETRule())
			return getCARETToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getCOMMARule())
			return getCOMMAToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getDBL_GTRule())
			return getDBL_GTToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getDBL_LTRule())
			return getDBL_LTToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getGTRule())
			return getGTToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getGT_EMRule())
			return getGT_EMToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getLTRule())
			return getLTToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getLT_EMRule())
			return getLT_EMToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getMINUSRule())
			return getMINUSToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getORRule())
			return getORToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getPIPERule())
			return getPIPEToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getROUND_CLOSERule())
			return getROUND_CLOSEToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getROUND_OPENRule())
			return getROUND_OPENToken(semanticObject, ruleCall, node);
		else if(ruleCall.getRule() == grammarAccess.getWILDCARDRule())
			return getWILDCARDToken(semanticObject, ruleCall, node);
		return "";
	}
	
	/**
	 * terminal AND 					: 'AND';
	 */
	protected String getANDToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "AND";
	}
	
	/**
	 * terminal CARET 					: '^';
	 */
	protected String getCARETToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "^";
	}
	
	/**
	 * terminal COMMA					: ',';
	 */
	protected String getCOMMAToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return ",";
	}
	
	/**
	 * terminal DBL_GT					: '>>';
	 */
	protected String getDBL_GTToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return ">>";
	}
	
	/**
	 * terminal DBL_LT					: '<<';
	 */
	protected String getDBL_LTToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "<<";
	}
	
	/**
	 * terminal GT						: '>';
	 */
	protected String getGTToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return ">";
	}
	
	/**
	 * terminal GT_EM					: '>!';
	 */
	protected String getGT_EMToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return ">!";
	}
	
	/**
	 * terminal LT						: '<';
	 */
	protected String getLTToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "<";
	}
	
	/**
	 * terminal LT_EM					: '<!';
	 */
	protected String getLT_EMToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "<!";
	}
	
	/**
	 * terminal MINUS					: 'MINUS';
	 */
	protected String getMINUSToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "MINUS";
	}
	
	/**
	 * terminal OR 					: 'OR';
	 */
	protected String getORToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "OR";
	}
	
	/**
	 * terminal PIPE 					: '|';
	 */
	protected String getPIPEToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "|";
	}
	
	/**
	 * terminal ROUND_CLOSE 			: ')';
	 */
	protected String getROUND_CLOSEToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return ")";
	}
	
	/**
	 * terminal ROUND_OPEN 			: '(';
	 */
	protected String getROUND_OPENToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "(";
	}
	
	/**
	 * terminal WILDCARD 				: '*';
	 */
	protected String getWILDCARDToken(EObject semanticObject, RuleCall ruleCall, INode node) {
		if (node != null)
			return getTokenText(node);
		return "*";
	}
	
	@Override
	protected void emitUnassignedTokens(EObject semanticObject, ISynTransition transition, INode fromNode, INode toNode) {
		if (transition.getAmbiguousSyntaxes().isEmpty()) return;
		List<INode> transitionNodes = collectNodes(fromNode, toNode);
		for (AbstractElementAlias syntax : transition.getAmbiguousSyntaxes()) {
			List<INode> syntaxNodes = getNodesFor(transitionNodes, syntax);
			if(match_AndExpressionConstraint_ANDTerminalRuleCall_1_1_0_or_COMMATerminalRuleCall_1_1_1.equals(syntax))
				emit_AndExpressionConstraint_ANDTerminalRuleCall_1_1_0_or_COMMATerminalRuleCall_1_1_1(semanticObject, getLastNavigableState(), syntaxNodes);
			else acceptNodes(getLastNavigableState(), syntaxNodes);
		}
	}

	/**
	 * Ambiguous syntax:
	 *     AND | COMMA
	 *
	 * This ambiguous syntax occurs at:
	 *     {AndExpressionConstraint.left=} (ambiguity) right=ExclusionExpressionConstraint
	 */
	protected void emit_AndExpressionConstraint_ANDTerminalRuleCall_1_1_0_or_COMMATerminalRuleCall_1_1_1(EObject semanticObject, ISynNavigable transition, List<INode> nodes) {
		acceptNodes(transition, nodes);
	}
	
}
