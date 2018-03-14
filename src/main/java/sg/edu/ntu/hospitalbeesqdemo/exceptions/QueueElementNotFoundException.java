package sg.edu.ntu.hospitalbeesqdemo.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public final class QueueElementNotFoundException extends Exception {
    public QueueElementNotFoundException(String queueNumber) {
        super(String.format("The specified Queue Number %s is not found", queueNumber));
    }
}
