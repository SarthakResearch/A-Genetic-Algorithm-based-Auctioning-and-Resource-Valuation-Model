# GARV: A Genetic Algorithm-based Auctioning and Resource Valuation Model for Multi-Constrained Task Allocation in Heterogeneous Cloud Computing Environments

## Overview

Efficient allocation of computational tasks to heterogeneous cloud resources under conflicting user constraints — such as deadlines, budgets, and service preferences — remains a fundamental challenge in IoT-driven cloud environments. Existing auction-based resource allocation mechanisms largely overlook execution-time-aware cost modeling, flexible constraint handling, and economically sound pricing that simultaneously incentivizes providers and compensates users, limiting their practical applicability in realistic multi-constrained cloud scenarios.

This repository provides the Java implementation of the GARV genetic algorithm and pricing model, and the synthetically generated datasets, associated with the following paper:

> "A Genetic Algorithm-based Auctioning and Resource Valuation Model for Multi-Constrained Task Allocation in Heterogeneous Cloud Computing Environments"
> Submitted to [Computers and Electrical Engineering Journal]

GARV integrates a multi-attributed double auction framework with a preference-aware Genetic Algorithm for winner determination and a performance and preference-aware pricing model that jointly ensures incentive compatibility, compensation-based fairness, individual rationality, and budget balance.


## Problem Description

In heterogeneous cloud environments, IoT devices and end users submit computationally intensive tasks to a cloud datacenter broker for allocation to Virtual Machines (VMs) offered by Cloud Service Providers (CSPs). Each task is characterized by:

* **Length** (in Million Instructions)
* **Deadline** (maximum allowable execution time)
* **Budget** (maximum cost the user is willing to pay)
* **Preference** (whether deadline or budget takes priority when both cannot be simultaneously satisfied)

Each VM is characterized by:

* **MIPS** (processing speed in Million Instructions Per Second)
* **Rate** (cost per second of usage)

The allocation problem is NP-hard due to the combinatorial task-to-VM mapping space. GARV addresses this through a preference-aware GA that optimizes task success, load balancing, and SLA compliance simultaneously, complemented by a formally validated pricing model.


## Dataset Format

### Task Dataset CSV

Each row represents one IoT task with the following columns:

|Column|Description|Type|
|-|-|-|
|TaskID|Unique task identifier|Integer|
|Length|Task length in Million Instructions (10,000 – 90,000 MI)|Integer|
|Budget|Maximum cost user is willing to pay (10 – 100)|Integer|
|Deadline|Maximum allowable execution time in seconds (20 – 200)|Double|
|Preference|Constraint priority flag: T = Time/Deadline priority, C = Cost/Budget priority|Char|

**Example:**

```
TaskID,Length,Budget,Deadline,Preference
1,45000,75,120,T
2,62000,50,150,C
3,38000,90,90,T
```

### VM Dataset CSV

Each row represents one Virtual Machine offered by a Cloud Service Provider:

|Column|Description|Type|
|-|-|-|
|VMID|Unique VM identifier|Integer|
|MIPS|Processing speed in Million Instructions Per Second (100 – 1000)|Integer|
|Rate|Usage cost per second (0.1 – 1.0)|Double|

**Example:**

```
VMID,MIPS,Rate
1,500,0.6
2,800,0.3
3,200,0.9
```


## Implementation Details

### Source File Descriptions

|File|Description|
|-|-|
|`Main.java`|Entry point of the simulation. Loads task and VM datasets from CSV files, sets GA parameters and the number of independent runs (n), initializes the GeneticAlgorithm, runs the winner determination over n runs, builds the final allocation list, and triggers result computation and export.|
|`GeneticAlgorithm.java`|Core GA implementation. Handles population initialization, fitness evaluation, tournament selection, order-preserving crossover, swap mutation, and elitism-based generational replacement. Automatically detects and handles three scenarios: tasks < VMs, tasks = VMs, and tasks > VMs through the allocSize = min(tasks, VMs) mechanism.|
|`Chromosome.java`|Encodes a complete task-to-VM mapping as a permutation of positions (0 to allocSize-1). Includes clone() method for elitism.|
|`Task.java`|Represents an IoT task with attributes: id, length, deadline, budget, preference flag, execution time, execution cost, payment, and constraint satisfaction flags.|
|`VM.java`|Represents a Virtual Machine with attributes: id, mips, rate, availability flag, earnings, incentive, and penalty values.|
|`Utils.java`|Provides static utility methods readTasks() and readVMs() for parsing task and VM CSV files into Java List objects.|
|`TaskVmPair.java`|Simple data structure pairing an allocated Task with its assigned VM, used to build the final allocation list passed to the pricing model.|
|`Execution.java`|Implements the performance and preference-aware pricing model. Iterates over each TaskVmPair and applies one of four pricing cases based on deadline satisfaction, budget satisfaction, and user preference, computing payments, incentives, and compensations accordingly.|
|`EachRunResult.java`|Reports the value of every evaluation parameter (CSR, PSR, IR, AIV, IEI, CVR, ACC, OPR, AOP, Makespan, Social Welfare) for each individual run, prior to averaging.|
|`Results.java`|Computes the corresponding values averaged across the n runs configured in `Main.java`, and exports them to an Excel workbook. These averaged values form the basis of the figures and tables reported in the associated paper.|



