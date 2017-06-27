package libsvm.core;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import libsvm.extension.ISelectiveSampling;
import sav.common.core.formula.Formula;
import sav.common.core.utils.Assert;
import sav.common.core.utils.ExecutionTimer;
import sav.common.core.utils.StringUtils;
import sav.settings.SAVExecutionTimeOutException;

/**
 * This class represents an SVM machine. After initialization, it is possible to
 * set the parameters of the machine and add data points to it so that the
 * machine can learn from the data points later.
 * 
 * @author Nguyen Phuoc Nguong Phuc (npn)
 * 
 */
public class Machine {

	private static final Logger LOGGER = LoggerFactory.getLogger(Machine.class);
	private static final String DEFAULT_FEATURE_PREFIX = "x";
	private static final int SVM_TIMEOUT = 2; // In seconds

	private svm_parameter parameter = null;
	private List<DataPoint> data = new ArrayList<DataPoint>();
	protected svm_model model = null;
	private List<String> dataLabels = new ArrayList<String>();
	private boolean isGeneratedDataLabel = false;
	private boolean isDataClean = false;
	private boolean performArtificialDataSynthesis = false;

	// Do not access this directly, please use the getter instead
	private ISelectiveSampling selectiveSamplingHandler;

	public Machine setParameter(final Parameter parameter) {
		this.parameter = new svm_parameter();
		if (parameter.getMachineType() != null) {
			this.parameter.svm_type = parameter.getMachineType().index();
		}
		if (parameter.getKernelType() != null) {
			this.parameter.kernel_type = parameter.getKernelType().index();
		}
		this.parameter.degree = parameter.getDegree();
		this.parameter.gamma = parameter.getGamma();
		this.parameter.coef0 = parameter.getCoef0();
		this.parameter.cache_size = parameter.getCacheSize();
		this.parameter.eps = parameter.getEps();
		this.parameter.C = parameter.getC();
		this.parameter.nr_weight = parameter.getNrWeight();
		this.parameter.weight_label = parameter.getWeightLabel();
		this.parameter.weight = parameter.getWeight();
		this.parameter.nu = parameter.getNU();
		this.parameter.p = parameter.getP();
		this.parameter.shrinking = parameter.isUseShrinking() ? 1 : 0;
		this.parameter.probability = parameter.isPredictProbability() ? 1 : 0;
		return this;
	}

	public Parameter getParameter() {
		return new Parameter(this.parameter);
	}

	/**
	 * Set the labels for the features. This will also determine the number of
	 * features of the {@link DataPoint} to be produced by this Machine.
	 * 
	 * @param dataLabels
	 *            List of labels for the features to be watched.
	 * @return The configured Machine.
	 */
	public Machine setDataLabels(final List<String> dataLabels) {
		this.dataLabels = dataLabels;
		this.isGeneratedDataLabel = false;
		return this;
	}

	public boolean isGeneratedDataLabels() {
		return this.isGeneratedDataLabel;
	}

	public List<String> getDataLabels() {
		return dataLabels;
	}

	/**
	 * Set the number of features to be watched. This will also set the labels
	 * for the features as ["x0", "x1", ..., "x{NumberOfFeature-1}"].
	 * 
	 * @param numberOfFeatures
	 *            The number of features to set.
	 * @return The configured Machine.
	 */
	public Machine setNumberOfFeatures(final int numberOfFeatures) {
		this.dataLabels = new ArrayList<String>(numberOfFeatures);
		for (int i = 0; i < numberOfFeatures; i++) {
			this.dataLabels.add(DEFAULT_FEATURE_PREFIX + i);
		}
		this.isGeneratedDataLabel = true;
		return this;
	}

	public int getNumberOfFeatures() {
		return this.dataLabels.size();
	}

	protected ISelectiveSampling getSelectiveSamplingHandler() {
		// TODO return a default handler here if nothing is set
		return this.selectiveSamplingHandler;
	}

	public void setSelectiveSamplingHandler(ISelectiveSampling selectiveSamplingHandler) {
		this.selectiveSamplingHandler = selectiveSamplingHandler;
	}

	public Machine resetData() {
		data = new ArrayList<DataPoint>();
		this.isDataClean = false;
		this.model = null;
		return this;
	}

	public Machine addDataPoints(final List<DataPoint> dataPoints) {
		for (DataPoint point : dataPoints) {
			addDataPoint(point);
		}
		return this;
	}

	public Machine addDataPoint(final DataPoint dataPoint) {
		// TODO NPN should we deep copy here?
		data.add(dataPoint);
		return this;
	}

	public Machine addDataPoint(final Category category, final double... values) {
		final DataPoint dp = createDataPoint(category, values);
		this.addDataPoint(dp);
		return this;
	}

