package hosvd.actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.remote.RemoteScope;
import au.com.bytecode.opencsv.CSVWriter;
import hosvd.datatype.Matrix;
import hosvd.datatype.Tensor3;
import hosvd.message.*;

import java.io.*;
import java.util.*;

/**
 * Created by cxy on 15-10-22.
 */
public class MasterActor extends UntypedActor{
    private int workerNumber = 0;
    private double tol = 0d;
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
    private String destFile = null;

    private long start_time;

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
//        out.print(matrix);
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
    private void writeTolToFile(String filename) throws IOException {
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

        //如果消息为初始化
        if(message instanceof Initialization) {

            start_time = System.currentTimeMillis();
            Initialization initialization = (Initialization)message;
            workerNumber = initialization.getWorkerNumber();
            int startIP = initialization.getStartIP();


            destFile = "result/" + initialization.getDim1() + " * " + initialization.getDim2() + " * " + initialization.getDim3();
            File file = new File(destFile);
            file.mkdir();

            log.info("Tensor initialized with size " + initialization.getDim1() + " * " + initialization.getDim2() + " * " + initialization.getDim3());

            boolean flag = initialization.getFlag_incr();

            //从文件集合中构建张量，张量的大小为Dim1*Dim2*Dim3
//            Tensor3 tensor = Tensor3.setTensor3(initialization.getDim1(),initialization.getDim2(),initialization.getDim3(),"./dataset");
//            Tensor3 tensor = Tensor3.setTensor3(initialization.getDim1(), initialization.getDim2(),initialization.getDim3(),"./dataset",flag);

            Tensor3 tensor = Tensor3.setTensor3(initialization.getDim1(), initialization.getDim2(),initialization.getDim3(),
                    "/home/cxy/IdeaProjects/ClientServer/dataset", false);

//            log.info(tensor.matricization(1) + " ");

            ranks = tensor.getRank();
            tol = Math.pow(tensor.norm(),2) * 1e-15 ;

            for(int i = 0; i < ranks; i++) {
                allConverged.add(true);
                matrixU.add(Matrix.emptyMatrix());
                filterList.add(i);
            }

            //创建子Actor，并将子Actor的引用和地址存放与列表当中
            for(int i = 0; i < workerNumber; i++) {
                String host = "192.168.18.1";
                int start = startIP;
                start += i % workerNumber;
                host += Integer.toString(start);
                Address addr = new Address("akka.tcp", "WorkerSystem", host, 2555);

                ActorRef actor = getContext().actorOf(Props.create(WokerActor.class, i)
                        .withDeploy(new Deploy(new RemoteScope(addr))), "worker" + i);

                String path = actor.path().toString();
                actorList.add(actor);
                actorPathList.add(path);
            }

            //将各个Actor的地址打印出来
            log.info(actorPathList.toString());

            //传给各个worker的数据块信息
            List<Matrix> upDataList = null;
            List<Matrix> dnDataList = null;
            Matrix vUpMatrix = null;
            Matrix vDnMatrix = null;

            //如果不是做增量计算的情况下
            if(!flag) {
                //返回一个单一矩阵（对角线元素全为1），这个应该是作为最终的结果U  ???
                Matrix _matrixV = Matrix.eye(tensor.getX());

                //将张量块切分成均等的块
                List<Tensor3> blockList = tensor.sliceEquallyBy1st(2*workerNumber);
                //将这个单一矩阵按列均分，用于存储结果
                List<Matrix> vList = _matrixV.sliceByColEqually(2*workerNumber);

                for(int i = 0; i < actorList.size(); i++) {

                    upDataList = new ArrayList<>();
                    dnDataList = new ArrayList<>();

                    for(int j = 0; j < ranks; j++) {
                        //如果是模以展开的情况
                        if(j == 0) {
                            upDataList.add(blockList.get(2*i).matricization(j+1).trans());
                            dnDataList.add(blockList.get(2*i+1).matricization(j+1).trans());
                            vUpMatrix = vList.get(2*i);
                            vDnMatrix = vList.get(2*i+1);
                        } else {
                            upDataList.add(blockList.get(2*i).matricization(j+1));
                            dnDataList.add(blockList.get(2*i+1).matricization(j+1));
                        }
                    }

                    //让每个worker的两个小块分别做块内正交
                    actorList.get(i).tell(
                            new InitialData(upDataList,dnDataList,vUpMatrix,vDnMatrix,tol,actorPathList,ranks)
                            ,getSelf());

                    actorList.get(i).tell(new InnerOrth(), getSelf());
                }
            }

            //如果是增量方式
            else {
                //1.从文件中读取出上一次的中间结果matrixU1 matrixU2 matrixU3 和 matrixV
                List<Matrix> _matrixU = new ArrayList<>();
                for(int i = 0; i < ranks; i++) {
                    _matrixU.add(Matrix.readMatrix("./tmp/matrixU" + (i+1)));
                }
                Matrix _matrixV = Matrix.readMatrix("./tmp/matrixV");
                tol += readTol("./tmp/Tol.txt");

                log.info(tol + " ");

                //2.将增量数据分到到各个部分中
                //初始化全局的左奇异矩阵，最开始为单位向量
                Matrix incrMatrixV = Matrix.eye(_matrixV.row + initialization.getDim1());
                //将左奇异矩阵的左上角用matrixV代替，对角线下角为１
                for(int i = 0; i < _matrixV.row; i++) {
                    for(int j = 0; j < _matrixV.col; j++) {
                        incrMatrixV.set(i,j,_matrixV.get(i,j));
                    }
                }
                _matrixU.set(0, _matrixU.get(0).addByCol(tensor.matricization(1).trans()));
                for(int i = 1; i < ranks; i++) {
                    _matrixU.set(i,_matrixU.get(i).addByCol(tensor.matricization(i+1)));
                }

                //3.将各个矩阵划分成2×workerNumber个块
                List<Matrix> vList = incrMatrixV.sliceByColEqually(2*workerNumber);
                ArrayList<List<Matrix>> blockList = new ArrayList<List<Matrix>>();
                for(Matrix m : _matrixU)
                    blockList.add(m.sliceByColEqually(2 * workerNumber));

                for(int i = 0; i < actorList.size(); i++) {
                    upDataList = new ArrayList<>();
                    dnDataList = new ArrayList<>();

                    for(int j = 0; j < ranks; j++) {
                        upDataList.add(blockList.get(j).get(2*i));
                        dnDataList.add(blockList.get(j).get(2*i+1));
                    }

                    vUpMatrix = vList.get(2*i);
                    vDnMatrix = vList.get(2*i+1);

                    //让每个worker的两个小块分别做块内正交
                    actorList.get(i).tell(
                            new InitialData(upDataList,dnDataList,vUpMatrix,vDnMatrix,tol,actorPathList,ranks)
                            ,getSelf());

                    actorList.get(i).tell(new InnerOrth(), getSelf());
                }
            }

        }

        /*******************************test***********************************/
        /**********************************************************************/
        /**********************************************************************/
        /**********************************************************************/
        //多阶同时分布式的情况
        else if(message instanceof MultiInitialization) {
            MultiInitialization initializationMulti = (MultiInitialization)message;
            workerNumber = initializationMulti.getWorkerNumber();

            //构建20*20*5的张量,并切成上下两块，对上面和下面的张量块各自做模一展开，并各自做正交化，按列追加的方式
            //按照按列追加的方式做正交化后，分别生成了U和V两个矩阵，然后将U1和U2转置并按列追加，再以按列追加的方式进行正交化，
            //记录下生成的V，看V是否与原来的HOSVD分解方式相同??????????????????????????????????????????????????????
            Tensor3 tensor = Tensor3.setTensor3(initializationMulti.getDim1(), initializationMulti.getDim2(),initializationMulti.getDim3(),"./dataset",false);

            ranks = tensor.getRank();
            tol = Math.pow(tensor.norm(),2) * 1e-15 ;

            for(int i = 0; i < ranks; i++) {
                allConverged.add(true);
                matrixU.add(Matrix.emptyMatrix());
                filterList.add(i);
            }

            //创建子Actor，并将子Actor的引用和地址存放与列表当中
            for(int i = 0; i < workerNumber; i++) {
                ActorRef actor = getContext().actorOf(Props.create(WokerActor.class,i),"worker"+i);
                String path = actor.path().toString();
                actorList.add(actor);
                actorPathList.add(path);
            }

            //将各个Actor的地址打印出来
            log.info(actorPathList.toString());

            //传给各个worker的数据块信息
            List<Matrix> upDataList = null;
            List<Matrix> dnDataList = null;
            Matrix vUpMatrix = null;
            Matrix vDnMatrix = null;


        }
        /**********************************************************************/
        /**********************************************************************/
        /**********************************************************************/


        //块内正交完成
        else if(message instanceof InnerOrthDone) {
            firstProcessNumber++;
            if(firstProcessNumber == workerNumber) {
                firstProcessNumber = 0;
                for(ActorRef actor : actorList) {
                    actor.tell(new OutterOrth(),getSelf());
                }
//                log.info("Master: InnerOrth is Done");
            }
        }

        //块间正交完成
        else if(message instanceof OutterOrthDone) {
            outterOrthNumber++;
            if(outterOrthNumber == workerNumber) {
                outterOrthNumber = 0;
                ActorRef head = actorList.get(0);
                head.tell(new RoundRobinProcess(),getSelf());
//                log.info("Master: OutterOrth is Done");
            }
        }

        //一次sweep中的一次位移完成
        else if(message instanceof RoundRobinProcessDone) {
            iteration++;
            //一次sweep共有2*workerNumber-1 次位移
            if(iteration == workerNumber*2-1) {

                //询问一次所有的块是否都已经正交化
                //如果均已正交化，则结束RoundRobin过程
                //否则进入下一次sweep过程
                for(ActorRef actor : actorList)
                    actor.tell(new QueryConverged(),getSelf());

                iteration = 0;
                //将sweep的次数加１
                sweepCount++;
                //剩余的最多sweep的次数减１
                maxSteps--;
                //当sweep的次数超出上限，则显示分解失败，返回当前各个块的结果
                if(maxSteps == 0) {
                    for(ActorRef actor : actorList)
                        actor.tell(new RetrieveBlocks(),getSelf());
                    log.error("iteration exceed the max steps and forced quit");
                }
                log.info("In RoundRobinProcessDone sweep count " + sweepCount);
            } else {
                for(ActorRef actor : actorList)
                    actor.tell(new OutterOrth(),getSelf());
            }
        }

        //询问一次所有的块是否都已经正交化
        //如果均已正交化，则结束RoundRobin过程
        //否则进入下一次sweep过程
        else if(message instanceof IterationEnd) {
            IterationEnd iterationEnd = (IterationEnd)message;
            secondProcessNumber++;
            for(int i : filterList) {
                allConverged.set(i,allConverged.get(i) && iterationEnd.getConverged().get(i));
            }

            //当所有的worker的converged的信息均已经查询出来
            //去除掉已经正交的展开面，留下还未正交的展开矩阵
            if(secondProcessNumber == workerNumber) {
                secondProcessNumber = 0;
                filterList = new ArrayList<Integer>();

                for(int i = 0; i < allConverged.size(); i++) {
                    //如果模i展开的矩阵还未收敛，则将其加入到filterList中
                    if(!allConverged.get(i)) {
                        filterList.add(i);
                    }
                }

                //当所有的展开均收敛，则返回结果
                if (filterList.size() == 0) {
                    for(ActorRef actor : actorList)
                        actor.tell(new RetrieveBlocks(),getSelf());

                } else {

                    //否则进入下一轮sweep，重新开始
                    for(int i = 0; i < allConverged.size(); i++)
                        //复位allConverged的各个标志位
                        allConverged.set(i,true);

                    //去掉已经收敛的展开面，通知各个worker
                    for(ActorRef actor : actorList)
                        actor.tell(new UpdateFilterList(filterList),getSelf());

                    //通知所有的worker重新开始InnerOrth -> OutterOrth -> RoundRobin
                    //开始新一轮的sweep
                    for(ActorRef actor : actorList)
                        actor.tell(new InnerOrth(),getSelf());

                }
            }
        }

        //所有的展开矩阵均已经收敛，则返回所有的矩阵块
        else if(message instanceof ReturnedBlock) {
            ReturnedBlock returnedBlock = (ReturnedBlock)message;

            //接收所有的按摸展开的矩阵
            for(int i = 0; i < ranks; i++) {
                if(matrixU.get(i).isEmpty()) {
                    matrixU.set(i,returnedBlock.getUp().get(i));
                    matrixU.set(i,matrixU.get(i).addByCol(returnedBlock.getDn().get(i)));
                } else {
                    matrixU.set(i,matrixU.get(i).addByCol(returnedBlock.getUp().get(i)));
                    matrixU.set(i,matrixU.get(i).addByCol(returnedBlock.getDn().get(i)));
                }
            }

            if(matrixV.isEmpty()) {
                matrixV = returnedBlock.getvUp();
                matrixV = matrixV.addByCol(returnedBlock.getvDn());
            } else {
                matrixV = matrixV.addByCol(returnedBlock.getvUp());
                matrixV = matrixV.addByCol(returnedBlock.getvDn());
            }

            lastProcessNumber++;

            //当接收到所有的worker的矩阵块时

            if(lastProcessNumber == workerNumber) {
                for(ActorRef actor : actorList)
                    getContext().stop(actor);
                log.info("Sweep count is " + sweepCount);
                log.info("matrixU0 :" + matrixU.get(0).row + " " + matrixU.get(0).col);
                log.info("matrixV :" + matrixV.row + " " + matrixV.col);

                long duration = System.currentTimeMillis() - start_time;

                log.info("time is : " + duration);

//                //将matrixU和matrixV分别写入到文件中,用于增量计算
//                for(int i = 0; i < matrixU.size(); i++)
//                    writeMatrixToFile(matrixU.get(i),"./result/matrixU"+(i+1));
//                writeMatrixToFile(matrixV,"./result/matrixV");

                //先写入按模２－Ｎ展开的矩阵
                for(int i = 1; i < matrixU.size(); i++)
                    writeMatrixToFile(matrixU.get(i).normalizeU().sliceByCol(0, matrixU.get(i).row-1), destFile+"/matrixU-"+(i+1));

                //然后写入按模１展开的矩阵，matrixU-1为matrixV,即转置的结果
                //需要将matrixV的结果按照matrixU.get(0)的norm大小进行排序
//                log.info(matrixV.row + " " + matrixV.col );
                writeMatrixToFile(matrixV.normalizeV(matrixU.get(0)), destFile+"/matrixU-1");

                File path = new File(destFile+"/times.txt");
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path)));
                out.println(duration);
                out.close();


                //写入tol信息、张量的各个阶的大小
//                writeTolToFile("./result/Tol.txt");


                log.info("Write matrixU to file OK");

                mainThread.tell("Success",mainThread);
            }
        }

        //询问是否完成
        else if(message instanceof HasDone) {
            mainThread = getSender();
        }

        else {
            unhandled(message);
        }
    }

}
