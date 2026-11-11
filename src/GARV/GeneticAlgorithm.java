package GARVaverageEachRunResultEvaluation;
import java.util.*;


	public class GeneticAlgorithm {
	    List<Task> tasks;
	    List<VM> vms;
	    int populationSize = 100;
	    int generations = 1000;
	    double crossoverRate = 0.9;           // tuned (DOE Stage II)
	    double mutationRate = 0.2;            // tuned (DOE Stage II)

	    int tournamentSize = 5;               // tuned (DOE Stage II)
	    double OMEGA = 100;                    // SLA penalty constant, tuned (DOE Stage III)
	    boolean USE_ELITISM = true;           // R4: set false for previous behaviour
	    boolean VERBOSE = true;               // DOE: set false to suppress the allocation summary

	    // Convergence check: if checkpointEvery > 0, the best-so-far chromosome
	    // is stored every checkpointEvery generations (key = generation number).
	    int checkpointEvery = 0;
	    Map<Integer, Chromosome> checkpoints = new LinkedHashMap<>();
	    Random rand;
	 
	    int allocSize;        // S = min(|tasks|, |vms|) : number of task-VM pairs
	    int chromLength;      // L = max(|tasks|, |vms|) : chromosome length (R1)
	    boolean tasksExceedVms;

	    public GeneticAlgorithm(List<Task> tasks, List<VM> vms) {
	        this(tasks, vms, new Random());
	    }
	    
	    public GeneticAlgorithm(List<Task> tasks, List<VM> vms, long seed) {
	        this(tasks, vms, new Random(seed));
	    }
	    
	    private GeneticAlgorithm(List<Task> tasks, List<VM> vms, Random rand) {
	        this.tasks = tasks;
	        this.vms = vms;
	        this.rand = rand;
	        this.tasksExceedVms = tasks.size() > vms.size();
	        this.allocSize = Math.min(tasks.size(), vms.size());
	        this.chromLength = Math.max(tasks.size(), vms.size());   // R1
	    }
	    
	    private Task taskAt(Chromosome c, int i) {
	        return tasksExceedVms ? tasks.get(c.gene[i]) : tasks.get(i);
	    }
	 
	    private VM vmAt(Chromosome c, int i) {
	        return tasksExceedVms ? vms.get(i) : vms.get(c.gene[i]);
	    }

	    
	    
	    
	    
	    
	    public List<Chromosome> initializePopulation() {
	        List<Chromosome> population = new ArrayList<>();
	        for (int i = 0; i < populationSize; i++) {
	            Chromosome c = new Chromosome(chromLength);
	            List<Integer> positions = new ArrayList<>();
	            for (int j = 0; j < chromLength; j++) positions.add(j);
	            Collections.shuffle(positions, rand);
	            for (int j = 0; j < chromLength; j++) c.gene[j] = positions.get(j);
	            c.fitness = evaluateFitness(c);
	            population.add(c);
	        }
	        return population;
	    }


	    
	    public double evaluateFitness(Chromosome c) {
	        double[] pairLoad = new double[allocSize];   // R2: one entry per allocated pair
	        double totalRevenue = 0;
	        double slaPenalty = 0;
	 
	        for (int i = 0; i < allocSize; i++) {
	            Task task = taskAt(c, i);
	            VM vm = vmAt(c, i);
	 
	            double execTime = (double) task.length / vm.mips;
	            double cost = execTime * vm.rate;
	            pairLoad[i] = execTime;
	 
	            // SLA and revenue logic — unchanged
	            if (execTime > task.deadline && cost > task.budget) {
	                slaPenalty += OMEGA;
	            }
	            if (cost <= task.budget && execTime <= task.deadline) {
	                totalRevenue += task.budget;
	            }
	            if (execTime <= task.deadline && cost > task.budget && task.preference == 'T') {
	                totalRevenue += task.budget / 2;
	            }
	            if (execTime > task.deadline && cost <= task.budget && task.preference == 'C') {
	                totalRevenue += task.budget / 2;
	            }
	            if (execTime <= task.deadline && cost > task.budget && task.preference == 'C') {
	                slaPenalty += OMEGA / 2;
	            }
	            if (execTime > task.deadline && cost <= task.budget && task.preference == 'T') {
	                slaPenalty += OMEGA / 2;
	            }
	        }
	 
	        // R2: mean and variance over the allocSize allocated pairs only
	        double meanLoad = 0;
	        for (double load : pairLoad) meanLoad += load;
	        meanLoad /= allocSize;
	        double variance = 0;
	        for (double load : pairLoad) variance += (load - meanLoad) * (load - meanLoad);
	        double loadDispersion = Math.sqrt(variance / allocSize);
	        if (loadDispersion == 0) loadDispersion = 0.1;   // prevent division by 0
	 
	        return (totalRevenue / loadDispersion) - slaPenalty;
	    }
	    
	    
	 // ============================================================
	    // ADDED FOR ILP COMPARISON (reporting only — does NOT affect
	    // the GA's search, selection, crossover, or mutation in any way)
	    // ============================================================
	    // Computes the same totalRevenue and slaPenalty as evaluateFitness(),
	    // but returns (totalRevenue - slaPenalty) directly, WITHOUT dividing
	    // by loadDispersion. This is the quantity to compare against the
	    // ILP-optimal objective value.
	    public double computeRawRevenuePenalty(Chromosome c) {
	        double totalRevenue = 0;
	        double slaPenalty = 0;
	        for (int i = 0; i < allocSize; i++) {
	            Task task = taskAt(c, i);
	            VM vm = vmAt(c, i);
	            double execTime = (double) task.length / vm.mips;
	            double cost = execTime * vm.rate;
	 
	            if (execTime > task.deadline && cost > task.budget) slaPenalty += OMEGA;
	            if (cost <= task.budget && execTime <= task.deadline) totalRevenue += task.budget;
	            if (execTime <= task.deadline && cost > task.budget && task.preference == 'T') totalRevenue += task.budget / 2;
	            if (execTime > task.deadline && cost <= task.budget && task.preference == 'C') totalRevenue += task.budget / 2;
	            if (execTime <= task.deadline && cost > task.budget && task.preference == 'C') slaPenalty += OMEGA / 2;
	            if (execTime > task.deadline && cost <= task.budget && task.preference == 'T') slaPenalty += OMEGA / 2;
	        }
	        return totalRevenue - slaPenalty;
	    }
	    
	    
	    
	    
	    

	    public Chromosome tournamentSelection(List<Chromosome> population) {
	        Chromosome best = null;
	        for (int i = 0; i < tournamentSize; i++) {
	            Chromosome c = population.get(rand.nextInt(population.size()));
	            if (best == null || c.fitness > best.fitness) best = c;
	        }
	        return best.clone();
	    }
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    public void crossover(Chromosome parent1, Chromosome parent2) {
	        if (rand.nextDouble() >= crossoverRate) return;
	 
	        int size = chromLength;
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
	        fillRemaining(child1, parent2.gene, start, end);
	        fillRemaining(child2, parent1.gene, start, end);
	 
	        parent1.gene = child1;
	        parent2.gene = child2;
	    }

	    
	    
	    
	    
	    private void fillRemaining(int[] child, int[] source, int start, int end) {
	        int size = child.length;
	        Set<Integer> used = new HashSet<>();
	        for (int i = start; i <= end; i++) used.add(child[i]);
	 
	        int insertPos = (end + 1) % size;
	        for (int i = 0; i < size; i++) {
	            int candidate = source[(end + 1 + i) % size];
	            if (!used.contains(candidate)) {
	                while (child[insertPos] != -1) insertPos = (insertPos + 1) % size;
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
	            int i = rand.nextInt(c.gene.length);
	            int j = rand.nextInt(c.gene.length);
	            int temp = c.gene[i];
	            c.gene[i] = c.gene[j];
	            c.gene[j] = temp;
	        }
	    }
	    
	    
	    
	    
	    
	    public Chromosome run() {
	        List<Chromosome> population = initializePopulation();
	 
	        // R3: start from the true best of the initial population
	        Chromosome best = population.get(0);
	        for (Chromosome c : population) {
	            if (c.fitness > best.fitness) best = c;
	        }
	        best = best.clone();
	 
	        for (int gen = 0; gen < generations; gen++) {
	            List<Chromosome> newPop = new ArrayList<>();
	 
	            // R4: carry the best-so-far chromosome into the new generation
	            if (USE_ELITISM) newPop.add(best.clone());
	 
	            while (newPop.size() < populationSize) {
	                Chromosome p1 = tournamentSelection(population);
	                Chromosome p2 = tournamentSelection(population);
	 
	                crossover(p1, p2);
	                mutate(p1);
	                mutate(p2);
	 
	                p1.fitness = evaluateFitness(p1);
	                p2.fitness = evaluateFitness(p2);
	 
	                newPop.add(p1);
	                if (newPop.size() < populationSize) newPop.add(p2);   // keep size exact
	            }
	 
	            population = newPop;
	 
	            for (Chromosome c : population) {
	                if (c.fitness > best.fitness) best = c.clone();
	            }

	            // Convergence check: record the best-so-far at each checkpoint
	            if (checkpointEvery > 0 && (gen + 1) % checkpointEvery == 0) {
	                checkpoints.put(gen + 1, best.clone());
	            }
	        }
	        return best;
	    }
	    
	    
	    
	    
	    
	    
	    
	    
	    
	    
	 // ============================================================
	    // MODIFICATION: NEW METHOD — buildFinalAllocation()
	    // ============================================================
	    // This replaces the allocation-building logic in Main.java
	    // Call this after ga.run() to get the final TaskVmPair list
	    // Handles both scenarios automatically
	    // ============================================================
	    public List<TaskVmPair> buildFinalAllocation(Chromosome best) {
	        List<TaskVmPair> finalAllocationList = new ArrayList<>();
	        for (int i = 0; i < allocSize; i++) {
	            finalAllocationList.add(new TaskVmPair(taskAt(best, i), vmAt(best, i)));
	        }
	 
	        if (VERBOSE) {
	            System.out.println("==========================================");
	            System.out.println("Tasks submitted : " + tasks.size());
	            System.out.println("VMs available   : " + vms.size());
	            System.out.println("Tasks allocated : " + allocSize);
	            System.out.println("Tasks rejected  : " + Math.max(0, tasks.size() - vms.size()));
	            System.out.println("Chromosome len  : " + chromLength);
	            System.out.println("==========================================");
	        }
	 
	        return finalAllocationList;
	    }
	    
	    public List<Integer> rejectedTaskIndices(Chromosome best) {
	        List<Integer> rejected = new ArrayList<>();
	        if (tasksExceedVms) {
	            for (int i = allocSize; i < chromLength; i++) rejected.add(best.gene[i]);
	        }
	        return rejected;
	    }
	    
	    
	    
	}