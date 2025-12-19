package ru.aqstream.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit тесты для TenantContext.
 * Проверяют корректность работы ThreadLocal хранилища tenant_id.
 */
class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("setTenantId / getTenantId")
    class SetAndGetTenantId {

        @Test
        @DisplayName("устанавливает и возвращает tenant_id")
        void setTenantId_ValidUuid_ReturnsSameUuid() {
            // given
            UUID tenantId = UUID.randomUUID();

            // when
            TenantContext.setTenantId(tenantId);

            // then
            assertThat(TenantContext.getTenantId()).isEqualTo(tenantId);
        }

        @Test
        @DisplayName("выбрасывает исключение если tenant_id не установлен")
        void getTenantId_NotSet_ThrowsException() {
            // when / then
            assertThatThrownBy(TenantContext::getTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant context не установлен");
        }

        @Test
        @DisplayName("перезаписывает предыдущее значение")
        void setTenantId_CalledTwice_OverwritesPrevious() {
            // given
            UUID firstTenantId = UUID.randomUUID();
            UUID secondTenantId = UUID.randomUUID();

            // when
            TenantContext.setTenantId(firstTenantId);
            TenantContext.setTenantId(secondTenantId);

            // then
            assertThat(TenantContext.getTenantId()).isEqualTo(secondTenantId);
        }
    }

    @Nested
    @DisplayName("getTenantIdOptional")
    class GetTenantIdOptional {

        @Test
        @DisplayName("возвращает Optional с tenant_id если установлен")
        void getTenantIdOptional_WhenSet_ReturnsOptionalWithValue() {
            // given
            UUID tenantId = UUID.randomUUID();
            TenantContext.setTenantId(tenantId);

            // when
            Optional<UUID> result = TenantContext.getTenantIdOptional();

            // then
            assertThat(result).isPresent().contains(tenantId);
        }

        @Test
        @DisplayName("возвращает пустой Optional если не установлен")
        void getTenantIdOptional_WhenNotSet_ReturnsEmpty() {
            // when
            Optional<UUID> result = TenantContext.getTenantIdOptional();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isSet")
    class IsSet {

        @Test
        @DisplayName("возвращает true если tenant_id установлен")
        void isSet_WhenSet_ReturnsTrue() {
            // given
            TenantContext.setTenantId(UUID.randomUUID());

            // when / then
            assertThat(TenantContext.isSet()).isTrue();
        }

        @Test
        @DisplayName("возвращает false если tenant_id не установлен")
        void isSet_WhenNotSet_ReturnsFalse() {
            // when / then
            assertThat(TenantContext.isSet()).isFalse();
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("очищает tenant_id")
        void clear_WhenSet_RemovesTenantId() {
            // given
            TenantContext.setTenantId(UUID.randomUUID());

            // when
            TenantContext.clear();

            // then
            assertThat(TenantContext.isSet()).isFalse();
        }

        @Test
        @DisplayName("не выбрасывает исключение если не установлен")
        void clear_WhenNotSet_DoesNotThrow() {
            // when / then — не должно выбросить исключение
            TenantContext.clear();
            assertThat(TenantContext.isSet()).isFalse();
        }
    }

    @Nested
    @DisplayName("Thread Isolation")
    class ThreadIsolation {

        @Test
        @DisplayName("tenant_id изолирован между потоками")
        void tenantId_DifferentThreads_AreIsolated() throws InterruptedException {
            // given
            UUID mainThreadTenantId = UUID.randomUUID();
            UUID otherThreadTenantId = UUID.randomUUID();
            AtomicReference<UUID> otherThreadResult = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            TenantContext.setTenantId(mainThreadTenantId);

            // when — устанавливаем другой tenant_id в другом потоке
            Thread otherThread = new Thread(() -> {
                TenantContext.setTenantId(otherThreadTenantId);
                otherThreadResult.set(TenantContext.getTenantId());
                latch.countDown();
            });
            otherThread.start();
            latch.await();

            // then — каждый поток имеет свой tenant_id
            assertThat(TenantContext.getTenantId()).isEqualTo(mainThreadTenantId);
            assertThat(otherThreadResult.get()).isEqualTo(otherThreadTenantId);
        }

        @Test
        @DisplayName("tenant_id не передаётся в дочерний поток")
        void tenantId_ChildThread_DoesNotInherit() throws InterruptedException {
            // given
            UUID parentTenantId = UUID.randomUUID();
            AtomicReference<Boolean> childThreadIsSet = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);

            TenantContext.setTenantId(parentTenantId);

            // when — создаём дочерний поток
            Thread childThread = new Thread(() -> {
                childThreadIsSet.set(TenantContext.isSet());
                latch.countDown();
            });
            childThread.start();
            latch.await();

            // then — дочерний поток не имеет tenant_id родителя
            assertThat(childThreadIsSet.get()).isFalse();
        }
    }
}
