package hosvd.message;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cxy on 15-10-22.
 */
public class IterationEnd implements Serializable{
    private List<Boolean> converged;

    public IterationEnd(List<Boolean> converged) {
        this.converged = converged;
    }

    public List<Boolean> getConverged() {
        return converged;
    }

    public void setConverged(List<Boolean> converged) {
        this.converged = converged;
    }
}
