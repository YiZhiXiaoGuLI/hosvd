package hosvd.datatype;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

import java.io.*;
import java.util.*;

/**
 * Created by cxy on 15-10-21.
 */
public class Matrix implements Serializable{
    private double matrix[][];
    public int row;
    public int col;



    public Matrix(int row, int col) {
        this.row = row;
        this.col = col;
        matrix = new double[row][col];
    }

    public double get(int i, int j) { return matrix[i][j]; }

    public double[][] getMatrix() {
        return matrix;
    }

    public double norm() {
        double result = 0;
        for(double[] arr : matrix) {
            for(double value : arr) {
                result += value*value;
            }
        }
        return Math.sqrt(result);
    }

    public boolean isEmpty() {
        if(row == 0 && col == 0)
            return true;
        else
            return false;
    }

    public void set(int i, int j, double value) {
        matrix[i][j] = value;
    }

    //根据一个列向量来生成一个矩阵的一列
    public void set(int colIndex, Vec arr) {
        for(int i = 0; i < arr.getSize(); i++) {
            matrix[i][colIndex] = arr.get(i);
        }
    }

    public Vec getCol(int colIndex) {
        ArrayList<Double> result = new ArrayList<>();
        for(int i = 0; i < row; i++) {
            result.add(matrix[i][colIndex]);
        }
        return Vec.fromList(result);
    }

    public Vec getRow(int rowIndex) {
        return Vec.fromArray(matrix[rowIndex]);
    }


    //按列追加
    public Matrix addByCol(Matrix that) {
        Matrix result = new Matrix(row,col+that.col);
        for(int j = 0; j < result.col; j++) {
            for(int i = 0; i < result.row; i++) {
                if(j < col) {
                    result.set(i,j,matrix[i][j]);
                }
                else{
                    result.set(i,j,that.get(i,j-col));
                }
            }
        }
        return result;
    }

    public Matrix addByRow(Matrix that) {
        Matrix result = new Matrix(row+that.row,col);
        for(int i = 0; i < result.row; i++) {
            for(int j = 0; j < result.col; j++) {
                if(i < row) {
                    result.set(i,j,matrix[i][j]);
                }
                else {
                    result.set(i,j,that.get(i-row,j));
                }
            }
        }
        return result;
    }

    //按照对剑先进行矩阵的拼接
    public Matrix addByDig(Matrix that) {
        Matrix result = new Matrix(this.row+that.row, this.col+that.col);

        for(int i = 0; i < result.row; i++) {
            for(int j = 0; j < result.col; j++) {

                if((i < this.row) && (j < this.col))
                    result.set(i,j,get(i,j));
                else if((i >= this.row) && (j >= this.col))
                    result.set(i,j,that.get(i-this.row, j-this.col));
                else
                    continue;
            }
        }
        return result;
    }

    //将矩阵进行切片，只取部分列
    public Matrix sliceByCol(int from, int to) {
        int max_col = Math.max(from,to);
        int min_col = Math.min(from, to);
        int slice_col = max_col - min_col + 1;
        Matrix result = new Matrix(row, slice_col);
        for(int i = 0; i < row; i++) {
            for(int j = 0; j < slice_col; j++) {
                result.set(i,j,get(i,j+min_col));
            }
        }
        return result;
    }

    //将矩阵进行切片，只取部分行
    public Matrix sliceByRow(int from, int to) {
        int max_row = Math.max(from,to);
        int min_row = Math.min(from, to);
        int slice_row = max_row - min_row + 1;
        Matrix result = new Matrix(slice_row, col);
        for(int i = 0; i < slice_row; i++) {
            for(int j = 0; j < col; j++) {
                result.set(i,j,get(i+min_row,j));
            }
        }
        return result;
    }

    public List<Matrix> sliceByColEqually(int pieces) {
        int colPerPiece = col/pieces;
        int mod = col%pieces;
        List<Matrix> result = new ArrayList<Matrix>();
        int index = 0;
        int tempCol;
        for(int i = 0; i < pieces; i++) {
            if(mod > 0)
                tempCol = colPerPiece+1;
            else
                tempCol = colPerPiece;
            result.add(sliceByCol(index,index+tempCol-1));

            index += tempCol;
            mod -= 1;
        }
        return result;
    }

