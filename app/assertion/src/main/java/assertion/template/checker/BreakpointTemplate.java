package assertion.template.checker;

import java.util.Arrays;
import java.util.List;

import invariant.templates.Template;
import sav.strategies.dto.BreakPoint;

public class BreakpointTemplate {

	private BreakPoint bkp;
	
	private List<Template> singleTemplates;
	
	private List<Template> compositeTemplates;
	
	public BreakpointTemplate(BreakPoint bkp, List<Template> singleTemplates,
			List<Template> compositeTemplates) {
		this.bkp = bkp;
		this.singleTemplates = singleTemplates;
		this.compositeTemplates = compositeTemplates;
	}
	
	public BreakPoint getBreakPoint() {
		return bkp;
	}
	
	public List<Template> getSingleTemplates() {
		return singleTemplates;
	}
	
	public List<Template> getCompositeTemplates() {
		return compositeTemplates;
	}
	
	@Override
	public String toString() {
//		return bkp + Arrays.toString(singleTemplates.toArray()) +
//				Arrays.toString(compositeTemplates.toArray());
		return Arrays.toString(singleTemplates.toArray()) +
				Arrays.toString(compositeTemplates.toArray());
	}
	
}
