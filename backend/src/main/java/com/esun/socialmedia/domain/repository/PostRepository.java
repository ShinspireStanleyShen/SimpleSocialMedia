package com.esun.socialmedia.domain.repository;

import com.esun.socialmedia.domain.dto.response.PostResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostRepository {

    private final EntityManager em;

    public Long createPost(Long userId, String content, String image) {
        StoredProcedureQuery q = em.createStoredProcedureQuery("sp_create_post");
        q.registerStoredProcedureParameter("p_user_id", Long.class,   ParameterMode.IN);
        q.registerStoredProcedureParameter("p_content", String.class, ParameterMode.IN);
        q.registerStoredProcedureParameter("p_image",   String.class, ParameterMode.IN);
        q.registerStoredProcedureParameter("p_post_id", Long.class,   ParameterMode.OUT);
        q.setParameter("p_user_id", userId);
        q.setParameter("p_content", content);
        q.setParameter("p_image",   image);
        q.execute();
        return (Long) q.getOutputParameterValue("p_post_id");
    }

    @SuppressWarnings("unchecked")
    public List<PostResponse> findAll() {
        StoredProcedureQuery q = em.createStoredProcedureQuery("sp_get_all_posts");
        q.execute();
        List<Object[]> rows = q.getResultList();
        List<PostResponse> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(mapRow(r));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Optional<PostResponse> findById(Long postId) {
        StoredProcedureQuery q = em.createStoredProcedureQuery("sp_get_post_by_id");
        q.registerStoredProcedureParameter("p_post_id", Long.class, ParameterMode.IN);
        q.setParameter("p_post_id", postId);
        q.execute();
        List<Object[]> rows = q.getResultList();
        if (rows.isEmpty()) return Optional.empty();
        return Optional.of(mapRow(rows.get(0)));
    }

    public int updatePost(Long postId, Long userId, String content, String image) {
        StoredProcedureQuery q = em.createStoredProcedureQuery("sp_update_post");
        q.registerStoredProcedureParameter("p_post_id", Long.class,    ParameterMode.IN);
        q.registerStoredProcedureParameter("p_user_id", Long.class,    ParameterMode.IN);
        q.registerStoredProcedureParameter("p_content", String.class,  ParameterMode.IN);
        q.registerStoredProcedureParameter("p_image",   String.class,  ParameterMode.IN);
        q.registerStoredProcedureParameter("p_result",  Integer.class, ParameterMode.OUT);
        q.setParameter("p_post_id", postId);
        q.setParameter("p_user_id", userId);
        q.setParameter("p_content", content);
        q.setParameter("p_image",   image);
        q.execute();
        return (Integer) q.getOutputParameterValue("p_result");
    }

    public int deletePost(Long postId, Long userId) {
        StoredProcedureQuery q = em.createStoredProcedureQuery("sp_delete_post");
        q.registerStoredProcedureParameter("p_post_id", Long.class,    ParameterMode.IN);
        q.registerStoredProcedureParameter("p_user_id", Long.class,    ParameterMode.IN);
        q.registerStoredProcedureParameter("p_result",  Integer.class, ParameterMode.OUT);
        q.setParameter("p_post_id", postId);
        q.setParameter("p_user_id", userId);
        q.execute();
        return (Integer) q.getOutputParameterValue("p_result");
    }

    private PostResponse mapRow(Object[] r) {
        long commentCount = r.length > 8 && r[8] instanceof Number n ? n.longValue() : 0L;
        return PostResponse.builder()
                .postId(toLong(r[0]))
                .userId(toLong(r[1]))
                .userName((String) r[2])
                .authorAvatar((String) r[3])
                .content((String) r[4])
                .image((String) r[5])
                .createdAt(toLocalDateTime(r[6]))
                .updatedAt(toLocalDateTime(r[7]))
                .commentCount(commentCount)
                .build();
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
