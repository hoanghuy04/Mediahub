package com.bondhub.friendservice.service.friendship;

import com.bondhub.common.dto.ApiResponse;
import com.bondhub.common.dto.client.userservice.user.response.UserSummaryResponse;
import com.bondhub.common.exception.AppException;
import com.bondhub.common.exception.ErrorCode;
import com.bondhub.common.utils.SecurityUtil;
import com.bondhub.friendservice.client.UserServiceClient;
import com.bondhub.friendservice.dto.request.FriendRequestSendRequest;
import com.bondhub.friendservice.dto.response.*;
import com.bondhub.friendservice.mapper.FriendShipMapper;
import com.bondhub.friendservice.model.FriendShip;
import com.bondhub.friendservice.model.enums.FriendStatus;
import com.bondhub.friendservice.repository.FriendShipRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {
    FriendShipRepository friendShipRepository;
    FriendShipMapper friendShipMapper;
    UserServiceClient userServiceClient;
    SecurityUtil securityUtil;

    @Override
    @Transactional
    public FriendRequestResponse sendFriendRequest(FriendRequestSendRequest request) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        String receiverId = request.receiverId();
        
        log.info("User {} sending friend request to {}", currentAccountId, receiverId);

        UserSummaryResponse currentUser = validateUserByAccountId(currentAccountId);
        validateUserExists(receiverId);

        if (currentUser.id().equals(receiverId)) {
            throw new AppException(ErrorCode.CANNOT_FRIEND_YOURSELF);
        }

        Optional<FriendShip> existingFriendship = friendShipRepository
                .findFriendshipBetweenUsers(currentUser.id(), receiverId);
        
        if (existingFriendship.isPresent()) {
            FriendShip friendship = existingFriendship.get();
            if (friendship.getFriendStatus() == FriendStatus.ACCEPTED) {
                throw new AppException(ErrorCode.ALREADY_FRIENDS);
            } else if (friendship.getFriendStatus() == FriendStatus.PENDING) {
                throw new AppException(ErrorCode.FRIEND_REQUEST_ALREADY_SENT);
            }
        }

        FriendShip friendShip = FriendShip.builder()
                .requested(currentUser.id())
                .received(receiverId)
                .content(request.message())
                .friendStatus(FriendStatus.PENDING)
                .build();

        friendShip = friendShipRepository.save(friendShip);
        log.info("Friend request created with id: {}", friendShip.getId());

        UserSummaryResponse requester = getUserSummary(friendShip.getRequested());
        UserSummaryResponse receiver = getUserSummary(friendShip.getReceived());
        return friendShipMapper.toFriendRequestResponse(friendShip, requester, receiver);
    }

    @Override
    @Transactional
    public FriendRequestResponse acceptFriendRequest(String friendshipId) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        UserSummaryResponse currentUser = validateUserByAccountId(currentAccountId);
        log.info("User {} accepting friend request {}", currentUser.id(), friendshipId);

        FriendShip friendShip = friendShipRepository.findById(friendshipId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friendShip.getReceived().equals(currentUser.id())) {
            throw new AppException(ErrorCode.NOT_AUTHORIZED_TO_ACCEPT);
        }

        if (friendShip.getFriendStatus() != FriendStatus.PENDING) {
            throw new AppException(ErrorCode.FRIEND_REQUEST_NOT_PENDING);
        }

        friendShip.setFriendStatus(FriendStatus.ACCEPTED);
        friendShip = friendShipRepository.save(friendShip);
        
        log.info("Friend request {} accepted successfully", friendshipId);
        UserSummaryResponse requester = getUserSummary(friendShip.getRequested());
        UserSummaryResponse receiver = getUserSummary(friendShip.getReceived());
        return friendShipMapper.toFriendRequestResponse(friendShip, requester, receiver);
    }

    @Override
    @Transactional
    public void declineFriendRequest(String friendshipId) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        UserSummaryResponse currentUser = validateUserByAccountId(currentAccountId);
        log.info("User {} declining friend request {}", currentUser.id(), friendshipId);

        FriendShip friendShip = friendShipRepository.findById(friendshipId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friendShip.getReceived().equals(currentUser.id())) {
            throw new AppException(ErrorCode.NOT_AUTHORIZED_TO_DECLINE);
        }

        if (friendShip.getFriendStatus() != FriendStatus.PENDING) {
            throw new AppException(ErrorCode.FRIEND_REQUEST_NOT_PENDING);
        }

        friendShip.setFriendStatus(FriendStatus.DECLINED);
        friendShipRepository.save(friendShip);
        log.info("Friend request {} declined with status DECLINED", friendshipId);
    }

    @Override
    @Transactional
    public void cancelFriendRequest(String friendshipId) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        UserSummaryResponse currentUser = validateUserByAccountId(currentAccountId);
        log.info("User {} canceling friend request {}", currentUser.id(), friendshipId);

        FriendShip friendShip = friendShipRepository.findById(friendshipId)
                .orElseThrow(() -> new AppException(ErrorCode.FRIEND_REQUEST_NOT_FOUND));

        if (!friendShip.getRequested().equals(currentUser.id())) {
            throw new AppException(ErrorCode.NOT_AUTHORIZED_TO_CANCEL);
        }

        if (friendShip.getFriendStatus() != FriendStatus.PENDING) {
            throw new AppException(ErrorCode.FRIEND_REQUEST_NOT_PENDING);
        }

        friendShip.setFriendStatus(FriendStatus.CANCELLED);
        friendShipRepository.save(friendShip);
        log.info("Friend request {} canceled and deleted", friendshipId);
    }

    @Override
    @Transactional
    public void unfriend(String friendId) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        UserSummaryResponse currentUser = validateUserByAccountId(currentAccountId);
        log.info("User {} unfriending user {}", currentUser.id(), friendId);

        FriendShip friendShip = friendShipRepository
                .findAcceptedFriendship(currentUser.id(), friendId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FRIENDS));

        friendShipRepository.delete(friendShip);
        log.info("Friendship between {} and {} removed", currentUser.id(), friendId);
    }

    @Override
    public List<FriendRequestResponse> getReceivedFriendRequests() {
        String currentAccountId = securityUtil.getCurrentAccountId();
        UserSummaryResponse currentUser = validateUserByAccountId(currentAccountId);
        log.info("Fetching received friend requests for user {}", currentUser.id());

        List<FriendShip> requests = friendShipRepository
                .findByReceivedAndFriendStatusOrderByCreatedAtDesc(currentUser.id(), FriendStatus.PENDING);

        return requests.stream()
                .map(friendShip -> {
                    UserSummaryResponse requester = getUserSummary(friendShip.getRequested());
                    UserSummaryResponse receiver = getUserSummary(friendShip.getReceived());
                    return friendShipMapper.toFriendRequestResponse(friendShip, requester, receiver);
                })
                .toList();
    }

    @Override
    public List<FriendRequestResponse> getSentFriendRequests() {
        String currentAccountId = securityUtil.getCurrentAccountId();
        UserSummaryResponse currentUser = validateUserByAccountId(currentAccountId);
        log.info("Fetching sent friend requests for user {}", currentUser.id());

        List<FriendShip> requests = friendShipRepository
                .findByRequestedAndFriendStatusOrderByCreatedAtDesc(currentUser.id(), FriendStatus.PENDING);

        return requests.stream()
                .map(friendShip -> {
                    UserSummaryResponse requester = getUserSummary(friendShip.getRequested());
                    UserSummaryResponse receiver = getUserSummary(friendShip.getReceived());
                    return friendShipMapper.toFriendRequestResponse(friendShip, requester, receiver);
                })
                .toList();
    }

    @Override
    public List<FriendResponse> getMyFriends() {
        String currentAccountId = securityUtil.getCurrentAccountId();
        UserSummaryResponse currentUser = validateUserByAccountId(currentAccountId);
        log.info("Fetching friends list for user {}", currentUser.id());

        List<FriendShip> friendships = friendShipRepository.findAllFriendsByUserId(currentUser.id());

        return friendships.stream()
                .map(friendship -> {
                    String friendId = friendship.getRequested().equals(currentUser.id())
                            ? friendship.getReceived()
                            : friendship.getRequested();
                    UserSummaryResponse friend = getUserSummary(friendId);
                    Integer mutualCount = getMutualFriendsCount(friendId);
                    return friendShipMapper.toFriendResponse(friend, friendship, mutualCount);
                })
                .toList();
    }

    @Override
    public List<FriendResponse> getFriendsByUserId(String userId) {
        List<FriendShip> friendships = friendShipRepository.findAllFriendsByUserId(userId);

        return friendships.stream()
                .map(friendship -> {
                    String friendId = friendship.getRequested().equals(userId)
                            ? friendship.getReceived()
                            : friendship.getRequested();
                    UserSummaryResponse friend = getUserSummary(friendId);
                    Integer mutualCount = getMutualFriendsCount(friendId);
                    return friendShipMapper.toFriendResponse(friend, friendship, mutualCount);
                })
                .toList();
    }

    @Override
    public FriendshipStatusResponse checkFriendshipStatus(String userId) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        UserSummaryResponse currentUser = validateUserByAccountId(currentAccountId);
        log.info("Checking friendship status between {} and {}", currentUser.id(), userId);

        Optional<FriendShip> friendship = friendShipRepository
                .findFriendshipBetweenUsers(currentUser.id(), userId);

        if (friendship.isEmpty()) {
            return FriendshipStatusResponse.builder()
                    .areFriends(false)
                    .status(null)
                    .friendshipId(null)
                    .requestedBy(null)
                    .build();
        }

        FriendShip fs = friendship.get();
        return FriendshipStatusResponse.builder()
                .areFriends(fs.getFriendStatus() == FriendStatus.ACCEPTED)
                .status(fs.getFriendStatus())
                .friendshipId(fs.getId())
                .requestedBy(fs.getRequested())
                .build();
    }

    @Override
    public MutualFriendsResponse getMutualFriends(String userId) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        UserSummaryResponse currentUser = validateUserByAccountId(currentAccountId);
        log.info("Fetching mutual friends between {} and {}", currentUser.id(), userId);

        List<FriendShip> currentUserFriends = friendShipRepository.findAllFriendsByUserId(currentUser.id());
        Set<String> currentUserFriendIds = extractFriendIds(currentUserFriends, currentUser.id());

        List<FriendShip> targetUserFriends = friendShipRepository.findAllFriendsByUserId(userId);
        Set<String> targetUserFriendIds = extractFriendIds(targetUserFriends, userId);

        Set<String> mutualFriendIds = new HashSet<>(currentUserFriendIds);
        mutualFriendIds.retainAll(targetUserFriendIds);

        List<FriendResponse> mutualFriends = mutualFriendIds.stream()
                .map(friendId -> {
                    UserSummaryResponse user = getUserSummary(friendId);
                    return friendShipMapper.toFriendResponseFromUser(user);
                })
                .toList();

        return MutualFriendsResponse.builder()
                .count(mutualFriends.size())
                .mutualFriends(mutualFriends)
                .build();
    }

    @Override
    public Integer getMutualFriendsCount(String userId) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        UserSummaryResponse currentUser = validateUserByAccountId(currentAccountId);
        log.info("Counting mutual friends between {} and {}", currentUser.id(), userId);

        List<FriendShip> currentUserFriends = friendShipRepository.findAllFriendsByUserId(currentUser.id());
        Set<String> currentUserFriendIds = extractFriendIds(currentUserFriends, currentUser.id());

        List<FriendShip> targetUserFriends = friendShipRepository.findAllFriendsByUserId(userId);
        Set<String> targetUserFriendIds = extractFriendIds(targetUserFriends, userId);

        Set<String> mutualFriendIds = new HashSet<>(currentUserFriendIds);
        mutualFriendIds.retainAll(targetUserFriendIds);

        return mutualFriendIds.size();
    }

    // Helper methods

    private Set<String> extractFriendIds(List<FriendShip> friendships, String userId) {
        return friendships.stream()
                .map(fs -> fs.getRequested().equals(userId) ? fs.getReceived() : fs.getRequested())
                .collect(Collectors.toSet());
    }

    private UserSummaryResponse getUserSummary(String userId) {
        try {
            ApiResponse<UserSummaryResponse> response = userServiceClient.getUserSummary(userId);
            if (response != null && response.data() != null) {
                return response.data();
            }
        } catch (Exception e) {
            log.error("Failed to fetch user summary for userId: {}", userId, e);
        }
        
        // Return minimal user info if service call fails
        return UserSummaryResponse.builder()
                .id(userId)
                .fullName("Unknown User")
                .avatar(null)
                .build();
    }

    private UserSummaryResponse validateUserByAccountId(String accountId) {
        try {
            ApiResponse<UserSummaryResponse> response = userServiceClient.getUserByAccountId(accountId);
            if (response == null || response.data() == null) {
                throw new AppException(ErrorCode.USER_NOT_FOUND);
            }
            return response.data();
        } catch (Exception e) {
            log.error("Failed to validate user by accountId: {}", accountId, e);
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateUserExists(String userId) {
        try {
            ApiResponse<UserSummaryResponse> response = userServiceClient.getUserSummary(userId);
            if (response == null || response.data() == null) {
                throw new AppException(ErrorCode.USER_NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("Failed to validate user: {}", userId, e);
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
    }
}
