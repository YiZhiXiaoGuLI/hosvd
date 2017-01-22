package hosvd.datatype;


import au.com.bytecode.opencsv.CSVReader;

import java.io.*;
import java.util.*;

/**
 * Created by cxy on 15-11-24.
 */
public class Tensor implements Serializable{
    //张量中存储的数据用一维数组来表示
    public double data[];
    private int Ranks;
    public int Len;
    public int Dim[];
    private final int Interval[];

    public Tensor(int ranks,int ...args) {
        if(args.length != ranks) {
            throw new IllegalArgumentException("Args is not compatible!");
        }
        this.Ranks = ranks;
        this.Dim = args;

        //各个阶上的长度的乘积，代表所有数据的长度
        Len = 1;
        for(int temp : args)
            Len *= temp;

        this.data = new double[Len];
        this.Interval = new int[ranks];

        //初始化interval数组
        for(int i = 0; i < Ranks-1; i++) {
            int result = 1;
            for(int j = i+1; j < Ranks; j++) {
                result *= Dim[j];
            }
            Interval[i] = result;
        }
        //该数组的最后一个元素为1
        Interval[ranks - 1] = 1;
    }

    /***********************************************私有方法***************************************************************/
    //参数cur_dim用于记录当前在哪一个阶上，level用于记录处理了几个阶
    //参数y用于记录展开矩阵的y值，interval记录各个阶的间隔
    //position记录各个阶上的坐标，result为展开矩阵
    private void recurUnfolding(int cur_dim, int level, int y, int[] interval, int[] position, Matrix result) {
        int tempy = 0;

        //递归的出口,当回到展开阶的位置时
        if(level == Ranks -1) {
            for(int pos = 0; pos < Dim[cur_dim]; pos++) {
                position[cur_dim] = pos;
                result.set(pos,y,get(position));
            }
            return;
        }
        else{
            //相当于外层的一层循环
            for(int pos = 0; pos < Dim[cur_dim]; pos++) {
                position[cur_dim] = pos;
                tempy = interval[level] *pos;
                tempy += y;
                recurUnfolding((cur_dim+1)%Ranks,level+1,tempy,interval,position,result);
            }
        }
    }

    //采用递归方式计算张量模乘矩阵
    private void recurTTM(int cur_dim, int level, int value, int[] position, Matrix matrix, Tensor result) {
        //到达矩阵运算的最小的单元层
        if(level == Ranks - 2) {
            for(int index = 0; index < matrix.row; index++) {
                for(int j = 0; j < Dim[cur_dim]; j++) {
                    position[cur_dim] = j;
                    double tempValue = 0d;
                    for(int i = 0; i < Dim[ (cur_dim+1) % Ranks]; i++) {
                        position[(cur_dim+1) % Ranks] = i;
                        tempValue += this.get(position) * matrix.get(index,i);
                    }
                    position[(cur_dim+1) % Ranks] = index;
                    result.set(tempValue,position);
                }
            }
            return;
        }
        else{
            //相当于外层的一层循环
            for(int pos = 0; pos < Dim[cur_dim]; pos++) {
                position[cur_dim] = pos;
                recurTTM((cur_dim + 1) % Ranks, level + 1, value, position, matrix, result);
            }
        }
    }

    //采用递归方式计算张量的合并
    private void recurCombine(int cur_dim, int level, int[] position, Tensor that, Tensor result) {
        if(level == Ranks -1) {
            for(int pos = 0; pos < result.Dim[cur_dim]; pos++) {
                position[cur_dim] = pos;
                if(pos < this.Dim[cur_dim]) {
                    result.set(this.get(position),position);
                } else {
                    int[] tempPos = position.clone();
                    tempPos[cur_dim] = pos - this.Dim[cur_dim];
                    result.set(that.get(tempPos),position);
                }
            }
            return ;
        } else {
            //相当于外层的一层循环
            for(int pos = 0; pos < Dim[cur_dim]; pos++) {
                position[cur_dim] = pos;
                recurCombine((cur_dim + 1) % Ranks, level + 1, position, that, result);
            }
        }
    }



    //采用递归方式计算张量的切块
    private void recurSlice(int cur_dim, int level, int start, int[] position, Tensor result) {
        if(level == Ranks -1) {
            for(int pos = 0; pos < result.Dim[cur_dim]; pos++) {
                position[cur_dim] = start + pos;
                double value = this.get(position);
                position[cur_dim] = pos;
                result.set(value,position);
            }
            return ;
        } else {
            //相当于外层的一层循环
            for(int pos = 0; pos < Dim[cur_dim]; pos++) {
                position[cur_dim] = pos;
                recurSlice((cur_dim + 1) % Ranks, level + 1, start, position, result);
            }
        }
    }
    /***********************************************私有方法END***************************************************************/

