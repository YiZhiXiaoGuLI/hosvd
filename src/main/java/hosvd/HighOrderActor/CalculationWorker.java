package hosvd.HighOrderActor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import hosvd.RoundRobinMessage.*;
import hosvd.datatype.Matrix;
import hosvd.datatype.Tensor;
import hosvd.message.HasDone;
import hosvd.multimessage.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import  hosvd.utils.HosvdUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Created by cxy on 15-12-21.
 */
public class CalculationWorker extends UntypedActor{
//    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private static Logger log = Logger.getLogger(CalculationWorker.class);


    private final int ID; //这个CalculationWorker的ID号
    private final int ORDER; //这个CalculationWorker所处的阶数0-N
    private final int Ranks; //整个张量的阶数
    private final boolean isRoundRobin;
    private int[] Count; //张量按照各个模的切块数目
    private int[] dim; //子张量块各个阶的维度
    private int IntermediateCounter = 0; //Intermediate计数器
    private int colNumber = 0; //矩阵列的数量
    private double tol = 0; //雅克比的误差系数
    private HosvdUtils hosvdUtils; //HOSVD分解的工具类

    private long start_time;


    private IntermediateResult[] msgList; //用于保存中间计算结果，以便进行合并、拼接过程中的张量位置恢复

    /**************用于preRoundRobin过程的一些变量******************/
    private int preRoundRobinCounter = 0;
    private int preRoundRobinDoneCounter = 0;
    private ArrayList<Integer>[] preRoundRobinMsgList;
    private ActorRef[] childrenList;
    private ArrayList<ArrayList<Matrix>> updataList;
    private ArrayList<ArrayList<Matrix>> dndataList;
    private ArrayList<ArrayList<Matrix>> updataListBack;
    private ArrayList<ArrayList<Matrix>> dndataListBack;
    private ActorRef parent;
    /**************用于preRoundRobin过程的一些变量******************/

    /*********用于RoundRobin过程的一些变量**************/
    private int maxStep = 1000; //最大的迭代次数
    private int innerOrthCounter = 0; //块内正交的计数器
    private int roundRobinWorkerNumber; //RoundRobinWorker的数目
    private int outterOrthCounter = 0; //块间正交的计数器
    private int roundRobinProcessNumber = 0; //RoundRobin的循环次数
    private int roundRobinMoveCounter = 0; //RoundRobin的循环次数
    private int roundRobinBackUpCounter = 0;
    private int sweepCounter = 0; //扫描计数器
    private int queryConvergedCounter = 0; //全局正交化过程的计数器
    private int returnedBlocksCounter = 0; //张量子块返回的计数器

    private List<Integer> filterList; //过滤列表，已经正交化的模展开矩阵不在其中
    private List<Boolean> allConverged;
    private List<Matrix> matrixU; //保存最终的各个按模展开矩阵后的U
    private List<Matrix> matrixV; //保存最终的各个按模展开矩阵后的V
    private int workerNumber;
    private int receiveCounter = 0; //用于统计在RoundRobin过程中的节点接收到了几次数据
    private double TotalTol = 0;

    /*********用于RoundRobin过程的一些变量**************/


    /**
     *
     * @param order
     * @param workerNumber
     * @param count
     */
    public CalculationWorker(int order, int workerNumber, int[] count, boolean isRoundRobin) {
        this.ID = workerNumber;
        this.ORDER = order;
        this.Count = count;
        this.Ranks = count.length;
        this.isRoundRobin = isRoundRobin;

        //注册一个HosvdUtils工具类
        hosvdUtils = new HosvdUtils(log,getSelf().path().toString(),Ranks,ORDER);

        if(order != Ranks)
            msgList = new IntermediateResult[Count[order]];
        else
            msgList = null;

        PropertyConfigurator.configure("log4j.properties");
    }

    @Override
    public void preStart() throws Exception {
        start_time = System.currentTimeMillis();
        log.info(getSelf().path() + " has been constructed!!!");
        getContext().parent().tell(new HasDone(), getSelf());
    }

