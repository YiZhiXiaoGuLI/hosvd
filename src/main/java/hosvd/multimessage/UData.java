package hosvd.multimessage;

import hosvd.datatype.Matrix;

import java.io.Serializable;

/**
 * Created by cxy on 16-3-3.
 */
public class UData implements Serializable{
    private Matrix U;
    private double tol;

    public UData(Matrix u, double tol) {
        U = u;
        this.tol = tol;
    }

    public Matrix getU() {
        return U;
    }

    public double getTol() {
        return tol;
    }
}
