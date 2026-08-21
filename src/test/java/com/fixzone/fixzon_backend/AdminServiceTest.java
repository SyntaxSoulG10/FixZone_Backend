package com.fixzone.fixzon_backend;

import com.fixzone.fixzon_backend.DTO.NotificationDTO;
import com.fixzone.fixzon_backend.DTO.ServiceCenterDTO;
import com.fixzone.fixzon_backend.DTO.SubscriptionDTO;
import com.fixzone.fixzon_backend.DTO.UserDTO;
import com.fixzone.fixzon_backend.model.*;
import com.fixzone.fixzon_backend.repository.*;
import com.fixzone.fixzon_backend.service.AdminService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * =========================================================
 *  Unit Tests – AdminService (Super Admin Dashboard)
 *  Section: 7.2.x  Super Admin Dashboard Management
 * =========================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService – Super Admin Dashboard Unit Tests")
class AdminServiceTest {

    /* ── Mocked repositories ── */
    @Mock private ServiceCenterRepository serviceCenterRepository;
    @Mock private UserRepository           userRepository;
    @Mock private NotificationRepository   notificationRepository;
    @Mock private SubscriptionRepository   subscriptionRepository;
    @Mock private OwnerRepository          ownerRepository;

    @InjectMocks
    private AdminService adminService;

    // ─────────────────────────────────────────────────────────────
    //  SECTION 1 – Dashboard Stats (GET /api/admin/stats)
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("1. Dashboard Stats")
    class DashboardStatsTests {

        @Test
        @DisplayName("TC-BE-01: getSystemStats returns correct total user count")
        void getSystemStats_returnsCorrectUserCount() {
            // Arrange
            when(userRepository.count()).thenReturn(42L);
            when(serviceCenterRepository.count()).thenReturn(10L);
            when(serviceCenterRepository.findByStatus("PENDING")).thenReturn(List.of());

            // Act
            Map<String, Object> stats = adminService.getSystemStats();

            // Assert
            assertThat(stats).containsEntry("totalUsers", 42L);
        }

        @Test
        @DisplayName("TC-BE-02: getSystemStats returns correct total service centers count")
        void getSystemStats_returnsCorrectServiceCenterCount() {
            when(userRepository.count()).thenReturn(10L);
            when(serviceCenterRepository.count()).thenReturn(7L);
            when(serviceCenterRepository.findByStatus("PENDING")).thenReturn(List.of());

            Map<String, Object> stats = adminService.getSystemStats();

            assertThat(stats).containsEntry("totalServiceCenters", 7L);
        }

        @Test
        @DisplayName("TC-BE-03: getSystemStats counts pending registrations correctly")
        void getSystemStats_countsPendingRegistrations() {
            ServiceCenter pending1 = new ServiceCenter();
            ServiceCenter pending2 = new ServiceCenter();
            when(userRepository.count()).thenReturn(5L);
            when(serviceCenterRepository.count()).thenReturn(5L);
            when(serviceCenterRepository.findByStatus("PENDING"))
                    .thenReturn(List.of(pending1, pending2));

            Map<String, Object> stats = adminService.getSystemStats();

            assertThat(stats).containsEntry("pendingRegistrations", 2);
        }

