package sg.edu.ntu.hospitalbeesqdemo.web;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import sg.edu.ntu.hospitalbeesqdemo.exceptions.*;
import sg.edu.ntu.hospitalbeesqdemo.model.*;
import sg.edu.ntu.hospitalbeesqdemo.repository.QueueRepository;

import java.net.MalformedURLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@RestController
@RequestMapping(value = "/queues")
public class QueuesController {

    private final QueueRepository queueRepository;
    private final SocketController socketController;
    private final String hospitalId;
    private final String apiUrl;
    private final String bookingApiUrl;
    private final int lateTimeAllowed;
    private RestTemplate restTemplate;
    private static final String tidTemplate = "CCCCYYYY-MM-DDTHH:mm:ssZQQQQ";

    @Autowired
    public QueuesController(QueueRepository queueRepository,
                            SocketController socketController,
                            RestTemplate restTemplate,
                            @Value("${queue.hb_url}") String serverUrl,
                            @Value("${queue.hospital_id}") String hospitalId,
                            @Value("${queue.late_time_in_minutes}") int lateTimeAllowed) throws MalformedURLException {
        this.queueRepository = queueRepository;
        this.socketController = socketController;
        this.restTemplate = restTemplate;
        this.apiUrl = serverUrl + "/api";
        this.hospitalId = hospitalId;
        this.lateTimeAllowed = lateTimeAllowed;

        bookingApiUrl = apiUrl + "/booking/";
    }

    /**
     * CREATE route for in-hospital booking
     * Not used by HospitalBee
     *
     * @return HttpHeader containing the corresponding SHOW route
     * @throws IllegalArgumentException if the queue number is not created
     */
    @PostMapping(value = "")
    @ResponseStatus(HttpStatus.CREATED)
    HttpHeaders createOfflineQueue() throws IllegalArgumentException {
        QueueElement qe = queueRepository.createAndInsert();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(linkTo(QueuesController.class).slash(qe.getQueueNumber()).toUri());
        return headers;
    }

    @PostMapping(value = "/connect")
    void connectToSocket() {
        if (!socketController.isConnected())
            socketController.connectToSocket();
    }

    @PostMapping(value = "/disconnect")
    void disconnectSocket () {
        if (socketController.isConnected())
            socketController.disconnectToSocket();
    }


