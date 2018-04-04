package sg.edu.ntu.hospitalbeesqdemo.repository;

import org.junit.Test;
import sg.edu.ntu.hospitalbeesqdemo.exceptions.*;
import sg.edu.ntu.hospitalbeesqdemo.model.LateRank;
import sg.edu.ntu.hospitalbeesqdemo.model.OnlineQueueElement;
import sg.edu.ntu.hospitalbeesqdemo.model.QueueStatus;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static org.junit.Assert.*;

public class InMemoryQueueRepositoryTest {

    private Clock mockClock;
    private QueueRepository createQueueRepositoryWithTenElements() {
        QueueRepository queueRepository = new InMemoryQueueRepository(30,1.0,1.0, Clock.systemUTC());
        for (int i = 0; i < 10; i++) {
            queueRepository.createAndInsert();
        }
        return queueRepository;
    }

    private QueueRepository createQueueRepositoryWithFakeClock() {
        mockClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        QueueRepository queueRepository = new InMemoryQueueRepository(30,1.0,1.0, mockClock);
        for (int i = 0; i < 10; i++) {
            queueRepository.createAndInsert();
        }
        return queueRepository;
    }

    private QueueRepository createEmptyQueueRepository() {
        return new InMemoryQueueRepository(30,1.0,1.0, Clock.systemUTC());
    }

