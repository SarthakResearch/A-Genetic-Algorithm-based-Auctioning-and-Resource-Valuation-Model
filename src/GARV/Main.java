package GARVaverageEachRunResult;
import java.util.*;

public class Main {
    static List<Task> tasks = new ArrayList<>();
    static List<VM> vms = new ArrayList<>();
    static List<TaskVmPair> finalAllocationList = new ArrayList<>();
    static double rawScore = 0;

    public static void main(String[] args) {
        try {
            tasks = Utils.readTasks("TaskDataset100.csv");
            vms = Utils.readVMs("100VmDataset.csv");

            int numRuns = 10;

            // accumulators for averaging
            double sumTSR=0, sumPSR=0, sumIR=0, sumAIV=0, sumCR=0, sumACC=0, sumOPR=0, sumAOP=0;
            double sumMakespan=0, sumSocialWelfare=0, sumExecTime=0, sumRawScore=0;

            for (int run = 1; run <= numRuns; run++) {

                // reset shared/static state before each run
                Results.reset();
                for (Task t : tasks) t.reset();
                for (VM v : vms) v.reset();

                GeneticAlgorithm ga = new GeneticAlgorithm(tasks, vms);

                long startTime = System.currentTimeMillis();
                Chromosome bestSolution = ga.run();
                rawScore = ga.computeRawRevenuePenalty(bestSolution);
                

                finalAllocationList = ga.buildFinalAllocation(bestSolution);
                
                long endTime = System.currentTimeMillis();

                Results.executionTimeSec = (endTime - startTime) / 1000.0;

                System.out.println("===== RUN " + run + " =====");
                for (TaskVmPair pair : finalAllocationList) {
                    System.out.println("Task " + pair.task.id + " -> VM " + pair.vm.id);
                }

                Execution.performExecution(finalAllocationList);
                Results.printResults();   // prints this run's numbers, and fills Results.TSR, PSR, etc.
                
                EachRunAllocationResult.writeRunResult(run, finalAllocationList, Results.TSR, Results.PSR, Results.socialWelfare);

                // accumulate this run's final metrics
                sumTSR += Results.TSR;
                sumPSR += Results.PSR;
                sumIR += Results.IR;
                sumAIV += Results.AIV;
                sumCR += Results.CR;
                sumACC += Results.ACC;
                sumOPR += Results.OPR;
                sumAOP += Results.AOP;
                sumMakespan += Results.makespan;
                sumSocialWelfare += Results.socialWelfare;
                sumExecTime += Results.executionTimeSec;
                sumRawScore += rawScore;
                
                
               
            }

            System.out.println("\n==================================================");
            System.out.println("AVERAGE RESULTS OVER " + numRuns + " RUNS");
            System.out.println("==================================================");
            System.out.println("Average TSR = " + String.format("%.3f", sumTSR / numRuns));
            System.out.println("Average PSR = " + String.format("%.3f", sumPSR / numRuns));
            System.out.println("Average IR = " + String.format("%.3f", sumIR / numRuns));
            System.out.println("Average AIV = " + String.format("%.3f", sumAIV / numRuns));
            System.out.println("Average CR = " + String.format("%.3f", sumCR / numRuns));
            System.out.println("Average ACC = " + String.format("%.3f", sumACC / numRuns));
            System.out.println("Average OPR = " + String.format("%.3f", sumOPR / numRuns));
            System.out.println("Average AOP = " + String.format("%.3f", sumAOP / numRuns));
            System.out.println("Average Makespan = " + String.format("%.3f", sumMakespan / numRuns));
            System.out.println("Average Social Welfare = " + String.format("%.3f", sumSocialWelfare / numRuns));
            System.out.println("Average Execution Time (s) = " + String.format("%.5f", sumExecTime / numRuns));
            System.out.println("Average Raw Revenue-Penalty = " + String.format("%.3f", sumRawScore / numRuns));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
