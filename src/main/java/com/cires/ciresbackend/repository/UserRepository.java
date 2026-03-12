package com.cires.ciresbackend.repository;

import com.cires.ciresbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // This allows the UserService to find, save, and delete users.
}