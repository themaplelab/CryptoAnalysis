package test.maven;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import boomerang.BackwardQuery;
import boomerang.Query;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.ForwardBoomerangResults;
import crypto.HeadlessCryptoScanner;
import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.CrySLAnalysisListener;
import crypto.analysis.EnsuredCryptSLPredicate;
import crypto.analysis.IAnalysisSeed;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.ImpreciseValueExtractionError;
import crypto.analysis.errors.IncompleteOperationError;
import crypto.analysis.errors.TypestateError;
import crypto.extractparameter.CallSiteWithParamIndex;
import crypto.extractparameter.ExtractedValue;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CryptSLPredicate;
import sync.pds.solver.nodes.Node;
import test.IDEALCrossingTestingFramework;
import typestate.TransitionFunction;

@RunWith(Parameterized.class)
public class MavenJarTests {

	public final static String MAVEN_PREFIX = "http://central.maven.org/maven2/";
	private String currentDirectory = System.getProperty("user.dir");
	public final static String DEFAULT_DOWNLOADS_FOLDER = "src/test/java/test/maven/downloads";
	private static boolean VISUALIZATION = false;
	private CrySLAnalysisListener errorCountingAnalysisListener;
	private Table<String, Class<?>, Integer> errorMarkerCountPerErrorTypeAndMethod = HashBasedTable.create();
	private MavenJar currentJarBeingTested;
	
	public MavenJarTests(MavenJar curr) {
		this.currentJarBeingTested = curr;
	}
	
	@Parameters
    public static Collection<MavenJar> data() {
    		MavenTestData testData = new MavenTestData();
    		return testData.jars;
    }
	
	@Before
	public void setup() {
		errorCountingAnalysisListener = new CrySLAnalysisListener() {
			@Override
			public void reportError(AbstractError error) {
				Integer currCount;
				if(!errorMarkerCountPerErrorTypeAndMethod
						.contains(error.getErrorLocation().getMethod().toString(), error.getClass())) {
					currCount = 0;
				} else {
					currCount = errorMarkerCountPerErrorTypeAndMethod
							.get(error.getErrorLocation().getMethod().toString(), error.getClass());
				}
				System.out.println(error.getErrorLocation().getMethod() + "  "+error);
				Integer newCount = --currCount;
				errorMarkerCountPerErrorTypeAndMethod.put(error.getErrorLocation().getMethod().toString(),
						error.getClass(), newCount);
			}

			@Override
			public void onSeedTimeout(Node<Statement, Val> seed) {
			}

			@Override
			public void onSeedFinished(IAnalysisSeed seed, ForwardBoomerangResults<TransitionFunction> solver) {
			}

			@Override
			public void ensuredPredicates(Table<Statement, Val, Set<EnsuredCryptSLPredicate>> existingPredicates,
					Table<Statement, IAnalysisSeed, Set<CryptSLPredicate>> expectedPredicates,
					Table<Statement, IAnalysisSeed, Set<CryptSLPredicate>> missingPredicates) {

			}

			@Override
			public void discoveredSeed(IAnalysisSeed curr) {

			}

			@Override
			public void collectedValues(AnalysisSeedWithSpecification seed,
					Multimap<CallSiteWithParamIndex, ExtractedValue> collectedValues) {
			}

			@Override
			public void checkedConstraints(AnalysisSeedWithSpecification analysisSeedWithSpecification,
					Collection<ISLConstraint> relConstraints) {
			}

			@Override
			public void seedStarted(IAnalysisSeed analysisSeedWithSpecification) {
			}

			@Override
			public void boomerangQueryStarted(Query seed, BackwardQuery q) {
			}

			@Override
			public void boomerangQueryFinished(Query seed, BackwardQuery q) {

			}

			@Override
			public void beforePredicateCheck(AnalysisSeedWithSpecification analysisSeedWithSpecification) {
			}

			@Override
			public void beforeConstraintCheck(AnalysisSeedWithSpecification analysisSeedWithSpecification) {
			}

			@Override
			public void beforeAnalysis() {
			}

			@Override
			public void afterPredicateCheck(AnalysisSeedWithSpecification analysisSeedWithSpecification) {
			}

			@Override
			public void afterConstraintCheck(AnalysisSeedWithSpecification analysisSeedWithSpecification) {
			}

			@Override
			public void afterAnalysis() {
			}
		};
	}

