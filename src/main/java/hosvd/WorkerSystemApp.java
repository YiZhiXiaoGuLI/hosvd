package hosvd;

import akka.actor.ActorSystem;
import akka.kernel.Bootable;
import com.typesafe.config.ConfigFactory;

/**
 * Created by cxy on 15-12-8.
 */
public class WorkerSystemApp implements Bootable{
    ActorSystem system;

    @Override
    public void startup() {
        system = ActorSystem.create("WorkerSystem",
                ConfigFactory.load().getConfig("WorkerSys"));
        System.out.println("Start WorkerSystem!!!");
    }

    @Override
    public void shutdown() {
        system.shutdown();
    }
}
