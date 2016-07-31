package assertion.template.checker;

import java.util.ArrayList;
import java.util.List;

import invariant.templates.CompositeTemplate;
import invariant.templates.SingleTemplate;
import invariant.templates.onefeature.OneNumIlpTemplate;
import invariant.templates.threefeatures.ThreeNumIlpTemplate;
import invariant.templates.twofeatures.TwoNumIlpTemplate;
import libsvm.svm_model;
import libsvm.core.Category;
import libsvm.core.Divider;
import libsvm.core.Machine;
import libsvm.core.Model;
import libsvm.core.StringDividerProcessor;
import libsvm.extension.PositiveSeparationMachine;
import sav.common.core.formula.Formula;
import sav.common.core.formula.LIAAtom;
import sav.strategies.dto.execute.value.ExecValue;

public class IlpConjChecker {
	
	private List<SingleTemplate> ilpTemplates;
	
	private List<CompositeTemplate> validTemplates;

	public IlpConjChecker(List<SingleTemplate> ilpTemplates) {
		this.ilpTemplates = ilpTemplates;
		validTemplates = new ArrayList<CompositeTemplate>();
	}
	
	public List<CompositeTemplate> getValidTemplates() {
		return validTemplates;
	}
	
	public void checkIlpConjunction() {
		int size = ilpTemplates.size();
		
		for (int i = 0; i < size; i++) {
			SingleTemplate t = ilpTemplates.get(i);
			Machine m = t.getMultiCutMachine();
		
			List<List<ExecValue>> passExecValuesList = t.getPassExecValuesList();
			List<List<ExecValue>> failExecValuesList = t.getFailExecValuesList();
			
			List<String> labels = new ArrayList<String>();
			for (ExecValue ev : passExecValuesList.get(0)) {
				labels.add(ev.getVarId());
			}
			
			m = m.setDataLabels(labels);
			
			for (List<ExecValue> evl : passExecValuesList) {
				int size1 = evl.size();
				
				double[] v = new double[size1];
				for (int i1 = 0; i1 < size1; i1++) {
					v[i1] = evl.get(i1).getDoubleVal();
				}
				
				m.addDataPoint(Category.POSITIVE, v);
			}
			
			for (List<ExecValue> evl : failExecValuesList) {
				int size1 = evl.size();
				
				double[] v = new double[size1];
				for (int i1 = 0; i1 < size1; i1++) {
					v[i1] = evl.get(i1).getDoubleVal();
				}
				
				m.addDataPoint(Category.NEGATIVE, v);
			}

			m = m.train();
			if (m.getModel() != null) {
				PositiveSeparationMachine psm = (PositiveSeparationMachine) m;
				if (psm.getLearnedModels().size() > 1) {
					int numberOfFeatures = psm.getRandomData().getNumberOfFeatures();
					
					List<SingleTemplate> conj = new ArrayList<SingleTemplate>();
					List<List<SingleTemplate>> disj = new ArrayList<List<SingleTemplate>>();
					
					for (svm_model svmModel : psm.getLearnedModels()) {
						Divider explicitDivider = new Model(svmModel, numberOfFeatures).getExplicitDivider();
						Formula formula = psm.getLearnedLogic(new StringDividerProcessor(), explicitDivider, false);
						
						if (formula instanceof LIAAtom) {
							LIAAtom lia = (LIAAtom) formula;
							if (lia.getMVFOExpr().size() == 1) {
								conj.add(createOnePrimIlpTemplate(t, lia));
							} else if (lia.getMVFOExpr().size() == 2) {
								conj.add(createTwoPrimIlpTemplate(t, lia));
							} else if (lia.getMVFOExpr().size() == 3) {
								conj.add(createThreePrimIlpTemplate(t, lia));
							}
						}
					}
					
					disj.add(conj);
					validTemplates.add(new CompositeTemplate(disj));
					
					return;
				}
			}
		}
	}
	
	private SingleTemplate createOnePrimIlpTemplate(SingleTemplate t, LIAAtom lia) {
		String id1 = lia.getMVFOExpr().get(0).getVariable().getLabel();
		
		List<List<ExecValue>> newPassExecValuesList = new ArrayList<List<ExecValue>>();
		List<List<ExecValue>> newFailExecValuesList = new ArrayList<List<ExecValue>>();
		
		addValues(newPassExecValuesList, t.getPassExecValuesList(), id1);
		addValues(newFailExecValuesList, t.getFailExecValuesList(), id1);

		OneNumIlpTemplate st = new OneNumIlpTemplate(newPassExecValuesList, newFailExecValuesList);
		
		st.setA(lia.getMVFOExpr().get(0).getCoefficient());
		st.setB(lia.getConstant());
		
		return st;
	}
	
	private SingleTemplate createTwoPrimIlpTemplate(SingleTemplate t, LIAAtom lia) {
		String id1 = lia.getMVFOExpr().get(0).getVariable().getLabel();
		String id2 = lia.getMVFOExpr().get(1).getVariable().getLabel();
		
		List<List<ExecValue>> newPassExecValuesList = new ArrayList<List<ExecValue>>();
		List<List<ExecValue>> newFailExecValuesList = new ArrayList<List<ExecValue>>();
		
		addValues(newPassExecValuesList, t.getPassExecValuesList(), id1, id2);
		addValues(newFailExecValuesList, t.getFailExecValuesList(), id1, id2);
		
		TwoNumIlpTemplate st = new TwoNumIlpTemplate(newPassExecValuesList, newFailExecValuesList);
		
		st.setA(lia.getMVFOExpr().get(0).getCoefficient());
		st.setB(lia.getMVFOExpr().get(1).getCoefficient());
		st.setC(lia.getConstant());
		
		return st;
	}
	
	private SingleTemplate createThreePrimIlpTemplate(SingleTemplate t, LIAAtom lia) {
		String id1 = lia.getMVFOExpr().get(0).getVariable().getLabel();
		String id2 = lia.getMVFOExpr().get(1).getVariable().getLabel();
		String id3 = lia.getMVFOExpr().get(2).getVariable().getLabel();
		
		List<List<ExecValue>> newPassExecValuesList = new ArrayList<List<ExecValue>>();
		List<List<ExecValue>> newFailExecValuesList = new ArrayList<List<ExecValue>>();
		
		addValues(newPassExecValuesList, t.getPassExecValuesList(), id1, id2, id3);
		addValues(newFailExecValuesList, t.getFailExecValuesList(), id1, id2, id3);
		
		ThreeNumIlpTemplate st = new ThreeNumIlpTemplate(newPassExecValuesList, newFailExecValuesList);
		
		st.setA(lia.getMVFOExpr().get(0).getCoefficient());
		st.setB(lia.getMVFOExpr().get(1).getCoefficient());
		st.setC(lia.getMVFOExpr().get(2).getCoefficient());
		st.setD(lia.getConstant());
		
		return st;
	}
	
	private void addValue(List<ExecValue> newEvl, List<ExecValue> evl, String id) {
		for (int i = 0; i < evl.size(); i++) {
			if (evl.get(i).getVarId().equals(id)) {
				newEvl.add(evl.get(i));
			}
		}
	}
	
	private void addValues(List<List<ExecValue>> newExecValuesList,
			List<List<ExecValue>> execValuesList, String... idl) {
		for (List<ExecValue> evl : execValuesList) {
			List<ExecValue> newEvl = new ArrayList<ExecValue>();
			for (String id : idl) addValue(newEvl, evl, id);
			newExecValuesList.add(newEvl);
		}
	}

}
