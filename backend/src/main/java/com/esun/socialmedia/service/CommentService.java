package com.esun.socialmedia.service;

import com.esun.socialmedia.common.exception.BusinessException;
import com.esun.socialmedia.common.util.SecurityUtils;
import com.esun.socialmedia.domain.dto.request.CommentRequest;
import com.esun.socialmedia.domain.dto.response.CommentResponse;
import com.esun.socialmedia.domain.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final SecurityUtils     securityUtils;

    public Long createComment(Long userId, Long postId, CommentRequest req) {
        String content = securityUtils.sanitize(req.getContent());
        Map<String, Object> out = commentRepository.createComment(userId, postId, content);
        int result = (Integer) out.get("result");
        if (result == 1) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "發文不存在");
        }
        return (Long) out.get("commentId");
    }

    public List<CommentResponse> getComments(Long postId) {
        return commentRepository.findByPostId(postId);
    }
}
