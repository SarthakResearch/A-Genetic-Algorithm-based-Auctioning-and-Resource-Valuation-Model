# GARV Datasets

This folder contains all problem instances used in the paper and the program that generated them (`DatasetGenerator.java`). The instances are synthetic, **heterogeneous but feasible**, and **fully reproducible**: every instance is generated from a recorded seed.

## Contents

```
dataset/
├── DatasetGenerator.java
├── manifest.csv                        one row per instance (statistics below)
├── tuningDataset/
│   ├── AR2/  TUNE_AR2_I01 … TUNE_AR2_I10   parameter tuning (DOE, Sections 6.4–6.5)
│   └── AR5/  TUNE_AR5_I01 … TUNE_AR5_I05   convergence check of the tuned GA
└── evaluationDataset/
    ├── AR1/  EVAL_AR1_I01 … EVAL_AR1_I30
    ├── AR2/  …
    └── AR5/  EVAL_AR5_I01 … EVAL_AR5_I30   performance evaluation (Sections 6.6–8)
```

Each instance consists of two files, `<NAME>_Tasks.csv` and `<NAME>_VMs.csv`. The names follow the pattern `<SET>_AR<r>_I<nn>`:

- `SET`: `TUNE` or `EVAL`
- `r`: auction round (1–5)
- `nn`: two-digit instance number

This allows any instance to be read automatically.

| Set | Rounds | Instances per round | Tasks | VMs | Total |
|---|---|---|---|---|---|
| Tuning | AR2 | 10 | 100 | 100 | 10 |
| Tuning (convergence check) | AR5 | 5 | 250 | 100 | 5 |
| Evaluation | AR1–AR5 | 30 | 50 / 100 / 150 / 200 / 250 | 100 | 150 |

The five auction rounds cover the demand–supply scenarios of the paper: under-demand (AR1), balanced demand (AR2) and over-demand (AR3–AR5). **The tuning and evaluation instances are generated from disjoint seeds**, so the GA parameters were never tuned on the evaluation data.

## File format

**Tasks:** `Task ID,Length,Budget,Deadline,Pref`

| Field | Unit | Range |
|---|---|---|
| Length | million instructions (MI), multiple of 10 | 10,000 – 90,000 |
| Budget | cost units (integer) | 10 – 100 |
| Deadline | seconds (integer) | 20 – 200 |
| Pref | `T` = deadline has priority, `C` = budget has priority | – |

**VMs:** `VM_ID,MIPS,Rate`

| Field | Unit | Range |
|---|---|---|
| MIPS | million instructions per second (integer) | 100 – 1000 |
| Rate | cost units per second (3 decimals) | 0.1 – 1.0 |

The execution time of task *i* on VM *j* is Length / MIPS, and its execution cost is execution time × Rate.

## How heterogeneity is created

Simply drawing every value uniformly at random produces instances that are either trivially easy or almost infeasible, and in which fast VMs are always expensive. The generator instead creates **structured heterogeneity** in three places.

### 1. VM pool: pricing heterogeneity
Each VM belongs to one of three types. These are mixed, and the file order is shuffled, so the type cannot be inferred from the position.

| VM type | Share | MIPS | Rate | Meaning |
|---|---|---|---|---|
| Proportional | 50% | U(100, 1000) | 0.001 × MIPS × U(0.85, 1.15) | price follows speed |
| Bargain | 25% | U(550, 1000) | U(0.10, 0.40) | fast and cheap |
| Premium-slow | 25% | U(100, 550) | U(0.60, 1.00) | slow and expensive |

Because speed and price do not always move together, choosing a VM involves a genuine trade-off between deadline and cost.

### 2. Tasks: constraint heterogeneity
Deadlines and budgets are **anchored to the actual VM pool**, so that they are realistic for the VMs on offer. For each task, a length is drawn from U(10,000, 90,000), and a reference VM is picked at random from the pool. Then:

- reference time = Length / MIPS_ref
- reference cost = reference time × Rate_ref
- Deadline = reference time × s_D
- Budget = reference cost × s_B

The slack factors s_D and s_B depend on the task class:

