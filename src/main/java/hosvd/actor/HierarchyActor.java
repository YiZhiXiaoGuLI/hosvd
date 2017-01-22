package hosvd.actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.remote.RemoteScope;
import au.com.bytecode.opencsv.CSVWriter;
import hosvd.datatype.Matrix;
import hosvd.datatype.Tensor;
import hosvd.datatype.Vec;
import hosvd.message.HasDone;
import hosvd.multimessage.ArgumentsInitialization;
import hosvd.multimessage.BlockReturn;
import hosvd.multimessage.TensorInitialization;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by cxy on 15-10-22.
 */
public class HierarchyActor extends UntypedActor{
    private List<ActorRef> actorList;
    private ActorRef mainThread;
    private int[] diviedList;
    //本actor为第几层actor？
    private final int hierarchy;
    //本actor中所包含的张量块在上一层张量块中的位置序号
    private int numOfTensor;
    //精度范围
    private double precision;
    private Tensor tensor;
    //用于保存各个阶的长度
    private int[] dim;

    //正交化的误差的门限值
    private double tol;
    //用于计数BlockReturn_count的值，判断子张量块是否全部正交化完毕
    private int BlockReturn_count = 0;
    //用于保存子块返回的消息，里面存放了UData、VData和子张量块的位置信息
    private List<BlockReturn> msgList = new ArrayList<BlockReturn>();

    //用于存放各个阶段的按各模展开的U、V和SIGMA的列表
    private List<Matrix> UList = new ArrayList<>();
    private List<Matrix> VList = new ArrayList<>();
    private List<Matrix> SIGMAList = new ArrayList<>();


    static class ProcessResult {
        //代表是第几个处理结果
        //1代表按行追加，2代表按列追加，3代表按列穿插
        private int which;
        //interval表示每个子张量展开矩阵的按列穿插单位
        private int[] interval;

        public ProcessResult(int which, int[] interval) {
            this.which = which;
            this.interval = interval;
        }

        public int getWhich() {
            return which;
        }

        public int[] getInterval() {
            return interval;
        }
    }




    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public HierarchyActor(int hierarchy) {
        this.hierarchy = hierarchy;
        log.info("Hierarchy: " + hierarchy + " was created!!!");
    }

    @Override
    public void postStop() throws Exception {
//        super.postStop();
        log.info("I died!!!");
    }