    public List<Matrix> sliceByRowEqually(int pieces) {
        int rowPerPiece = row/pieces;
        int mod = row%pieces;
        List<Matrix> result = new ArrayList<Matrix>();
        int index = 0;
        int tempRow;
        for(int i = 0; i < pieces; i++) {
            if(mod > 0)
                tempRow = rowPerPiece+1;
            else
                tempRow = rowPerPiece;
            result.add(sliceByRow(index, index + tempRow - 1));

            index += tempRow;
            mod -= 1;
        }
        return result;
    }

    public Matrix trans() {
        Matrix result = new Matrix(col,row);
        for(int i = 0; i < col; i++)
            for(int j = 0; j < row; j++)
                result.set(i,j,get(j,i));

        return result;
    }

    public Matrix dot(Matrix that) {
        if(this.col != that.row) {
            System.out.println("Matrix dot operation is not matched!");
            throw new IllegalArgumentException();
        } else {
            Matrix result = new Matrix(this.row,that.col);
            for(int i = 0; i < result.row; i++) {
                for(int j = 0; j < result.col; j++) {
                    Vec rowVec = getRow(i);
                    Vec colVec = that.getCol(j);
                    result.set(i,j,rowVec.dot(colVec));
                }
            }
            return result;
        }
    }

    //矩阵乘以一个对角矩阵，对角矩阵用数组表示
    //用以节省存储空间
    public void dotInSelf(double[] SIGMA) {
        if(this.col != SIGMA.length) {
            System.out.println("Matrix dot operation is not matched!");
            throw new IllegalArgumentException();
        } else {
            for(int i = 0; i < this.col; i++) {
                for(int j = 0; j < this.row; j++) {
                    this.set(j,i, get(j,i)*SIGMA[i]);
                }
            }
        }
    }

    //矩阵乘以一个对角矩阵，对角矩阵用数组表示
    //用以节省存储空间
    public Matrix dot(double[] SIGMA) {
        if(this.col != SIGMA.length) {
            System.out.println("Matrix dot operation is not matched!");
            throw new IllegalArgumentException();
        } else {
            Matrix result = new Matrix(this.row,this.col);
            for(int i = 0; i < result.col; i++) {
                for(int j = 0; j < result.row; j++) {
                    result.set(j,i, get(j,i)*SIGMA[i]);
                }
            }
            return result;
        }
    }

    public Matrix minus(Matrix that) {
        if( (this.row != that.row) && (this.col != that.col) ) {
            System.out.println("Matrix dot operation is not matched!");
            throw new IllegalArgumentException();
        } else {
            Matrix result = new Matrix(this.row,this.col);
            for(int i = 0; i < result.row; i++) {
                for(int j = 0; j < result.col; j++) {
                    result.set(i,j,Math.abs(Math.abs(this.get(i, j)) - Math.abs(that.get(i, j))));
                }
            }
            return result;
        }
    }

    public Matrix normalizeU() {
        List<Tuple> normVecList = new ArrayList<Tuple>();
        Matrix result = new Matrix(row,col);

        for(int j = 0; j < col; j++) {
            Vec vector = getCol(j);
            normVecList.add(new Tuple(vector.norm(),vector.divide(vector.norm())));
        }

        //对存放Tuple的list进行排序，按照降序顺序
//        normVecList.sort(new TupleComparator());
        Collections.sort(normVecList,new TupleComparator());



        for(int j = 0; j < col; j++) {
            result.set(j,normVecList.get(j).getVector());
        }

        return result;
    }

    //归一化矩阵，每一列除以当前向量的norm，此norm为对应sigma矩阵的对角线元素
    public Matrix normalizeU(Matrix sigma) {
        List<Tuple> normVecList = new ArrayList<Tuple>();
        Matrix result = new Matrix(row,col);

        for(int j = 0; j < col; j++) {
            Vec vector = getCol(j);
            double norm = vector.norm();
            normVecList.add(new Tuple(norm,vector.divide(norm)));
        }

        //对存放Tuple的list进行排序，按照降序顺序
//        normVecList.sort(new TupleComparator());
        Collections.sort(normVecList,new TupleComparator());


        for(int j = 0; j < col; j++) {
            result.set(j,normVecList.get(j).getVector());
            sigma.set(j,j,normVecList.get(j).getNorm());
        }

        return result;
    }

