package invariant.templates.twofeatures;

import java.util.List;

import sav.strategies.dto.execute.value.ExecValue;

public class TwoPrimNeTemplate extends TwoFeaturesTemplate {

	public TwoPrimNeTemplate(List<List<ExecValue>> passExecValuesList, List<List<ExecValue>> failExecValuesList) {
		super(passExecValuesList, failExecValuesList);
	}
	
	@Override
	public boolean check() {
		// list of pass and fail exec value only has two features
		// two features in pass values must be different
		for (List<ExecValue> evl : passExecValuesList) {
			if (evl.get(0).getDoubleVal() == evl.get(1).getDoubleVal()) {
				return false;
			}
		}
		
		// two features in pass values must be equals
		for (List<ExecValue> evl : failExecValuesList) {
			if (evl.get(0).getDoubleVal() != evl.get(1).getDoubleVal()) {
				return false;
			}
		}
		
		return true;
	}

}