	public DataPoint createDataPoint(final Category category, final double... values) {
		final int numberOfFeatures = getNumberOfFeatures();
		Assert.assertTrue(values != null && values.length == numberOfFeatures,
				"Must specify " + numberOfFeatures + " items as values.");
		final DataPoint dp = new DataPoint(numberOfFeatures);
		dp.setCategory(category);
		dp.setValues(values);
		return dp;
	}

	/**
	 * Generate artificial data based on existing data points.
	 * 
	 * @param calculator
	 *            An object used to determine the category of a given point.
	 * @return <code>true</code> if the generated data is not conflicted,
	 *         <code>false</code> otherwise.
	 */
	public boolean artificialDataSynthesis(final CategoryCalculator calculator) {
		// Collect available values for each feature
		final int numberOfFeatures = getNumberOfFeatures();
		if (numberOfFeatures <= 1) {
			// Do nothing
			return true;
		}
		List<List<Double>> allValues = new ArrayList<List<Double>>(numberOfFeatures);
		for (int i = 0; i < numberOfFeatures; i++) {
			Set<Double> valueSet = new HashSet<Double>(data.size());
			for (DataPoint point : data) {
				valueSet.add(point.getValue(i));
			}
			allValues.add(new ArrayList<Double>(valueSet));
		}

		// Generate some other possible combinations
		final List<DataPoint> allPoints = new ArrayList<DataPoint>();
		for (DataPoint dp : data) {
			for (int i = 0; i < numberOfFeatures; i++) {
				for (Double value : allValues.get(i)) {
					if (Double.compare(dp.getValue(i), value.doubleValue()) == 0) {
						// This would be an existing point, so skip it
						continue;
					}

					double[] pointValues = new double[numberOfFeatures];
					for (int j = 0; j < numberOfFeatures; j++) {
						pointValues[j] = j == i ? value.doubleValue() : dp.getValue(j);
					}

					final DataPoint point = new DataPoint(numberOfFeatures);
					point.setValues(pointValues);
					point.setCategory(calculator.getCategory(point));
					if (point.getCategory() != null) {
						allPoints.add(point);
					} else {
						// No need to run machine learning
						return false;
					}
				}
			}
		}

		// Add the points into the machine
		this.addDataPoints(allPoints);
		return true;
	}

	/**
	 * Train the current machine using the preset parameters and data.
	 * <p>
	 * <b>Preconditions</b>: The parameters and data are set.
	 * </p>
	 * 
	 * @return The instance of the current machine after training completed.
	 * @throws SAVExecutionTimeOutException 
	 */
	public final Machine train() throws SAVExecutionTimeOutException {
		Assert.assertNotNull(parameter, "SVM parameters is not set.");
		Assert.assertTrue(!data.isEmpty(), "SVM training data is empty.");

		//this.data = cleanUp(data);
		if (getNumberOfFeatures() <= 0) {
//			LOGGER.warn("The feature list is empty. SVM will not run.");
			return this;
		}

		train(data);
		return this;
	}

	/**
	 * Train the current machine using the preset parameters and the given data.
	 * <p>
	 * <b>Preconditions</b>: The parameters are set.
	 * </p>
	 * 
	 * @param dataPoints
	 *            The data used to learn.
	 * @return The instance of the current machine after training completed.
	 */
	protected Machine train(final List<DataPoint> dataPoints) throws SAVExecutionTimeOutException{
		Assert.assertNotNull(parameter, "SVM parameters is not set.");
		Assert.assertTrue(!dataPoints.isEmpty(), "SVM training data is empty.");

		final svm_problem problem = new svm_problem();
		final int length = dataPoints.size();
		problem.l = length;
		problem.y = new double[length];
		problem.x = new svm_node[length][];

		for (int i = 0; i < length; i++) {
			final DataPoint point = dataPoints.get(i);
			if (point != null) {
				problem.y[i] = point.getCategory().intValue();
				problem.x[i] = point.getSvmNode();
			}
		}

		model = performTrainingTask(problem, parameter);
		
		return this;
	}

	private svm_model performTrainingTask(final svm_problem prob, final svm_parameter param) {
		ExecutionTimer timer = ExecutionTimer.getExecutionTimer(SVM_TIMEOUT, TimeUnit.SECONDS);
		SvmRunner svmRunner = new SvmRunner(prob, param);
		timer.run(svmRunner);
		
		return svmRunner.getResult();
	}

