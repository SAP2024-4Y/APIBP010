package com.hospital.hospital.repository;

import com.hospital.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface HospitalRepository
        extends JpaRepository<Hospital, UUID>, JpaSpecificationExecutor<Hospital> {

    @Query(value = """
            SELECT * FROM hospitals
            WHERE ST_DWithin(
                location,
                ST_SetSRID(ST_Point(:longitude, :latitude), 4326)::geography,
                :radiusMetres
            )
            AND status = 'ACTIVE'
            ORDER BY ST_Distance(location, ST_SetSRID(ST_Point(:longitude, :latitude), 4326)::geography)
            """, nativeQuery = true)
    List<Hospital> findNearby(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMetres") double radiusMetres);

    @Query(value = """
            SELECT * FROM hospitals
            WHERE ST_Within(
                location,
                ST_MakeEnvelope(:minLon, :minLat, :maxLon, :maxLat, 4326)
            )
            """, nativeQuery = true)
    List<Hospital> findInBox(
            @Param("minLat") double minLat,
            @Param("minLon") double minLon,
            @Param("maxLat") double maxLat,
            @Param("maxLon") double maxLon);
}