    //归一化矩阵，每一列除以当前向量的norm，此norm为对应sigma矩阵的对角线元素
    public Matrix normalizeU(double[] sigma) {
        List<Tuple> normVecList = new ArrayList<Tuple>();
        Matrix result = new Matrix(row,col);

        for(int j = 0; j < col; j++) {
            Vec vector = getCol(j);
            double norm = vector.norm();
            normVecList.add(new Tuple(norm,vector.divide(norm)));
        }

        //对存放Tuple的list进行排序，按照降序顺序
//        normVecList.sort(new TupleComparator());
        Collections.sort(normVecList,new TupleComparator());


        for(int j = 0; j < col; j++) {
            result.set(j,normVecList.get(j).getVector());
            sigma[j] = normVecList.get(j).getNorm();
        }

        return result;
    }

    public Matrix normalizeV(Matrix matrixU) {
        List<Tuple> normVecList = new ArrayList<Tuple>();
        Matrix result = new Matrix(row,col);

        for(int j = 0; j < col; j++)
            normVecList.add(new Tuple(matrixU.getCol(j).norm(), this.getCol(j)));



        //对存放Tuple的list进行排序，按照降序顺序
//        normVecList.sort(new TupleComparator());
        Collections.sort(normVecList,new TupleComparator());


        for(int j = 0; j < col; j++) {
            result.set(j,normVecList.get(j).getVector());
        }

        return result;
    }


    @Override
    public String toString() {
//        String result = "[\n";
        String result = "";
        for(double[] arr: matrix) {
            for(double value : arr){
                result += value;
                result += "\b\t";
            }
            result += "\n";
        }
//        result += "]\n";
        return result;
    }

    public static class TupleComparator implements Comparator<Tuple> {

        @Override
        public int compare(Tuple o1, Tuple o2) {
            if (o1 == o2) {
                return 0;
            }
            else {
                if (o1.getNorm() > o2.getNorm())
                    return -1;
                else if (o1.getNorm() < o2.getNorm())
                    return 1;
                else
                    return 0;
            }
        }
    }

    public static class Tuple {
        private double norm;
        private Vec vector;
        public Tuple(double norm, Vec vector) {
            this.norm = norm;
            this.vector = vector;
        }

        public double getNorm(){ return norm; }
        public Vec getVector() { return vector; }
    }

    public static Matrix random(int row, int col) {
        Random rand = new Random(47);
        Matrix result = new Matrix(row,col);

        for(int i = 0; i < row; i++)
            for(int j = 0; j < col; j++)
                result.set(i,j,rand.nextDouble());

        return result;
    }

    public static Matrix from2DArray(double[][] arr) {
        int row = arr.length;
        int col = arr[0].length;
        Matrix result = new Matrix(row,col);

        for(int i = 0; i < row; i++)
            for(int j = 0; j < col; j++)
                result.set(i,j,arr[i][j]);

        return result;
    }

    public static Matrix emptyMatrix() {
        return new Matrix(0,0);
    }

    public static Matrix eye(int size) {
        Matrix result = new Matrix(size,size);

        for(int i = 0; i < size; i++) {
            result.set(i,i,1);
        }

        return result;
    }

    public static Matrix readMatrix(String path) throws IOException{
        File file;
        CSVReader csvReader = null;
        Matrix result = null;

        try {
            file = new File(path+".csv");
            csvReader = new CSVReader(new FileReader(file));
            List<String[]> data = csvReader.readAll();

            int x = data.size();
            int y = data.get(0).length;

            result = new Matrix(x,y);

            for (int i = 0; i < x; i++) {
                for (int j = 0; j < y; j++) {
                    result.set(i, j, Double.parseDouble(data.get(i)[j]));
//                    System.out.print(Double.parseDouble(data.get(i)[j]) + " ");
                }
            }
        } finally {
            csvReader.close();
        }

        return result;
    }

    //将矩阵写入到文件中，文件名为filename
    public  static void writeMatrixToFile(Matrix matrix, String filename) throws IOException {
        File path = new File(filename+".csv");
        Writer writer = new FileWriter(path);
        CSVWriter csvWriter = new CSVWriter(writer);

        String[] temp = new String[matrix.getMatrix()[0].length];
        for(double[] x : matrix.getMatrix()) {
            for (int i = 0; i < temp.length; i++) {
                temp[i] = String.valueOf(x[i]);
            }
            csvWriter.writeNext(temp);
        }
        csvWriter.close();
    }

