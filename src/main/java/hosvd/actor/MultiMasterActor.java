package hosvd.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import au.com.bytecode.opencsv.CSVWriter;
import hosvd.datatype.Matrix;
import hosvd.datatype.Tensor3;
import hosvd.datatype.Vec;
import hosvd.message.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by cxy on 15-10-22.
 */
public class MultiMasterActor extends UntypedActor{
    private int workerNumber = 0;
    private List<Integer> filterList = new ArrayList<>();
    private int firstProcessNumber = 0;
    private int outterOrthNumber = 0;
    private int secondProcessNumber = 0;
    private int iteration = 0;
    private List<Boolean> allConverged = new ArrayList<>();
    private List<ActorRef> actorList = new ArrayList<>();
    private List<String> actorPathList = new ArrayList<>();
    private ActorRef mainThread;
    private int lastProcessNumber = 0;
    private List<Matrix> matrixU = new ArrayList<>();
    private Matrix matrixV = Matrix.emptyMatrix();
    private int sweepCount = 0;
    private int maxSteps = 75;
    private int ranks = 0;


    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    //打开相关的资源，比如说打开文件描述符
    @Override
    public void preStart() throws Exception {

    }

    //关闭相关的资源，比如关闭文件描述符
    @Override
    public void postStop() throws Exception {

    }

    //将矩阵写入到文件中，文件名为filename
    private void writeMatrixToFile(Matrix matrix, String filename) throws IOException {
        File path = new File(filename+".csv");
//        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path)));
//'''        out.print(matrix);
//        out.close();
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
        log.info("Write matrix: " + filename + " OK!");
    }