    @Test
    public void testNotifyQueue() throws QueueElementNotFoundException, EmptyQueueException, QueueNumberAlreadyExistsException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();

        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement, "0000");
        queueRepository.notifyQueueElement();
        queueRepository.notifyQueueElement();

        assertEquals(9, queueRepository.getLength());
        assertEquals("0001", queueRepository.getClinicQueue()[0]);
        assertEquals(queueRepository.findQueueElementByNumber("0000").getStatus(), QueueStatus.NOTIFIED);
        assertEquals(queueRepository.findQueueElementByNumber("HB0000").getStatus(), QueueStatus.NOTIFIED);
    }

    @Test(expected = EmptyQueueException.class)
    public void testNotifyEmptyQueue() throws EmptyQueueException {
        QueueRepository queueRepository = createEmptyQueueRepository();
        queueRepository.notifyQueueElement();
    }

    @Test(expected = QueueElementNotFoundException.class)
    public void testSetComplete() throws QueueNumberAlreadyExistsException, QueueElementNotFoundException, EmptyQueueException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement, "0000");
        queueRepository.notifyQueueElement();
        queueRepository.notifyQueueElement();
        try {
            queueRepository.setComplete("0000");
            queueRepository.setComplete("HB0000");
        } catch (Exception e) {
            e.printStackTrace();
        }

        queueRepository.findQueueElementByNumber("HB0000");
        queueRepository.findQueueElementByNumber("0000");

    }

    @Test(expected = IllegalTransitionException.class)
    public void testSetCompleteWithoutNotify() throws QueueElementNotFoundException, IllegalTransitionException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        queueRepository.setComplete("0000");
    }

    @Test
    public void testSetMissed() throws QueueNumberAlreadyExistsException, QueueElementNotFoundException, EmptyQueueException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement, "0000");
        queueRepository.notifyQueueElement();
        queueRepository.notifyQueueElement();
        try {
            queueRepository.setComplete("0000");
            queueRepository.setMissed("HB0000");
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertNotEquals(0,queueRepository.findQueueElementByNumber("HB0000").getMissedTime());
        assertEquals(QueueStatus.MISSED, queueRepository.findQueueElementByNumber("HB0000").getStatus());
        assertTrue(!Arrays.asList(queueRepository.getClinicQueue()).contains("HB0000"));

    }

    @Test(expected = IllegalTransitionException.class)
    public void testSetMissedWithoutNotify() throws QueueElementNotFoundException, IllegalTransitionException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        queueRepository.setMissed("0000");
    }
    
    @Test
    public void testFindQueueByTid() throws QueueElementNotFoundException, QueueNumberAlreadyExistsException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        OnlineQueueElement qe = new OnlineQueueElement("HB0007","00012018040315000007", LateRank.ON_TIME);
        queueRepository.insert(qe, "0007");
        assertEquals(qe, queueRepository.findQueueElementByTid("00012018040315000007"));
    }

    @Test(expected = QueueElementNotFoundException.class)
    public void testFindQueueByTidMismatches() throws QueueElementNotFoundException, QueueNumberAlreadyExistsException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        OnlineQueueElement qe = new OnlineQueueElement("HB0007","00012018040315000006", LateRank.ON_TIME);
        queueRepository.insert(qe, "0007");
        assertEquals(qe, queueRepository.findQueueElementByTid("00012018040315000007"));
    }

    ////////// ***** INSERTION TESTS ***** //////////

    @Test
    public void testNormalInsertion() {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();

        assertEquals(10, queueRepository.getLength());
//        System.out.println(Arrays.toString(queueRepository.getClinicQueue()));
    }

    @Test
    public void testConcurrentInsertion() throws InterruptedException, QueueElementNotFoundException {
        QueueRepository queueRepository = createEmptyQueueRepository();
        queueRepository.createAndInsert();
        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        OnlineQueueElement onlineQueueElement1 = new OnlineQueueElement(1, "0001", LateRank.ON_TIME);
        Thread[] workers = new Thread[5];
        workers[0] = new Thread(() -> queueRepository.createAndInsert());
        workers[1] = new Thread(() -> queueRepository.createAndInsert());
        workers[2] = new Thread(() -> {
            try {
                queueRepository.insert(onlineQueueElement, "0000");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        workers[3] = new Thread(() -> {
            try {
                queueRepository.insert(onlineQueueElement1, "0000");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        workers[4] = new Thread(() -> {
            try {
                queueRepository.getLengthFrom("0000");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        for (Thread worker : workers) {
            worker.start();
        }

        for (Thread worker : workers) {
            worker.join();
        }

        assertEquals(5, queueRepository.getLength());
        assertEquals(1, queueRepository.getLengthFrom("HB0000"));
        assertEquals(2, queueRepository.getLengthFrom("HB0001"));
    }

    @Test
    public void testNoTailOnlineInsertion() throws QueueNumberAlreadyExistsException, QueueElementNotFoundException {
        QueueRepository queueRepository = createEmptyQueueRepository();
        String[] expected = {"HB0000"};
        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement, "NO_TAIL");
        assertArrayEquals(expected, queueRepository.getClinicQueue());

        queueRepository.reset();
        onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.LITTLE_LATE);
        queueRepository.insert(onlineQueueElement, "NO_TAIL");
        assertArrayEquals(expected, queueRepository.getClinicQueue());

        queueRepository.reset();
        onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.VERY_LATE);
        queueRepository.insert(onlineQueueElement, "NO_TAIL");
        assertArrayEquals(expected, queueRepository.getClinicQueue());

    }

    @Test(expected = QueueNumberAlreadyExistsException.class)
    public void testDuplicateOnlineInsertion() throws QueueNumberAlreadyExistsException, QueueElementNotFoundException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement, "0004");
        assertEquals(11, queueRepository.getLength());
        assertEquals(5, queueRepository.getLengthFrom("HB0000"));

        OnlineQueueElement onlineQueueElement1 = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement1, "0008");
    }

    @Test
    public void testOnTimeOnlineInsertionWithRefQ() throws QueueElementNotFoundException, QueueNumberAlreadyExistsException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();

        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement, "0004");
        assertEquals(11, queueRepository.getLength());
        assertEquals(5, queueRepository.getLengthFrom("HB0000"));

        OnlineQueueElement onlineQueueElement1 = new OnlineQueueElement(1, "0001", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement1, "0004");
        assertEquals(12, queueRepository.getLength());
        assertEquals(6, queueRepository.getLengthFrom("HB0001"));

//        System.out.println(Arrays.toString(queueRepository.getClinicQueue()));
    }

    @Test
    public void testVeryLateOnlineInsertionWithRefQ() throws QueueElementNotFoundException, QueueNumberAlreadyExistsException, EmptyQueueException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();

        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.VERY_LATE);
        queueRepository.insert(onlineQueueElement, "0004");
        assertEquals(11, queueRepository.getLength());
        assertEquals(queueRepository.peekLast(), onlineQueueElement);

        OnlineQueueElement onlineQueueElement1 = new OnlineQueueElement(1, "0001", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement1, "0004");
        assertEquals(12, queueRepository.getLength());
        assertEquals(5, queueRepository.getLengthFrom("HB0001"));
