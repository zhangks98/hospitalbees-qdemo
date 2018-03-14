package sg.edu.ntu.hospitalbeesqdemo.model;

public class QueueElement {
    private final String queueNumber;
    private final Object lock = new Object();
    private volatile QueueStatus status = QueueStatus.ACTIVE;
    private volatile long missedTime = 0;

    public QueueElement(int queueNumber) {
        this.queueNumber = String.format("%04d",queueNumber);
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
}