    @Override
    public void onReceive(Object message) throws Exception {

        /*****************************************************************************************************/
        /*******************************************参数初始化*************************************************
         该函数在整个系统中只运行一次，用于初始化系统的启动参数，包括各个阶的分个数，张量的大小信息等
         /*****************************************************************************************************/
        if (message instanceof ArgumentsInitialization) {
            ArgumentsInitialization argumentsInitialization = (ArgumentsInitialization) message;

            int[] tempList = argumentsInitialization.getCount();
            int[] tempDim = argumentsInitialization.getDim();
            precision = argumentsInitialization.getPrecision();

            //构建张量
//            Tensor tempTensor = Tensor.setTensor3(tempDim[0], tempDim[1], tempDim[2], "./dataset", false);

            Random random = new Random(47);
            Tensor tempTensor = new Tensor(argumentsInitialization.getRank(),tempDim);

            int num = 1;
            for(int i : tempDim)
                    num *= i;

//            for(int l = 0; l < 20; l++)
//                for(int k = 0; k < 20; k++)
//                    for(int i = 0; i < 20; i++)
//                        for(int n = 0; n < 20; n++)
//                            for(int m = 0; m < 20; m++)
//                                for(int j = 0; j < 20; j++)
//                                    tempTensor.set(random.nextDouble(),l,k,i,n,m,j);

            for(int i = 0; i < num; i++)
                tempTensor.data[i] = random.nextDouble();

//            this.tensor = tempTensor;

            //将临时信息赋值给类的成员变量
            getSelf().tell(new TensorInitialization(tempTensor,tempList,precision,0), getSelf());
        }
        /*****************************************************************************************************/
        /********************************ArgumentsInitialization Finished*************************************/
        /*****************************************************************************************************/



        /*****************************************************************************************************/
        /******************递归构建处理actor，直到返回到此actor中，进行一次大的Roundrobin循环处理*******************
         张量块分发的原则:8->4->2->1，即先从x轴的方向上进行切，切成8/2=4块，则构建2个actor
         在子actor中，从y轴的方向上进行切块，切成4/2=2块，则构建2个actor
         在子actor中，从z轴的方向上进行切块，切成2/2=1块，则构建2个actor
         在每个actor中，单独处理每个最小的子张量块，在内存中做正交化，返回U SIGMA V
         返回到actor中，进行合并处理
         返回actor中，进行合并成处理
         在大的actor中，进行一次roundrobin循环处理，生成最终的结果

         最终生成的actor的个数即为countX*countY*countZ:
         最外层为一个主actor,用于将张量块进行切块，并创建第一层actor，并合并最终的矩阵块。第一层actor个数为countX个
         第一层actor，用于将张量块进行切块和一层合并，并创建第二层actor。第二层actor的个数为countY个
         第二层actor，用于将张量进行切块和三层合并，并创建第三层actor。第二层actor的个数为countZ个
         第三层actor，用于内部处理一个最小的子张量块
         *****************************************************************************************************/
        else if(message instanceof TensorInitialization) {

            log.info("I am in TensorInitialization");

            TensorInitialization tensorInitialization = (TensorInitialization)message;

            //获取每个阶上的分割次数
            diviedList = tensorInitialization.getDiviedList();
            //获取初始化张量块
            Tensor tensor = tensorInitialization.getTensor();
            //用于保存子块上的各个阶的长度
            dim = tensor.Dim;

            //获取到子张量块的位置信息
            numOfTensor = tensorInitialization.getNumOfTensor();

            //获取精度信息
            precision = tensorInitialization.getPrecision();

            //初始化正交门限值
            tol = Math.pow(tensor.norm(),2)*precision;

            //如果还未到达最后一层actor,即张量块还未分解完全，继续进行分解，进入下一层actor
            if(hierarchy < tensor.getRanks()) {
                List<Tensor> blockList = new ArrayList<>();
                //根据当前是第几层actor,在第几个阶上进行切分
                blockList = tensor.sliceEqually(hierarchy+1,diviedList[hierarchy]);

                actorList = new ArrayList<ActorRef>();


//                String host = "192.168.200.3";


                //创建下一层的actor
                for (int i = 0; i < diviedList[hierarchy]; i++) {
                    String host = "192.168.200.3";
                    if(hierarchy == 0) {
                        int start = 5;
                        host += Integer.toString(start);
                    }

                    else{
                        int start = 6;
                        start += (4*i)/diviedList[hierarchy];
                        host += Integer.toString(start);
                    }
                    Address addr = new Address("akka.tcp", "WorkerSystem", host, 2555);

                    ActorRef actor = getContext().actorOf(Props.create(HierarchyActor.class, hierarchy + 1).withDeploy(
                                    new Deploy(new RemoteScope(addr))),
                            "Level" + (hierarchy+1) + "-" + Integer.toString(i+1));
                    actorList.add(actor);
                }

                //将子张量块各自分发到下一层的actor中去
                for (int i = 0; i < diviedList[hierarchy]; i++) {
                    actorList.get(i).tell(new TensorInitialization(blockList.get(i), diviedList, precision, i), getSelf());
                }
            }

            //到达了最后一层actor，则进行内部的正交化处理
            //并生成UData、VData、U、SIGMA、V
            else {
                log.info("I am in InnerOrth");
                //张量按照各个模展开后的矩阵
                for(int i = 0; i < tensor.getRanks(); i++) {
                    UList.add(tensor.matricization(i + 1));
                    VList.add(Matrix.eye(UList.get(i).col));
                    log.info("matricization row: " + UList.get(i).row + " ,col: " + UList.get(i).col);
                }

                //按各个模展开后的矩阵依次做雅克比变换
                doInnerOrth(UList,VList,tol);
                //将正交化后的UData、VData返回到上一层actor中，让上一层actor进行合并
                getContext().parent().tell(new BlockReturn(UList,VList,numOfTensor,dim),getSelf());

                //停掉自己
                getContext().stop(self());
            }

        }
        /*****************************************************************************************************/
        /***********************************TensorInitialization Finished*************************************/
        /*****************************************************************************************************/



        /*****************************************************************************************************/
        /*******************************************两个子块的合并***********************************************
         将两个子张量块进行合并，并做正交化
         接收到BlcokReturn的actor的hierarchy的范围：0-2,3接收不到，因为3为最小粒度的actor，只做正交化处理，
         并将处理后的结果传输到上一层actor做合并、正交化处理
         /*****************************************************************************************************/
        else if(message instanceof BlockReturn) {
            log.info("I am in BlockReturn");
            BlockReturn blockReturn = (BlockReturn)message;
            //停止下一层的actor，因为它的工作已经完成了
//            getContext().stop(getSender());

            //计数器加1
            BlockReturn_count++;

            //用于保存消息记录，用于后续的数据的排序
            msgList.add(blockReturn);

            //如果接收到子块的数目等于这一阶上的分块数目,即所有的子块处理工作均已经完成
            //则将msgList中存放的消息按照numOfTensor的大小排序
            if(BlockReturn_count == diviedList[hierarchy]) {
                BlockReturn_count = 0;
                log.info("Returned Block Combine process!");
                BlockReturn[] temp = new BlockReturn[msgList.size()];
                for(BlockReturn b : msgList)
                    temp[b.getNumOfTensor()] = b;
                //按照从小到大的顺序对子张量块进行排序后。
                msgList = Arrays.asList(temp);

                /*****************************************************/
                //矩阵块的位置还原
                List<List<Matrix>> combineMatrix = CombineMatrix(msgList,hierarchy);

                //如果这不是最上面的一层actor，则将合并、正交化后的结果返回到上一层actor中做进一步处理
                if(hierarchy != 0) {
                    getContext().parent().tell(new BlockReturn(combineMatrix.get(0),combineMatrix.get(1),numOfTensor,dim),getSelf());
                }
                //如果这是最后一层的actor,则将计算后的结果输出到文件中
                else {
                    log.info("The process is done!");

                    //对于每一个按模展开的计算结果求出它的U矩阵
                    for(int unfold = 0; unfold < tensor.getRanks(); unfold++) {
                        //1.将U进行normalizeU的操作
                        Matrix U = combineMatrix.get(0).get(unfold).normalizeU()
                                .sliceByCol(0,tensor.Dim[unfold]-1).sliceByRow(0,tensor.Dim[unfold]-1);
                        writeMatrixToFile(U,"U-mode"+(unfold+1));
                    }

                    mainThread.tell("SUCCESS",getSelf());
                }
            }

        }
        /*****************************************************************************************************/
        /***********************************BlockReturn Finished**********************************************/
        /*****************************************************************************************************/


        else if(message instanceof HasDone) {
            log.info("I received HasDone message");
            mainThread = getSender();
        }

    }



