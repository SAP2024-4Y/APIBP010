package com.hospital.doctor.repository;

import com.hospital.doctor.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID>, JpaSpecificationExecutor<Doctor> {

    List<Doctor> findByIdIn(List<UUID> ids);

    @Query("SELECT d FROM Doctor d WHERE d.departmentId = :departmentId")
    List<Doctor> findByDepartmentId(@Param("departmentId") UUID departmentId);
}