        @Test
        @DisplayName("TC-BE-04: getSystemStats returns all three keys")
        void getSystemStats_containsAllRequiredKeys() {
            when(userRepository.count()).thenReturn(0L);
            when(serviceCenterRepository.count()).thenReturn(0L);
            when(serviceCenterRepository.findByStatus("PENDING")).thenReturn(List.of());

            Map<String, Object> stats = adminService.getSystemStats();

            assertThat(stats).containsKeys("totalUsers", "totalServiceCenters", "pendingRegistrations");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  SECTION 2 – User Management (GET /api/admin/users)
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("2. User Management")
    class UserManagementTests {

        @Test
        @DisplayName("TC-BE-05: getAllUsers returns mapped UserDTOs for all users")
        void getAllUsers_returnsMappedDTOs() {
            User user = createUser(UUID.randomUUID(), "Ali", "ali@test.com", "CUSTOMER", "Active");
            when(userRepository.findAll()).thenReturn(List.of(user));

            List<UserDTO> result = adminService.getAllUsers();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFullName()).isEqualTo("Ali");
        }

        @Test
        @DisplayName("TC-BE-06: getAllUsers returns empty list when no users exist")
        void getAllUsers_returnsEmptyList_whenNoUsers() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<UserDTO> result = adminService.getAllUsers();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("TC-BE-07: updateUserStatus changes status to Suspended")
        void updateUserStatus_toSuspended() {
            UUID id = UUID.randomUUID();
            User user = createUser(id, "Bob", "bob@test.com", "CUSTOMER", "Active");
            when(userRepository.findById(id)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDTO result = adminService.updateUserStatus(id, "Suspended", "Violation of policy");

            assertThat(result.getStatus()).isEqualTo("Suspended");
            verify(notificationRepository, times(1)).save(any(Notification.class));
        }

        @Test
        @DisplayName("TC-BE-08: updateUserStatus changes status to Active")
        void updateUserStatus_toActive() {
            UUID id = UUID.randomUUID();
            User user = createUser(id, "Clara", "clara@test.com", "CUSTOMER", "Suspended");
            when(userRepository.findById(id)).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserDTO result = adminService.updateUserStatus(id, "Active", null);

            assertThat(result.getStatus()).isEqualTo("Active");
        }

        @Test
        @DisplayName("TC-BE-09: updateUserStatus throws when user not found")
        void updateUserStatus_throwsWhenUserNotFound() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateUserStatus(id, "Suspended", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("TC-BE-10: updateUserStatus with null id throws NullPointerException")
        void updateUserStatus_nullId_throwsNPE() {
            assertThatThrownBy(() -> adminService.updateUserStatus(null, "Active", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  SECTION 3 – Service Center Management
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("3. Service Center Management")
    class ServiceCenterManagementTests {

        @Test
        @DisplayName("TC-BE-11: getAllServiceCenters returns all centers")
        void getAllServiceCenters_returnsAll() {
            ServiceCenter sc = createServiceCenter(UUID.randomUUID(), "AutoFix", "APPROVED");
            when(serviceCenterRepository.findAll()).thenReturn(List.of(sc));

            List<ServiceCenterDTO> result = adminService.getAllServiceCenters();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("AutoFix");
        }

        @Test
        @DisplayName("TC-BE-12: getPendingServiceCenters returns only PENDING centers")
        void getPendingServiceCenters_returnsOnlyPending() {
            ServiceCenter pending = createServiceCenter(UUID.randomUUID(), "PendingCo", "PENDING");
            when(serviceCenterRepository.findByStatus("PENDING")).thenReturn(List.of(pending));

            List<ServiceCenterDTO> result = adminService.getPendingServiceCenters();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("TC-BE-13: approveServiceCenter sets status to APPROVED")
        void approveServiceCenter_setsStatusApproved() {
            UUID id = UUID.randomUUID();
            ServiceCenter sc = createServiceCenter(id, "GoodCenter", "PENDING");
            when(serviceCenterRepository.findById(id)).thenReturn(Optional.of(sc));
            when(serviceCenterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceCenterDTO result = adminService.approveServiceCenter(id);

            assertThat(result.getStatus()).isEqualTo("APPROVED");
            assertThat(result.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("TC-BE-14: approveServiceCenter sends notification to owner")
        void approveServiceCenter_sendsOwnerNotification() {
            UUID id = UUID.randomUUID();
            ServiceCenter sc = createServiceCenter(id, "AlertCenter", "PENDING");
            User owner = createUser(UUID.randomUUID(), "Owner", "owner@test.com", "OWNER", "Active");
            sc.setOwner(owner);

            when(serviceCenterRepository.findById(id)).thenReturn(Optional.of(sc));
            when(serviceCenterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            adminService.approveServiceCenter(id);

            verify(notificationRepository, times(1)).save(any(Notification.class));
        }

        @Test
        @DisplayName("TC-BE-15: rejectServiceCenter sets status to REJECTED with reason")
        void rejectServiceCenter_setsStatusRejected() {
            UUID id = UUID.randomUUID();
            ServiceCenter sc = createServiceCenter(id, "BadDocs", "PENDING");
            when(serviceCenterRepository.findById(id)).thenReturn(Optional.of(sc));
            when(serviceCenterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceCenterDTO result = adminService.rejectServiceCenter(id, "Incomplete documents");

            assertThat(result.getStatus()).isEqualTo("REJECTED");
            assertThat(result.getRejectionReason()).isEqualTo("Incomplete documents");
        }


        @Test
        @DisplayName("TC-BE-16: updateServiceCenterStatus updates status and active flag")
        void updateServiceCenterStatus_updatesStatusAndActiveFlag() {
            UUID id = UUID.randomUUID();
            ServiceCenter sc = createServiceCenter(id, "Test Center", "APPROVED");
            when(serviceCenterRepository.findById(id)).thenReturn(Optional.of(sc));
            when(serviceCenterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ServiceCenterDTO result = adminService.updateServiceCenterStatus(id, "SUSPENDED");

            assertThat(result.getStatus()).isEqualTo("SUSPENDED");
            assertThat(result.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("TC-BE-17: approveServiceCenter throws when center not found")
        void approveServiceCenter_throwsWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(serviceCenterRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.approveServiceCenter(id))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Service Center not found");
        }

        @Test
        @DisplayName("TC-BE-18: approveServiceCenter with null id throws NullPointerException")
        void approveServiceCenter_nullId_throwsNPE() {
            assertThatThrownBy(() -> adminService.approveServiceCenter(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  SECTION 4 – Subscription Management
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("4. Subscription Management")
    class SubscriptionManagementTests {

        @Test
        @DisplayName("TC-BE-19: getSubscriptions(null) returns all subscriptions")
        void getSubscriptions_withNullStatus_returnsAll() {
            Subscription sub = createSubscription(UUID.randomUUID(), "ACTIVE");
            when(subscriptionRepository.findAllByOrderByStartDateDesc()).thenReturn(List.of(sub));

            List<SubscriptionDTO> result = adminService.getSubscriptions(null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("TC-BE-20: getSubscriptions(ALL) returns all subscriptions")
        void getSubscriptions_withALLStatus_returnsAll() {
            Subscription sub = createSubscription(UUID.randomUUID(), "ACTIVE");
            when(subscriptionRepository.findAllByOrderByStartDateDesc()).thenReturn(List.of(sub));

            List<SubscriptionDTO> result = adminService.getSubscriptions("ALL");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("TC-BE-21: getSubscriptions with specific status filters correctly")
        void getSubscriptions_withSpecificStatus_filters() {
            Subscription sub = createSubscription(UUID.randomUUID(), "EXPIRED");
            when(subscriptionRepository.findByStatus("EXPIRED")).thenReturn(List.of(sub));

            List<SubscriptionDTO> result = adminService.getSubscriptions("EXPIRED");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("TC-BE-22: updateSubscriptionStatus changes to CANCELLED")
        void updateSubscriptionStatus_toCancelled() {
            UUID id = UUID.randomUUID();
            Subscription sub = createSubscription(id, "ACTIVE");
            Owner owner = new Owner();
            owner.setUserId(UUID.randomUUID());
            sub.setOwner(owner);
            when(subscriptionRepository.findById(id)).thenReturn(Optional.of(sub));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ownerRepository.findById(any())).thenReturn(Optional.of(owner));

            SubscriptionDTO result = adminService.updateSubscriptionStatus(id, "CANCELLED");

            assertThat(result.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("TC-BE-23: updateSubscriptionStatus sends notification to owner")
        void updateSubscriptionStatus_sendsNotification() {
            UUID id = UUID.randomUUID();
            Subscription sub = createSubscription(id, "ACTIVE");
            Owner owner = new Owner();
            owner.setUserId(UUID.randomUUID());
            sub.setOwner(owner);
            when(subscriptionRepository.findById(id)).thenReturn(Optional.of(sub));
            when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ownerRepository.findById(any())).thenReturn(Optional.of(owner));

            adminService.updateSubscriptionStatus(id, "CANCELLED");

            verify(notificationRepository, times(1)).save(any(Notification.class));
        }

        @Test
        @DisplayName("TC-BE-24: updateSubscriptionStatus throws when subscription not found")
        void updateSubscriptionStatus_throwsWhenNotFound() {
            UUID id = UUID.randomUUID();
            when(subscriptionRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.updateSubscriptionStatus(id, "CANCELLED"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Subscription not found");
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  SECTION 5 – Notifications & Broadcast
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("5. Notifications & Broadcast")
    class NotificationsTests {

        @Test
        @DisplayName("TC-BE-25: getAdminNotifications returns all notifications")
        void getAdminNotifications_returnsAll() {
            Notification note = new Notification();
            note.setTitle("Test Notification");
            when(notificationRepository.findAll()).thenReturn(List.of(note));

            List<NotificationDTO> result = adminService.getAdminNotifications();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("TC-BE-26: broadcastCustomNotification broadcasts to all users")
        void broadcastToAll_savesNotificationsForAllUsers() {
            User u1 = createUser(UUID.randomUUID(), "U1", "u1@x.com", "CUSTOMER", "Active");
            User u2 = createUser(UUID.randomUUID(), "U2", "u2@x.com", "CUSTOMER", "Active");
            when(userRepository.findAll()).thenReturn(List.of(u1, u2));

            adminService.broadcastCustomNotification("Hello", "World", "INFO", "ALL", null, null);

            verify(notificationRepository, times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("TC-BE-27: broadcastCustomNotification targets only OWNER role")
        void broadcastToOwners_targetsCorrectRole() {
            User owner = createUser(UUID.randomUUID(), "Owner", "o@x.com", "OWNER", "Active");
            when(userRepository.findByRole("ROLE_COMPANY_OWNER")).thenReturn(List.of(owner));

            adminService.broadcastCustomNotification("Plan Update", "Your plan...", "INFO", "OWNER", null, null);

            verify(userRepository).findByRole("ROLE_COMPANY_OWNER");
            verify(notificationRepository, times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("TC-BE-28: broadcastCustomNotification targets specific user by ID")
        void broadcastToSpecificUser_targetsById() {
            UUID userId = UUID.randomUUID();
            User user = createUser(userId, "Specific", "s@x.com", "CUSTOMER", "Active");
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            adminService.broadcastCustomNotification("Hi", "Message", "INFO", "ALL", null, userId.toString());

            verify(userRepository).findById(userId);
            verify(notificationRepository, times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("TC-BE-29: broadcastCustomNotification with unknown targetRole saves empty list")
        void broadcastUnknownRole_savesEmptyList() {
            adminService.broadcastCustomNotification("Hi", "Msg", "INFO", "UNKNOWN_ROLE", null, null);

            verify(notificationRepository, times(1)).saveAll(Collections.emptyList());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  SECTION 6 – SuperAdmin Analytics Service
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("6. SuperAdminAnalyticsService – Utility Methods")
    class AnalyticsUtilityTests {

        @Test
        @DisplayName("TC-BE-30: getSystemStats with zero users returns 0 for totalUsers")
        void systemStats_withZeroUsers() {
            when(userRepository.count()).thenReturn(0L);
            when(serviceCenterRepository.count()).thenReturn(0L);
            when(serviceCenterRepository.findByStatus("PENDING")).thenReturn(List.of());

            Map<String, Object> stats = adminService.getSystemStats();

            assertThat(stats.get("totalUsers")).isEqualTo(0L);
            assertThat(stats.get("pendingRegistrations")).isEqualTo(0);
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────

    private User createUser(UUID id, String name, String email, String role, String status) {
        User user = new User();
        user.setUserId(id);
        user.setFullName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }

    private ServiceCenter createServiceCenter(UUID id, String name, String status) {
        ServiceCenter sc = new ServiceCenter();
        sc.setCenterId(id);
        sc.setName(name);
        sc.setStatus(status);
        sc.setIsActive("APPROVED".equals(status));
        return sc;
    }

    private Subscription createSubscription(UUID id, String status) {
        Subscription sub = new Subscription();
        sub.setId(id);
        sub.setStatus(status);
        return sub;
    }
}
