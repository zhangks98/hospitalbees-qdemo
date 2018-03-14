package sg.edu.ntu.hospitalbeesqdemo.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public final class QueueNumberAlreadyExistsException extends Exception {
    public QueueNumberAlreadyExistsException(String queueNumber) {
        super(String.format("The queue number %s already exists in queue", queueNumber));
    }
}
