package com.esun.socialmedia.service;

import com.esun.socialmedia.common.exception.BusinessException;
import com.esun.socialmedia.common.util.SecurityUtils;
import com.esun.socialmedia.domain.dto.request.PostRequest;
import com.esun.socialmedia.domain.dto.response.PostResponse;
import com.esun.socialmedia.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final SecurityUtils  securityUtils;

    public Long createPost(Long userId, PostRequest req) {
        String content = securityUtils.sanitize(req.getContent());
        String image   = req.getImage() != null ? securityUtils.sanitize(req.getImage()) : null;
        return postRepository.createPost(userId, content, image);
    }

    public List<PostResponse> getAllPosts() {
        return postRepository.findAll();
    }

    public PostResponse getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "發文不存在"));
    }

    public void updatePost(Long postId, Long userId, PostRequest req) {
        String content = securityUtils.sanitize(req.getContent());
        String image   = req.getImage() != null ? securityUtils.sanitize(req.getImage()) : null;
        int result = postRepository.updatePost(postId, userId, content, image);
        if (result != 0) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "無法編輯此發文（不存在或無權限）");
        }
    }

    public void deletePost(Long postId, Long userId) {
        int result = postRepository.deletePost(postId, userId);
        if (result != 0) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "無法刪除此發文（不存在或無權限）");
        }
    }
}
