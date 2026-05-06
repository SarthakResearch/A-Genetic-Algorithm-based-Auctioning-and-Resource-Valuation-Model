package SourceCode;

import java.util.List;


public class Execution {

		static Task thisTask=null;
		static VM thisVm = null;
		static double surplus;
		public static void performExecution(List<TaskVmPair> allocationList) {
			for(TaskVmPair mapping : allocationList) {
				thisTask = mapping.task;
				thisVm = mapping.vm;
				thisTask.finishTime = thisTask.length/thisVm.mips;
				Results.makespan = Results.makespan<thisTask.finishTime? thisTask.finishTime:Results.makespan;
				thisTask.executionCost = thisTask.finishTime * thisVm.rate;
				thisVm.ecost = thisTask.executionCost;
				
				if(thisTask.finishTime<=thisTask.deadline) {
					thisTask.isDeadlineMet=true;
				}
				if(thisTask.executionCost<=thisTask.budget) {
					thisTask.isBudgetMet=true;
				}
				
				if(thisTask.isDeadlineMet && thisTask.isBudgetMet) {
					Results.winners++;

					
					thisVm.totalEarning = thisTask.budget;
					thisVm.incentive = thisVm.totalEarning - thisVm.ecost;
					thisTask.paymentMade = thisVm.totalEarning;
					Results.costspan += thisTask.paymentMade;
					Results.numOfTasksHavingCombinedSuccess += 1;

					if(thisTask.preference=='C') {
						Results.numOfTasksHavingBudgetPrferedSuccess += 1;
					}
					else {
						Results.numOfTasksHavingDeadlinePreferedSuccess += 1;
					}
					Results.totalIncentiveEarned += thisVm.incentive;
					Results.totalIncentivizedVms += 1;
					
				}
				
				
				if(thisTask.isDeadlineMet && !thisTask.isBudgetMet && thisTask.preference=='T') {
					Results.winners++;
					surplus = thisTask.executionCost - thisTask.budget;
					thisVm.totalEarning = thisVm.ecost;
					thisTask.paymentMade = thisVm.totalEarning;
					Results.costspan += thisTask.paymentMade;
					Results.numOfTasksHavingDeadlinePreferedSuccess += 1;
					Results.numberOfUsersGoneOverbudget += 1;
					Results.totalOverbudgetPayment += (surplus);
				
				}
				
				if(!thisTask.isDeadlineMet && thisTask.isBudgetMet && thisTask.preference=='C') {
					Results.winners++;
					surplus = thisTask.budget - thisTask.executionCost;
					
					thisVm.totalEarning = thisVm.ecost + (surplus/2);
					thisTask.paymentMade = thisVm.totalEarning;
					Results.costspan += thisTask.paymentMade;
					Results.numOfTasksHavingBudgetPrferedSuccess += 1;
					Results.numberOfUsersGetSavings += 1;
					Results.totalIncentivizedVms++;
					Results.totalIncentiveEarned += (surplus/2);
					Results.totalUserSavings += (thisTask.budget - thisTask.paymentMade);
					
				}
			}

		}
	}

