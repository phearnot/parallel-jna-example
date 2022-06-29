package com.wavesplatform;

import com.google.common.primitives.Longs;
import com.protonail.leveldb.jna.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestApp {
    static final int THREAD_COUNT = 4;

    static class IteratorTask implements Runnable {
        private final LevelDB db;
        private final int id;
        private final CountDownLatch latch;

        IteratorTask(LevelDB db, int id, CountDownLatch latch) {
            this.db = db;
            this.id = id;
            this.latch = latch;
        }

        @Override
        public void run() {
            System.out.println(id + " Computing sum");
            long sum = 0L;
            try (LevelDBReadOptions options = new LevelDBReadOptions(); LevelDBKeyValueIterator iter = new LevelDBKeyValueIterator(db, options)) {
                while (iter.hasNext()) {
                    KeyValuePair pair = iter.next();
                    sum += Longs.fromByteArray(pair.getValue());
                }
            }
            System.out.println(id + " Sum is " + sum);
            latch.countDown();
        }
    }

    public static void main(String[] args) {
        try (LevelDBOptions options = new LevelDBOptions()) {
            options.setCreateIfMissing(true);
            try (LevelDB db = new LevelDB("leveldb", options)) {
                System.out.println("Populating DB...");
                try (LevelDBWriteBatch batch = new LevelDBWriteBatch(); LevelDBWriteOptions opts = new LevelDBWriteOptions()) {
                    for (int i = 1; i < 500_000; i++) {
                        batch.put(Longs.toByteArray(i), Longs.toByteArray(System.nanoTime()));
                    }

                    db.write(batch, opts);
                }

                System.out.println("Finished populating DB");

                CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
                ExecutorService e = Executors.newFixedThreadPool(THREAD_COUNT);
                for (int i = 0; i < THREAD_COUNT; i++)
                    e.execute(new IteratorTask(db, i, latch));

                try {
                    latch.await();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                e.shutdown();
            }
        }
    }
}
