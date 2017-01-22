package hosvd.HighOrderActor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.remote.RemoteScope;
import hosvd.message.HasDone;
import hosvd.multimessage.OrderMessage;
import hosvd.multimessage.OrderWorkerDone;

/**
 * Created by cxy on 15-12-23.
 */
public class OrderWorker extends UntypedActor{

    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    //这是第几阶上的OrderWorker
    private final int ORDER;
    private int[] workerNum;
    private final int MACHINES;
    private final int Start_ip;
    private final boolean isRoundRobin;
    private int calculationworkerNum = 0;
    private int hasDoneCounter = 0;



    public OrderWorker(int order, int machines, int start_ip, boolean isRoundRobin) {
        ORDER = order;
        MACHINES = machines;
        Start_ip = start_ip;
        this.isRoundRobin = isRoundRobin;
        log.info("OrderWorker" + ORDER +" has constructed, with Machines : " + MACHINES);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof OrderMessage) {
            OrderMessage orderMessage = (OrderMessage)message;
            workerNum = orderMessage.getWorkerNum();
            int[] count = orderMessage.getDividedList();
            calculationworkerNum = workerNum[ORDER];

            //平均每个节点上有多少个actor
            int actorPerNode = workerNum[ORDER] / MACHINES;
            if(actorPerNode == 0)
                actorPerNode = 1;

            //创建CalculationWorker
            //System.out.println(ORDER);
            for (int i = 0; i < workerNum[ORDER]; i++) {
                String host = "192.168.18.1";
                int start = Start_ip;
                start += i % MACHINES;
                host += Integer.toString(start);
                Address addr = new Address("akka.tcp", "WorkerSystem", host, 2555);

                ActorRef actor = getContext().actorOf(Props.create(
                                CalculationWorker.class,ORDER,i,count,isRoundRobin).withDeploy(new Deploy(new RemoteScope(addr))),
                        "CalculationWorker" + i);
            }
        }

        else if (message instanceof HasDone) {
            hasDoneCounter++;
            if (hasDoneCounter == calculationworkerNum) {
                log.info("I have done!!!!!!");
                hasDoneCounter = 0;
                getContext().parent().tell(new OrderWorkerDone(),getSelf());
            }
        }

        else {
            unhandled(message);
        }
    }
}
