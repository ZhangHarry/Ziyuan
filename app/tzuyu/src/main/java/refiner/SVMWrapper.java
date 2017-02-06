package refiner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import tzuyu.engine.bool.FieldVar;
import tzuyu.engine.bool.LIATerm;
import tzuyu.engine.bool.utils.FormulaUtils;
import tzuyu.engine.iface.ITzManager;
import tzuyu.engine.model.ArtFieldInfo;
import tzuyu.engine.model.ClassInfo;
import tzuyu.engine.model.Formula;
import tzuyu.engine.model.ObjectInfo;
import tzuyu.engine.model.Prestate;
import tzuyu.engine.model.QueryTrace;
import tzuyu.engine.model.StatementKind;
import tzuyu.engine.model.TzuYuAction;
import tzuyu.engine.utils.Pair;
import tzuyu.engine.utils.Randomness;

/**
 * This class is responsible for finding the divider in the theory of Quantifier
 * Free Linear Rational Arithmetic (QRLRA). It first try to find a best maximum
 * simple separator for the positive and negative training data sets. If the
 * training data sets are not linearly separable in this simple form, we will
 * try to find the intersection of half-spaces of the simple dividers generated
 * by considering each data point and the positive data set.
 * 
 * The generated divider may also the union of half-spaces, only if the found
 * divider in the first step is not valid. The union of half-spaces are
 * generated interchange the label for positive and negative training data set
 * and then output the negation of the generated divider.
 * 
 * In the worst case, we may find the trivial divider which is the conjunction
 * of each positive data points.
 * 
 * We argue that the found divider is in the form of QFLRA because the SMT
 * solver can only handle QFLRA formulas in general.
 * 
 * @author Spencer Xiao
 * 
 */
public class SVMWrapper {

	private static final double EPSILON = 1e-3;

	private svm_parameter configuration = getDefaultParameters();
	private List<FieldVar> properties = new ArrayList<FieldVar>();
	private int currentLevel = 0;
	private int svmCallCount = 0;
	private int timeConsumed = 0;
	private int classInDepth = 0;
	private ITzManager<?> manager = null;

	public SVMWrapper(ITzManager<?> manager) {
		this.manager = manager;
	}

	private svm_parameter getDefaultParameters() {
		svm_parameter param = new svm_parameter();
		param.svm_type = svm_parameter.C_SVC;
		param.kernel_type = svm_parameter.LINEAR;
		param.C = 1000.0;
		param.cache_size = 40;
		param.eps = 1e-3;
		param.shrinking = 1;
		param.probability = 1;
		param.nr_weight = 0;
		return param;
	}
	
	public void setClassInDepth(int classInDepth) {
		this.classInDepth = classInDepth;
	}

	public int getSVMCallCount() {
		return svmCallCount;
	}

	public int getTimeConsumed() {
		return timeConsumed;
	}

	public Formula candidateDivide(TzuYuAction action,
			List<QueryTrace> positive, List<QueryTrace> negative,
			Map<Class<?>, ClassInfo> classInfoMap) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		if (positive == null || negative == null) {
			throw new IllegalArgumentException(
					"The input sets must not be null");
		}

		if (positive.size() == 0 || negative.size() == 0) {
			throw new IllegalArgumentException(
					"The size of negative and postive "
							+ "data set for refinement must be greater than 0");
		}

		List<Prestate> positiveStates = new ArrayList<Prestate>();
		List<Prestate> negativeStates = new ArrayList<Prestate>();
		for (int index = 0; index < positive.size(); index++) {
			QueryTrace trace = positive.get(index);
			positiveStates.add(trace.getLastState());
		}

		for (int index = 0; index < negative.size(); index++) {
			QueryTrace trace = negative.get(index);
			negativeStates.add(trace.getLastState());
		}
		// For candidate query we need to check whether the non-receiver
		// parameter
		// is relevant to the execution result in order to make SVM converge
		// faster.
		List<Boolean> relevance = SVMPreprocessor.checkParametersRelevance(
				action, positive, negative);

