package hosvd.message;

import java.io.Serializable;

/**
 * Created by cxy on 15-10-22.
 */
public class Initialization implements Serializable{
    private int workerNumber;
    private int dim1;
    private int dim2;
    private int dim3;
    private boolean flag_incr = false;
    private int startIP = 56;

    public Initialization(int workerNumber, int dim1, int dim2, int dim3) {
        this.workerNumber = workerNumber;
        this.dim1 = dim1;

        this.dim2 = dim2;
        this.dim3 = dim3;
    }

    public Initialization(int workerNumber, int dim1, int dim2, int dim3, boolean flag_incr) {
        this(workerNumber,dim1,dim2,dim3);
        this.flag_incr = flag_incr;
    }

    public Initialization(int workerNumber, int dim1, int dim2, int dim3, boolean flag_incr, int startIP) {
        this(workerNumber,dim1,dim2,dim3);
        this.flag_incr = flag_incr;
        this.startIP = startIP;
    }

    public int getStartIP() {
        return startIP;
    }

    public void setWorkerNumber(int workerNumber) {
        this.workerNumber = workerNumber;
    }

    public void setDim1(int dim1) {
        this.dim1 = dim1;
    }

    public void setDim2(int dim2) {
        this.dim2 = dim2;
    }

    public void setDim3(int dim3) {
        this.dim3 = dim3;
    }

    public int getWorkerNumber() {
        return workerNumber;
    }

    public int getDim1() {
        return dim1;
    }

    public int getDim2() {
        return dim2;
    }

    public int getDim3() {
        return dim3;
    }

    public boolean getFlag_incr() {
        return flag_incr;
    }
}
