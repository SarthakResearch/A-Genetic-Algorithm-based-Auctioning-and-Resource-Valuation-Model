package GARVaverageEachRunResult;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class EachRunAllocationResult {
	
	private static final String FILE_NAME = "EachRunResults.csv";
	
	public static void writeRunResult (int runID, List<TaskVmPair> finalAllocationList, double CSR, double PSR, double SW) throws IOException {
		
		File file = new File(FILE_NAME);
		
		boolean isNewFile = file.createNewFile();
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
		
		if (isNewFile) {

            writer.write("Run_ID");

            for (int i = 0; i < 100; i++) {
                writer.write(",Task_" + i);
            }

            writer.write(",CSR,PSR,SW");
            writer.newLine();
        }
		
		finalAllocationList.sort(Comparator.comparingInt(pair -> pair.task.id));
		
		writer.write(String.valueOf(runID)); 				//column 1
		
		for (TaskVmPair pair : finalAllocationList) {
            writer.write("," + pair.vm.id);					//column 2-101
        }
		
		writer.write("," + CSR + "," + PSR + "," + SW);		//column 102, 103, 104
		
		writer.newLine();
		
		writer.close();
		
		
		
	}

}
