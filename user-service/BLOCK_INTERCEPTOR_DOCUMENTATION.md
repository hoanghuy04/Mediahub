# Block Communication Interceptor Documentation

## Overview

The Block Communication Interceptor is a global AOP-based system that automatically restricts communication between users based on blocking preferences. When User A blocks User B, various types of communication (messages, calls, stories) can be restricted based on configured preferences.

## Architecture Components

### 1. Core Components

#### BlockType Enum
Defines the types of blockable communications:
- `MESSAGE` - Text messaging
- `CALL` - Voice/video calls
- `STORY` - Story viewing
- `ALL` - All communication types

**Location:** `com.bondhub.userservice.enums.BlockType`

#### CheckBlockStatus Annotation
Custom annotation to mark methods that require block checking.

**Location:** `com.bondhub.userservice.annotation.CheckBlockStatus`

**Parameters:**
- `blockType()` - Type of communication to check (MESSAGE, CALL, STORY, ALL)
- `targetUserIdParam()` - Name of the method parameter containing target user ID
- `bidirectional()` - If true, checks both directions (A→B and B→A)

**Example:**
```java
@CheckBlockStatus(blockType = BlockType.MESSAGE, targetUserIdParam = "receiverId")
public void sendMessage(String receiverId, String message) {
    // Method implementation
}
```

### 2. Services

#### BlockCheckService
Core service for checking blocking status and throwing appropriate exceptions.

**Location:** `com.bondhub.userservice.service.blocklist.BlockCheckService`

**Key Methods:**
- `checkAndThrowIfBlocked(senderId, receiverId, blockType)` - Checks if sender blocked receiver for specific type
- `checkBidirectionalBlock(userId1, userId2, blockType)` - Checks blocking in both directions
- `isBlocked(blockerId, blockedUserId, blockType)` - Returns boolean without throwing exception

#### BlockListService
Enhanced service with utility methods for blocking management.

**Location:** `com.bondhub.userservice.service.blocklist.BlockListService`

**New Methods:**
- `isBlockedForType(targetUserId, blockType)` - Check if current user blocked target for specific type
- `getBlockPreference(blockedUserId)` - Get block preferences for a blocked user
- `hasBidirectionalBlock(userId1, userId2)` - Check if two users blocked each other

### 3. AOP Aspect

#### BlockCheckAspect
Intercepts methods annotated with `@CheckBlockStatus` and performs blocking validation.

**Location:** `com.bondhub.userservice.aspect.BlockCheckAspect`

**Execution Flow:**
1. Intercepts method call before execution
2. Extracts current user ID from SecurityContext
3. Extracts target user ID from method parameters
4. Calls BlockCheckService to validate blocking status
5. Throws exception if blocked, otherwise allows method to proceed

## Error Codes

| Error Code | Message | Description |
|------------|---------|-------------|
| 2304 | MESSAGE_BLOCKED | User has blocked you from sending messages |
| 2305 | CALL_BLOCKED | User has blocked you from making calls |
| 2306 | STORY_BLOCKED | User has blocked you from viewing stories |
| 2307 | COMMUNICATION_BLOCKED | User has blocked all communication with you |

## Usage Examples

### Example 1: Message Sending
```java
@PostMapping("/send-message/{receiverId}")
@CheckBlockStatus(blockType = BlockType.MESSAGE, targetUserIdParam = "receiverId")
public ResponseEntity<ApiResponse<String>> sendMessage(
        @PathVariable String receiverId,
        @RequestBody MessageRequest request) {
    
    // If sender blocked receiver for messages → MESSAGE_BLOCKED exception
    messageService.send(receiverId, request);
    return ResponseEntity.ok(ApiResponse.success("Message sent"));
}
```

### Example 2: Call Initiation
```java
@PostMapping("/initiate-call/{receiverId}")
@CheckBlockStatus(blockType = BlockType.CALL, targetUserIdParam = "receiverId")
public ResponseEntity<ApiResponse<String>> initiateCall(
        @PathVariable String receiverId) {
    
    // If sender blocked receiver for calls → CALL_BLOCKED exception
    callService.initiate(receiverId);
    return ResponseEntity.ok(ApiResponse.success("Call initiated"));
}
```

### Example 3: Story Viewing
```java
@GetMapping("/view-story/{userId}")
@CheckBlockStatus(blockType = BlockType.STORY, targetUserIdParam = "userId")
public ResponseEntity<ApiResponse<StoryResponse>> viewStory(
        @PathVariable String userId) {
    
    // If current user blocked story owner → STORY_BLOCKED exception
    StoryResponse story = storyService.getStory(userId);
    return ResponseEntity.ok(ApiResponse.success(story));
}
```

### Example 4: Bidirectional Check
```java
@PostMapping("/start-conversation/{receiverId}")
@CheckBlockStatus(blockType = BlockType.ALL, targetUserIdParam = "receiverId", bidirectional = true)
public ResponseEntity<ApiResponse<String>> startConversation(
        @PathVariable String receiverId) {
    
    // Checks both:
    // 1. If sender blocked receiver
    // 2. If receiver blocked sender
    conversationService.start(receiverId);
    return ResponseEntity.ok(ApiResponse.success("Conversation started"));
}
```

### Example 5: Request Parameter
```java
@PostMapping("/send-notification")
@CheckBlockStatus(blockType = BlockType.MESSAGE, targetUserIdParam = "targetUserId")
public ResponseEntity<ApiResponse<String>> sendNotification(
        @RequestParam String targetUserId,
        @RequestBody NotificationRequest request) {
    
    // Works with @RequestParam, @PathVariable, and method parameters
    notificationService.send(targetUserId, request);
    return ResponseEntity.ok(ApiResponse.success("Notification sent"));
}
```

