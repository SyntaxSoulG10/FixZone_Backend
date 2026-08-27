package com.fixzone.fixzon_backend.service;

import com.fixzone.fixzon_backend.model.Booking;
import com.fixzone.fixzon_backend.model.ServiceCenter;
import com.fixzone.fixzon_backend.model.ServicePackage;
import com.fixzone.fixzon_backend.repository.BookingRepository;
import com.fixzone.fixzon_backend.repository.ServiceCenterRepository;
import com.fixzone.fixzon_backend.repository.ServicePackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Scheduling Engine for multi-tenant vehicle service centers.
 * - Enforces fixed operating hours: Morning (08:00-12:00), Lunch (12:00-13:00), Evening (13:00-18:00).
 * - Enforces mandatory 15-minute buffer between consecutive services in each lane.
 * - Calculates start times based on package duration and minimum package size to prevent dead/fragmented gaps.
 * - Generates duration-aware available START TIMES without arbitrary 15-min stepping.
 */
@Service
public class SchedulingService {

    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);

    // Fixed Operating Hours
    public static final LocalTime MORNING_START = LocalTime.of(8, 0);
    public static final LocalTime MORNING_END = LocalTime.of(12, 0);
    public static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    public static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    public static final LocalTime EVENING_START = LocalTime.of(13, 0);
    public static final LocalTime EVENING_END = LocalTime.of(18, 0);
    public static final int BUFFER_MINUTES = 15;
    public static final int DEFAULT_PACKAGE_DURATION_MINS = 60;

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final BookingRepository bookingRepository;
    private final ServiceCenterRepository serviceCenterRepository;
    private final ServicePackageRepository servicePackageRepository;

    public SchedulingService(
            BookingRepository bookingRepository,
            ServiceCenterRepository serviceCenterRepository,
            ServicePackageRepository servicePackageRepository) {
        this.bookingRepository = bookingRepository;
        this.serviceCenterRepository = serviceCenterRepository;
        this.servicePackageRepository = servicePackageRepository;
    }

    /**
     * Resolves the duration in minutes for a service package.
     */
    public int resolvePackageDuration(UUID packageId) {
        if (packageId == null) {
            return DEFAULT_PACKAGE_DURATION_MINS;
        }
        return servicePackageRepository.findById(packageId)
                .map(pkg -> (pkg.getEstimatedDurationMins() != null && pkg.getEstimatedDurationMins() > 0)
                        ? pkg.getEstimatedDurationMins()
                        : DEFAULT_PACKAGE_DURATION_MINS)
                .orElse(DEFAULT_PACKAGE_DURATION_MINS);
    }

    /**
     * Resolves the minimum package duration for a service center.
     */
    public int resolveMinPackageDuration(UUID centerId) {
        if (centerId == null) {
            return DEFAULT_PACKAGE_DURATION_MINS;
        }
        List<ServicePackage> packages = servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId);
        if (packages == null || packages.isEmpty()) {
            return DEFAULT_PACKAGE_DURATION_MINS;
        }
        return packages.stream()
                .map(ServicePackage::getEstimatedDurationMins)
                .filter(d -> d != null && d > 0)
                .min(Integer::compareTo)
                .orElse(DEFAULT_PACKAGE_DURATION_MINS);
    }

    /**
     * Resolves all active package durations available at a service center.
     */
    public List<Integer> resolveCenterPackageDurations(UUID centerId) {
        if (centerId == null) {
            return List.of(DEFAULT_PACKAGE_DURATION_MINS);
        }
        List<ServicePackage> packages = servicePackageRepository.findByServiceCenter_CenterIdAndIsActiveTrue(centerId);
        if (packages == null || packages.isEmpty()) {
            return List.of(DEFAULT_PACKAGE_DURATION_MINS);
        }
        List<Integer> durations = packages.stream()
                .map(ServicePackage::getEstimatedDurationMins)
                .filter(d -> d != null && d > 0)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return durations.isEmpty() ? List.of(DEFAULT_PACKAGE_DURATION_MINS) : durations;
    }

    /**
     * Resolves the number of operational lanes for a service center (default = 1).
     */
    public int resolveLaneCount(UUID centerId) {
        if (centerId == null) {
            return 1;
        }
        return serviceCenterRepository.findById(centerId)
                .map(sc -> (sc.getServiceLanesCount() != null && sc.getServiceLanesCount() > 0)
                        ? sc.getServiceLanesCount()
                        : 1)
                .orElse(1);
    }

    /**
     * Validates whether a requested booking slot satisfies session boundaries:
     * - Fits entirely within Morning [08:00, 12:00] OR Evening [13:00, 18:00].
     * - Does NOT cross or overlap the Lunch break [12:00, 13:00].
     */
    public boolean isValidSessionTime(LocalTime startTime, int durationMins) {
        if (startTime == null || durationMins <= 0) {
            return false;
        }

        LocalTime endTime = startTime.plusMinutes(durationMins);

        // Check if service finishes after midnight (overflow)
        if (endTime.isBefore(startTime)) {
            return false;
        }

        // Morning session check
        boolean isMorningValid = !startTime.isBefore(MORNING_START) && !endTime.isAfter(MORNING_END);

        // Evening session check
        boolean isEveningValid = !startTime.isBefore(EVENING_START) && !endTime.isAfter(EVENING_END);

        return isMorningValid || isEveningValid;
    }

    /**
     * Generates a list of available START TIMES formatted for display (e.g., "08:00 AM", "09:00 AM", "01:00 PM").
     */
    @Transactional(readOnly = true)
    public List<String> getAvailableStartTimes(UUID centerId, LocalDate date, UUID packageId) {
        int durationMins = resolvePackageDuration(packageId);
        List<LocalTime> availableTimes = getAvailableStartTimesRaw(centerId, date, durationMins, null);

        return availableTimes.stream()
                .map(time -> time.format(DISPLAY_FORMATTER))
                .collect(Collectors.toList());
    }

    /**
     * Generates raw LocalTime start times that can accommodate a service of durationMins,
     * considering minimum package duration to avoid dead idle gaps.
     */
    @Transactional(readOnly = true)
    public List<LocalTime> getAvailableStartTimesRaw(UUID centerId, LocalDate date, int durationMins, UUID excludeBookingId) {
        if (centerId == null || date == null || durationMins <= 0) {
            return Collections.emptyList();
        }

        int totalLanes = resolveLaneCount(centerId);
        int minDuration = resolveMinPackageDuration(centerId);
        List<Integer> centerDurations = resolveCenterPackageDurations(centerId);

        List<Booking> activeBookings = bookingRepository.findActiveBookingsForCenterAndDate(
                centerId, date, LocalDateTime.now());

        if (excludeBookingId != null) {
            activeBookings = activeBookings.stream()
                    .filter(b -> !excludeBookingId.equals(b.getBookingId()))
                    .collect(Collectors.toList());
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (date.isBefore(today)) {
            return Collections.emptyList();
        }

        // Build lane timelines
        List<List<TimeInterval>> laneTimelines = buildLaneTimelines(activeBookings, totalLanes);

        // Generate intelligent, gap-aware candidate start times across all lanes
        Set<LocalTime> candidateTimes = generateSmartCandidateStartTimes(laneTimelines, durationMins, minDuration, centerDurations);

        // Filter candidate times that fit in at least one lane
        List<LocalTime> validTimes = new ArrayList<>();
        for (LocalTime candidate : candidateTimes) {
            if (date.isEqual(today) && candidate.isBefore(now)) {
                continue;
            }
            if (!isValidSessionTime(candidate, durationMins)) {
                continue;
            }

            int bestLane = findBestLaneForSlot(laneTimelines, candidate, durationMins);
            if (bestLane != -1) {
                validTimes.add(candidate);
            }
        }

        validTimes.sort(Comparator.naturalOrder());
        return validTimes;
    }

    /**
     * Checks whether a specific start time and duration can be accommodated across available lanes.
     */
    @Transactional(readOnly = true)
    public boolean isSlotAvailable(UUID centerId, LocalDate date, LocalTime startTime, int durationMins, UUID excludeBookingId) {
        if (date == null || date.isBefore(LocalDate.now())) {
            return false;
        }
        if (date.isEqual(LocalDate.now()) && startTime != null && startTime.isBefore(LocalTime.now())) {
            return false;
        }
        if (!isValidSessionTime(startTime, durationMins)) {
            return false;
        }

        int totalLanes = resolveLaneCount(centerId);
        List<Booking> activeBookings = bookingRepository.findActiveBookingsForCenterAndDate(
                centerId, date, LocalDateTime.now());

        if (excludeBookingId != null) {
            activeBookings = activeBookings.stream()
                    .filter(b -> !excludeBookingId.equals(b.getBookingId()))
                    .collect(Collectors.toList());
        }

        List<List<TimeInterval>> laneTimelines = buildLaneTimelines(activeBookings, totalLanes);
        return findBestLaneForSlot(laneTimelines, startTime, durationMins) != -1;
    }

    /**
     * Builds simulated independent timelines for each of the service center's lanes.
     */
    private List<List<TimeInterval>> buildLaneTimelines(List<Booking> bookings, int totalLanes) {
        List<List<TimeInterval>> lanes = new ArrayList<>(totalLanes);
        for (int i = 0; i < totalLanes; i++) {
            lanes.add(new ArrayList<>());
        }

        List<Booking> unassigned = new ArrayList<>();

        for (Booking b : bookings) {
            if (b.getBookingTime() == null) continue;

            int duration = b.getDurationMins() != null ? b.getDurationMins() : resolvePackageDuration(b.getPackageId());
            LocalTime start = b.getBookingTime();
            LocalTime end = b.getEndTime() != null ? b.getEndTime() : start.plusMinutes(duration);
            TimeInterval interval = new TimeInterval(start, end);

            Integer assigned = b.getAssignedLane();
            if (assigned != null && assigned >= 1 && assigned <= totalLanes) {
                lanes.get(assigned - 1).add(interval);
            } else {
                unassigned.add(b);
            }
        }

        // Place unassigned bookings into the first available simulated lane
        for (Booking b : unassigned) {
            int duration = b.getDurationMins() != null ? b.getDurationMins() : resolvePackageDuration(b.getPackageId());
            LocalTime start = b.getBookingTime();
            LocalTime end = b.getEndTime() != null ? b.getEndTime() : start.plusMinutes(duration);
            TimeInterval interval = new TimeInterval(start, end);

            boolean placed = false;
            for (int l = 0; l < totalLanes; l++) {
                if (canFitInLane(lanes.get(l), start, duration)) {
                    lanes.get(l).add(interval);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                lanes.stream()
                        .min(Comparator.comparingInt(List::size))
                        .ifPresent(lane -> lane.add(interval));
            }
        }

        // Sort all lane intervals by start time
        for (List<TimeInterval> lane : lanes) {
            lane.sort(Comparator.comparing(TimeInterval::getStart));
        }

        return lanes;
    }

    /**
     * Checks if a candidate interval [startTime, startTime + duration] fits into a lane's timeline,
     * enforcing the 15-minute buffer before and after.
     */
    private boolean canFitInLane(List<TimeInterval> laneBookings, LocalTime startTime, int durationMins) {
        LocalTime endTime = startTime.plusMinutes(durationMins);

        for (TimeInterval existing : laneBookings) {
            boolean endsBeforeExisting = !endTime.plusMinutes(BUFFER_MINUTES).isAfter(existing.getStart());
            boolean startsAfterExisting = !startTime.isBefore(existing.getEnd().plusMinutes(BUFFER_MINUTES));

            if (!endsBeforeExisting && !startsAfterExisting) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds the best lane index (0 to totalLanes - 1) for a slot using Gap Minimization rules.
     */
    private int findBestLaneForSlot(List<List<TimeInterval>> lanes, LocalTime startTime, int durationMins) {
        int bestLane = -1;
        int bestScore = Integer.MAX_VALUE;

        for (int i = 0; i < lanes.size(); i++) {
            List<TimeInterval> lane = lanes.get(i);
            if (canFitInLane(lane, startTime, durationMins)) {
                int score = calculateGapScore(lane, startTime, durationMins);
                if (score < bestScore) {
                    bestScore = score;
                    bestLane = i;
                }
            }
        }

        return bestLane;
    }

    /**
     * Calculates a gap penalty score (lower is better).
     */
    private int calculateGapScore(List<TimeInterval> laneBookings, LocalTime startTime, int durationMins) {
        if (laneBookings.isEmpty()) {
            return 100;
        }

        LocalTime endTime = startTime.plusMinutes(durationMins);
        long minIdleGap = Long.MAX_VALUE;

        for (TimeInterval existing : laneBookings) {
            if (!startTime.isBefore(existing.getEnd())) {
                long idle = ChronoUnit.MINUTES.between(existing.getEnd().plusMinutes(BUFFER_MINUTES), startTime);
                if (idle >= 0 && idle < minIdleGap) {
                    minIdleGap = idle;
                }
            }
            if (!existing.getStart().isBefore(endTime)) {
                long idle = ChronoUnit.MINUTES.between(endTime.plusMinutes(BUFFER_MINUTES), existing.getStart());
                if (idle >= 0 && idle < minIdleGap) {
                    minIdleGap = idle;
                }
            }
        }

        return (minIdleGap == Long.MAX_VALUE) ? 50 : (int) minIdleGap;
    }

    /**
     * Generates intelligent candidate start times for each lane's free windows.
     * Starts from the window opening (e.g. 08:00, 13:00, or previous service end + 15m)
     * and steps forward using package durations + buffer, eliminating arbitrary 15-minute holes.
     */
    private Set<LocalTime> generateSmartCandidateStartTimes(
            List<List<TimeInterval>> lanes,
            int durationMins,
            int minDuration,
            List<Integer> centerDurations) {
        Set<LocalTime> candidates = new TreeSet<>();

        for (List<TimeInterval> lane : lanes) {
            // Find free windows in Morning (08:00 - 12:00) and Evening (13:00 - 18:00)
            List<TimeInterval> freeWindows = computeFreeWindows(lane);

            for (TimeInterval window : freeWindows) {
                long windowMinutes = ChronoUnit.MINUTES.between(window.getStart(), window.getEnd());
                if (windowMinutes < durationMins) {
                    continue; // Cannot accommodate the service
                }

                // 1. Forward stepping: Pack from window start with this package duration
                generateSteppingTimes(candidates, window, durationMins, durationMins);

                // 2. Forward stepping: Pack assuming preceding slot filled by other active package durations
                for (int otherDur : centerDurations) {
                    generateSteppingTimes(candidates, window, durationMins, otherDur);
                }

                // 3. Backward exact fill: Ensure the last slot flush with window end is always available
                LocalTime endFill = window.getEnd().minusMinutes(durationMins);
                if (!endFill.isBefore(window.getStart())) {
                    candidates.add(endFill);
                }
            }
        }

        return candidates;
    }

    /**
     * Steps through a free window, adding candidate start times that respect duration + 15m buffer.
     */
    private void generateSteppingTimes(
            Set<LocalTime> candidates,
            TimeInterval window,
            int requestedDuration,
            int stepDuration) {
        LocalTime curr = window.getStart();
        int step = stepDuration + BUFFER_MINUTES;

        while (!curr.plusMinutes(requestedDuration).isAfter(window.getEnd())) {
            candidates.add(curr);
            curr = curr.plusMinutes(step);
        }
    }

    /**
     * Computes the available free time windows in a lane for both morning and evening sessions.
     */
    private List<TimeInterval> computeFreeWindows(List<TimeInterval> laneBookings) {
        List<TimeInterval> windows = new ArrayList<>();

        // Morning Session [08:00, 12:00]
        computeSessionFreeWindows(windows, laneBookings, MORNING_START, MORNING_END);

        // Evening Session [13:00, 18:00]
        computeSessionFreeWindows(windows, laneBookings, EVENING_START, EVENING_END);

        return windows;
    }

    private void computeSessionFreeWindows(
            List<TimeInterval> windows,
            List<TimeInterval> laneBookings,
            LocalTime sessionStart,
            LocalTime sessionEnd) {
        LocalTime currentPointer = sessionStart;

        List<TimeInterval> sessionBookings = laneBookings.stream()
                .filter(b -> !b.getStart().isBefore(sessionStart) && !b.getEnd().isAfter(sessionEnd))
                .sorted(Comparator.comparing(TimeInterval::getStart))
                .collect(Collectors.toList());

        for (TimeInterval booking : sessionBookings) {
            // Free window before this booking
            LocalTime windowEnd = booking.getStart().minusMinutes(BUFFER_MINUTES);
            if (currentPointer.isBefore(sessionStart)) {
                currentPointer = sessionStart;
            }
            if (windowEnd.isAfter(currentPointer)) {
                windows.add(new TimeInterval(currentPointer, windowEnd));
            }

            // Move pointer to end of this booking + buffer
            LocalTime nextAvailable = booking.getEnd().plusMinutes(BUFFER_MINUTES);
            currentPointer = nextAvailable;
        }

        // Remaining window until session end
        if (currentPointer.isBefore(sessionEnd)) {
            windows.add(new TimeInterval(currentPointer, sessionEnd));
        }
    }

    /**
     * Helper value object representing an interval [start, end].
     */
    public static class TimeInterval {
        private final LocalTime start;
        private final LocalTime end;

        public TimeInterval(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }

        public LocalTime getStart() { return start; }
        public LocalTime getEnd() { return end; }
    }
}
