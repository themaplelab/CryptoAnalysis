package crypto.analysis;

import crypto.analysis.util.StmtWithMethod;
import soot.Unit;
import typestate.interfaces.ISLConstraint;

public interface ConstraintReporter {

	public void constraintViolated(ISLConstraint con, StmtWithMethod unit);
	
	void callToForbiddenMethod(ClassSpecification classSpecification, Unit callSite);

}
