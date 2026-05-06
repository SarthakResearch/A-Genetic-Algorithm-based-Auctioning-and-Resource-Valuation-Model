package SourceCode;

import java.io.*;
import java.util.*;

public class Utils {
	public static List<Task> readTasks(String filePath) throws IOException {
        List<Task> tasks = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line = br.readLine(); // skip header
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            tasks.add(new Task(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Double.parseDouble(parts[3]),
                parts[4].charAt(0)
            ));
        }
        return tasks;
    }

    public static List<VM> readVMs(String filePath) throws IOException {
        List<VM> vms = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line = br.readLine(); // skip header
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            vms.add(new VM(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Double.parseDouble(parts[2])
            ));
        }
        return vms;
    }
}
