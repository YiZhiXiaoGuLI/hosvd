package hosvd.multimessage;

import hosvd.datatype.Tensor;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cxy on 15-11-6.
 */
public class TensorInitialization implements Serializable {
    //初始化的张量块
    private Tensor tensor;

    //记录每个阶上的分割数
    private int[] diviedList;

    //当前子张量块在上一层张量块的位置顺序标号
    private int numOfTensor;

    //精度值
    private double precision;

    public TensorInitialization(Tensor tensor, int[] diviedList, double precision,  int numOfTensor) {
        this.tensor = tensor;
        this.diviedList = diviedList;
        this.numOfTensor = numOfTensor;
        this.precision = precision;
    }

    public Tensor getTensor() {
        return tensor;
    }

    public int[] getDiviedList() {
        return diviedList;
    }

    public int getNumOfTensor() {
        return numOfTensor;
    }

    public double getPrecision() {
        return precision;
    }
}
