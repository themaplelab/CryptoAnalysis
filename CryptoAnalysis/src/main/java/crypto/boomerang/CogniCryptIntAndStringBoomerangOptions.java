package crypto.boomerang;

import com.google.common.base.Optional;

import boomerang.IntAndStringBoomerangOptions;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.stats.CSVBoomerangStatsWriter;
import boomerang.stats.IBoomerangStats;
import crypto.analysis.CryptoScanner;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;

/**
 * Created by johannesspath on 23.12.17.
 */
public class CogniCryptIntAndStringBoomerangOptions extends IntAndStringBoomerangOptions {
	private String queryReportFile;
	public CogniCryptIntAndStringBoomerangOptions(CryptoScanner scanner) {
		if(scanner != null)
			queryReportFile = scanner.getQueryReportFileName();
	}
    @Override
    public Optional<AllocVal> getAllocationVal(SootMethod m, Stmt stmt, Val fact, BiDiInterproceduralCFG<Unit, SootMethod> icfg) {
        if (stmt.containsInvokeExpr() && stmt instanceof AssignStmt) {
            AssignStmt as = (AssignStmt) stmt;
            if (as.getLeftOp().equals(fact.value())) {
                if (icfg.getCalleesOfCallAt(stmt).isEmpty())
                    return Optional.of(new AllocVal(as.getLeftOp(), m, as.getRightOp(),new Statement(as, m)));
            }
        }
        return super.getAllocationVal(m, stmt, fact, icfg);
    }

    @Override
    public boolean onTheFlyCallGraph() {
        return false;
    }

    @Override
    public boolean arrayFlows() {
        return true;
    }
	@Override
	public IBoomerangStats statsFactory() {
		if(queryReportFile == null)
			return super.statsFactory();
		return new CSVBoomerangStatsWriter(queryReportFile);
	}

    @Override
    public int analysisTimeoutMS() {
    	return 5000;
    }
}
