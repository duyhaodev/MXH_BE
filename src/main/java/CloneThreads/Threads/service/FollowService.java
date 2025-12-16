package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.response.FollowResponse;
import CloneThreads.Threads.entity.Follow;
import CloneThreads.Threads.repository.FollowRepository;
import CloneThreads.Threads.exception.AppException;
import CloneThreads.Threads.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FollowService {
    @Autowired private FollowRepository followRepo;
    @Autowired private UserService userService;
    @Autowired private NotificationService notificationService;

    @Transactional
    public FollowResponse followUser(String followerId, String followingId) {
        if (followerId.equals(followingId)) {
            throw new AppException(ErrorCode.CANNOT_FOLLOW_SELF);
        }
        if (followRepo.existsByFollowerAndFollowing(followerId, followingId)) {
            throw new AppException(ErrorCode.ALREADY_FOLLOWING);
        }

        Follow follow = Follow.builder()
                .followerId(followerId)
                .followingId(followingId)
                .createdAt(LocalDateTime.now())
                .build();
        followRepo.save(follow);

        // Create notification for the followed user
        notificationService.createFollowNotification(followingId, followerId);

        // Update counts
        userService.incrementFollowers(followingId);
        userService.incrementFollowing(followerId);

        return FollowResponse.builder()
                .success(true)
                .isFollowing(true)
                .message("Followed successfully")
                .build();
    }

    @Transactional
    public FollowResponse unfollowUser(String followerId, String followingId) {
        if (!followRepo.existsByFollowerAndFollowing(followerId, followingId)) {
            throw new AppException(ErrorCode.NOT_FOLLOWING);
        }

        followRepo.deleteByFollowerIdAndFollowingId(followerId, followingId);

        // Update counts (no notification for unfollow)
        userService.decrementFollowers(followingId);
        userService.decrementFollowing(followerId);

        return FollowResponse.builder()
                .success(true)
                .isFollowing(false)
                .message("Unfollowed successfully")
                .build();
    }

    public boolean isFollowing(String followerId, String followingId) {
        return followRepo.existsByFollowerAndFollowing(followerId, followingId);
    }
}