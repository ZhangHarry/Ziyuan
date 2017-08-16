

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** 
* @author ZhangHr 
*/
public class LogExplorer {
	
	public static void main(String[] args) throws IOException  {
		String path = "E:\\hairui\\eclipse-java-mars-clean\\eclipse\\learntest-eclipse.log.Statistics0804";
		String recorder = "E:\\hairui\\eclipse-java-mars-clean\\eclipse\\learned0804.log";
		merge(path);
		getLearnedMethod(path, recorder);
	}
	
	private static void getLearnedMethod(String path, String recorder) {
		File file = new File(path);
		FileWriter writer = null;
        BufferedReader reader = null;
        try {
        	writer = new FileWriter(recorder, true);;
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            String methodInfo = "", methodSignal = "WORKING METHOD:",  
            		formulaSignal = "=============learned multiple cut:", ignoredFormula = "=============learned multiple cut: null";
            HashMap<String, Integer> recordMap = new HashMap<>();
            while ((tempString = reader.readLine()) != null) {
            	int beginIndex = tempString.indexOf(methodSignal);
            	if (beginIndex >= 0) {
					methodInfo = tempString.substring(beginIndex+methodSignal.length()+1);
				}else if (tempString.contains(formulaSignal) && !tempString.contains(ignoredFormula)) {
					if (!recordMap.containsKey(methodInfo)) {
						recordMap.put(methodInfo, line);
			            writer.write(methodInfo+", line : "+line+"\n");
					}
					System.out.println(methodInfo+" is learned");
				}
                line++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
            if (writer != null) {
                try {
                	writer.close();
                } catch (IOException e1) {
                }
            }
        }
	}

	private static void merge(String path) throws IOException {
		List<String> files = new ArrayList<>();
		files.add("E:\\hairui\\eclipse-java-mars-clean\\eclipse\\learntest-eclipse.log.2017-08-03");
		files.add("E:\\hairui\\eclipse-java-mars-clean\\eclipse\\learntest-eclipse.log");
		merge(files, path);
	}

	public static String merge(List<String> files, String path) throws IOException  {
		FileWriter writer = null;
		try {
			writer = new FileWriter(path, true);
			for (String file : files) {
				readFileByLines(file, writer);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {			
			writer.close();
		}
		return path;
	}
	
	public static void readFileByLines(String fileName, FileWriter writer) {
        File file = new File(fileName);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            while ((tempString = reader.readLine()) != null) {
	            writer.write(tempString+"\n");
                System.out.println(String.format("file : %s, line : %d", fileName, line));
                line++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
    }
	
}
