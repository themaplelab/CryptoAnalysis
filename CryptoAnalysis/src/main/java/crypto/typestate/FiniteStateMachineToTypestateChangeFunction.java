package crypto.typestate;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Sets;

import boomerang.WeightedForwardQuery;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import crypto.rules.CryptSLMethod;
import soot.RefType;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.Node;
import typestate.TransitionFunction;
import typestate.finiteautomata.ITransition;
import typestate.finiteautomata.MatcherTransition;
import typestate.finiteautomata.State;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;

public class FiniteStateMachineToTypestateChangeFunction extends TypeStateMachineWeightFunctions {

	private Collection<SootMethod> methodsInvokedOnInstance = Sets.newHashSet();
	
	private Collection<RefType> analyzedType = Sets.newHashSet();
	private ExtendedIDEALAnaylsis solver;

	private SootBasedStateMachineGraph fsm;

	public FiniteStateMachineToTypestateChangeFunction(SootBasedStateMachineGraph fsm, ExtendedIDEALAnaylsis solver) {
		for(MatcherTransition trans : fsm.getAllTransitions()){
			this.addTransition(trans);
		}
		for(SootMethod m : fsm.initialTransitonLabel()){
			if(m.isConstructor()){
				analyzedType.add(m.getDeclaringClass().getType());
			}
		}
		this.solver = solver;
		this.fsm = fsm;
	}


	@Override
	public TransitionFunction normal(Node<Statement, Val> curr, Node<Statement, Val> succ) {
		TransitionFunction val = super.normal(curr, succ);
		if(curr.stmt().isCallsite()){
			for (ITransition t : val.values()) {
				if (!(t instanceof LabeledMatcherTransition))
					continue;
				injectQueryAtCallSite(((LabeledMatcherTransition)t).label(),curr.stmt());
			}		

			Stmt callSite = (Stmt) curr.stmt().getUnit().get();
			if(callSite.getInvokeExpr() instanceof InstanceInvokeExpr){
				InstanceInvokeExpr e = (InstanceInvokeExpr)callSite.getInvokeExpr();
				if(e.getBase().equals(curr.fact().value())){
					solver.methodInvokedOnInstance(curr.stmt());
				}
			}
		}
		return val;
	}

	public void injectQueryForSeed(Statement u){
        injectQueryAtCallSite(fsm.getInitialTransition(),u);
	}
	
	private void injectQueryAtCallSite(List<CryptSLMethod> list, Statement callSite) {
		if(!callSite.isCallsite())
			return;
		for(CryptSLMethod matchingDescriptor : list){
			for(SootMethod m : CryptSLMethodToSootMethod.v().convert(matchingDescriptor)){
				SootMethod method = callSite.getUnit().get().getInvokeExpr().getMethod();
				if (!m.equals(method))
					continue;
				{
					int index = 0;
					for(Entry<String, String> param : matchingDescriptor.getParameters()){
						if(!param.getKey().equals("_")){
							soot.Type parameterType = method.getParameterType(index);
							if(parameterType.toString().equals(param.getValue())){
								solver.addQueryAtCallsite(param.getKey(), callSite, index);
							}
						}
						index++;
					}
				}
			}
		}
	}

	@Override
	public Collection<WeightedForwardQuery<TransitionFunction>> generateSeed(SootMethod method, Unit unit, Collection<SootMethod> optional) {
		Set<WeightedForwardQuery<TransitionFunction>> out = new HashSet<>();
		if(!method.getDeclaringClass().isApplicationClass()){
			return out;
		}
		if(fsm.seedIsConstructor()){
			if(unit instanceof AssignStmt){
				AssignStmt as = (AssignStmt) unit;
				if(as.getRightOp() instanceof NewExpr){
					NewExpr newExpr = (NewExpr) as.getRightOp();
					if(analyzedType.contains(newExpr.getType())){
						AssignStmt stmt = (AssignStmt) unit;
						out.add(createQuery(unit,method,new AllocVal(stmt.getLeftOp(), method, as.getRightOp())));
					}
				}
			}
		}
		if (!(unit instanceof Stmt) || !((Stmt) unit).containsInvokeExpr())
			return out;
		InvokeExpr invokeExpr = ((Stmt) unit).getInvokeExpr();
		SootMethod calledMethod = invokeExpr.getMethod();
		if (!fsm.initialTransitonLabel().contains(calledMethod) || calledMethod.isConstructor())
			return out;
		if (calledMethod.isStatic()) {
			if(unit instanceof AssignStmt){
				AssignStmt stmt = (AssignStmt) unit;
				out.add(createQuery(stmt,method,new AllocVal(stmt.getLeftOp(), method, stmt.getRightOp())));
			}
		} else if (invokeExpr instanceof InstanceInvokeExpr){
			InstanceInvokeExpr iie = (InstanceInvokeExpr) invokeExpr;
			out.add(createQuery(unit,method,new AllocVal(iie.getBase(), method,iie)));
		}
		return out;
	}

	private WeightedForwardQuery<TransitionFunction> createQuery(Unit unit, SootMethod method, AllocVal allocVal) {
		return new WeightedForwardQuery<TransitionFunction>(new Statement((Stmt)unit,method), allocVal, fsm.getInitialWeight());
	}


	public Collection<SootMethod> getAllMethodsInvokedOnInstance(){
		return Sets.newHashSet(methodsInvokedOnInstance);
	}


	@Override
	protected State initialState() {
		throw new RuntimeException("Should never be called!");
	}
	
	
}
