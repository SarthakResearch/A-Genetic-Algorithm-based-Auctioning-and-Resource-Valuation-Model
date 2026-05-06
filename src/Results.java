package SourceCode;


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
	
	
	public static void printResults() {
		
		System.out.println("::Execution Results::");
		System.out.println("******************************************************");
		//System.out.println();
		//System.out.println("Number of Tasks Descarded by Users Preference: " + (AuctionMain.descardedByPreference.size()));
		//System.out.println("Number of Tasks Descarded by Clashed VMs: " + (Allocation.clashedTaskList.size()));
		//System.out.println("Number of Tasks Descarded by QoS Demands: " + (AuctionMain.descardedTaskList.size()));
		//System.out.println("Number of Tasks allocated: " + (AuctionMain.taskList.size() - (AuctionMain.descardedByPreference.size()+Allocation.clashedTaskList.size()+AuctionMain.descardedTaskList.size())));
		//System.out.println("******************************************************");
		System.out.println("Number of Tasks Having Combined Success = " + numOfTasksHavingCombinedSuccess);
		System.out.println("Number of Tasks Having Deadline Preferred Success = " + numOfTasksHavingDeadlinePreferedSuccess);
		System.out.println("Number of Tasks Having Budget Preferred Success = " + numOfTasksHavingBudgetPrferedSuccess);
		System.out.println("Total Incentive Earned = " + String.format("%.3f", totalIncentiveEarned));
		System.out.println("Total Incentivized VMs = " + totalIncentivizedVms);
		System.out.println("Total User Savings = " + String.format("%.3f", totalUserSavings));
		System.out.println("Number of Users Getting Savings = " + numberOfUsersGetSavings);
		System.out.println("Number of Users Gone Overbudget = " + numberOfUsersGoneOverbudget);
		System.out.println("Total Overbudget Payment = " + String.format("%.3f", totalOverbudgetPayment));
		System.out.println();
		System.out.println("******************************************************");
		System.out.println("::Final Results::");
		System.out.println("******************************************************");
		System.out.println("Task Success Rate (TSR) = " + (numOfTasksHavingCombinedSuccess*100)/Main.finalAllocationList.size());
		System.out.println("Preference Satisfaction Rate (PSR)= " + ((numOfTasksHavingDeadlinePreferedSuccess+numOfTasksHavingBudgetPrferedSuccess)*100)/Main.finalAllocationList.size());
		System.out.println("Incentivization Rate (IR) = " + (totalIncentivizedVms*100)/Main.finalAllocationList.size());
		System.out.println("Average Incentive Per VM (AIV) = " + String.format("%.3f", ((double)totalIncentiveEarned/Main.finalAllocationList.size())));
		System.out.println("Compensation Rate (CR) = " + (numberOfUsersGetSavings*100)/Main.finalAllocationList.size());
		System.out.println("Average Compensation Per Client (ACC) = " + String.format("%.3f", ((double)totalUserSavings/Main.finalAllocationList.size())));
		System.out.println("Overbudget Payment Rate (OPR) = " + (numberOfUsersGoneOverbudget*100)/Main.finalAllocationList.size());
		System.out.println("Average Overbudget payment (AOP) = " + String.format("%.3f", ((double)totalOverbudgetPayment/Main.finalAllocationList.size())));
		System.out.println("Makespan = " + String.format("%.3f", makespan));
		//System.out.println("Costspan = " + String.format("%.3f", costspan));
		//for()
		
		for(TaskVmPair mapping : Main.finalAllocationList) {
			thisTask = mapping.task;
			thisVm = mapping.vm;
			
			clientUtility += (thisTask.budget - thisTask.paymentMade);
			providerUtility += (thisTask.paymentMade - thisTask.executionCost);
		}
		
		socialWelfare = clientUtility + providerUtility;
		
		System.out.println("Client's Utility = " + String.format("%.3f", clientUtility));
		System.out.println("Provider's Utility = " + String.format("%.3f", providerUtility));
		System.out.println("Social Welfare = " + String.format("%.3f", socialWelfare));
		System.out.println("Execution time in second: " + String.format("%.5f", executionTimeSec));	
		}
	}

