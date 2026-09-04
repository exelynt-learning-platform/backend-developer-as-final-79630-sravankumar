package com.example.resourcebooking.seed;

import com.example.resourcebooking.entity.Reservation;
import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.entity.User;
import com.example.resourcebooking.enums.ReservationStatus;
import com.example.resourcebooking.enums.ResourceType;
import com.example.resourcebooking.enums.Role;
import com.example.resourcebooking.repository.ReservationRepository;
import com.example.resourcebooking.repository.ResourceRepository;
import com.example.resourcebooking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            ResourceRepository resourceRepository,
            ReservationRepository reservationRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.reservationRepository = reservationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedRooms();
        seedSampleReservations();
    }

    private void seedUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User(
                    "admin",
                    "admin@example.com",
                    passwordEncoder.encode("Admin@123"),
                    Role.ADMIN,
                    true
            );
            userRepository.save(admin);
            log.info("Seeded default ADMIN user: 'admin'");
        }

        if (!userRepository.existsByUsername("user1")) {
            User user1 = new User(
                    "user1",
                    "user1@example.com",
                    passwordEncoder.encode("User@123"),
                    Role.USER,
                    true
            );
            userRepository.save(user1);
            log.info("Seeded default USER user: 'user1'");
        }
    }

    private void seedRooms() {
        if (resourceRepository.count() >= 20) {
            log.info("Rooms already seeded ({} found), skipping room seeding.", resourceRepository.count());
            return;
        }

        List<Resource> rooms = new ArrayList<>();
        String[] locations = {"Tower A - Floor 1", "Tower A - Floor 2", "Tower B - Floor 1", "Tower B - Floor 2"};

        for (int i = 1; i <= 20; i++) {
            int roomNumber = 100 + i;
            String roomName = "Room " + roomNumber;

            if (resourceRepository.existsByName(roomName)) {
                continue;
            }

            int capacity = 2 + ((i % 5) * 2); // 2, 4, 6, 8, 10
            BigDecimal pricePerHour = BigDecimal.valueOf(500 + ((i % 5) * 250)).setScale(2, RoundingMode.HALF_UP); // 500.00, 750.00, 1000.00, 1250.00, 1500.00
            String location = locations[(i - 1) % locations.length];
            String description = "Executive Conference Room " + roomNumber + " with high-speed Wi-Fi, 4K display, and whiteboard";

            Resource room = new Resource(
                    roomName,
                    description,
                    ResourceType.ROOM,
                    location,
                    capacity,
                    pricePerHour,
                    true
            );
            rooms.add(room);
        }

        if (!rooms.isEmpty()) {
            resourceRepository.saveAll(rooms);
            log.info("Successfully seeded {} meeting rooms (Room 101 to Room 120)", rooms.size());
        }
    }

    private void seedSampleReservations() {
        if (reservationRepository.count() > 0) {
            return;
        }

        User user1 = userRepository.findByUsername("user1").orElse(null);
        Resource room101 = resourceRepository.findAll().stream()
                .filter(r -> "Room 101".equals(r.getName()))
                .findFirst().orElse(null);

        Resource room102 = resourceRepository.findAll().stream()
                .filter(r -> "Room 102".equals(r.getName()))
                .findFirst().orElse(null);

        if (user1 != null && room101 != null && room102 != null) {
            LocalDateTime start1 = LocalDateTime.now().plusDays(10).withHour(10).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end1 = start1.plusHours(2);
            BigDecimal price1 = room101.getPricePerHour().multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP);

            Reservation res1 = new Reservation(room101, user1, start1, end1, price1, ReservationStatus.CONFIRMED);
            reservationRepository.save(res1);

            LocalDateTime start2 = LocalDateTime.now().plusDays(11).withHour(14).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end2 = start2.plusHours(3);
            BigDecimal price2 = room102.getPricePerHour().multiply(BigDecimal.valueOf(3)).setScale(2, RoundingMode.HALF_UP);

            Reservation res2 = new Reservation(room102, user1, start2, end2, price2, ReservationStatus.PENDING);
            reservationRepository.save(res2);

            log.info("Successfully seeded sample non-conflicting reservations for development testing");
        }
    }
}
