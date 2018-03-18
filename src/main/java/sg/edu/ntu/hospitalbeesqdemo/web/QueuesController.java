package sg.edu.ntu.hospitalbeesqdemo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import sg.edu.ntu.hospitalbeesqdemo.exceptions.*;
import sg.edu.ntu.hospitalbeesqdemo.model.OnlineQueueElement;
import sg.edu.ntu.hospitalbeesqdemo.model.OnlineQueueForm;
import sg.edu.ntu.hospitalbeesqdemo.model.QueueElement;
import sg.edu.ntu.hospitalbeesqdemo.model.QueueElementResponse;
import sg.edu.ntu.hospitalbeesqdemo.repository.QueueRepository;

import javax.validation.Valid;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@RestController
@RequestMapping(value = "/queues")
public class QueuesController {

    private final QueueRepository queueRepository;

    @Autowired
    public QueuesController(QueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    // Not used by HB
    @PostMapping(value = "")
    @ResponseStatus(HttpStatus.CREATED)
    HttpHeaders createOfflineQueue() throws IllegalArgumentException {
        QueueElement qe = queueRepository.createAndInsert();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(linkTo(QueuesController.class).slash(qe.getQueueNumber()).toUri());
        return headers;
    }

    // CREATE Route for check-in
    @PostMapping(value = "/online", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    HttpHeaders createOnlineQueue(@Valid @RequestBody OnlineQueueForm onlineQueueForm) throws IllegalArgumentException, QueueNumberAlreadyExistsException, QueueElementNotFoundException {
        OnlineQueueElement onlineQueueElement = onlineQueueForm.toOnlineQueueElement();
        queueRepository.insert(onlineQueueElement, onlineQueueForm.getRefQueueNumber());
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(linkTo(QueuesController.class).slash(onlineQueueElement.getQueueNumber()).toUri());
        return headers;
    }

    @GetMapping(value = "")
    String[] getAllQueueNumbers() {
        return queueRepository.getClinicQueue();
    }

    @PutMapping(value = "/notify")
    QueueElement notifyHead() throws EmptyQueueException {
        return queueRepository.notifyQueueElement();
    }

    @GetMapping(value = "/{queueNumber}", produces = MediaType.APPLICATION_JSON_VALUE)
    QueueElementResponse getQueueNumber(@PathVariable("queueNumber") String queueNumber) throws QueueElementNotFoundException {
        QueueElement qe = queueRepository.findQueueElementByNumber(queueNumber);
        return new QueueElementResponse(qe, queueRepository.getLengthFrom(queueNumber));
    }

    @PutMapping(value = "/{queueNumber}/miss")
    void setMiss(@PathVariable("queueNumber") String queueNumber) throws QueueElementNotFoundException, IllegalTransitionException {
        queueRepository.setMissed(queueNumber);
    }

    @PutMapping(value = "/{queueNumber}/reactivate")
    void reactivate(@PathVariable("queueNumber") String queueNumber) throws MissedQueueExpiredException, QueueElementNotFoundException, IllegalTransitionException {
        queueRepository.reactivate(queueNumber);
    }

    @DeleteMapping(value = "/{queueNumber}/complete")
    void setComplete(@PathVariable("queueNumber") String queueNumber) throws QueueElementNotFoundException, IllegalTransitionException {
        queueRepository.setComplete(queueNumber);
    }

    // Not used by HB
    @DeleteMapping(value = "/reset")
    String reset() {
        queueRepository.reset();
        return "Queue Repository Reset Successful!";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String handleBadRequest(Exception e) {
        return e.getMessage();
    }
}
