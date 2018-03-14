package sg.edu.ntu.hospitalbeesqdemo.model;

public class OnlineQueueElement extends QueueElement {
    private final long tid;
    private final LateRank lateRank;

    public OnlineQueueElement(int queueNumber, long tid, LateRank lateRank) {
        super("HB" + String.format(String.format("%04d",queueNumber)));
        this.tid = tid;
        this.lateRank = lateRank;
    }
    public OnlineQueueElement(String queueNumber, long tid, LateRank lateRank) {
        super(queueNumber);
        this.tid = tid;
        this.lateRank = lateRank;
    }


    public long getTid() {
        return tid;
    }

    public LateRank getLateRank() {
        return lateRank;
    }
}
