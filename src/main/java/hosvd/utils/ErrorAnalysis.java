package hosvd.utils;

import hosvd.datatype.Matrix;
import hosvd.datatype.Tensor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by cxy on 16-4-17.
 */
public class ErrorAnalysis {
    private static String fileLoader = "/home/cxy/experiment/error-4/";

    private static double[][] errors = new double[4][6];

//    private static String[] sp = {"[5, 2, 2]-4", "[5, 5, 2]-4", "[5, 5, 5]-4",
//                   "[10, 5, 5]-4", "[10, 10, 5]-4", "[10, 10, 10]-4"};

    private static String[] sp = {"[2, 2, 2, 4]-4", "[2, 2, 4, 4]-4", "[2, 4, 4, 4]-4", "[4, 4, 4, 4]-4"};


//    private static String sz = ", 50, 50]";
//    private static String sz = "[20, 20, 20, ";
    private static String sz = "[16, 16, ";
    private static String sz2 = ", 16]";

//    private static int[] start = {50, 80, 100, 130, 150, 180, 200, 230, 250};
//    private static int[] start = {20, 40, 60, 80, 100, 120};
    private static int[] start = {16, 32, 48, 64, 80, 96};


    //获得误差，并存放到数组当中
    public static void getErrors() throws FileNotFoundException {

        for (int j = 0; j < 6; j++) {
//            String size = "[" + start[j] + sz;
//            String size = sz + start[j] + "]";
            String size = sz + start[j] + sz2;

            for (int i = 0; i < 4; i++) {
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
        for (int i = 0; i < 4; i++) {
            System.out.println(Arrays.toString(errors[i]));
        }

    }

    public static void main(String[] args) throws IOException {
        getErrors();
    }
}
