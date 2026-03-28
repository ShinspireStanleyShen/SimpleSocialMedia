-- ============================================================
-- Simple Social Media Platform - Stored Procedures
-- ============================================================

USE simple_social_media;

DELIMITER $$

-- ============================================================
-- SP: sp_register_user  (註冊使用者)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_register_user$$
CREATE PROCEDURE sp_register_user(
    IN  p_phone      VARCHAR(20),
    IN  p_user_name  VARCHAR(100),
    IN  p_email      VARCHAR(255),
    IN  p_password   VARCHAR(255),
    IN  p_salt       VARCHAR(64),
    OUT p_user_id    BIGINT,
    OUT p_result     INT          -- 0=success, 1=phone exists
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = -1;
    END;

    START TRANSACTION;

    IF EXISTS (SELECT 1 FROM users WHERE phone = p_phone) THEN
        SET p_result = 1;
        ROLLBACK;
    ELSE
        INSERT INTO users (phone, user_name, email, password, salt)
        VALUES (p_phone, p_user_name, p_email, p_password, p_salt);

        SET p_user_id = LAST_INSERT_ID();
        SET p_result  = 0;
        COMMIT;
    END IF;
END$$

-- ============================================================
-- SP: sp_get_user_by_phone  (依手機號取得使用者)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_get_user_by_phone$$
CREATE PROCEDURE sp_get_user_by_phone(
    IN p_phone VARCHAR(20)
)
BEGIN
    SELECT user_id, phone, user_name, email, password, salt,
           cover_image, biography, created_at
    FROM   users
    WHERE  phone = p_phone
      AND  is_deleted = 0
    LIMIT 1;
END$$

-- ============================================================
-- SP: sp_get_user_by_id  (依 ID 取得使用者)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_get_user_by_id$$
CREATE PROCEDURE sp_get_user_by_id(
    IN p_user_id BIGINT
)
BEGIN
    SELECT user_id, phone, user_name, email,
           cover_image, biography, created_at
    FROM   users
    WHERE  user_id   = p_user_id
      AND  is_deleted = 0
    LIMIT 1;
END$$

-- ============================================================
-- SP: sp_create_post  (新增發文)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_create_post$$
CREATE PROCEDURE sp_create_post(
    IN  p_user_id  BIGINT,
    IN  p_content  TEXT,
    IN  p_image    VARCHAR(500),
    OUT p_post_id  BIGINT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_post_id = -1;
    END;

    START TRANSACTION;
    INSERT INTO posts (user_id, content, image)
    VALUES (p_user_id, p_content, p_image);
    SET p_post_id = LAST_INSERT_ID();
    COMMIT;
END$$

-- ============================================================
-- SP: sp_get_all_posts  (取得所有發文，含作者資訊)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_get_all_posts$$
CREATE PROCEDURE sp_get_all_posts()
BEGIN
    SELECT p.post_id,
           p.user_id,
           u.user_name,
           u.cover_image AS author_avatar,
           p.content,
           p.image,
           p.created_at,
           p.updated_at,
           (SELECT COUNT(*) FROM comments c
            WHERE c.post_id = p.post_id AND c.is_deleted = 0) AS comment_count
    FROM   posts p
    JOIN   users u ON u.user_id = p.user_id AND u.is_deleted = 0
    WHERE  p.is_deleted = 0
    ORDER  BY p.created_at DESC;
END$$

-- ============================================================
-- SP: sp_get_post_by_id  (取得單篇發文)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_get_post_by_id$$
CREATE PROCEDURE sp_get_post_by_id(
    IN p_post_id BIGINT
)
BEGIN
    SELECT p.post_id,
           p.user_id,
           u.user_name,
           u.cover_image AS author_avatar,
           p.content,
           p.image,
           p.created_at,
           p.updated_at
    FROM   posts p
    JOIN   users u ON u.user_id = p.user_id AND u.is_deleted = 0
    WHERE  p.post_id   = p_post_id
      AND  p.is_deleted = 0
    LIMIT 1;
END$$

-- ============================================================
-- SP: sp_update_post  (編輯發文)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_update_post$$
CREATE PROCEDURE sp_update_post(
    IN  p_post_id  BIGINT,
    IN  p_user_id  BIGINT,
    IN  p_content  TEXT,
    IN  p_image    VARCHAR(500),
    OUT p_result   INT          -- 0=success, 1=not found/not owner
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = -1;
    END;

    START TRANSACTION;
    UPDATE posts
    SET    content = p_content,
           image   = p_image
    WHERE  post_id    = p_post_id
      AND  user_id    = p_user_id
      AND  is_deleted = 0;

    IF ROW_COUNT() = 0 THEN
        SET p_result = 1;
        ROLLBACK;
    ELSE
        SET p_result = 0;
        COMMIT;
    END IF;
END$$

-- ============================================================
-- SP: sp_delete_post  (刪除發文，同步軟刪除留言)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_delete_post$$
CREATE PROCEDURE sp_delete_post(
    IN  p_post_id  BIGINT,
    IN  p_user_id  BIGINT,
    OUT p_result   INT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = -1;
    END;

    START TRANSACTION;
    UPDATE posts
    SET    is_deleted = 1
    WHERE  post_id    = p_post_id
      AND  user_id    = p_user_id
      AND  is_deleted = 0;

    IF ROW_COUNT() = 0 THEN
        SET p_result = 1;
        ROLLBACK;
    ELSE
        -- 同步軟刪除該文所有留言 (Transaction)
        UPDATE comments
        SET    is_deleted = 1
        WHERE  post_id    = p_post_id
          AND  is_deleted  = 0;

        SET p_result = 0;
        COMMIT;
    END IF;
END$$

-- ============================================================
-- SP: sp_create_comment  (新增留言)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_create_comment$$
CREATE PROCEDURE sp_create_comment(
    IN  p_user_id    BIGINT,
    IN  p_post_id    BIGINT,
    IN  p_content    TEXT,
    OUT p_comment_id BIGINT,
    OUT p_result     INT          -- 0=success, 1=post not found
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_result = -1;
    END;

    START TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM posts WHERE post_id = p_post_id AND is_deleted = 0) THEN
        SET p_result = 1;
        ROLLBACK;
    ELSE
        INSERT INTO comments (user_id, post_id, content)
        VALUES (p_user_id, p_post_id, p_content);
        SET p_comment_id = LAST_INSERT_ID();
        SET p_result     = 0;
        COMMIT;
    END IF;
END$$

-- ============================================================
-- SP: sp_get_comments_by_post  (取得發文的所有留言)
-- ============================================================
DROP PROCEDURE IF EXISTS sp_get_comments_by_post$$
CREATE PROCEDURE sp_get_comments_by_post(
    IN p_post_id BIGINT
)
BEGIN
    SELECT c.comment_id,
           c.user_id,
           u.user_name,
           u.cover_image AS author_avatar,
           c.content,
           c.created_at
    FROM   comments c
    JOIN   users u ON u.user_id = c.user_id AND u.is_deleted = 0
    WHERE  c.post_id   = p_post_id
      AND  c.is_deleted = 0
    ORDER  BY c.created_at ASC;
END$$

DELIMITER ;
