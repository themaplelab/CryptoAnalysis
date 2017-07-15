package crypto.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Table.Cell;

import boomerang.accessgraph.AccessGraph;
import boomerang.cfg.IExtendedICFG;
import crypto.rules.CryptSLCondPredicate;
import crypto.rules.CryptSLMethod;
import crypto.rules.CryptSLObject;
import crypto.rules.CryptSLPredicate;
import crypto.rules.CryptSLRule;
import crypto.rules.StateMachineGraph;
import crypto.rules.StateNode;
import crypto.typestate.CallSiteWithParamIndex;
import crypto.typestate.CryptSLMethodToSootMethod;
import crypto.typestate.CryptoTypestateAnaylsisProblem;
import crypto.typestate.FiniteStateMachineToTypestateChangeFunction;
import ideal.Analysis;
import ideal.AnalysisSolver;
import ideal.FactAtStatement;
import ideal.IFactAtStatement;
import ideal.ResultReporter;
import ideal.debug.IDebugger;
import soot.IntType;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import typestate.TypestateDomainValue;
import typestate.interfaces.ICryptSLPredicateParameter;
import typestate.interfaces.ISLConstraint;

public class AnalysisSeedWithSpecification implements IAnalysisSeed {

	private final IFactAtStatement factAtStmt;
	private final SootMethod method;
	private final ClassSpecification spec;
	private CryptoScanner cryptoScanner;
	private Analysis<TypestateDomainValue<StateNode>> analysis;
	private Multimap<CallSiteWithParamIndex, Unit> parametersToValues = HashMultimap.create();
	private CryptoTypestateAnaylsisProblem problem;
	private HashBasedTable<Unit, AccessGraph, TypestateDomainValue<StateNode>> results;
	private boolean solved;
	private Collection<EnsuredCryptSLPredicate> ensuredPredicates = Sets.newHashSet();
	private Multimap<Unit, StateNode> typeStateChange = HashMultimap.create();

