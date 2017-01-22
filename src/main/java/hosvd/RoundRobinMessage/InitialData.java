package hosvd.RoundRobinMessage;

import hosvd.datatype.Matrix;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cxy on 15-10-22.
 */
public class InitialData implements Serializable{
    private List<Matrix> upUDataList;
    private List<Matrix> dnUDataList;
    private List<Matrix> upVDataList;
    private List<Matrix> dnVDataList;
    private List<String> actorPathList;

    public InitialData(List<Matrix> upUDataList, List<Matrix> dnUDataList,
                       List<Matrix> upVDataList, List<Matrix> dnVDataList,
                       List<String> actorPathList) {
        this.upUDataList = upUDataList;
        this.dnUDataList = dnUDataList;
        this.upVDataList = upVDataList;
        this.dnVDataList = dnVDataList;
        this.actorPathList = actorPathList;
    }

    public List<Matrix> getUpUDataList() {
        return upUDataList;
    }

    public List<Matrix> getDnUDataList() {
        return dnUDataList;
    }

    public List<Matrix> getUpVDataList() {
        return upVDataList;
    }

    public List<Matrix> getDnVDataList() {
        return dnVDataList;
    }

    public List<String> getActorPathList() {
        return actorPathList;
    }
}
