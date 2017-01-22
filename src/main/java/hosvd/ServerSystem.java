package hosvd;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.kernel.Bootable;
import akka.util.Timeout;
import hosvd.actor.MultiMasterActor;
import hosvd.message.HasDone;
import hosvd.message.MultiInitialization;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 * Created by cxy on 15-10-23.
 */
public class ServerSystem implements Bootable{

    private ActorSystem system = ActorSystem.create("ParallelHOSVD");

    @Override
    public void startup() {
        ActorRef actor = system.actorOf(Props.create(MultiMasterActor.class),"master");

        final Timeout t = new Timeout(Duration.create(3, TimeUnit.DAYS));
        Future<Object> future = ask(actor, new HasDone(), t);

        actor.tell(new MultiInitialization(2, 20, 20, 5, 2,1,1), ActorRef.noSender());


        try {
            String result = (String) Await.result(future,t.duration());
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void shutdown() {
        system.shutdown();
    }
}
