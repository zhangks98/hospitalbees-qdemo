package sg.edu.ntu.hospitalbeesqdemo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import sg.edu.ntu.hospitalbeesqdemo.model.QueueElement;
import sg.edu.ntu.hospitalbeesqdemo.repository.QueueRepository;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@RestController
@RequestMapping(value = "/queues")
public class QueuesController {

    private final QueueRepository queueRepository;

    final String regex = "^HB\\d{4}$";

    @Autowired
    public QueuesController(QueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    @RequestMapping(method = RequestMethod.POST, value="", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    HttpHeaders createQueue(@RequestParam(value = "online",required = true, defaultValue = "false") boolean online,
                            @RequestBody Map<String,String> payload) throws IllegalArgumentException {

        QueueElement qe;
        if(!online) {
            qe = queueRepository.createAndInsert();
        } else {
            // TODO Implement Validation

        }
        HttpHeaders headers = new HttpHeaders();
//        headers.setLocation(linkTo(QueuesController.class).slash(qe.getQueueNumber()).toUri());

        return headers;
    }
}