## BlockPreference Structure

Each block relationship has a `BlockPreference` object with granular control:

```java
public class BlockPreference {
    private boolean message = true;  // Block messages
    private boolean call = true;     // Block calls
    private boolean story = true;    // Block stories
}
```

**Default Behavior:** When blocking a user, all communication types are blocked by default.

**Customization:** Use `updateBlockPreference` API to enable/disable specific blocking types.

## Blocking Logic

### Single Direction Check (default)
```
Sender → Receiver

If Sender blocked Receiver for MESSAGE:
  - Sender CANNOT send messages to Receiver
  - Receiver CAN still send messages to Sender
```

### Bidirectional Check (`bidirectional = true`)
```
User A ↔ User B

Checks if:
  - User A blocked User B for specified type, OR
  - User B blocked User A for specified type
  
If either is true → Exception thrown
```

## BlockType Validation Rules

| BlockType | Checks |
|-----------|--------|
| MESSAGE | preference.isMessage() |
| CALL | preference.isCall() |
| STORY | preference.isStory() |
| ALL | preference.isMessage() AND preference.isCall() AND preference.isStory() |

## Integration Steps

### Step 1: Add Dependency
Ensure `spring-boot-starter-aop` is in your `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### Step 2: Annotate Controller Methods
Add `@CheckBlockStatus` annotation to endpoints that require blocking validation:
```java
@CheckBlockStatus(blockType = BlockType.MESSAGE, targetUserIdParam = "receiverId")
```

### Step 3: Handle Exceptions
The interceptor throws `AppException` with appropriate error codes. These are automatically handled by the global exception handler.

### Step 4: Test Blocking Scenarios
```bash
# 1. User A blocks User B for messages
POST /api/v1/block-list/block
{
  "blockedUserId": "user-b-id",
  "preference": {
    "message": true,
    "call": false,
    "story": false
  }
}

# 2. User A tries to send message to User B
POST /api/v1/messages/send/user-b-id
# Result: 2304 MESSAGE_BLOCKED exception
```

## Best Practices

### 1. Choose Appropriate BlockType
- Use `MESSAGE` for chat/messaging endpoints
- Use `CALL` for voice/video call endpoints
- Use `STORY` for story viewing endpoints
- Use `ALL` for critical operations requiring full communication access

### 2. Use Bidirectional When Needed
```java
// Use bidirectional for peer-to-peer features
@CheckBlockStatus(blockType = BlockType.CALL, targetUserIdParam = "userId", bidirectional = true)
public void startVideoCall(String userId) { }

// Don't use bidirectional for one-way actions
@CheckBlockStatus(blockType = BlockType.STORY, targetUserIdParam = "userId")
public StoryResponse viewStory(String userId) { }
```

### 3. Parameter Naming
Ensure `targetUserIdParam` matches the actual parameter name:
```java
// ✅ Correct
@CheckBlockStatus(targetUserIdParam = "receiverId")
public void send(String receiverId, String message) { }

// ❌ Incorrect - parameter name mismatch
@CheckBlockStatus(targetUserIdParam = "userId")
public void send(String receiverId, String message) { }
```

### 4. Logging
The aspect automatically logs blocking checks at DEBUG level:
```
DEBUG - Checking block status for method: sendMessage
DEBUG - Block check passed for user-a-id -> user-b-id (type: MESSAGE)
```

## Performance Considerations

### 1. Database Queries
- Each check performs 1 database query (or 2 for bidirectional)
- Queries are optimized with compound indexes on `(blockerId, blockedUserId)`

### 2. Caching (Recommended)
Consider adding caching to `BlockCheckService` for frequently checked relationships:
```java
@Cacheable(value = "blockCache", key = "#blockerId + '_' + #blockedUserId")
public boolean isBlocked(String blockerId, String blockedUserId, BlockType blockType) {
    // Implementation
}
```

### 3. Async Operations
Block checks are synchronous. For async operations, use programmatic checking:
```java
@Async
public CompletableFuture<Void> sendAsyncNotification(String userId) {
    blockCheckService.checkAndThrowIfBlocked(currentUserId, userId, BlockType.MESSAGE);
    // Send notification
}
```

## Troubleshooting

### Issue 1: Annotation Not Working
**Cause:** Spring AOP dependency missing
**Solution:** Add `spring-boot-starter-aop` to pom.xml

### Issue 2: Parameter Not Found Warning
```
WARN - Parameter 'userId' not found in method sendMessage
```
**Cause:** `targetUserIdParam` doesn't match actual parameter name
**Solution:** Ensure annotation parameter name matches method parameter

### Issue 3: Block Check Always Passes
**Cause:** User not authenticated or security context empty
**Solution:** Ensure endpoint requires authentication and SecurityContext is populated

## API Reference

### Block User
```http
POST /api/v1/block-list/block
Content-Type: application/json

{
  "blockedUserId": "string",
  "preference": {
    "message": true,
    "call": true,
    "story": true
  }
}
```

### Update Block Preference
```http
PUT /api/v1/block-list/{blockedUserId}/preference
Content-Type: application/json

{
  "blockMessage": false,
  "blockCall": true,
  "blockStory": true
}
```

### Unblock User
```http
DELETE /api/v1/block-list/unblock/{blockedUserId}
```

### Get My Blocked Users
```http
GET /api/v1/block-list/my-blocked-users
```

### Get Block Details
```http
GET /api/v1/block-list/{blockedUserId}/details
```

## Related Documentation
- [BlockList API Documentation](./BlockList_API.md)
- [Security Architecture](../SECURITY_ARCHITECTURE.md)
- [Backend Development Guide](../BACKEND_DEVELOPMENT_GUIDE.md)
