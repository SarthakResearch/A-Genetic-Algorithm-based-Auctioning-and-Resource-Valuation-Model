package SourceCode;
import java.util.*;

public class Chromosome {
    public int[] gene;
    public double fitness;
 
    public Chromosome(int size) {
        gene = new int[size];
    }
 
    public Chromosome clone() {
        Chromosome c = new Chromosome(gene.length);
        System.arraycopy(this.gene, 0, c.gene, 0, gene.length);
        c.fitness = this.fitness;
        return c;
    }

}
