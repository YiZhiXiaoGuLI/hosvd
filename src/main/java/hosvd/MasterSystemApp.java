package hosvd;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.kernel.Bootable;
import akka.util.Timeout;
import com.typesafe.config.ConfigFactory;
import hosvd.HighOrderActor.PartitionWorker;
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
public class MasterSystemApp implements Bootable{

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
            actor = system.actorOf(Props.create(PartitionWorker.class), "PartitionWorker");

            final Timeout t = new Timeout(Duration.create(5, TimeUnit.DAYS));
            Future<Object> future = ask(actor, new MainThread(), t);

            System.out.println("Please input the order of the tensor: ");
            int order = sc.nextInt();
            System.out.println("The order of the tensor is: " + order);

            int[] dim = new int[order];
            int[] count = new int[order];

            for (int i = 0; i < order; i++) {
                System.out.println("Please input the number of the order" + (i + 1) + ": ");
                dim[i] = sc.nextInt();
                count[i] = 2;
            }

            for (int i = 0; i < order; i++) {
                System.out.println("Please input the divideds of the order" + (i + 1) + ": ");
                count[i] = sc.nextInt();
            }

            System.out.println("Please input the number of Computers :");
            int machines = sc.nextInt();

            System.out.println("Please input the starts of IP :");
            int ip = sc.nextInt();

            System.out.println("Needs RoundRobin??? (true/false) :");
            boolean roundrobin = sc.nextBoolean();

            for (int i = 0; i < order; i++)
                System.out.print(dim[i] + " ,");

            System.out.println();

            for (int i = 0; i < order; i++)
                System.out.print(count[i] + " ,");

            System.out.println(machines + " ,");

            System.out.println(ip + " ,");

            System.out.println(roundrobin + " ,");

            System.out.println("Starting!!!");


            actor.tell(new ArgumentsInitialization(dim.length, dim, count, machines, ip, roundrobin, 1e-14), ActorRef.noSender());

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

            try {
                Thread.sleep(10000);
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