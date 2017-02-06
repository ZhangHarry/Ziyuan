package lstar;

import java.util.List;
import java.util.Set;

import lstar.LStarException.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tzuyu.engine.algorithm.iface.Learner;
import tzuyu.engine.algorithm.iface.Teacher;
import tzuyu.engine.iface.ITzManager;
import tzuyu.engine.model.Action;
import tzuyu.engine.model.Trace;
import tzuyu.engine.model.dfa.Alphabet;
import tzuyu.engine.model.dfa.DFA;
import tzuyu.engine.model.dfa.State;
import tzuyu.engine.model.dfa.Transition;
import tzuyu.engine.model.exception.ReportException;
import tzuyu.engine.model.exception.TzException;

/**
 * The main interface where the client interacts with the library
 * 
 * @author Spencer Xiao
 * 
 */
public class LStar<A extends Alphabet<?>> implements Learner<A> {
	private static Logger log = LoggerFactory.getLogger(LStar.class);
	// The teacher for the L* algorithm
	private Teacher<A> teacher;

	private ObservationTable otable;

	// The alphabet for this L* algorithm
	private A sigma;

	private DFA lastDFA;
	private ITzManager<A> manager;

	public LStar(ITzManager<A> tzManager) {
		teacher = tzManager.getTeacher();
		this.otable = new ObservationTable();
		manager = tzManager;
	}
	
	public DFA getDFA() {
		return lastDFA;
	}

	public void setAlphabet(A sig) {
		teacher.setInitAlphabet(sig);
		otable.clear();
	}

	/**
	 * every time starting learning, we need to reset every state and variable in the object.
	 */
	@SuppressWarnings("unchecked")
	public DFA startLearning(A sig) throws LStarException, InterruptedException, TzException {
		this.sigma = sig;
		assert sigma != null : "Init Alphabet for L* learner is not set";
		int iterationCount = 0;
		boolean restart;
		do {
			restart = false;
			setAlphabet(sigma);
			try {
				manager.getOutStream().writeln("-------restart iteration" + 
						iterationCount++ + "-------");
				learn();
			} catch (LStarException e) {
				if (e.getType() == Type.RestartLearning) {
					log.info("Tzuyu - Restart Learning");
					restart = true;
					this.sigma = (A) e.getNewSigma();
				} else {
					// rethrow for other types of LStarException.
					throw e;
				}
			}
		} while (restart);

		return getDFA();
	}

	private void learn() throws LStarException, InterruptedException, TzException {
		if (sigma.isEmpty()) {
			// only epsilon, => no method for test detected.
			throw new LStarException(Type.AlphabetEmptyAction);
		}

		// step 1: initialize the observation table for the case where
		// both S rows and columns contain the only epsilon strings

		// Initialize the column with epsilon string
		otable.columns.add(Trace.epsilon);
		// Initialize the S rows with epsilon
		boolean ebMember = teacher.membershipQuery(Trace.epsilon);
		String eMember = BitString.extend("", ebMember);
		otable.addSRow(Trace.epsilon, eMember);

		// Initialize the SA rows with each alphabet symbol
		// ignore the epsilon alphabet, thus starts from index 1
		for (Action action : sigma.getActions()) {
			Trace str = new Trace(action);
			boolean isMember = teacher.membershipQuery(str);
			String membership = BitString.extend("", isMember);
			otable.addSARow(str, membership);
		}

		while (true) { // run until cex is empty or when need to restart
						// learning.
			manager.checkProgress();
			Trace closedCex = null;
			Trace consistentCex = null;

			while ((consistentCex = isConsistent()) != null
					|| (closedCex = isClosed()) != null) {
				if (consistentCex != null) {
					updateWhenInconsistent(consistentCex);
				}

				if (closedCex != null) {
					updateWhenOpen(closedCex);
				}
			}
			// conjecture the DFA
			lastDFA = conjectureDFA();
			// make the equivalence query
			Trace cex = teacher.candidateQuery(lastDFA);
			// the answer is yes, thus we found the unknown DFA
			if (cex.size() == 0) {
				return;
			}
			// counterexample returned and the conjectured DFA
			// is not equal to the unknown automata.
			refineWithCounterExample(cex);
		}
	}

	/**
	 * The current observation table is inconsistent, update it according to the
	 * algorithm in the paper
	 * 
	 * @param conflict
	 *            the string for which the table is inconsistent.
	 */
	private void updateWhenInconsistent(Trace conflict) throws LStarException,
			InterruptedException, TzException {
		// add the conflict into the columns
		otable.columns.add(conflict);
		// update the rows with the result of membership query

		for (Trace key1 : otable.sRows.keySet()) {
			String oldValue = otable.sRows.get(key1);
			Trace newStr = key1.concatenateAtTail(conflict);
			boolean member = teacher.membershipQuery(newStr);

			String value = BitString.extend(oldValue, member);
			otable.sRows.put(key1, value);
		}

		for (Trace key2 : otable.saRows.keySet()) {
			String oldValue = otable.saRows.get(key2);
			Trace newStr = key2.concatenateAtTail(conflict);

			boolean member = teacher.membershipQuery(newStr);
			String value = BitString.extend(oldValue, member);
			otable.saRows.put(key2, value);
		}
	}

	/**
	 * The observation is not closed (open) then update according to the
	 * algorithm defined in the paper
	 * 
	 * @param conflict
	 *            the string whose row is not included in the S row.
	 */
	private void updateWhenOpen(Trace conflict) throws LStarException,
			InterruptedException, TzException {
		// add the conflict into the S set
		otable.sRows.put(conflict, otable.saRows.get(conflict));
		otable.saRows.remove(conflict);
		// extend the added conflict with elements from Sigma

		for (Action action : sigma.getActions()) {
			Trace newKey = conflict.concatenateAtTail(action);
			String newValue = "";
			for (int j = 0; j < otable.columns.size(); j++) {
				Trace newStr = newKey.concatenateAtTail(otable.columns.get(j));

				boolean member = teacher.membershipQuery(newStr);
				newValue = BitString.extend(newValue, member);
			}

			otable.saRows.put(newKey, newValue);
		}
	}

