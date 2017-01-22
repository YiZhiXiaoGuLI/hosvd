package hosvd;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.kernel.Bootable;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import hosvd.HighOrderActor.PartitionWorker;
import hosvd.actor.MasterActor;
import hosvd.message.HasDone;
import hosvd.message.Initialization;
import hosvd.multimessage.ArgumentsInitialization;
import hosvd.multimessage.MainThread;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 * Created by cxy on 15-12-8.
 */
public class RoundRobinSystemApp implements Bootable{

    ActorSystem system;
    ActorRef actor;


    @Override
    public void startup() {

        Scanner sc = new Scanner(System.in);
        System.out.print("Input the number of the experiment :");
        int numOfexperiment = sc.nextInt();

        for (int exp = 0; exp < numOfexperiment; exp++) {
            system = ActorSystem.create("ParallelHOSVD",
                    ConfigFactory.load().getConfig("MasterSys"));
            actor = system.actorOf(Props.create(MasterActor.class), "Master");

            final Timeout t = new Timeout(Duration.create(5, TimeUnit.DAYS));
            Future<Object> future = ask(actor, new HasDone(), t);

            System.out.println("Please input the order of the tensor: ");
            int order = sc.nextInt();
            System.out.println("The order of the tensor is: " + order);

            int[] dim = new int[order];

            for (int i = 0; i < order; i++) {
                System.out.println("Please input the number of the order" + (i + 1) + ": ");
                dim[i] = sc.nextInt();
            }

            System.out.println("Please input the number of Computers :");
            int machines = sc.nextInt();

            System.out.println("Please input the starts of IP :");
            int ip = sc.nextInt();


            for (int i = 0; i < order; i++)
                System.out.print(dim[i] + " ,");

            System.out.println();


            System.out.println(machines + " ,");

            System.out.println(ip + " ,");

            System.out.println("Starting!!!");

            actor.tell(new Initialization(machines, dim[0], dim[1], dim[2], false, ip), ActorRef.noSender());


            try {
                String dest = (String) Await.result(future, t.duration());
            } catch (Exception e) {
                e.printStackTrace();
            }

            system.stop(actor);
            system.shutdown();

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void shutdown() {
        system.stop(actor);
        System.gc();
        system.shutdown();
    }
}
