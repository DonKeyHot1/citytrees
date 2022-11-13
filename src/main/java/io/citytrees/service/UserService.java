package io.citytrees.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.citytrees.configuration.security.JWTUserDetails;
import io.citytrees.model.User;
import io.citytrees.repository.UserRepository;
import io.citytrees.service.exception.UserInputError;
import io.citytrees.util.HashUtil;
import io.citytrees.v1.model.UserRegisterRequest;
import io.citytrees.v1.model.UserRole;
import io.citytrees.v1.model.UserUpdatePasswordRequest;
import io.citytrees.v1.model.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final HashUtil hashUtil;
    private final ObjectMapper objectMapper;
    private final SecurityService securityService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return findByEmail(username)
            .map(user -> JWTUserDetails.builder()
                .id(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build())
            .orElseThrow();
    }

    public UUID create(UserRegisterRequest request) {
        return create(UUID.randomUUID(),
            request.getEmail(),
            request.getPassword(),
            Set.of(UserRole.BASIC),
            null,
            null);
    }

    public UUID create(User user) {
        return create(user.getId(), user.getEmail(), user.getPassword(), user.getRoles(), user.getFirstName(), user.getLastName());
    }

    public UUID createIfNotExists(User user) {
        return findByEmail(user.getEmail()).map(User::getId).orElseGet(() -> create(user));
    }

    public UUID register(UserRegisterRequest registerUserRequest) {
        findByEmail(registerUserRequest.getEmail()).ifPresent(user -> {
            throw new UserInputError(String.format("Email '%s' is already in use", user.getEmail()));
        });

        return create(registerUserRequest);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getById(UUID id) {
        return userRepository.findByUserId(id);
    }

    public void update(UUID id, UserUpdateRequest request) {
        update(id, request.getEmail(), request.getFirstName(), request.getLastName());
    }

    public void update(UUID id, String email, String firstName, String lastName) {
        userRepository.update(id, email, firstName, lastName);
    }

    public void updatePassword(UserUpdatePasswordRequest userUpdatePasswordRequest) {
        var hashedPassword = hashUtil.md5WithSalt(userUpdatePasswordRequest.getNewPassword());
        userRepository.updatePassword(securityService.getCurrentUserId(), hashedPassword);
    }

    @SneakyThrows
    @SuppressWarnings("ParameterNumber")
    private UUID create(UUID id, String email, String pwd, Set<UserRole> roles, String firstName, String lastName) {
        var rolesJson = objectMapper.writeValueAsString(roles);
        var hashedPassword = hashUtil.md5WithSalt(pwd);
        return userRepository.create(id, email, hashedPassword, rolesJson, firstName, lastName);
    }
}
