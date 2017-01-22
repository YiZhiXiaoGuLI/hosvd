package hosvd.utils;

import hosvd.datatype.Matrix;
import hosvd.datatype.Tensor3;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * Created by cxy on 16-5-28.
 */
public class Error {
    public static void main(String[] args) throws IOException {
        int[] dim = {0, 0, 0};
        Arrays.fill(dim, 160);
        Tensor3 rawTensor = Tensor3.setTensor3(dim[0], dim[1], dim[2], "/home/cxy/IdeaProjects/ClientServer/dataset", false);
//        String filePath = "/home/cxy/akka-2.3.14/bin/result/10 * 10 * 10/";
        String filePath = "/home/cxy/akka-2.3.14/bin/result/" + dim[0] + " * " + dim[1] + " * " + dim[2] + "/";
        Matrix U1 = Matrix.readMatrix(filePath + "matrixU-1");
        Matrix U2 = Matrix.readMatrix(filePath + "matrixU-2");
        Matrix U3 = Matrix.readMatrix(filePath + "matrixU-3");

        Tensor3 simTensor = rawTensor.ttm(U1, 1).ttm(U2, 2).ttm(U3, 3);
        simTensor = simTensor.ttm(U1.trans(),1).ttm(U2.trans(), 2).ttm(U3.trans(), 3);
        simTensor = rawTensor.minus(simTensor);
        System.out.println(simTensor.getError());

        File destfile = new File(filePath + "/" + "error.txt");
        PrintWriter printWriter = new PrintWriter(destfile);
        printWriter.print(simTensor.getError());
        printWriter.close();
    }
}
