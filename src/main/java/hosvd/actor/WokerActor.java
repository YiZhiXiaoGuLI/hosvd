package hosvd.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import hosvd.datatype.Matrix;
import hosvd.datatype.Vec;
import hosvd.message.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cxy on 15-10-22.
 */
public class WokerActor extends UntypedActor{
    private int id;
    private List<Matrix> upDataList = new ArrayList<>();
    private List<Matrix> dnDataList = new ArrayList<>();
    private Matrix vUpMatrix = Matrix.emptyMatrix();
    private Matrix vDnMatrix = Matrix.emptyMatrix();
    private List<Boolean> converged = new ArrayList<>();
    private double tol = 0.0;
    private int ranks = 0;
    private List<Integer> filterList = new ArrayList<>();
    private int workerNumber = 0;
    private ActorRef master = null;
    private List<String> workerPathList = new ArrayList<>();

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public WokerActor(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public void onReceive(Object message) throws Exception {

        //如果是初始化数据message
        if(message instanceof InitialData) {
            InitialData initialData = (InitialData)message;
            master = getSender();
            ranks = initialData.getRanks();

            //初始化
            for(int i = 0; i < ranks; i++) {
                converged.add(true);
                //记录按模展开的矩阵的个数即张量的阶
                filterList.add(i);
            }

            //初始化沿各模展开的矩阵
            upDataList = initialData.getUpDataList();
            dnDataList = initialData.getDnDataList();

            //初始化两个用于存放结果V的矩阵，用于模一展开时的计算
            vUpMatrix = initialData.getvUp();
            vDnMatrix = initialData.getvDn();
            //初始化各类数据
            tol = initialData.getTol();
            workerNumber = initialData.getActorPathList().size();
            workerPathList = initialData.getActorPathList();

            log.info("Worker: " + getId() + " received initial data!");
        }


        //接收到做块内正交的通知
        else if(message instanceof InnerOrth) {
//            log.info("Worker " + id + " :InnerOrth start");

            //这个三阶张量有多个阶,用于记录按照每一模展开的矩阵是否收敛
            for(int i : filterList) {
                //更新收敛矩阵
                converged.set(i,true);
            }

            //模１、模２、模３以此展开，并求它的块内正交结果
            for(int i : filterList) {
                //如果是按照模１展开
                if(i == 0) {
                    List<Matrix> res1 = innerOrthForDim1(upDataList.get(i), vUpMatrix, i + 1);
                    upDataList.set(i,res1.get(0));
                    vUpMatrix = res1.get(1);
                    List<Matrix> res2 = innerOrthForDim1(dnDataList.get(i),vDnMatrix,i+1);
                    dnDataList.set(i,res2.get(0));
                    vDnMatrix = res2.get(1);
                } else {
                    //如果按照其他模进行展开，求快内正交结果
                    upDataList.set(i,innerOrth(upDataList.get(i), i + 1));
                    dnDataList.set(i,innerOrth(dnDataList.get(i), i + 1));
                }
            }
//            log.info("Worker " + id + " : InnerOrth end!");
//            log.info(upDataList.toString());
            master.tell(new InnerOrthDone(), getSelf());
        }

        //接收到块间正交的消息
        else if(message instanceof OutterOrth) {
//            log.info("Worker " + id + " :OutterOrth start");
            for(int i : filterList) {
                //如果是按模一展开
                if(i == 0) {
                    List<Matrix> res1 = outterOrthForDim1(upDataList.get(i), dnDataList.get(i), vUpMatrix, vDnMatrix, i+1);
                    upDataList.set(i,res1.get(0));
                    dnDataList.set(i,res1.get(1));
                    vUpMatrix = res1.get(2);
                    vDnMatrix = res1.get(3);
                } else {
                    List<Matrix> res2 = outterOrth(upDataList.get(i),dnDataList.get(i),i+1);
                    upDataList.set(i,res2.get(0));
                    dnDataList.set(i,res2.get(1));
                }
            }
//            log.info("In worker " + id + " OutterOrth end!");
            master.tell(new OutterOrthDone(), getSelf());
        }

        else if(message instanceof RoundRobinProcess) {
//            log.info("Worker " + id + " :RoundRobin process start");
            ActorSelection worker2 = getContext().actorSelection(workerPathList.get(1));
            List<Matrix> tempList = new ArrayList<Matrix>();

            int count = 0;
            for(int i : filterList) {
                tempList.add(dnDataList.get(i));
                if(i == 0)
                    count = 1;
            }
            //filterList中只存在按模１展开的矩阵
            if(count == 1)
                worker2.tell(new RoundRobin(tempList,vDnMatrix,"dn"), getSelf());
            else
                worker2.tell(new RoundRobin(tempList,Matrix.emptyMatrix(),"dn"), getSelf());

//            log.info("In worker " + id + " RoundRobinProcess end!");

        }

        else if(message instanceof RoundRobin) {

            RoundRobin roundRobin = (RoundRobin)message;

            switch (roundRobin.getFlag()) {
                case "dn" :

                    //当遍历完所有的worker的时候，即dnDataList都已经遍历完，则开始反向传播，遍历upDataList
                    //id的取值范围为[0,workerNumber-1],id==workerNumber-1 意思为遍历到最后一个worker
                    if(id == workerNumber-1) {

//                        log.info("I am in the worker" + id + ". I will backwards");
                        //找到倒数第二个worker，作为反向传播的第一个落脚点
                        ActorSelection worker = getContext().actorSelection(workerPathList.get(workerNumber - 2));
                        List<Matrix> tempList = new ArrayList<Matrix>();

                        //将dnDataList的内容传送到　倒数第二个 worker上的 upDataList之上
                        int count = 0;
                        for(int i : filterList) {
                            tempList.add(dnDataList.get(i));
                            if(i == 0)
                                count = 1;
                        }

                        //将dnDataList的内容传送到　倒数第二个 worker上的 upDataList之上
                        if(count == 1)
                            worker.tell(new RoundRobin(tempList,vDnMatrix,"up"),getSelf());
                        else
                            worker.tell(new RoundRobin(tempList,Matrix.emptyMatrix(),"up"),getSelf());

                        //用发送过来的数据块覆盖掉本地的数据块
//                        for(int i : filterList) {
                        for(int i = 0; i < filterList.size(); i++) {
                            dnDataList.set(filterList.get(i),roundRobin.getData().get(i));
                            if(filterList.get(i) == 0) {
                                vDnMatrix = roundRobin.getvMatrix();
                            }
                        }
                    } else {
                        //获取下一个接着的worker的地址
//                        log.info("workerNumber " + workerNumber + "");
//                        log.info("Forwards: Current id is " + id);
                        ActorSelection worker = getContext().actorSelection(workerPathList.get(id + 1));
                        List<Matrix> tempList = new ArrayList<Matrix>();

                        int count = 0;
                        for(int i : filterList) {
                            tempList.add(dnDataList.get(i));
                            if(i == 0)
                                count = 1;
                        }

                        //将dnDatalist发送给下一个worker,即每个worker中的　下半部分数据块
                        if(count == 1)
                            worker.tell(new RoundRobin(tempList,vDnMatrix,"dn"),getSelf());
                        else
                            worker.tell(new RoundRobin(tempList,Matrix.emptyMatrix(),"dn"),getSelf());

                        //用发送过来的数据块覆盖掉本地的数据块
//                        for(int i : filterList) {
                        for(int i = 0; i < filterList.size(); i++) {
                            dnDataList.set(filterList.get(i),roundRobin.getData().get(i));
                            if(filterList.get(i) == 0) {
                                vDnMatrix = roundRobin.getvMatrix();
                            }
                        }
                    }
                    break;

                case "up":

                    //当反向传播，返回到第一个worker时候，一次RoundRobin循环的一次位移动作完成
                    if(id == 0) {

//                        log.info("I am in the worker" + id + ". A step of a sweep is end");

                        //在第一个worker里，先用upDataList覆盖掉dnDataList
                        //然后用发送过来的数据块覆盖掉本地的数据块
//                        for(int i : filterList) {
                        for(int i = 0; i < filterList.size(); i++) {
                            dnDataList.set(filterList.get(i),upDataList.get(filterList.get(i)));
                            upDataList.set(filterList.get(i),roundRobin.getData().get(i));
                            if(filterList.get(i) == 0) {
                                vDnMatrix = vUpMatrix;
                                vUpMatrix = roundRobin.getvMatrix();
                            }
                        }

                        //当一次位移结束后，通知master，通知每个worker进行一次块间正交
                        master.tell(new RoundRobinProcessDone(),getSelf());

                    } else {
                        //获取下一个接着的worker的地址
//                        log.info("workerNumber " + workerNumber + "");
//                        log.info("Backwards: Current id is " + id);
                        ActorSelection worker = getContext().actorSelection(workerPathList.get(id - 1));
                        List<Matrix> tempList = new ArrayList<Matrix>();

                        int count = 0;
                        for(int i : filterList) {
                            tempList.add(upDataList.get(i));
                            if(i == 0)
                                count = 1;
                        }

                        //将dnDatalist发送给下一个worker,即每个worker中的　下半部分数据块
                        if(count == 1)
                            worker.tell(new RoundRobin(tempList,vUpMatrix,"up"),getSelf());
                        else
                            worker.tell(new RoundRobin(tempList,Matrix.emptyMatrix(),"up"),getSelf());

                        //用发送过来的数据块覆盖掉本地的数据块
//                        for(int i : filterList) {
                        for(int i = 0; i < filterList.size(); i++) {
                            upDataList.set(filterList.get(i),roundRobin.getData().get(i));
                            if(filterList.get(i) == 0) {
                                vUpMatrix = roundRobin.getvMatrix();
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        //将各个worker的各个展开矩阵的converged信息返回给master,用于判断全局是否收敛
        else if(message instanceof QueryConverged) {
            master.tell(new IterationEnd(converged),getSelf());
        }

        //进入一轮新的sweep之前，需要更新一次filterList,以免进行不必要的展开面的正交化求解
        else if(message instanceof UpdateFilterList) {
            UpdateFilterList updateFilterList = (UpdateFilterList)message;
            filterList = updateFilterList.getFilterList();
        }

        //当所有展开面都已经收敛时，则返回各个展开面的结果
        else if(message instanceof RetrieveBlocks) {
            master.tell(new ReturnedBlock(upDataList,dnDataList,vUpMatrix,vDnMatrix),getSelf());
        }

        else
            unhandled(message);
    }




    //其余模块内正交处理,即除开模以展开的
    public Matrix innerOrth(Matrix block, int dim) {
        Matrix matrix = block;
        for(int i = 0; i < block.col -1; i++) {
            for (int j = i + 1; j < block.col; j++) {
                Vec di = block.getCol(i);
                Vec dj = block.getCol(j);
                double dii = di.dot(di);
                double dij = di.dot(dj);
                double djj = dj.dot(dj);

                //如果存在两列之间的正交化结果大于误差值，则判定为按照该模展开的矩阵未收敛
                if (Math.abs(dij) > tol) {
                    converged.set(dim - 1, false);

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
        return matrix;
    }

    //按模一进行块内正交时特殊处理
    public List<Matrix> innerOrthForDim1(Matrix block, Matrix v, int dim) {
        List<Matrix> result = new ArrayList<Matrix>(2);
        Matrix uMatrix = block;
        Matrix vMatrix = v;

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
                    converged.set(dim-1,false);

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
        result.add(uMatrix);
        result.add(vMatrix);
        return result;
    }

    public List<Matrix> outterOrth(Matrix block1, Matrix block2, int dim) {
        List<Matrix> result = new ArrayList<Matrix>(2);
        Matrix matrix1 = block1;
        Matrix matrix2 = block2;

        for(int i = 0; i < block1.col; i++) {
            for (int j = 0; j < block2.col; j++) {
                Vec di = block1.getCol(i);
                Vec dj = block2.getCol(j);
                double dii = di.dot(di);
                double dij = di.dot(dj);
                double djj = dj.dot(dj);

                //如果存在两列之间的正交化结果大于误差值，则判定为按照该模展开的矩阵未收敛
                if (Math.abs(dij) > tol) {
                    converged.set(dim - 1, false);

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
                }
            }
        }

        result.add(block1);
        result.add(block2);
        return result;
    }

    public List<Matrix> outterOrthForDim1(Matrix block1, Matrix block2, Matrix v1, Matrix v2, int dim) {
        List<Matrix> result = new ArrayList<Matrix>(4);
        Matrix matrix1 = block1;
        Matrix matrix2 = block2;

        for(int i = 0; i < block1.col; i++) {
            for (int j = 0; j < block2.col; j++) {
                Vec di = block1.getCol(i);
                Vec dj = block2.getCol(j);
                double dii = di.dot(di);
                double dij = di.dot(dj);
                double djj = dj.dot(dj);

                //如果存在两列之间的正交化结果大于误差值，则判定为按照该模展开的矩阵未收敛
                if (Math.abs(dij) > tol) {
                    converged.set(dim - 1, false);

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

        result.add(block1);
        result.add(block2);
        result.add(v1);
        result.add(v2);
        return result;
    }
}
