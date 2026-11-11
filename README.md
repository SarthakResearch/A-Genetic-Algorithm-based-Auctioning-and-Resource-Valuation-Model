# GARV: A Genetic Algorithm-based Auctioning and Resource Valuation Model for Multi-Constrained Task Allocation in Heterogeneous Cloud Computing Environments

## Overview

Efficient allocation of computational tasks to heterogeneous cloud resources under conflicting user constraints, such as deadlines, budgets and service preferences, remains a fundamental challenge in IoT-driven cloud environments. Existing auction-based resource allocation mechanisms largely overlook execution-time-aware cost modeling, flexible constraint handling, and economically sound pricing that both incentivizes providers and compensates users. This limits their practical applicability in realistic multi-constrained cloud scenarios.

This repository provides the Java implementation of GARV, the synthetically generated datasets, and all the programs used for parameter tuning and result analysis in the following paper:

> "A Genetic Algorithm-based Auctioning and Resource Valuation Model for Multi-Constrained Task Allocation in Heterogeneous Cloud Computing Environments"
> Submitted to *Computers and Electrical Engineering*

GARV integrates a multi-attributed double auction with a preference-aware Genetic Algorithm for winner determination, and a performance- and preference-aware pricing model. The pricing model provides provider incentives and user compensation, and it is proven to be individually rational, budget balanced and truthful with respect to the users' declared preferences.

## Problem Description

IoT devices and end users submit computationally intensive tasks to a cloud datacenter broker, which allocates them to Virtual Machines (VMs) offered by Cloud Service Providers (CSPs).

- Each **task** has a length (million instructions), a deadline, a budget and a **preference**: whether the deadline or the budget takes priority when both cannot be satisfied.
- Each **VM** has a processing speed (MIPS) and a usage rate (cost per second).

Each VM serves at most one task and each task is assigned at most one VM. Finding the best one-to-one task-to-VM mapping is NP-hard. GARV solves it with a preference-aware GA that maximizes task success and SLA compliance while balancing VM load. Its pricing model then settles each allocated pair according to which constraints were met:

| Case | Condition | User pays | Provider receives |
|---|---|---|---|
| 1 | Deadline and budget both met | budget b<sub>i</sub> | b<sub>i</sub>, i.e. cost + incentive (b<sub>i</sub> − exCost) |
| 2 | Deadline priority: deadline met, budget exceeded | exCost (beyond budget, with the user's prior consent) | exCost |
| 3 | Budget priority: budget met, deadline missed | (exCost + b<sub>i</sub>)/2: the user is compensated with half the surplus | (exCost + b<sub>i</sub>)/2: the provider receives half the surplus as incentive |
| 4 | Preferred constraint violated | task discarded | – |

## Repository Structure

```
src/
├── GARV/        GARV mechanism: genetic algorithm, pricing model, metrics and evaluation runner
├── dataset/     dataset generator, tuning instances and evaluation instances
├── Analysis/    parameter tuning (DOE), scalability, graphs and statistical analysis
└── results/     result files reported in the paper
```

Each folder has its own README with full details:

| Folder | What its README explains | Paper |
|---|---|---|
| [`src/GARV`](src/GARV) | each class, GA configuration, encoding and fitness, how to run the evaluation, output files and metrics | 
| [`src/dataset`](src/dataset) | file formats, how heterogeneity is generated, feasibility guard, instance statistics, seeds | 
| [`src/Analysis`](src/Analysis) | DOE tuning and ANOVA, convergence and confirmation checks, scalability, graphs, statistical tests, optimality gap, stability |

## Experimental Setup at a Glance

- **Datasets:** 5 auction rounds (50, 100, 150, 200 and 250 tasks; 100 VMs each), with **30 heterogeneous instances per round** (150 in total). A separate set of tuning instances is used only for parameter tuning.
- **GA configuration**, from a three-stage DOE: population 100, generations 1000, crossover 0.9, mutation 0.2, tournament size 5, SLA penalty ω = 100.
- **Runs:** GARV is run 10 times per instance with recorded seeds; the baselines are deterministic and run once per instance.
- **Statistics:** Friedman test, average ranks, Nemenyi critical difference, and two-sided Wilcoxon signed-rank tests with Holm correction.

## Requirements

- **Java:** 1.8.0_441 or higher
- **IDE:** Eclipse (recommended) or any Java IDE
- **Dependencies:** none; only the standard Java libraries are used
- **Tested on:** Windows 11 Pro (64-bit), Intel Xeon W-1250 @ 3.30 GHz, 6 cores

## Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/SarthakResearch/A-Genetic-Algorithm-based-Auctioning-and-Resource-Valuation-Model.git
   ```
2. **Import into Eclipse:** *File → Import → Existing Projects into Workspace*, select the cloned folder, then *Finish*.
3. **Set the paths:** every runnable class has a short *Settings to edit* block at its top, holding its input and output folders. Point these to `src/dataset` and to an output folder of your choice.
4. **Run in this order:**
   1. `dataset/DatasetGenerator`. This step is optional: the generated instances are already included.
   2. Parameter tuning in `Analysis`: `DOERunner` → `AnovaAnalysis` → `ConvergenceCheckAR5` → `ConfirmationCheck`.
   3. `GARV/Main` for each auction round (AR1–AR5).
   4. Analysis in `Analysis`: `EvaluationGraphs`, `SummaryTable`, `StatisticalTests`, `OptimalityGap`, `StabilityAnalysis`, and `ScalabilityRunner` → `ScalabilityGraphs`.

## Reproducibility

Every dataset instance and every GA run uses a fixed, recorded seed, so all results reported in the paper can be reproduced exactly with the code and datasets in this repository. The result files in `src/results` allow the reported values to be checked without rerunning the experiments.

## License

This repository is made available for academic and research purposes. All rights reserved by the authors. For any use beyond academic research, please contact the authors.

## Contact

For questions about the implementation or datasets, please open an issue in this repository or contact the author at [sarthak@bhu.ac.in].
