package GARVaverageEachRunResultEvaluation;

import java.io.PrintWriter;
import java.util.*;
 

public class EachRunAllocationResult {
 
    public static void writeHeader(PrintWriter w) {
        w.println("Round,Instance,Run,Seed,TaskID,VMID,Executed");
    }
 
    public static void writeRunAllocation(PrintWriter w, int round, int instance, int run, long seed,
                                          List<TaskVmPair> allocation) {
        List<TaskVmPair> sorted = new ArrayList<>(allocation);          // do not reorder the caller's list
        sorted.sort(Comparator.comparingInt(pair -> pair.task.id));
        for (TaskVmPair pair : sorted) {
            w.printf("AR%d,%d,%d,%d,%d,%d,%d%n", round, instance, run, seed,
                    pair.task.id, pair.vm.id, pair.task.isExecuted ? 1 : 0);
        }
    }
}
