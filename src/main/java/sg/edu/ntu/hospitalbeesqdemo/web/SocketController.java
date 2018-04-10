package sg.edu.ntu.hospitalbeesqdemo.web;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.io.IOException;
import java.io.StringWriter;

@Controller
public class SocketController {
    private final String serverUrl;
    private final String hospitalId;
    private final String hospitalName;
    private final QueueRepository queueRepository;
    private Socket mSocket;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private ObjectMapper objectMapper = new ObjectMapper();
    private JsonFactory jsonFactory = new JsonFactory();


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
        log.info("Connected to HB Server at " + serverUrl);

    }

    @PreDestroy
    public void disconnectToSocket() {
        mSocket.disconnect();
        mSocket.off();
        log.info("Disconnected to HB Server");

    }

    private Emitter.Listener onPeekLast = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Ack ack = (Ack) args[args.length - 1];
            try {
                QueueElement queueElement = queueRepository.peekLast();
                StringWriter writer = new StringWriter();
                JsonGenerator jsonGenerator = jsonFactory.createGenerator(writer);
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("queueNumber", queueElement.getQueueNumber());
                jsonGenerator.writeNumberField("queueLength", queueRepository.getLength());
                jsonGenerator.writeEndObject();
                jsonGenerator.close();
                writer.close();
                String res = writer.toString();
                ack.call(res);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
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
