package com.bondhub.friendservice.service.friendship;

import com.bondhub.friendservice.dto.request.FriendRequestSendRequest;
import com.bondhub.friendservice.dto.response.*;

import java.util.List;

public interface FriendshipService {
    
    FriendRequestResponse sendFriendRequest(FriendRequestSendRequest request); 

    FriendRequestResponse acceptFriendRequest(String friendshipId);

    void declineFriendRequest(String friendshipId);

    void cancelFriendRequest(String friendshipId);
    
    void unfriend(String friendId);
    
    List<FriendRequestResponse> getReceivedFriendRequests();
   
    List<FriendRequestResponse> getSentFriendRequests();
    
    List<FriendResponse> getMyFriends();
    
    List<FriendResponse> getFriendsByUserId(String userId);
    
    FriendshipStatusResponse checkFriendshipStatus(String userId);
    
    MutualFriendsResponse getMutualFriends(String userId);
    
    Integer getMutualFriendsCount(String userId);
}