## GA Parameters

The following GA parameters, determined through the two-stage Design of Experiments (DOE) analysis described in the paper, are used in the final comparative experiments and are the values set in `Main.java`:

|Parameter|Value|
|-|-|
|Population Size|100|
|Number of Generations|500|
|Crossover Rate|0.6|
|Mutation Rate|0.2|
|Tournament Size|7|
|SLA Penalty (ω)|10|


## Requirements

* **Java Version:** Java 1.8.0\_441 or higher
* **IDE:** Eclipse IDE (recommended) or any Java-compatible IDE
* **Operating System:** Any (tested on Windows 11 Pro, 64-bit)
* **Hardware:** No special requirements (tested on Intel Xeon W-1250 @ 3.30 GHz, 6 cores)
* **Dependencies:** No external libraries required — uses only standard Java libraries (java.util, java.io, java.util.Arrays)


## How to Run

### Step 1 — Clone the Repository

```bash
git clone https://github.com/SarthakResearch/A-Genetic-Algorithm-based-Auctioning-and-Resource-Valuation-Model.git
```

### Step 2 — Import into Eclipse

1. Open Eclipse IDE
2. Go to **File → Import → Existing Projects into Workspace**
3. Select the cloned repository folder
4. Click **Finish**

### Step 3 — Configure Dataset Paths and Run Count

Open `Main.java` and update the file paths to point to your local dataset locations, and set the desired number of independent runs (n):

```java
tasks = Utils.readTasks("path/to/your/datasets/tasks/TaskDataset50.csv");
vms = Utils.readVMs("path/to/your/datasets/vms/VmDataset100.csv");
int numRuns = 10; // set number of independent runs
```

Replace `"path/to/your/datasets/"` with the actual absolute path on your system.

### Step 4 — Run the Simulation

1. Right-click on `Main.java` in the Project Explorer
2. Select **Run As → Java Application**
3. Per-run results (via `EachRunResult.java`) are printed to the Eclipse console; averaged results across all n runs (via `Results.java`) are printed and exported to an Excel workbook.

### Step 5 — Run Different Auction Rounds

To run different auction round configurations, change the task dataset file path in `Main.java`:

```java
// Auction Round 1: 50 tasks, 100 VMs
tasks = Utils.readTasks("path/to/TaskDataset50.csv");

// Auction Round 2: 100 tasks, 100 VMs
tasks = Utils.readTasks("path/to/TaskDataset100.csv");

// Auction Round 3: 150 tasks, 100 VMs
tasks = Utils.readTasks("path/to/TaskDataset150.csv");

// Auction Round 4: 200 tasks, 100 VMs
tasks = Utils.readTasks("path/to/TaskDataset200.csv");

// Auction Round 5: 250 tasks, 100 VMs
tasks = Utils.readTasks("path/to/TaskDataset250.csv");
```

The VM dataset remains fixed at `VmDataset100.csv` across all auction rounds, or you can evaluate performance using different VM pool sizes; CSV files for these are also included.


## Handling Three Allocation Scenarios

The implementation automatically handles three distinct operational scenarios based on the relationship between task count and VM pool size:

|Scenario|Example|Behavior|
|-|-|-|
|Tasks < VMs|50 tasks, 100 VMs|All tasks allocated, some VMs remain idle|
|Tasks = VMs|100 tasks, 100 VMs|Perfect one-to-one allocation possible|
|Tasks > VMs|150-250 tasks, 100 VMs|GA selects optimal task subset of size = VMs|

This is handled automatically through `allocSize = Math.min(tasks.size(), vms.size())` in `GeneticAlgorithm.java` — no manual configuration is required.


## Pricing Model Cases

The `Execution.java` implements the following four pricing cases:

|Case|Condition|Payment|Incentive|Compensation|
|-|-|-|-|-|
|Case 1|Both deadline and budget satisfied|Full budget (bi)|bi − exCost|None|
|Case 2|Deadline priority (T), deadline met, budget exceeded|Full exCost|None|None|
|Case 3|Budget priority (C), budget met, deadline exceeded|(exCost + bi)/2|Surplus/2|Surplus/2|
|Case 4|Preferred constraint violated|Task discarded|None|None|

## Reproducibility

All experimental results reported in the associated paper are fully reproducible using the datasets and source code provided in this repository.

## License

This repository is made available for academic and research purposes.
All rights reserved by the authors.
For any usage beyond academic research, please contact the authors.


## Contact

For questions regarding the implementation or datasets, please open an issue in this repository or contact the author at \[sarthak@bhu.ac.in].