	public AnalysisSeedWithSpecification(CryptoScanner cryptoScanner, IFactAtStatement factAtStmt, SootMethod method, ClassSpecification spec) {
		this.cryptoScanner = cryptoScanner;
		this.factAtStmt = factAtStmt;
		this.method = method;
		this.spec = spec;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((spec == null) ? 0 : spec.hashCode());
		result = prime * result + ((factAtStmt == null) ? 0 : factAtStmt.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AnalysisSeedWithSpecification other = (AnalysisSeedWithSpecification) obj;
		if (spec == null) {
			if (other.spec != null)
				return false;
		} else if (!spec.equals(other.spec))
			return false;
		if (factAtStmt == null) {
			if (other.factAtStmt != null)
				return false;
		} else if (!factAtStmt.equals(other.factAtStmt))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AnalysisSeed [" + factAtStmt + " in " + method + " with spec " + spec.getRule().getClassName() + "]";
	}

	public void execute() {
		if (!solved) {
			cryptoScanner.analysisListener().seedStarted(this);
			getOrCreateAnalysis(new ResultReporter<TypestateDomainValue<StateNode>>() {

				@Override
				public void onSeedFinished(IFactAtStatement seed, AnalysisSolver<TypestateDomainValue<StateNode>> solver) {
					parametersToValues = problem.getCollectedValues();
					cryptoScanner.analysisListener().onSeedFinished(seed, solver);
					AnalysisSeedWithSpecification.this.onSeedFinished(seed, solver);
				}

				@Override
				public void onSeedTimeout(IFactAtStatement seed) {}
			}).analysisForSeed(this);
			cryptoScanner.analysisListener().seedFinished(this);
			cryptoScanner.analysisListener().collectedValues(this, problem.getCollectedValues());
			final CryptSLRule rule = spec.getRule();
			for (ISLConstraint cons : rule.getConstraints()) {
				if (cons instanceof CryptSLPredicate && ((CryptSLPredicate) cons).isNegated()) {
					cryptoScanner.addDisallowedPredicatePair(rule.getPredicates().get(0),
							((CryptSLPredicate) cons).setNegated(false));
				}
			}
			solved = true;
		}
	}

	public void onSeedFinished(IFactAtStatement seed, AnalysisSolver<TypestateDomainValue<StateNode>> solver) {
		// Merge all information (all access graph here point to the seed
		// object)
		results = solver.results();
		Multimap<Unit, StateNode> unitToStates = HashMultimap.create();
		for (Cell<Unit, AccessGraph, TypestateDomainValue<StateNode>> c : results.cellSet()) {
			unitToStates.putAll(c.getRowKey(), c.getValue().getStates());
		}
		for (Unit curr : unitToStates.keySet()) {
			Collection<StateNode> stateAtCurrMinusPred = Sets.newHashSet(unitToStates.get(curr));
			for (Unit pred : cryptoScanner.icfg().getPredsOf(curr)) {
				Collection<StateNode> stateAtPred = unitToStates.get(pred);
				stateAtCurrMinusPred.removeAll(stateAtPred);
				for (StateNode newStateAtCurr : stateAtCurrMinusPred) {
					typeStateChangeAtStatement(pred, newStateAtCurr);
				}
			}
		}
	}

	private void typeStateChangeAtStatement(Unit curr, StateNode stateNode) {
		typeStateChange.put(curr, stateNode);
		onAddedTypestateChange(curr, stateNode);
	}

	private void onAddedTypestateChange(Unit curr, StateNode stateNode) {
		for (CryptSLPredicate predToBeEnsured : spec.getRule().getPredicates()) {
			if(predToBeEnsured.isNegated()){
				continue;
			}

			if (predToBeEnsured instanceof CryptSLCondPredicate
					&& ((CryptSLCondPredicate) predToBeEnsured).getConditionalMethods().contains(stateNode)
					|| (!(predToBeEnsured instanceof CryptSLCondPredicate) && stateNode.getAccepting())) {
					ensuresPred(predToBeEnsured, curr, stateNode);
			}
		}
	}

	private void ensuresPred(CryptSLPredicate predToBeEnsured, Unit currStmt, StateNode stateNode) {
		if (predToBeEnsured.isNegated()) {
			return;
		}
		boolean satisfiesConstraintSytem = checkConstraintSystem();
				
		for (ICryptSLPredicateParameter predicateParam : predToBeEnsured.getParameters()) {
			if (predicateParam.getName().equals("this")) {
				expectPredicateWhenThisObjectIsInState(stateNode,currStmt,predToBeEnsured,satisfiesConstraintSytem);
			}
		}
		if (currStmt instanceof Stmt && ((Stmt) currStmt).containsInvokeExpr()) {
			InvokeExpr ie = ((Stmt) currStmt).getInvokeExpr();
			SootMethod invokedMethod = ie.getMethod();
			Collection<CryptSLMethod> convert = CryptSLMethodToSootMethod.v().convert(invokedMethod);

			for (CryptSLMethod cryptSLMethod : convert) {
				Entry<String, String> retObject = cryptSLMethod.getRetObject();
				if (!retObject.getKey().equals("_") && currStmt instanceof AssignStmt) {
					AssignStmt as = (AssignStmt) currStmt;
					Value leftOp = as.getLeftOp();
					AccessGraph accessGraph = new AccessGraph((Local) leftOp, leftOp.getType());
					expectPredicateOnOtherObject(predToBeEnsured, currStmt, accessGraph,satisfiesConstraintSytem);
				}
				int i = 0;
				for (Entry<String, String> p : cryptSLMethod.getParameters()) {
					for (ICryptSLPredicateParameter predicateParam : predToBeEnsured.getParameters()) {
						if (p.getKey().equals(predicateParam.getName())) {
							Value param = ie.getArg(i);
							if (param instanceof Local) {
								AccessGraph accessGraph = new AccessGraph((Local) param, param.getType());
								expectPredicateOnOtherObject(predToBeEnsured, currStmt, accessGraph,satisfiesConstraintSytem);
							}
						}
					}
					i++;
				}

			}

		}
	}

	private void expectPredicateOnOtherObject(CryptSLPredicate predToBeEnsured, Unit currStmt,
			AccessGraph accessGraph, boolean satisfiesConstraintSytem) {
			AnalysisSeedWithEnsuredPredicate seed = cryptoScanner
					.getOrCreateSeed(new FactAtStatement(currStmt, accessGraph));
			cryptoScanner.expectPredicate(seed, currStmt, predToBeEnsured);
		if(satisfiesConstraintSytem){
			seed.addEnsuredPredicate(
					new EnsuredCryptSLPredicate(predToBeEnsured, parametersToValues));
		}
	}

	private void expectPredicateWhenThisObjectIsInState(StateNode stateNode, Unit currStmt, CryptSLPredicate predToBeEnsured, boolean satisfiesConstraintSytem) {
		cryptoScanner.expectPredicate(this,currStmt,predToBeEnsured);
		
		if(!satisfiesConstraintSytem)
			return;
		for (Cell<Unit, AccessGraph, TypestateDomainValue<StateNode>> e : results.cellSet()) {
			// TODO check for any reachable state that don't kill
			// predicates.
			if (e.getValue().getStates().contains(stateNode)) {
				cryptoScanner.addNewPred(this,e.getRowKey(), e.getColumnKey(),
						new EnsuredCryptSLPredicate(predToBeEnsured, parametersToValues));
			}
		}
	}

	private Analysis<TypestateDomainValue<StateNode>> getOrCreateAnalysis(final ResultReporter<TypestateDomainValue<StateNode>> resultReporter) {
		if (analysis == null) {
			problem = new CryptoTypestateAnaylsisProblem() {

				@Override
				public ResultReporter<TypestateDomainValue<StateNode>> resultReporter() {
					return resultReporter;
				}
				@Override
				public FiniteStateMachineToTypestateChangeFunction createTypestateChangeFunction() {
					return new FiniteStateMachineToTypestateChangeFunction(this);
				}

				@Override
				public IExtendedICFG icfg() {
					return cryptoScanner.icfg();
				}

				@Override
				public IDebugger<TypestateDomainValue<StateNode>> debugger() {
					return cryptoScanner.debugger();
				}

				@Override
				public StateMachineGraph getStateMachine() {
					return spec.getRule().getUsagePattern();
				}
				@Override
				public CryptSLAnalysisListener analysisListener() {
					return cryptoScanner.analysisListener();
				}
			};
			analysis = new Analysis<TypestateDomainValue<StateNode>>(problem);
		}
		return analysis;
	}

	private boolean checkConstraintSystem() {
		ConstraintSolver solver = new ConstraintSolver(spec, parametersToValues);
		List<ISLConstraint> relConstraints = solver.getRelConstraints();
		if (!checkPredicates(relConstraints))
			return false;
		return 0 == solver.evaluateRelConstraints();
	}

	private boolean checkPredicates(List<ISLConstraint> relConstraints) {
		List<CryptSLPredicate> requiredPredicates = Lists.newLinkedList();
		for (ISLConstraint con : relConstraints) {
			if (con instanceof CryptSLPredicate && !ConstraintSolver.predefinedPreds.contains(((CryptSLPredicate) con).getPredName())) {
				requiredPredicates.add((CryptSLPredicate) con);
			}
		}
		Set<CryptSLPredicate> remainingPredicates = Sets.newHashSet(requiredPredicates);
		for (CryptSLPredicate pred : requiredPredicates) {
			if (pred.isNegated()) {
				for (EnsuredCryptSLPredicate ensPred : ensuredPredicates) {
					if (ensPred.getPredicate().equals(pred))
						return false;
				}
				remainingPredicates.remove(pred);
			} else {
				for (EnsuredCryptSLPredicate ensPred : ensuredPredicates) {
					if (ensPred.getPredicate().equals(pred) && doPredsMatch(pred, ensPred)) {
						remainingPredicates.remove(pred);
					}
				}
			}
		}
		return remainingPredicates.isEmpty();
	}

	private boolean doPredsMatch(CryptSLPredicate pred, EnsuredCryptSLPredicate ensPred) {
		boolean requiredPredicatesExist = true;
		for (int i = 0; i < pred.getParameters().size(); i++) {
			String var = pred.getParameters().get(i).getName();
			if (isOfNonTrackableType(var)) {
				continue;
			} else if (pred.getInvolvedVarNames().contains(var)) {
				
				final String parameterI = ensPred.getPredicate().getParameters().get(i).getName();
				Collection<String> actVals = null;
				Collection<String> expVals = null;
				
				for (CallSiteWithParamIndex cswpi : ensPred.getParametersToValues().keySet()) {
					if (cswpi.getVarName().equals(parameterI)) {
						actVals = retrieveValueFromUnit(cswpi, ensPred.getParametersToValues().get(cswpi));
					}
				}
				for (CallSiteWithParamIndex cswpi : parametersToValues.keySet()) {
					if (cswpi.getVarName().equals(var)) {
						expVals = retrieveValueFromUnit(cswpi, parametersToValues.get(cswpi));
					}
				}

				String splitter = "";
				int index = -1;
				if (pred.getParameters().get(i) instanceof CryptSLObject) {
					CryptSLObject obj = (CryptSLObject) pred.getParameters().get(i);
					if (obj.getSplitter() != null) {
						splitter = obj.getSplitter().getSplitter();
						index = obj.getSplitter().getIndex();
					}
				}
				for (String foundVal : expVals) {
					if (index > -1) {
						foundVal = foundVal.split(splitter)[index];
					}
					requiredPredicatesExist &= actVals.contains(foundVal);
				}
			} else {
				requiredPredicatesExist = false;
			}
		}
		return pred.isNegated() != requiredPredicatesExist;
	}

	private Collection<String> retrieveValueFromUnit(CallSiteWithParamIndex cswpi, Collection<Unit> collection) {
		Collection<String> values = new ArrayList<String>();
		for (Unit u : collection) {
			if (cswpi.getStmt().equals(u)) {
				if (u instanceof AssignStmt) {
					values.add(retrieveConstantFromValue(((AssignStmt) u).getRightOp().getUseBoxes().get(cswpi.getIndex()).getValue()));
				} else {
					values.add(retrieveConstantFromValue(u.getUseBoxes().get(cswpi.getIndex()).getValue()));
				}
			} else if (u instanceof AssignStmt) {
				final Value rightSide = ((AssignStmt) u).getRightOp();
				if (rightSide instanceof Constant) {
					values.add(retrieveConstantFromValue(rightSide));
				} else {
					final List<ValueBox> useBoxes = rightSide.getUseBoxes();
					
//					varVal.put(callSite.getVarName(), retrieveConstantFromValue(useBoxes.get(callSite.getIndex()).getValue()));
				}
			}	
//			if (u instanceof AssignStmt) {
//				final List<ValueBox> useBoxes = ((AssignStmt) u).getRightOp().getUseBoxes();
//				if (!(useBoxes.size() <= cswpi.getIndex())) {
//					values.add(retrieveConstantFromValue(useBoxes.get(cswpi.getIndex()).getValue()));
//				} 
//			} else 	if (cswpi.getStmt().equals(u)) {
//				values.add(retrieveConstantFromValue(cswpi.getStmt().getUseBoxes().get(cswpi.getIndex()).getValue()));
//			}
		}
		return values;
	}

	private String retrieveConstantFromValue(Value val) {
		if (val instanceof StringConstant) {
			return ((StringConstant) val).value;
		} else if (val instanceof IntConstant || val.getType() instanceof IntType){
			return val.toString();
		} else {
			return "";
		}
	}
	
	private final static List<String> trackedTypes = Arrays.asList("java.lang.String", "int", "java.lang.Integer");

	private boolean isOfNonTrackableType(String varName) {
		for (Entry<String, String> object : spec.getRule().getObjects()) {
			if (object.getValue().equals(varName) && trackedTypes.contains(object.getKey())) {
				return false;
			}
		}
		return true;
	}

	public Set<EnsuredCryptSLPredicate> getEnsuredPredicates() {
		return Collections.emptySet();
	}

	@Override
	public AccessGraph getFact() {
		return factAtStmt.getFact();
	}

	@Override
	public Unit getStmt() {
		return factAtStmt.getStmt();
	}

	@Override
	public CryptoTypestateAnaylsisProblem getAnalysisProblem() {
		return problem;
	}

	@Override
	public boolean isSolved() {
		return solved;
	}

	public ClassSpecification getSpec() {
		return spec;
	}

	public void addEnsuredPredicate(EnsuredCryptSLPredicate ensPred) {
		if (ensuredPredicates.add(ensPred)) {
			for (Entry<Unit, StateNode> e : typeStateChange.entries())
				onAddedTypestateChange(e.getKey(), e.getValue());
		}
	}

	@Override
	public boolean contradictsNegations() {
		return false;
	}
}
