# GARV — Parameter Tuning and Result Analysis

This folder contains the programs used to **tune GARV's GA parameters** and to **analyse the evaluation results**. Together, they produce the tables and figures of Sections 6.4–8 of the paper. The classes fall into three groups:

| Group | Classes | Runs the GA? | Input |
|---|---|---|---|
| A. Parameter tuning (DOE) | `DOERunner`, `AnovaAnalysis`, `ConvergenceCheckAR5`, `ConfirmationCheck` | yes (except `AnovaAnalysis`) | tuning instances |
| B. Scalability | `ScalabilityRunner`, `ScalabilityGraphs` | `ScalabilityRunner` only | one evaluation instance + evaluation results |
| C. Evaluation analysis | `EvaluationGraphs`, `SummaryTable`, `StatisticalTests`, `OptimalityGap`, `StabilityAnalysis` | no (read the CSV results only) | evaluation results |

> **Compilation note.** The classes that run the GA (`DOERunner`, `ConvergenceCheckAR5`, `ConfirmationCheck` and `ScalabilityRunner`) use the GARV classes in `src/GARV` (`GeneticAlgorithm`, `Task`, `VM`, `Utils`, …) and must be compiled together with them in the same package. The classes in group C are independent: they only read CSV files.

Every class has a short **"Settings to edit"** block at the top, which holds its input and output folders. Set these before running.

---

## A. Parameter tuning 

All tuning runs use the **tuning instances** (`dataset/tuningDataset`), which are disjoint from the evaluation instances. Each configuration is run on every tuning instance with the seeds `1000·instance + rep`. The same seeds are used for every configuration (common random numbers), so that differences between configurations are not caused by different random streams.

### `DOERunner.java`: three-stage Design of Experiments
Runs the GA for every configuration of one DOE stage on the 10 AR2 tuning instances.

| Stage | Parameters varied | Parameters held fixed |
|---|---|---|
| I | population size × number of generations (full grid, followed by an extended range of generations) | secondary parameters at their defaults |
| II | crossover rate, mutation rate, tournament size (one at a time) | population and generations from Stage I |
| III | SLA penalty constant ω | all GA parameters from Stages I–II |

Set `STAGE` (1, 2 or 3), run, choose the best values from the output, enter them in the `CHOSEN_…` constants, and run the next stage. The levels of each stage are listed at the top of the class.

**Output:** `DOE_Stage<X>_Runs.csv` (one row per run) and `DOE_Stage<X>_Summary.csv` (one row per configuration: mean and SD of CSR, PSR, SW and runtime).

### `AnovaAnalysis.java`: ANOVA of the DOE 
Reads the three `DOE_Stage<X>_Runs.csv` files. It analyses CSR and PSR as a **randomized complete block design**: the repeated runs of a configuration on an instance are averaged, and the tuning instance is included as a blocking factor.
- **Stage I:** Population + Generations + Population × Generations + Instance
- **Stages II and III:** Level + Instance, separately for each parameter

p-values are computed from the F distribution, with no external library.

**Output:** ANOVA tables (SS, df, MS, F, p-value, F-critical) in `ANOVA_Results.csv`, and level means in `ANOVA_LevelMeans.csv`.

### `ConvergenceCheckAR5.java`: convergence of the tuned GA
Runs the tuned GA well beyond the chosen number of generations on the 5 AR5 tuning instances (250 tasks, the longest chromosome), with the AR2 tuning instances as a reference. It records the best-so-far fitness, CSR, PSR and SW at regular checkpoints. It verifies that the chosen generation budget (1000) is sufficient.

**Output:** `Convergence_Runs.csv` (per run and checkpoint) and `Convergence_Summary.csv` (means and SDs per round and checkpoint).

### `ConfirmationCheck.java`: confirmation of the final configuration
Stage II varies one secondary parameter at a time, so the final combination was not itself tested as a whole in Stage II. This class compares the final combination (crossover 0.9, mutation 0.2, tournament 5) directly with two alternative combinations, at 1000 generations and several values of ω, on all 10 AR2 tuning instances.

**Output:** `Confirmation_Runs.csv` and a summary on the console.

**Final configuration** (Table 8): population 100, generations 1000, crossover 0.9, mutation 0.2, tournament size 5, ω = 100.

---

## B. Scalability

