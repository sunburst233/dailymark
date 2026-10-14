package com.dailymark.repository;

import com.dailymark.entity.Asset;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, UUID> {}
