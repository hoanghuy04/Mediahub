package com.bondhub.userservice.service.blocklist;

import com.bondhub.userservice.dto.request.BlockUserRequest;
import com.bondhub.userservice.dto.request.UpdateBlockPreferenceRequest;
import com.bondhub.userservice.dto.response.BlockedUserResponse;
import com.bondhub.userservice.dto.response.BlockedUserDetailResponse;
import com.bondhub.userservice.model.enums.BlockType;
import com.bondhub.userservice.model.BlockPreference;

import java.util.List;
import java.util.Optional;

public interface BlockListService {
    

    BlockedUserResponse blockUser(BlockUserRequest request);

    void unblockUser(String blockedUserId);

    BlockedUserResponse updateBlockPreference(String blockedUserId, UpdateBlockPreferenceRequest request);
    

    List<BlockedUserResponse> getMyBlockedUsers();
    
    /**
     * Get all users blocked by current user with detailed user information
     */
    List<BlockedUserDetailResponse> getMyBlockedUsersWithDetails();
    

    boolean isUserBlocked(String userId);
    

    BlockedUserResponse getBlockDetails(String blockedUserId);
    
    /**
     * Check if current user has blocked target user for a specific communication type
     */
    boolean isBlockedForType(String targetUserId, BlockType blockType);
    
    /**
     * Get block preference for a blocked user
     */
    Optional<BlockPreference> getBlockPreference(String blockedUserId);
    
    /**
     * Check if two users have bidirectional block (both blocked each other)
     */
    boolean hasBidirectionalBlock(String userId1, String userId2);
}
