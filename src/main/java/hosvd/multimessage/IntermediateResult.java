package hosvd.multimessage;

import hosvd.datatype.Matrix;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cxy on 15-11-6.
 */
//一次子张量的合并结果的返回信息
public class IntermediateResult implements Serializable {
    private List<Matrix> UList;
    private List<Matrix> VList;
    //序列号，用于记录该块在合并过程中处于的位置
    private int serialNumber;
    //tol用于保存子张量块的误差量
    private double tol;
    //dim保存子张量块各个阶的大小
    //主要的目的是用于恢复子张量块在整体张量块中的位置
    private int[] dim;


    public IntermediateResult(List<Matrix> UList, List<Matrix> VList, int serialNumber, double tol, int[] dim) {
        this.UList = UList;
        this.VList = VList;
        this.serialNumber = serialNumber;
        this.tol = tol;
        this.dim = dim;
    }

    public List<Matrix> getUList() {
        return UList;
    }

    public List<Matrix> getVList() {
        return VList;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public double getTol() {
        return tol;
    }

    public int[] getDim() {
        return dim;
    }
}
