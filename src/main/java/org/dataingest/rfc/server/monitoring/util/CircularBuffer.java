package org.dataingest.rfc.server.monitoring.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe circular buffer for time-windowed metrics
 */
public class CircularBuffer<T> {
    private final Object[] buffer;
    private final int capacity;
    private final AtomicInteger writeIndex = new AtomicInteger(0);

    public CircularBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Object[capacity];
    }

    public void add(T item) {
        int index = writeIndex.getAndIncrement() % capacity;
        buffer[index] = item;
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= capacity) {
            return null;
        }
        return (T) buffer[index];
    }

    public int size() {
        int written = writeIndex.get();
        return Math.min(written, capacity);
    }

    public int capacity() {
        return capacity;
    }

    public void clear() {
        writeIndex.set(0);
        for (int i = 0; i < capacity; i++) {
            buffer[i] = null;
        }
    }

    /**
     * Get all non-null entries
     */
    @SuppressWarnings("unchecked")
    public Object[] getAllEntries() {
        int size = size();
        Object[] result = new Object[size];
        for (int i = 0; i < size; i++) {
            result[i] = buffer[i];
        }
        return result;
    }
}
