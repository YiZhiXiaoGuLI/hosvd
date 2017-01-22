package hosvd.utils;

import hosvd.datatype.Matrix;
import hosvd.datatype.Tensor;
import hosvd.datatype.Tensor3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cxy on 16-4-17.
 */
public class ErrorCalculate {
//    private static String fileLoader = "/home/cxy/误差分析/";
//    private static String fileLoader = "/home/cxy/akka-2.3.14/bin/result/CaseStudyError/";
    private static String fileLoader = "/home/cxy/akka-2.3.14/bin/result/SencondPaper/error/X*100*50/";

    private static double[][] errors = new double[7][9];

    private static String[] sp = {"[2, 2, 2]-4", "[5, 2, 2]-4", "[5, 5, 2]-4", "[5, 5, 5]-4",
                   "[10, 5, 5]-4", "[10, 10, 5]-4", "[10, 10, 10]-4"};

    private static String sz = ", 50, 50]";

    private static int[] start = {50, 80, 100, 130, 150, 180, 200, 230, 250};


    //获得误差，并存放到数组当中
    public static void getErrors() throws FileNotFoundException {

        for (int j = 0; j < 9; j++) {
            String size = "[" + start[j] + sz;

            for (int i = 0; i < 7; i++) {
                String split = sp[i];
//                System.out.println(size + split);
                String fileName = size + split;
                File file = new File( fileLoader + fileName + "/error.txt");
                Scanner in = new Scanner(file);
                errors[i][j] = in.nextDouble();
                in.close();
            }
        }

//        System.out.println(Arrays.deepToString(errors));
        for (int i = 0; i < 7; i++) {
            System.out.println(Arrays.toString(errors[i]));
        }

    }

    public static void main(String[] args) throws IOException {
        File path = new File(fileLoader);
        String[] file = path.list();

//        double[][] result = new double[4][5];

        for (String fname : file) {
            Pattern pat = Pattern.compile("\\[(.+)\\]\\[(.+)\\]");
            Matcher mat = pat.matcher(fname);
            mat.find();
            String size = mat.group(1);
            String splits = mat.group(2);
//            int cores = Integer.parseInt(fname.split("-")[1]);
//            System.out.println(splits + "-" + cores);

            String[] temp = size.split(",");
            int[] dim = new int[temp.length];
            for (int i = 0; i < temp.length; i++) {
                dim[i] = Integer.parseInt(temp[i].trim());
            }

            System.out.println(Arrays.toString(dim));

//            System.exit(1);

//            Tensor3 rawTensor = Tensor3.setTensor3(dim[0], dim[1], dim[2], "/home/cxy/IdeaProjects/ClientServer/dataset", false);
//
            Tensor rawTensor = GetTime.getTensor(size, splits);

            Matrix U1 = Matrix.readMatrix(fileLoader + fname + "/" + "U-Mode1");
            Matrix U2 = Matrix.readMatrix(fileLoader + fname + "/" + "U-Mode2");
            Matrix U3 = Matrix.readMatrix(fileLoader + fname + "/" + "U-Mode3");

            Tensor simTensor = rawTensor.ttm(U1, 1).ttm(U2, 2).ttm(U3, 3);
            simTensor = simTensor.ttm(U1.trans(),1).ttm(U2.trans(), 2).ttm(U3.trans(), 3);
            simTensor = rawTensor.minus(simTensor);
            System.out.println(simTensor.getError() / 1000000.0);

            File destfile = new File(fileLoader + fname + "/" + "error.txt");
            PrintWriter printWriter = new PrintWriter(destfile);
            printWriter.print(simTensor.getError());
            printWriter.close();

        }

//        getErrors();
    }
}
