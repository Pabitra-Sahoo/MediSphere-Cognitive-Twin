package com.medisphere.risk.repository;

import com.medisphere.risk.model.RiskPrediction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskPredictionRepository extends MongoRepository<RiskPrediction, String> {

    List<RiskPrediction> findByPatientIdOrderByEvaluatedAtDesc(String patientId);

    List<RiskPrediction> findByPatientIdAndTaskTypeOrderByEvaluatedAtDesc(String patientId, String taskType);
}