    /*****************************************************************************************************/
    /***********************************用于矩阵的合并与位置还原*********************************************
     模1展开： 按列穿插 -> 按列追加 -> 按行追加  I1 * (I3 * I2)
     模2展开： 按列追加 -> 按行追加 -> 按列穿插  I2 * (I1 * I3)
     模3展开： 按行追加 -> 按列穿插 -> 按列追加  I3 * (I2 * I1)

     result: 返回结果，里面只存放要直接进行正交化的U、V，不需要再做进一步的变换了。
     /*****************************************************************************************************/
    private  List<List<Matrix>> CombineMatrix(List<BlockReturn> msgList, int hierarchy) {
        //内部存放的是各自的UList和VList
        List<List<Matrix>> result = new ArrayList<List<Matrix>>();
        //初始化result数组
        result.add(new ArrayList<Matrix>());
        result.add(new ArrayList<Matrix>());

        ProcessResult WhichProcess = null;


        //对沿着各个阶展开的矩阵进行拼接
        for(int unfold = 0; unfold < tensor.getRanks(); unfold++) {
            //判断在第hierarchy层的order展开矩阵的合并是属于哪一种情况
            WhichProcess = whichProcess(unfold,hierarchy,msgList);

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
                    doInsertByCol(msgList,unfold,WhichProcess.getInterval(),result);
                    break;
                default:
                    break;
            }

        }