    //按模一进行块内正交时特殊处理
    public static boolean innerOrth(Matrix block, Matrix v, double tol) {
        Matrix uMatrix = block;
        Matrix vMatrix = v;
        boolean converged = true;

        //一次round-robin循环，一次sweep来正交化矩阵
        for(int i = 0; i < block.col -1; i++) {
            for(int j = i+1; j < block.col; j++) {
                Vec di = block.getCol(i);
                Vec dj = block.getCol(j);
                double dii = di.dot(di);
                double dij = di.dot(dj);
                double djj = dj.dot(dj);

                //如果存在两列之间的正交化结果大于误差值，则判定为按照该模展开的矩阵未收敛
                if (Math.abs(dij) > tol) {
                    converged = false;

                    double tao = (djj - dii) / (2 * dij);
                    double t = Math.signum(tao) / (Math.abs(tao) + Math.sqrt(Math.pow(tao, 2) + 1));
                    double c = 1.0 / Math.sqrt(Math.pow(t, 2) + 1);
                    double s = t * c;

                    //update data block
                    //乘以旋转矩阵
                    for(int k = 0; k < block.row; k++) {
                        double res1 = block.get(k, i) * c - block.get(k, j) * s;
                        double res2 = block.get(k, i) * s + block.get(k, j) * c;
                        uMatrix.set(k,i,res1);
                        uMatrix.set(k,j,res2);
                    }

                    for(int k = 0; k < v.row; k++) {
                        double res3 = v.get(k, i) * c - v.get(k, j) * s;
                        double res4 = v.get(k, i) * s + v.get(k, j) * c;
                        vMatrix.set(k,i,res3);
                        vMatrix.set(k,j,res4);
                    }
                }
            }
        }
        return converged;
    }

    public static boolean outterOrth(Matrix block1, Matrix block2, Matrix v1, Matrix v2, double tol) {
        Matrix matrix1 = block1;
        Matrix matrix2 = block2;
        boolean converged = true;

        for(int i = 0; i < block1.col; i++) {
            for (int j = 0; j < block2.col; j++) {
                Vec di = block1.getCol(i);
                Vec dj = block2.getCol(j);
                double dii = di.dot(di);
                double dij = di.dot(dj);
                double djj = dj.dot(dj);

                //如果存在两列之间的正交化结果大于误差值，则判定为按照该模展开的矩阵未收敛
                if (Math.abs(dij) > tol) {
                    converged = false;

                    double tao = (djj - dii) / (2 * dij);
                    double t = Math.signum(tao) / (Math.abs(tao) + Math.sqrt(Math.pow(tao, 2) + 1));
                    double c = 1.0 / Math.sqrt(Math.pow(t, 2) + 1);
                    double s = t * c;

                    //update data block
                    //乘以旋转矩阵
                    for (int k = 0; k < block1.row; k++) {
                        double res1 = block1.get(k, i) * c - block2.get(k, j) * s;
                        double res2 = block1.get(k, i) * s + block2.get(k, j) * c;
                        matrix1.set(k, i, res1);
                        matrix2.set(k, j, res2);
                    }

                    for (int k = 0; k < v1.row; k++) {
                        double res3 = v1.get(k, i) * c - v2.get(k, j) * s;
                        double res4 = v1.get(k, i) * s + v2.get(k, j) * c;
                        v1.set(k, i, res3);
                        v2.set(k, j, res4);
                    }
                }
            }
        }
        return converged;
    }

    public static Matrix combineMatrix(Matrix one, Matrix two, int order) {
        //如果是按列拼接矩阵
        Matrix result = null;
        if (order == 2) {
            result = new Matrix(one.row, (one.col + two.col));
            for (int i = 0; i < result.row; i++) {
               for (int j = 0; j < result.col; j++) {
                   if (j < one.col)
                       result.matrix[i][j] = one.matrix[i][j];
                   else
                       result.matrix[i][j] = two.matrix[i][j - one.col];

               }
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        int start = 3;
        Matrix m1 = readMatrix("/home/cxy/akka-2.3.14/bin/result/90-90-90/U" + start + start);
        Matrix m2 = readMatrix("/home/cxy/akka-2.3.14/bin/result/90-90-90/U-mode" + start);
        writeMatrixToFile(m1.minus(m2),"/home/cxy/akka-2.3.14/bin/result/90-90-90/error-U" + start);

        Thread.sleep(3000);

        Matrix error = readMatrix("/home/cxy/akka-2.3.14/bin/result/90-90-90/error-U" + start);
        double tol = 0;
        for(int i = 0; i < error.row; i++) {
            for(int j = 0; j < error.col; j++) {
                tol += error.matrix[i][j];
            }
        }
        System.out.println(tol);
    }
}
