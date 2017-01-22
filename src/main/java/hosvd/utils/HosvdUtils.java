package hosvd.utils;

import akka.event.LoggingAdapter;
import au.com.bytecode.opencsv.CSVWriter;
import hosvd.datatype.Matrix;
import hosvd.datatype.Vec;
import hosvd.multimessage.IntermediateResult;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by cxy on 15-12-24.
 */
public class HosvdUtils {

//    private LoggingAdapter log;
    private final Logger log;
    private int Ranks;
    private int hierarchy;
    private String address;

    public HosvdUtils(Logger log, String address, int ranks, int hierarchy) {
        this.log = log;
        this.Ranks = ranks;
        this.hierarchy = hierarchy;
        this.address = address;
    }

    public HosvdUtils(Logger log) {
        this.log = log;
    }

    public void doInnerOrth(List<Matrix> ulist, List<Matrix> vlist, double tol) {//tol是累计误差
        int maxSteps = 1000;
        for (int i = 0; i < ulist.size(); i++) {
//            log.info("In doInnerOrth of U " + (i+1));
            for (int j = 0; j < maxSteps; j++) {
//                log.info("In Step: " + j);
                boolean con = innerOrth(ulist.get(i), vlist.get(i), tol);
                if (con) {
                    log.info(address + " : Converged Mode: " + (i+1) + "! With steps of" + j);
                    break;
                }
            }
        }
    }

    public void doInnerOrth(Matrix U, Matrix V, double tol) {
        int maxSteps = 1000;

        for (int i = 0; i < maxSteps; i++) {
//            log.info("In Step: " + i);
            boolean con = innerOrth(U, V, tol);
            if (con) {
                log.info(address + " : Converged With steps of: " + i);
                break;
            }
        }
    }

    private boolean innerOrth(Matrix block, Matrix v, double tol) {

        Matrix uMatrix = block;
        Matrix vMatrix = v;
        boolean converged = true;       //是否收敛

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
        //return Matrix.innerOrth(block, v, tol);
    }


    public void doOutterOrth(Matrix U1, Matrix U2, Matrix V1, Matrix V2, double tol) {
        int maxSteps = 1000;

        for (int i = 0; i < maxSteps; i++) {
//            log.info("In Step: " + i);
            boolean con = outterOrth(U1, U2, V1, V2, tol);
            if (con) {
//                log.info("Converged With steps of: " + i);
                break;
            }
        }
    }

