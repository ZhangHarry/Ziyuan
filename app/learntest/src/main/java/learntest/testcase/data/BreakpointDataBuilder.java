package learntest.testcase.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import icsetlv.common.dto.BreakpointValue;
import learntest.breakpoint.data.BreakpointBuilder;
import learntest.breakpoint.data.DecisionLocation;
import sav.strategies.dto.BreakPoint;

public class BreakpointDataBuilder {
	
	private BreakpointBuilder bkpBuilder;
	private DecisionLocation target;
	private Map<DecisionLocation, BreakpointData> bkpDataMap;
	private List<BreakpointData> result;
	
	/*private Map<BreakPoint, DecisionLocation> branchMap;
	private Map<BreakPoint, DecisionLocation> loopMap;
	private Map<DecisionLocation, BreakpointData> bkpDataMap;*/
	
	public BreakpointDataBuilder(BreakpointBuilder bkpBuilder) {
		this.bkpBuilder = bkpBuilder;
		bkpDataMap = new HashMap<DecisionLocation, BreakpointData>();
	}
	
	public void setTarget(DecisionLocation target) {
		this.target = target;
		bkpDataMap = new HashMap<DecisionLocation, BreakpointData>();
		result = null;
	}
	
	public void build(List<BreakPoint> path, BreakpointValue inputValue) {
		Map<BreakPoint, List<Integer>> pathMap = buildPathMap(path);
		if(target == null) {
			List<DecisionLocation> locations = bkpBuilder.getLocations();
			for (DecisionLocation location : locations) {
				build(pathMap, inputValue, location);
			}
		} else {
			build(pathMap, inputValue, target);
			target = null;
		}
	}
	
	private void build(Map<BreakPoint, List<Integer>> pathMap, BreakpointValue inputValue, DecisionLocation location) {
		List<Integer> occurs = pathMap.get(bkpBuilder.getBreakPoint(location));
		BreakpointData bkpData = bkpDataMap.get(location);
		if (bkpData == null) {
			if (location.isLoop()) {
				bkpData = new LoopTimesData(location);
			} else {
				bkpData = new BranchSelectionData(location);
			}
			bkpDataMap.put(location, bkpData);
		}
		if (occurs == null) {
			bkpData.addFalseValue(inputValue);
			return;
		}
		if (location.isLoop()) {
			List<Integer> parentOccurs = pathMap.get(bkpBuilder.getParentBreakPoint(location));
			int cnt = 0;
			if (parentOccurs == null) {
				cnt = occurs.size();
			} else {
				cnt = calculateLoopTimes(occurs, parentOccurs);
			}
			if (cnt == 1) {
				((LoopTimesData)bkpData).addOneTimeValue(inputValue);
			} else {
				((LoopTimesData)bkpData).addMoreTimesValue(inputValue);
			}
		} else {
			((BranchSelectionData)bkpData).addTrueValue(inputValue);
		}
	}
	
	/*public BreakpointDataBuilder(Map<BreakPoint, List<DecisionLocation>> decisionMap){
		branchMap = new HashMap<BreakPoint, DecisionLocation>();
		loopMap = new HashMap<BreakPoint, DecisionLocation>();
		bkpDataMap = new HashMap<DecisionLocation, BreakpointData>();
		
		Set<Entry<BreakPoint, List<DecisionLocation>>> entries = decisionMap.entrySet();
		for (Entry<BreakPoint, List<DecisionLocation>> entry : entries) {
			BreakPoint breakPoint = entry.getKey();
			List<DecisionLocation> locations = entry.getValue();
			for (DecisionLocation location : locations) {
				if (location.isLoop()) {
					Assert.assertTrue(loopMap.get(breakPoint) == null, 
							"Two loops depend on line " + breakPoint.getLineNo());
					loopMap.put(breakPoint, location);
					bkpDataMap.put(location, new LoopTimesData(location));
				} else {
					Assert.assertTrue(branchMap.get(breakPoint) == null, 
							"Two branches depend on line " + breakPoint.getLineNo());
					branchMap.put(breakPoint, location);
					bkpDataMap.put(location, new BranchSelectionData(location));
				}
			}
		}
	}*/
	
	/*public void build(List<BreakPoint> path, BreakpointValue inputValue) {
		Set<DecisionLocation> locationSet = new HashSet<DecisionLocation>(bkpDataMap.keySet());
		
		if (loopMap.isEmpty()) {
			Set<BreakPoint> bkps = new HashSet<BreakPoint>(path);
			for (BreakPoint bkp : bkps) {
				DecisionLocation branch = branchMap.get(bkp);
				BranchSelectionData branchData = (BranchSelectionData) bkpDataMap.get(branch);
				branchData.addTrueValue(inputValue);
				locationSet.remove(branch);
			}
		} else {
			Map<BreakPoint, List<Integer>> pathMap = buildPathMap(path);
			Set<BreakPoint> bkps = pathMap.keySet();
			for (BreakPoint bkp : bkps) {
				DecisionLocation branch = branchMap.get(bkp);
				if (branch != null) {
					BranchSelectionData branchData = (BranchSelectionData) bkpDataMap.get(branch);
					branchData.addTrueValue(inputValue);
					locationSet.remove(branch);
				}
				DecisionLocation loop = loopMap.get(bkp);
				if (loop != null) {
					LoopTimesData loopData = (LoopTimesData) bkpDataMap.get(loop);
					List<Integer> occurs = pathMap.get(bkp);
					int max = 1;
					int first = occurs.get(0);
					if (first == 0) {
						max = occurs.size();
					} else {
						BreakPoint previous = path.get(first - 1);
						max = calculateLoopTimes(occurs, pathMap.get(previous));
					}
					if (max > 1) {
						loopData.addMoreTimesValue(inputValue);
					} else {
						loopData.addOneTimeValue(inputValue);
					}
					locationSet.remove(loop);
				}
			}
		}
		
		for (DecisionLocation location : locationSet) {
			BreakpointData data = bkpDataMap.get(location);
			data.addFalseValue(inputValue);
		}
	}*/

	private Map<BreakPoint, List<Integer>> buildPathMap(List<BreakPoint> path){
		Map<BreakPoint, List<Integer>> pathMap = new HashMap<BreakPoint, List<Integer>>();
		int index = 0;
		for (BreakPoint bkp : path) {
			List<Integer> occurs = pathMap.get(bkp);
			if (occurs == null) {
				occurs = new ArrayList<Integer>();
				pathMap.put(bkp, occurs);
			}
			occurs.add(index ++);
		}
		return pathMap;
	}
	
	private int calculateLoopTimes(List<Integer> occurs, List<Integer> parentOccurs) {
		int times = 0;
		int maxIdx = parentOccurs.size() - 1;
		for (int i = 0; i < maxIdx; i++) {
			times = 0;
			int first = parentOccurs.get(i);
			int second = parentOccurs.get(i + 1);
			for (Integer occur : occurs) {
				if (occur > first && occur < second) {
					times ++;
				}
				if (times > 1) {
					return times;
				}
			}
		}
		int last = parentOccurs.get(maxIdx);
		times = 0;
		for (Integer occur : occurs) {
			if (occur > last) {
				times ++;
			}
			if (times > 1) {
				return times;
			}
		}
		return times;
	}
	
	public List<BreakpointData> getResult() {
		if (result == null) {
			result = new ArrayList<BreakpointData>(bkpDataMap.values());
		}
		return result;
	}

}
