package hosvd.message;

/**
 * Created by cxy on 15-10-22.
 */
public class MultiInitialization {
    private int workerNumber;
    private int dim1;
    private int dim2;
    private int dim3;
    private int countX;
    private int countY;
    private int countZ;

    public MultiInitialization(int workerNumber, int dim1, int dim2, int dim3, int countX, int countY, int countZ) {
        this.workerNumber = workerNumber;
        this.dim1 = dim1;
        this.dim2 = dim2;
        this.dim3 = dim3;
        this.countX = countX;
        this.countY = countY;
        this.countZ = countZ;
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

    public int getCountX() {
        return countX;
    }

    public int getCountY() {
        return countY;
    }

    public int getCountZ() {
        return countZ;
    }
}
