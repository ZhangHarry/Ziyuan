package assertion.template.checker;

import java.util.ArrayList;
import java.util.List;

import invariant.templates.SingleTemplate;
import sav.settings.SAVExecutionTimeOutException;
import sav.strategies.dto.execute.value.ExecValue;

public class TypeTemplateChecker {

	protected List<SingleTemplate> validTemplates = new ArrayList<SingleTemplate>();
	
	protected List<SingleTemplate> allTemplates = new ArrayList<SingleTemplate>();
	
	public boolean checkTemplates(List<List<ExecValue>> passExecValuesList,
			List<List<ExecValue>> failExecValuesList) throws SAVExecutionTimeOutException {
		return false;
	}
	
	public List<SingleTemplate> getValidTemplates() {
		return validTemplates;
	}
	
	public List<SingleTemplate> getAllTemplates() {
		return allTemplates;
	}
	
	public boolean check(SingleTemplate t) throws SAVExecutionTimeOutException {
		boolean valid = t.validateInput() && t.check();
		
		allTemplates.add(t);
		
		if (valid) validTemplates.add(t);
		
		return valid;
	}
	
}
