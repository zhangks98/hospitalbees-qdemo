package sg.edu.ntu.hospitalbeesqdemo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class QueueElement implements Serializable {
    private final String queueNumber;

    @JsonIgnore
    private transient Object lock = new Object();

    private volatile QueueStatus status = QueueStatus.ACTIVE;
    private volatile long missedTime = 0;
    private volatile boolean isReactivated = false;
    private static final long serialVersionUID = 6745785690069626941L;

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.lock = new Object();
    }

    public QueueElement(int queueNumber) {
        this.queueNumber = String.format("%04d", queueNumber);
    }

    public QueueElement(String queueNumber) {
        this.queueNumber = queueNumber;
    }

    public String getQueueNumber() {
        return queueNumber;
    }

    public QueueStatus getStatus() {
        synchronized (this.lock) {
            return status;
        }
    }

    public void setStatus(QueueStatus status) {
        synchronized (this.lock) {
            this.status = status;
        }
    }

    public long getMissedTime() {
        synchronized (this.lock) {
            return missedTime;
        }
    }

    public void setMissedTime(long missedTime) {
        synchronized (this.lock) {
            this.missedTime = missedTime;
        }
    }

    public boolean isReactivated() {
        synchronized (this.lock) {
            return isReactivated;
        }
    }

    public void setReactivated(boolean reactivated) {
        synchronized (this.lock) {
            isReactivated = reactivated;
        }
    }


}
