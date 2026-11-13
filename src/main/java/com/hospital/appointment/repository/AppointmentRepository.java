package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID>,
        JpaSpecificationExecutor<Appointment> {

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.doctorId = :doctorId
              AND a.status != 'CANCELLED'
              AND a.startsAt < :endsAt
              AND a.endsAt > :startsAt
            """)
    List<Appointment> findOverlapping(
            @Param("doctorId") UUID doctorId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.doctorId = :doctorId
              AND a.status != 'CANCELLED'
              AND a.startsAt < :endsAt
              AND a.endsAt > :startsAt
              AND a.id != :excludeId
            """)
    List<Appointment> findOverlappingExcluding(
            @Param("doctorId") UUID doctorId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt,
            @Param("excludeId") UUID excludeId);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.doctorId = :doctorId
              AND a.status != 'CANCELLED'
              AND (
                (a.startsAt BETWEEN :bufferStart AND :bufferEnd)
                OR (a.endsAt BETWEEN :bufferStart AND :bufferEnd)
              )
            """)
    List<Appointment> findWithinBuffer(
            @Param("doctorId") UUID doctorId,
            @Param("bufferStart") OffsetDateTime bufferStart,
            @Param("bufferEnd") OffsetDateTime bufferEnd);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.doctorId = :doctorId
              AND a.status != 'CANCELLED'
              AND (
                (a.startsAt BETWEEN :bufferStart AND :bufferEnd)
                OR (a.endsAt BETWEEN :bufferStart AND :bufferEnd)
              )
              AND a.id != :excludeId
            """)
    List<Appointment> findWithinBufferExcluding(
            @Param("doctorId") UUID doctorId,
            @Param("bufferStart") OffsetDateTime bufferStart,
            @Param("bufferEnd") OffsetDateTime bufferEnd,
            @Param("excludeId") UUID excludeId);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.doctorId = :doctorId
              AND a.startsAt < :endsAt
              AND a.endsAt > :startsAt
            ORDER BY a.startsAt
            """)
    List<Appointment> findByDoctorAndWindow(
            @Param("doctorId") UUID doctorId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt);

    List<Appointment> findByPatientId(UUID patientId);
}
