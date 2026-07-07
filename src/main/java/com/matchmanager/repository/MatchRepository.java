package com.matchmanager.repository;

import com.matchmanager.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByMatchGroupIdAndDelYnOrderByCourtNoAscRoundNoAsc(Long matchGroupId, String delYn);
}
