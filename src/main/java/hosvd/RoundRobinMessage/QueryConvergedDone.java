package hosvd.RoundRobinMessage;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cxy on 15-10-22.
 */
public class QueryConvergedDone implements Serializable{
    private List<Boolean> converged;

    public QueryConvergedDone(List<Boolean> converged) {
        this.converged = converged;
    }

    public List<Boolean> getConverged() {
        return converged;
    }
}
