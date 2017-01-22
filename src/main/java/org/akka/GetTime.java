package org.akka;

import hosvd.datatype.Matrix;
import hosvd.datatype.Tensor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cxy on 16-4-13.
 */


class TensorStruct {
    private String size;
    private String splits;
    private int[] timesWithCores;

    public TensorStruct(String size, String splits) {
        this.size = size;
        this.splits = splits;
        timesWithCores = new int[8];
        System.out.println(size + " " + splits + " has constructed!");
    }

    public void setTime(int i, int time) {
        timesWithCores[i-1] = time;
    }

    public String getSize() {
        return size;
    }

    public String getSplits() {
        return splits;
    }

    public int[] getTimesWithCores() {
        return timesWithCores;
    }
}

public class GetTime {

    private static Tensor getTensor(String tensorSize, String tensorSplits) {
        final int Ranks = 3;
        int Dim[] = new int[Ranks];
        int Count[] = new int[Ranks];

        int i = 0;
        for (String str : tensorSize.split(",")) {
            Dim[i++] = Integer.parseInt(str.trim());
        }
        i = 0;
        for (String str : tensorSplits.split(",")) {
            Count[i++] = Integer.parseInt(str.trim());
        }

        //子张量的个数
        int numOfSubtensor = 1;
        for(int c : Count)
            numOfSubtensor *= c;


        int[] dimOfSubtensor = new int[Ranks];
        int sizeOfSubtensor = 1;
        for(i = 0; i < Ranks; i++) {
            dimOfSubtensor[i] = Dim[i] / Count[i];
            sizeOfSubtensor *= dimOfSubtensor[i];
        }

        //子张量块的数据初始化
        ArrayList<Tensor> subTensors = new ArrayList<Tensor>(numOfSubtensor);
        Random random = new Random(47);
        for(i = 0; i < numOfSubtensor; i++) {
            Tensor tempTensor = new Tensor(3,dimOfSubtensor);
            for(int j = 0; j < sizeOfSubtensor; j++)
                tempTensor.data[j] = random.nextDouble();
            subTensors.add(tempTensor);
        }

//        System.out.println(subTensors.size());

        //小张量的按摸合并的过程
        for(int o = Ranks-1; o >= 0; o--) {
            ArrayList<Tensor> temp = new ArrayList<Tensor>();

            for(i = 0; i < subTensors.size(); i += Count[o]) {
                Tensor tensor = subTensors.get(i);
                for(int j = i+1; j < i+ Count[o]; j++) {
                    tensor = tensor.combine(o+1,subTensors.get(j));
                }
                temp.add(tensor);
            }
            subTensors = temp;
        }

        Tensor one = subTensors.get(0);
        return one;
    }

    public static void main(String[] args) throws FileNotFoundException {
        String folder = "/home/cxy/dataset/";
        File path = new File(folder);
        HashMap<String,Integer> map = new HashMap<>();

        HashMap<String,ArrayList<TensorStruct>> result = new HashMap<>();


        for (String str : path.list()) {
            String[] temp = str.split("-");
            String tag = temp[0];

            if (map.get(tag) == null) {
                map.put(tag,1);
            } else
                map.put(tag,1+map.get(tag));
        }

        Iterator<Map.Entry<String,Integer>> itr = map.entrySet().iterator();

        while (itr.hasNext()) {
            Map.Entry<String,Integer> entry = itr.next();
            String tensorInfo = entry.getKey();
            int sizeOfExperiments = entry.getValue();
            if (sizeOfExperiments < 8)
                continue;

//            System.out.println(tensorInfo);

            Pattern pat = Pattern.compile("\\[(.+)\\]\\[(.+)\\]");
            Matcher mat = pat.matcher(tensorInfo);
            mat.find();

            String size = mat.group(1);
            String splits = mat.group(2);

            TensorStruct struct = new TensorStruct(size, splits);
            Tensor rawTensor = getTensor(size, splits);

            for (int i = 1; i <= 8; i++) {
                try {
                    String pathOfi = folder + tensorInfo + "-" + i + "/";
                    Scanner in = new Scanner(new File(pathOfi+ "times.txt"));

                    int times = in.nextInt();
                    struct.setTime(i,times);

                    Matrix U1 = Matrix.readMatrix(pathOfi + "U-Mode1");
                    Matrix U2 = Matrix.readMatrix(pathOfi + "U-Mode2");
                    Matrix U3 = Matrix.readMatrix(pathOfi + "U-Mode3");

                    Tensor simTensor = rawTensor.ttm(U1,1).ttm(U2, 2).ttm(U3,3);
                    simTensor = simTensor.ttm(U1.trans(),1).ttm(U2.trans(), 2).ttm(U3.trans(), 3);
                    simTensor = rawTensor.minus(simTensor);
//                    System.out.println(simTensor.getError()/1000000.0);

                    File file = new File(pathOfi + "error.txt");
                    PrintWriter printWriter = new PrintWriter(file);
                    printWriter.print(simTensor.getError());
                    printWriter.close();

                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }

            ArrayList<TensorStruct> tempList = null;
            if ( (tempList = result.get(size)) == null) {
                tempList = new ArrayList<TensorStruct>();
                tempList.add(struct);
                result.put(size, tempList);
            } else {
                tempList.add(struct);
            }
        }

        System.out.println(Arrays.toString(result.entrySet().toArray()));

        for (Map.Entry<String, ArrayList<TensorStruct>> m : result.entrySet()) {
            System.out.println(m.getKey());
            ArrayList<TensorStruct> times = m.getValue();
            System.out.println("[ ");
            for (TensorStruct tensorStruct : times) {
//                System.out.println(Arrays.toString(tensorStruct.getTimesWithCores()) + ",");
                System.out.println(tensorStruct.getSplits());
            }
            System.out.println(" ]");

        }
    }
}
