package susMQ;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unchecked")
public class SusQueue<T> implements BlockingQueue<T> {

    private final int capacity;
    private final Queue<T> queue = new LinkedList<>();
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public SusQueue(int capacity) {
        this.capacity = capacity;
    }

    /**
     * Always calls put(t)
     * @param t the element to add
     * @return put(t)
     */
    @Override
    public boolean add(T t) {
        try {
            put(t);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * Always calls add(t) because of capacity.
     * @param t the element to offer
     * @return add(t)
     */
    @Override
    public boolean offer(T t) {
        return add(t);
    }

    /**
     * Retrieves and removes the head of this queue.
     * This method differs from poll() only in that it throws an exception if this queue is empty.
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    @Override
    public T remove() {
        lock.lock();
        T t;
        try {
            while (queue.isEmpty()) notEmpty.await();
            t = queue.remove();
            notFull.signal();
        } catch (InterruptedException e) {
            t = null;
        } finally {
            lock.unlock();
        }
        return t;
    }

    /**
     * Retrieves and removes the head of this queue, or returns null if this queue is empty.
     * @return the head of this queue, or null if this queue is empty
     */
    @Override
    public T poll() {
        lock.lock();
        T t;
        try {
            while (queue.isEmpty()) notEmpty.await();
            t = queue.poll();
            notFull.signal();
        } catch (InterruptedException e) {
            t = null;
        } finally {
            lock.unlock();
        }
        return t;
    }

    /**
     * Retrieves, but does not remove, the head of this queue. This method differs from peek only in that it throws an exception if this queue is empty.
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    @Override
    public T element() {
        lock.lock();
        T t;
        try {
            while (queue.isEmpty()) notEmpty.await();
            t = queue.element();
            notFull.signal();
        } catch (InterruptedException e) {
            t = null;
        } finally {
            lock.unlock();
        }
        return t;
    }

    /**
     * Retrieves, but does not remove, the head of this queue, or returns null if this queue is empty.
     * @return the head of this queue, or null if this queue is empty
     */
    @Override
    public T peek() {
        lock.lock();
        T t;
        try {
            while (queue.isEmpty()) notEmpty.await();
            t = queue.peek();
            notFull.signal();
        } catch (InterruptedException e) {
            t = null;
        } finally {
            lock.unlock();
        }
        return t;
    }

    /**
     * Inserts the specified element into this queue if it is possible to do so immediately without violating capacity restrictions, returning true upon success and throwing an IllegalStateException if no space is currently available.
     * @param t the element to add
     * @throws InterruptedException if the current thread is interrupted (and interruption of thread suspension is supported)
     * @throws IllegalArgumentException if some property of this element prevents it from being added to this queue
     * @throws IllegalStateException if the element cannot be added at this time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null and this queue does not permit null elements
     * @throws IllegalMonitorStateException if the current thread doesn't hold the lock associated with this Condition when this method is called
     */
    @Override
    public void put(T t) throws InterruptedException, IllegalArgumentException, IllegalStateException,
                                ClassCastException,   NullPointerException,     IllegalMonitorStateException {
        lock.lock();
        try {
            while (queue.size() == capacity) notFull.await();
            queue.add(t);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * offer(t) after waiting for 'l' 'timeUnit'
     * @param t the element to offer
     * @param l the time to wait before the action
     * @param timeUnit the unit of time
     * @return true if the element has been added or throws the exception
     * @throws InterruptedException if the current thread is interrupted while acquiring the lock (and interruption of lock acquisition is supported
     */
    @Override
    public boolean offer(T t, long l, TimeUnit timeUnit) throws InterruptedException {
        if (lock.tryLock(l, timeUnit)) add(t);
        else throw new InterruptedException("The waiting time elapsed before the lock was acquired");
        return true;
    }

    /**
     * @return remove()
     */
    @Override
    public T take() {
        return remove();
    }

    /**
     * poll() after waiting for 'l' 'timeUnit'
     * @param l the time to wait before the action
     * @param timeUnit the unit of time
     * @return true if the element has been added or throws the exception
     * @throws InterruptedException if the current thread is interrupted while acquiring the lock (and interruption of lock acquisition is supported
     */
    @Override
    public T poll(long l, TimeUnit timeUnit) throws InterruptedException {
        if (lock.tryLock(l, timeUnit)) return poll();
        else throw new InterruptedException("The waiting time elapsed before the lock was acquired");
    }

    /**
     * @return the remaining capacity of the queue.
     */
    @Override
    public int remainingCapacity() {
        return capacity - queue.size();
    }

    /**
     * Removes a single instance of the specified element from this collection, if it is present (optional operation). More formally, removes an element e such that Objects.equals(o, e), if this collection contains one or more such elements. Returns true if this collection contained the specified element (or equivalently, if this collection changed as a result of the call).
     * @param t the element to be removed from this collection, if present
     * @return true if an element was removed as a result of this call
     */
    @Override
    public boolean remove(Object t) {
        lock.lock();
        boolean flag = false;
        try {
            while (queue.isEmpty()) notEmpty.await();
            flag = queue.remove(t);
            notFull.signal();
        } catch (Exception ignored) {
        } finally {
            lock.unlock();
        }
        return flag;
    }

    /**
     * Adds all the elements in the specified collection to this collection (optional operation). The behavior of this operation is undefined if the specified collection is modified while the operation is in progress. (This implies that the behavior of this call is undefined if the specified collection is this collection, and this collection is nonempty.)
     * @param collection the collection containing elements to be added to this collection
     * @returntrue if this collection changed as a result of the call
     */
    @Override
    public boolean addAll(Collection collection) {
        lock.lock();
        boolean flag = false;
        try {
            while (queue.isEmpty()) notEmpty.await();
            flag = queue.addAll(collection);
            notFull.signal();
        } catch (Exception ignored) {
        } finally {
            lock.unlock();
        }
        return flag;
    }

    /**
     * Removes all the elements from this collection (optional operation). The collection will be empty after this method returns.
     */
    @Override
    public void clear() {
        lock.lock();
        try {
            while (queue.isEmpty()) notEmpty.await();
            queue.clear();
            notFull.signal();
        } catch (Exception ignored) {
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retains only the elements in this collection that are contained in the specified collection (optional operation). In other words, removes from this collection all of its elements that are not contained in the specified collection.
     * @param collection the collection containing elements to be retained in this collection
     * @return true if this collection changed as a result of the call
     */
    @Override
    public boolean retainAll(Collection collection) {
        lock.lock();
        boolean flag = false;
        try {
            while (queue.isEmpty()) notEmpty.await();
            flag = queue.retainAll(collection);
            notFull.signal();
        } catch (Exception ignored) {
        } finally {
            lock.unlock();
        }
        return flag;
    }

    /**
     * Removes all of this collection's elements that are also contained in the specified collection (optional operation). After this call returns, this collection will contain no elements in common with the specified collection.
     * @param collection the collection containing elements to be removed from this collection
     * @return true if this collection changed as a result of the call
     */
    @Override
    public boolean removeAll(Collection collection) {
        lock.lock();
        boolean flag = false;
        try {
            while (queue.isEmpty()) notEmpty.await();
            flag = queue.removeAll(collection);
            notFull.signal();
        } catch (Exception ignored) {
        } finally {
            lock.unlock();
        }
        return flag;
    }

    @Override
    public boolean containsAll(Collection collection) {
        lock.lock();
        boolean flag = false;
        try {
            while (queue.isEmpty()) notEmpty.await();
            flag = queue.containsAll(collection);
            notFull.signal();
        } catch (InterruptedException ignored) {
        } finally {
            lock.unlock();
        }
        return flag;
    }

    @Override
    public int size() {
        return queue.size();
    }

    public int getCapacity() {
        return this.capacity;
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return queue.iterator();
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public Object[] toArray(Object[] objects) {
        return queue.toArray(objects);
    }

    @Override
    public int drainTo(Collection collection) {
        collection.addAll(queue);
        clear();
        return collection.size();
    }

    @Override
    public int drainTo(Collection collection, int i) {
        while (size() > i) remove();
        return drainTo(collection);
    }
}
