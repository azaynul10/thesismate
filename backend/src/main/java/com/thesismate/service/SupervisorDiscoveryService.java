package com.thesismate.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.SupervisorDirectoryItem;
import com.thesismate.dto.SupervisorPageResponse;
import com.thesismate.model.SupervisorCapacity;
import com.thesismate.model.SupervisorProfile;
import com.thesismate.model.Track;
import com.thesismate.repository.SupervisorCapacityRepository;
import com.thesismate.repository.SupervisorProfileRepository;

/** F2: supervisor directory search with area/keyword/track/hasSlots filters, whitelisted sorting, and pagination. */
@Service
public class SupervisorDiscoveryService {

    public static final int MAX_PAGE_SIZE = 50;

    private static final Comparator<SupervisorDirectoryItem> BY_NAME_ASC =
            Comparator.comparing((SupervisorDirectoryItem i) -> i.fullName().toLowerCase(Locale.ROOT))
                    .thenComparing(SupervisorDirectoryItem::profileId);

    private static final Map<String, Comparator<SupervisorDirectoryItem>> SORT_WHITELIST = Map.of(
            "name", BY_NAME_ASC,
            "name,asc", BY_NAME_ASC,
            "name,desc", Comparator.comparing(
                            (SupervisorDirectoryItem i) -> i.fullName().toLowerCase(Locale.ROOT),
                            Comparator.reverseOrder())
                    .thenComparing(SupervisorDirectoryItem::profileId),
            "remaining", Comparator.comparingLong(SupervisorDirectoryItem::remainingSlots).reversed()
                    .thenComparing(BY_NAME_ASC),
            "remaining,desc", Comparator.comparingLong(SupervisorDirectoryItem::remainingSlots).reversed()
                    .thenComparing(BY_NAME_ASC),
            "remaining,asc", Comparator.comparingLong(SupervisorDirectoryItem::remainingSlots)
                    .thenComparing(BY_NAME_ASC),
            "capacity", Comparator.comparingInt(SupervisorDirectoryItem::capacity).reversed()
                    .thenComparing(BY_NAME_ASC),
            "capacity,desc", Comparator.comparingInt(SupervisorDirectoryItem::capacity).reversed()
                    .thenComparing(BY_NAME_ASC),
            "capacity,asc", Comparator.comparingInt(SupervisorDirectoryItem::capacity)
                    .thenComparing(BY_NAME_ASC)
    );

    private final SupervisorProfileRepository profiles;
    private final SupervisorCapacityRepository capacities;
    private final AllocationCountProvider allocationCountProvider;

    public SupervisorDiscoveryService(SupervisorProfileRepository profiles,
                                      SupervisorCapacityRepository capacities,
                                      AllocationCountProvider allocationCountProvider) {
        this.profiles = profiles;
        this.capacities = capacities;
        this.allocationCountProvider = allocationCountProvider;
    }

    // student ra area, keyword, track, hasSlots diye filter kore supervisor khujbe
    @Transactional(readOnly = true)
    public SupervisorPageResponse search(String semester,
                                         String area,
                                         String keyword,
                                         Track track,
                                         Boolean hasSlots,
                                         String sort,
                                         int page,
                                         int size) {
        String sem = normalizeSemester(semester);
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size between 1 and " + MAX_PAGE_SIZE);
        }
        String sortKey = (sort == null || sort.isBlank()) ? "name" : sort.trim().toLowerCase(Locale.ROOT);
        Comparator<SupervisorDirectoryItem> comparator = SORT_WHITELIST.get(sortKey);
        if (comparator == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort parameter: " + sort);
        }

        String areaFilter = normalizeFilter(area);
        String keywordFilter = normalizeFilter(keyword);

        Map<Long, Integer> capacityByProfileId = capacities.findBySemester(sem).stream()
                .collect(Collectors.toMap(
                        c -> c.getProfile().getId(),
                        SupervisorCapacity::getCapacity,
                        (a, b) -> b));

        List<SupervisorDirectoryItem> filtered = profiles.findAll().stream()
                .map(p -> toItem(p, capacityByProfileId.getOrDefault(p.getId(), 0), sem))
                .filter(item -> matchesTrack(item, track))
                .filter(item -> matchesArea(item, areaFilter))
                .filter(item -> matchesKeyword(item, keywordFilter))
                .filter(item -> matchesHasSlots(item, hasSlots))
                .sorted(comparator)
                .toList();

        long totalElements = filtered.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<SupervisorDirectoryItem> pageItems = filtered.subList(fromIndex, toIndex);

        return new SupervisorPageResponse(
                pageItems,
                page,
                size,
                totalElements,
                totalPages,
                page + 1 < totalPages,
                page > 0 && totalPages > 0);
    }

    // single supervisor er public card view
    @Transactional(readOnly = true)
    public SupervisorDirectoryItem getByProfileId(Long profileId, String semester) {
        String sem = normalizeSemester(semester);
        SupervisorProfile profile = profiles.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supervisor profile not found"));
        int capacity = capacities.findByProfileIdAndSemester(profile.getId(), sem)
                .map(SupervisorCapacity::getCapacity)
                .orElse(0);
        return toItem(profile, capacity, sem);
    }

    private SupervisorDirectoryItem toItem(SupervisorProfile profile, int capacity, String semester) {
        long used = allocationCountProvider.countActiveAllocations(profile.getUser().getId(), semester);
        long remaining = Math.max(0L, capacity - used);
        List<String> sortedAreas = profile.getResearchAreas().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        List<Track> sortedTracks = profile.getTracks().stream()
                .sorted()
                .toList();
        return new SupervisorDirectoryItem(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getBio(),
                profile.getOfficeRoom(),
                sortedAreas,
                sortedTracks,
                semester,
                capacity,
                used,
                remaining,
                remaining > 0);
    }

    private static boolean matchesTrack(SupervisorDirectoryItem item, Track track) {
        return track == null || item.tracks().contains(track);
    }

    private static boolean matchesArea(SupervisorDirectoryItem item, String areaFilter) {
        if (areaFilter == null) {
            return true;
        }
        return item.researchAreas().stream()
                .anyMatch(a -> a.toLowerCase(Locale.ROOT).contains(areaFilter));
    }

    private static boolean matchesKeyword(SupervisorDirectoryItem item, String keywordFilter) {
        if (keywordFilter == null) {
            return true;
        }
        if (item.fullName().toLowerCase(Locale.ROOT).contains(keywordFilter)) {
            return true;
        }
        if (item.bio() != null && item.bio().toLowerCase(Locale.ROOT).contains(keywordFilter)) {
            return true;
        }
        if (item.officeRoom() != null && item.officeRoom().toLowerCase(Locale.ROOT).contains(keywordFilter)) {
            return true;
        }
        return item.researchAreas().stream()
                .anyMatch(a -> a.toLowerCase(Locale.ROOT).contains(keywordFilter));
    }

    private static boolean matchesHasSlots(SupervisorDirectoryItem item, Boolean hasSlots) {
        return hasSlots == null || item.available() == hasSlots;
    }

    private static String normalizeSemester(String semester) {
        if (semester == null || semester.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Semester is required");
        }
        return semester.trim();
    }

    private static String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
