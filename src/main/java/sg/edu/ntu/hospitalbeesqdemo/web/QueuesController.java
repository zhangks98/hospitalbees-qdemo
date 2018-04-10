package sg.edu.ntu.hospitalbeesqdemo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import sg.edu.ntu.hospitalbeesqdemo.exceptions.*;
import sg.edu.ntu.hospitalbeesqdemo.model.*;
import sg.edu.ntu.hospitalbeesqdemo.repository.QueueRepository;

import javax.validation.Valid;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@RestController
@RequestMapping(value = "/queues")
public class QueuesController {

    private final QueueRepository queueRepository;
    private final SocketController socketController;
    private final URL apiUrl;

    @Autowired
    public QueuesController(QueueRepository queueRepository,
                            SocketController socketController,
                            @Value("${queue.hb_url}") String serverUrl) throws MalformedURLException {
        this.queueRepository = queueRepository;
        this.socketController = socketController;
        this.apiUrl = new URL (new URL(serverUrl), "api");
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
    HttpHeaders createOnlineQueue(@PathVariable("tid") String tid) throws IllegalArgumentException, QueueNumberAlreadyExistsException, QueueElementNotFoundException, IllegalTransitionException, MissedQueueExpiredException, IOException {
        OnlineQueueElement qe;
        try {
            qe = queueRepository.findQueueElementByTid(tid);
            if (qe.getStatus().equals(QueueStatus.MISSED) && !qe.isReactivated()) {
                queueRepository.reactivate(qe.getQueueNumber());
            }
        } catch (QueueElementNotFoundException e) {
            // TODO query HB for the queueElement
            URL queryBookingUrl = new URL(apiUrl,"booking/" + tid);
            HttpURLConnection connection = (HttpURLConnection) queryBookingUrl.openConnection();


        }
//        OnlineQueueElement onlineQueueElement = onlineQueueForm.toOnlineQueueElement();
//        queueRepository.insert(onlineQueueElement, onlineQueueForm.getRefQueueNumber());
        HttpHeaders headers = new HttpHeaders();
//        headers.setLocation(linkTo(QueuesController.class).slash(qe.getQueueNumber()).toUri());
        return headers;
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
    @DeleteMapping(value = "/{queueNumber}/complete")
    void setComplete(@PathVariable("queueNumber") String queueNumber) throws QueueElementNotFoundException, IllegalTransitionException {
        queueRepository.setComplete(queueNumber);
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
