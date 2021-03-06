package sg.edu.ntu.hospitalbeesqdemo.repository;

import sg.edu.ntu.hospitalbeesqdemo.exceptions.*;
import sg.edu.ntu.hospitalbeesqdemo.model.OnlineQueueElement;
import sg.edu.ntu.hospitalbeesqdemo.model.QueueElement;

/**
 * The repository that manages all the QueueElement Numbers
 */
public interface QueueRepository {

    /**
     * Find the queue element of the corresponding queue number
     *
     * @param queueNumber specifies the queue number
     * @return the queue element associated with that number
     */
    QueueElement findQueueElementByNumber(String queueNumber) throws QueueElementNotFoundException;

    /**
     * Find the onlinequeue element of the corresponding tid
     *
     * @param tid specifies the tid
     * @return the queue element associated with that number
     */
    OnlineQueueElement findQueueElementByTid(String tid) throws QueueElementNotFoundException;

    /**
     * Get the Entire Clinic Queue
     *
     * @return the current clinic queue
     */
    String[] getClinicQueue();

    /**
     * Create a QueueElement for bookings at the clinic and insert it into the tail of the clinicQueue
     *
     * @return the QueueElement that is created
     */
    QueueElement createAndInsert();

    /**
     * Insert an OnlineQueueElement into the clinicQueue,
     * the online booking is created just as if the User is physically present at the clinic
     *
     * @param onlineQueueElement OnlineQueueElement obtained in {@link sg.edu.ntu.hospitalbeesqdemo.web.QueuesController}
     * @param refQueueNumber     the reference queue number used to determine where to insert onlineQueueElement
     *                           if value is "NO_TAIL", it implies that HospitalBee find an empty queue {@link sg.edu.ntu.hospitalbeesqdemo.exceptions.EmptyQueueException}
     *                           while peeking the tail of the queue when creating the online queue number
     */
    void insert(OnlineQueueElement onlineQueueElement, String refQueueNumber) throws QueueNumberAlreadyExistsException, QueueElementNotFoundException;

    /**
     * Reactivate the Queue Number if still exist in the clinicQueue
     *
     * @param queueNumber the QueueElement with the number in the missedQueue
     */
    void reactivate(String queueNumber) throws QueueElementNotFoundException, IllegalTransitionException, MissedQueueExpiredException;

    /**
     * Remove the head and set it as the pending queue element, notify the QueueElement if it is online booking
     * Also notify the second QueueElement in the updated queue
     */
    QueueElement[] notifyQueueElement() throws EmptyQueueException;

    /**
     * Remove the specified QueueElement and notify HospitalBee on the completed booking
     *
     * @param queueNumber to find the corresponding QueueElement
     */
    QueueElement setComplete(String queueNumber) throws QueueElementNotFoundException, IllegalTransitionException;

    /**
     * Set the specified element as the missed and notify HospitalBee on missed queue
     *
     * @param queueNumber to find the corresponding QueueElement
     */
    void setMissed(String queueNumber) throws QueueElementNotFoundException, IllegalTransitionException;


    /**
     * Get the last element of the queue
     *
     * @return the QueueELement of the last element
     */
    QueueElement peekLast();

    /**
     * Get the length of the queue
     *
     * @return the length of the queue
     */
    int getLength();

    /**
     * See how many list element before the specified QueueElement
     *
     * @param queueElement the QueueElement to be queried
     * @return the number of list item
     */
    int getLengthFrom(String queueElement) throws QueueElementNotFoundException;

    /**
     * Reset the Queue Controller after end of clinic operation, mark remaining missedQueue as absent and notify the HospitalBee
     */
    void reset();


}
