package hosvd.datatype;

import au.com.bytecode.opencsv.CSVReader;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by cxy on 15-10-22.
 */
public class Tensor3 {
    private double[][][] tensor;
    private final int rank = 3;
    private int x;
    private int y;
    private int z;

    private static Random random = new Random(47);

    public Tensor3(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        tensor = new double[z][x][y];
    }

    public int getX() { return x; }
    public int getY() { return y;}
    public int getZ() { return z;}
    public int getRank() { return rank;}


    public double getError() {
        double error = 0.0;
        for (int i = 0; i < x; i++)
            for (int j = 0; j < y; j++)
                for (int k = 0; k < z; k++)
                    error += tensor[k][i][j];
        return error;
    }


    public double norm() {
        double frob_norm = 0.0;
        for (int i = 0; i < x; i++)
            for (int j = 0; j < y; j++)
                for (int k = 0; k < z; k++)
                    frob_norm += Math.pow(get(i,j,k),2);
        return Math.sqrt(frob_norm);
    }

    public void set(int i, int j, int k, double value) { tensor[k][i][j] = value; }

    public double get(int i, int j, int k) { return tensor[k][i][j]; }

    //张量切片的矩阵形式
    public Matrix sliceToMatrix(int dim, int index) {
        Matrix result = null;
        switch (dim){
            case 3:
                result = Matrix.from2DArray(tensor[index]);
                break;
            case 2:
                result = new Matrix(x,z);
                for(int i = 0; i < x; i++)
                    for(int k = 0; k < z; k++)
                        result.set(i,k,tensor[k][i][index]);
                break;
            case 1:
                result = new Matrix(y,z);
                for(int j = 0; j < y; j++)
                    for(int k = 0; k < z; k++)
                        result.set(j,k,tensor[k][index][j]);
                break;
            default:
                result = Matrix.emptyMatrix();
        }
        return result;
    }

    //沿着第一个order进行切片
    public Tensor3 slice_1st(int start, int end) {
        int min_start = Math.min(start,end);
        int max_end = Math.max(start, end);
        int distance = max_end - min_start + 1;
        Tensor3 result = new Tensor3(distance,y,z);

        for(int i = 0; i < result.getX(); i++)
            for(int j = 0; j < result.getY(); j++)
                for(int k = 0; k < result.getZ(); k++)
                    result.set(i,j,k,get(min_start+i,j,k));

        return result;
    }

    //沿着3个order进行进行切块
    public Tensor3 slice(int startX, int endX, int startY, int endY, int startZ, int endZ) {
        int min_startX = Math.min(startX,endX);
        int max_endX = Math.max(startX, endX);
        int distanceX = max_endX - min_startX + 1;

        int min_startY = Math.min(startY,endY);
        int max_endY = Math.max(startY, endY);
        int distanceY = max_endY - min_startY + 1;

        int min_startZ = Math.min(startZ,endZ);
        int max_endZ = Math.max(startZ, endZ);
        int distanceZ = max_endZ - min_startZ + 1;

        Tensor3 result = new Tensor3(distanceX,distanceY,distanceZ);

        for(int i = 0; i < result.getX(); i++)
            for(int j = 0; j < result.getY(); j++)
                for(int k = 0; k < result.getZ(); k++)
                    result.set(i,j,k,get(min_startX+i, min_startY+j, min_startZ+k));

        return result;
    }

    //沿着第一个order进行切块
    public List<Tensor3> sliceEquallyBy1st(int count) {
        int firstDimPerTensor = getX() / count;
        int mod = getX() % count;
        int index = 0;
        int tempCol = 0;
        ArrayList<Tensor3> result = new ArrayList<Tensor3>();

        for(int i = 0; i < count; i++) {
            if(mod > 0)
                tempCol = firstDimPerTensor + 1;
            else
                tempCol = firstDimPerTensor;

            result.add(slice_1st(index,index+tempCol-1));
            index += tempCol;
            mod -= 1;
        }

        return result;
    }

    public List<Tensor3> sliceEqually(int[] count) {
        if (count.length != 3) {
            System.out.println("Must be 3 order tensor !!!");
            throw new RuntimeException();
        }

        return sliceEqually(count[0], count[1], count[2]);
    }