    private boolean outterOrth(Matrix block1, Matrix block2, Matrix v1, Matrix v2, double tol) {
        boolean converged = true;

        //一次round-robin循环，一次sweep来正交化矩阵
        for(int i = 0; i < block1.col; i++) {
            for(int j = 0; j < block2.col; j++) {
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
                    for(int k = 0; k < block1.row; k++) {
                        double res1 = block1.get(k, i) * c - block2.get(k, j) * s;
                        double res2 = block1.get(k, i) * s + block2.get(k, j) * c;
                        block1.set(k,i,res1);
                        block2.set(k,j,res2);
                    }

                    for(int k = 0; k < v1.row; k++) {
                        double res3 = v1.get(k, i) * c - v2.get(k, j) * s;
                        double res4 = v1.get(k, i) * s + v2.get(k, j) * c;
                        v1.set(k,i,res3);
                        v2.set(k,j,res4);
                    }
                }
            }
        }
        return converged;
    }


    public void doIOrth(Matrix block1, Matrix block2, Matrix v1, Matrix v2,
                        List<Boolean> converged,int mode, double tol) {
        int col1 = block1.col;
        int col2 = block2.col;
        int col = col1 + col2;
        int urow = block1.row;
        int vrow = v1.row;


        short flag = 0;

        //一次round-robin循环，一次sweep来正交化矩阵
        for (int i = 0; i < col - 1; i++) {
            for (int j = i + 1; j < col; j++) {
                Vec di = null;
                Vec dj = null;

                //情况1，如果j < col1,则列向量都从block1中取出
                if (j < col1) {
                    di = block1.getCol(i);
                    dj = block1.getCol(j);
                    flag = 1;
                }
                //情况2，如果i >= col1,则列向量都从block2中取出
                else if (i >= col1) {
                    di = block2.getCol(i - col1);
                    dj = block2.getCol(j - col1);
                    flag = 2;
                }
                //情况3，如果i < col1 && j >= col1，则di从block1中取出，dj从block2中取出
                else {
                    di = block1.getCol(i);
                    dj = block2.getCol(j - col1);
                    flag = 3;
                }

                double dii = di.dot(di);
                double dij = di.dot(dj);
                double djj = dj.dot(dj);

                //如果存在两列之间的正交化结果大于误差值，则判定为按照该模展开的矩阵未收敛
                if (Math.abs(dij) > tol) {
                    converged.set(mode, false);

                    double tao = (djj - dii) / (2 * dij);
                    double t = Math.signum(tao) / (Math.abs(tao) + Math.sqrt(Math.pow(tao, 2) + 1));
                    double c = 1.0 / Math.sqrt(Math.pow(t, 2) + 1);
                    double s = t * c;

                    //update data block 乘以旋转矩阵
                    //向量都从block1中取出
                    if (flag == 1) {
                        for (int k = 0; k < urow; k++) {
                            double res1 = block1.get(k, i) * c - block1.get(k, j) * s;
                            double res2 = block1.get(k, i) * s + block1.get(k, j) * c;
                            block1.set(k, i, res1);
                            block1.set(k, j, res2);
                        }

                        for (int k = 0; k < vrow; k++) {
                            double res3 = v1.get(k, i) * c - v1.get(k, j) * s;
                            double res4 = v1.get(k, i) * s + v1.get(k, j) * c;
                            v1.set(k, i, res3);
                            v1.set(k, j, res4);
                        }
                    }
                    //向量都从block2中取出
                    else if (flag == 2) {
                        for (int k = 0; k < urow; k++) {
                            double res1 = block2.get(k, i - col1) * c - block2.get(k, j - col1) * s;
                            double res2 = block2.get(k, i - col1) * s + block2.get(k, j - col1) * c;
                            block2.set(k, i - col1, res1);
                            block2.set(k, j - col1, res2);
                        }

                        for (int k = 0; k < vrow; k++) {
                            double res3 = v2.get(k, i - col1) * c - v2.get(k, j - col1) * s;
                            double res4 = v2.get(k, i - col1) * s + v2.get(k, j - col1) * c;
                            v2.set(k, i - col1, res3);
                            v2.set(k, j - col1, res4);
                        }
                    }

                    //第三种情况
                    else {
                        for (int k = 0; k < urow; k++) {
                            double res1 = block1.get(k, i) * c - block2.get(k, j - col1) * s;
                            double res2 = block1.get(k, i) * s + block2.get(k, j - col1) * c;
                            block1.set(k, i, res1);
                            block2.set(k, j - col1, res2);
                        }
                        for (int k = 0; k < vrow; k++) {
                            double res3 = v1.get(k, i) * c - v2.get(k, j - col1) * s;
                            double res4 = v1.get(k, i) * s + v2.get(k, j - col1) * c;
                            v1.set(k, i, res3);
                            v2.set(k, j - col1, res4);
                        }
                    }
                }
            }
        }
    }

    public void doOOrth(Matrix block1, Matrix block2, Matrix v1, Matrix v2,
                        List<Boolean> converged,int mode, double tol) {

        //一次round-robin循环，一次sweep来正交化矩阵
        for(int i = 0; i < block1.col; i++) {
            for(int j = 0; j < block2.col; j++) {
                Vec di = block1.getCol(i);
                Vec dj = block2.getCol(j);
                double dii = di.dot(di);
                double dij = di.dot(dj);
                double djj = dj.dot(dj);

                //如果存在两列之间的正交化结果大于误差值，则判定为按照该模展开的矩阵未收敛
                if (Math.abs(dij) > tol) {
                    converged.set(mode,false);

                    double tao = (djj - dii) / (2 * dij);
                    double t = Math.signum(tao) / (Math.abs(tao) + Math.sqrt(Math.pow(tao, 2) + 1));
                    double c = 1.0 / Math.sqrt(Math.pow(t, 2) + 1);
                    double s = t * c;

                    //update data block
                    //乘以旋转矩阵
                    for(int k = 0; k < block1.row; k++) {
                        double res1 = block1.get(k, i) * c - block2.get(k, j) * s;
                        double res2 = block1.get(k, i) * s + block2.get(k, j) * c;
                        block1.set(k,i,res1);
                        block2.set(k,j,res2);
                    }

                    for(int k = 0; k < v1.row; k++) {
                        double res3 = v1.get(k, i) * c - v2.get(k, j) * s;
                        double res4 = v1.get(k, i) * s + v2.get(k, j) * c;
                        v1.set(k,i,res3);
                        v2.set(k,j,res4);
                    }
                }
            }
        }
    }

    static class ProcessResult {
        //代表是第几个处理结果
        //1代表按行追加，2代表按列追加，3代表按列穿插
        private int which;
        //interval表示每个子张量展开矩阵的按列穿插单位
        private int pieces;

        public ProcessResult(int which, int pieces) {
            this.which = which;
            this.pieces = pieces;
        }

        public int getWhich() {
            return which;
        }

        public int getPieces() {
            return pieces;
        }
    }

    private ProcessResult whichProcess(int unfold, int incremental, IntermediateResult msgList) {
        ProcessResult result = null;

        if (unfold == incremental) {
            //按行追加的情况
            result = new ProcessResult(1,0);
        }

        else if ( ((unfold+1) % Ranks) == incremental) {
            //按列追加的情况
            result = new ProcessResult(2,0);
        }

        else {
            //按列穿插的情况
            int pieces = 1;
            for(int i = (unfold+1)%Ranks; i != incremental; i = (i+1)%Ranks) {
                //对于每个分块，它的拼接单位可能是不同的
                pieces *= msgList.getDim()[i];
            }
            log.info(address + " : pieces: " + pieces);
//            pieces = msgList.getVList().get(unfold).col / pieces;
            result = new ProcessResult(3,pieces);
        }

        return result;
    }


    /*****************************************************************************************************/
    /***********************************用于矩阵拼接的按行追加*********************************************
     /*****************************************************************************************************/
    private void doAddByRow(IntermediateResult[] msgList, int unfold, List<List<Matrix>> result) {
        Matrix U;
        Matrix V;
        double[] SIGMA;
        double tol = 0;


        Matrix V_SIG = msgList[0].getUList().get(unfold);
        U = msgList[0].getVList().get(unfold);
        SIGMA = new double[V_SIG.row];
        V = V_SIG.normalizeU(SIGMA);
        U = U.dot(SIGMA);
        tol = msgList[0].getTol();

//        if(hierarchy == 0) {
//            log.info("I am in doAddByRow1!");
//            log.info("doAddByRow: U" + U.row + " " + U.col);
//            log.info("doAddByRow: V" + V.row + " " + V.col);
//        }

        for(int i = 1; i < msgList.length; i++) {
            V_SIG = msgList[i].getUList().get(unfold);
            Matrix tempU = msgList[i].getVList().get(unfold);
            SIGMA = new double[V_SIG.row];
            V = V.addByDig(V_SIG.normalizeU(SIGMA));
            U = U.addByCol(tempU.dot(SIGMA));
            tol += msgList[i].getTol();
        }

        log.info(address + " : In AddByRow : V row " + V.row + " ,V col " + V.col + "; U row " + U.row + ",U col " + U.col);


//        if(hierarchy == 0) {
//            log.info("I am in doAddByRow2!");
//            log.info("doAddByRow: U" + U.row + " " + U.col);
//            log.info("doAddByRow: V" + V.row + " " + V.col);
//        }

        doInnerOrth(U, V, tol);

        log.info(address + " : AddByRow is finished!");

//        if(hierarchy == 0)
//            log.info("I am in doAddByRow3!");


        //按行追加的情况还需要将U、SIGMA、V再次还原
        SIGMA = new double[U.col];
        Matrix tempV = U.normalizeU(SIGMA);
//        SIGMA = SIGMA.sliceByCol(0,U.row-1);
//        log.info("tempv: " + tempV.row + " " + tempV.col);
        U = V.normalizeV(U);
        V = tempV;

//        log.info("doAddByRow: U" + U.row + " " + U.col);
//        log.info("doAddByRow: V" + V.row + " " + V.col);

        //转置后的U应该为U*SIGMA
        result.get(0).add(U.dot(SIGMA));
        result.get(1).add(V);


    }
    /*****************************************************************************************************/
    /***********************************doAddByRow Finished**********************************************/
    /*****************************************************************************************************/


    /*****************************************************************************************************/
    /***********************************用于矩阵拼接的按列追加*********************************************
     /*****************************************************************************************************/
    private void doAddByCol(IntermediateResult[] msgList, int unfold, List<List<Matrix>> result) {
        Matrix U;
        Matrix V;
        double tol = 0;

//        if(hierarchy == 0)
//            log.info("I am in doAddByCol!");

        //构建矩阵U，将子U直接合并成一个大的U
        U = msgList[0].getUList().get(unfold);
        V = msgList[0].getVList().get(unfold);
        tol = msgList[0].getTol();


        for(int i = 1; i < msgList.length; i++) {
            Matrix tempU = msgList[i].getUList().get(unfold);
            Matrix tempV = msgList[i].getVList().get(unfold);
            U = U.addByCol(tempU);
            V = V.addByDig(tempV);
            tol += msgList[i].getTol();
        }

        log.info(address + " : In AddByCol : V row " + V.row + " ,V col " + V.col + "; U row " + U.row + ",U col " + U.col);

        doInnerOrth(U,V,tol);

        log.info(address + " : AddByCol is finished!!!");

        //需要将处理的结果进行排序、裁切
        V = V.normalizeV(U).sliceByCol(0, U.row - 1);
        U = U.normalizeV(U).sliceByCol(0,U.row-1);

//        log.info("doAddByCol: U" + U.row + " " + U.col);
//        log.info("doAddByCol: V" + V.row + " " + V.col);

        result.get(0).add(U);
        result.get(1).add(V);
    }
    /*****************************************************************************************************/
    /***********************************doAddByCol Finished**********************************************/
    /*****************************************************************************************************/


    /*****************************************************************************************************/
    /***********************************用于矩阵拼接的按列穿插*********************************************
     /*****************************************************************************************************/
    private void doInsertByCol(IntermediateResult[] msgList, int unfold, int pieces, List<List<Matrix>> result) {
        Matrix U;
        Matrix V;
        double tol = 0;

//        if(hierarchy == 0)
//            log.info("I am in doInsertByCol!");


        //V矩阵的重构过程
        List<List<Matrix>> sliceV = new ArrayList<List<Matrix>>(msgList.length);
        for(int i = 0; i < msgList.length; i++) {
            List<Matrix> temp = msgList[i].getVList().get(unfold).sliceByRowEqually(pieces);
            sliceV.add(temp);
        }

        V = sliceV.get(0).get(0);
        for(int i = 1; i < msgList.length; i++) {
            V = V.addByDig(sliceV.get(i).get(0));
        }

        for(int i = 1; i < pieces; i++) {
            Matrix tempV = sliceV.get(0).get(i);
            for(int j = 1; j < msgList.length; j++) {
                tempV = tempV.addByDig(sliceV.get(j).get(i));
            }
            V = V.addByRow(tempV);
        }


        //构建矩阵U，将子U直接合并成一个大的U
        U = msgList[0].getUList().get(unfold);
        tol = msgList[0].getTol();

        for(int i = 1; i < msgList.length; i++) {
            U = U.addByCol(msgList[i].getUList().get(unfold));
            tol += msgList[i].getTol();
        }

        log.info("In InsertByCol : V row " + V.row + " ,V col " + V.col + "; U row " + U.row + ",U col " + U.col);

        doInnerOrth(U,V,tol);

        log.info("Insert By Col is finished!!!");

        //需要将处理的结果进行排序、裁切
        V = V.normalizeV(U).sliceByCol(0, U.row - 1);
        U = U.normalizeV(U).sliceByCol(0,U.row-1);

//        log.info("doInsertByCol: U " + U.row + " " + U.col);
//        log.info("doInsertByCol: V " + V.row + " " + V.col);


        result.get(0).add(U);
        result.get(1).add(V);

    }
    /*****************************************************************************************************/
    /***********************************doInsertByCol Finished**********************************************/
    /*****************************************************************************************************/


    /**
     * 用于矩阵的合并、单机正交化处理
     * @param msgList
     * @return
     */
    public List<List<Matrix>> CombineMatrix(IntermediateResult[] msgList) {
        //内部存放的是各自的UList和VList
        List<List<Matrix>> result = new ArrayList<List<Matrix>>(2);
        //初始化result数组
        result.add(new ArrayList<Matrix>());
        result.add(new ArrayList<Matrix>());

        ProcessResult WhichProcess = null;

        //对沿着各个阶展开的矩阵进行拼接
        for(int unfold = 0; unfold < Ranks; unfold++) {
            //判断在第hierarchy层的order展开矩阵的合并是属于哪一种情况
//            log.info(Arrays.toString(msgList[0].getDim()));
            WhichProcess = whichProcess(unfold,hierarchy,msgList[0]);

            switch (WhichProcess.getWhich()) {
                case 1:
                    //按行追加
                    doAddByRow(msgList,unfold,result);
                    break;
                case 2:
                    //按列追加
                    doAddByCol(msgList,unfold,result);
                    break;
                case 3:
                    //按列穿插
//                    doInsertByCol(msgList,unfold,WhichProcess.getPieces(),result);
                    doAddByCol(msgList,unfold,result);
                    break;
                default:
                    break;
            }
        }

//        System.gc();
        //将合并的结果返回
        return result;
    }


    /*****************************************************************************************************/
    /***********************************用于矩阵拼接的按行追加*********************************************
     /*****************************************************************************************************/
    private void doAddByRow(IntermediateResult[] msgList, int unfold, int blocks, List<List<List<Matrix>>> result) {
        Matrix U;
        Matrix V;
        double[] SIGMA;
        double tol = 0;


        Matrix V_SIG = msgList[0].getUList().get(unfold);
        U = msgList[0].getVList().get(unfold);
        SIGMA = new double[V_SIG.row];
        V = V_SIG.normalizeU(SIGMA);
        U = U.dot(SIGMA);
        tol = msgList[0].getTol();

        for(int i = 1; i < msgList.length; i++) {
            V_SIG = msgList[i].getUList().get(unfold);
            Matrix tempU = msgList[i].getVList().get(unfold);
            SIGMA = new double[V_SIG.row];
            V = V.addByDig(V_SIG.normalizeU(SIGMA));
            U = U.addByCol(tempU.dot(SIGMA));
            tol += msgList[i].getTol();
        }

//        log.info("tol: " + tol);

        //此时已经得到了拼接后的U*SIGMA和V矩阵，需要再将它们进行切割，分发到各个RoundRobinWorker节点上
        List<Matrix> tempUList = U.sliceByColEqually(blocks);
        List<Matrix> tempVList = V.sliceByColEqually(blocks);

        result.get(0).add(tempUList);
        result.get(1).add(tempVList);

    }
    /*****************************************************************************************************/
    /***********************************doAddByRow Finished**********************************************/
    /*****************************************************************************************************/


    /*****************************************************************************************************/
    /***********************************用于矩阵拼接的按列追加*********************************************
     /*****************************************************************************************************/
    private void doAddByCol(IntermediateResult[] msgList, int unfold, int blocks, List<List<List<Matrix>>> result) {
        Matrix U;
        Matrix V;
        double tol = 0;

//        if(hierarchy == 0)
//            log.info("I am in doAddByCol!");

        //构建矩阵U，将子U直接合并成一个大的U
        U = msgList[0].getUList().get(unfold);
        V = msgList[0].getVList().get(unfold);
        tol = msgList[0].getTol();


        for(int i = 1; i < msgList.length; i++) {
            Matrix tempU = msgList[i].getUList().get(unfold);
            Matrix tempV = msgList[i].getVList().get(unfold);
            U = U.addByCol(tempU);
            V = V.addByDig(tempV);
            tol += msgList[i].getTol();
        }

//        log.info("tol: " + tol);

        //此时已经得到了拼接后的U*SIGMA和V矩阵，需要再将它们进行切割，分发到各个RoundRobinWorker节点上
        List<Matrix> tempUList = U.sliceByColEqually(blocks);
        List<Matrix> tempVList = V.sliceByColEqually(blocks);

        result.get(0).add(tempUList);
        result.get(1).add(tempVList);
    }
    /*****************************************************************************************************/
    /***********************************doAddByCol Finished**********************************************/
    /*****************************************************************************************************/


    /*****************************************************************************************************/
    /***********************************用于矩阵拼接的按列穿插*********************************************
     /*****************************************************************************************************/
    private void doInsertByCol(IntermediateResult[] msgList, int unfold, int pieces, int blocks,
                               List<List<List<Matrix>>> result) {
        Matrix U;
        Matrix V;
        double tol = 0;

        //V矩阵的重构过程
        List<List<Matrix>> sliceV = new ArrayList<List<Matrix>>(msgList.length);
        for(int i = 0; i < msgList.length; i++) {
            List<Matrix> temp = msgList[i].getVList().get(unfold).sliceByRowEqually(pieces);
            sliceV.add(temp);
        }

        V = sliceV.get(0).get(0);
        for(int i = 1; i < msgList.length; i++) {
            V = V.addByDig(sliceV.get(i).get(0));
        }

        for(int i = 1; i < pieces; i++) {
            Matrix tempV = sliceV.get(0).get(i);
            for(int j = 1; j < msgList.length; j++) {
                tempV = tempV.addByDig(sliceV.get(j).get(i));
            }
            V = V.addByRow(tempV);
        }


        //构建矩阵U，将子U直接合并成一个大的U
        U = msgList[0].getUList().get(unfold);
        tol = msgList[0].getTol();

        for(int i = 1; i < msgList.length; i++) {
            U = U.addByCol(msgList[i].getUList().get(unfold));
            tol += msgList[i].getTol();
        }

//        log.info("tol: " + tol);

        //此时已经得到了拼接后的U*SIGMA和V矩阵，需要再将它们进行切割，分发到各个RoundRobinWorker节点上
        List<Matrix> tempUList = U.sliceByColEqually(blocks);
        List<Matrix> tempVList = V.sliceByColEqually(blocks);

        result.get(0).add(tempUList);
        result.get(1).add(tempVList);

    }
    /*****************************************************************************************************/
    /***********************************doInsertByCol Finished**********************************************/
    /*****************************************************************************************************/


    /**
     * 用于矩阵的拼接、然后切块处理，目的是为了分发给RoundRobinWorker进行环形处理
     * @param msgList
     * @return
     */
    public List<List<List<Matrix>>> CombineSplitMatrix(IntermediateResult[] msgList, int blocks) {
        //内部存放的是各自的UList和VList
        List<List<List<Matrix>>> result = new ArrayList<List<List<Matrix>>>(2);
        //初始化result数组
        //Mode-UList
        result.add(new ArrayList<List<Matrix>>());
        //Mode-VList
        result.add(new ArrayList<List<Matrix>>());

        //UList
//        result.get(0).add(new ArrayList<Matrix>());
//        VList
//        result.get(1).add(new ArrayList<Matrix>());


        ProcessResult WhichProcess = null;

        //对沿着各个阶展开的矩阵进行拼接
        for(int unfold = 0; unfold < Ranks; unfold++) {
            //判断在第hierarchy层的order展开矩阵的合并是属于哪一种情况
            WhichProcess = whichProcess(unfold,hierarchy,msgList[0]);

            switch (WhichProcess.getWhich()) {
                case 1:
                    //按行追加
                    doAddByRow(msgList,unfold,blocks,result);
                    break;
                case 2:
                    //按列追加
                    doAddByCol(msgList,unfold,blocks,result);
                    break;
                case 3:
                    //按列穿插
                    doAddByCol(msgList,unfold,blocks,result);
//                    doInsertByCol(msgList,unfold,WhichProcess.getPieces(),blocks,result);
                    break;
                default:
                    break;
            }
        }

        //将合并的结果返回
        List<List<List<Matrix>>> returnResult = new ArrayList<List<List<Matrix>>>(2);
        returnResult.add(new ArrayList<List<Matrix>>());
        returnResult.add(new ArrayList<List<Matrix>>());

        for(int block = 0; block < blocks; block++) {
            List<Matrix> tempU = new ArrayList<Matrix>();
            List<Matrix> tempV = new ArrayList<Matrix>();
           for(int unfold = 0; unfold < Ranks; unfold++) {
               tempU.add(result.get(0).get(unfold).get(block));
               tempV.add(result.get(1).get(unfold).get(block));
           }
            returnResult.get(0).add(tempU);
            returnResult.get(1).add(tempV);
        }
        result = null;
        return returnResult;
    }


    /**
     * 用于矩阵的合并、单机正交化处理
     * @param msgList
     * @return
     */
    public void CombineSplitRoundRobin(IntermediateResult[] msgList,
                                       ArrayList<ArrayList<Matrix>> updataList,
                                       ArrayList<ArrayList<Matrix>> dndataList) {
        ProcessResult WhichProcess = null;

        //对沿着各个阶展开的矩阵进行拼接
        for(int unfold = 0; unfold < Ranks; unfold++) {
            //判断在第hierarchy层的order展开矩阵的合并是属于哪一种情况
//            log.info(Arrays.toString(msgList[0].getDim()));
            WhichProcess = whichProcess(unfold,hierarchy,msgList[0]);

            switch (WhichProcess.getWhich()) {
                case 1:
                    //按行追加
                    doAddByRow(msgList,unfold,updataList,dndataList);
                    break;
                case 2:
                    //按列追加
                    doAddByCol(msgList,unfold,updataList,dndataList);
                    break;
                case 3:
                    //按列穿插
//                    doInsertByCol(msgList,unfold,WhichProcess.getPieces(),result);
                    doAddByCol(msgList,unfold,updataList,dndataList);
                    break;
                default:
                    break;
            }
        }

        //垃圾回收
//        System.gc();
    }

    /*****************************************************************************************************/
    /***********************************用于矩阵拼接的按行追加*********************************************
     /*****************************************************************************************************/
    private void doAddByRow(IntermediateResult[] msgList, int unfold,
                            ArrayList<ArrayList<Matrix>> up, ArrayList<ArrayList<Matrix>> dn) {
        Matrix U;
        Matrix V;
        double[] SIGMA;
        double tol = 0;


        Matrix V_SIG = msgList[0].getUList().get(unfold);
        U = msgList[0].getVList().get(unfold);
        SIGMA = new double[V_SIG.row];
        V = V_SIG.normalizeU(SIGMA);
        U.dotInSelf(SIGMA);
        tol = msgList[0].getTol();


        for(int i = 1; i < msgList.length; i++) {
            V_SIG = msgList[i].getUList().get(unfold);
            Matrix tempU = msgList[i].getVList().get(unfold);
            SIGMA = new double[V_SIG.row];
            V = V.addByDig(V_SIG.normalizeU(SIGMA));
            U = U.addByCol(tempU.dot(SIGMA));
            tol += msgList[i].getTol();
        }

        if (msgList.length == 1) {
            log.info(address + " : In AddByRow, I will not do InnerOrth!");
        }
        else {

            log.info(address + " : In AddByRow : V row " + V.row + " ,V col " + V.col + "; U row " + U.row + ",U col " + U.col);

            doInnerOrth(U, V, tol);

            log.info(address + " : AddByRow is finished!");
        }

        //按行追加的情况还需要将U、SIGMA、V再次还原
        SIGMA = new double[U.col];
        Matrix tempV = U.normalizeU(SIGMA);
        U = V.normalizeV(U);
        V = tempV;

        U.dotInSelf(SIGMA);

        List<Matrix> temp = U.sliceByColEqually(2);
        Matrix upU = temp.get(0);
        Matrix dnU = temp.get(1);

        temp = V.sliceByColEqually(2);
        Matrix upV = temp.get(0);
        Matrix dnV = temp.get(1);

        up.get(0).add(upU);
        up.get(1).add(upV);
        dn.get(0).add(dnU);
        dn.get(1).add(dnV);

        temp = null;
        U = null;
        V = null;
        tempV = null;
        dnU = null;
        dnV = null;
        upU = null;
        upV = null;
    }
    /*****************************************************************************************************/
    /***********************************doAddByRow Finished**********************************************/
    /*****************************************************************************************************/


    /*****************************************************************************************************/
    /***********************************用于矩阵拼接的按列追加*********************************************
     /*****************************************************************************************************/
    private void doAddByCol(IntermediateResult[] msgList, int unfold,
                            ArrayList<ArrayList<Matrix>> up, ArrayList<ArrayList<Matrix>> dn) {
        Matrix U;
        Matrix V;
        double tol = 0;

        //构建矩阵U，将子U直接合并成一个大的U
        U = msgList[0].getUList().get(unfold);
        V = msgList[0].getVList().get(unfold);
        tol = msgList[0].getTol();


        for(int i = 1; i < msgList.length; i++) {
            Matrix tempU = msgList[i].getUList().get(unfold);
            Matrix tempV = msgList[i].getVList().get(unfold);
            U = U.addByCol(tempU);
            V = V.addByDig(tempV);
            tol += msgList[i].getTol();
        }

        if (msgList.length == 1) {
            log.info(address + " : In AddByCol, I will not do InnerOrth!");
        }
        else {

            log.info(address + " : In AddByCol : V row " + V.row + " ,V col " + V.col + "; U row " + U.row + ",U col " + U.col);

            doInnerOrth(U, V, tol);

            log.info(address + " : AddByCol is finished!!!");
        }

        //需要将处理的结果进行排序、裁切
        V = V.normalizeV(U).sliceByCol(0, U.row-1);
        U = U.normalizeV(U).sliceByCol(0,U.row-1);


        double[] SIGMA = new double[U.col];

        //如果再上一层是按行追加的情况，需要将计算结果进行重新的整合
        if (unfold == hierarchy-1) {
            Matrix tempV = U.normalizeU(SIGMA);
            V.dotInSelf(SIGMA);
            U = V;
            V = tempV;
        }
        else
            U.normalizeU(SIGMA);


        List<Matrix> temp = U.sliceByColEqually(2);
        Matrix upU = temp.get(0);
        Matrix dnU = temp.get(1);

        temp = V.sliceByColEqually(2);
        Matrix upV = temp.get(0);
        Matrix dnV = temp.get(1);

        up.get(0).add(upU);
        up.get(1).add(upV);
        dn.get(0).add(dnU);
        dn.get(1).add(dnV);

        temp = null;
        U = null;
        V = null;
        dnU = null;
        dnV = null;
        upU = null;
        upV = null;
    }
/*****************************************************************************************************/
/***********************************doAddByCol Finished**********************************************/
/*****************************************************************************************************/



    //将矩阵写入到文件中，文件名为filename
    public void writeMatrixToFile(Matrix matrix, String filename) throws IOException {
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
        log.info(address + " : Write matrix: " + filename + " OK!");
    }

}