    /**
     * CREATE route for HospitalBee check-in
     *
     * //@param onlineQueueForm the JSON request body for online check-in, for constraints see {@link OnlineQueueForm}
     * @return HttpHeader containing the corresponding SHOW route
     * @throws IllegalArgumentException          if the request body is illegal
     * @throws QueueNumberAlreadyExistsException if the queue number is already in the queue
     * @throws QueueElementNotFoundException     if the queue element cannot be found by queue number
     */
    @PostMapping(value = "/checkin/{tid}")
    @ResponseStatus(HttpStatus.CREATED)
    void createOnlineQueue(@PathVariable("tid") String tid) throws IllegalArgumentException, IllegalTransitionException, QueueNumberAlreadyExistsException, QueueElementNotFoundException, MissedQueueExpiredException,  JSONException {
        OnlineQueueElement qe = null;
        try {
            qe = queueRepository.findQueueElementByTid(tid);
            if (qe.getStatus().equals(QueueStatus.MISSED) && !qe.isReactivated()) {
                queueRepository.reactivate(qe.getQueueNumber());
            }
        } catch (QueueElementNotFoundException e) {
            // TODO query HB for the queueElement
            ResponseEntity<String> response = restTemplate.getForEntity(bookingApiUrl + tid, String.class);
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                JSONObject obj = new JSONObject(response.getBody());

                final String bookingTid = obj.getString("Booking_TID");
                if (!bookingTid.equals(tid) || tid.length() != tidTemplate.length())
                    throw new IllegalArgumentException("Illegal tid format");
                final String bookingHospitalId = tid.substring(0, 4);
                if (!bookingHospitalId.equals(this.hospitalId))
                    throw new IllegalArgumentException("Hospital ID does not match");
                final String bookingQueueStatus = obj.getString("Booking_QueueStatus");
                if (!bookingQueueStatus.equals("INACTIVE"))
                    throw new IllegalArgumentException("Queue Status must be INACTIVE");

                final String bookingQueueNumber = tid.substring(tid.length() - 4, tid.length());
                final String refQueueNumber = obj.getString("Booking_ReferencedQueueNumber");
                final int eta = obj.getInt("Booking_ETA");
                Instant bookingTime = Instant.parse(tid.substring(4, tid.length() - 4));
                LateRank bookingLateRank = null;
                if (bookingTime.plus(eta, ChronoUnit.MINUTES).isAfter(Instant.now())) {
                    bookingLateRank = LateRank.ON_TIME;
                } else if (bookingTime.plus(eta, ChronoUnit.MINUTES).plus(this.lateTimeAllowed, ChronoUnit.MINUTES).isAfter(Instant.now())) {
                    bookingLateRank = LateRank.LITTLE_LATE;
                } else {
                    bookingLateRank = LateRank.VERY_LATE;
                }

                qe = new OnlineQueueElement(bookingQueueNumber, tid, bookingLateRank);
                queueRepository.insert(qe, refQueueNumber);
                restTemplate.put(bookingApiUrl + tid + "/QSUpdateToActive", null);
            }
        }
    }


    /**
     * INDEX route for showing all the queue numbers
     *
     * @return a JSON list of all the queue numbers in the system
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    AllQueueElementResponse getAllQueueNumbers() {
        return new AllQueueElementResponse(queueRepository.getClinicQueue());
    }

    /**
     * UPDATE route for notifying the head of the queue
     *
     * @return the notified QueueElement
     * @throws EmptyQueueException if there are no elements in the queue
     */
    @PutMapping(value = "/notify")
    QueueElement notifyHead() throws EmptyQueueException {
        // TODO Notify the HospitalBee API on calling QueueElement
        return queueRepository.notifyQueueElement();
    }

    /**
     * SHOW route for displaying the details of the QueueElement
     *
     * @param queueNumber the queue number to query the QueueElement
     * @return QueueElement fields as well as the number of QueueElements before the given one see {@link QueueElementResponse}
     * @throws QueueElementNotFoundException if the queue element cannot be found by queue number
     */
    @GetMapping(value = "/{queueNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    QueueElementResponse getQueueNumber(@PathVariable("queueNumber") String queueNumber) throws QueueElementNotFoundException {
        return new QueueElementResponse(queueRepository.findQueueElementByNumber(queueNumber), queueRepository.getLengthFrom(queueNumber));
    }

    /**
     * UPDATE route for setting the queue number as missed
     * if QueueElement isReactivated, then delete the queue and notify HospitalBee that it is absent
     *
     * @param queueNumber the queue number to be set missed
     * @throws QueueElementNotFoundException if the queue element cannot be found by queue number
     * @throws IllegalTransitionException    if the queue element has not been NOTIFIED
     */
    @PutMapping(value = "/{queueNumber}/miss")
    void setMiss(@PathVariable("queueNumber") String queueNumber) throws QueueElementNotFoundException, IllegalTransitionException {
        queueRepository.setMissed(queueNumber);
    }

    /**
     * UPDATE route for reactivating a missed queue
     *
     * @param queueNumber the queue number to be reactivated
     * @throws MissedQueueExpiredException   if the queue number is expired
     * @throws QueueElementNotFoundException if the queue element cannot be found by queue number
     * @throws IllegalTransitionException    if the queue number is not MISSED
     */
    @PutMapping(value = "/{queueNumber}/reactivate")
    void reactivate(@PathVariable("queueNumber") String queueNumber) throws MissedQueueExpiredException, QueueElementNotFoundException, IllegalTransitionException {
        queueRepository.reactivate(queueNumber);
    }

    /**
     * DESTROY route for setting the queue as completed
     *
     * @param queueNumber the queue number to be set as complete
     * @throws QueueElementNotFoundException if the queue element cannot be found by queue number
     * @throws IllegalTransitionException    if the queue number is not NOTIFIED
     */
    @PutMapping(value = "/{queueNumber}/complete")
    void setComplete(@PathVariable("queueNumber") String queueNumber) throws QueueElementNotFoundException, IllegalTransitionException {
        QueueElement qe = queueRepository.setComplete(queueNumber);
        if (qe instanceof OnlineQueueElement) {
            restTemplate.put(bookingApiUrl + ((OnlineQueueElement) qe).getTid() + "/BSUpdateToCompleted", null);
        }
    }

    /**
     * Reset the QueueRepository at the end of the clinic operational hours
     * Not used by HB
     *
     * @return Success message
     */
    @DeleteMapping(value = "/reset")
    String reset() {
        queueRepository.reset();
        return "Queue Repository Reset Successful!";
    }

    /**
     * Exception Handler for Illegal Request Body
     *
     * @param e the exception
     * @return the exception message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String handleBadRequest(Exception e) {
        return e.getMessage();
    }
}
