package hosvd.message;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cxy on 15-10-22.
 */
public class UpdateFilterList implements Serializable{
    private List<Integer> filterList;

    public UpdateFilterList(List<Integer> filterList) {
        this.filterList = filterList;
    }

    public List<Integer> getFilterList() {
        return filterList;
    }

    public void setFilterList(List<Integer> filterList) {
        this.filterList = filterList;
    }
}
