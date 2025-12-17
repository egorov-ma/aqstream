package ru.aqstream.user.db.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.aqstream.user.db.entity.User;

/**
 * Репозиторий для работы с пользователями.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Ищет пользователя по email (case-insensitive).
     *
     * @param email email пользователя
     * @return пользователь если найден
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmail(String email);

    /**
     * Ищет пользователя по Telegram ID.
     *
     * @param telegramId Telegram ID
     * @return пользователь если найден
     */
    Optional<User> findByTelegramId(String telegramId);

    /**
     * Проверяет существование пользователя с email (case-insensitive).
     *
     * @param email email для проверки
     * @return true если пользователь существует
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    boolean existsByEmail(String email);

    /**
     * Проверяет существование пользователя с Telegram ID.
     *
     * @param telegramId Telegram ID для проверки
     * @return true если пользователь существует
     */
    boolean existsByTelegramId(String telegramId);
}
