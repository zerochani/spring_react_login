package org.example.backend.domain.user.service;

import org.example.backend.domain.user.dto.UserRequestDTO;
import org.example.backend.domain.user.entity.UserEntity;
import org.example.backend.domain.user.entity.UserRoleType;
import org.example.backend.domain.user.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    //자체 로그인 회원 가입 (존재 여부)
    @Transactional(readOnly = true)
    public Boolean existUser(UserRequestDTO dto) {
        return userRepository.existsByUsername(dto.getUsername());
    }

    //자체 로그인 회원 가입
    @Transactional
    public Long addUser(UserRequestDTO dto) {

        if(userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("이미 유저가 존재합니다.");
        }
        UserEntity entity = UserEntity.builder()
                .username(dto.getUsername())
                .password(passwordEncoder.encode(dto.getPassword()))
                .isLock(false)
                .isSocial(false)
                .roleType(UserRoleType.USER)
                .nickname(dto.getNickname())
                .email(dto.getEmail())
                .build();
        return userRepository.save(entity).getId();
    }

    @Transactional
    public Long updateUser(UserRequestDTO dto) throws AccessDeniedException {

        //본인만 수정 기능 검증
        String sessionUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if(!sessionUsername.equals(dto.getUsername())) {
            throw new AccessDeniedException("본인 계정만 수정 가능");
        }

        //조회
        UserEntity entity = userRepository.findByUsernameAndIsLockAndIsSocial(dto.getUsername(), false, false)
                .orElseThrow(()-> new UsernameNotFoundException(dto.getUsername()));

        //회원 정보 수정
        entity.updateUser(dto);

        return userRepository.save(entity).getId();
    }

}
