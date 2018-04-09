package sg.edu.ntu.hospitalbeesqdemo.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.socket.client.Ack;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.UriComponentsBuilder;
import sg.edu.ntu.hospitalbeesqdemo.exceptions.QueueElementNotFoundException;
import sg.edu.ntu.hospitalbeesqdemo.model.QueueElement;
import sg.edu.ntu.hospitalbeesqdemo.repository.QueueRepository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Controller
public class SocketController {
    private String serverUrl;
    private String hospitalId;
    private String hospitalName;
    private final QueueRepository queueRepository;
    private Socket mSocket;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private boolean isConnected = false;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public SocketController (@Value("${queue.hb_url}") String serverUrl,
                             @Value("${queue.hospital_id}") String hospitalId,
                             @Value("${queue.hospital_name}") String hospitalName,
                             QueueRepository queueRepository) {
        this.serverUrl = serverUrl;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.queueRepository = queueRepository;
        Manager manager = new Manager(UriComponentsBuilder
                .fromUriString(serverUrl)
                .queryParam("hospitalId", hospitalId)
                .queryParam("name", hospitalName)
                .build().toUri()
        );
        mSocket = manager.socket("/hospital");
        log.info("serverUrl = [" + serverUrl + "], hospitalId = [" + hospitalId + "], hospitalName = [" + hospitalName + "]");

    }

    @PostConstruct
    public void connectToSocket() {
        mSocket.on("peekLast", onPeekLast)
                .on("getLength", onGetLength)
                .on("getLengthFrom", onGetLengthFrom);
        mSocket.connect();
        isConnected = true;
        log.info("Connected to HB Server at " + serverUrl);

    }

    @PreDestroy
    public void disconnectToSocket() {
        mSocket.disconnect();
        mSocket.off();
        isConnected = false;
        log.info("Disconnected to HB Server");

    }

    public boolean isConnected() {
        return isConnected;
    }

    private Emitter.Listener onPeekLast = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Ack ack = (Ack) args[args.length - 1];
            try {
                QueueElement queueElement = queueRepository.peekLast();
                String res = objectMapper.writeValueAsString(queueElement);
                ack.call(res);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    };

    private Emitter.Listener onGetLength = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Ack ack = (Ack) args[args.length - 1];
            int length = queueRepository.getLength();
            ack.call(length);
        }
    };

    private Emitter.Listener onGetLengthFrom = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            String queueNumber = (String) args[0];
            Ack ack = (Ack) args[args.length - 1];
            try {
                int length = queueRepository.getLengthFrom(queueNumber);
                ack.call(length);
            } catch (QueueElementNotFoundException e) {
                ack.call(e.getMessage());
            }
        }
    };


}
