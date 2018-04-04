package sg.edu.ntu.hospitalbeesqdemo.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sg.edu.ntu.hospitalbeesqdemo.exceptions.*;
import sg.edu.ntu.hospitalbeesqdemo.model.LateRank;
import sg.edu.ntu.hospitalbeesqdemo.model.OnlineQueueElement;
import sg.edu.ntu.hospitalbeesqdemo.model.QueueElement;
import sg.edu.ntu.hospitalbeesqdemo.model.QueueStatus;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public final class InMemoryQueueRepository implements QueueRepository {

    private final AtomicInteger queueNumberGenerator = new AtomicInteger();
    private final List<String> clinicQueue = Collections.synchronizedList(new ArrayList<>());
    private final ConcurrentMap<String, QueueElement> clinicQueueMap = new ConcurrentHashMap<>();
//    private final ConcurrentMap<String, QueueElement> missedQueueMap = new ConcurrentHashMap<>();
//    private final Object lock = new Object();
    private final SecureRandom random = new SecureRandom();
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Clock clock;
    private final double latePercentage;
    private final double missPercentage;
    private final long missTimeAllowed;

    private static final String EMPTY_QUEUE = "NO_TAIL";

    // TODO Add Serialization Code to save state
    // TODO Add handshake with hb
    @PreDestroy
    public void persistData() throws IOException {
        SerializationUtil.saveObject(clinicQueue, "queue.ser");
        SerializationUtil.saveObject(clinicQueueMap, "queue-map.ser");
        SerializationUtil.saveObject(queueNumberGenerator.get(), "generator.ser");
    }

    @PostConstruct
    public void loadData(){
        Object cq = SerializationUtil.loadObject("queue.ser");
        Object cqm = SerializationUtil.loadObject("queue-map.ser");
        Object g = SerializationUtil.loadObject("generator.ser");

        if (cq instanceof List  && cqm instanceof ConcurrentHashMap && g!= null){
            clinicQueue.addAll((List<String>) cq);
            clinicQueueMap.putAll((ConcurrentMap<String, QueueElement>) cqm);
            queueNumberGenerator.set((Integer) g);
            boolean validatorFlag = true;
            ListIterator<String> iter = clinicQueue.listIterator(clinicQueue.size());
            while(iter.hasPrevious()) {
                String qnString = iter.previous();
                if (!clinicQueueMap.containsKey(qnString)) {
                    validatorFlag = false;
                    break;
                }
            }

            if(!validatorFlag) {
                queueNumberGenerator.set(0);
                clinicQueue.clear();
                clinicQueueMap.clear();
            }
        }

        log.info("Current Queue: " + String.join(", ",clinicQueue));
        log.info("Current Generator Number: " + queueNumberGenerator.get());
    }

    @Autowired
    public InMemoryQueueRepository( @Value("${queue.miss_time_allowed_in_minutes}") long missTimeAllowedInMinutes,
                                    @Value("${queue.late_percentage}") double latePercentage,
                                    @Value("${queue.miss_percentage}") double missPercentage,
                                   Clock clock) {
        this.latePercentage = latePercentage;
        this.missPercentage = missPercentage;
        this.missTimeAllowed = TimeUnit.MINUTES.toMillis(missTimeAllowedInMinutes);
        this.clock = clock;
        log.info("missTimeAllowedInMinutes = [" + missTimeAllowedInMinutes + "], latePercentage = [" + latePercentage + "], missPercentage = [" + missPercentage + "], clock = [" + clock + "]");
    }

    @Override
    public QueueElement findQueueElementByNumber(String queueNumber) throws QueueElementNotFoundException {
        if (!clinicQueueMap.containsKey(queueNumber)) {
            throw new QueueElementNotFoundException(queueNumber);
        }
        return clinicQueueMap.get(queueNumber);
    }

    @Override
    public OnlineQueueElement findQueueElementByTid(String tid) throws QueueElementNotFoundException {
        String extractedQueueNumber = "HB" + tid.substring(tid.length() - 4);
        if (!clinicQueueMap.containsKey(extractedQueueNumber)) {
            throw new QueueElementNotFoundException(extractedQueueNumber);
        } else {
            OnlineQueueElement qe = (OnlineQueueElement) clinicQueueMap.get(extractedQueueNumber);
            if (!qe.getTid().equals(tid)) {
                throw new QueueElementNotFoundException(tid);
            }
            return qe;
        }
    }

    @Override
    public String[] getClinicQueue() {
        return clinicQueue.toArray(new String[clinicQueue.size()]);
    }

    @Override
    public QueueElement createAndInsert() {
        QueueElement q = new QueueElement(queueNumberGenerator.getAndIncrement());
        clinicQueueMap.put(q.getQueueNumber(), q);
        clinicQueue.add(q.getQueueNumber());
        return q;
    }

    @Override
    public void insert(OnlineQueueElement onlineQueueElement, String refQueueNumber) throws QueueNumberAlreadyExistsException, QueueElementNotFoundException {
        String queueNumber = onlineQueueElement.getQueueNumber();
        if (clinicQueueMap.containsKey(queueNumber)) {
            throw new QueueNumberAlreadyExistsException(queueNumber);
        }
        clinicQueueMap.put(queueNumber, onlineQueueElement);
        LateRank lateRank = onlineQueueElement.getLateRank();
        if (lateRank.equals(LateRank.VERY_LATE)) {
            clinicQueue.add(queueNumber);
            return;
        } else {
            synchronized (clinicQueue) {
                int insertPos = getInsertPosition(queueNumber, refQueueNumber);
                if (onlineQueueElement.getLateRank().equals(LateRank.ON_TIME)) {
                    // do nothing
                } else if (lateRank.equals(LateRank.LITTLE_LATE)) {
                    // insert the number at the last {latePercentage} percent of the queue
                    int endPos = clinicQueue.size();
                    int delta = (int) ((endPos - insertPos) * latePercentage);
                    if (delta > 1) {
                        insertPos = clinicQueue.size() - 1 - random.nextInt(delta - 1);
                    }
                }
                clinicQueue.add(insertPos, queueNumber);
            }
        }

    }

    private int getInsertPosition(String onlineQueueNumberString, String refQueueNumber) throws QueueElementNotFoundException {
        if (refQueueNumber.equals(EMPTY_QUEUE)) {
            return 0;
        }

        ListIterator<String> iter = clinicQueue.listIterator(clinicQueue.size());
        int onlineQueueNumber = Integer.valueOf(onlineQueueNumberString.split("HB")[1]);
        while (iter.hasPrevious()) {
            int index = iter.previousIndex();
            String qnString = iter.previous();
            if (qnString.contains("HB")) {
                if (!clinicQueueMap.containsKey(qnString)) {
                    throw new QueueElementNotFoundException(qnString);
                }
                OnlineQueueElement onlineQueueElement = (OnlineQueueElement) clinicQueueMap.get(qnString);
                int qnInt = Integer.valueOf(qnString.split("HB")[1]);
                if (qnInt < onlineQueueNumber && onlineQueueElement.getLateRank().equals(LateRank.ON_TIME)
                        && onlineQueueElement.getStatus().equals(QueueStatus.ACTIVE)) {
                    return index + 1;
                }
            }

            if (qnString.equals(refQueueNumber)) {
                return index + 1;
            }
        }

        return 0;
    }

    @Override
    public QueueElement notifyQueueElement() throws EmptyQueueException {
        if (clinicQueue.size() == 0) {
            throw new EmptyQueueException();
        }
        String qnHead = clinicQueue.remove(0);
        QueueElement qe = clinicQueueMap.get(qnHead);
        qe.setStatus(QueueStatus.NOTIFIED);
        if (clinicQueue.size() > 2) {
            String qnPending = clinicQueue.get(2);
            QueueElement qePending = clinicQueueMap.get(qnPending);
        }
        // TODO Notify the HospitalBee API on calling QueueElement
        return qe;

    }

    @Override
    public void setComplete(String queueNumber) throws QueueElementNotFoundException, IllegalTransitionException {
        if (!clinicQueueMap.containsKey(queueNumber)) {
            throw new QueueElementNotFoundException(queueNumber);
        }
        QueueElement qe = clinicQueueMap.get(queueNumber);
        if (!qe.getStatus().equals(QueueStatus.NOTIFIED)) {
            throw new IllegalTransitionException(queueNumber, qe.getStatus(), QueueStatus.COMPLETED);
        }
        clinicQueueMap.remove(queueNumber);
        // TODO Notify the HospitalBee API on completed QueueElement


    }

    @Override
    public void setMissed(String queueNumber) throws QueueElementNotFoundException, IllegalTransitionException {
        if (!clinicQueueMap.containsKey(queueNumber)) {
            throw new QueueElementNotFoundException(queueNumber);
        }
        QueueElement qe = clinicQueueMap.get(queueNumber);
        if (!qe.getStatus().equals(QueueStatus.NOTIFIED)) {
            throw new IllegalTransitionException(queueNumber, qe.getStatus(), QueueStatus.MISSED);
        }

        if (qe.isReactivated()) {
            clinicQueueMap.remove(queueNumber);
            // TODO notify HospitalBee API on absent queue

        } else {
            qe.setMissedTime(clock.millis());
            qe.setStatus(QueueStatus.MISSED);
            // TODO Notify the HospitalBee API on missed QueueElement
        }


    }

    @Override
    public void reactivate(String queueNumber) throws QueueElementNotFoundException, IllegalTransitionException, MissedQueueExpiredException {
        if (!clinicQueueMap.containsKey(queueNumber)) {
            throw new QueueElementNotFoundException(queueNumber);
        }
        QueueElement qe = clinicQueueMap.get(queueNumber);
        if (!qe.getStatus().equals(QueueStatus.MISSED)) {
            throw new IllegalTransitionException(queueNumber, qe.getStatus(), QueueStatus.ACTIVE);
        }

        if (clock.millis() - qe.getMissedTime() > missTimeAllowed) {
            clinicQueueMap.remove(queueNumber);
            // TODO notify HosipitalBee for double check
            throw new MissedQueueExpiredException(queueNumber);
        }

        synchronized (clinicQueue) {
            if (clinicQueue.size() <= 2) {
                clinicQueue.add(qe.getQueueNumber());
            } else {
                int delta = (int) (clinicQueue.size() * missPercentage);
                int insertPos = clinicQueue.size() - random.nextInt(delta);
                clinicQueue.add(insertPos, qe.getQueueNumber());
            }
        }

        qe.setStatus(QueueStatus.ACTIVE);
        qe.setReactivated(true);
    }

    @Override
    public QueueElement peekLast() {
        if (clinicQueue.size() == 0) {
            return new QueueElement(EMPTY_QUEUE);
        }
        return clinicQueueMap.get(clinicQueue.get(clinicQueue.size() - 1));
    }

    @Override
    public int getLength() {
        return clinicQueue.size();
    }

    @Override
    public int getLengthFrom(String queueNumber) throws QueueElementNotFoundException {
        int pos = 0;
        synchronized (clinicQueue) {
            for (String qn : clinicQueue) {
                if (qn.equals(queueNumber)) {
                    return pos;
                }
                pos++;
            }
        }
        throw new QueueElementNotFoundException(queueNumber);
    }

    @Override
    public void reset() {
        queueNumberGenerator.set(0);
        // TODO notify HospitalBee on remaining online queue numbers

        clinicQueue.clear();
        clinicQueueMap.clear();

    }
}
