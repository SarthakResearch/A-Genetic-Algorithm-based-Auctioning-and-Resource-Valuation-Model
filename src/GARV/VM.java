package GARVaverageEachRunResult;

public class VM {
	public int id;
    public double mips;       // speed
    public double rate;    // cost per second
    public boolean isFree=true;
    public double ecost=0;
    public double totalEarning=0;
    public double incentive=0;
    public double penalty=0; 
	
    public VM(int id, int mips, double rate) {
        this.id = id;
        this.mips = mips;
        this.rate = rate;
    }
    
    public void reset() {
        this.isFree = true;
        this.ecost = 0;
        this.totalEarning = 0;
        this.incentive = 0;
        this.penalty = 0;
    }
}