| Task class | Share | Deadline slack s_D | Budget slack s_B | Meaning |
|---|---|---|---|---|
| Deadline-tight | 30% | U(0.6, 1.0) | U(1.2, 2.0) | time-critical, generous budget |
| Budget-tight | 30% | U(1.2, 2.0) | U(0.6, 1.0) | cost-critical, relaxed deadline |
| Moderate | 25% | U(0.9, 1.4) | U(0.9, 1.4) | balanced |
| Both-tight | 15% | U(0.6, 0.9) | U(0.6, 0.9) | hard on both constraints |

All values are rounded and clipped to the ranges above.

### 3. Preferences: not always the tighter constraint
| Task class | P(Pref = T) | P(Pref = C) |
|---|---|---|
| Deadline-tight | 0.6 | 0.4 |
| Budget-tight | 0.4 | 0.6 |
| Moderate, Both-tight | 0.5 | 0.5 |

A user's declared priority is therefore usually, but not always, the constraint that is harder to meet, as in practice. The overall split is close to 50/50.

## Feasibility guard

Each instance must pass two checks; otherwise, it is regenerated from the next sub-seed:

- **Density:** the average fraction of VMs on which a task meets both its deadline and its budget must lie between **25% and 45%**.
- **Coverage:** at most **20%** of the tasks may have no VM that meets both constraints.

The guard is the same for all rounds, so the difficulty of the instances does not depend on the demand–supply scenario. The accepted attempt number is recorded in `manifest.csv`.

## Statistics of the generated instances (averages per round, from `manifest.csv`)

| Set | Density (%) | Tasks with no fully feasible VM (%) | CSR upper bound (%) | PSR upper bound (%) | Pref = T (%) |
|---|---|---|---|---|---|
| Tuning AR2 | 35.1 | 10.6 | 79.1 | 92.3 | 48.8 |
| Tuning AR5 | 34.7 | 9.8 | 92.0 | 97.6 | 47.4 |
| Evaluation AR1 | 33.5 | 10.7 | 88.8 (80–98) | 94.0 | 51.3 |
| Evaluation AR2 | 33.4 | 11.1 | 77.9 (69–85) | 92.8 | 51.4 |
| Evaluation AR3 | 34.3 | 10.5 | 84.9 (78–92) | 96.4 | 48.9 |
| Evaluation AR4 | 33.5 | 11.5 | 87.9 (82–93) | 97.8 | 50.7 |
| Evaluation AR5 | 34.1 | 10.6 | 91.3 (86–95) | 98.3 | 51.2 |

The **CSR upper bound** is the largest fraction of min(|tasks|, |VMs|) that any mechanism could allocate with both constraints satisfied. It is computed as a maximum one-to-one matching over the fully feasible task–VM pairs. The **PSR upper bound** is the same, but counts a pair as feasible when the task's preferred constraint is met. These bounds show that 100% success is not attainable in these instances. They also give a reference against which the results of every mechanism can be judged.

Across the evaluation set, the task classes average about 30% deadline-tight, 30% budget-tight, 25% moderate and 15% both-tight. The VM types average about 50% proportional, 25% bargain and 25% premium-slow.

### `manifest.csv` columns
`Set, Round, Instance, Tasks, VMs, Seed, Attempt, TaskFile, VmFile, Density, TasksWithNoFeasibleVM, CSRUpperBound, PrefUpperBound, DeadlineTight, BudgetTight, Moderate, BothTight, PrefT, PrefC, VmProportional, VmBargain, VmPremiumSlow`

## Seeds

| Set | Seed |
|---|---|
| Tuning AR2, instance i | i (1–10) |
| Tuning AR5, instance i | 50 + i (51–55) |
| Evaluation round r, instance i | 100000 + 1000·r + i (e.g. AR3, I07 → 103007) |

Attempt *a* of a seed uses `new Random(seed × 1000 + a)`.

## Regenerating the datasets

Requirements: Java 8 or later; no external libraries.

```
javac -d bin DatasetGenerator.java
java -cp bin GARVdataset.DatasetGenerator <output folder>
```

The program writes `manifest.csv` and the folders `tuning/` and `evaluation/` into the output folder (renamed `tuningDataset/` and `evaluationDataset/` in this repository). Because all seeds are fixed, it reproduces the uploaded instances exactly.