//        System.out.println(Arrays.toString(queueRepository.getClinicQueue()));
    }

    @Test
    public void testLittleLateOnlineInsertionWithRefQ() throws QueueElementNotFoundException, QueueNumberAlreadyExistsException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();

        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.LITTLE_LATE);
        queueRepository.insert(onlineQueueElement, "0004");
        assertEquals(11, queueRepository.getLength());
        int len = queueRepository.getLengthFrom("HB0000");
        assertTrue(5 < len && len < 10);

        OnlineQueueElement onlineQueueElement1 = new OnlineQueueElement(1, "0001", LateRank.LITTLE_LATE);
        queueRepository.insert(onlineQueueElement1, "0004");
        assertEquals(12, queueRepository.getLength());
        len = queueRepository.getLengthFrom("HB0001");
        assertTrue(5 < len && len < 11);

        OnlineQueueElement onlineQueueElement2 = new OnlineQueueElement(2, "0002", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement2, "0004");
        assertEquals(13, queueRepository.getLength());
        assertEquals(5, queueRepository.getLengthFrom("HB0002"));

//        System.out.println(Arrays.toString(queueRepository.getClinicQueue()));
    }

    @Test
    public void testOnTimeOnlineInsertionNotFoundRefQ() throws QueueElementNotFoundException, EmptyQueueException, QueueNumberAlreadyExistsException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();

        for (int i = 0; i < 5; i++) {
            queueRepository.notifyQueueElement();
        }

        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement, "0004");
        assertEquals(6, queueRepository.getLength());
        assertEquals(0, queueRepository.getLengthFrom("HB0000"));
    }

    @Test
    public void testVeryLateOnlineInsertionNotFoundRefQ() throws QueueElementNotFoundException, EmptyQueueException, QueueNumberAlreadyExistsException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        for (int i = 0; i < 5; i++) {
            queueRepository.notifyQueueElement();
        }

        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.VERY_LATE);
        queueRepository.insert(onlineQueueElement, "0004");
        assertEquals(6, queueRepository.getLength());
        assertEquals("HB0000", queueRepository.peekLast().getQueueNumber());

        OnlineQueueElement onlineQueueElement1 = new OnlineQueueElement(1, "0001", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement1, "0004");
        assertEquals(7, queueRepository.getLength());
        assertEquals(0, queueRepository.getLengthFrom("HB0001"));
    }

    @Test
    public void testLittleLateOnlineInsertionNotFoundRefQ() throws QueueElementNotFoundException, QueueNumberAlreadyExistsException, EmptyQueueException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        for (int i = 0; i < 5; i++) {
            queueRepository.notifyQueueElement();
        }

        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.LITTLE_LATE);
        queueRepository.insert(onlineQueueElement, "0004");
        assertEquals(6, queueRepository.getLength());
        int len = queueRepository.getLengthFrom("HB0000");
        assertTrue(0 < len && len < 5);

        OnlineQueueElement onlineQueueElement1 = new OnlineQueueElement(1, "0001", LateRank.LITTLE_LATE);
        queueRepository.insert(onlineQueueElement1, "0004");
        assertEquals(7, queueRepository.getLength());
        len = queueRepository.getLengthFrom("HB0001");
        assertTrue(0 < len && len < 6);

        OnlineQueueElement onlineQueueElement2 = new OnlineQueueElement(2, "0002", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement2, "0004");
        assertEquals(8, queueRepository.getLength());
        assertEquals(0, queueRepository.getLengthFrom("HB0002"));
