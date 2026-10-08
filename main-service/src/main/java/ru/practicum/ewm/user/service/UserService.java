package ru.practicum.ewm.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.UserMapper;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;
import ru.practicum.ewm.util.OffsetPageRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserDto createUser(NewUserRequest request) {
        User user = UserMapper.toUser(request);
        User savedUser = userRepository.save(user);

        return UserMapper.toUserDto(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsers(
            List<Long> ids,
            int from,
            int size
    ) {
        OffsetPageRequest pageable = new OffsetPageRequest(
                from,
                size,
                Sort.by("id").ascending()
        );

        List<User> users;

        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAllBy(pageable);
        } else {
            users = userRepository.findAllByIdIn(ids, pageable);
        }

        return users.stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Transactional
    public void deleteUser(long userId) {
        User user = getUserEntity(userId);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public User getUserEntity(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "User with id=" + userId + " was not found"
                ));
    }
}