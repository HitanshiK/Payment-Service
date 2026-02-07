package com.paymentSystem.project.repos;

import com.paymentSystem.project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Component
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailOrMobile(String email, String mobile);

    boolean existsByEmailOrMobile(String email, String mobile);

}
