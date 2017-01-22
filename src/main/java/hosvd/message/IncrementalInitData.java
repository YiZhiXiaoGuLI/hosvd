package hosvd.message;

import hosvd.datatype.Matrix;

import java.util.List;

/**
 * Created by cxy on 15-10-28.
 */
public class IncrementalInitData {

    private List<Matrix> upDataList;
    private List<Matrix> dnDataList;
    private Matrix vUpMatrix;
    private Matrix vDnMatrix;
    private double tol;
    private List<String> actorPathList;
    private int ranks;

    public IncrementalInitData(List<Matrix> upDataList, List<Matrix> dnDataList, Matrix vUpMatrix, Matrix vDnMatrix, double tol, List<String> actorPathList, int ranks) {
        this.upDataList = upDataList;
        this.dnDataList = dnDataList;
        this.vUpMatrix = vUpMatrix;
        this.vDnMatrix = vDnMatrix;
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

    public Matrix getvUpMatrix() {
        return vUpMatrix;
    }

    public Matrix getvDnMatrix() {
        return vDnMatrix;
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