    @Override
    public void postStop() throws Exception {
        //计算完以后，则将自己杀掉，释放资源
        long durationTime = System.currentTimeMillis() - start_time;
        log.info(getSelf().path() + " lived : " + durationTime + " mill seconds! Finished!!!");
        System.gc();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        /**
         *各个子张量块的按摸展开并正交化处理
         * 只会发生在最小张量块的展开、分解过程中，中间结果的合并处理不会出现在此处
         */
        if(message instanceof TensorInitialization) {
            TensorInitialization tensorInitialization = (TensorInitialization)message;
            Tensor tensor = tensorInitialization.getTensor();
            double precision = tensorInitialization.getPrecision();
            dim = tensor.Dim;

            //累积误差
            tol = Math.pow(tensor.norm(), 2)*precision;

            //用于存放各个阶段的按各模展开的U、V和SIGMA的列表
            List<Matrix> UList = new ArrayList<>();
            List<Matrix> VList = new ArrayList<>();

            for(int i = 0; i < Ranks; i++) {
                UList.add(tensor.matricization(i + 1));
                VList.add(Matrix.eye(UList.get(i).col));//设置一个单位方阵添加到VList
//                log.info("matricization row: " + UList.get(i).row + " ,col: " + UList.get(i).col);
            }

            //子块张量进行展开、正交化处理的操作
            hosvdUtils.doInnerOrth(UList, VList, tol);

            //对正交化后的结果进行裁剪
            for(int i = 0; i < UList.size(); i++) {
                Matrix U = UList.get(i);
                Matrix V = VList.get(i);
                V = V.normalizeV(U).sliceByCol(0,U.row-1);
                U = U.normalizeV(U).sliceByCol(0,U.row-1);
                UList.set(i,U);
                VList.set(i,V);
//                log.info("U -> row: " + UList.get(i).row + " ,col: " + UList.get(i).col +
//                        " V -> row: " + VList.get(i).row + " ,col: " + VList.get(i).col);
            }

            //然后将处理后的数据再次发送到相应的节点上，进行合并、正交化处理工作
            //发送到哪一个节点上进行合并、还原处理
            int destination = ID /Count[ORDER-1];
            //发送到节点上所对应的拼接序号
            int serialNumber = ID %Count[ORDER-1];

            String address = "../../OrderWorker" + (ORDER - 1) + "/CalculationWorker" + destination;

            getContext().actorSelection(address)
                    .tell(new IntermediateResult(UList, VList, serialNumber, tol, dim), self());

            //通知系统进行垃圾回收
            //？？？可以执行吗？？？
            UList = null;
            VList = null;
            //停止掉自己？可以这样做吗？不会产生sender not alive的错误吗？
            getContext().stop(getSelf());
        }

        /**
         * 中间结果的合并还原处理
         * 此处的消息只能是 OrderWorker0 - OrderWorkerN-1上的CalculationWorker
         * 才能够接收到，OrderWorkerN节点不接收此消息
         */
        else if(message instanceof IntermediateResult) {
            //接受到消息后就停止掉发送端，释放资源
            getContext().system().stop(sender());

            IntermediateResult intermediateResult = (IntermediateResult) message;
            int serialNumber = intermediateResult.getSerialNumber();

            int tempCol = 0;
            //计算按不同模展开的矩阵的列的总长度，目的是为了求平均值
            for(Matrix tempV : intermediateResult.getVList())
                tempCol += tempV.col;
            colNumber += tempCol/intermediateResult.getVList().size();

            IntermediateCounter++;
            msgList[serialNumber] = intermediateResult;

            //中间处理结果的合并
            if (IntermediateCounter == Count[ORDER]) {
                IntermediateCounter = 0;

                int dimOfOrder = msgList[0].getDim()[ORDER];
                tol = msgList[0].getTol();
                //msgList.length is 2 here if count is {5,2,2}
                for(int i = 1; i < msgList.length; i++) {
                    dimOfOrder += msgList[i].getDim()[ORDER];
                    tol += msgList[i].getTol();
                }

                dim = new int[Ranks];
                System.arraycopy(msgList[0].getDim(),0,dim,0,Ranks);
                dim[ORDER] = dimOfOrder;
                //System.out.println("dim is "+ dim[0]+ " " +dim[1]+ " " + dim[2]);
                //dim is {6, 15, 30}

//                log.info("Returned Block Combine process!");
                log.info(getSelf().path() + " dim : " + Arrays.toString(dim) + " msgList[0]: " + Arrays.toString(msgList[0].getDim()) +
                        " tol : " + tol + " colNumber: " + colNumber);


                //该节点的上一层需要进行RoundRobin操作，则在这一层需要把矩阵按列切块成上下两块
                if (ORDER == 1 && isRoundRobin) {
                    updataList = new ArrayList<ArrayList<Matrix>>();
                    //upUdataList
                    updataList.add(new ArrayList<Matrix>());
                    //upVdataList
                    updataList.add(new ArrayList<Matrix>());

                    dndataList = new ArrayList<>();
                    //dnUdataList
                    dndataList.add(new ArrayList<Matrix>());
                    //dnVdataList
                    dndataList.add(new ArrayList<Matrix>());

                    log.info(getSelf().path() + " : I will in CombineSplitRoundRobin");

                    //此处属于最后进行优化的阶段，此处优化比较复杂，程序控制比较困难
                    //矩阵块的位置还原与正交化的计算
                    hosvdUtils.CombineSplitRoundRobin(msgList,updataList,dndataList);
                    msgList = null;

                    //保存每一个按模展开矩阵的V的行数
                    ArrayList<Integer> v_row = new ArrayList<Integer>(Ranks);
                    for (int unfold = 0; unfold < Ranks; unfold++) {
                        int vrow = updataList.get(1).get(unfold).row;
                        v_row.add(vrow);
                    }

                    log.info(getSelf().path() + " : My row is :" + v_row.toString());

                    //将张量的按模展开矩阵V的行数发送到上一层节点，由它来计算新生成的V的行数为多少
                    //然后将处理后的数据再次发送到相应的节点上，进行合并、正交化处理工作
                    int destination = ID / Count[ORDER - 1];
                    serialNumber = ID % Count[ORDER - 1];
                    String address = "../../OrderWorker" + (ORDER - 1) + "/CalculationWorker" + destination;

                    //通知父节点进行行数的计算
                    getContext().actorSelection(address).tell(new PreRoundRobin(v_row, serialNumber, tol), self());
                }

                /***********************************************************/
                /**否则还是在单机上进行处理，单机能够处理小于10000个列的矩阵的正交化计算**/
                /***********************************************************/
                else {
                    //如果该层只有一个， 直接发送到上一层，不再做重复的计算
                    if (Count[ORDER] == 1) {
                        log.info("In Level: " + ORDER + " , I will do nothing but send my data up!");
                        if (ORDER != 0) {
                            //然后将处理后的数据再次发送到相应的节点上，进行合并、正交化处理工作
                            int destination = ID / Count[ORDER - 1];
                            serialNumber = ID % Count[ORDER - 1];

                            String address = "../../OrderWorker" + (ORDER - 1) + "/CalculationWorker" + destination;

                            getContext().actorSelection(address)
                                    .tell(new IntermediateResult(msgList[0].getUList(), msgList[0].getVList(), serialNumber, tol, dim), self());


                        } else {
                            //如果是OrderWorker0上的CalculationWorker上的节点，则处理相关的操作，并最终释放资源
                            log.info(getSelf().path() + " : The process is done!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            //对于每一个按模展开的计算结果求出它的U矩阵
                            for (int unfold = 0; unfold < Ranks; unfold++) {
                                //1.将U进行normalizeU的操作
                                Matrix U = msgList[0].getUList().get(unfold).normalizeU();
                                String address = "../../";
//                            hosvdUtils.writeMatrixToFile(U, "U-mode" + (unfold + 1));
                                getContext().actorSelection(address).tell(new UData(U,unfold+1), self());
                            }

                            getContext().stop(self());
                        }
                        msgList = null;
                    }

                    else {
                        //矩阵块的位置还原与正交化的计算
                        List<List<Matrix>> combineMatrix = hosvdUtils.CombineMatrix(msgList);
                        msgList = null;

                        //要修改的地方在此处，需要将合并后的矩阵进行切块，然后分发到RoundRobin节点中
                        //进行环形的正交化处理操作


                        if (ORDER != 0) {
                            //然后将处理后的数据再次发送到相应的节点上，进行合并、正交化处理工作
                            int destination = ID / Count[ORDER - 1];
                            serialNumber = ID % Count[ORDER - 1];

                            String address = "../../OrderWorker" + (ORDER - 1) + "/CalculationWorker" + destination;

                            getContext().actorSelection(address)
                                    .tell(new IntermediateResult(combineMatrix.get(0), combineMatrix.get(1), serialNumber, tol, dim), self());

                            combineMatrix = null;

                        } else {
                            //如果是OrderWorker0上的CalculationWorker上的节点，则处理相关的操作，并最终释放资源
                            log.info(getSelf().path() + " : The process is done!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                            //对于每一个按模展开的计算结果求出它的U矩阵
                            for (int unfold = 0; unfold < Ranks; unfold++) {
                                //1.将U进行normalizeU的操作
                                Matrix U = combineMatrix.get(0).get(unfold).normalizeU();
                                String address = "../../";
//                            hosvdUtils.writeMatrixToFile(U, "U-mode" + (unfold + 1));
                                getContext().actorSelection(address).tell(new UData(U, unfold + 1), self());
                            }

                            getContext().stop(self());
                        }
                    }
                }
                //执行垃圾回收？
                System.gc();
            }
        }

        /**
         *父节点接收到消息
         */
        /**将张量的按模展开矩阵V的行数发送到上一层节点，由它来计算新生成的V的行数为多少**/
        else if (message instanceof PreRoundRobin) {
            preRoundRobinCounter++;
            //第一次接收到该类消息，则初始化相应的数据
            if(preRoundRobinCounter == 1) {
                roundRobinWorkerNumber = Count[ORDER];
                preRoundRobinMsgList = new ArrayList[roundRobinWorkerNumber];
                childrenList = new ActorRef[roundRobinWorkerNumber];
                tol = 0;
            }

            PreRoundRobin preRoundRobin = (PreRoundRobin) message;
            int serialNum = preRoundRobin.getSerialNumber();
            tol += preRoundRobin.getTol();
            preRoundRobinMsgList[serialNum] = preRoundRobin.getVRow();
            //获取子节点的地址
            childrenList[serialNum] = getSender();

            //接收到所有的消息，则进行一次全局的统计计算
            if (preRoundRobinCounter == roundRobinWorkerNumber) {
                preRoundRobinCounter = 0;

                int[] totalRows = new int[Ranks];
                for (int i = 0; i < Ranks; i++) {
                    for (int j = 0; j < preRoundRobinMsgList.length; j++) {
                        totalRows[i] += preRoundRobinMsgList[j].get(i);
                    }
                }

                log.info(getSelf().path() + " : Total row is " + Arrays.toString(totalRows));

                //告诉每个子节点Ｖ的行长、以及它在整个矩阵中的偏移量
                for (int i = 0; i < roundRobinWorkerNumber; i++) {
                    int[] offset = new int[Ranks];
                    for (int j = 0; j < Ranks; j++) {
                        for (int k = 0; k < i; k++) {
                            offset[j] += preRoundRobinMsgList[k].get(j);
                        }
                    }
                    //告诉每个子节点它的总行数、偏移量、总体误差以及子节点地址列表
                    childrenList[i].tell(new AnswerPreRoundRobin(totalRows,offset,tol,childrenList), getSelf());
                }

                preRoundRobinMsgList = null;
            }
        }


        /**
         * 将矩阵块切成上下两个部分，并将相应的Ｖ的行进行扩充
         * 子节点接收到消息
         */
        else if(message instanceof AnswerPreRoundRobin) {
            AnswerPreRoundRobin answerPreRoundRobin = (AnswerPreRoundRobin) message;
            int[] Totals = answerPreRoundRobin.getTotalRow();
            int[] Offsets = answerPreRoundRobin.getOffset();
            parent = getSender();
            //初始化RoundRobin过程中必须的数据
            childrenList = answerPreRoundRobin.getChildrenList();
            workerNumber = childrenList.length;
            TotalTol = answerPreRoundRobin.getTol();

            //初始化，是几阶张量，就有几个展开矩阵需要做正交化处理
            allConverged = new ArrayList<>();
            filterList = new ArrayList<>();
            //初始化标记信息
            for(int i = 0; i < Ranks; i++) {
                allConverged.add(true);
                //记录按模展开的矩阵的个数即张量的阶
                filterList.add(i);
            }

            //根据Totals和Offsets调整upV 和 dnV这两个矩阵
            for (int unfold = 0; unfold < Ranks; unfold++) {
                int off = Offsets[unfold];

                Matrix upV = updataList.get(1).get(unfold);
                Matrix tempv = new Matrix(Totals[unfold],upV.col);
                for (int row = 0; row < upV.row; row++) {
                    for (int col = 0; col < upV.col; col++) {
                        tempv.set(row+off,col,upV.get(row,col));
                    }
                }
                updataList.get(1).set(unfold,tempv);

                Matrix dnV = dndataList.get(1).get(unfold);
                tempv = new Matrix(Totals[unfold],dnV.col);
                for (int row = 0; row < dnV.row; row++) {
                    for (int col = 0; col < dnV.col; col++) {
                        tempv.set(row+off,col,dnV.get(row,col));
                    }
                }
                dndataList.get(1).set(unfold,tempv);
            }

            parent.tell(new PreRoundRobinDone(), self());

//            System.gc();
        }


        /**
         * PreRoundRobinDone
         * 父节点接收到消息
         */
        else if (message instanceof PreRoundRobinDone) {
            preRoundRobinDoneCounter++;
            if (preRoundRobinDoneCounter == roundRobinWorkerNumber) {
                allConverged = new ArrayList<>();
                filterList = new ArrayList<>();
                matrixU = new ArrayList<>();
                matrixV = new ArrayList<>();

                //开始通知各个子节点进行RoundRobin
                for(int i = 0; i < Ranks; i++) {
                    allConverged.add(true);
                    matrixU.add(Matrix.emptyMatrix());
                    matrixV.add(Matrix.emptyMatrix());
                    filterList.add(i);
                }

                //通知各个节点进行数据的备份操作
                for (int i = 0; i < roundRobinWorkerNumber; i++) {
                    childrenList[i].tell(new RoundRobinBackUp(),getSelf());
                }
            }
        }


        /**
         * Step1: RoundRobinBackUp
         * 将要发送的数据备份到缓冲区中
         * 子节点接收到消息
         */
        else if (message instanceof RoundRobinBackUp) {

            updataListBack = new ArrayList<>();
            updataListBack.add(new ArrayList<Matrix>());
            updataListBack.add(new ArrayList<Matrix>());

            dndataListBack = new ArrayList<>();
            dndataListBack.add(new ArrayList<Matrix>());
            dndataListBack.add(new ArrayList<Matrix>());


            //如果它是最后的一个节点，则只备份dnDataList,upDataList不用进行备份
            //因为最后一个节点的upDataList在整个RoundRobinProcess中保持不变
            //其他的节点的upDataList与dnDataList都需要进行备份保存工作
            //只备份还未正交化的数据块,已正交化的数据不用备份到发送缓冲区中
            for(int i : filterList) {
                //如果当前节点不是最后一个节点
                if(ID != Count[ORDER-1]-1) {
                    updataListBack.get(0).add(updataList.get(0).get(i));
                    updataListBack.get(1).add(updataList.get(1).get(i));
                }
                dndataListBack.get(0).add(dndataList.get(0).get(i));
                dndataListBack.get(1).add(dndataList.get(1).get(i));
            }

            //备份完成之后，通知管理节点，备份成功
            parent.tell(new RoundRobinBackUpDone(), getSelf());
            System.gc();
        }

        /**
         *RoundRobinBackUp完成，即完成了数据块的备份
         *父节点接收到消息
         */
        else if(message instanceof RoundRobinBackUpDone) {
            roundRobinBackUpCounter++;
            if(roundRobinBackUpCounter == roundRobinWorkerNumber) {
//                log.info("RoundRobinBackUpDone !!!");
                //完成了数据块的备份，则通知各个节点将缓冲区中的数据发送到相应的位置
                roundRobinBackUpCounter = 0;
                for(ActorRef actor : childrenList) {
                    actor.tell(new RoundRobinSendBuffer(),getSelf());
                }
            }
        }

        /**
         * 将发送缓冲区中的数据发送到相应节点的相应位置，完成一次RoundRobinProcess
         * 子节点接收到消息
         */
        else if(message instanceof RoundRobinSendBuffer) {
            log.info(getSelf().path() + " : I will do RoundRobinSendBuffer!!!");

            if(ID == 0) {
                ActorRef nextLocation = childrenList[1];

                nextLocation.tell(new RoundRobin(dndataListBack, "dn"), getSelf());
                getSelf().tell(new RoundRobin(updataListBack, "dn"), getSelf());

            } else if(ID == workerNumber-1) {
                //将dnBackBuffer的数据发送到preLocation
                ActorRef preLocation = childrenList[workerNumber-2];
                preLocation.tell(new RoundRobin(dndataListBack,"up"),getSelf());

            }else {
                //依次将upBackBuffer的数据发送到preLocation
                //依次将dnBackBuffer的数据发送到nextLocation
                ActorRef nextLocation = childrenList[ID+1];
                ActorRef preLocation = childrenList[ID-1];

                nextLocation.tell(new RoundRobin(dndataListBack,"dn"),getSelf());
                preLocation.tell(new RoundRobin(updataListBack,"up"),getSelf());
            }
        }


        /**
         * 进行一次数据块的传输
         * 子节点接收到消息
         */
        else if(message instanceof RoundRobin) {
            log.info(getSelf().path()+  " : recveived RoundRobin message!!!");
            RoundRobin roundRobin = (RoundRobin)message;
            ArrayList<ArrayList<Matrix>> backList = roundRobin.getData();

            receiveCounter++;

            switch (roundRobin.getFlag()) {
                case "dn" :
                    //用接收的数据覆盖dnDataList
                    for(int i = 0; i < filterList.size(); i++) {
                        dndataList.get(0).set(filterList.get(i),backList.get(0).get(i));
                        dndataList.get(1).set(filterList.get(i),backList.get(1).get(i));
                    }
                    break;

                case "up":
                    //用接收的数据覆盖dnDataList
                    for(int i = 0; i < filterList.size(); i++) {
                        updataList.get(0).set(filterList.get(i),backList.get(0).get(i));
                        updataList.get(1).set(filterList.get(i),backList.get(1).get(i));
                    }
                    break;
                default:
                    break;
            }

            //在完成了数据块的覆盖后，判断是否已经全部接收成功
            if(ID == (workerNumber-1)) {
                receiveCounter = 0;
                parent.tell(new RoundRobinProcessDone(), getSelf());
                log.info(getSelf().path() + " : I end in RoundRobin");
            }else {
                if(receiveCounter == 2) {
                    receiveCounter = 0;
                    parent.tell(new RoundRobinProcessDone(), getSelf());
                    log.info(getSelf().path() + " : I end in RoundRobin");
                }
            }

            backList = null;
            roundRobin = null;
        }

        /**
         * 一次移位完成
         * 父节点接收到消息
         */
        else if(message instanceof RoundRobinProcessDone) {
            roundRobinProcessNumber++;

            //完成了一次移位操作
            if(roundRobinProcessNumber == roundRobinWorkerNumber) {
                roundRobinProcessNumber = 0;
                roundRobinMoveCounter++;
                log.info(getSelf().path() + " : A RoundRobin move step: " + roundRobinMoveCounter + " is done!!!");

                //如果一次sweep完成,则查询它是否已经全局正交化
                if(roundRobinMoveCounter == roundRobinWorkerNumber*2 -1) {
                    log.info(getSelf().path() + " : A Sweep is Done!!! ");
                    //询问一次所有的块是否都已经正交化
                    //如果均已正交化，则结束RoundRobin过程
                    //否则进入下一次sweep过程
                    roundRobinMoveCounter = 0;
                    for(ActorRef actor : childrenList)
                        actor.tell(new QueryConverged(),getSelf());
                    //将sweep的次数加１
                    sweepCounter++;
                    //当sweep的次数超出上限，则显示分解失败，返回当前各个块的结果
                    if(sweepCounter > maxStep) {
                        for(ActorRef actor : childrenList)
                            actor.tell(new RetrieveBlocks(),getSelf());
                        log.error( getSelf().path() + " : iteration exceed the max steps and forced quit");
                    }
                    log.info( getSelf().path() + " : In RoundRobinProcessDone sweep count " + sweepCounter);
                }
                //如果一次sweep还未完成，则对移位后的数据进行正交化处理
                else {
                    for(ActorRef actor : childrenList)
                        actor.tell(new OutterOrth(),getSelf());
                }
            }
        }


        /**
         * 接收到块间正交的消息
         * 子节点接收到消息
         */
        else if(message instanceof InnerOrth) {
            //各个分块内部做块间正交化
            updataListBack = null;
            dndataListBack = null;
            for(int i : filterList) {
                hosvdUtils.doIOrth(updataList.get(0).get(i), dndataList.get(0).get(i),
                        updataList.get(1).get(i), dndataList.get(1).get(i),
                        allConverged, i, TotalTol);
            }

            //告诉父节点块间正交已经完成
            parent.tell(new InnerOrthDone(), getSelf());
        }

        /**
         *块内正交完成
         * 父节点接收到消息
         */
        else if(message instanceof InnerOrthDone) {
            innerOrthCounter++;
            if(innerOrthCounter == roundRobinWorkerNumber) {
                log.info( getSelf().path() + " : InnerOrthDone !!!");
                innerOrthCounter = 0;
                for(ActorRef actor : childrenList) {
                    actor.tell(new RoundRobinBackUp(),getSelf());
                }
            }
        }

        /**
         * 接收到块间正交的消息
         * 子节点接收到消息
         */
        else if(message instanceof OutterOrth) {
            updataListBack = null;
            dndataListBack = null;
            //各个分块内部做块间正交化
            for(int i : filterList) {
                hosvdUtils.doOOrth(updataList.get(0).get(i),dndataList.get(0).get(i),
                        updataList.get(1).get(i),dndataList.get(1).get(i),
                        allConverged,i,TotalTol);
            }

            //告诉父节点块间正交已经完成
            parent.tell(new OutterOrthDone(), getSelf());
        }

        /**
         *块间正交完成
         * 父节点接收到消息
         */
        else if(message instanceof OutterOrthDone) {
            outterOrthCounter++;
            if(outterOrthCounter == roundRobinWorkerNumber) {
                log.info(getSelf().path() + " : OutterOrthDone !!!");
                outterOrthCounter = 0;
                for(ActorRef actor : childrenList) {
                    actor.tell(new RoundRobinBackUp(),getSelf());
                }
            }
        }


        /**
         * 子节点接收到消息
         */
        else if(message instanceof QueryConverged) {
            parent.tell(new QueryConvergedDone(allConverged), getSelf());
        }

        /**
         * 父节点接收到消息
         * 接受到QueryConverged的消息，判断是否已经全局正交化
         */
        else if(message instanceof QueryConvergedDone) {
            QueryConvergedDone queryConvergedDone = (QueryConvergedDone) message;
            queryConvergedCounter++;
            for(int i : filterList) {
                allConverged.set(i,allConverged.get(i) && queryConvergedDone.getConverged().get(i));
            }

            //当所有的worker的converged的信息均已经查询出来
            //去除掉已经正交的展开面，留下还未正交的展开矩阵
            if(queryConvergedCounter == roundRobinWorkerNumber) {
                queryConvergedCounter = 0;
                filterList = new ArrayList<Integer>();

                for(int i = 0; i < allConverged.size(); i++) {
                    //如果模i展开的矩阵还未收敛，则将其加入到filterList中
                    if(!allConverged.get(i)) {
                        filterList.add(i);
                    }
                }

                //当所有的展开均收敛，则返回结果
                if (filterList.size() == 0) {
                    log.info( getSelf().path() + " : All Converged!!!");
                    for(ActorRef actor : childrenList)
                        actor.tell(new RetrieveBlocks(),getSelf());

                } else {
                    log.info( getSelf().path() + " : Not Converged!!! Start next sweep!!!");
                    //否则进入下一轮sweep，重新开始
                    for(int i = 0; i < allConverged.size(); i++) {
                        //复位allConverged的各个标志位
                        allConverged.set(i, true);
                    }

                    //去掉已经收敛的展开面，通知各个worker
                    for(ActorRef actor : childrenList)
                        actor.tell(new UpdateFilterList(filterList),getSelf());

//                    Thread.sleep(500);

                    //通知所有的worker重新开始InnerOrth -> OutterOrth -> RoundRobin
                    //开始新一轮的sweep
                    log.info(getSelf().path() + " : We will do InnerOrth!!!");
                    for(ActorRef actor : childrenList)
                        actor.tell(new InnerOrth(),getSelf());

                }
            }
        }


        /**
         *子节点接收，用于更新收敛判断条件
         */
        else if(message instanceof UpdateFilterList) {
            UpdateFilterList updateFilterList = (UpdateFilterList)message;
            filterList = updateFilterList.getFilterList();
            for(int i : filterList) {
                //更新收敛矩阵
                allConverged.set(i, true);
            }
        }

        /**
         * 子节点接收，将各自收敛的矩阵块上传到父节点上
         */
        else if(message instanceof RetrieveBlocks) {
            log.info(getSelf().path() + " : I will send my data to parents");

//            for (int unfold = 0; unfold < Ranks; unfold++) {
//                hosvdUtils.writeMatrixToFile(updataList.get(0).get(unfold), "result/" + ID + "-upU-mode" + (unfold + 1));
//                hosvdUtils.writeMatrixToFile(updataList.get(1).get(unfold), "result/" + ID + "-upV-mode" + (unfold + 1));
//
//                hosvdUtils.writeMatrixToFile(dndataList.get(0).get(unfold), "result/" + ID + "-dnU-mode" + (unfold + 1));
//                hosvdUtils.writeMatrixToFile(dndataList.get(1).get(unfold), "result/" + ID + "-dnV-mode" + (unfold + 1));
//            }

            parent.tell(new ReturnedBlock(updataList,dndataList),getSelf());
        }

        /**
         * 所有的展开矩阵均已经收敛，则返回所有的矩阵块
         * 父节点接收到消息
         */
        else if(message instanceof ReturnedBlock) {
            log.info(getSelf().path() + " : I in ReturnedBlcok!!!");
            //需要判断是否是按行追加的方式，因为在此处做正交化后,并没有对结果进行还原
            ReturnedBlock returnedBlock = (ReturnedBlock) message;
            //对所有的矩阵结果进行按列追加，然后求出U*SIGMA和V
            for(int i = 0; i < Ranks; i++) {
                Matrix tempU = returnedBlock.getUpdataList().get(0).get(i).addByCol(
                        returnedBlock.getDndataList().get(0).get(i));
                Matrix tempV = returnedBlock.getUpdataList().get(1).get(i).addByCol(
                        returnedBlock.getDndataList().get(1).get(i));

                if(matrixU.get(i).isEmpty()) {
                    matrixU.set(i,tempU);
                    matrixV.set(i,tempV);
                } else {
                    matrixU.set(i,matrixU.get(i).addByCol(tempU));
                    matrixV.set(i,matrixV.get(i).addByCol(tempV));
                }
            }

            returnedBlock = null;

            returnedBlocksCounter++;

            //当接受到所有的RoundRobinWorker的矩阵块时
            if(returnedBlocksCounter == roundRobinWorkerNumber) {
                returnedBlocksCounter = 0;
                //然后对矩阵的结果进行处理
                for(int i = 0; i < Ranks; i++) {
                    Matrix U = matrixU.get(i);
                    Matrix V = matrixV.get(i);
                    //按列追加或者按列穿插的方式
                    if(i != ORDER) {
                        //需要进行标准化的处理，并进行排序、裁剪等操作
                        V = V.normalizeV(U).sliceByCol(0, U.row-1);
                        U = U.normalizeV(U).sliceByCol(0,U.row-1);
                        matrixU.set(i,U);
                        matrixV.set(i,V);
                    } else {
                        //按行追加的情况还需要将U、SIGMA、V再次还原
                        double[] SIGMA = new double[U.col];
                        Matrix tempV = U.normalizeU(SIGMA);
                        U = V.normalizeV(U);
                        U = U.dot(SIGMA);
                        V = tempV;
                        matrixU.set(i,U);
                        matrixV.set(i,V);
                    }
                }

                if (ORDER != 0) {
                    //然后将处理后的数据再次发送到相应的节点上，进行合并、正交化处理工作
                    int destination = ID / Count[ORDER - 1];
                    int serialNumber = ID % Count[ORDER - 1];

                    String address = "../../OrderWorker" + (ORDER - 1) + "/CalculationWorker" + destination;

                    getContext().actorSelection(address)
                            .tell(new IntermediateResult(matrixU, matrixV, serialNumber, tol, dim), self());

                    getContext().stop(getSelf());

                } else {
                    //如果是OrderWorker0上的CalculationWorker上的节点，则处理相关的操作，并最终释放资源
                    log.info(getSelf().path() + " : The process is done!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    //对于每一个按模展开的计算结果求出它的U矩阵
                    for (int unfold = 0; unfold < Ranks; unfold++) {
                        //1.将U进行normalizeU的操作
                        Matrix U = matrixU.get(unfold).normalizeU();
                        String address = "../../";
//                            hosvdUtils.writeMatrixToFile(U, "U-mode" + (unfold + 1));
                        getContext().actorSelection(address).tell(new UData(U, unfold + 1), self());
                    }
                }
            }
        }

    }
}