    //获取张量的阶数
    public int getRanks() {
        return Ranks;
    }

    //获取第i阶的长度
    public int getDim(int dim) {
        if(dim > Ranks) {
            throw new IllegalArgumentException("Args is not compatible!");
        }
        return Dim[dim-1];
    }

    //获取张量坐标位置在一维数组中的位置
    public int getIndex(int args[]) {
        int result = 0;
        for(int i = 0; i < args.length; i++) {
            result += args[i] * Interval[i];
        }
        return result;
    }

    public double get(int ...args) {
        if(args.length != Ranks) {
            throw new IllegalArgumentException("Args is not compatible!");
        }
        double result;
        result = data[getIndex(args)];
        return result;
    }

    public void set(double vaule, int ...args) {
        if(args.length != Ranks) {
            throw new IllegalArgumentException("Args is not compatible!");
        }
        data[getIndex(args)] = vaule;
    }

    //求张量的二范数
    public double norm() {
        double frob_norm = 0.0;
        for(int i = 0; i < Len; i++) {
            frob_norm += Math.pow(data[i],2);
        }
        return Math.sqrt(frob_norm);
    }


    //张量的按摸展开，按dim进行展开，dim的范围从：[1 , Ranks]
    public Matrix matricization(int dim) {
        if(dim > Ranks) {
            throw new IllegalArgumentException("Args is not compatible!");
        }
        Matrix result = new Matrix(Dim[dim-1],Len/Dim[dim-1]);

        //用于记录阶与阶之间的间隔值
        int interval[] = new int[Ranks - 1];

        //初始化interval数组
        for(int i = 0; i < Ranks-2; i++) {
            int temp = 1;
            for(int j = (dim+1+i)%Ranks; j != (dim-1)%Ranks; j = (j+1)%Ranks) {
                temp *= Dim[j];
            }
            interval[i] = temp;
        }
        //该数组的最后一个元素为1
        interval[Ranks - 2] = 1;

        int postion[] = new int[Ranks];


        //递归地处理张量展开过程中的各个阶的过程
        recurUnfolding(dim % Ranks, 0, 0, interval, postion, result);

        return result;
    }


    //张量模乘矩阵
    public Tensor ttm(Matrix matrix, int dim) {
        Tensor result = null;
        if(dim > Ranks) {
            throw new IllegalArgumentException("Args is not compatible!");
        }
        //深拷贝Dim数组
        int tempDim[] = Dim.clone();
        //改变tempDim数组的dim阶的长度，使其为matrix.row大小
        tempDim[dim-1] = matrix.row;
        //创建结果张量
        result = new Tensor(Ranks,tempDim);

        int position[] = new int[Ranks];

        recurTTM(dim % Ranks, 0, 0, position, matrix, result);
        return result;
    }


    //张量相减，用于计算近似张量与原始张量之间的误差，值大于等于0
    public Tensor minus(Tensor that) {
        if(this.Ranks != that.getRanks()) {
            throw new IllegalArgumentException("Args is not compatible!");
        }
        //比较各个阶上的长度是否一样
        for(int i = 0; i < Ranks; i++) {
            if(this.Dim[i] != that.Dim[i])
                throw new IllegalArgumentException("Args is not compatible!");
        }

        Tensor result = new Tensor(Ranks,Dim);
        double data[] = new double[Len];
        for(int i = 0; i < Len; i++) {
            data[i] = Math.abs( Math.abs(this.data[i]) - Math.abs(that.data[i]));
        }
        result.data = data;

        return result;
    }

    public double getError() {
        double error = 0d;
        for (double err : data)
            error += err;
        return error;
    }


    public Tensor combine(int dim, Tensor that) {
        Tensor result = null;
        if( (dim > Ranks) && (this.Ranks != that.getRanks())) {
            throw new IllegalArgumentException("Args is not compatible!");
        }

        //深拷贝Dim数组
        int tempDim[] = Dim.clone();
        //改变tempDim数组的dim阶的长度，使其为matrix.row大小
        tempDim[dim-1] = this.Dim[dim-1] + that.Dim[dim-1];
        //创建结果张量
        result = new Tensor(Ranks,tempDim);
        int position[] = new int[Ranks];

        recurCombine(dim % Ranks, 0, position, that, result);
        return result;
    }