        //将合并的结果返回
        return result;
    }
    /*****************************************************************************************************/
    /***********************************CombineMatrix Finished********************************************/
    /*****************************************************************************************************/



    /*****************************************************************************************************/
    /***********************************用于计算采用哪种方式进行矩阵拼接*******************************************
     /*****************************************************************************************************/
    /**
     *
     * @param unfold 按照哪一模进行展开
     * @param incremental 按照哪一模进行增量
     * @param msgList 传递进来的数据信息
     * @return 选择的结果（1：按行追加 2：按列追加 3：按列穿插）
     */
    private ProcessResult whichProcess(int unfold, int incremental, List<BlockReturn> msgList) {
        ProcessResult result = null;

        if (unfold == incremental) {
            //按行追加的情况
            result = new ProcessResult(1,null);
        }

        else if ( ((unfold+1) % tensor.getRanks()) == incremental) {
            //按列追加的情况
            result = new ProcessResult(2,null);
        }

        else {
            //按列穿插的情况
            int[] interval = new int[msgList.size()];
            Arrays.fill(interval,1);
            for(int i = incremental; i != unfold; i = (i+1)%tensor.getRanks()) {
                //对于每个分块，它的拼接单位可能是不同的
                for(int j = 0; j < interval.length; j++) {
                    interval[j] *= msgList.get(j).getDim()[i];
                }
            }
            result = new ProcessResult(3,interval);
        }

        return result;
    }
    /*****************************************************************************************************/
    /***********************************whichProcess Finished**********************************************/
    /*****************************************************************************************************/







    /*****************************************************************************************************/
    /***********************************用于矩阵拼接的按行追加*********************************************
     /*****************************************************************************************************/
    private void doAddByRow(List<BlockReturn> msgList, int unfold, List<List<Matrix>> result) {
        Matrix U;
        Matrix V;
        Matrix SIGMA;


        Matrix V_SIG = msgList.get(0).getUList().get(unfold);
        U = msgList.get(0).getVList().get(unfold).normalizeV(V_SIG).sliceByCol(0,V_SIG.row-1);
        V_SIG = V_SIG.normalizeV(V_SIG).sliceByCol(0,V_SIG.row-1);
        SIGMA = new Matrix(V_SIG.row,V_SIG.row);
        V = V_SIG.normalizeU(SIGMA);
        U = U.dot(SIGMA);


        if(hierarchy == 0) {
            log.info("I am in doAddByRow1!");
            log.info("doAddByRow: U" + U.row + " " + U.col);
            log.info("doAddByRow: V" + V.row + " " + V.col);
        }

        for(int i = 1; i < msgList.size(); i++) {
            V_SIG = msgList.get(i).getUList().get(unfold);
            Matrix tempU = msgList.get(i).getVList().get(unfold).normalizeV(V_SIG).sliceByCol(0, V_SIG.row - 1);
            V_SIG = V_SIG.normalizeV(V_SIG).sliceByCol(0,V_SIG.row-1);
            SIGMA = new Matrix(V_SIG.row,V_SIG.row);
            V = V.addByDig(V_SIG.normalizeU(SIGMA));
            U = U.addByCol(tempU.dot(SIGMA));
        }


        if(hierarchy == 0) {
            log.info("I am in doAddByRow2!");
            log.info("doAddByRow: U" + U.row + " " + U.col);
            log.info("doAddByRow: V" + V.row + " " + V.col);
        }

        doInnerOrth(U, V, tol);

        if(hierarchy == 0)
            log.info("I am in doAddByRow3!");


        //按行追加的情况还需要将U、SIGMA、V再次还原
        SIGMA = new Matrix(U.col, U.col);
        Matrix tempV = U.normalizeU(SIGMA);
//        SIGMA = SIGMA.sliceByCol(0,U.row-1);
//        log.info("tempv: " + tempV.row + " " + tempV.col);
        U = V.normalizeV(U);
        V = tempV;

        log.info("doAddByRow: U" + U.row + " " + U.col);
        log.info("doAddByRow: V" + V.row + " " + V.col);

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
    private void doAddByCol(List<BlockReturn> msgList, int unfold, List<List<Matrix>> result) {
        Matrix U;
        Matrix V;
        Matrix SIGMA;

        if(hierarchy == 0)
            log.info("I am in doAddByCol!");

        //构建矩阵U，将子U直接合并成一个大的U
        U = msgList.get(0).getUList().get(unfold);
        V = msgList.get(0).getVList().get(unfold);
        V = V.normalizeV(U).sliceByCol(0, U.row - 1);
        U = U.normalizeV(U).sliceByCol(0, U.row - 1);

        for(int i = 1; i < msgList.size(); i++) {
            Matrix tempU = msgList.get(i).getUList().get(unfold);
            Matrix tempV = msgList.get(i).getVList().get(unfold);
            tempV = tempV.normalizeV(tempU).sliceByCol(0,tempU.row-1);
            tempU = tempU.normalizeV(tempU).sliceByCol(0,tempU.row-1);
            U = U.addByCol(tempU);
            V = V.addByDig(tempV);
        }


        doInnerOrth(U,V,tol);

        log.info("doAddByCol: U" + U.row + " " + U.col);
        log.info("doAddByCol: V" + V.row + " " + V.col);

        result.get(0).add(U);
        result.get(1).add(V);
    }
    /*****************************************************************************************************/
    /***********************************doAddByCol Finished**********************************************/
    /*****************************************************************************************************/


    /*****************************************************************************************************/
    /***********************************用于矩阵拼接的按列穿插*********************************************
     /*****************************************************************************************************/
    private void doInsertByCol(List<BlockReturn> msgList, int unfold, int[] interval, List<List<Matrix>> result) {
        Matrix U;
        Matrix V;
        Matrix SIGMA;
        Matrix elementaryMatrix;

        if(hierarchy == 0)
            log.info("I am in doInsertByCol!");

        //构建矩阵U，将子U直接合并成一个大的U
        U = msgList.get(0).getUList().get(unfold);
        V = msgList.get(0).getVList().get(unfold);
        V = V.normalizeV(U).sliceByCol(0, U.row - 1);
        U = U.normalizeV(U).sliceByCol(0,U.row-1);

        for(int i = 1; i < msgList.size(); i++) {
            Matrix tempU = msgList.get(i).getUList().get(unfold);
            Matrix tempV = msgList.get(i).getVList().get(unfold);
            tempV = tempV.normalizeV(tempU).sliceByCol(0,tempU.row-1);
            tempU = tempU.normalizeV(tempU).sliceByCol(0,tempU.row-1);
            U = U.addByCol(tempU);
            V = V.addByDig(tempV);
        }

        //构建初等矩阵
        int colnums = 1;
        for(int i = (unfold+1)%tensor.getRanks(); i != unfold; i = (i+1)%tensor.getRanks()) {
            colnums *= tensor.Dim[i];
        }
        elementaryMatrix = new Matrix(colnums,colnums);


        int dual = 0;
        for(int i = 0; i < interval.length; i++)
            dual += interval[i];

        int howmany = colnums/dual;

        int intervalX = 0;
        int intervalY = 0;

        //从msgList的每一个元素开始，进行重排列,有多少个块
        for(int i = 0; i < msgList.size(); i++) {
            for(int j = 0; j < howmany; j++) {
                int startX = interval[i]*j + intervalX;
                int startY = dual*j + intervalY;
                //对初等矩阵M进行赋值，保持原始展开矩阵的顺序
                for(int k = 0; k < interval[i]; k++) {
                    elementaryMatrix.set(startX+k,startY+k,1);
                }
            }
            intervalX += howmany*interval[i];
            intervalY += interval[i];
        }

//        log.info("elementary: " + elementaryMatrix.row +" " + elementaryMatrix.col);

        V = elementaryMatrix.trans().dot(V);
        doInnerOrth(U,V,tol);

        log.info("doInsertByCol: U " + U.row + " " + U.col);
        log.info("doInsertByCol: V " + V.row + " " + V.col);


        result.get(0).add(U);
        result.get(1).add(V);

    }
    /*****************************************************************************************************/
    /***********************************doInsertByCol Finished**********************************************/
    /*****************************************************************************************************/


    /*****************************************************************************************************/
    /***********************************用于矩阵的内部正交化过程*********************************************
     /*****************************************************************************************************/
    private void doInnerOrth(List<Matrix> ulist, List<Matrix> vlist, double tol) {
        int maxSteps = 5000;
        for(int i = 0; i < ulist.size(); i++) {
            for (int j = 0; j < maxSteps; j++) {
                boolean con = innerOrthForDim1(ulist.get(i), vlist.get(i), tol);
                if (con) {
                    log.info("Converged Mode: " + (i+1) + "! With steps of" + j);
                    break;
                }
            }
        }
    }
    /*****************************************************************************************************/
    /***********************************doInnerOrth Finished**********************************************/
    /*****************************************************************************************************/

    /*****************************************************************************************************/
    /***********************************用于矩阵的内部正交化过程*********************************************
     /*****************************************************************************************************/
    private void doInnerOrth(Matrix U, Matrix V, double tol) {
        int maxSteps = 5000;

        if(hierarchy == 0)
            log.info("I am in doInnerOrth!");

        for (int i = 0; i < maxSteps; i++) {
            boolean con = innerOrthForDim1(U, V, tol);
            if (con) {
                log.info("Converged With steps of: " + i);
                break;
            }
        }
    }
    /*****************************************************************************************************/
    /***********************************doInnerOrth Finished**********************************************/
    /*****************************************************************************************************/




    //将矩阵写入到文件中，文件名为filename
    private void writeMatrixToFile(Matrix matrix, String filename) throws IOException {
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
        log.info("Write matrix: " + filename + " OK!");
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
