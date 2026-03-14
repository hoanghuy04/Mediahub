package com.bondhub.messageservice.service;

import com.bondhub.messageservice.model.ChatUser;
import com.bondhub.common.enums.Status;
import com.bondhub.messageservice.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPresenceService {

    private final ChatUserRepository repository;

    public ChatUser saveUser(ChatUser user) {
        return repository.findById(user.getId())
                .map(storedUser -> {
                    storedUser.setStatus(Status.ONLINE);
                    storedUser.setFullName(user.getFullName());
                    storedUser.setEmail(user.getEmail());
                    log.info("[Presence] User ONLINE: {}", storedUser.getEmail());
                    return repository.save(storedUser);
                })
                .orElseGet(() -> {
                    user.setStatus(Status.ONLINE);
                    log.info("[Presence] New user ONLINE: {}", user.getEmail());
                    return repository.save(user);
                });
    }

    public void disconnect(String userId) {
        repository.findById(userId).ifPresent(user -> {
            user.setStatus(Status.OFFLINE);
            repository.save(user);
            log.info("[Presence] User OFFLINE: {}", user.getEmail());
        });
    }

    public List<ChatUser> findConnectedUsers() {
        return repository.findAllByStatus(Status.ONLINE);
    }
}
