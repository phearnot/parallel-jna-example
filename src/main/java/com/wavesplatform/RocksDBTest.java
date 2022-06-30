package com.wavesplatform;

import com.google.common.primitives.Longs;
import org.rocksdb.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RocksDBTest {
    static final int THREAD_COUNT = 8;

    static class IteratorTask implements Runnable {
        private final RocksDB db;
        private final int id;
        private final CountDownLatch latch;

        IteratorTask(RocksDB db, int id, CountDownLatch latch) {
            this.db = db;
            this.id = id;
            this.latch = latch;
        }

        @Override
        public void run() {
            System.out.println(id + " Computing sum");
            long sum = 0L;
            try (RocksIterator iter = db.newIterator()) {
                iter.seekToFirst();
                while (iter.isValid()) {
                    sum += Longs.fromByteArray(iter.value());
                    iter.next();
                }
            }
            System.out.println(id + " Sum is " + sum);
            latch.countDown();
        }
    }

    public static void main(String[] args) {
        RocksDB.loadLibrary();

        try (final Options options = new Options().setCreateIfMissing(true)) {
            System.out.println("Populating DB...");
            try (final RocksDB db = RocksDB.open(options, "rocksdb")) {
                try (final WriteBatch batch = new WriteBatch(); final WriteOptions opts = new WriteOptions()) {
                    for (int i = 1; i < 1_000_000; i++) {
                        batch.put(Longs.toByteArray(i), Longs.toByteArray(System.nanoTime()));
                    }

                    db.write(opts, batch);
                }
                try (final FlushOptions fo = new FlushOptions()) {
                    fo.setWaitForFlush(true);
                    db.flush(fo);
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
        } catch (RocksDBException e) {

        }
    }
}
