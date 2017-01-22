package hosvd.datatype;

import au.com.bytecode.opencsv.CSVReader;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by cxy on 15-10-22.
 */
public class Tensor4 {
    private double[][][][] tensor;
    private final int rank = 4;
    private int a;
    private int b;
    private int c;
    private int d;

    public Tensor4(int a, int b, int c, int d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        tensor = new double[d][c][a][b];
    }

    public int getRank() {
        return rank;
    }

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }

    public int getC() {
        return c;
    }

    public int getD() {
        return d;
    }


    public double norm() {
        double frob_norm = 0.0;
        for (int dim1 = 0; dim1 < a; dim1++)
            for (int dim2 = 0; dim2 < b; dim2++)
                for (int dim3 = 0; dim3 < c; dim3++)
                    for (int dim4 = 0; dim4 < d; dim4++)
                    frob_norm += Math.pow(get(dim1,dim2,dim3,dim4),2);
        return Math.sqrt(frob_norm);
    }

    public void set(int dim1, int dim2, int dim3, int dim4, double value) { tensor[dim4][dim3][dim1][dim2] = value; }

    public double get(int dim1, int dim2, int dim3, int dim4) { return tensor[dim4][dim3][dim1][dim2]; }


    //3阶张量按摸展开
    public Matrix matricization(int dim) {
        Matrix result = null;

        switch (dim) {
            //按模１展开
            case 1:
                result = new Matrix(a,b*c*d);
                for(int dim2 = 0; dim2 < b; dim2++) {
                    for(int dim3 = 0; dim3 < c; dim3++) {
                        for(int dim4 = 0; dim4 < d; dim4++) {
                            for(int dim1 = 0; dim1 < a; dim1++) {
                                result.set(dim1,dim2*c*d+dim3*d+dim4,get(dim1,dim2,dim3,dim4));
                            }
                        }
                    }
                }
                break;

            //按模2展开
            case 2:
                result = new Matrix(b,c*d*a);
                for(int dim3 = 0; dim3 < c; dim3++) {
                    for(int dim4 = 0; dim4 < d; dim4++) {
                        for(int dim1 = 0; dim1 < a; dim1++) {
                            for(int dim2 = 0; dim2 < b; dim2++) {
                                result.set(dim2,dim3*d*a+dim4*a+dim1,get(dim1,dim2,dim3,dim4));
                            }
                        }
                    }
                }
                break;

            //按模3展开
            case 3:
                result = new Matrix(c,d*a*b);
                for(int dim4 = 0; dim4 < d; dim4++) {
                    for(int dim1 = 0; dim1 < a; dim1++) {
                        for(int dim2 = 0; dim2 < b; dim2++) {
                            for(int dim3 = 0; dim3 < c; dim3++) {
                                result.set(dim3,dim4*a*b+dim1*b+dim2,get(dim1,dim2,dim3,dim4));
                            }
                        }
                    }
                }
                break;

            //按模4展开
            case 4:
                result = new Matrix(d,a*b*c);
                for(int dim1 = 0; dim1 < a; dim1++) {
                    for(int dim2 = 0; dim2 < b; dim2++) {
                        for(int dim3 = 0; dim3 < c; dim3++) {
                            for(int dim4 = 0; dim4 < d; dim4++) {
                                result.set(dim4,dim1*b*c+dim2*c+dim3,get(dim1,dim2,dim3,dim4));
                            }
                        }
                    }
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
        return result;

    }

    public Tensor4 combineWithTensor(final Tensor4 that, int dim) {

        Tensor4 result = null;
        int dim1 = 0;
        int dim2 = 0;
        int dim3 = 0;
        int dim4 = 0;

        //按摸进行张量块的合并
        switch (dim) {
            //沿着第一模进行合并
            case 1:
                dim1 = this.getA() + that.getA();
                dim2 = this.getB();
                dim3 = this.getC();
                dim4 = this.getD();
                result = new Tensor4(dim1,dim2,dim3,dim4);
                for(int l = 0; l < dim4; l++)
                    for(int k = 0; k < dim3; k++)
                        for(int j = 0; j < dim2; j++)
                            for(int i = 0; i < dim1; i++) {
                                if(i < this.getA()) {
                                    result.set(i,j,k,l,this.get(i,j,k,l));
                                } else {
                                    result.set(i,j,k,l,that.get(i-this.getA(),j,k,l));
                                }
                            }

                break;

            //沿着第二个模进行合并
            case 2:
                dim1 = this.getA();
                dim2 = this.getB() + that.getB();
                dim3 = this.getC();
                dim4 = this.getD();
                result = new Tensor4(dim1,dim2,dim3,dim4);
                for(int l = 0; l < dim4; l++)
                    for(int k = 0; k < dim3; k++)
                        for(int i = 0; i < dim1; i++)
                            for(int j = 0; j < dim2; j++) {
                                if(j < this.getB()) {
                                    result.set(i,j,k,l,this.get(i,j,k,l));
                                } else {
                                    result.set(i,j,k,l,that.get(i,j-this.getB(),k,l));
                                }
                            }

                break;

            //沿着第三个模进行合并
            case 3:
                dim1 = this.getA();
                dim2 = this.getB();
                dim3 = this.getC() + that.getC();
                dim4 = this.getD();
                result = new Tensor4(dim1,dim2,dim3,dim4);
                for(int l = 0; l < dim4; l++)
                    for(int i = 0; i < dim1; i++)
                        for(int j = 0; j < dim2; j++)
                            for(int k = 0; k < dim3; k++) {
                                if(k < this.getC()) {
                                    result.set(i,j,k,l,this.get(i,j,k,l));
                                } else {
                                    result.set(i,j,k,l,that.get(i,j,k-this.getC(),l));
                                }
                            }

                break;

            //沿着第四个模进行合并
            case 4:
                dim1 = this.getA();
                dim2 = this.getB();
                dim3 = this.getC();
                dim4 = this.getD() + that.getD();
                result = new Tensor4(dim1,dim2,dim3,dim4);
                for(int k = 0; k < dim3; k++)
                    for(int i = 0; i < dim1; i++)
                        for(int j = 0; j < dim2; j++)
                            for(int l = 0; l < dim4; l++) {
                                if(l < this.getD()) {
                                    result.set(i,j,k,l,this.get(i,j,k,l));
                                } else {
                                    result.set(i,j,k,l,that.get(i,j,k,l-this.getD()));
                                }
                            }

                break;
            default:
                break;
        }

        return result;
    }

    //张量模n乘矩阵，
    public Tensor4 ttm(Matrix matrix, int dim) {
        Tensor4 result = null;

        switch (dim) {
            //模１乘
            case 1:
                //无法做乘法的情况暂未处理
                result = new Tensor4(matrix.row,b,c,d);

                for(int second = 0; second < b; second++) {
                    for(int third = 0; third < c; third++) {
                        for(int index = 0; index < matrix.row; index++) {
                            for(int fourth = 0; fourth < d; fourth++) {
                                double tempValue = 0d;
                                for(int first = 0; first < a; first++) {
                                    tempValue += this.get(first,second,third,fourth) * matrix.get(index,first);
                                }
                                result.set(index,second,third,fourth,tempValue);
                            }
                        }
                    }
                }
                break;

            //模2乘
            case 2:
                result = new Tensor4(a,matrix.row,c,d);

                for(int third = 0; third < c; third++) {
                    for(int fourth = 0; fourth < d; fourth++) {
                        for(int index = 0; index < matrix.row; index++) {
                            for(int first = 0; first < a; first++) {
                                double tempValue = 0d;
                                for(int second = 0; second < b; second++) {
                                    tempValue += this.get(first,second,third,fourth) * matrix.get(index,second);
                                }
                                result.set(first,index,third,fourth,tempValue);
                            }
                        }
                    }
                }
                break;

            //模3乘
            case 3:
                result = new Tensor4(a,b,matrix.row,d);

                for(int fourth = 0; fourth < d; fourth++) {
                    for(int first = 0; first < a; first++) {
                        for(int index = 0; index < matrix.row; index++) {
                            for(int second = 0; second < b; second++) {
                                double tempValue = 0d;
                                for(int third = 0; third < c; third++) {
                                    tempValue += this.get(first,second,third,fourth) * matrix.get(index,third);
                                }
                                result.set(first,second,index,fourth,tempValue);
                            }
                        }
                    }
                }
                break;

            case 4:
                result = new Tensor4(a,b,c,matrix.row);

                for(int first = 0; first < a; first++) {
                    for(int second = 0; second < b; second++) {
                        for(int index = 0; index < matrix.row; index++) {
                            for(int third = 0; third < c; third++) {
                                double tempValue = 0d;
                                for(int fourth = 0; fourth < d; fourth++) {
                                    tempValue += this.get(first,second,third,fourth) * matrix.get(index,fourth);
                                }
                                result.set(first,second,third,index,tempValue);
                            }
                        }
                    }
                }
                break;

            default:
                throw new IllegalArgumentException();
        }

        return result;
    }

    public Tensor4 minus(Tensor4 that) {
        if( (this.a != that.a) && (this.b != that.b) && (this.c != that.c) && (this.d != that.d)) {
            System.out.println("Matrix dot operation is not matched!");
            throw new IllegalArgumentException();
        } else {
            Tensor4 result = new Tensor4(this.a,this.b,this.c,this.d);
            for(int i = 0; i < result.a; i++) {
                for(int j = 0; j < result.b; j++) {
                    for(int k = 0; k < result.c; k++) {
                        for(int l = 0; l < result.d; l++) {
                            result.set(i, j, k, l, Math.abs(Math.abs(this.get(i, j, k,l)) - Math.abs(that.get(i, j, k,l))));
                        }
                    }
                }
            }
            return result;
        }
    }


//    @Override
//    public String toString() {
//        String result = "";
//        for(int k = 0; k < z; k++) {
//            result += sliceToMatrix(3,k).toString() + "\n";
//        }
//        return result;
//    }

    public static Tensor4 emptyTensor3() {
        return new Tensor4(0,0,0,0);
    }

    public static Tensor4 random(int dim1, int dim2, int dim3, int dim4) {
        Random random = new Random(47);
        Tensor4 result = new Tensor4(dim1,dim2, dim3,dim4);

        for(int l = 0; l < dim4; l++)
            for(int k = 0; k < dim3; k++)
                for(int i = 0; i < dim1; i++)
                    for(int j = 0; j < dim2; j++)
                        result.set(i,j,k,l,random.nextDouble());

        return result;
    }

    public static Tensor4 init_ten(int dim1, int dim2, int dim3, int dim4, int start_count) {
        Tensor4 result = new Tensor4(dim1,dim2,dim3,dim4);

        for(int l = 0; l < dim4; l++)
            for(int k = 0; k < dim3; k++)
                for(int i = 0; i < dim1; i++)
                    for(int j = 0; j < dim2; j++)
                        result.set(i,j,k,l,start_count++);

        return result;

    }


    public static void main(String[] args) throws IOException {

        Tensor4 o = init_ten(2,2,2,2,0);
        System.out.println(o.matricization(4));
        System.exit(1);

        File file = new File("file1.txt");
        PrintWriter pt = new PrintWriter(file);

        pt.println("2*2*2*2张量1: ");

        Integer count = 1;
        Tensor4 one = init_ten(3,4,5,6,count);
        pt.println("模一展开: ");
        pt.println(one.matricization(1));
        pt.println();
        pt.println("模二展开: ");
        pt.println(one.matricization(2));
        pt.println();
        pt.println("模三展开: ");
        pt.println(one.matricization(3));
        pt.println();
        pt.println("模四展开: ");
        pt.println(one.matricization(4));
        pt.println("---------------------------");
        count += 3*4*5*6;
//        count += 2*2*2*2;


        boolean con = false;

        Matrix U1 = one.matricization(1);
        Matrix V1 = Matrix.eye(U1.col);
        for(int i = 0; i < 5000; i++) {
            con = Matrix.innerOrth(U1, V1, Math.pow(one.norm(), 2) * 1e-30);
            if(con) {
                pt.println("Con!");
                break;
            }
        }


        U1 = U1.normalizeU();
        U1 = U1.sliceByCol(0,U1.row-1);

        Matrix U2 = one.matricization(2);
        Matrix V2 = Matrix.eye(U2.col);
        for(int i = 0; i < 5000; i++) {
            con = Matrix.innerOrth(U2, V2, Math.pow(one.norm(), 2) * 1e-30);
            if(con) {
                pt.println("Con!");
                break;
            }
        }

        U2 = U2.normalizeU();
        U2 = U2.sliceByCol(0,U2.row-1);

        Matrix U3 = one.matricization(3);
        Matrix V3 = Matrix.eye(U3.col);
        for(int i = 0; i < 5000; i++) {
            con = Matrix.innerOrth(U3, V3, Math.pow(one.norm(), 2) * 1e-30);
            if(con) {
                pt.println("Con!");
                break;
            }
        }

        U3 = U3.normalizeU();
        U3 = U3.sliceByCol(0,U3.row-1);

        Matrix U4 = one.matricization(4);
        Matrix V4 = Matrix.eye(U4.col);
        for(int i = 0; i < 5000; i++) {
            con = Matrix.innerOrth(U4, V4, Math.pow(one.norm(), 2) * 1e-30);
            if(con) {
                pt.println("Con!");
                break;
            }
        }


        U4 = U4.normalizeU();
        U4 = U4.sliceByCol(0, U4.row - 1);

        Tensor4 a = one.ttm(U1.trans(),1).ttm(U2.trans(),2).ttm(U3.trans(),3).ttm(U4.trans(), 4);
        a = a.ttm(U1,1).ttm(U2, 2).ttm(U3,3).ttm(U4, 4);

        Tensor4 c = one.minus(a);

        pt.println(a.matricization(1));
        pt.println();
        pt.println(a.matricization(2));
        pt.println();
        pt.println(a.matricization(3));
        pt.println();
        pt.println(a.matricization(4));
        pt.println();
        Matrix.writeMatrixToFile(c.matricization(1),"Tensor4-error1");
        Matrix.writeMatrixToFile(c.matricization(2),"Tensor4-error2");
        Matrix.writeMatrixToFile(c.matricization(3),"Tensor4-error3");
        Matrix.writeMatrixToFile(c.matricization(4),"Tensor4-error4");

        pt.close();


        System.exit(0);




        pt.println("2*2*2*2张量2: ");
        Tensor4 two = init_ten(3,4,5,6,count);
//        count += 2*2*2*2;
        pt.println("模一展开: ");
        pt.println(two.matricization(1));
        pt.println();
        pt.println("模二展开: ");
        pt.println(two.matricization(2));
        pt.println();
        pt.println("模三展开: ");
        pt.println(two.matricization(3));
        pt.println();
        pt.println("模四展开: ");
        pt.println(two.matricization(4));
        pt.println("---------------------------");


        Matrix U21 = two.matricization(1);
        Matrix V21 = Matrix.eye(U21.col);
        for(int i = 0; i < 100; i++) {
            con = Matrix.innerOrth(U21, V21, Math.pow(two.norm(), 2) * 1e-15);
            if(con)
                break;
        }
//        U1 = U1.normalizeU();
//        U1 = U1.sliceByCol(0,U1.row-1);

        Matrix U22 = two.matricization(2);
        Matrix V22 = Matrix.eye(U22.col);
        for(int i = 0; i < 100; i++) {
            con = Matrix.innerOrth(U22, V22, Math.pow(two.norm(), 2) * 1e-15);
            if(con)
                break;
        }
//        U2 = U2.normalizeU();
//        U2 = U2.sliceByCol(0,U2.row-1);

        Matrix U23 = two.matricization(3);
        Matrix V23 = Matrix.eye(U23.col);
        for(int i = 0; i < 100; i++) {
            con = Matrix.innerOrth(U23, V23, Math.pow(two.norm(), 2) * 1e-15);
            if(con)
                break;
        }
//        U3 = U3.normalizeU();
//        U3 = U3.sliceByCol(0,U3.row-1);

        Matrix U24 = two.matricization(4);
        Matrix V24 = Matrix.eye(U24.col);
        for(int i = 0; i < 100; i++) {
            con = Matrix.innerOrth(U24, V24, Math.pow(two.norm(), 2) * 1e-15);
            if(con)
                break;
        }


        //模1增量
        Tensor4 three = one.combineWithTensor(two, 1);

        Matrix U32 = three.matricization(2);
        Matrix V32 = Matrix.eye(U32.col);
        for(int i = 0; i < 100; i++) {
            con = Matrix.innerOrth(U32, V32, Math.pow(three.norm(), 2) * 1e-15);
            if(con)
                break;
        }
        U32 = U32.normalizeU();
        U32 = U32.sliceByCol(0, U32.row - 1);

        pt.println("***************************************");
        pt.println(U32);
        pt.println("***************************************");


        Matrix U = U2.addByCol(U22);
        Matrix V = V2.addByDig(V22);
        for(int i = 0; i < 100; i++) {
            con = Matrix.innerOrth(U, V, Math.pow(three.norm(), 2) * 1e-15);
            if(con)
                break;
        }
        U = U.normalizeU();
        U = U.sliceByCol(0, U.row - 1);

        pt.println("***************************************");
        pt.println(U);
        pt.println("***************************************");
        pt.close();

        System.exit(0);










        pt.println("张量按模一合并: ");
        pt.println("合并后的张量做展开： ");
//        Tensor4 three = one.combineWithTensor(two, 1);
        pt.println("模一展开: ");
        pt.println(three.matricization(1));
        pt.println();
        pt.println("模二展开: ");
        pt.println(three.matricization(2));
        pt.println();
        pt.println("模三展开: ");
        pt.println(three.matricization(3));
        pt.println();
        pt.println("模四展开: ");
        pt.println(three.matricization(4));
        pt.println("---------------------------");

        pt.println("张量按模二合并: ");
        pt.println("合并后的张量做展开： ");
        three = one.combineWithTensor(two, 2);
        pt.println("模一展开: ");
        pt.println(three.matricization(1));
        pt.println();
        pt.println("模二展开: ");
        pt.println(three.matricization(2));
        pt.println();
        pt.println("模三展开: ");
        pt.println(three.matricization(3));
        pt.println();
        pt.println("模四展开: ");
        pt.println(three.matricization(4));
        pt.println("---------------------------");

        pt.println("张量按模三合并: ");
        pt.println("合并后的张量做展开： ");
        three = one.combineWithTensor(two, 3);
        pt.println("模一展开: ");
        pt.println(three.matricization(1));
        pt.println();
        pt.println("模二展开: ");
        pt.println(three.matricization(2));
        pt.println();
        pt.println("模三展开: ");
        pt.println(three.matricization(3));
        pt.println();
        pt.println("模四展开: ");
        pt.println(three.matricization(4));
        pt.println("---------------------------");

        pt.println("张量按模四合并: ");
        pt.println("合并后的张量做展开： ");
        three = one.combineWithTensor(two, 4);
        pt.println("模一展开: ");
        pt.println(three.matricization(1));
        pt.println();
        pt.println("模二展开: ");
        pt.println(three.matricization(2));
        pt.println();
        pt.println("模三展开: ");
        pt.println(three.matricization(3));
        pt.println();
        pt.println("模四展开: ");
        pt.println(three.matricization(4));
        pt.println("---------------------------");

        pt.close();
    }
}
