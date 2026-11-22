package com.psicology.backend.domain.user;

import com.psicology.backend.domain.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    long countByRole(Role role);
    long countByRoleAndEnabledTrue(Role role);

    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) AND " +
           "(:q IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', CONCAT(:q, '%'))) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', CONCAT(:q, '%'))))")
    Page<User> search(@Param("role") Role role, @Param("q") String q, Pageable pageable);
}
