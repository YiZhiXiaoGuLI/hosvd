package hosvd;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.util.Timeout;
import hosvd.HighOrderActor.PartitionWorker;
import hosvd.multimessage.ArgumentsInitialization;
import hosvd.multimessage.MainThread;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 * Created by cxy on 15-11-9.
 */
public class HierarchyApp {

    public static void main(String[] args) throws InterruptedException {
//        ActorSystem system = ActorSystem.create("ParallelHOSVD");
//        ActorRef actor = system.actorOf(Props.create(MasterActor.class),"master");
//        actor.tell(new Initialization(2, 20, 20, 5, false), ActorRef.noSender());




//        for (int i = 0; i < 4; i++) {
            ActorSystem system = ActorSystem.create("ParallelHOSVD");
            ActorRef actor = system.actorOf(Props.create(PartitionWorker.class), "PartitionWorker");

            final Timeout t = new Timeout(Duration.create(3, TimeUnit.DAYS));
            Future<Object> future = ask(actor, new MainThread(), t);

//        int[] dim = {5,10,10,5};
//        int[] count = {2,2,2,2};

            int[] dim = {30, 30, 30};
//            int[] dim = new int[3];
//            Arrays.fill(dim,(i+1)*10);
            int[] count = {5, 2, 2};
            int cores = 5;
            int ip = 5;

            actor.tell(new ArgumentsInitialization(dim.length, dim, count, cores, ip, true, 1e-15), ActorRef.noSender());

            long times = System.currentTimeMillis();

            try {
                String dest = (String) Await.result(future, t.duration());
                long dura = System.currentTimeMillis() - times;
                System.out.println(dura);
                File file = new File(dest + "/times.txt");
                file.createNewFile();
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write(String.valueOf(dura));
                fileWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            system.stop(actor);
            system.shutdown();
//        }


    }
}