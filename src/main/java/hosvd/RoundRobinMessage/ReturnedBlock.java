package hosvd.RoundRobinMessage;

import hosvd.datatype.Matrix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cxy on 15-10-22.
 */
public class ReturnedBlock implements Serializable {
    private ArrayList<ArrayList<Matrix>> updataList;
    private ArrayList<ArrayList<Matrix>> dndataList;

    public ReturnedBlock(ArrayList<ArrayList<Matrix>> updataList, ArrayList<ArrayList<Matrix>> dndataList) {
        this.updataList = updataList;
        this.dndataList = dndataList;
    }

    public ArrayList<ArrayList<Matrix>> getUpdataList() {
        return updataList;
    }

    public ArrayList<ArrayList<Matrix>> getDndataList() {
        return dndataList;
    }
}
