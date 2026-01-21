package com.bondhub.authservice.repository;

import com.bondhub.authservice.model.OtpRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Redis repository for OTP records
 */
@Repository
public interface OtpRepository extends CrudRepository<OtpRecord, String> {
    // CrudRepository provides standard methods: save, findById, delete, etc.
}
