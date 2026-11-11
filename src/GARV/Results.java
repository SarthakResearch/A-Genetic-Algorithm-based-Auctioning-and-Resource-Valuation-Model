package GARVaverageEachRunResultEvaluation;


public class Results {
	static double numOfTasksHavingCombinedSuccess=0;
	static double numOfTasksHavingDeadlinePreferedSuccess=0;
	static double numOfTasksHavingBudgetPrferedSuccess=0;
	static double totalIncentiveEarned=0;
	static double totalIncentivizedVms=0;
	static double totalUserSavings=0;
	static double numberOfUsersGetSavings=0;
	static double numberOfUsersGoneOverbudget=0;
	static double totalOverbudgetPayment=0;
	static double totalPenalty=0;
	static double totalPenalyzedVms=0;
	static double makespan=0;
	static double costspan=0;
	static double winners=0;
	static double resourceUtilizationIndex=0;
	static double executionTimeSec=0;
	static double clientUtility=0;
	static double providerUtility=0;
	static double socialWelfare=0;
	static Task thisTask=null;
	static VM thisVm = null;
	static double TSR, PSR, IR, AIV, CR, ACC, OPR, AOP;
	
	// DOE: all metric calculations, without printing
	public static void computeResults() {
		double C = (Main.tasks.size()<Main.vms.size()?Main.tasks.size():Main.vms.size());
		double n = Main.finalAllocationList.size();

		TSR = (numOfTasksHavingCombinedSuccess*100)/C;
		PSR = ((numOfTasksHavingDeadlinePreferedSuccess+numOfTasksHavingBudgetPrferedSuccess)*100)/C;
		IR  = (totalIncentivizedVms*100)/n;
		AIV = totalIncentiveEarned/n;
		CR  = (numberOfUsersGetSavings*100)/n;
		ACC = totalUserSavings/n;
		OPR = (numberOfUsersGoneOverbudget*100)/n;
		AOP = totalOverbudgetPayment/n;

		clientUtility = 0;
		providerUtility = 0;
		for(TaskVmPair mapping : Main.finalAllocationList) {
			thisTask = mapping.task;
			thisVm = mapping.vm;
			if (!thisTask.isExecuted) continue;   // discarded (Case 4) tasks have zero utility
			clientUtility += (thisTask.budget - thisTask.paymentMade);
			providerUtility += (thisTask.paymentMade - thisTask.executionCost);
		}
		socialWelfare = clientUtility + providerUtility;
	}

	public static void printResults() {
		computeResults();

		System.out.println("::Execution Results::");
		System.out.println("******************************************************");

		System.out.println("::Final Results::");
		System.out.println("******************************************************");
		System.out.println("Combined Success Rate (CSR) = " + TSR);
		System.out.println("Preference Saticfaction Rate (PSR) = " + PSR);
		System.out.println("Incentivization Rate (IR) " + IR);
		System.out.println("Average Incentive Per VM (AIV) = " + String.format("%.3f", AIV));
		System.out.println("Compensation Rate (CR) = " + CR);
		System.out.println("Average Compensation Per Client (ACC) = " + String.format("%.3f", ACC));
		System.out.println("Overbudget Payment Rate (OPR) = " + OPR);
		System.out.println("Average Overbudget payment (AOP) = " + String.format("%.3f", AOP));
		System.out.println("Makespan = " + String.format("%.3f", makespan));
		System.out.println("Social Welfare = " + String.format("%.3f", socialWelfare));
		System.out.println("Execution time in second: " + String.format("%.5f", executionTimeSec));	
		System.out.println("GARV Revenue-Penalty (pre-dispersion, for ILP comparison): " + Main.rawScore);
		}
	
	public static void reset() {
	    numOfTasksHavingCombinedSuccess = 0;
	    numOfTasksHavingDeadlinePreferedSuccess = 0;
	    numOfTasksHavingBudgetPrferedSuccess = 0;
	    totalIncentiveEarned = 0;
	    totalIncentivizedVms = 0;
	    totalUserSavings = 0;
	    numberOfUsersGetSavings = 0;
	    numberOfUsersGoneOverbudget = 0;
	    totalOverbudgetPayment = 0;
	    totalPenalty = 0;
	    totalPenalyzedVms = 0;
	    makespan = 0;
	    costspan = 0;
	    winners = 0;
	    resourceUtilizationIndex = 0;
	    executionTimeSec = 0;
	    clientUtility = 0;
	    providerUtility = 0;
	    socialWelfare = 0;
	    TSR = PSR = IR = AIV = CR = ACC = OPR = AOP = 0;
	}
	}

