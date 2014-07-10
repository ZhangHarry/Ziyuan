/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.plugin.analyzer.marker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import tzuyu.plugin.commons.constants.PluginConstants;
import tzuyu.plugin.commons.utils.IStatusUtils;

/**
 * @author LLT
 * 
 */
public class SlicingMarker {
	public static final String NAME = PluginConstants.ID_SLICING_MARKER;
	
	public static void clearMarker(IJavaProject project) {
		try {
			project.getResource().deleteMarkers(PluginConstants.ID_SLICING_MARKER,
					true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void createMarkers(final IJavaProject project, final List<BreakPoint> bkps,
			IProgressMonitor monitor) {
		WorkspaceJob wsjob = new WorkspaceJob("Mark slicing statement result") {
			
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor)
					throws CoreException {
				new SlicingMarkerRunnable(bkps, project).run(monitor);
				return IStatusUtils.afterRunning(monitor);
			}
		};
		wsjob.setRule(project.getSchedulingRule());
		wsjob.setSystem(true);
		wsjob.setUser(false);
		wsjob.schedule();
	}
	
	public static List<IMarker> createMarker(List<BreakPoint> bkps,
			IJavaProject project) {
		List<IMarker> result = new ArrayList<>();
		for (BreakPoint bkp : bkps) {
			result.add(createMarker(bkp, project));
		}
		return result;
	}

	public static IMarker createMarker(BreakPoint bkp, IJavaProject project) {
		try {
			IType type = project.findType(bkp.getClassCanonicalName());
			return createMarker(type.getResource());
		} catch (JavaModelException e) {
			// TODO LLT exception handling
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO LLT exception handling
			e.printStackTrace();
		}
		return null;
	}

	public static IMarker createMarker(IResource res) throws CoreException {
		return res.createMarker(PluginConstants.ID_SLICING_MARKER);
	}

	public static List<IMarker> findMarkers(IResource resource) {
		try {
			return Arrays.asList(resource.findMarkers(
					PluginConstants.ID_SLICING_MARKER, true,
					IResource.DEPTH_ZERO));
		} catch (CoreException e) {
			return new ArrayList<IMarker>();
		}
	}

	public static class SlicingMarkerRunnable implements IWorkspaceRunnable {
		private List<BreakPoint> bkps;
		private IJavaProject project;

		public SlicingMarkerRunnable(List<BreakPoint> bkps, IJavaProject project) {
			this.bkps = bkps;
			this.project = project;
		}

		@Override
		public void run(IProgressMonitor monitor) throws CoreException {
			for (BreakPoint bkp : bkps) {
				IMarker marker = SlicingMarker.createMarker(bkp, project);
				marker.setAttributes(toAttributes(bkp));
	            marker.setAttribute(IMarker.CHAR_START, null);
	            marker.setAttribute(IMarker.CHAR_END, null);
			}
		}

		private Map<String, Object> toAttributes(BreakPoint bkp) {
			Map<String, Object> atts = new HashMap<>();
			atts.put(IMarker.LINE_NUMBER, bkp.getLineNo());
			return atts;
		}
	}
}
