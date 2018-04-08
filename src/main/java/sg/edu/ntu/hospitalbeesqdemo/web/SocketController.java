package sg.edu.ntu.hospitalbeesqdemo.web;

import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import sg.edu.ntu.hospitalbeesqdemo.repository.QueueRepository;

import javax.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;

@Controller
public class SocketController {
    private String serverUrl;
    private String hospitalId;
    private String hospitalName;
    private final QueueRepository queueRepository;
    private Socket mSocket;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private JSONObject hospitalDetails = new JSONObject();
    private boolean isConnected = false;

    @Autowired
    public SocketController (@Value("${queue.hb_url}") String serverUrl,
                             @Value("${queue.hospital_id}") String hospitalId,
                             @Value("${queue.hospital_name}") String hospitalName,
                             QueueRepository queueRepository) {
        this.serverUrl = serverUrl;
        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;
        this.queueRepository = queueRepository;
        Manager manager = new Manager(URI.create(serverUrl));
        mSocket = manager.socket("/hospital");
        try {
            hospitalDetails.put("hospitalId", hospitalId);
            hospitalDetails.put("name", hospitalName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        log.info("serverUrl = [" + serverUrl + "], hospitalId = [" + hospitalId + "], hospitalName = [" + hospitalName + "]");

    }

    @PostConstruct
    public void connectToSocket() {
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.connect();
        log.info("Connected to HB Server at " + serverUrl);

    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            if (!isConnected) {
                mSocket.emit("handshake", hospitalDetails);
                isConnected = true;
            }
        }
    };




}
