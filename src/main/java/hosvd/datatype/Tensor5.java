package hosvd.datatype;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Created by cxy on 15-10-22.
 */
public class Tensor5 {
    private double[][][][][] tensor;
    private final int rank = 5;
    private int a;
    private int b;
    private int c;
    private int d;
    private int e;

    public Tensor5(int a, int b, int c, int d, int e) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
        tensor = new double[e][d][c][a][b];
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

    public int getE() {
        return e;
    }


    public double norm() {
        double frob_norm = 0.0;
        for (int dim1 = 0; dim1 < a; dim1++)
            for (int dim2 = 0; dim2 < b; dim2++)
                for (int dim3 = 0; dim3 < c; dim3++)
                    for (int dim4 = 0; dim4 < d; dim4++)
                        for(int dim5 = 0; dim5 < e; dim5++)
                            frob_norm += Math.pow(get(dim1,dim2,dim3,dim4,dim5),2);
        return Math.sqrt(frob_norm);
    }

    public void set(int dim1, int dim2, int dim3, int dim4, int dim5, double value) {
        tensor[dim5][dim4][dim3][dim1][dim2] = value;
    }

    public double get(int dim1, int dim2, int dim3, int dim4, int dim5) { return tensor[dim5][dim4][dim3][dim1][dim2]; }