		// Call the preprocessor to balance the inputs
		Pair<List<Prestate>, List<Prestate>> scaled = SVMPreprocessor
				.balanceTraningSet(positiveStates, negativeStates);

		StatementKind stmt = action.getAction();
		List<Class<?>> inputTypes = stmt.getInputTypes();
		List<ClassInfo> wrapperTypes = new ArrayList<ClassInfo>(
				inputTypes.size());

		int maxLevel = classInDepth;
		for (int index = 0; index < inputTypes.size(); index++) {
			Class<?> type = inputTypes.get(index);
			ClassInfo typeClassInfo = classInfoMap.get(type);
			wrapperTypes.add(typeClassInfo);
		}

		for (currentLevel = 0; currentLevel <= maxLevel; currentLevel++) {
			properties.clear();
			for (int index = 0; index < wrapperTypes.size(); index++) {
				// Don't include irrelevant parameters
				if (relevance.get(index)) {
					ClassInfo ci = wrapperTypes.get(index);

					List<ArtFieldInfo> tfields = ci
							.getFieldsAboveLevel(currentLevel);
					for (ArtFieldInfo field : tfields) {
						properties.add(FieldVar.getVar(stmt, index, field));
					}
				}
			}

			if (properties.size() == 0) {
				continue;
			}

			Formula divider = internalDivide(scaled.first(), scaled.second(),
					relevance);

			if (!divider.equals(Formula.TRUE)) {
				timeConsumed += (System.currentTimeMillis() - startTime);
				return divider;
			} else if (divider.equals(Formula.FALSE)) { // Unreachable code!?!
				return null;
			} else {
				continue;
			}
		}

