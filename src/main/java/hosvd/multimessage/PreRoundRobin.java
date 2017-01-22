package hosvd.multimessage;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by cxy on 15-12-25.
 */
public class PreRoundRobin implements Serializable{
    private ArrayList<Integer> VRow;
    private int serialNumber;
    private double Tol;

    public PreRoundRobin(ArrayList<Integer> VRow, int serialNumber, double Tol) {
        this.VRow = VRow;
        this.serialNumber = serialNumber;
        this.Tol = Tol;
    }

    public ArrayList<Integer> getVRow() {
        return VRow;
    }

    public int getSerialNumber() {
        return serialNumber;
    }

    public double getTol() {
        return Tol;
    }
}
