package com.wsdev.simplestock.domain.user;

import com.wsdev.simplestock.domain.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>
{
    Optional<UserDetails> findUserByEmail( String userEmail );
    Optional<User> findByEmail( String email );
    boolean existsByEmail( String email );
}
