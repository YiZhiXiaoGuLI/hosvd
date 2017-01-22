package hosvd.RoundRobinMessage;

import hosvd.datatype.Matrix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cxy on 15-10-22.
 */
public class RoundRobin implements Serializable{
    private ArrayList<ArrayList<Matrix>> data;
    private String flag;

    public RoundRobin(ArrayList<ArrayList<Matrix>> data, String flag) {
        this.data = data;
        this.flag = flag;
    }

    public ArrayList<ArrayList<Matrix>> getData() {
        return data;
    }

    public String getFlag() {
        return flag;
    }
}
