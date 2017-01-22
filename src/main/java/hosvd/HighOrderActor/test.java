package hosvd.HighOrderActor;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.sun.org.apache.bcel.internal.generic.NEW;
import hosvd.message.HasDone;
import hosvd.message.InnerOrthDone;

/**
 * Created by cxy on 15-12-23.
 */
public class test extends UntypedActor{
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public test() {
        log.info("I has constructed!!!");
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof NewMessage) {
            NewMessage newMessage = (NewMessage)message;
            int level = newMessage.getLevel();

            if(level <= 5) {
                ActorRef actor = context().actorOf(Props.create(test.class), Integer.toString(level + 1));
                actor.tell(new NewMessage(level+1),self());
            }
        }

        else if(message instanceof HasDone) {
//            ActorRef actorSelection = context().actorFor("*/6");
            ActorSelection actorSelection = context().actorSelection("/*/6");
            actorSelection.tell(new InnerOrthDone(),self());
        }

        else if(message instanceof InnerOrthDone) {
            log.info("I am -> " + self().toString());
        }
    }
}
