package hosvd;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import hosvd.actor.MasterActor;
import hosvd.actor.MultiMasterActor;
import hosvd.message.HasDone;
import hosvd.message.IncrementalInit;
import hosvd.message.Initialization;
import static akka.pattern.Patterns.ask;
import static akka.pattern.Patterns.pipe;


import hosvd.message.MultiInitialization;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import akka.dispatch.Futures;
import akka.dispatch.Mapper;
import akka.util.Timeout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Created by cxy on 15-10-23.
 */
public class HosvdApp {

    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("ParallelHOSVD");
        ActorRef actor = system.actorOf(Props.create(MasterActor.class),"master");
        actor.tell(new Initialization(5, 20, 20, 20, false), ActorRef.noSender());
//
//        ActorSystem system = ActorSystem.create("ParallelHOSVD");
//        ActorRef actor = system.actorOf(Props.create(MultiMasterActor.class),"master");

        final Timeout t = new Timeout(Duration.create(3, TimeUnit.DAYS));
        Future<Object> future = ask(actor, new HasDone(), t);

//        actor.tell(new MultiInitialization(2, 20, 20, 5, 2,1,1), ActorRef.noSender());


        try {
            String result = (String) Await.result(future,t.duration());
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //做一次增量算法，看结果是否正确
//        actor.tell(new IncrementalInit(3,10,20,5),ActorRef.noSender());
//        future = ask(actor,new HasDone(),t);
//        try {
//            String result = (String) Await.result(future,t.duration());
//            System.out.println(result);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        final ArrayList<Future<Object>> futures = new ArrayList<>();
//        Future f1 = ask(actor, new HasDone(), t);
//        futures.add(f1);
//
//        final Future<Iterable<Object>> aggregate = Futures.sequence(futures,
//                system.dispatcher());
//
//        final Future<Object> transformed = aggregate.map(
//                new Mapper<Iterable<Object>, Object>() {
//                    public Object apply(Iterable<Object> coll) {
//                        final Iterator<Object> it = coll.iterator();
//                        return it.next();
//                    }
//                }, system.dispatcher());

        system.shutdown();
    }
}
