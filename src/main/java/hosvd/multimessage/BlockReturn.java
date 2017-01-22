package hosvd.multimessage;

import hosvd.datatype.Matrix;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cxy on 15-11-6.
 */
//一次子张量的合并结果的返回信息
public class BlockReturn implements Serializable {
    private List<Matrix> UList;
    private List<Matrix> VList;
    private int numOfTensor;
    //dim保存子张量块各个阶的大小
    //主要的目的是用于恢复子张量块在整体张量块中的位置
    private int[] dim;

    public BlockReturn(List<Matrix> UList, List<Matrix> VList, int numOfTensor, int[] dim) {
        this.UList = UList;
        this.VList = VList;
        this.numOfTensor = numOfTensor;
        this.dim = dim;
    }

    public List<Matrix> getUList() {
        return UList;
    }

    public List<Matrix> getVList() {
        return VList;
    }

    public int getNumOfTensor() {
        return numOfTensor;
    }

    public int[] getDim() {
        return dim;
    }
}
