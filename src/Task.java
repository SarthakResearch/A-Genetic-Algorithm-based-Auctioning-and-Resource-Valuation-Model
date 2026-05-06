package SourceCode;

public class Task {
	public int id;
    public double length;      // in MI
    public double deadline;    // in seconds
    public double budget;   // cost units
    public double finishTime;
    public double executionCost;
    public double paymentMade;
    public double savings;
    public boolean isDeadlineMet=false;
    public boolean isBudgetMet=false;
    public char preference;
    public Task(int id, int length, int budget, double deadline, char pref) {
        this.id = id;
        this.length = length;
        this.budget = budget;
        this.deadline = deadline;
        this.preference = pref;
    }
}
