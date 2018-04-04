package sg.edu.ntu.hospitalbeesqdemo.model;

public class AllQueueElementResponse {
    private final String[] queue;

    private final int length;

    public AllQueueElementResponse(String[] queue) {
        this.queue = queue;
        this.length = queue.length;
    }

    public String[] getQueue() {
        return queue;
    }

    public int getLength() {
        return length;
    }
}