    //沿着dim 阶进行均匀切块
    public List<Tensor> sliceEqually(int dim, int count) {
        if(dim > Ranks) {
            throw new IllegalArgumentException("Args is not compatible!");
        }

        List<Tensor> result = new ArrayList<Tensor>(count);
        int dimPerTensor = this.Dim[dim-1] / count;
        int mod = this.Dim[dim-1] % count;
        int tempCol = 0;
        int index = 0;

        for(int i = 0; i < count; i++) {
            if(mod > 0)
                tempCol = dimPerTensor + 1;
            else
                tempCol = dimPerTensor;

            result.add(slice(dim, index, index+tempCol-1));
            index += tempCol;
            mod -= 1;
        }

        return result;
    }

    //沿着某一阶进行切块，切从start - end的块
    public Tensor slice(int dim, int start, int end) {
        if(dim > Ranks) {
            throw new IllegalArgumentException("Args is not compatible!");
        }

        int[] tempDim = Dim.clone();
        int distance = end - start + 1;
        tempDim[dim-1] = distance;
        Tensor result = new Tensor(Ranks,tempDim);
        int[] position = new int[Ranks];

        recurSlice(dim%Ranks,0,start,position,result);

        return result;
    }



    //从文件集中读取数据并构建张量
    //flag表示是否建立增量张量
    public static Tensor setTensor3(int z, int x, int y, String path, boolean flag) throws IOException {
        Tensor result = new Tensor(3,z,x,y);
        File file;
        CSVReader csvReader = null;

        if(!flag) {
            try {
                //从模三面开始构造张量
                for (int k = 0; k < z; k++) {
                    file = new File(path + "/" + (k + 1) + ".csv");
                    csvReader = new CSVReader(new FileReader(file));
                    List<String[]> data = csvReader.readAll();

                    for (int i = 0; i < Math.min(data.size(), x); i++) {
                        for (int j = 0; j < Math.min(data.get(0).length, y); j++) {
                            result.set(Double.parseDouble(data.get(i)[j]), k, i, j);
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
                            result.set(Double.parseDouble(data.get(i)[j]), i-lastX, j, k);
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


    public static void main(String[] args) throws IOException {

//        Tensor tensor = setTensor3(5, 20, 20, "./dataset", false);
//        System.out.println(tensor.matricization(1));

//        System.out.println(random.nextDouble());


        Scanner sc = new Scanner(System.in);
        System.out.print("Input the number of the experiment :");
        int numOfexperiment = sc.nextInt();

        for (int exp = 0; exp < numOfexperiment; exp++) {

            System.out.println("Please input the order of the tensor: ");
            int order = sc.nextInt();
            System.out.println("The order of the tensor is: " + order);

            int[] dim = new int[order];
            int[] count = new int[order];

            for (int i = 0; i < order; i++) {
                System.out.println("Please input the number of the order" + (i + 1) + ": ");
                dim[i] = sc.nextInt();
                count[i] = 2;
            }

            for (int i = 0; i < order; i++) {
                System.out.println("Please input the divideds of the order" + (i + 1) + ": ");
                count[i] = sc.nextInt();
            }

            for (int i = 0; i < order; i++)
                System.out.print(dim[i] + " ,");

            System.out.println();

            for (int i = 0; i < order; i++)
                System.out.print(count[i] + " ,");

            System.out.println("Starting!!!");

            int[] Dim = dim;
            int[] Count = count;
            int Ranks = Dim.length;

            //子张量的个数
            int numOfSubtensor = 1;
            for (int c : Count)
                numOfSubtensor *= c;


            int[] dimOfSubtensor = new int[Ranks];
            int sizeOfSubtensor = 1;
            for (int i = 0; i < Ranks; i++) {
                dimOfSubtensor[i] = Dim[i] / Count[i];
                sizeOfSubtensor *= dimOfSubtensor[i];
            }

            //子张量块的数据初始化
            ArrayList<Tensor> subTensors = new ArrayList<Tensor>(numOfSubtensor);
            List<Tensor3> list = null;
            String destFile ="result/" + Arrays.toString(Dim) + Arrays.toString(Count);
            File file = new File(destFile);
            file.mkdir();

            //如果是三阶张量
            if (Ranks == 3) {
                Tensor3 tensor3 = Tensor3.setTensor3(Dim[0], Dim[1], Dim[2],
                        "/home/cxy/akka-2.3.14/dataset", false);

                list = tensor3.sliceEqually(Count);

                for (int i = 0; i < numOfSubtensor; i++) {
                    Tensor3 t = list.get(i);
                    Tensor tempTensor = new Tensor(Ranks, dimOfSubtensor);

                    for (int z = 0; z < t.getZ(); z++) {
                        for (int x = 0; x < t.getX(); x++) {
                            for (int y = 0; y < t.getY(); y++) {
                                tempTensor.set(t.get(x, y, z), x, y, z);
                            }
                        }
                    }
                    subTensors.add(tempTensor);
                }

                list.clear();
                list = null;
            } else {
                Random random = new Random(47);
                for (int i = 0; i < numOfSubtensor; i++) {
                    Tensor tempTensor = new Tensor(3, dimOfSubtensor);
                    for (int j = 0; j < sizeOfSubtensor; j++)
                        tempTensor.data[j] = random.nextDouble();
                    subTensors.add(tempTensor);
                }
            }

            System.out.println(subTensors.size());

            //小张量的按摸合并的过程
            for (int o = Ranks - 1; o >= 0; o--) {
                ArrayList<Tensor> temp = new ArrayList<Tensor>();

                for (int i = 0; i < subTensors.size(); i += Count[o]) {
                    Tensor tensor = subTensors.get(i);
                    for (int j = i + 1; j < i + Count[o]; j++) {
                        tensor = tensor.combine(o + 1, subTensors.get(j));
                    }
                    temp.add(tensor);
                }
                subTensors = temp;
            }

            Tensor one = subTensors.get(0);
            System.out.println(subTensors.size());
            System.out.println(Arrays.toString(one.Dim));


//        Matrix matrix = Matrix.random(1,800);

//        one = one.ttm(matrix, 1);
//        System.out.println(Arrays.toString(one.Dim));
//
//        System.exit(1);


            long start = System.currentTimeMillis();
            System.out.println("Started!");



            boolean con = false;
            try {
                //张量1的模2展开矩阵
                Matrix U1 = one.matricization(1);
                Matrix.writeMatrixToFile(U1, destFile + "/Mode11");
                Matrix V1 = Matrix.eye(U1.col);
                for (int i = 0; i < 200; i++) {
                    System.out.println("In step :" + i);
                    con = Matrix.innerOrth(U1, V1, Math.pow(one.norm(), 2) * 1e-16);
                    if (con) {
                        System.out.println("Con!");
                        break;
                    }
                }
                U1 = U1.normalizeU();
                U1 = U1.sliceByCol(0, U1.row - 1);
                Matrix.writeMatrixToFile(U1, destFile + "/U11");
                U1 = null;
                V1 = null;
            } catch (Exception e) {

            }

            try {
                Matrix U2 = one.matricization(2);
                Matrix.writeMatrixToFile(U2, destFile + "/Mode22");
                Matrix V2 = Matrix.eye(U2.col);
                for (int i = 0; i < 200; i++) {
                    System.out.println("In step :" + i);
                    con = Matrix.innerOrth(U2, V2, Math.pow(one.norm(), 2) * 1e-16);
                    if (con) {
                        System.out.println("Con!");
                        break;
                    }
                }
                U2 = U2.normalizeU();
                U2 = U2.sliceByCol(0, U2.row - 1);
                Matrix.writeMatrixToFile(U2, destFile + "/U22");
                U2 = null;
                V2 = null;
            } catch (Exception e) {

            }


            try {
                Matrix U3 = one.matricization(3);
                Matrix.writeMatrixToFile(U3, destFile + "/Mode33");
                Matrix V3 = Matrix.eye(U3.col);
                for (int i = 0; i < 200; i++) {
                    System.out.println("In step :" + i);
                    con = Matrix.innerOrth(U3, V3, Math.pow(one.norm(), 2) * 1e-16);
                    if (con) {
                        System.out.println("Con!");
                        break;
                    }
                }
                U3 = U3.normalizeU();
                U3 = U3.sliceByCol(0, U3.row - 1);
                Matrix.writeMatrixToFile(U3, destFile + "/U33");
                U3 = null;
                V3 = null;
            } catch (Exception e) {

            }



            long end = System.currentTimeMillis() - start;
            System.out.println(end);
            File fileT = new File(destFile + "/times.txt");
            fileT.createNewFile();
            FileWriter fileWriter = new FileWriter(fileT);
            fileWriter.write(String.valueOf(end));
            fileWriter.close();

        }
    }
}
