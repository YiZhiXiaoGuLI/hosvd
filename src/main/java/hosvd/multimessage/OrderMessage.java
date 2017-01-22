package hosvd.multimessage;

import java.io.Serializable;

/**
 * Created by cxy on 15-12-23.
 */
public class OrderMessage implements Serializable{
    private int[] workerNum;
    private int[] dividedList;

    public OrderMessage(int[] workerNum, int[] dividedList) {
        this.workerNum = workerNum;
        this.dividedList = dividedList;
    }

    public int[] getWorkerNum() {
        return workerNum;
    }

    public int[] getDividedList() {
        return dividedList;
    }

}
