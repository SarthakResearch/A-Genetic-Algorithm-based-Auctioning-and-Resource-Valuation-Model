package SourceCode;
import java.util.*;


	public class GeneticAlgorithm {
	    List<Task> tasks;
	    List<VM> vms;
	    int populationSize = 100;
	    int generations = 500;
	    double crossoverRate = 0.8;
	    double mutationRate = 0.1;
	    Random rand = new Random();
	    
	    int allocSize;
	    // MODIFICATION: flag detected automatically from data
	    boolean tasksExceedVms;

	    public GeneticAlgorithm(List<Task> tasks, List<VM> vms) {
	        this.tasks = tasks;
	        this.vms = vms;
	        // MODIFICATION: detect scenario automatically
	        // No manual switching needed — just read the sizes
	        this.tasksExceedVms = tasks.size() > vms.size();
	        this.allocSize = Math.min(tasks.size(), vms.size());
	    }

	    
	    
	    
	    
	    
	    public List<Chromosome> initializePopulation() {
	        List<Chromosome> population = new ArrayList<>();
	        
	        for (int i = 0; i < populationSize; i++) {
	            Chromosome c = new Chromosome(allocSize);
	 
	            // Create positions list: always 0 to allocSize-1
	            List<Integer> positions = new ArrayList<>();
	            for (int j = 0; j < allocSize; j++) {
	                positions.add(j);
	            }
	            Collections.shuffle(positions, rand);
	 
	            for (int j = 0; j < allocSize; j++) {
	                c.gene[j] = positions.get(j);
	            }
	 
	            c.fitness = evaluateFitness(c);
	            population.add(c);
	        }

//	       

	        return population;
	    }

	    
	    
	    
	    
	    
	    
	    
	    
	    public double evaluateFitness(Chromosome c) {
	        double[] vmLoad = new double[vms.size()];
	        //double[] vmCost = new double[vms.size()];
	        double totalRevenue = 0;
	        double slaPenalty = 0;
	        
	        for (int i = 0; i < allocSize; i++) {
	            Task task;
	            VM vm;
	 
	            if (!tasksExceedVms) {
	                // tasks <= vms
	                // task i gets VM at position gene[i]
	                task = tasks.get(i);
	                vm = vms.get(c.gene[i]);
	            } else {
	                // tasks > vms
	                // VM i gets task at position gene[i]
	                task = tasks.get(c.gene[i]);
	                vm = vms.get(i);
	            }
	 
	            double execTime = (double) task.length / vm.mips;
	            double cost = execTime * vm.rate;
	 
	            vmLoad[i] += execTime;
	 
	            // SLA and revenue logic — completely unchanged
	            if (execTime > task.deadline && cost > task.budget) {
	                slaPenalty += 10;
	            }
	            if (cost <= task.budget && execTime <= task.deadline) {
	                totalRevenue += task.budget;
	            }
	            if (execTime <= task.deadline && cost > task.budget
	                    && task.preference == 'T') {
	                totalRevenue += task.budget / 2;
	            }
	            if (execTime > task.deadline && cost <= task.budget
	                    && task.preference == 'C') {
	                totalRevenue += task.budget / 2;
	            }
	            if (execTime <= task.deadline && cost > task.budget
	                    && task.preference == 'C') {
	                slaPenalty += 5;
	            }
	            if (execTime > task.deadline && cost <= task.budget
	                    && task.preference == 'T') {
	                slaPenalty += 5;
	            }
	        }
	        
//	       

	        // Compute load standard deviation
	        double meanLoad = Arrays.stream(vmLoad).average().orElse(1);
	        double variance = 0;
	        for (double load : vmLoad) {
	            variance += Math.pow(load - meanLoad, 2);
	        }
	        double loadDispersion = Math.sqrt(variance / allocSize);

	        if (loadDispersion == 0) loadDispersion = 0.1; // prevent division by 0

	        return (totalRevenue / loadDispersion) - slaPenalty;
	    }
	    
	    
	    
	    
	    
	    
	    
	    

	    public Chromosome tournamentSelection(List<Chromosome> population) {
	        int tournamentSize = 3;
	        Chromosome best = null;
	        for (int i = 0; i < tournamentSize; i++) {
	            Chromosome c = population.get(rand.nextInt(population.size()));
	            if (best == null || c.fitness > best.fitness) {
	                best = c;
	            }
	        }
	        return best.clone();
	    }
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    public void crossover(Chromosome parent1, Chromosome parent2) {
	        if (rand.nextDouble() >= crossoverRate) return;

	        int size = allocSize;
	        int start = rand.nextInt(size);
	        int end = rand.nextInt(size - start) + start;
	        
	        int[] child1 = new int[size];
	        int[] child2 = new int[size];
	        Arrays.fill(child1, -1);
	        Arrays.fill(child2, -1);
	 
	        for (int i = start; i <= end; i++) {
	            child1[i] = parent1.gene[i];
	            child2[i] = parent2.gene[i];
	        }
	 
	        // fillRemaining works correctly now because
	        // gene values are always 0 to size-1
	        fillRemaining(child1, parent2.gene, start, end);
	        fillRemaining(child2, parent1.gene, start, end);
	 
	        parent1.gene = child1;
	        parent2.gene = child2;
	        
//	        

	        
	    }
	    
//	    
	    
	    
	    
	    
	    private void fillRemaining(int[] child, int[] source, int start, int end) {
	    	
	        int size = child.length;
	        Set<Integer> used = new HashSet<>();
	        for (int i = start; i <= end; i++) {
	            used.add(child[i]);
	        }

	        int insertPos = (end + 1) % size;

	        for (int i = 0; i < size; i++) {
	            int candidate = source[(end + 1 + i) % size];
	            if (!used.contains(candidate)) {
	                while (child[insertPos] != -1) {
	                    insertPos = (insertPos + 1) % size;
	                }
	                child[insertPos] = candidate;
	                used.add(candidate);
	            }
	        }
	    }

	    
	    
	    private boolean contains(int[] arr, int val) {
	        for (int j : arr) {
	            if (j == val) return true;
	        }
	        return false;
	    }
	    
	   
	    
	    
	    public void mutate(Chromosome c) {
	    	if (rand.nextDouble() < mutationRate) {
	            // Simple swap — same as original
	            // Works for both cases because gene is always
	            // a permutation of 0..allocSize-1
	            int i = rand.nextInt(c.gene.length);
	            int j = rand.nextInt(c.gene.length);
	            int temp = c.gene[i];
	            c.gene[i] = c.gene[j];
	            c.gene[j] = temp;
	        }
	    	
//	        
	    }
	    
	    
	    
	    
	    
	    public Chromosome run() {
	        List<Chromosome> population = initializePopulation();
	        Chromosome best = population.get(0);

	        for (int gen = 0; gen < generations; gen++) {
	            List<Chromosome> newPop = new ArrayList<>();

	            while (newPop.size() < populationSize) {
	                Chromosome p1 = tournamentSelection(population);
	                Chromosome p2 = tournamentSelection(population);

	                crossover(p1, p2);
	                mutate(p1);
	                mutate(p2);

	                p1.fitness = evaluateFitness(p1);
	                p2.fitness = evaluateFitness(p2);

	                newPop.add(p1);
	                newPop.add(p2);
	            }

	            population = newPop;

	            for (Chromosome c : population) {
	                if (c.fitness > best.fitness) {
	                    best = c.clone();
	                }
	            }

	            // Optional: print progress
	            // System.out.println("Generation " + gen + " Best Fitness: " + best.fitness);
	        }

	        return best;
	    }
	    
	    
  
	    
	    
	    public List<TaskVmPair> buildFinalAllocation(Chromosome best) {
	    	
	    	List<TaskVmPair> finalAllocationList = new ArrayList<>();
	    	 
	        for (int i = 0; i < allocSize; i++) {
	            Task task;
	            VM vm;
	 
	            if (!tasksExceedVms) {
	                // tasks <= vms
	                // task i → VM at position gene[i]
	                task = tasks.get(i);
	                vm = vms.get(best.gene[i]);
	            } else {
	                // tasks > vms
	                // VM i → task at position gene[i]
	                task = tasks.get(best.gene[i]);
	                vm = vms.get(i);
	            }
	 
	            finalAllocationList.add(new TaskVmPair(task, vm));
	        }
	 
	        // Print scenario summary
	        System.out.println("==========================================");
	        System.out.println("Tasks submitted : " + tasks.size());
	        System.out.println("VMs available   : " + vms.size());
	        System.out.println("Tasks allocated : " + allocSize);
	        System.out.println("Tasks rejected  : "
	                + Math.max(0, tasks.size() - vms.size()));
	        System.out.println("==========================================");
	 
	        return finalAllocationList;
//	        
	    }
	    
	    
	    
	}
