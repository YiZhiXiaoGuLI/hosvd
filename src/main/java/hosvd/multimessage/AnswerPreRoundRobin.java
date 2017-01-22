package hosvd.multimessage;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by cxy on 15-12-25.
 */
public class AnswerPreRoundRobin implements Serializable{
    private int[] TotalRow;
    private int[] Offset;
    private ActorRef[] childrenList;
    private double Tol;

    public AnswerPreRoundRobin(int[] totalRow, int[] offset, double Tol, ActorRef[] childrenList) {
        TotalRow = totalRow;
        Offset = offset;
        this.childrenList = childrenList;
        this.Tol = Tol;
    }

    public int[] getTotalRow() {
        return TotalRow;
    }

    public int[] getOffset() {
        return Offset;
    }

    public ActorRef[] getChildrenList() {
        return childrenList;
    }

    public double getTol() {
        return Tol;
    }
}