    //5阶张量按摸展开
    public Matrix matricization(int dim) {
        Matrix result = null;

        switch (dim) {
            //按模１展开
            case 1:
                result = new Matrix(a,b*c*d*e);
                for(int dim2 = 0; dim2 < b; dim2++) {
                    for(int dim3 = 0; dim3 < c; dim3++) {
                        for(int dim4 = 0; dim4 < d; dim4++) {
                            for(int dim5 = 0; dim5 < e; dim5++) {
                                for (int dim1 = 0; dim1 < a; dim1++) {
                                    result.set(dim1, dim2*c*d*e + dim3*d*e + dim4*e + dim5, get(dim1, dim2, dim3, dim4, dim5));
                                }
                            }
                        }
                    }
                }
                break;

            //按模2展开
            case 2:
                result = new Matrix(b,c*d*e*a);
                for(int dim3 = 0; dim3 < c; dim3++) {
                    for(int dim4 = 0; dim4 < d; dim4++) {
                        for(int dim5 = 0; dim5 < e; dim5++) {
                            for (int dim1 = 0; dim1 < a; dim1++) {
                                for (int dim2 = 0; dim2 < b; dim2++) {
                                    result.set(dim2, dim3*d*e*a + dim4*e*a + dim5*a + dim1, get(dim1, dim2, dim3, dim4, dim5));
                                }
                            }
                        }
                    }
                }
                break;

            //按模3展开
            case 3:
                result = new Matrix(c,d*e*a*b);
                for(int dim4 = 0; dim4 < d; dim4++) {
                    for(int dim5 = 0; dim5 < e; dim5++) {
                        for (int dim1 = 0; dim1 < a; dim1++) {
                            for (int dim2 = 0; dim2 < b; dim2++) {
                                for (int dim3 = 0; dim3 < c; dim3++) {
                                    result.set(dim3, dim4*e*a*b + dim5*a*b + dim1*b + dim2, get(dim1, dim2, dim3, dim4, dim5));
                                }
                            }
                        }
                    }
                }
                break;

            //按模4展开
            case 4:
                result = new Matrix(d,e*a*b*c);
                for(int dim5 = 0; dim5 < e; dim5++) {
                    for (int dim1 = 0; dim1 < a; dim1++) {
                        for (int dim2 = 0; dim2 < b; dim2++) {
                            for (int dim3 = 0; dim3 < c; dim3++) {
                                for (int dim4 = 0; dim4 < d; dim4++) {
                                    result.set(dim4, dim5*a*b*c + dim1*b*c + dim2*c + dim3, get(dim1, dim2, dim3, dim4, dim5));
                                }
                            }
                        }
                    }
                }
                break;

            //按模5展开
            case 5:
                result = new Matrix(e,a*b*c*d);
                for (int dim1 = 0; dim1 < a; dim1++) {
                    for (int dim2 = 0; dim2 < b; dim2++) {
                        for (int dim3 = 0; dim3 < c; dim3++) {
                            for (int dim4 = 0; dim4 < d; dim4++) {
                                for(int dim5 = 0; dim5 < e; dim5++) {
                                    result.set(dim5, dim1*b*c*d + dim2*c*d + dim3*d + dim4, get(dim1, dim2, dim3, dim4, dim5));
                                }
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

    public Tensor5 combineWithTensor(final Tensor5 that, int dim) {

        Tensor5 result = null;
        int dim1 = 0;
        int dim2 = 0;
        int dim3 = 0;
        int dim4 = 0;
        int dim5 = 0;

        //按摸进行张量块的合并
        switch (dim) {
            //沿着第一模进行合并
            case 1:
                dim1 = this.getA() + that.getA();
                dim2 = this.getB();
                dim3 = this.getC();
                dim4 = this.getD();
                dim5 = this.getE();
                result = new Tensor5(dim1,dim2,dim3,dim4,dim5);

                for(int m = 0; m < dim5; m++)
                    for(int l = 0; l < dim4; l++)
                        for(int k = 0; k < dim3; k++)
                            for(int j = 0; j < dim2; j++)
                                for(int i = 0; i < dim1; i++) {
                                    if(i < this.getA()) {
                                        result.set(i,j,k,l,m,this.get(i, j, k, l, m));
                                    } else {
                                        result.set(i,j,k,l,m,that.get(i-this.getA(),j,k,l,m));
                                    }
                                }
                break;

            //沿着第二个模进行合并
            case 2:
                dim1 = this.getA();
                dim2 = this.getB() + that.getB();
                dim3 = this.getC();
                dim4 = this.getD();
                dim5 = this.getE();
                result = new Tensor5(dim1,dim2,dim3,dim4,dim5);

                for(int m = 0; m < dim5; m++)
                    for(int l = 0; l < dim4; l++)
                        for(int k = 0; k < dim3; k++)
                            for(int i = 0; i < dim1; i++)
                                for(int j = 0; j < dim2; j++) {
                                    if(j < this.getB()) {
                                        result.set(i,j,k,l,m,this.get(i, j, k, l, m));
                                    } else {
                                        result.set(i,j,k,l,m,that.get(i,j-this.getB(),k,l,m));
                                    }
                                }
                break;

            //沿着第三个模进行合并
            case 3:
                dim1 = this.getA();
                dim2 = this.getB();
                dim3 = this.getC() + that.getC();
                dim4 = this.getD();
                dim5 = this.getE();
                result = new Tensor5(dim1,dim2,dim3,dim4,dim5);

                for(int m = 0; m < dim5; m++)
                    for(int l = 0; l < dim4; l++)
                        for(int i = 0; i < dim1; i++)
                            for(int j = 0; j < dim2; j++)
                                for(int k = 0; k < dim3; k++) {
                                    if(k < this.getC()) {
                                        result.set(i,j,k,l,m,this.get(i, j, k, l, m));
                                    } else {
                                        result.set(i,j,k,l,m,that.get(i,j,k-this.getC(),l,m));
                                    }
                                }
                break;

            //沿着第四个模进行合并
            case 4:
                dim1 = this.getA();
                dim2 = this.getB();
                dim3 = this.getC();
                dim4 = this.getD() + that.getD();
                dim5 = this.getE();
                result = new Tensor5(dim1,dim2,dim3,dim4,dim5);

                for(int m = 0; m < dim5; m++)
                    for(int k = 0; k < dim3; k++)
                        for(int i = 0; i < dim1; i++)
                            for(int j = 0; j < dim2; j++)
                                for(int l = 0; l < dim4; l++) {
                                    if(l < this.getD()) {
                                        result.set(i,j,k,l,m,this.get(i, j, k, l, m));
                                    } else {
                                        result.set(i,j,k,l,m,that.get(i,j,k,l-this.getD(),m));
                                    }
                                }
                break;

            //沿着第五个模进行合并
            case 5:
                dim1 = this.getA();
                dim2 = this.getB();
                dim3 = this.getC();
                dim4 = this.getD();
                dim5 = this.getE() + that.getE();
                result = new Tensor5(dim1,dim2,dim3,dim4,dim5);

                for(int l = 0; l < dim4; l++)
                    for(int k = 0; k < dim3; k++)
                        for(int j = 0; j < dim2; j++)
                            for(int i = 0; i < dim1; i++)
                                for(int m = 0; m < dim5; m++) {
                                    if(m < this.getE()) {
                                        result.set(i,j,k,l,m,this.get(i, j, k, l, m));
                                    } else {
                                        result.set(i,j,k,l,m,that.get(i,j,k,l,m-this.getE()));
                                    }
                                }
                break;
            default:
                break;
        }

        return result;
    }

//    @Override
//    public String toString() {
//        String result = "";
//        for(int k = 0; k < z; k++) {
//            result += sliceToMatrix(3,k).toString() + "\n";
//        }
//        return result;
//    }

    public static Tensor5 emptyTensor5() {
        return new Tensor5(0,0,0,0,0);
    }

    public static Tensor5 random(int dim1, int dim2, int dim3, int dim4, int dim5) {
        Random random = new Random(47);
        Tensor5 result = new Tensor5(dim1,dim2,dim3,dim4,dim5);

        for(int m = 0; m < dim5; m++)
            for(int l = 0; l < dim4; l++)
                for(int k = 0; k < dim3; k++)
                    for(int i = 0; i < dim1; i++)
                        for(int j = 0; j < dim2; j++)
                            result.set(i,j,k,l,m,random.nextDouble());

        return result;
    }

    public static Tensor5 init_ten(int dim1, int dim2, int dim3, int dim4, int dim5, int start_count) {
        Tensor5 result = new Tensor5(dim1,dim2,dim3,dim4,dim5);

        for(int m = 0; m < dim5; m++)
            for(int l = 0; l < dim4; l++)
                for(int k = 0; k < dim3; k++)
                    for(int i = 0; i < dim1; i++)
                        for(int j = 0; j < dim2; j++)
                            result.set(i,j,k,l,m,start_count++);

        return result;

    }


    public static void main(String[] args) throws IOException {

        File file = new File("File1.txt");
        PrintWriter pt = new PrintWriter(file);

        pt.println("2*2*2*2*2张量1: ");

        Integer count = 1;
        Tensor5 one = init_ten(2,2,2,2,2,count);
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
        pt.println();
        pt.println("模五展开: ");
        pt.println(one.matricization(5));
        pt.println("---------------------------");
//        count += 3*4*5*6;
        count += 2*2*2*2*2;


        pt.println("2*2*2*2*2张量2: ");
        Tensor5 two = init_ten(2,2,2,2,2,count);
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
        pt.println();
        pt.println("模五展开: ");
        pt.println(two.matricization(5));
        pt.println("---------------------------");


        pt.println("张量按模一合并: ");
        pt.println("合并后的张量做展开： ");
        Tensor5 three = one.combineWithTensor(two, 1);
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
        pt.println();
        pt.println("模五展开: ");
        pt.println(three.matricization(5));
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
        pt.println();
        pt.println("模五展开: ");
        pt.println(three.matricization(5));
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
        pt.println();
        pt.println("模五展开: ");
        pt.println(three.matricization(5));
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
        pt.println();
        pt.println("模五展开: ");
        pt.println(three.matricization(5));
        pt.println("---------------------------");


        pt.println("张量按模五合并: ");
        pt.println("合并后的张量做展开： ");
        three = one.combineWithTensor(two, 5);
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
        pt.println();
        pt.println("模五展开: ");
        pt.println(three.matricization(5));
        pt.println("---------------------------");

        pt.close();
    }
}
