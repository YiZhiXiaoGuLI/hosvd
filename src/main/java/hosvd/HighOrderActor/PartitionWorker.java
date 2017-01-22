package hosvd.HighOrderActor;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import au.com.bytecode.opencsv.CSVWriter;
import hosvd.datatype.Matrix;
import hosvd.datatype.Tensor;
import hosvd.datatype.Tensor3;
import hosvd.message.HasDone;
import hosvd.multimessage.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by cxy on 15-12-21.
 */

/**
 * 用于张量的切块、分发处理
 * 它是整个系统物理上的主节点
 * 还可以做故障失效与节点状态检测的处理
 */
public class PartitionWorker extends UntypedActor{
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef mainThread = null;
    private int SuccessCounter = 0;
    private int orderWorkerDoneCounter = 0;
    private String destFile;
    private int Ranks;
    private int numOfSubtensor;
    private ArrayList<Tensor> subTensors;
    private double Precision;


    @Override
    public void onReceive(Object message) throws Exception {
        /**
         * 用于初始化系统的启动参数
         * 包括各个阶的分块个数，张量的大小信息等
         * 此处用随机数据模拟张量分块后的数据
         */
        if (message instanceof ArgumentsInitialization) {
            ArgumentsInitialization argumentsInitialization = (ArgumentsInitialization)message;
            int[] Dim = argumentsInitialization.getDim();
            int[] Count = argumentsInitialization.getCount();
            Precision = argumentsInitialization.getPrecision();
            Ranks = argumentsInitialization.getRank();
            int machines = argumentsInitialization.getCores();
            int ip = argumentsInitialization.getIp();
            boolean roundrobin = argumentsInitialization.isRoundrobin();

            //子张量的个数
            numOfSubtensor = 1;
            for(int c : Count)
                numOfSubtensor *= c;

            //有多少个子张量块:numOfSubtensor
            log.info(numOfSubtensor + " !!!");
            destFile ="result/" + Arrays.toString(Dim) + Arrays.toString(Count) + "-" + machines;
            File file = new File(destFile);
            file.mkdir();

            //每个子张量的大小
            int[] dimOfSubtensor = new int[Ranks];
            int sizeOfSubtensor = 1;
            for(int i = 0; i < Ranks; i++) {
                dimOfSubtensor[i] = Dim[i] / Count[i];
                sizeOfSubtensor *= dimOfSubtensor[i];
            }

            //子张量块的数据初始化
            subTensors = new ArrayList<Tensor>(numOfSubtensor);
            //List<Tensor3> list = null;

            Random random = new Random(47);
            for (int i = 0; i < numOfSubtensor; i++) {
                Tensor tempTensor = new Tensor(Ranks, dimOfSubtensor);
                for (int j = 0; j < sizeOfSubtensor; j++)
                    tempTensor.data[j] = random.nextDouble();
                subTensors.add(tempTensor);
            }

            //创建OrderWorker
            //1.每一个order合并过程中应该有几个worker,有OrderWorker0 - OrderWorkerN
            int[] workerNum = new int[Ranks + 1];
            for(int i = 0; i < workerNum.length; i++) {
                int temp = 1;
                for(int j = 0; j < i; j++)
                    temp *= Count[j];
                workerNum[i] =  temp;
            }

            log.info(Arrays.toString(workerNum));

//            int machines = 5;

            //2.开始创建OrderWorker,OrderWorker再生成各个子CalculationWorker
            for(int i = 0; i < workerNum.length; i++) {
                ActorRef actor = getContext().actorOf(Props.create(
                        OrderWorker.class,i,machines,ip,roundrobin),
                        "OrderWorker" + i);
                actor.tell(new OrderMessage(workerNum,Count), getSelf());
            }

        }

        else if (message instanceof OrderWorkerDone) {
            orderWorkerDoneCounter++;
            //当所有的节点都已经构造完毕时
            if (orderWorkerDoneCounter == Ranks+1) {
                log.info("All actor have been constructed!!!");
                //3.将子张量块发送到最终一层的节点上
                //获取到最终一层的节点的地址
                String address = "OrderWorker" + Ranks + "/CalculationWorker";
                //将子张量块传递到各个最终的worker上
                for(int i = 0; i < numOfSubtensor; i++)
                    getContext().actorSelection(address+i).tell(
                            new TensorInitialization(subTensors.get(i),null,Precision,i), self());

                orderWorkerDoneCounter = 0;
                subTensors = null;
                System.gc();

            }
        }


        else if (message instanceof UData) {
            SuccessCounter++;
            Matrix U = ((UData) message).getU();
            int mode = new Double(((UData) message).getTol()).intValue();
            writeMatrixToFile(U, destFile + "/U-Mode" + mode);
            if (SuccessCounter == Ranks) {
                SuccessCounter = 0;
                mainThread.tell(destFile, self());
                getContext().stop(getSelf());
            }
        }

        else if(message instanceof MainThread) {
            mainThread = sender();
        }

    }

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
        log.info(self().path() + " : Write matrix: " + filename + " OK!");
    }
}