    //沿着3个order同时进行均匀切块
    //每一个order均匀切count块，则共有countX*countY*countZ个切块
    public List<Tensor3> sliceEqually(int countX, int countY, int countZ) {
        int firstDimPerTensor = getX() / countX;
        int secondDimPerTensor = getY() / countY;
        int thirdDimPerTensor = getZ() / countZ;
        int modX = getX() % countX;
        int modY = getY() % countY;
        int modZ = getZ() % countZ;
        int tempColX = 0;
        int tempColY = 0;
        int tempColZ = 0;
        int indexX = 0;
        int indexY = 0;
        int indexZ = 0;
        ArrayList<Tensor3> result = new ArrayList<Tensor3>(countX*countY*countZ);

        for(int i = 0; i < countX; i++) {
            if(modX > 0)
                tempColX = firstDimPerTensor +  1;
            else
                tempColX = firstDimPerTensor;

            for(int j = 0; j < countY; j++) {
                if(modY > 0)
                    tempColY = secondDimPerTensor + 1;
                else
                    tempColY = secondDimPerTensor;

                for(int k = 0; k < countZ; k++) {
                    if(modZ > 0)
                        tempColZ = thirdDimPerTensor + 1;
                    else
                        tempColZ = thirdDimPerTensor;

                    result.add(slice(indexX,tempColX+indexX-1,indexY,tempColY+indexY-1,indexZ,tempColZ+indexZ-1));

                    indexZ += tempColZ;
                    modZ--;
                }
                //复位Z相关的信息
                indexZ = 0;
                modZ = getZ() % countZ;
                tempColZ = 0;

                indexY += tempColY;
                modY--;
            }
            //复位Y相关的信息
            indexY = 0;
            modY = getY() % countY;
            tempColY = 0;

            indexX += tempColX;
            modX--;
        }

        return result;
    }


    //3阶张量按摸展开
    public Matrix matricization(int dim) {
        Matrix result = null;

        switch (dim) {
            //按模１展开
            case 1:
                result = new Matrix(x,y*z);
//                for(int k = 0; k < z; k++)
//                    for(int j = 0; j < y; j++)
//                        for(int i = 0; i < x; i++)
//                            result.set(i,j+y*k,get(i,j,k));
                for(int j = 0; j < y; j++)
                    for(int i = 0; i < x; i++)
                        for(int k = 0; k < z; k++)
                            result.set(i,j*z+k,get(i,j,k));
                break;

            //按模2展开
            case 2:
                result = new Matrix(y,z*x);
//                for(int k = 0; k < z; k++)
//                    for(int i = 0; i < x; i++)
//                        for(int j = 0; j < y; j++)
//                            result.set(j,i+x*k,get(i,j,k));
                for(int k = 0; k < z; k++)
                    for(int j = 0; j < y; j++)
                        for(int i = 0; i < x; i++)
                            result.set(j, k * x + i, get(i, j, k));
                break;

            //按模3展开
            case 3:
                result = new Matrix(z,x*y);
//                for(int j = 0; j < y; j++)
//                    for(int i = 0; i < x; i++)
//                        for(int k = 0; k < z; k++)
//                            result.set(k,i+x*j,get(i,j,k));
                for(int i = 0; i < x; i++)
                    for(int k = 0; k < z; k++)
                        for(int j = 0; j < y; j++)
                            result.set(k,i*y+j,get(i,j,k));
                break;

            default:
                throw new IllegalArgumentException();
        }
        return result;
    }

    //张量模n乘矩阵，
    public Tensor3 ttm(Matrix matrix, int dim) {
        Tensor3 result = null;

        switch (dim) {
            //模１乘
            case 1:
                //无法做乘法的情况暂未处理
                result = new Tensor3(matrix.row,y,z);
                for(int second = 0; second < y; second++) {
                    for(int index = 0; index < matrix.row; index++) {
                        for(int third = 0; third < z; third++) {
                            double tempValue = 0d;
                            for(int first = 0; first < x; first++) {
                                tempValue += this.get(first,second,third) * matrix.get(index,first);
                            }
                            result.set(index,second,third,tempValue);
                        }
                    }
                }
                break;

            //模2乘
            case 2:
                result = new Tensor3(x,matrix.row,z);
                for(int third = 0; third < z; third++) {
                    for(int index = 0; index < matrix.row; index++) {
                        for(int first = 0; first < x; first++) {
                            double tempValue = 0d;
                            for(int second = 0; second < y; second++) {
                                tempValue += this.get(first,second,third) * matrix.get(index,second);
                            }
                            result.set(first,index,third,tempValue);
                        }
                    }
                }
                break;

            //模3乘
            case 3:
                result = new Tensor3(x,y,matrix.row);
                for(int first = 0; first < x; first++) {
                    for(int index = 0; index < matrix.row; index++) {
                        for(int second = 0; second < y; second++) {
                            double tempValue = 0d;
                            for(int third = 0; third < z; third++) {
                                tempValue += this.get(first,second,third) * matrix.get(index,third);
                            }
                            result.set(first,second,index,tempValue);
                        }
                    }
                }
                break;

            default:
                throw new IllegalArgumentException();
        }

        return result;
    }