	/**
	 * The condition under which the observation table is consistent is that:
	 * for all s1 and s2 in S, if row(s1) == row(s2) then for all action a in
	 * the alphabet such that row(s1.a) == row(s2.a)
	 * 
	 * @return
	 */
	private Trace isConsistent() {
		Set<Trace> keys = otable.sRows.keySet();
		for (Trace key1 : keys) {
			for (Trace key2 : keys) {
				if (!key1.equals(key2)
						&& otable.sRows.get(key1)
								.equals(otable.sRows.get(key2))) {
					for (Action action : sigma.getActions()) {
						Trace newKey1 = key1.concatenateAtTail(action);
						Trace newKey2 = key2.concatenateAtTail(action);
						if (!otable.getRow(newKey1).equals(
								otable.getRow(newKey2))) {
							Trace conflict = null;
							// save the a, key1 and key2 in the cache,
							// they will be used very soon
							for (int j = 0; j < otable.getRow(newKey1).length(); j++) {
								if (otable.getRow(newKey1).charAt(j) != otable
										.getRow(newKey2).charAt(j)) {
									// save j;
									conflict = otable.columns.get(j)
											.concatenateAtFront(action);
									break;
								}
							}

							return conflict;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * The condition under which the observation table is closed is that: for
	 * every s1.a in RA row there exists a s2 in S such that row(s2) ==
	 * row(s1.a)
	 * 
	 * @return
	 */
	private Trace isClosed() {
		for (Trace key1 : otable.saRows.keySet()) {
			boolean found = false;
			for (Trace key2 : otable.sRows.keySet()) {
				if (otable.sRows.get(key2).equals(otable.saRows.get(key1))) {
					found = true;
					break;
				}
			}

			if (found == false) {
				// Save key1 which will be used immediately into the cache
				return key1;
			}
		}

		return null;
	}

	/**
	 * Construct a DFA according to current observation table.
	 * 
	 * @return
	 */
	private DFA conjectureDFA() {

		DFA dfa = new DFA();
		dfa.sigma = sigma;

		int i = 1;
		for (String value : otable.sRows.values()) {
			State s = new State();
			s.id = value.toString();
			int index = dfa.addState(s);
			// the initial state;
			if (i == 1) {
				dfa.setInitialState(index);
			}
			// accepting state
			if (value.charAt(0) == '1') {
				dfa.addAcceptingState(index);
			}

			i++;
		}

		// Step 4: construct the transition relation

		for (Trace key1 : otable.sRows.keySet()) {
			int state = dfa.getStateID(otable.sRows.get(key1).toString());

			State source = dfa.getState(state);
			// Since the states associated with the key was handled, just
			// ignore it,since different keys may corresponds to the same state.
			if (source.trans.size() != 0) {
				continue;
			}
			
			for (Action action : sigma.getActions()) {
				Transition tran = new Transition();
				tran.source = state;
				tran.action = action;

				Trace newStr = key1.concatenateAtTail(action);
				String value = otable.saRows.get(newStr);
				if (value == null) {
					value = otable.sRows.get(newStr);
				}

				int target = dfa.getStateID(value.toString());
				tran.target = target;

				source.trans.add(tran);
			}
		}
		return dfa;
	}

	/**
	 * refine the observation table according to the counter example
	 */
	private void refineWithCounterExample(Trace cea) throws LStarException,
			InterruptedException, TzException {
		// Generate all prefix except the epsilon prefix which already exists
		// in S.
		List<Trace> prefix = cea.getPrefix();
		// Add the prefix to S rows and extends it by updating the SA rows.
		for (Trace str : prefix) {
			if (!otable.sRows.containsKey(str)) {
				String value = "";
				for (int i = 0; i < otable.columns.size(); i++) {
					Trace newStr = str.concatenateAtTail(otable.columns.get(i));

					boolean member = teacher.membershipQuery(newStr);
					value = BitString.extend(value, member);
				}

				otable.sRows.put(str, value);
				// If the new row exists in SA rows then remove it. Since
				// It has been added to the S rows.
				otable.saRows.remove(str);

				// extends the newly added row by adding rows to SA rows
				for (Action action : sigma.getActions()) {
					Trace newStr = str.concatenateAtTail(action);
					// Don't add the counter example string into SA rows,
					// since it has been added to S rows
					if (newStr.equals(cea)) {
						continue;
					}

					String saValue = "";
					for (int j = 0; j < otable.columns.size(); j++) {
						Trace newMemberStr = newStr
								.concatenateAtTail(otable.columns.get(j));

						boolean member = teacher.membershipQuery(newMemberStr);
						saValue = BitString.extend(saValue, member);
					}

					otable.saRows.put(newStr, saValue);
				}
			}
		}
	}

	/**
	 * report all output of the learning.
	 */
	public void report(IReportHandler<A> reporter) throws ReportException {
		if (lastDFA == null) {
			return;
		}
		// last DFA
		manager.getOutStream().writeln("Alphabet Size in Final DFA:"
						+ (lastDFA.sigma.getFullSize() - 1))
			.writeln("Number of States in Final DFA:"
						+ lastDFA.getStateSize());
		lastDFA.print(manager.getOutStream());

		reporter.reportDFA(lastDFA, sigma);
		// make report from all it component
		teacher.report(reporter);
	}

	public void setTeacher(Teacher<A> teacher) {
		this.teacher = teacher;
	}
	
}
