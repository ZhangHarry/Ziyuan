package faultLocalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import faultLocalization.LineCoverageInfo.LineCoverageInfoComparator;
import sav.common.core.utils.CollectionUtils;
import sav.common.core.utils.ObjectUtils;
import sav.strategies.dto.ClassLocation;

public class FaultLocalizationReport {

	private CoverageReport coverageReport;
	private List<LineCoverageInfo> lineCoverageInfos;

	public CoverageReport getCoverageReport() {
		return coverageReport;
	}

	public void setCoverageReport(CoverageReport coverageReport) {
		this.coverageReport = coverageReport;
	}

	public List<LineCoverageInfo> getLineCoverageInfos() {
		return CollectionUtils.nullToEmpty(lineCoverageInfos);
	}

	public void setLineCoverageInfos(List<LineCoverageInfo> lineCoverageInfos) {
		this.lineCoverageInfos = lineCoverageInfos;
	}
	
	public List<LineCoverageInfo> getFirstRanks(int rank){
		List<LineCoverageInfo> result = new ArrayList<LineCoverageInfo>();
		
		int size = lineCoverageInfos.size();
		int run = 0;
		while(rank > 0 && run < size){
			//add linecoverage for current rank
			double maxSuspiciousness = lineCoverageInfos.get(run).getSuspiciousness();
			if (ObjectUtils.equalsWithNull(maxSuspiciousness, 0)) {
				break;
			}
			int i ;
			for(i = run; i < size && Double.compare(lineCoverageInfos.get(i).getSuspiciousness(), maxSuspiciousness) == 0; i++){
				result.add(lineCoverageInfos.get(i));
			}
			run = i;		
			
			rank--;
			
		}
		
		return result;
	}
	
	public List<ClassLocation> getFirstRanksLocation(int rank){
		List<LineCoverageInfo> lineCoverateInfos = getFirstRanks(rank);
		
		List<ClassLocation> result = new ArrayList<ClassLocation>();
		for(LineCoverageInfo lineCoverateInfo: lineCoverateInfos){
			result.add(lineCoverateInfo.getLocation());
		}
		
		return result;
	}
	
	public void sort(){
		Collections.sort(lineCoverageInfos, new LineCoverageInfoComparator());
	}
	
	public void setSuspiciousnessForAll(double value){
		for(LineCoverageInfo lineInfo: lineCoverageInfos){
			lineInfo.setSuspiciousness(value);
		}
	}

	@Override
	public String toString() {
		return lineCoverageInfos.toString();
	}

}
