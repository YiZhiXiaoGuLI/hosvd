package hosvd.HighOrderActor;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import hosvd.RoundRobinMessage.*;
import hosvd.datatype.Matrix;
import hosvd.datatype.Vec;
import hosvd.utils.HosvdUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cxy on 16-1-9.
 */
public class RoundRobinWorker extends UntypedActor{
    private final int id;
    private final double tol;
    private final int ranks;
//    private final HosvdUtils hosvdUtils;

    private List<Matrix> upUDataList = new ArrayList<>();
    private List<Matrix> dnUDataList = new ArrayList<>();
    private List<Matrix> upVDataList = new ArrayList<>();
    private List<Matrix> dnVDataList = new ArrayList<>();

    //备份缓冲区
    private List<Matrix> upUBackBuffer;
    private List<Matrix> dnUBackBuffer;
    private List<Matrix> upVBackBuffer;
    private List<Matrix> dnVBackBuffer;

    private List<Boolean> converged = new ArrayList<>();
    private List<Integer> filterList = new ArrayList<>();
    private int workerNumber = 0;
    private int receiveCounter = 0; //用于统计在RoundRobin过程中的节点接收到了几次数据
    private List<String> workerPathList = new ArrayList<>();
    private ActorRef parent = null;
    private int sweepCount = 0;

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public RoundRobinWorker(int id, int ranks, double tol) {
        this.id = id;
        this.ranks = ranks;
        this.tol = tol;
//        hosvdUtils = new HosvdUtils(log);
    }

    public int getId() {
        return id;
    }

    @Override
    public void postStop() throws Exception {
        log.info("RoundRobinWorker" + id +": has finished!!!");
    }

