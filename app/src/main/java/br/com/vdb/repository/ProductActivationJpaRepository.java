package br.com.vdb.repository;

import br.com.vdb.entity.ProductActivationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductActivationJpaRepository extends JpaRepository<ProductActivationEntity, UUID> {

    boolean existsByExternalId(String externalId);
}
