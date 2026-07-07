package com.matchmanager.repository;

import com.matchmanager.entity.MatchGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchGroupRepository extends JpaRepository<MatchGroup, Long> {

    List<MatchGroup> findByRegIdAndDelYnOrderByRegDateDesc(Long regId, String delYn);

    Optional<MatchGroup> findByIdAndDelYn(Long id, String delYn);

    Optional<MatchGroup> findByShareTokenAndDelYn(String shareToken, String delYn);
}