    //将上一次计算出来的tol写入文件中
    //以及计算出来的展开矩阵的大小
    private void writeTolToFile(String filename, double tol) throws IOException {
        File path = new File(filename);
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path)));
        out.println(tol);
        out.println(matrixV.row);
        for(int i = 1; i < ranks; i++)
            out.println(matrixU.get(i).row);
        out.close();
    }

    //从文件中读入tol
    private double readTol(String filename) throws IOException {
        File path = new File(filename);
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
        double result = in.readDouble();
        return result;
    }

    @Override
    public void onReceive(Object message) throws Exception {

        /*******************************test***********************************/
        /**********************************************************************/
        /**********************************************************************/
        /**********************************************************************/
        //多阶同时分布式的情况
        //沿着模一切成两块，每块单独一个流程，每块单独一个actor,每个actor下面再包含多个子worker,用于分布式处理
        //最终将每个块的结果汇总返回在master中，master再开启一个actor进行汇总后的处理，包括合并、正交化处理等
        if (message instanceof MultiInitialization) {
            MultiInitialization initializationMulti = (MultiInitialization) message;
            workerNumber = initializationMulti.getWorkerNumber();

            //构建20*20*5的张量,并切成上下两块，对上面和下面的张量块各自做模一展开，并各自做正交化，按列追加的方式
            //按照按列追加的方式做正交化后，分别生成了U和V两个矩阵，然后将U1和U2转置并按列追加，再以按列追加的方式进行正交化，
            //记录下生成的V，看V是否与原来的HOSVD分解方式相同??????????????????????????????????????????????????????
            Tensor3 tensor = Tensor3.setTensor3(initializationMulti.getDim1(), initializationMulti.getDim2(), initializationMulti.getDim3(), "./dataset", false);
            //将张量切块成上下两块
            List<Tensor3> blockList = tensor.sliceEqually(2, 1, 1);

            //将张量切块成前后两块
//            List<Tensor3> blockList = tensor.sliceEqually(1, 1, 2);

            List<Matrix> UDataList = new ArrayList<Matrix>();
            List<Matrix> VDataList = new ArrayList<Matrix>();
            List<Double> tol = new ArrayList<Double>();

            List<Matrix> UList = new ArrayList<Matrix>();
            List<Matrix> VList = new ArrayList<Matrix>();
            List<Matrix> SIGMA_List  = new ArrayList<Matrix>();


//            Matrix U0 = tensor.matricization(3);
//            writeMatrixToFile(U0, "mode3");
//            Matrix V0 = Matrix.eye(U0.col);
//            for(int i = 0; i < 75; i++) {
//                boolean con = innerOrthForDim1(U0, V0, Math.pow(tensor.norm(), 2) * 1e-15);
//                if(con) {
//                    log.info("Converged : Matrix" + (i+1));
//                    break;
//                }
//            }
//            writeMatrixToFile(U0.normalizeU().sliceByCol(0,U0.row-1), "U3");









            //各个切块按照模一展开
            for (Tensor3 t : blockList) {
                UDataList.add(t.matricization(1));
                tol.add(Math.pow(t.norm(), 2) * 1e-15);
            }
            for(Matrix m : UDataList)
                VDataList.add(Matrix.eye(m.col));

            int maxSteps = 75;

            for(int i = 0; i < UDataList.size(); i++) {
                for(int j = 0; j < maxSteps; j++) {
                    boolean con = innerOrthForDim1(UDataList.get(i), VDataList.get(i), tol.get(i));
                    if(con) {
                        log.info("Converged : Matrix" + (i+1));
                        break;
                    }
                }
            }

            //按行追加的情形
            //求出每个分块张量展开后的U，SIGMA，V
            for(int i = 0; i < UDataList.size(); i++) {
                Matrix SIGMA = Matrix.eye(UDataList.get(i).col);
//                UList.add(UDataList.get(i).normalizeU(SIGMA).sliceByCol(0, UDataList.get(i).row-1));
                UList.add(UDataList.get(i).normalizeU(SIGMA));
//                SIGMA_List.add(SIGMA.sliceByCol(0,UDataList.get(i).row-1).sliceByRow(0,UDataList.get(i).row-1));
                SIGMA_List.add(SIGMA);
//                VList.add(VDataList.get(i).normalizeV(UDataList.get(i)).sliceByCol(0,UDataList.get(i).row-1));
                VList.add(VDataList.get(i).normalizeV(UDataList.get(i)));
            }

            //按列追加的情形
//            for(int i = 0; i < UDataList.size(); i++) {
//                Matrix SIGMA = Matrix.eye(UDataList.get(i).col);
//                UList.add(UDataList.get(i));
////                SIGMA_List.add(SIGMA);
//                VList.add(VDataList.get(i));
//            }

//            Matrix temp = UDataList.get(0);
//            Matrix SIGMA = Matrix.eye(temp.col);
//            Matrix U = UDataList.get(0).normalizeU(SIGMA).sliceByCol(0, temp.row - 1);
//            Matrix V = VDataList.get(0).normalizeV(UDataList.get(0)).sliceByCol(0,temp.row-1);
//            SIGMA = SIGMA.sliceByCol(0,9).sliceByRow(0,9);
//            writeMatrixToFile(U,"U");
//            writeMatrixToFile(SIGMA,"Sigma");
//            writeMatrixToFile(V,"V");

//            writeMatrixToFile(U.dot(SIGMA).dot(V.trans()),"Result");
//            writeMatrixToFile(VList.get(1).dot(SIGMA_List.get(1)).dot(UList.get(1).trans()),"Result");

//            List<Double> norm = new ArrayList<>();
//            for(int i = 0; i < temp.col; i++) {
//                norm.add(temp.getCol(i).norm());
//            }
//
//            Collections.sort(norm);
//            Collections.reverse(norm);
//            log.info(norm.toString());

//            for(int i = 0; i < norm.size(); i++)
//                sig.set(i,i,norm.get(i));

//            writeMatrixToFile(blockList.get(0).matricization(1), "raw");
//            writeMatrixToFile(sig, "sig");
//            writeMatrixToFile(UDataList.get(0),"rawM");
//            writeMatrixToFile(UDataList.get(0).normalizeU(sig).dot(sig).dot(VList.get(0).normalizeV(UDataList.get(0)).trans()),"rawMatrix");




            //更新新的tol
            double toleration = 0d;
            for(Double d : tol)
                toleration += d;


            //此时开始探索上下两块之间的关系，上下两块各自做了按列追加的正交化操作
            //探索上面的U和下面的U如何进行合并，生成整体张量块的U
            /*******************************方法1***********************************/
            //方法1，直接将上面的U和下面的U进行上下拼凑,然后再做一次大循环
            //验证结果：结果错误，此方法不行

//            Matrix result = UDataList.get(0);
//            for(int i = 1; i < UDataList.size(); i++) {
//                result = result.addByRow(UDataList.get(i));
//            }
//            log.info(result.toString());
//
//            for(int i = 0; i < maxSteps; i++) {
//                boolean con = innerOrth(result,toleration);
//                if(con) {
//                    log.info("Converged : MatrixU_UP combine MatrixU_DOWN");
//                    break;
//                }
//            }
//            writeMatrixToFile(result.normalizeU().sliceByCol(0, result.row-1),"tempMatrixCombine");
//            writeMatrixToFile(result,"tempMatrixCombine_raw");
            /*******************************方法1***********************************/


            /*******************************方法2***********************************/
            //方法2，将两个矩阵进行转置，然后将转置后的矩阵进行按列追加，计算列正交，并记录旋转矩阵
            //即v1*v2*v3***vn就是整理张量的U矩阵
            //验证结果：结果错误，此方法不行

            //matrixV用与记录旋转矩阵
//            Matrix matrixV = Matrix.eye(UDataList.get(0).row + UDataList.get(1).row);
//            Matrix result = UDataList.get(0);
//            for(int i = 1; i < UDataList.size(); i++) {
//                result = result.addByRow(UDataList.get(i));
//            }
//            result = result.trans();
//
//            for(int i = 0; i < maxSteps; i++) {
//                boolean con = innerOrthForDim1(result,matrixV,toleration);
//                if(con) {
//                    log.info("Converged : MatrixU_UP combine MatrixU_DOWN");
//                    break;
//                }
//            }
////            log.info(matrixV.toString());
////            log.info(result.toString());
//
//            writeMatrixToFile(matrixV.normalizeV(result),"tempMatrixCombine");
            /*******************************方法2***********************************/


            /*******************************方法3***********************************/
            //方法3，利用两个矩阵的V1和V2，U1乘以V2，U2乘以V1，然后将两个相乘后的矩阵按行拼接起来
            //再进行列之间的正交化
            //验证结果：结果错误，此方法不行

//            Matrix U1 = UDataList.get(0);
//            Matrix U2 = UDataList.get(1);
//            Matrix V1 = VList.get(0);
//            Matrix V2 = VList.get(1);
//            U1 = U1.dot(V2);
//            U2 = U2.dot(V1);
//
//            Matrix result = U1.addByRow(U2);
//
//            for(int i = 0; i < maxSteps; i++) {
//                boolean con = innerOrth(result,toleration);
//                if(con) {
//                    log.info("Converged : MatrixU_UP combine MatrixU_DOWN");
//                    break;
//                }
//            }
//            writeMatrixToFile(result.normalizeU().sliceByCol(0, result.row-1),"tempMatrixCombine");
//            writeMatrixToFile(result,"tempMatrixCombine_raw");
            /*******************************方法3***********************************/


            /*******************************方法4***********************************/
            //按列追加方式后再进行按行追加情形的处理
            //方法3，已经计算出了按列追加方式的U和V，U可以理解为按行追加方式的V，U按行追加方式的V
            //则尝试将U变成V，同按列追加方式求U的方法一样，取U的norm值最大的10个拼凑成V
            //验证结果：结果成功，此方法正确

            //A 100*200
//            Matrix result = VList.get(0).dot(SIGMA_List.get(0));
//            for(int i = 1; i < VList.size(); i++) {
//                result = result.addByCol(VList.get(i).dot(SIGMA_List.get(i)));
//            }
////            result = result.trans();
//
//            Matrix V1 = UList.get(0);
//            Matrix V2 = UList.get(1);
//
//            //构建V矩阵，此V即为按行追加方式转置后的V
//            Matrix matrixV = Matrix.eye(V1.row+V2.row);
//            for(int i = 0; i < matrixV.row; i++) {
//                for(int j = 0; j < V1.col; j++) {
//                    if(i < V1.row) {
//                        matrixV.set(i,j,V1.get(i,j));
//                    } else {
//                        matrixV.set(i,j+V1.col,V2.get(i-V1.row,j));
//                    }
//                }
//            }
//
//            for(int i = 0; i < maxSteps; i++) {
//                boolean con = innerOrthForDim1(result,matrixV,toleration);
//                if(con) {
//                    log.info("Converged : MatrixU_UP combine MatrixU_DOWN");
//                    break;
//                }
//            }
//            Matrix sigma = Matrix.eye(matrixV.row);
//            writeMatrixToFile(result.normalizeU(sigma),"VUP");
//            writeMatrixToFile(sigma,"SIGMA1");
//            writeMatrixToFile(matrixV.normalizeV(result), "tempMatrixCombine");
            /*******************************方法4***********************************/



            /*******************************方法5***********************************/
            //按列追加方式后再进行按行追加情形的处理
            //方法3，已经计算出了按列追加方式的U和V，U可以理解为按行追加方式的V，U按行追加方式的V
            //则尝试将U变成V，同按列追加方式求U的方法一样，取U的norm值最大的10个拼凑成V
            //验证结果：结果成功，此方法正确

            //A 100*200
            Matrix result = VList.get(0).dot(SIGMA_List.get(0));
            for(int i = 1; i < VList.size(); i++) {
                result = result.addByCol(VList.get(i).dot(SIGMA_List.get(i)));
            }
//            result = result.trans();

            Matrix V1 = UList.get(0);
            Matrix V2 = UList.get(1);

            //构建V矩阵，此V即为按行追加方式转置后的V
            Matrix matrixV = new Matrix(V1.row+V2.row, V1.col+V2.col);
            for(int i = 0; i < matrixV.row; i++) {
                for(int j = 0; j < V1.col; j++) {
                    if(i < V1.row) {
                        matrixV.set(i,j,V1.get(i,j));
                    } else {
                        matrixV.set(i,j+V1.col,V2.get(i-V1.row,j));
                    }
                }
            }

            for(int i = 0; i < maxSteps; i++) {
                boolean con = innerOrthForDim1(result,matrixV,toleration);
                if(con) {
                    log.info("Converged : MatrixU_UP combine MatrixU_DOWN");
                    break;
                }
            }
//            Matrix sigma = Matrix.eye(matrixV.row);
            Matrix sigma = Matrix.eye(matrixV.col);
            writeMatrixToFile(result.normalizeU(sigma),"VUP");
            writeMatrixToFile(sigma,"SIGMA1");
            writeMatrixToFile(matrixV.normalizeV(result), "tempMatrixCombine");
            /*******************************方法5***********************************/




            /*******************************按列追加方式的处理情况***********************************/
//            Matrix result = UList.get(0);
//            for(int i = 1; i < UList.size(); i++) {
//                result = result.addByCol(UList.get(i));
//            }
////            result = result.trans();
//
//            Matrix V1 = VList.get(0);
//            Matrix V2 = VList.get(1);
//
//            //构建V矩阵，此V即为按列追加方式转置后的V
//            Matrix matrixV = Matrix.eye(V1.row+V2.row);
//            for(int i = 0; i < matrixV.row; i++) {
//
//                //当记录上面的这个矩阵的时候
//                if(i < V1.row) {
//                    for(int j = 0; j < V1.col; j++)
//                        matrixV.set(i,j,V1.get(i,j));
//                } else {
//                    for(int j = 0; j < V2.col; j++)
//                        matrixV.set(i,j+V1.col,V2.get(i-V1.row,j));
//                }
//            }
//
//            for(int i = 0; i < maxSteps; i++) {
//                boolean con = innerOrthForDim1(result,matrixV,toleration);
//                if(con) {
//                    log.info("Converged : MatrixU_UP combine MatrixU_DOWN");
//                    break;
//                }
//            }
//            Matrix sig = new Matrix(result.col,result.col);
//            writeMatrixToFile(result.normalizeU(sig).sliceByCol(0,result.row-1),"tempMatrixCombine");
//            writeMatrixToFile(sig,"SIGMA");
//            writeMatrixToFile(matrixV.normalizeV(result).sliceByCol(0,result.row-1),"VV");
            /*******************************按列追加方式的处理情况***********************************/




            /*******************************按列穿插方式的处理情况,最终结果V是相同的***********************************/
//            Matrix result = UList.get(0);
//            for(int i = 1; i < UList.size(); i++) {
//                result = result.addByCol(UList.get(i));
//            }
////            result = result.trans();
//
//            Matrix V1 = VList.get(0);
//            Matrix V2 = VList.get(1);
//
//            //构建初等矩阵，用于矩阵块的还原
//            Matrix M = new Matrix(V1.row+V2.row, V1.row+V2.row);
//            //根据原始矩阵与现在U矩阵构建初等矩阵M,最终结果的V为: M.T*V，为原始矩阵SVD分解之后的V
//            //根据dim2的长度构建,构建20次，每次将3和2的构建到M中, UList(0).dim2 = UList(1).dim2 = 20
//            for(int i = 0; i < 20; i++) {
//                //对于3 ,UList(0).dim3 = 3
//                    // 行：UList(0).dim3*i
//                    // 列：UList(0).dim3*i + UList(1).dim3*i
//                //对于2 ,UList(1).dim3 = 2
//                    // 行：UList(0).dim3*UList(0).dim2 + UList(1).dim3*i
//                    // 列：UList(0).dim3*(i+1) + UList(1).dim3*(i)
//                int startX1 = 3*i;
//                int startY1 = 3*i + 2*i;
//                for(int j = 0; j < 3; j++) {
//                    M.set(startX1+j,startY1+j,1);
//                }
//
//                int startX2 = 3*20 + 2*i;
//                int startY2 = 3*(i+1) + 2*i;
//                for(int j = 0; j < 2; j++) {
//                    M.set(startX2+j,startY2+j,1);
//                }
//            }
//
//            writeMatrixToFile(M,"MMMM");
//
//            //构建V矩阵，此V即为按列追加方式转置后的V
//            Matrix matrixV = Matrix.eye(V1.row + V2.row);
//            for(int i = 0; i < matrixV.row; i++) {
//
//                //当记录上面的这个矩阵的时候
//                if(i < V1.row) {
//                    for(int j = 0; j < V1.col; j++)
//                        matrixV.set(i,j,V1.get(i,j));
//                } else {
//                    for(int j = 0; j < V2.col; j++)
//                        matrixV.set(i,j+V1.col,V2.get(i-V1.row,j));
//                }
//            }
//
//            for(int i = 0; i < maxSteps; i++) {
//                boolean con = innerOrthForDim1(result,matrixV,toleration);
//                if(con) {
//                    log.info("Converged : MatrixU_UP combine MatrixU_DOWN");
//                    break;
//                }
//            }
//            Matrix sig = new Matrix(result.col,result.col);
//            Matrix U = result.normalizeU(sig).sliceByCol(0, result.row - 1);
//            Matrix V = matrixV.normalizeV(result).sliceByCol(0, result.row - 1);
//            writeMatrixToFile(U,"tempMatrixCombine");
//            writeMatrixToFile(sig,"SIGMA");
//            writeMatrixToFile(V,"VV");
//            writeMatrixToFile(M.trans().dot(V),"VVVV");
//            writeMatrixToFile(U.dot(sig.sliceByCol(0,19).sliceByRow(0,19)).dot(M.trans().dot(V).trans()),"ABS");
            /*******************************按列穿插方式的处理情况***********************************/




            mainThread.tell("Success",mainThread);
        }

        else if(message instanceof HasDone) {
            log.info("I received HasDone message");
            mainThread = getSender();
        }

    }
        /**********************************************************************/
        /**********************************************************************/
        /**********************************************************************/


    //其余模块内正交处理,即除开模以展开的
    public boolean innerOrth(Matrix block, double tol) {
        Matrix matrix = block;
        boolean converged = true;

        for(int i = 0; i < block.col -1; i++) {
            for (int j = i + 1; j < block.col; j++) {
                Vec di = block.getCol(i);
                Vec dj = block.getCol(j);
                double dii = di.dot(di);
                double dij = di.dot(dj);
                double djj = dj.dot(dj);

                //如果存在两列之间的正交化结果大于误差值，则判定为按照该模展开的矩阵未收敛
                if (Math.abs(dij) > tol) {
                    converged = false;

                    double tao = (djj - dii) / (2 * dij);
                    double t = Math.signum(tao) / ( Math.abs(tao) + Math.sqrt(Math.pow(tao, 2) + 1) );
                    double c = 1.0 / Math.sqrt(Math.pow(t, 2) + 1);
                    double s = t * c;

                    //update data block
                    //乘以旋转矩阵
                    for (int k = 0; k < block.row; k++) {
                        double res1 = block.get(k, i) * c - block.get(k, j) * s;
                        double res2 = block.get(k, i) * s + block.get(k, j) * c;
                        matrix.set(k, i, res1);
                        matrix.set(k, j, res2);
                    }
                }
            }
        }
        return converged;
    }

    //按模一进行块内正交时特殊处理
    public boolean innerOrthForDim1(Matrix block, Matrix v, double tol) {
        List<Matrix> result = new ArrayList<Matrix>(2);
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

}
