package invariant.templates.onefeature;

import java.util.ArrayList;
import java.util.List;

import sav.common.core.formula.Eq;
import sav.common.core.formula.Var;
import sav.strategies.dto.execute.value.ExecValue;
import sav.strategies.dto.execute.value.ExecVar;
import sav.strategies.dto.execute.value.ExecVarType;

//Template x != a

public class OneNumNeTemplate extends OneFeatureTemplate {

	private double a;
	
	public OneNumNeTemplate(List<List<ExecValue>> passExecValuesList, List<List<ExecValue>> failExecValuesList) {
		super(passExecValuesList, failExecValuesList);
	}

	@Override
	public boolean checkPassValue(List<ExecValue> evl) {
		// list of pass and fail exec value only has one feature
		// all pass values must be must be different with the fail value
		double v = evl.get(0).getDoubleVal();
		return v != a;
	}
	
	@Override
	public boolean checkFailValue(List<ExecValue> evl) {
		// list of pass and fail exec value only has one feature
		// all fail value must be the same
		double v = evl.get(0).getDoubleVal();
		return v == a;
	}
	
	@Override
	public boolean check() {
		a = failValues.get(0).get(0).getDoubleVal();
 		return check(passValues, failValues);
	}
	
	@Override
	public List<List<Eq<?>>> sampling() {
		List<List<Eq<?>>> samples = new ArrayList<List<Eq<?>>>();
		
		ExecValue ev = passValues.get(0).get(0);
		Var v = new ExecVar(ev.getVarId(), ev.getType());
		
		List<Eq<?>> sample1 = new ArrayList<Eq<?>>();
		if (ev.getType() == ExecVarType.INTEGER)
			sample1.add(new Eq<Number>(v, (int) (a - 1.0)));
		else if (ev.getType() == ExecVarType.LONG)
			sample1.add(new Eq<Number>(v, (long) (a - 1.0)));
		else if (ev.getType() == ExecVarType.FLOAT)
			sample1.add(new Eq<Number>(v, (float) (a - 0.01)));
		else
			sample1.add(new Eq<Number>(v, (a - 0.01)));
		
		List<Eq<?>> sample2 = new ArrayList<Eq<?>>();
		if (ev.getType() == ExecVarType.INTEGER)
			sample2.add(new Eq<Number>(v, (int) (a + 1.0)));
		else if (ev.getType() == ExecVarType.LONG)
			sample2.add(new Eq<Number>(v, (long) (a + 1.0)));
		else if (ev.getType() == ExecVarType.FLOAT)
			sample2.add(new Eq<Number>(v, (float) (a + 0.01)));
		else
			sample2.add(new Eq<Number>(v, (a + 0.01)));
		
//		List<Eq<?>> sample3 = new ArrayList<Eq<?>>();
//		if (ev.getType() == ExecVarType.INTEGER)
//			sample3.add(new Eq<Number>(v, (int) 0.0));
//		else if (ev.getType() == ExecVarType.LONG)
//			sample3.add(new Eq<Number>(v, (long) 0.0));
//		else
//			sample3.add(new Eq<Number>(v, 0.0));
		
		samples.add(sample1);
		samples.add(sample2);
//		samples.add(sample3);
		
		return samples;
	}
	
	@Override
	public String toString() {
		return passValues.get(0).get(0).getVarId() + " != " + a;
	}
	
}