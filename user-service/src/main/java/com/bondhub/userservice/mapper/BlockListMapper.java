package com.bondhub.userservice.mapper;

import com.bondhub.userservice.dto.request.BlockUserRequest;
import com.bondhub.userservice.dto.response.BlockPreferenceResponse;
import com.bondhub.userservice.dto.response.BlockedUserResponse;
import com.bondhub.userservice.dto.response.BlockedUserDetailResponse;
import com.bondhub.userservice.model.BlockList;
import com.bondhub.userservice.model.BlockPreference;
import com.bondhub.userservice.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface BlockListMapper {


    BlockedUserResponse toBlockedUserResponse(BlockList blockList);

    BlockPreferenceResponse toBlockPreferenceResponse(BlockPreference preference);

    @Mapping(target = "id", source = "blockList.id")
    @Mapping(target = "blockedUserId", source = "user.id")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "avatar", source = "user.avatar")
    @Mapping(target = "bio", source = "user.bio")
    @Mapping(target = "gender", source = "user.gender")
    @Mapping(target = "dob", source = "user.dob")
    @Mapping(target = "preference", source = "blockList.preference")
    @Mapping(target = "blockedAt", source = "blockList.createdAt")
    BlockedUserDetailResponse toBlockedUserDetailResponse(BlockList blockList, User user);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "blockerId", source = "blockerId")
    @Mapping(target = "blockedUserId", source = "request.blockedUserId")
    @Mapping(target = "preference", expression = "java(createBlockPreference(request))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "active", ignore = true)
    BlockList toBlockList(BlockUserRequest request, String blockerId);


    default BlockPreference createBlockPreference(BlockUserRequest request) {
        return BlockPreference.builder()
            .message(request.blockMessage() != null ? request.blockMessage() : true)
            .call(request.blockCall() != null ? request.blockCall() : true)
            .story(request.blockStory() != null ? request.blockStory() : true)
            .build();
    }
}