	/**
	 * Remove duplicated features. I.e.: to remove the features which have same
	 * value for all data points. Such features cannot provide any useful
	 * information.
	 * 
	 */
	private List<DataPoint> cleanUp(final List<DataPoint> dataPoints) {
		if (this.isDataClean) {
			return dataPoints;
		}

		// Find the redundant features list
		final int originalSize = getNumberOfFeatures();
		final List<Integer> indexesToRemove = new ArrayList<Integer>(originalSize);
		for (int i = 0; i < originalSize; i++) {
			if (isRedundantFeature(i, dataPoints)) {
				indexesToRemove.add(i);
			}
		}

		final int cleanedSize = originalSize - indexesToRemove.size();
		if (originalSize != cleanedSize) {
			// Clean up data labels, this will also correct getNumberOfFeatures
			final List<String> cleanedDataLabel = new ArrayList<String>(cleanedSize);
			for (int i = 0; i < originalSize; i++) {
				if (!indexesToRemove.contains(Integer.valueOf(i))) {
					cleanedDataLabel.add(this.dataLabels.get(i));
				}
			}
			// NOTE: I don't call setDataLabels here to avoid messing with the
			// property isGeneratedDataLabel
			this.dataLabels = cleanedDataLabel;

			// Clean up data points
			for (DataPoint dp : dataPoints) {
				dp.numberOfFeatures = cleanedSize;
				final double[] cleanedValues = new double[cleanedSize];
				int index = 0;
				for (int i = 0; i < originalSize; i++) {
					if (!indexesToRemove.contains(Integer.valueOf(i))) {
						cleanedValues[index++] = dp.values[i];
					}
				}
				dp.values = cleanedValues;
			}

//			LOGGER.info("Reduced feature size from " + originalSize + " to " + cleanedSize);
		}

		this.isDataClean = true;

		return dataPoints;
	}

