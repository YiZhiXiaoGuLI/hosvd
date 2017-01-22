package hosvd.message;

import hosvd.datatype.Matrix;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cxy on 15-10-22.
 */
public class ReturnedBlock implements Serializable{
    private List<Matrix> up;
    private List<Matrix> dn;
    private Matrix vUp;
    private Matrix vDn;

    public ReturnedBlock(List<Matrix> up, List<Matrix> dn, Matrix vUp, Matrix vDn) {
        this.up = up;
        this.dn = dn;
        this.vUp = vUp;
        this.vDn = vDn;
    }

    public List<Matrix> getUp() {
        return up;
    }

    public void setUp(List<Matrix> up) {
        this.up = up;
    }

    public List<Matrix> getDn() {
        return dn;
    }

    public void setDn(List<Matrix> dn) {
        this.dn = dn;
    }

    public Matrix getvUp() {
        return vUp;
    }

    public void setvUp(Matrix vUp) {
        this.vUp = vUp;
    }

    public Matrix getvDn() {
        return vDn;
    }

    public void setvDn(Matrix vDn) {
        this.vDn = vDn;
    }
}
