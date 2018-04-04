package sg.edu.ntu.hospitalbeesqdemo.model;

public class OnlineQueueElement extends QueueElement {
    private final String tid;
    private final LateRank lateRank;
    private static final long serialVersionUID = 8036172451619784152L;

    public OnlineQueueElement(int queueNumber, String tid, LateRank lateRank) {
        super("HB" + String.format(String.format("%04d",queueNumber)));
        this.tid = tid;
        this.lateRank = lateRank;
    }
    public OnlineQueueElement(String queueNumber, String tid, LateRank lateRank) {
        super(queueNumber);
        this.tid = tid;
        this.lateRank = lateRank;
    }


    public String getTid() {
        return tid;
    }

    public LateRank getLateRank() {
        return lateRank;
    }
}
