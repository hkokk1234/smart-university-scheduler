package com.smartuniversity.scheduler.catalog.infrastructure;

import com.smartuniversity.scheduler.catalog.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    Optional<Room> findByBuildingAndCode(String building, String code);
}
