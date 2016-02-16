package microbat.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserInterestedVariables {
	
	private List<AttributionVar> roots = new ArrayList<>();
	
	private Map<String, Integer> varIDs = new HashMap<>();
	
	public boolean contains(String varID){
		return this.varIDs.keySet().contains(varID);
	}
	
	public void add(String varID, int checkTime){
		this.varIDs.put(varID, checkTime);
		
		AttributionVar var = new AttributionVar(varID, checkTime);
		List<AttributionVar> vars = findAllValidateAttributionVar();
		if(!vars.contains(var)){
			roots.add(var);
		}
	}

	public void remove(String varID) {
		this.varIDs.remove(varID);
	}
	
	public Set<String> getVarIDs(){
		return this.varIDs.keySet();
	}
	
	public double getVarScore(String varID){
		return varIDs.get(varID);
	}
	
	public String getNewestVarID(){
		String newestID = null;
		for(String varID: this.varIDs.keySet()){
			if(newestID == null){
				newestID = varID;
			}
			else{
				if(this.varIDs.get(newestID) < this.varIDs.get(varID)){
					newestID = varID;
				}
			}
		}
		return newestID;
	}

	public List<AttributionVar> getRoots() {
		return roots;
	}

	public void setRoots(List<AttributionVar> roots) {
		this.roots = roots;
	}

	public AttributionVar findOrCreateVar(String varID, int checkTime) {
		for(AttributionVar rootVar: this.roots){
			AttributionVar var = rootVar.findChild(varID);
			if(var != null){
				return var;
			}
		}
		
		AttributionVar var = new AttributionVar(varID, checkTime);
		roots.add(var);
		
		return var;
	}

	public void updateAttributionTrees() {
		List<AttributionVar> vars = findAllValidateAttributionVar();
		
		List<AttributionVar> roots = new ArrayList<>();
		for(AttributionVar var: vars){
			if(var.getParents().isEmpty()){
				roots.add(var);
			}
		}
		this.roots = roots;
	}
	
	private List<AttributionVar> findAllValidateAttributionVar(){
		List<AttributionVar> vars = new ArrayList<>();
		for(AttributionVar var: roots){
			collectVars(vars, var);
		}
		return vars;
	}

	private void collectVars(List<AttributionVar> vars, AttributionVar var) {
		if(!this.varIDs.containsKey(var.getVarID())){
			for(AttributionVar parent: var.getParents()){
				parent.getChildren().remove(var);
//				var.getParents().remove(parent);
			}
			for(AttributionVar child: var.getChildren()){
				child.getParents().remove(var);
//				var.getChildren().remove(child);
			}
		}
		else{
			vars.add(var);
		}
		
		for(AttributionVar child: var.getChildren()){
			collectVars(vars, child);
		}
	}

	/**
	 * When deciding the focus variable, I adopt the following policy: <br><br>
	 *	(1) If the user has select some read variables as wrong variables, the focus variable will be an arbitrary one;<br><br>
	 *	(2) If not, the focus variable will be the newest root attribution variables.<br><br>
	 * @param readVars
	 * @return
	 */
	public AttributionVar findFocusVar(List<AttributionVar> readVars) {
		if(readVars.isEmpty()){
			if(!roots.isEmpty()){
				return roots.get(0);				
			}
			else{
				return null;
			}
		}
		else{
			AttributionVar readVar = readVars.get(0);
			
			Collections.sort(roots, new Comparator<AttributionVar>(){

				@Override
				public int compare(AttributionVar o1, AttributionVar o2) {
					return o2.getCheckTime()-o1.getCheckTime();
				}
				
			});
			
			AttributionVar var = null;
			for(AttributionVar root: roots){
				var = root.findChild(readVar.getVarID());
				if(var != null){
					return var;
				}
			}
		}
		
		return null;
	}

	public void clear() {
		this.varIDs.clear();
		this.roots.clear();
	}
}
