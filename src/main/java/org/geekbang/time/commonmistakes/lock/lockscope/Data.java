package org.geekbang.time.commonmistakes.lock.lockscope;

import lombok.Getter;

class Data {
    @Getter
    private static int counter = 0;
    private static Object locker = new Object();

    public static int reset() {
        counter = 0;
        return counter;
    }

    // 实例锁
    public synchronized void wrong() {
        counter++;
    }

    // 类锁  静态变量字段属于类级别 ,需要类锁
    public void right() {
        synchronized (locker) {
            counter++;
        }
    }
}