package com.genie.notification.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformNotificationRepository extends JpaRepository<PlatformNotification, Long> {
}
