package com.genie.common.settings;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PlatformSetting s WHERE s.settingKey = :key")
    Optional<PlatformSetting> findByKeyForUpdate(@Param("key") String key);
}
