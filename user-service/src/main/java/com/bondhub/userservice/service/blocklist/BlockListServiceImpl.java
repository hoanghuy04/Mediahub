package com.bondhub.userservice.service.blocklist;

import com.bondhub.common.exception.AppException;
import com.bondhub.common.exception.ErrorCode;
import com.bondhub.common.utils.SecurityUtil;
import com.bondhub.common.utils.S3Util;
import com.bondhub.userservice.dto.request.BlockUserRequest;
import com.bondhub.userservice.dto.request.UpdateBlockPreferenceRequest;
import com.bondhub.userservice.dto.response.BlockedUserResponse;
import com.bondhub.userservice.dto.response.BlockedUserDetailResponse;
import com.bondhub.userservice.mapper.BlockListMapper;
import com.bondhub.userservice.model.enums.BlockType;
import com.bondhub.userservice.model.BlockList;
import com.bondhub.userservice.model.BlockPreference;
import com.bondhub.userservice.model.User;
import com.bondhub.userservice.repository.BlockListRepository;
import com.bondhub.userservice.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;


@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class BlockListServiceImpl implements BlockListService {
    
    final BlockListRepository blockListRepository;
    final UserRepository userRepository;
    final BlockListMapper blockListMapper;
    final SecurityUtil securityUtil;
    
    @Value("${aws.s3.bucket.name}")
    String bucketName;

    @Value("${cloud.aws.region.static}")
    String region;
    
    @Override
    @Transactional
    public BlockedUserResponse blockUser(BlockUserRequest request) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        log.info("User {} attempting to block user {}", currentAccountId, request.blockedUserId());
        

        User currentUser = userRepository.findByAccountId(currentAccountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
  
        User blockedUser = userRepository.findById(request.blockedUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
      
        if (currentUser.getId().equals(blockedUser.getId())) {
            throw new AppException(ErrorCode.CANNOT_BLOCK_YOURSELF);
        }
        
     
        if (blockListRepository.existsByBlockerIdAndBlockedUserId(currentUser.getId(), blockedUser.getId())) {
            throw new AppException(ErrorCode.USER_ALREADY_BLOCKED);
        }
        
 
        BlockList blockList = blockListMapper.toBlockList(request, currentUser.getId());
        blockList = blockListRepository.save(blockList);
        
        log.info("User {} successfully blocked user {}", currentUser.getId(), blockedUser.getId());
        return blockListMapper.toBlockedUserResponse(blockList);
    }
    
    @Override
    @Transactional
    public void unblockUser(String blockedUserId) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        log.info("User {} attempting to unblock user {}", currentAccountId, blockedUserId);
        

        User currentUser = userRepository.findByAccountId(currentAccountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
       
        userRepository.findById(blockedUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
      
        BlockList blockList = blockListRepository.findByBlockerIdAndBlockedUserId(
                currentUser.getId(), blockedUserId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOCK_NOT_FOUND));
        
   
        blockListRepository.delete(blockList);
        
        log.info("User {} successfully unblocked user {}", currentUser.getId(), blockedUserId);
    }
    
    @Override
    @Transactional
    public BlockedUserResponse updateBlockPreference(String blockedUserId, UpdateBlockPreferenceRequest request) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        log.info("User {} updating block preferences for user {}", currentAccountId, blockedUserId);
        
      
        User currentUser = userRepository.findByAccountId(currentAccountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
     
        BlockList blockList = blockListRepository.findByBlockerIdAndBlockedUserId(
                currentUser.getId(), blockedUserId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOCK_NOT_FOUND));
        
      
        BlockPreference preference = blockList.getPreference();
        if (preference == null) {
            preference = new BlockPreference();
        }
        
        if (request.blockMessage() != null) {
            preference.setMessage(request.blockMessage());
        }
        if (request.blockCall() != null) {
            preference.setCall(request.blockCall());
        }
        if (request.blockStory() != null) {
            preference.setStory(request.blockStory());
        }
        
        blockList.setPreference(preference);
        blockList = blockListRepository.save(blockList);
        
        log.info("Block preferences updated successfully for user {}", blockedUserId);
        return blockListMapper.toBlockedUserResponse(blockList);
    }
    
    @Override
    public List<BlockedUserResponse> getMyBlockedUsers() {
        String currentAccountId = securityUtil.getCurrentAccountId();
        log.info("Fetching blocked users for user {}", currentAccountId);
        
    
        User currentUser = userRepository.findByAccountId(currentAccountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
       
        List<BlockList> blockList = blockListRepository.findByBlockerId(currentUser.getId());
        
        log.info("Found {} blocked users for user {}", blockList.size(), currentUser.getId());
        return blockList.stream()
                .map(blockListMapper::toBlockedUserResponse)
                .toList();
    }
    
    @Override
    public boolean isUserBlocked(String userId) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        
    
        User currentUser = userRepository.findByAccountId(currentAccountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        return blockListRepository.existsByBlockerIdAndBlockedUserId(currentUser.getId(), userId);
    }
    
    @Override
    public BlockedUserResponse getBlockDetails(String blockedUserId) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        log.info("Fetching block details for user {} by {}", blockedUserId, currentAccountId);
        
 
        User currentUser = userRepository.findByAccountId(currentAccountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
    
        BlockList blockList = blockListRepository.findByBlockerIdAndBlockedUserId(
                currentUser.getId(), blockedUserId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOCK_NOT_FOUND));
        
        return blockListMapper.toBlockedUserResponse(blockList);
    }
    
    @Override
    public List<BlockedUserDetailResponse> getMyBlockedUsersWithDetails() {
        String currentAccountId = securityUtil.getCurrentAccountId();
        log.info("Fetching blocked users with details for user {}", currentAccountId);
        
      
        User currentUser = userRepository.findByAccountId(currentAccountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
   
        List<BlockList> blockLists = blockListRepository.findByBlockerId(currentUser.getId());
        
   
        String baseUrl = S3Util.getS3BaseUrl(bucketName, region);
        
        
        List<BlockedUserDetailResponse> responses = new ArrayList<>();
        for (BlockList blockList : blockLists) {
            try {
                User blockedUser = userRepository.findById(blockList.getBlockedUserId())
                        .orElse(null);
                
                if (blockedUser != null) {
                    BlockedUserDetailResponse response = blockListMapper.toBlockedUserDetailResponse(blockList, blockedUser);
                    
                    
                    if (response.avatar() != null && !response.avatar().isEmpty()) {
                        response = BlockedUserDetailResponse.builder()
                                .id(response.id())
                                .blockedUserId(response.blockedUserId())
                                .fullName(response.fullName())
                                .avatar(baseUrl + response.avatar())
                                .bio(response.bio())
                                .gender(response.gender())
                                .dob(response.dob())
                                .preference(response.preference())
                                .blockedAt(response.blockedAt())
                                .build();
                    }
                    
                    responses.add(response);
                } else {
                    log.warn("Blocked user not found: {}", blockList.getBlockedUserId());
                }
            } catch (Exception e) {
                log.error("Error fetching blocked user details: {}", blockList.getBlockedUserId(), e);
            }
        }
        
        log.info("Found {} blocked users with details for user {}", responses.size(), currentUser.getId());
        return responses;
    }
    
    @Override
    public boolean isBlockedForType(String targetUserId, BlockType blockType) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        
        User currentUser = userRepository.findByAccountId(currentAccountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Optional<BlockList> blockList = blockListRepository.findByBlockerIdAndBlockedUserId(
                currentUser.getId(), targetUserId);
        
        if (blockList.isEmpty()) {
            return false;
        }
        
        BlockPreference preference = blockList.get().getPreference();
        if (preference == null) {
            return true; // Default: all blocked
        }
        
        return switch (blockType) {
            case MESSAGE -> preference.isMessage();
            case CALL -> preference.isCall();
            case STORY -> preference.isStory();
            case ALL -> preference.isMessage() && preference.isCall() && preference.isStory();
        };
    }
    
    @Override
    public Optional<BlockPreference> getBlockPreference(String blockedUserId) {
        String currentAccountId = securityUtil.getCurrentAccountId();
        
        User currentUser = userRepository.findByAccountId(currentAccountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        return blockListRepository.findByBlockerIdAndBlockedUserId(
                currentUser.getId(), blockedUserId)
                .map(BlockList::getPreference);
    }
    
    @Override
    public boolean hasBidirectionalBlock(String userId1, String userId2) {
        boolean user1BlocksUser2 = blockListRepository.existsByBlockerIdAndBlockedUserId(userId1, userId2);
        boolean user2BlocksUser1 = blockListRepository.existsByBlockerIdAndBlockedUserId(userId2, userId1);
        
        return user1BlocksUser2 && user2BlocksUser1;
    }
}
