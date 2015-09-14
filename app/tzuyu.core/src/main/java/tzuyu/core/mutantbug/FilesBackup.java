/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.core.mutantbug;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import mutation.utils.FileUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sav.common.core.SavRtException;

/**
 * @author LLT
 *
 */
public class FilesBackup {
	private static Logger log = LoggerFactory.getLogger(FilesBackup.class);
	private File backupDir;
	/* map to store files and backup files*/
	private Map<File, File> backupMap;
	
	private FilesBackup(){
		backupMap = new HashMap<File, File>();
	}
	
	public void backup(String filePath) {
		backup(new File(filePath));
	}

	public void backup(File file) {
		ensureOpen();
		try {
			File backupFile = FileUtils.copyFileToDirectory(file,
					backupDir, true);
			backupMap.put(file, backupFile);
			log.debug("backup ", file.getAbsolutePath());
		} catch (IOException e) {
			throw new SavRtException(e);
		}
	}
	
	public void backup(List<File> files) {
		for (File file : files) {
			backup(file);
		}
	}
	
	private void ensureOpen() {
		if (isClose()) {
			throw new SavRtException("FileBackup is not open!");
		}
	}

	public void restoreAll() {
		for (Entry<File, File> entry : backupMap.entrySet()) {
			restore(entry.getKey(), entry.getValue());
		}
		backupMap.clear();
	}
	
	public void restore(File file) {
		File backupFile = backupMap.remove(file);
		restore(file, backupFile);
	}
	
	public void restore(List<File> files) {
		for (File file : files) {
			restore(file);
		}
	}

	private void restore(File file, File backupFile) {
		if (backupFile == null) {
			throw new SavRtException("Cannot find backup for file: ", file.getAbsolutePath());
		}
		try {
			FileUtils.copyFile(backupFile, file, true);
			log.debug("restore ", file.getAbsolutePath());
		} catch (IOException e) {
			throw new SavRtException(e);
		}
		backupFile.delete();
	}
	
	public void open() {
		backupDir = FileUtils.createTempFolder("backup");
		log.debug("create backup folder ", backupDir.getAbsolutePath());
	}
	
	public void close() {
		try {
			org.apache.commons.io.FileUtils.deleteDirectory(backupDir);
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		backupDir = null;
	}
	
	public boolean isClose() {
		return backupDir == null;
	}
	
	public static FilesBackup startBackup() {
		FilesBackup backup = new FilesBackup();
		backup.open();
		return backup;
	}

}
