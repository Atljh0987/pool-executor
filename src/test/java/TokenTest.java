import org.example.DBEmulator;
import org.example.ThreadSynchronizer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class TokenTest {
    private DBEmulator dbEmulator = new DBEmulator();
    private ThreadSynchronizer threadSynchronizer = new ThreadSynchronizer();

    @Test
    void test1() {
        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for(int i = 0; i < 5; i++) {
            executorService.submit(() -> threadSynchronizer.start());
        }

        await().atMost(10, TimeUnit.SECONDS).until(() -> dbEmulator.hasToken());
    }
}