		timeConsumed += (System.currentTimeMillis() - startTime);
		// We cannot find a divider for these counter examples
		return null;
	}

	public Formula memberDivide(List<QueryTrace> positive,
			List<QueryTrace> negative, Map<Class<?>, ClassInfo> classInfoMap)
			throws InterruptedException {

		long startTime = System.currentTimeMillis();
		if (positive == null || negative == null) {
			throw new IllegalArgumentException(
					"The input sets must not be null");
		}

		if (positive.size() == 0 || negative.size() == 0) {
			throw new IllegalArgumentException(
					"The size of negative and postive "
							+ "data set for refinement must be greater than 0");
		}

		List<Prestate> positiveStates = new ArrayList<Prestate>();
		List<Prestate> negativeStates = new ArrayList<Prestate>();
		for (int index = 0; index < positive.size(); index++) {
			QueryTrace trace = positive.get(index);
			positiveStates.add(trace.getLastState());
		}

		for (int index = 0; index < negative.size(); index++) {
			QueryTrace trace = negative.get(index);
			negativeStates.add(trace.getLastState());
		}

		TzuYuAction transition = negative.get(0).getNextAction();

		// For membership query we also need to check whether the non-receiver
		// parameter is relevant to the execution result in order to make SVM
		// converge faster.
		List<Boolean> relevance = SVMPreprocessor.checkParametersRelevance(
				transition, positive, negative);

		// Call the preprocessor to balance the inputs
		Pair<List<Prestate>, List<Prestate>> scaled = SVMPreprocessor
				.balanceTraningSet(positiveStates, negativeStates);

		// The inconsistency may happen during the intermediate method calls,
		// so we firstly need to find where the first inconsistent happened.
		StatementKind stmt = transition.getAction();
		List<Class<?>> inputTypes = stmt.getInputTypes();
		List<ClassInfo> wrapperTypes = new ArrayList<ClassInfo>(
				inputTypes.size());

		int maxLevel = classInDepth;
		for (int index = 0; index < inputTypes.size(); index++) {
			Class<?> type = inputTypes.get(index);
			ClassInfo targetType = classInfoMap.get(type);
			wrapperTypes.add(targetType);
		}

		for (currentLevel = 0; currentLevel < maxLevel; currentLevel++) {
			properties.clear();
			for (int index = 0; index < wrapperTypes.size(); index++) {
				// Only generate divider for relevant parameters
				if (relevance.get(index)) {
					ClassInfo ci = wrapperTypes.get(index);
					List<ArtFieldInfo> fields = ci
							.getFieldsAboveLevel(currentLevel);
					for (ArtFieldInfo field : fields) {
						properties.add(FieldVar.getVar(stmt, index, field));
					}
				}
			}

			if (properties.size() == 0) {
				continue;
			}

			Formula divider = internalDivide(scaled.first(), scaled.second(),
					relevance);
			if (!divider.equals(Formula.TRUE)) {
				timeConsumed += (System.currentTimeMillis() - startTime);
				return divider;
			} else if (divider.equals(Formula.FALSE)) {
				return null;
			}
		}
		timeConsumed += (System.currentTimeMillis() - startTime);
		// We cannot find a divider for these counter examples
		return null;
	}

	private Formula internalDivide(List<Prestate> positive,
			List<Prestate> negative, List<Boolean> relevance) throws InterruptedException {
		svm_problem problem = initializeProblem(positive, negative, relevance);

		svm_model model = solveSimple(problem);

		Formula formula = generateDivider(model, problem);
		if (!formula.equals(Formula.FALSE)) {
			return formula;
		} else {
			return solveIntersection(problem);
		}
	}

	private svm_problem initializeProblem(List<Prestate> positive,
			List<Prestate> negative, List<Boolean> filter) {
		int propertySize = properties.size();
		svm_problem prob = new svm_problem();

		prob.l = positive.size() + negative.size();
		// Since the input problem may be sparse, we don't know how many nodes
		// there are in each line.
		prob.x = new svm_node[prob.l][];
		prob.y = new double[prob.l];

		int pSize = positive.size();
		for (int i = 0; i < pSize; i++) {
			// Handle the positive values
			// Get the second to last main object from
			// both positive and negative examples
			Prestate state = positive.get(i);
			// Set the i-th point
			Vector<svm_node> nodes = new Vector<svm_node>();
			List<ObjectInfo> objs = state.getValuesAboveLevel(currentLevel,
					filter);
			for (int j = 0; j < propertySize; j++) {
				svm_node node = new svm_node();
				node.index = j + 1;

				if (j > objs.size() - 1) {
					node.value = 0;
				} else {
					ObjectInfo obj = objs.get(j);
					node.value = obj.getNumericValue();
				}
				nodes.add(node);
			}

			prob.x[i] = nodes.toArray(new svm_node[nodes.size()]);
			// Set to the positive category
			prob.y[i] = 1;
		}

		int nSize = negative.size();
		for (int i = 0; i < nSize; i++) {
			// Handle the negative values
			Prestate state = negative.get(i);
			// Set the i-th point
			Vector<svm_node> nodes = new Vector<svm_node>();
			List<ObjectInfo> objs = state.getValuesAboveLevel(currentLevel,
					filter);
			for (int j = 0; j < propertySize; j++) {
				svm_node node = new svm_node();
				node.index = j + 1;
				if (j > objs.size() - 1) {
					node.value = 0;
				} else {
					ObjectInfo obj = objs.get(j);
					node.value = obj.getNumericValue();
				}
				nodes.add(node);
			}

			prob.x[i + pSize] = nodes.toArray(new svm_node[nodes.size()]);
			// Set to the negative category
			prob.y[i + pSize] = -1;
		}
		return prob;
	}

	/**
	 * Generate the simple QFLRA separator directly from the positive training
	 * data set and the negative training data set.
	 */
	private svm_model solveSimple(svm_problem problem) throws InterruptedException {
		beforeSvm();
		return svm.svm_train(problem, configuration);
	}

	/**
	 * Generate the intersections of the half-spaces with simple QFLRA
	 */
	private Formula solveIntersection(svm_problem problem) throws InterruptedException {
		// There is the assumption that the size of negative and positive set
		// are
		// the same in the input problem.
		int size = problem.l / 2;
		// initialize the mis-classified set;
		List<svm_node[]> misclassified = new ArrayList<svm_node[]>(size);
		for (int index = size; index < problem.l; index++) {
			misclassified.add(problem.x[index]);
		}

		svm_problem newProblem = new svm_problem();
		newProblem.l = size + 1;
		newProblem.x = new svm_node[newProblem.l][];
		newProblem.y = new double[newProblem.l];
		// Copy the positive data set from problem
		for (int index = 0; index < size; index++) {
			newProblem.x[index] = problem.x[index];
			newProblem.y[index] = problem.y[index];
		}

		Formula formula = Formula.TRUE;
		do {
			// Initialize the only negative data point randomly chose
			// from the mis-classified set;
			svm_node[] dataPoint = Randomness.randomSetMember(misclassified);
			newProblem.x[size] = dataPoint;
			newProblem.y[size] = -1;
			// Generate the sub-formula
			beforeSvm();
			svm_model model = svm.svm_train(newProblem, configuration);
			Formula subDivider = getHalfspace(model, newProblem, misclassified);
			// Intersection of half-spaces
			if (subDivider.equals(Formula.TRUE)) {
				// SVM cannot differentiate the data set
				return Formula.TRUE;
			} else {
				formula = FormulaUtils.and(formula, subDivider); 
			}
		} while (misclassified.size() != 0);

		return formula;
	}
	
	private void beforeSvm() throws InterruptedException {
		if (manager != null) {			
			manager.checkProgress();
		}
		svmCallCount ++;
	}

	private Formula generateDivider(svm_model model, svm_problem problem) {
		/**
		 * Step 1: Calculate the halfspace from the model.
		 */
		int nr_class = model.nr_class;
		// Only when the number of class is two we can get the hyperplane
		if (nr_class != 2) {
			return null;
		}

		// Initialize the bias and one order multiple variables polynomial
		// for the final half-space.
		double bias = 0;
		int propertySize = properties.size();

		svm_node[] hyperplane = new svm_node[propertySize];
		for (int i = 0; i < propertySize; i++) {
			hyperplane[i] = new svm_node();
			hyperplane[i].index = i;
			hyperplane[i].value = 0.0;
		}
		// Start is the starting index for class i in the SV array.
		int[] start = new int[nr_class];
		start[0] = 0;
		for (int i = 1; i < nr_class; i++) {
			start[i] = start[i - 1] + model.nSV[i - 1];
		}

		int p = 0;
		for (int i = 0; i < nr_class; i++) {
			for (int j = i + 1; j < nr_class; j++) {
				int si = start[i];
				int sj = start[j];
				int ci = model.nSV[i];
				int cj = model.nSV[j];

				double[] coef1 = model.sv_coef[j - 1];
				double[] coef2 = model.sv_coef[i];

				for (int k = 0; k < ci; k++) {
					for (int m = 0; m < propertySize; m++) {
						hyperplane[m].value += coef1[si + k]
								* model.SV[si + k][m].value;
					}
				}

				for (int k = 0; k < cj; k++) {
					for (int m = 0; m < propertySize; m++) {
						hyperplane[m].value += coef2[sj + k]
								* model.SV[sj + k][m].value;
					}
				}

				bias = model.rho[p];
				p++;
			}
		}

		/**
		 * Step 2: check whether the generated hyper-plane is valid. Since the
		 * hyper-plane returned by libSVM may not be correct if the data sets
		 * are not linear separable, but there is no means to check whether the
		 * data is linear separable beforehand, thus we check whether the
		 * divider returned by libSVM is an correct divider post-priori. If the
		 * returned divider is not correct we return false.
		 */
		for (int i = 0; i < problem.x.length; i++) {
			svm_node[] data = problem.x[i];
			double leftHand = 0.0;
			for (int index = 0; index < propertySize; index++) {
				leftHand += data[index].value * hyperplane[index].value;
			}

			if (problem.y[i] == 1) {
				if (leftHand < bias) {
					return Formula.FALSE;
				}
			} else {
				if (leftHand > bias) {
					return Formula.FALSE;
				}
			}
		}

		/**
		 * Step 3: Construct the boolean expression from the half-space.
		 */
		List<LIATerm> terms = new ArrayList<LIATerm>();
		for (int i = 0; i < propertySize; i++) {
			// If the coefficient is zero, ignore the field.
			if (Math.abs(hyperplane[i].value) < EPSILON) {
				continue;
			}
			FieldVar field = properties.get(hyperplane[i].index);
			LIATerm term = new LIATerm(field, hyperplane[i].value);
			terms.add(term);
		}

		if (terms.size() == 0) {
			return Formula.TRUE;
		}

		// return new LIAAtom(terms, Operator.GE, bias);

		Formula divider = DividerProcessor.process(terms, bias);
		return divider;

	}

	private Formula getHalfspace(svm_model model, svm_problem problem,
			List<svm_node[]> misclassified) {
		/**
		 * Step 1: Calculate the halfspace from the model.
		 */
		int nr_class = model.nr_class;
		// Only when the number of class is two we can get the hyperplane
		if (nr_class != 2) {
			return null;
		}

		// Initialize the bias and one order multiple variables polynomial
		// for the final half-space.
		double bias = 0;
		int propertySize = properties.size();

		svm_node[] hyperplane = new svm_node[propertySize];
		for (int i = 0; i < propertySize; i++) {
			hyperplane[i] = new svm_node();
			hyperplane[i].index = i;
			hyperplane[i].value = 0.0;
		}
		// Start is the starting index for class i in the SV array.
		int[] start = new int[nr_class];
		start[0] = 0;
		for (int i = 1; i < nr_class; i++) {
			start[i] = start[i - 1] + model.nSV[i - 1];
		}

		int p = 0;
		for (int i = 0; i < nr_class; i++) {
			for (int j = i + 1; j < nr_class; j++) {
				int si = start[i];
				int sj = start[j];
				int ci = model.nSV[i];
				int cj = model.nSV[j];

				double[] coef1 = model.sv_coef[j - 1];
				double[] coef2 = model.sv_coef[i];

				for (int k = 0; k < ci; k++) {
					for (int m = 0; m < propertySize; m++) {
						hyperplane[m].value += coef1[si + k]
								* model.SV[si + k][m].value;
					}
				}

				for (int k = 0; k < cj; k++) {
					for (int m = 0; m < propertySize; m++) {
						hyperplane[m].value += coef2[sj + k]
								* model.SV[sj + k][m].value;
					}
				}

				bias = model.rho[p];
				p++;
			}
		}

		/**
		 * Step 2: Construct the boolean expression from the half-space.
		 */
		List<LIATerm> terms = new ArrayList<LIATerm>();
		for (int i = 0; i < propertySize; i++) {
			// If the coefficient is zero, ignore the field.
			if (Math.abs(hyperplane[i].value) < 1e-10) {
				continue;
			}
			FieldVar field = properties.get(hyperplane[i].index);
			LIATerm term = new LIATerm(field, hyperplane[i].value);
			terms.add(term);
		}

		if (terms.size() == 0) {
			return Formula.TRUE;
		}

		/**
		 * Step 3: Filter out the data that are correctly classified by this
		 * divider
		 */
		List<svm_node[]> negativeSet = new ArrayList<svm_node[]>(misclassified);
		for (int i = 0; i < negativeSet.size(); i++) {
			svm_node[] data = negativeSet.get(i);
			double leftHand = 0.0;
			for (int index = 0; index < propertySize; index++) {
				leftHand += data[index].value * hyperplane[index].value;
			}

			if (leftHand < bias) {
				misclassified.remove(data);
			}
		}

		// return new LIAAtom(terms, Operator.GE, bias);

		Formula divider = DividerProcessor.process(terms, bias);
		return divider;
	}
}