### `ScalabilityRunner.java`: runtime vs number of VMs (scenario 2)
Loads one AR5 evaluation instance (`EVAL_AR5_I01`, 250 tasks). For VM pool sizes 20, 30, …, 100, it keeps the first *k* VMs (the VM order in the file is random) and runs the GA 10 times per size, timing only the winner determination. Three untimed warm-up runs are made first, to exclude JVM start-up effects. Seeds: `900000 + 1000·k + r`.

**Output:** `Scalability_VMs_Runs.csv` (one row per run) and `Scalability_VMs.csv` (one row per pool size: mean, SD, 95% CI).

### `ScalabilityGraphs.java`: Figures 4 and 5
- **`Runtime_Tasks.png` (scenario 1):** runtime vs number of tasks (50–250, 100 VMs), taken from the `GATimeSec` row of the evaluation summary files. Since the evaluation already timed every run, no new runs are needed.
- **`Runtime_VMs.png` (scenario 2):** runtime vs number of VMs, read from `Scalability_VMs.csv`.

Error bars are 95% confidence intervals.

---

## C. Evaluation analysis

These classes read the result files written by `Main` (GARV) and by the baseline runners (Algo1, Algo2, Algo3): `<ALGO>_AR<r>_Runs.csv`, `_InstanceMeans.csv`, `_Summary.csv` and `_Allocations.csv`. They run no algorithm.

### `EvaluationGraphs.java`: performance graphs 
One bar chart per metric (CSR, PSR, IR, AIV, CVR, ACC, Makespan, SW, OPR, AOP). Each chart shows GARV and the three baselines in all five auction rounds, with 95% confidence intervals as error bars. The charts are 90 mm wide at 600 dpi, as needed for the journal.

### `SummaryTable.java`: summary table 
Writes `Summary_Table.csv`, with the mean ± SD of every metric over the 30 instances, for every algorithm and auction round.

### `StatisticalTests.java`: statistical analysis 
Reads the instance-level means of all four algorithms and writes to `Statistical_Results/`:

| File | Content | Paper |
|---|---|---|
| `Friedman_Test.csv` | Friedman test per metric over all 150 instances (with tie correction) | 
| `Rank_Summary.csv` | average rank of each algorithm per metric (rank 1 = best) | 
| `Critical_Difference.csv` | Nemenyi critical difference and GARV's rank difference to each baseline | 
| `Wilcoxon_Summary.csv` | GARV vs each baseline, per metric and round: significant / baseline better / not significant | 
| `Wilcoxon_Details.csv` | full test details: wins/ties/losses, W⁺, W⁻, W, Z, p-value, Holm-adjusted p-value | 

The Wilcoxon signed-rank tests are two-sided and run over 30 paired instances per round. Zero differences are dropped. p-values are exact when there are no ties, and otherwise come from the normal approximation with a tie correction. Holm's correction is applied over the three comparisons of each metric and round. Pairwise tests are run only for metrics whose Friedman test is significant. Whether a lower or higher value is better is set per metric: lower is better for CVR, OPR, AOP and Makespan.

### `OptimalityGap.java`: optimality gap 
For every evaluation instance, it solves the Revenue − Penalty part of GARV's fitness to **global optimality** as an assignment problem, using the Hungarian algorithm. It uses the same revenue and penalty rules as the GA, ω = 100, and min(|tasks|, |VMs|) one-to-one pairs. It then compares the optimum with GARV's value (`RawRevenuePenalty`).

**Output:** `Optimality_Gap.csv`, with columns Round, Mean Value of GARV, Optimal Value and Percentage Optimum.

### `StabilityAnalysis.java`: run-to-run stability 
Uses the 10 runs of each evaluation instance:
- **Allocation level:** the pairwise similarity of the final allocations (45 pairs per instance, 1,350 per round), compared with the chance level 100 / max(|tasks|, |VMs|).
- **Outcome level:** the coefficient of variation of CSR and PSR across the runs of each instance, averaged over the 30 instances.

**Output:** `Stability_Analysis.csv`, one row per round.

---

## Order of execution

1. Generate the datasets (`dataset/DatasetGenerator.java`), or use the uploaded instances.
2. **Tuning:** `DOERunner` (stages 1 → 2 → 3) → `AnovaAnalysis` → `ConvergenceCheckAR5` → `ConfirmationCheck`.
3. **Evaluation:** `Main` in `src/GARV` and the baseline runners, for AR1–AR5.
4. **Analysis:** `EvaluationGraphs`, `SummaryTable`, `StatisticalTests`, `OptimalityGap`, `StabilityAnalysis`.
5. **Scalability:** `ScalabilityRunner` → `ScalabilityGraphs`.

Requirements: Java 8 or later; no external libraries.