	private boolean isRedundantFeature(final int featureIndex, final List<DataPoint> dataPoints) {
		double value = dataPoints.get(0).getValue(featureIndex);
		for (int i = 1; i < dataPoints.size(); i++) {
			if (dataPoints.get(i).getValue(featureIndex) != value) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Get the learned model.
	 * 
	 * @return The learned model or <code>null</code> if the learning process
	 *         was not completed.
	 */
	public Model getModel() {
		return model == null || model.l <= 0 || data == null || data.size() <= 0 ? null : new Model(model, data
				.get(0).getNumberOfFeatures());
	}

	protected List<DataPoint> getWrongClassifiedDataPoints(final List<DataPoint> dataPoints) {
		return getWrongClassifiedDataPoints(dataPoints, new ModelBasedCategoryCalculator(model));
	}

	protected List<DataPoint> getWrongClassifiedDataPoints(final List<DataPoint> dataPoints,
			final CategoryCalculator calculator) {
		final List<DataPoint> wrong = new ArrayList<DataPoint>();
		for (DataPoint dp : dataPoints) {
			if (!dp.getCategory().equals(calculator.getCategory(dp))) {
				wrong.add(dp);
			}
		}
		return wrong;
	}

	public double getModelAccuracy() {
		if (model == null) {
			return 0.0;
		}
		return 1.0 - ((double) getWrongClassifiedDataPoints(data).size() / data.size());
	}

	/**
	 * @param round
	 *            To indicate whether the numbers should be rounded for nicer
	 *            representation.
	 * @return The learned logic.
	 */
	public String getLearnedLogic(boolean round) {
		return getLearnedLogic(getDivider(), round);
	}

	public <R> R getLearnedLogic(IDividerProcessor<R> processor, boolean round) {
		return getLearnedLogic(processor, getDivider(), round);
	}
	
	public <R> R getLearnedLogic(IDividerProcessor<R> processor, Divider divider, boolean round) {
		return processor.process(divider, dataLabels, round);
	}
	
	public <R> R getLearnedLogic(IDividerProcessor<R> processor, Divider divider,
			boolean round, int num) {
		return processor.process(divider, dataLabels, round, num);
	}

	protected String getLearnedLogic(final Divider divider, boolean round) {
		Formula formula = getLearnedLogic(new StringDividerProcessor(), divider, round);
		if (formula == null) {
			return StringUtils.EMPTY;
		}
		return formula.toString();
	}
	
	public Divider getDivider() {
		Model currentModel = getModel();
		if (currentModel == null) {
			return null;
		}
		return currentModel.getExplicitDivider();
	}

	/**
	 * Try to fine-tune the learned logic using selective sampling. The process
	 * is considered to be successful if it can maintain or improve the existing
	 * accuracy.
	 * 
	 * @return <code>true</code> if the logic was fine-tuned successfully, or
	 *         <code>false</code> otherwise.
	 * @throws SAVExecutionTimeOutException 
	 */
	public boolean selectiveSampling() throws SAVExecutionTimeOutException {
		final Model currentModel = getModel();
		if (currentModel == null) {
			return false;
		}
		String adjustedLogic = getLearnedLogic(currentModel.getExplicitDivider(), false);
		double adjustedAccuracy = getModelAccuracy();
		String logic;
		double accuracy;
		// While the logic can still be adjusted to be better
		do {
			// Record current status
			logic = adjustedLogic;
			accuracy = adjustedAccuracy;
			// Generate new points
			List<DataPoint> dataPoints = generateNewPoints();
			// If new data points are the same as the existing ones,
			// it means that the model will not be improved anymore
			// thus we stop
			if (dataPoints.isEmpty()) {
				LOGGER.debug("No more new point added in Selective Sampling.");
				break;
			} else {
				LOGGER.debug(dataPoints.size() + " more point(s) added.");
			}
			dataPoints.addAll(this.data);
			if (!dataPoints.isEmpty()) {
				// Add to model
				this.resetData();
				this.addDataPoints(dataPoints);
				// Train again
				this.train();
			}
			// Check the new logic
			adjustedLogic = getLearnedLogic(false);
			adjustedAccuracy = getModelAccuracy();
			LOGGER.debug("adjustedLogic: " + adjustedLogic);
			LOGGER.debug("adjustedAccuracy: " + adjustedAccuracy);
		} while (!logic.equals(adjustedLogic) && Double.compare(adjustedAccuracy, accuracy) >= 0);

		return Double.compare(adjustedAccuracy, 1.0) >= 0;
	}

	private List<DataPoint> generateNewPoints() throws SAVExecutionTimeOutException {
		final ISelectiveSampling handler = getSelectiveSamplingHandler();
		if (handler != null) {
			return handler.selectData(this);
		}

		return Collections.emptyList();
	}

	public DataPoint getRandomData() {
		return data.get(0);
	}

	/**
	 * This class represents a data point to be used in SVM machine. It consists
	 * the values of that data point and its classification/category.
	 * 
	 * @author Nguyen Phuoc Nguong Phuc (npn)
	 * 
	 */
	public static class DataPoint {

		private int numberOfFeatures;
		private double[] values;
		private Category category;

		public DataPoint(final int numberOfFeatures) {
			// Hide the constructor
			// I.e.: can only be created using Machine's factory method
			this.numberOfFeatures = numberOfFeatures;
			this.values = new double[this.numberOfFeatures];
		}

		public int getNumberOfFeatures() {
			return numberOfFeatures;
		}

		public void setCategory(final Category category) {
			this.category = category;
		}

		public Category getCategory() {
			return category;
		}

		public void setValues(final double... values) {
			if (values.length != numberOfFeatures) {
				throw new InvalidParameterException("The array values must have exactly "
						+ numberOfFeatures + " number of elements");
			}
			for (int i = 0; i < values.length; i++) {
				this.values[i] = values[i];
			}
		}

		public double getValue(final int index) {
			if (index >= numberOfFeatures) {
				throw new InvalidParameterException("Index must be less than " + numberOfFeatures);
			}
			return values[index];
		}
		
		public double[] getValues() {
			return values;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}

			if (obj instanceof DataPoint) {
				DataPoint other = (DataPoint) obj;
				if (other.numberOfFeatures != numberOfFeatures || !other.category.equals(category)) {
					return false;
				} else {
					for (int i = 0; i < values.length; i++) {
						if (values[i] != other.values[i]) {
							return false;
						}
					}
					return true;
				}
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			int hash = 7;
			hash = 31 * hash + numberOfFeatures;
			hash = 31 * hash + (category == null ? 0 : category.hashCode());
			hash = 31 * hash + (values == null ? 0 : Arrays.hashCode(values));
			return hash;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append(Arrays.toString(values));
			sb.append(" : ").append(category);
			return sb.toString();
		}

		public svm_node[] getSvmNode() {
			final int numberOfFeatures = this.getNumberOfFeatures();
			final svm_node[] node = new svm_node[numberOfFeatures];
			for (int i = 0; i < numberOfFeatures; i++) {
				final svm_node svmNode = new svm_node();
				svmNode.index = i;
				svmNode.value = this.getValue(i);
				node[i] = svmNode;
			}
			return node;
		}
	}
	
	public void setDefaultParams() {
		setParameter(new Parameter().
				setMachineType(MachineType.C_SVC).
				setKernelType(KernelType.LINEAR).
				setEps(0.00001).
				setUseShrinking(false).
				setPredictProbability(false).
				setC(Double.MAX_VALUE));
	}

	public List<DataPoint> getDataPoints() {
		return data;
	}

	public boolean isPerformArtificialDataSynthesis() {
		return performArtificialDataSynthesis;
	}

	public Machine setPerformArtificialDataSynthesis(boolean performArtificialDataSynthesis) {
		this.performArtificialDataSynthesis = performArtificialDataSynthesis;
		return this;
	}
}
