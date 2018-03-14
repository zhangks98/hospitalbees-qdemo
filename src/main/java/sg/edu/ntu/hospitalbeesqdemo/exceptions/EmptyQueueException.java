package sg.edu.ntu.hospitalbeesqdemo.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.CONFLICT)
public final class EmptyQueueException extends Exception {
    public EmptyQueueException() {
        super("The Queue is Currently Empty");
    }
}
