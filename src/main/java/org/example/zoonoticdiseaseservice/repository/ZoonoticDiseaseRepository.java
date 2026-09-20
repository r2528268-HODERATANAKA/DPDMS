package org.example.zoonoticdiseaseservice.repository;

import org.example.zoonoticdiseaseservice.entity.ZoonoticDisease;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoonoticDiseaseRepository extends JpaRepository<ZoonoticDisease, Long> {
}

