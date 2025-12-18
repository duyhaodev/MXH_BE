package CloneThreads.Threads.service;

import CloneThreads.Threads.dto.response.PostResponse;
import CloneThreads.Threads.entity.Post;
import CloneThreads.Threads.entity.User;
import CloneThreads.Threads.mapper.PostMapper;
import CloneThreads.Threads.repository.CommentRepository;
import CloneThreads.Threads.repository.LikeRepository;
import CloneThreads.Threads.repository.PostRepository;
import CloneThreads.Threads.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final PostMapper postMapper;

    public List<PostResponse> searchPosts(String keyword, String currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Post> posts = postRepository.searchPosts(keyword, pageable);

        Set<String> userIds = posts.stream()
                .map(Post::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return posts.stream().map(post -> {
            String ownerId = post.getUserId();
            User owner = userMap.get(ownerId);
            if (owner == null) {
                owner = User.builder().id(ownerId).build();
            }

            String originalId = getOriginalId(post);

            long repostCount = postRepository.countByRepostOf_Id(originalId);
            long commentCount = commentRepository.countByPostId(originalId);
            long likeCount = likeRepository.countByPostId(originalId);

            boolean likedByCurrentUser = currentUserId != null
                    && likeRepository.existsByUserIdAndPostId(currentUserId, originalId);

            boolean repostedByCurrentUser = currentUserId != null
                    && postRepository.existsByUserIdAndRepostOf_Id(currentUserId, originalId);

            PostResponse res = postMapper.toResponse(post, owner);
            res.setCommentCount(commentCount);
            res.setLikeCount(likeCount);
            res.setRepostCount(repostCount);
            res.setLikedByCurrentUser(likedByCurrentUser);
            res.setRepostedByCurrentUser(repostedByCurrentUser);
            return res;
        }).collect(Collectors.toList());
    }

    private String getOriginalId(Post post) {
        Post cur = post;
        while (cur.getRepostOf() != null) {
            cur = cur.getRepostOf();
        }
        return cur.getId();
    }
}
