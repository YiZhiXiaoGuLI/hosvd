package hosvd.datatype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cxy on 15-10-21.
 */
public class Vec implements Serializable{

    private ArrayList<Double> vector;
    private int size;

    public Vec(int size) {
        this.size = size;
        vector = new ArrayList<Double>(size);
    }

    public double norm() {
        double result = 0;
        for(int i = 0; i < size; i++) {
            result += vector.get(i) * vector.get(i);
        }
        return Math.sqrt(result);
    }

    public double dot(Vec that) throws IllegalArgumentException{
        if(this.size != that.size) {
            System.out.println("Two vector size do not match!");
            throw new IllegalArgumentException();
        } else {
            double result = 0;
            for(int i = 0; i < size; i++) {
                result += this.vector.get(i) * that.vector.get(i);
            }
            return result;
        }
    }

    public double get(int i) {
        return vector.get(i);
    }

    public int getSize() {return size;}

    public void set(int i, double value) {
        if(i >= size)
            System.out.println("Out of index bounds");
        else {
            vector.set(i,value);
        }
    }

    public void set(double value) {
        vector.add(value);
    }

    public Vec divide(double factor) {
        ArrayList<Double> result = new ArrayList<>();
        for(double value : vector) {
            result.add(value/factor);
        }
        return Vec.fromList(result);
    }

    @Override
    public String toString() {
        String str = vector.toString();
        return str;
    }

    public static Vec fromArray(double[] arr) {
        Vec vec = new Vec(arr.length);
        for(double value : arr)
            vec.set(value);
        return vec;
    }

    public static Vec fromList(List<Double> list) {
        Vec vec = new Vec(list.size());
        for(double value : list)
            vec.set(value);
        return vec;
    }

    public static void main(String[] args) {
        double[] y = {1, 3.0, 2.0};
        Vec v1 = Vec.fromArray(y);
        ArrayList<Double> list = new ArrayList<Double>();
//        System.out.println(list.size());
        list.add(2.0);
        list.add(2.0);
        list.add(1.0);
//        System.out.println(list.size());
        Vec v2 = Vec.fromList(list);

        double result = v1.dot(v2);
        System.out.println(result);

        System.out.println(v1);
        System.out.println(v1.norm());
        v1.divide(3);
        System.out.println(v1);
        System.out.println(v1.norm());

    }

}
