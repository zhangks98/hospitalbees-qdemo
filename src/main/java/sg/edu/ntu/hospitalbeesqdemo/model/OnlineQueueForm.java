package sg.edu.ntu.hospitalbeesqdemo.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public class OnlineQueueForm {

    @NotBlank
    @Pattern(regexp = "^HB\\d{4}$")
    private String queueNumber;

    private String tid;

    @NotBlank
    private String lateRank;

    @NotBlank
    private String refQueueNumber;

    public String getRefQueueNumber() {
        return refQueueNumber;
    }

    public void setRefQueueNumber(String refQueueNumber) {
        this.refQueueNumber = refQueueNumber;
    }

    public String getQueueNumber() {
        return queueNumber;
    }

    public void setQueueNumber(String queueNumber) {
        this.queueNumber = queueNumber;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getLateRank() {
        return lateRank;
    }

    public void setLateRank(String lateRank) {
        this.lateRank = lateRank;
    }

    public OnlineQueueElement toOnlineQueueElement() throws IllegalArgumentException {
        LateRank lateRank;
        lateRank = LateRank.parse(this.lateRank);
        return new OnlineQueueElement(queueNumber,tid,lateRank);
    }
}