    @Override
    public void onReceive(Object message) throws Exception {
//
//        /**
//         *如果是初始化数据message
//         */
//        if(message instanceof InitialData) {
//            InitialData initialData = (InitialData)message;
//            //初始化，是几阶张量，就有几个展开矩阵需要做正交化处理
//            for(int i = 0; i < ranks; i++) {
//                converged.add(true);
//                //记录按模展开的矩阵的个数即张量的阶
//                filterList.add(i);
//            }
//
//            //初始化沿各模展开的矩阵
//            upUDataList = initialData.getUpUDataList();
//            dnUDataList = initialData.getDnUDataList();
//            upVDataList = initialData.getUpVDataList();
//            dnVDataList = initialData.getDnVDataList();
//            workerPathList = initialData.getActorPathList();
//            workerNumber = workerPathList.size();
//            parent = getContext().parent();
//            log.info("RoundRobinWorker" + getId() + ": received initial data!");
////            log.info("upDataList :" + upUDataList.size() + " row :" + upUDataList.get(0).row + " col :" + upUDataList.get(0).col);
////            log.info("upDataList :" + upUDataList.size() + " row :" + upUDataList.get(1).row + " col :" + upUDataList.get(1).col);
//        }
//
//        /**
//         *接收到做块内正交的通知
//         */
//        else if(message instanceof InnerOrth) {
//            sweepCount++;
//            log.info("RoundRobinWorker" + id + " : starts roundrobin " + sweepCount + " V row is : " + upVDataList.get(0).row);
//            //各个分块内部做块内正交化
//            for(int i : filterList) {
//                innerOrth(upUDataList.get(i), dnUDataList.get(i),
//                        upVDataList.get(i), dnVDataList.get(i),
//                        i);
//            }
//
//            //告诉父节点块内正交已经完成
//            parent.tell(new InnerOrthDone(), getSelf());
//        }
//
//        /**
//         * 接收到块间正交的消息
//         */
//        else if(message instanceof OutterOrth) {
//            //各个分块内部做块间正交化
//            for(int i : filterList) {
//                outterOrth(upUDataList.get(i), dnUDataList.get(i),
//                        upVDataList.get(i), dnVDataList.get(i),
//                        i);
//            }
//
//            //告诉父节点块间正交已经完成
//            parent.tell(new OutterOrthDone(), getSelf());
//        }
//
//        /**
//         * 一次RoundRobinProcess的过程有：
//         *
//         */
//
//
//        /**
//         * Step1: RoundRobinBackUp
//         * 将要发送的数据备份到缓冲区中
//          */
//        else if(message instanceof RoundRobinBackUp) {
//
//            //重新初始化缓冲区，覆盖掉之前的缓冲区
//            upUBackBuffer = new ArrayList<>();
//            upVBackBuffer = new ArrayList<>();
//            dnUBackBuffer = new ArrayList<>();
//            dnVBackBuffer = new ArrayList<>();
//
//            //如果它是最后的一个节点，则只备份dnDataList,upDataList不用进行备份
//            //因为最后一个节点的upDataList在整个RoundRobinProcess中保持不变
//            //其他的节点的upDataList与dnDataList都需要进行备份保存工作
//            //只备份还未正交化的数据块,已正交化的数据不用备份到发送缓冲区中
//          for(int i : filterList) {
//              if(id != workerNumber-1) {
//                  upUBackBuffer.add(upUDataList.get(i));
//                  upVBackBuffer.add(upVDataList.get(i));
//              }
//              dnUBackBuffer.add(dnUDataList.get(i));
//              dnVBackBuffer.add(dnVDataList.get(i));
//          }
//
//            //备份完成之后，通知管理节点，备份成功
//            parent.tell(new RoundRobinBackUpDone(), getSelf());
//        }
//
//        /**
//         * 将发送缓冲区中的数据发送到相应节点的相应位置，完成一次RoundRobinProcess
//         */
//        else if(message instanceof RoundRobinSendBuffer) {
////            log.info("I received RoundRobinSendBuffer!!!");
//
//            if(id == 0) {
//                ActorSelection nextLocation = getContext().actorSelection(workerPathList.get(1));
//
//                nextLocation.tell(new RoundRobin(dnUBackBuffer,dnVBackBuffer,"dn"),getSelf());
//                getSelf().tell(new RoundRobin(upUBackBuffer,upVBackBuffer,"dn"),getSelf());
//
//            } else if(id == workerNumber-1) {
//                //将dnBackBuffer的数据发送到preLocation
//                ActorSelection preLocation = getContext().actorSelection(workerPathList.get(workerNumber-2));
//                preLocation.tell(new RoundRobin(dnUBackBuffer,dnVBackBuffer,"up"),getSelf());
//
//            }else {
//                //依次将upBackBuffer的数据发送到preLocation
//                //依次将dnBackBuffer的数据发送到nextLocation
//                ActorSelection nextLocation = getContext().actorSelection(workerPathList.get(id+1));
//                ActorSelection preLocation = getContext().actorSelection(workerPathList.get(id-1));
//
//                nextLocation.tell(new RoundRobin(dnUBackBuffer,dnVBackBuffer,"dn"),getSelf());
//                preLocation.tell(new RoundRobin(upUBackBuffer,upVBackBuffer,"up"),getSelf());
//            }
//        }
//
//
//        /**
//         * 进行一次数据块的传输
//         */
//        else if(message instanceof RoundRobin) {
////            log.info("RoundRobinWorker" + id + ": recveived RoundRobin message!!!");
//            RoundRobin roundRobin = (RoundRobin)message;
//            List<Matrix> ulist = roundRobin.getUdata();
//            List<Matrix> vlist = roundRobin.getVdata();
//
//            receiveCounter++;
//
//            switch (roundRobin.getFlag()) {
//                case "dn" :
//                    //用接收的数据覆盖dnDataList
//                    for(int i = 0; i < filterList.size(); i++) {
//                        dnUDataList.set(filterList.get(i),ulist.get(i));
//                        dnVDataList.set(filterList.get(i),vlist.get(i));
//                    }
//                    break;
//
//                case "up":
//                    //用接收的数据覆盖dnDataList
//                    for(int i = 0; i < filterList.size(); i++) {
//                        upUDataList.set(filterList.get(i),ulist.get(i));
//                        upVDataList.set(filterList.get(i),vlist.get(i));
//                    }
//                    break;
//                default:
//                    break;
//            }
//
//            //在完成了数据块的覆盖后，判断是否已经全部接收成功
//            if(id == (workerNumber-1)) {
//                receiveCounter = 0;
//                parent.tell(new RoundRobinProcessDone(), getSelf());
//            }else {
//                if(receiveCounter == 2) {
//                    receiveCounter = 0;
//                    parent.tell(new RoundRobinProcessDone(), getSelf());
//                }
//            }
//        }
//
//        /**
//         *将各个worker的各个展开矩阵的converged信息返回给master,用于判断全局是否收敛
//         */
//        else if(message instanceof QueryConverged) {
//            getContext().parent().tell(new QueryConvergedDone(converged), getSelf());
//        }
//
//        /**
//         * 进入一轮新的sweep之前，需要更新一次filterList,以免进行不必要的展开面的正交化求解
//         */
//        else if(message instanceof UpdateFilterList) {
//            UpdateFilterList updateFilterList = (UpdateFilterList)message;
//            filterList = updateFilterList.getFilterList();
//            for(int i : filterList) {
//                //更新收敛矩阵
//                converged.set(i,true);
//            }
//        }
//
//        /**
//         * 当所有展开面都已经收敛时，则返回各个展开面的结果
//         */
//        else if(message instanceof RetrieveBlocks) {
//            getContext().parent().tell(new ReturnedBlock(upUDataList,dnUDataList,upVDataList,dnVDataList),getSelf());
//        }
//
//        else
//            unhandled(message);
//
    }


    //方法区
    /****************************************************************************************
     *innerOrth
     */
    private void outterOrth(Matrix block1, Matrix block2, Matrix v1, Matrix v2, int mode) {

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

    /****************************************************************************************
     *outterOrth
     */
    private void innerOrth(Matrix block1, Matrix block2, Matrix v1, Matrix v2, int mode) {
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

}