//        System.out.println(Arrays.toString(queueRepository.getClinicQueue()));
    }

    ////////// ***** REACTIVATION TESTS ***** //////////
    @Test
    public void testNormalReactivate() throws QueueElementNotFoundException, IllegalTransitionException, EmptyQueueException, QueueNumberAlreadyExistsException, MissedQueueExpiredException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement, "0000");
        queueRepository.notifyQueueElement();
        queueRepository.notifyQueueElement();
        queueRepository.setMissed("0000");
        queueRepository.setMissed("HB0000");

        queueRepository.reactivate("0000");
        queueRepository.reactivate("HB0000");
        assertTrue(queueRepository.findQueueElementByNumber("0000").isReactivated());
        assertTrue(queueRepository.findQueueElementByNumber("HB0000").isReactivated());
        assertEquals(QueueStatus.ACTIVE, queueRepository.findQueueElementByNumber("0000").getStatus());
        assertEquals(QueueStatus.ACTIVE, queueRepository.findQueueElementByNumber("HB0000").getStatus());

        assertEquals(11, queueRepository.getLength());
        int len = queueRepository.getLengthFrom("0000");
        assertTrue(len > 0);
        len = queueRepository.getLengthFrom("HB0000");
        assertTrue(len > 0);
    }

    @Test(expected = IllegalTransitionException.class)
    public void testReactivateWithoutNotify() throws IllegalTransitionException, QueueNumberAlreadyExistsException, QueueElementNotFoundException, MissedQueueExpiredException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement, "0000");
        queueRepository.reactivate("HB0000");
    }

    @Test(expected = IllegalTransitionException.class)
    public void testReactivateWithoutMissed() throws IllegalTransitionException, QueueNumberAlreadyExistsException, QueueElementNotFoundException, EmptyQueueException, MissedQueueExpiredException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement, "0000");
        queueRepository.notifyQueueElement();
        queueRepository.notifyQueueElement();
        queueRepository.reactivate("HB0000");
    }

    @Test(expected = QueueElementNotFoundException.class)
    public void testReactivateNotFound() throws QueueElementNotFoundException, IllegalTransitionException, EmptyQueueException, MissedQueueExpiredException {
        QueueRepository queueRepository = createQueueRepositoryWithTenElements();
        queueRepository.notifyQueueElement();
        queueRepository.setComplete("0000");
        assertEquals(9, queueRepository.getLength());
        queueRepository.reactivate("0000");
    }

    @Test(expected = MissedQueueExpiredException.class)
    public void testReactivateExpired() throws QueueElementNotFoundException, IllegalTransitionException, EmptyQueueException, MissedQueueExpiredException {
        QueueRepository queueRepository = createQueueRepositoryWithFakeClock();
        queueRepository.notifyQueueElement();
        queueRepository.setMissed("0000");
        assertEquals(9, queueRepository.getLength());
        queueRepository.findQueueElementByNumber("0000").setMissedTime(mockClock.instant().minus(31, ChronoUnit.MINUTES).toEpochMilli());
        queueRepository.reactivate("0000");
    }

    @Test(expected = QueueElementNotFoundException.class)
    public void testMissedAfterReactivate() throws QueueNumberAlreadyExistsException, QueueElementNotFoundException, EmptyQueueException, IllegalTransitionException, MissedQueueExpiredException {
        QueueRepository queueRepository = createEmptyQueueRepository();
        queueRepository.createAndInsert();
        OnlineQueueElement onlineQueueElement = new OnlineQueueElement(0, "0000", LateRank.ON_TIME);
        queueRepository.insert(onlineQueueElement, "0000");
        queueRepository.notifyQueueElement();
        queueRepository.notifyQueueElement();
        queueRepository.setMissed("0000");
        queueRepository.setMissed("HB0000");
        queueRepository.reactivate("HB0000");
        queueRepository.reactivate("0000");

        queueRepository.notifyQueueElement();
        queueRepository.setMissed("HB0000");
        assertEquals(1, queueRepository.getLength());
        queueRepository.findQueueElementByNumber("HB0000");
    }
}