	private void assertErrors() {
		for (Cell<String, Class<?>, Integer> c : errorMarkerCountPerErrorTypeAndMethod.cellSet()) {
			if (c.getValue() != 0) {
				if (c.getValue() > 0) {
					throw new RuntimeException(
							"Did not find all errors of type " + c.getColumnKey() + " in method " + c.getRowKey());
				} else {
					throw new RuntimeException(
							"Found too many  errors of type " + c.getColumnKey() + " in method " + c.getRowKey());
				}
			}
		}
	}
	
	private void setErrorsCount(String methodSignature, Class<?> errorType, int errorMarkerCount) {
		if (errorMarkerCountPerErrorTypeAndMethod.contains(methodSignature, errorType)) {
			throw new RuntimeException("Error Type already specified for this method");
		}
		errorMarkerCountPerErrorTypeAndMethod.put(methodSignature, errorType, errorMarkerCount);
	}
	
	private HeadlessCryptoScanner createAnalysisFor(String applicationClassPath) {
		return createAnalysisFor(applicationClassPath,
				new File(IDEALCrossingTestingFramework.RESOURCE_PATH).getAbsolutePath());
	}
	
	private HeadlessCryptoScanner createAnalysisFor(String applicationClassPath,
			String rulesDir) {
		HeadlessCryptoScanner scanner = new HeadlessCryptoScanner() {
			@Override
			protected String getRulesDirectory() {
				return rulesDir;
			}

			@Override
			protected String applicationClassPath() {
				return applicationClassPath;
			}

			@Override
			protected CrySLAnalysisListener getAdditionalListener() {
				return errorCountingAnalysisListener;
			}
			@Override
			protected String getOutputFolder() {
				File file = new File("cognicrypt-output/");
				file.mkdirs();
				return VISUALIZATION ? file.getAbsolutePath() : super.getOutputFolder();
			}

			@Override
			protected boolean enableVisualization() {
				return VISUALIZATION;
			}
		};
		return scanner;
	}
	
	@Test
	public void jarTest() throws ParseException, MalformedURLException, IOException, ClassNotFoundException {
		System.out.println(currentJarBeingTested.groupId + "\n");
		
		String jarFileName = currentJarBeingTested.artifactId.concat("-").concat(currentJarBeingTested.version).concat(".jar");
		downloadJar(currentJarBeingTested.groupId, currentJarBeingTested.artifactId, currentJarBeingTested.version, jarFileName);
		String applicationCp = new File(currentDirectory.concat(File.separator).
				concat(DEFAULT_DOWNLOADS_FOLDER).concat(File.separator).
				concat(jarFileName)).getAbsolutePath();
		HeadlessCryptoScanner scanner = createAnalysisFor(applicationCp);
		Map<String, Map<String, Integer>> errors = currentJarBeingTested.errorTable.rowMap();
		
		for (Cell<String, String, Integer> cell: currentJarBeingTested.errorTable.cellSet()){
		    setErrorsCount(cell.getRowKey(), Class.forName(cell.getColumnKey()), cell.getValue());
		}
		scanner.exec();
		deleteJar(jarFileName);
		assertErrors();
	}
	
	private void downloadJar(String groupId, String artifactId, String version, String fileName) throws MalformedURLException, IOException {
		groupId = groupId.replace(".", "/");
		String fromFile = MAVEN_PREFIX + groupId + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar"; 
        String toFile = currentDirectory.concat(File.separator).concat(DEFAULT_DOWNLOADS_FOLDER).concat(File.separator).concat(fileName);
        FileUtils.copyURLToFile(new URL(fromFile), new File(toFile), 10000, 10000);    
	}
	
	private void deleteJar(String fileName) {
		File temp = new File(currentDirectory.concat(File.separator).concat(DEFAULT_DOWNLOADS_FOLDER).concat(File.separator).concat(fileName));
		temp.delete();
	}
}
