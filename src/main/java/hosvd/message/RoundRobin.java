package hosvd.message;

import hosvd.datatype.Matrix;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cxy on 15-10-22.
 */
public class RoundRobin implements Serializable{
    private List<Matrix> data;
    private Matrix vMatrix;
    private String flag;

    public RoundRobin(List<Matrix> data, Matrix vMatrix, String flag) {
        this.data = data;
        this.vMatrix = vMatrix;
        this.flag = flag;
    }

    public List<Matrix> getData() {
        return data;
    }

    public Matrix getvMatrix() {
        return vMatrix;
    }

    public String getFlag() {
        return flag;
    }
}
