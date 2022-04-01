package yolo.chatapp.model;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.subscription.MultiEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedDeque;

@ApplicationScoped
public class Chat {

    private static final Logger logger = LoggerFactory.getLogger(Chat.class);

    private final ConcurrentLinkedDeque<Message> messages = new ConcurrentLinkedDeque<>() {
        {
            push(new Message(Instant.now(), "Start!"));
        }
    };

    private final ConcurrentLinkedDeque<Object> broadcastTrigger = new ConcurrentLinkedDeque<>();
    private final Multi<String> updatesMulti;

    public Chat() {
        updatesMulti = Multi.createFrom().<String>emitter(this::pollForUpdates)
                .broadcast().toAllSubscribers()
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    public void push(Message message) {
        messages.push(message);
        broadcastTrigger.push(1);
    }

    public Collection<Message> messages() {
        return Collections.unmodifiableCollection(messages);
    }

    public int messageCount() {
        return messages.size();
    }

    private void pollForUpdates(MultiEmitter<? super String> emitter) {
        while (!emitter.isCancelled()) {
            if (broadcastTrigger.poll() != null) {
                logger.info("Tick");
                emitter.emit("broadcast");
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                emitter.fail(e);
                return;
            }
        }
    }

    public Multi<String> updatesBroadcaster() {
        return updatesMulti;
    }
}
