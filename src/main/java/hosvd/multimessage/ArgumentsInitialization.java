package hosvd.multimessage;

import java.io.Serializable;

/**
 * Created by cxy on 15-11-6.
 */
public class ArgumentsInitialization implements Serializable{
    private int rank;
    private int[] dim;
    private int[] count;
    private double precision;
    private int cores;
    private int ip;
    private boolean roundrobin;

    public ArgumentsInitialization(int rank, int[] dim, int[] count,
                                   int cores, int ip, boolean roundrobin, double precision) {
        this.rank = rank;
        this.dim = dim;
        this.count = count;
        this.precision = precision;
        this.cores = cores;
        this.ip = ip;
        this.roundrobin = roundrobin;


        if( (rank != dim.length) || (rank != count.length)) {
            System.out.println("Argument is not compatiable!");
            throw new IllegalArgumentException("Argument is not compatiable!");
        }
    }

    public int getRank() {
        return rank;
    }

    public int[] getDim() {
        return dim;
    }

    public int[] getCount() {
        return count;
    }

    public int getCores() {
        return cores;
    }

    public double getPrecision() {
        return precision;
    }

    public int getIp() {
        return ip;
    }

    public boolean isRoundrobin() {
        return roundrobin;
    }
}
