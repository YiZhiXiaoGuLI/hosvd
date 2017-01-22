package hosvd.message;

import hosvd.datatype.Matrix;
import hosvd.datatype.Tensor3;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cxy on 15-10-22.
 */
public class InitialData implements Serializable{
    private List<Matrix> upDataList;
    private List<Matrix> dnDataList;
    private Matrix vUp;
    private Matrix vDn;
    private double tol;
    private List<String> actorPathList;
    private int ranks;

    public InitialData(List<Matrix> upDataList, List<Matrix> dnDataList, Matrix vUp, Matrix vDn, double tol, List<String> actorPathList, int ranks) {
        this.upDataList = upDataList;
        this.dnDataList = dnDataList;
        this.vUp = vUp;
        this.vDn = vDn;
        this.tol = tol;
        this.actorPathList = actorPathList;
        this.ranks = ranks;
    }

    public List<Matrix> getUpDataList() {
        return upDataList;
    }

    public List<Matrix> getDnDataList() {
        return dnDataList;
    }

    public Matrix getvUp() {
        return vUp;
    }

    public Matrix getvDn() {
        return vDn;
    }

    public double getTol() {
        return tol;
    }

    public List<String> getActorPathList() {
        return actorPathList;
    }

    public int getRanks() {
        return ranks;
    }
}
