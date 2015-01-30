package libsvm.extension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import libsvm.svm_model;
import libsvm.core.DataPoint;
import libsvm.core.Machine;

import org.junit.Assert;

/**
 * This SVM machine will try to divide the given data points many times if
 * possible to increase accuracy.<br/>
 * The training algorithm for this Machine is as follows:<br/>
 * <ul>
 * <li>Run SVM algorithm on the current data set to find a divider</li>
 * <li>Find the collection of all points which are (A) and are not (B)
 * classified correctly using the divider</li>
 * <li>While (B) is not empty, <b>and only if <i>all points</i> in (B) resides
 * on 1 side of the divider</b>, do the following steps:</li>
 * <ul>
 * <li>Use (B) as the new training data set.</li>
 * <li>Run SVM algorithm on (B).</li>
 * <li>Use the new divider in conjunction with the old divider.</li>
 * </ul>
 * </ul> <br/>
 * The limit of this algorithm is that it <b>depends on the ability of SVM</b>
 * to give out a divider which can separate data points into a state in which
 * there is only 1 side contains wrong data. I.e.: the algorithm may stop
 * without being able to improve the learning result at all.
 * 
 * @author Nguyen Phuoc Nguong Phuc (npn)
 * 
 */
public class MultiCutMachine extends Machine {

	private List<LearnedData> learnedDatas;

	public MultiCutMachine() {
		learnedDatas = new ArrayList<LearnedData>();
	}

	private class LearnedData {
		private svm_model model;
		private Set<Category> wrongSides = new HashSet<Category>();
	}

	@Override
	protected Machine train(final List<DataPoint> dataPoints) {
		List<DataPoint> trainingData = dataPoints;
		boolean stop = false;
		while (!stop) {
			super.train(trainingData);
			final List<DataPoint> wrongClassifications = getWrongClassifiedDataPoints(trainingData,
					null);

			LearnedData learnedData = new LearnedData();
			learnedData.model = model;
			Category wrongCategory = null; // Only valid if wrongCategories == 1
			for (DataPoint dp : wrongClassifications) {
				wrongCategory = dp.getCategory();
				learnedData.wrongSides.add(wrongCategory);
			}
			learnedDatas.add(learnedData);

			int wrongCategories = learnedData.wrongSides.size();
			if (wrongCategories == 1) {
				// Try to improve the result
				List<DataPoint> newTraninigData = new ArrayList<DataPoint>();
				for (DataPoint dp : trainingData) {
					// Points on the same side of the wrong classification
					final Category calculatedCategory = calculateCategory(dp, learnedData.model,
							null);
					if (calculatedCategory.equals(wrongCategory)) {
						newTraninigData.add(dp);
					}
				}
				trainingData = newTraninigData;

				if (trainingData.isEmpty()) {
					// The SVM divider failed to separate any points at all
					// I.e.: it simply divides the space into 2 parts:
					// - 1 with all points
					// - 1 with no point
					stop = true;
				}
			} else {
				// There may still be inaccuracy but we cannot do anything more
				stop = true;
			}
		}

		return this;
	}

	@Override
	protected List<DataPoint> getWrongClassifiedDataPoints(List<DataPoint> dataPoints) {
		return getWrongClassifiedDataPoints(dataPoints, new LearnedDataBasedCategoryCalculator(
				learnedDatas));
	}

	private class LearnedDataBasedCategoryCalculator implements CategoryCalculator {
		private List<LearnedData> learnedDatas;

		public LearnedDataBasedCategoryCalculator(final List<LearnedData> learnedDatas) {
			this.learnedDatas = learnedDatas;
		}

		public Integer getCategoryIndex(DataPoint dataPoint) {
			Assert.assertTrue("There is no learned data.", !learnedDatas.isEmpty());
			Integer result = null;
			for (LearnedData data : learnedDatas) {
				result = new ModelBasedCategoryCalculator(data.model).getCategoryIndex(dataPoint);
				if (!data.wrongSides.contains(result)) {
					// Correct data found
					break;
				}
			}
			return result; // Can be null or incorrect
		}
	}

}
