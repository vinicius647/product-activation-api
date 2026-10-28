package br.com.vdb.adapter;

import br.com.vdb.core.port.in.CreateActivationCommand;
import br.com.vdb.core.port.in.CreateActivationResult;
import br.com.vdb.core.port.in.CreateActivationUseCase;
import br.com.vdb.entity.ActivationRequestEntity;
import br.com.vdb.repository.ActivationRequestJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Many equal requests at the same time must create exactly one row. Runs against the real
 * database schema (H2 with the Flyway migration), so the unique constraint is what is tested.
 */
@SpringBootTest(properties = "app.activation.scheduler.enabled=false")
class ConcurrentCreationTest {

    private static final int THREADS = 10;

    @Autowired
    private CreateActivationUseCase createActivation;

    @Autowired
    private ActivationRequestJpaRepository repository;

    @Test
    void parallelRequestsWithSameExternalIdCreateOneRow() throws Exception {
        String externalId = "EXT-" + UUID.randomUUID();
        var command = new CreateActivationCommand(externalId, "CUST-1", "INTERNET-500", "client-a", null);
        CountDownLatch start = new CountDownLatch(1);

        List<CreateActivationResult> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
            List<Future<CreateActivationResult>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return createActivation.create(command);
                }));
            }
            start.countDown();
            for (Future<CreateActivationResult> future : futures) {
                results.add(future.get());
            }
        }

        assertThat(results).filteredOn(CreateActivationResult::created).hasSize(1);
        assertThat(results).extracting(result -> result.request().id()).containsOnly(results.getFirst().request().id());
        assertThat(repository.findAll().stream().map(ActivationRequestEntity::toDomain)
                .filter(request -> request.externalId().equals(externalId))).hasSize(1);
    }
}
