package sg.edu.ntu.hospitalbeesqdemo.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import sg.edu.ntu.hospitalbeesqdemo.model.QueueStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public final class IllegalTransitionException extends Exception {
    public IllegalTransitionException(String queueNuber, QueueStatus from, QueueStatus to) {
        super(String.format("Status of Queue Number %s cannot change from %s to %s", queueNuber, from, to));
    }
}
