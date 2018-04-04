package sg.edu.ntu.hospitalbeesqdemo.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public final class MissedQueueExpiredException extends Exception {
    public MissedQueueExpiredException(String queueNumber) {
        super(String.format("The specified Queue Number %s is expired and cannot be reactivated",queueNumber));
    }

}
