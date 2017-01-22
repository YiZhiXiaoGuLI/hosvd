package hosvd.HighOrderActor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.util.Timeout;
import hosvd.message.HasDone;
import hosvd.multimessage.ArgumentsInitialization;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

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

        ActorSystem system = ActorSystem.create("ParallelHOSVD");
        ActorRef actor = system.actorOf(Props.create(test.class), "PartitionWorker");
        actor.tell(new NewMessage(0),ActorRef.noSender());


        Thread.sleep(1000);

        actor.tell(new HasDone(), ActorRef.noSender());

        Thread.sleep(1000);
        system.shutdown();
    }
}
