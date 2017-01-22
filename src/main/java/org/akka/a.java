package org.akka;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cxy on 16-1-22.
 */
public class a {
    public static void main(String[] args) {
        List<String>[] ls;
        List[] la = new List[10];
        ls = (List<String>[]) la;
        ls[0] = new ArrayList<String>();

        Object[] object = ls;


    }
}