    public Tensor3 minus(Tensor3 that) {
        if( (this.x != that.x) && (this.y != that.y) && (this.z != that.z) ) {
            System.out.println("Matrix dot operation is not matched!");
            throw new IllegalArgumentException();
        } else {
            Tensor3 result = new Tensor3(this.x,this.y,this.z);
            for(int i = 0; i < result.x; i++) {
                for(int j = 0; j < result.y; j++) {
                    for(int k = 0; k < result.z; k++) {
                        result.set(i, j, k, Math.abs(Math.abs(this.get(i, j, k)) - Math.abs(that.get(i, j, k))));
                    }
                }
            }
            return result;
        }
    }

    @Override
    public String toString() {
        String result = "";
        for(int k = 0; k < z; k++) {
            result += sliceToMatrix(3,k).toString() + "\n";
        }
        return result;
    }

    public static Tensor3 emptyTensor3() {
        return new Tensor3(0,0,0);
    }

    public static Tensor3 random(int x, int y, int z) {
        Tensor3 result = new Tensor3(x,y,z);

        for(int i = 0; i < x; i++) {
            for(int j = 0; j < y; j++) {
                for(int k = 0; k < z; k++) {
                    result.set(i,j,k,random.nextDouble());
                }
            }
        }

        return result;
    }

    //从文件集中读取数据并构建张量
    //flag表示是否建立增量张量
    public static Tensor3 setTensor3(int x, int y, int z, String path, boolean flag) throws IOException {
        Tensor3 result = new Tensor3(x,y,z);
        File file;
        CSVReader csvReader = null;

        if(!flag) {
            try {
                //从模三面开始构造张量
                for (int k = 0; k < z; k++) {
                    file = new File(path + "/" + k + ".csv");
                    csvReader = new CSVReader(new FileReader(file));
                    List<String[]> data = csvReader.readAll();

                    for (int i = 0; i < Math.min(data.size(), x); i++) {
                        for (int j = 0; j < Math.min(data.get(0).length, y); j++) {
                            result.set(i, j, k, Double.parseDouble(data.get(i)[j]));
                        }
                    }
//            System.out.print("x is : " + Math.min(data.size(),x) + " y is : " + Math.min(data.get(0).length,y) +" \n");
                }
            } finally {
                csvReader.close();
            }
        } else {
            //如果是建立增量张量，则从文件中读取出上一次增量的一些相关信息
            file = new File("./tmp/Tol.txt");
            BufferedReader in = new BufferedReader(new FileReader(file));
            in.readLine();
            int lastX = Integer.parseInt(in.readLine());
            int lastY = Integer.parseInt(in.readLine());
            int lastZ = Integer.parseInt(in.readLine());
            in.close();

//            System.out.println(lastX + " " + lastY + " " + lastZ);

            //1.如果是按照模１增量的话：
            try {
                //从模三面开始构造张量
                for (int k = 0; k < z; k++) {
                    file = new File(path + "/" + (k + 1) + ".csv");
                    csvReader = new CSVReader(new FileReader(file));
                    List<String[]> data = csvReader.readAll();

                    for (int i = lastX; i < Math.min(data.size(), x+lastX); i++) {
                        for (int j = 0; j < Math.min(data.get(0).length, y); j++) {
                            result.set(i-lastX, j, k, Double.parseDouble(data.get(i)[j]));
                        }
                    }
//            System.out.print("x is : " + Math.min(data.size(),x) + " y is : " + Math.min(data.get(0).length,y) +" \n");
                }
            } finally {
                csvReader.close();
            }

        }

        return result;
    }

