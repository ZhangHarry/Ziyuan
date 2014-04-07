package tester;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import lstar.IReportHandler;
import tzuyu.engine.TzClass;
import tzuyu.engine.algorithm.iface.Tester;
import tzuyu.engine.iface.ITzManager;
import tzuyu.engine.iface.TzReportHandler;
import tzuyu.engine.instrument.TzuYuInstrumentor;
import tzuyu.engine.model.Prestate;
import tzuyu.engine.model.Query;
import tzuyu.engine.model.QueryResult;
import tzuyu.engine.model.QueryTrace;
import tzuyu.engine.model.Sequence;
import tzuyu.engine.model.TzuYuAction;
import tzuyu.engine.model.TzuYuAlphabet;
import tzuyu.engine.model.exception.ReportException;
import tzuyu.engine.model.exception.TzException;
import tzuyu.engine.utils.Pair;

/**
 * This version of tester treats the constructors defined in the target class as
 * a normal method. for the epsilon membership query we just return true.
 * 
 * @author Spencer Xiao
 * 
 */
public class TzuYuTester implements Tester {

	private ITCGStrategy tcg;

	private IInstrumentor instrumentor;

	private HashSet<TzuYuAction> cachedUnkownResult = new HashSet<TzuYuAction>();
	private TzClass project;

	public TzuYuTester(ITzManager<?> prjFactory) {
		tcg = prjFactory.getTCGStrategy();
		tcg = new RandomTCGStrategy(prjFactory);
		instrumentor = new TzuYuInstrumentor();
	}
	
	public void setProject(TzClass project) {
		this.project = project;
		tcg.setProject(project);
		instrumentor.setProject(project);
	}

	/**
	 * Test the the trace for the give query by generating multiple test cases
	 * for it and then execute each test cases by collecting the runtime result
	 * and return to the query handler. We don't add another constructor call
	 * for each trace since in this version, the constructors are treated as
	 * normal methods. For the epsilon alphabet we always return true.
	 * 
	 * @param query
	 * @return
	 */
	public QueryResult test(Query query) {

		QueryResult result = new QueryResult();
		// For the epsilon trace
		if (query.isEpsilon()) {
			return result;
		}

		// For normal traces
		List<TestCase> testCases = tcg.generate(query);

		for (TestCase tc : testCases) {
			QueryTrace runtimeTrace = tc.execute(instrumentor);

			if (runtimeTrace.isAccepted()) {
				result.positiveSet.add(runtimeTrace);
			} else if (runtimeTrace.isRejected()) {
				result.negativeSet.add(runtimeTrace);
			} else {
				result.unknownSet.add(runtimeTrace);
			}
		}

		return result;

	}

	public boolean confirmWishfulThinking(TzuYuAction stmt) {
		if (cachedUnkownResult.contains(stmt)) {
			return false;
		} else {
			// Find in the error traces in the history store to confirm whether
			// the same transition has parameters but failed after its action.
			List<TestCase> cases = tcg
					.findFailedEvidenceForUnknownStatement(stmt);
			boolean found = false;
			for (TestCase tc : cases) {
				Query trace = tc.getTrace();
				TzuYuAction lastAction = trace.getStatement(trace.size() - 1);

				if (lastAction.equals(stmt)) {
					found = true;
					break;
				} else if (lastAction.getAction().equals(stmt.getAction())) {
					QueryTrace runtimeTrace = tc.execute(instrumentor);
					List<Prestate> states = runtimeTrace.getStates();

					Prestate lastState = states.get(states.size() - 1);

					if (stmt.getGuard().evaluate(lastState)) {
						found = true;
						break;
					}
				}
			}

			if (found) {
				cachedUnkownResult.add(stmt);
			}

			// If we cannot find such a transition then we return true, which
			// means
			// the transition is OK according to our wishful thinking; If such a
			// transition exists in the error traces, we must return false,
			// which
			// means the wishful thinking does not work.
			return !found;
		}
	}

	/**
	 * Execute all the generated test cases so far. Since the test cases does
	 * not save the instrumented states with it, every time if we want to get
	 * the instrumented states of the test case, we have to execute it.
	 * 
	 * @return
	 */
	public QueryResult executeAllOldTestCases() {
		List<TestCase> allCases = tcg.getAllGeneratedTestCases();
		QueryResult result = new QueryResult();
		for (TestCase tc : allCases) {
			QueryTrace runtimeTrace = tc.execute(instrumentor);
			if (runtimeTrace.isAccepted()) {
				result.positiveSet.add(runtimeTrace);
			} else if (runtimeTrace.isRejected()) {
				result.negativeSet.add(runtimeTrace);
			} else {
				result.unknownSet.add(runtimeTrace);
			}
		}
		return result;
	}

	private Pair<List<Sequence>, List<Sequence>> getAllTestCases() {
		// Only retrieve good test cases
		Pair<List<TestCase>, List<TestCase>> cases = tcg.getAllTestcases(project.getConfiguration()
				.isPrintPassTests(), project.getConfiguration()
				.isPrintFailTests());
		// Retrieve all good and error traces
		// List<TestCase> cases = tcg.getAllGeneratedTestCases();
		return Pair.of(extractSequences(cases.a), extractSequences(cases.b));
	}
	
	private List<Sequence> extractSequences(List<TestCase> tcs) {
		List<Sequence> seqs = new ArrayList<Sequence>(tcs.size());
		for (TestCase tc : tcs) {
			seqs.add(tc.getSequence());
		}
		return seqs;
	}

	public void report(IReportHandler<TzuYuAlphabet> reporter)
			throws ReportException {
		try {
			((TzReportHandler)reporter).writeTestCases(getAllTestCases(), project);
		} catch (TzException e) {
			throw new ReportException(e);
		}
	}
}
