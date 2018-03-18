package sg.edu.ntu.hospitalbeesqdemo.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class QueueElementResponse {
    @JsonUnwrapped
    private final QueueElement queueElement;

    private final int lengthBefore;

    public QueueElementResponse (QueueElement queueElement, int lengthBefore) {
        this.queueElement = queueElement;
        this.lengthBefore = lengthBefore;
    }

    public QueueElement getQueueElement() {
        return queueElement;
    }

    public int getLengthBefore() {
        return lengthBefore;
    }
}