    //以增量的方式构建张量，沿着模一的方向增量构建张量
    //第一个参数代表一切面的起始位置，即开始的文件名
    //第二个参数代表一切面的结束位置，即结束的文件名
    // 有多少个模一切面，即有多少个文件
    public static Tensor3 setTensor3(int x_start, int x_end, int y, int z, String path) throws IOException {
        int fileNums = x_end - x_start + 1;
        Tensor3 result = new Tensor3(fileNums,y,z);
        File file;
        CSVReader csvReader = null;


        try {
            //从模三面开始构造张量
            for (int i = 0; i < fileNums; i++) {
                file = new File(path + "/" + (x_start+i) + ".csv");
                csvReader = new CSVReader(new FileReader(file));
                List<String[]> data = csvReader.readAll();

                for (int j = 0; j < Math.min(data.size(), y); j++) {
                    for (int k = 0; k < Math.min(data.get(0).length, z); k++) {
                        result.set(i, j, k, Double.parseDouble(data.get(j)[k]));
                    }
                }
//            System.out.print("x is : " + Math.min(data.size(),x) + " y is : " + Math.min(data.get(0).length,y) +" \n");
            }
        } finally {
            csvReader.close();
        }

        return result;
    }

    public static void main(String[] args) throws IOException {
        Matrix U1 = Matrix.readMatrix("/home/cxy/IdeaProjects/ClientServer/u1");
        U1 = U1.dot(U1.trans());
        System.out.println(U1);

        Matrix U2 = Matrix.readMatrix("/home/cxy/IdeaProjects/ClientServer/u2");
        U2 = U2.dot(U2.trans());
        System.out.println(U2);

        Matrix U = U1.combineMatrix(U1, U2, 2);
        Matrix V = Matrix.eye(U.col);
        System.out.println(U);

        boolean con = false;

        for (int i = 0; i < 200; i++) {
            System.out.println("In step :" + i);
            con = Matrix.innerOrth(U, V, Math.pow(U1.norm()+U2.norm(),2) * 1e-16);
            if (con) {
                System.out.println("Con!");
                break;
            }
        }
        U = U.normalizeU();
        U = U.sliceByCol(0, U.row - 1);
        Matrix.writeMatrixToFile(U, "u");
        System.out.println(U.dot(U.trans()));


//        Matrix.writeMatrixToFile(U1,"/home/cxy/test");
//        System.out.println(U1.dot(U1.trans()));
//
//
//        Tensor3 tensor3 = setTensor3(10, 10, 10, "/home/cxy/IdeaProjects/ClientServer/dataset", false);
//        System.out.println(tensor3.matricization(1));
//        System.exit(1);
////        System.out.println(tensor.tensor[0].length);
//        int[] count = {10, 10 ,10};
////        List<Tensor3> list = tensor3.sliceEqually(10, 10, 10);
//        List<Tensor3> list = tensor3.sliceEqually(count);
//
//        System.out.println("OK");
//        System.out.println(list.get(0).getX());
//
//        Tensor3 t = list.get(0);
//        Tensor tensor = new Tensor(3,20,20,20);
//
//        for (int k = 0; k < t.getZ(); k++) {
//            for (int i = 0; i < t.getX(); i++) {
//                for (int j = 0; j < t.getY(); j++) {
//                    tensor.set(t.get(i,j,k), i, j, k);
//                }
//            }
//        }
//
//        System.out.println(tensor.matricization(1));
//
//        System.out.println();
//        System.out.println();
//
//        System.out.println(t.matricization(1));

//        Tensor3 one = random(5, 5, 5);
//        Tensor3 two = random(5,5,5);
//
//        System.out.println(one);
//        System.out.println("----------------------------------");
//        System.out.println(two);



//        System.out.println(tensor.matricization(1));




//        Tensor3 tensor = Tensor3.random(3,2,3);
////        System.out.println(tensor);
////        System.out.println(tensor.get(0, 0, 0));
//        tensor.set(0, 0, 0, 1) ;tensor.set(0, 0, 1, 1) ;tensor.set(1, 0, 0, 1);
//        tensor.set(1, 0, 1, -1) ;tensor.set(1, 0, 2, 2) ;tensor.set(2, 0, 0, 2);
//        tensor.set(2, 0, 2, 2) ;tensor.set(0, 1, 0, 2); tensor.set(0, 1, 1, 2);
//        tensor.set(1, 1, 0, 2); tensor.set(1, 1, 1, -2); tensor.set(1, 1, 2, 4);
//        tensor.set(2, 1, 0, 4); tensor.set(2, 1, 2, 4); tensor.set(0, 0, 2, 0);
//        tensor.set(2, 0, 1, 0); tensor.set(0, 1, 2, 0); tensor.set(2, 1, 1, 0);
//        System.out.println(tensor);
//        System.out.println(tensor.matricization(1));
//        System.out.println(tensor.matricization(2));
//        System.out.println(tensor.matricization(3));
//
//        Matrix eye = Matrix.eye(2);
//        eye.set(0,1,1);
//        eye.set(1,0,1);
//
//        tensor = tensor.ttm(eye,2);
//
//        System.out.println(tensor);

//        List<Tensor3> list = tensor.sliceEqually(2,1,1);
//        int i = 0;
//        for(Tensor3 tensor3 : list)
//            System.out.println("tensor is : " + ++i + "\n"  + tensor3);


//
//        List<Tensor3> list = tensor.sliceEquallyBy1st(2);
//        System.out.println(list);
//
//        System.out.println(tensor.norm());
//
//        Matrix matrix = new Matrix(2, 2);
//        matrix.set(0, 0, 1); matrix.set(0, 1, 1); matrix.set(1, 0, 2) ;matrix.set(1, 1, 2);
//        System.out.println(matrix);
//        System.out.println(tensor.ttm(matrix, 1));
//
//        System.out.println(tensor.ttm(matrix, 1).sliceToMatrix(3, 0));
//        System.out.println(tensor.ttm(matrix, 1).sliceToMatrix(2,0));
//        System.out.println(tensor.ttm(matrix, 1).sliceToMatrix(1,0));

//        Tensor3 tensor = setTensor3(10,20,5,"./dataset",true);
//        System.out.println(tensor);




//
//        Matrix U1 = Matrix.readMatrix("./实验比较/分布式HOSVD/U1");
//        Matrix U2 = Matrix.readMatrix("./实验比较/分布式HOSVD/U2");
//        Matrix U3 = Matrix.readMatrix("./实验比较/分布式HOSVD/U3");
//        Tensor3 core1 = tensor.ttm(U1.trans(),1).ttm(U2.trans(),2).ttm(U3.trans(), 3);
//        Matrix.writeMatrixToFile(core1.matricization(1),"CoreTensor1");
//
//
//
//        Matrix U21 = Matrix.readMatrix("./实验比较/非分布式/U1");
//        Matrix U22 = Matrix.readMatrix("./实验比较/非分布式/U2");
//        Matrix U23 = Matrix.readMatrix("./实验比较/非分布式/U3");
//        Tensor3 core2 = tensor.ttm(U21.trans(),1).ttm(U22.trans(),2).ttm(U23.trans(), 3);
//        Matrix.writeMatrixToFile(core2.matricization(1),"CoreTensor2");
//
//        Tensor3 core_error = core1.minus(core2);
//        Matrix.writeMatrixToFile(core_error.matricization(1),"CoreTensor-error1");
//        Matrix.writeMatrixToFile(core_error.matricization(2),"CoreTensor-error2");
//        Matrix.writeMatrixToFile(core_error.matricization(3),"CoreTensor-error3");
//
//        Tensor3 raw1 = core1.ttm(U1,1).ttm(U2,2).ttm(U3,3);
//        Tensor3 raw2 = core2.ttm(U21,1).ttm(U22,2).ttm(U23, 3);
//        Matrix.writeMatrixToFile(raw1.matricization(1),"rawTensor1");
//        Matrix.writeMatrixToFile(raw2.matricization(1),"rawTensor2");
//        Tensor3 tensor_error = raw1.minus(raw2);
//        Matrix.writeMatrixToFile(tensor_error.matricization(1),"rawTensor-error1");
//        Matrix.writeMatrixToFile(tensor_error.matricization(2),"rawTensor-error2");
//        Matrix.writeMatrixToFile(tensor_error.matricization(3),"rawTensor-error3");

//        core1 = core1.ttm(U1,1).ttm(U2,2).ttm(U3,3);



//        Matrix mode1 = core.matricization(1);
//        Matrix.writeMatrixToFile(mode1,"mode11");

    }
}
