package com.esun.socialmedia.domain.repository;

import com.esun.socialmedia.domain.dto.response.CommentResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class CommentRepository {

    private final EntityManager em;

    public Map<String, Object> createComment(Long userId, Long postId, String content) {
        StoredProcedureQuery q = em.createStoredProcedureQuery("sp_create_comment");
        q.registerStoredProcedureParameter("p_user_id",    Long.class,    ParameterMode.IN);
        q.registerStoredProcedureParameter("p_post_id",    Long.class,    ParameterMode.IN);
        q.registerStoredProcedureParameter("p_content",    String.class,  ParameterMode.IN);
        q.registerStoredProcedureParameter("p_comment_id", Long.class,    ParameterMode.OUT);
        q.registerStoredProcedureParameter("p_result",     Integer.class, ParameterMode.OUT);
        q.setParameter("p_user_id", userId);
        q.setParameter("p_post_id", postId);
        q.setParameter("p_content", content);
        q.execute();

        Map<String, Object> out = new HashMap<>();
        out.put("commentId", q.getOutputParameterValue("p_comment_id"));
        out.put("result",    q.getOutputParameterValue("p_result"));
        return out;
    }

    @SuppressWarnings("unchecked")
    public List<CommentResponse> findByPostId(Long postId) {
        StoredProcedureQuery q = em.createStoredProcedureQuery("sp_get_comments_by_post");
        q.registerStoredProcedureParameter("p_post_id", Long.class, ParameterMode.IN);
        q.setParameter("p_post_id", postId);
        q.execute();
        List<Object[]> rows = q.getResultList();
        List<CommentResponse> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(CommentResponse.builder()
                    .commentId(toLong(r[0]))
                    .userId(toLong(r[1]))
                    .userName((String) r[2])
                    .authorAvatar((String) r[3])
                    .content((String) r[4])
                    .postId(postId)
                    .createdAt(toLocalDateTime(r[5]))
                    .build());
        }
        return result;
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Number n) return n.longValue();
        return null;
    }

    private static LocalDateTime toLocalDateTime(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime ldt) return ldt;
        if (o instanceof Timestamp ts) return ts.toLocalDateTime();
        return null;
    }
}
