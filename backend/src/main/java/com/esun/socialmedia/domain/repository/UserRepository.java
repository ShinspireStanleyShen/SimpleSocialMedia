package com.esun.socialmedia.domain.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final EntityManager em;

    /**
     * 呼叫 sp_register_user，回傳 result code (0=成功, 1=手機已存在)
     */
    public Map<String, Object> register(String phone, String userName, String email,
                                        String password, String salt) {
        StoredProcedureQuery q = em.createStoredProcedureQuery("sp_register_user");
        q.registerStoredProcedureParameter("p_phone",     String.class,  ParameterMode.IN);
        q.registerStoredProcedureParameter("p_user_name", String.class,  ParameterMode.IN);
        q.registerStoredProcedureParameter("p_email",     String.class,  ParameterMode.IN);
        q.registerStoredProcedureParameter("p_password",  String.class,  ParameterMode.IN);
        q.registerStoredProcedureParameter("p_salt",      String.class,  ParameterMode.IN);
        q.registerStoredProcedureParameter("p_user_id",   Long.class,    ParameterMode.OUT);
        q.registerStoredProcedureParameter("p_result",    Integer.class, ParameterMode.OUT);
        q.setParameter("p_phone",     phone);
        q.setParameter("p_user_name", userName);
        q.setParameter("p_email",     email);
        q.setParameter("p_password",  password);
        q.setParameter("p_salt",      salt);
        q.execute();

        Map<String, Object> out = new HashMap<>();
        out.put("userId", q.getOutputParameterValue("p_user_id"));
        out.put("result", q.getOutputParameterValue("p_result"));
        return out;
    }

    /**
     * 檢查 email 是否已被使用
     */
    public boolean emailExists(String email) {
        if (email == null || email.isBlank()) return false;
        Query q = em.createNativeQuery(
                "SELECT COUNT(*) FROM users WHERE email = :email AND is_deleted = 0");
        q.setParameter("email", email);
        Number count = (Number) q.getSingleResult();
        return count.longValue() > 0;
    }

    /**
     * 依手機號取得使用者（含密碼/salt，用於登入驗證）
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> findByPhone(String phone) {
        StoredProcedureQuery q = em.createStoredProcedureQuery("sp_get_user_by_phone");
        q.registerStoredProcedureParameter("p_phone", String.class, ParameterMode.IN);
        q.setParameter("p_phone", phone);
        q.execute();
        var rows = q.getResultList();
        if (rows.isEmpty()) return Optional.empty();

        Object[] r = (Object[]) rows.get(0);
        Map<String, Object> user = new HashMap<>();
        user.put("userId",     r[0]);
        user.put("phone",      r[1]);
        user.put("userName",   r[2]);
        user.put("email",      r[3]);
        user.put("password",   r[4]);
        user.put("salt",       r[5]);
        user.put("coverImage", r[6]);
        user.put("biography",  r[7]);
        return Optional.of(user);
    }
}
