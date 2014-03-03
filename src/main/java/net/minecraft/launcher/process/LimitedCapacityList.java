package net.minecraft.launcher.process;

import java.lang.reflect.Array;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LimitedCapacityList<T> {
    private final T[] items;
    private final Class<? extends T> clazz;
    private final ReadWriteLock locks;
    private int size;
    private int head;

    public LimitedCapacityList(final Class<? extends T> clazz, final int maxSize, final int minSize) {
        super();
        this.locks = new ReentrantReadWriteLock();
        this.clazz = clazz;
        this.items = (T[]) Array.newInstance(clazz, maxSize);
    }

    public T add(final T value) {
        this.locks.writeLock().lock();
        this.items[this.head] = value;
        this.head = (this.head + 1) % this.getMaxSize();
        if (this.size < this.getMaxSize()) {
            ++this.size;
        }
        this.locks.writeLock().unlock();
        return value;
    }

    public int getSize() {
        this.locks.readLock().lock();
        final int result = this.size;
        this.locks.readLock().unlock();
        return result;
    }

    public int getMaxSize() {
        this.locks.readLock().lock();
        final int result = this.items.length;
        this.locks.readLock().unlock();
        return result;
    }

    public T[] getItems() {
        final T[] result = (T[]) Array.newInstance(this.clazz, this.size);
        this.locks.readLock().lock();
        for (int i = 0; i < this.size; ++i) {
            int pos = (this.head - this.size + i) % this.getMaxSize();
            if (pos < 0) {
                pos += this.getMaxSize();
            }
            result[i] = (T) this.items[pos];
        }
        this.locks.readLock().unlock();
        return result;
    }
}
