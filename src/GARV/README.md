# GARV — Core Mechanism and Evaluation Runner

This folder contains the implementation of **GARV (Genetic Algorithm based Auctioning and Resource Valuation Model)**: the genetic algorithm used for winner determination, the performance- and preference-aware pricing model, the evaluation metrics, and the runner that produced the GARV results reported in the paper (Sections 6–8).

## Contents

| File | Role |
|---|---|
| `Task.java` | A user task: length (MI), budget, deadline and preference (`T` = deadline, `C` = budget). |
| `VM.java` | A provider VM: processing speed (MIPS) and usage rate (cost units per second). |
| `TaskVmPair.java` | One allocated task–VM pair. |
| `Chromosome.java` | A GA chromosome (permutation of gene indices) and its fitness. |
| `Utils.java` | Reads the task and VM CSV files of an instance. |
| `GeneticAlgorithm.java` | Winner determination: encoding, fitness, tournament selection, order crossover, swap mutation, elitism. Also computes the Revenue − Penalty value used in the optimality gap analysis. |
| `Execution.java` | Executes the allocated pairs and applies the pricing model: payments, provider incentives, user compensation and discards. |
| `Results.java` | Computes the evaluation metrics. |
| `EachRunAllocationResult.java` | Writes the final allocation of every run. |
| `Main.java` | Evaluation runner: runs GARV on all instances of one auction round and writes the result files. |

## GA configuration

The default values in `GeneticAlgorithm.java` are the final configuration obtained from the DOE:

| Parameter | Value |
|---|---|
| Population size | 100 |
| Generations | 1000 |
| Crossover rate | 0.9 |
| Mutation rate | 0.2 |
| Tournament size (η) | 5 |
| SLA penalty constant (ω) | 100 |
| Elitism | enabled (best-so-far chromosome carried into each generation) |

**Encoding.** A chromosome is a permutation of length L = max(|tasks|, |VMs|). Its first S = min(|tasks|, |VMs|) genes form the allocated pairs. When tasks ≤ VMs, gene position *i* is task *i* and its value is the assigned VM. When tasks > VMs, gene position *j* is VM *j* and its value is the assigned task. The remaining genes hold the unallocated tasks (or VMs). Crossover and mutation act on all L genes, so the GA itself selects which tasks (or VMs) take part in the allocation.

**Fitness.** (Revenue / loadDispersion) − Penalty, computed over the S allocated pairs, where loadDispersion is the standard deviation of their execution times (clamped to 0.1 if it is zero).

## Input data

Each instance consists of two CSV files from the `datasets` folder of this repository:

- `<INSTANCE>_Tasks.csv`: `Task ID,Length,Budget,Deadline,Pref`
- `<INSTANCE>_VMs.csv`: `VM_ID,MIPS,Rate`

The evaluation set has 30 instances per auction round (`EVAL_AR<r>_I01` … `EVAL_AR<r>_I30`, r = 1…5), each with 100 VMs and 50, 100, 150, 200 or 250 tasks for AR1–AR5.

## How to run

Requirements: Java 8 or later; no external libraries.

1. Set the three constants at the top of `Main.java`:
   - `AR_FOLDER`: the folder of one auction round, e.g. `datasets/evaluation/AR1`
   - `OUT_DIR`: the folder where the result files are written
   - `NUM_RUNS`: independent runs per instance (10 in the paper)
2. Compile and run from this folder:
   ```
   javac -d bin *.java
   java -cp bin <package>.Main
   ```
   Replace `<package>` with the package name declared at the top of the source files.
3. Repeat for AR1–AR5. Each round is run separately.

**Reproducibility.** Every run uses a recorded seed, `seed = 100000·R + 100·i + r` (round R, instance i, run r), so all results can be reproduced exactly. Only the GA's winner determination (`ga.run()`) is timed.

## Output files (per auction round, in `OUT_DIR`)

| File | Content |
|---|---|
| `GARV_AR<r>_Runs.csv` | One row per run: instance, sizes, run, seed and all metrics. |
| `GARV_AR<r>_InstanceMeans.csv` | One row per instance: the mean of each metric over its runs, plus its run-to-run SD (`SD_<metric>`). |
| `GARV_AR<r>_Summary.csv` | One row per metric: mean, SD and 95% confidence interval over the 30 instances, and the mean run-to-run SD. |
| `GARV_AR<r>_Allocations.csv` | The final allocation of every run: `Round,Instance,Run,Seed,TaskID,VMID,Executed`. |
