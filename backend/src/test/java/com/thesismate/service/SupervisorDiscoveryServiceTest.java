package com.thesismate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.thesismate.dto.SupervisorDirectoryItem;
import com.thesismate.dto.SupervisorPageResponse;
import com.thesismate.model.AppUser;
import com.thesismate.model.Role;
import com.thesismate.model.SupervisorCapacity;
import com.thesismate.model.SupervisorProfile;
import com.thesismate.model.Track;
import com.thesismate.repository.SupervisorCapacityRepository;
import com.thesismate.repository.SupervisorProfileRepository;

@ExtendWith(MockitoExtension.class)
class SupervisorDiscoveryServiceTest {

    private static final String SEM = "Spring 2027";

    @Mock SupervisorProfileRepository profiles;
    @Mock SupervisorCapacityRepository capacities;
    @Mock AllocationCountProvider counts;

    @InjectMocks SupervisorDiscoveryService service;

    private SupervisorProfile alice;
    private SupervisorProfile bob;
    private SupervisorProfile carol;

    @BeforeEach
    void setUp() {
        alice = makeProfile(1L, 10L, "Dr. Alice Rahman", "Deep learning and NLP group", "07A-01C",
                Set.of("Machine Learning", "NLP"), Set.of(Track.THESIS, Track.PROJECT));
        bob = makeProfile(2L, 20L, "Dr. Bob Karim", "Distributed systems and cloud", "07B-02L",
                Set.of("Distributed Systems", "Cloud Computing"), Set.of(Track.PROJECT, Track.INTERNSHIP));
        carol = makeProfile(3L, 30L, "Dr. Carol Ahmed", "Computer vision and robotics", "08A-03C",
                Set.of("Computer Vision", "Machine Learning"), Set.of(Track.THESIS));
    }

    private static SupervisorProfile makeProfile(Long profileId, Long userId, String name,
                                                 String bio, String room, Set<String> areas, Set<Track> tracks) {
        AppUser user = new AppUser("sup" + userId + "@t.test", name, Role.SUPERVISOR, "hash", null);
        ReflectionTestUtils.setField(user, "id", userId);
        SupervisorProfile p = new SupervisorProfile(user, bio, room);
        ReflectionTestUtils.setField(p, "id", profileId);
        p.setResearchAreas(areas);
        p.setTracks(tracks);
        return p;
    }

    private void stubDirectory() {
        when(profiles.findAll()).thenReturn(List.of(bob, carol, alice));
        when(capacities.findBySemester(SEM)).thenReturn(List.of(
                new SupervisorCapacity(alice, SEM, 3),
                new SupervisorCapacity(bob, SEM, 1),
                new SupervisorCapacity(carol, SEM, 2)
        ));
        when(counts.countActiveAllocations(10L, SEM)).thenReturn(1L); // Alice: 2 remaining
        when(counts.countActiveAllocations(20L, SEM)).thenReturn(1L); // Bob: 0 remaining (Full)
        when(counts.countActiveAllocations(30L, SEM)).thenReturn(0L); // Carol: 2 remaining
    }

    @Test
    void filtersByAreaTrackKeywordAndHasSlots() {
        stubDirectory();

        SupervisorPageResponse byArea = service.search(SEM, "machine learning", null, null, null, "name", 0, 10);
        assertThat(byArea.items()).extracting(SupervisorDirectoryItem::fullName)
                .containsExactly("Dr. Alice Rahman", "Dr. Carol Ahmed");

        SupervisorPageResponse byTrackAndSlots = service.search(SEM, null, null, Track.PROJECT, true, "name", 0, 10);
        assertThat(byTrackAndSlots.items()).extracting(SupervisorDirectoryItem::fullName)
                .containsExactly("Dr. Alice Rahman");

        SupervisorPageResponse fullOnly = service.search(SEM, null, null, null, false, "name", 0, 10);
        assertThat(fullOnly.items()).extracting(SupervisorDirectoryItem::fullName)
                .containsExactly("Dr. Bob Karim");

        SupervisorPageResponse byKeyword = service.search(SEM, null, "08A-03C", null, null, "name", 0, 10);
        assertThat(byKeyword.items()).extracting(SupervisorDirectoryItem::fullName)
                .containsExactly("Dr. Carol Ahmed");
    }

    @Test
    void sortsByWhitelistedFieldsAndPaginates() {
        stubDirectory();

        SupervisorPageResponse byRemainingDesc = service.search(SEM, null, null, null, null, "remaining,desc", 0, 2);
        assertThat(byRemainingDesc.items()).extracting(SupervisorDirectoryItem::fullName)
                .containsExactly("Dr. Alice Rahman", "Dr. Carol Ahmed");
        assertThat(byRemainingDesc.totalElements()).isEqualTo(3);
        assertThat(byRemainingDesc.totalPages()).isEqualTo(2);
        assertThat(byRemainingDesc.hasNext()).isTrue();
        assertThat(byRemainingDesc.hasPrevious()).isFalse();

        SupervisorPageResponse pageTwo = service.search(SEM, null, null, null, null, "remaining,desc", 1, 2);
        assertThat(pageTwo.items()).extracting(SupervisorDirectoryItem::fullName)
                .containsExactly("Dr. Bob Karim");
        assertThat(pageTwo.hasNext()).isFalse();
        assertThat(pageTwo.hasPrevious()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 10, name",
            "0, 0, name",
            "0, 1000, name",
            "0, 10, invalid_sort"
    })
    void rejectsInvalidPaginationOrSortWith400(int page, int size, String sort) {
        assertThatThrownBy(() -> service.search(SEM, null, null, null, null, sort, page, size))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getByProfileIdReturnsPublicCardOr404() {
        when(profiles.findById(1L)).thenReturn(Optional.of(alice));
        when(capacities.findByProfileIdAndSemester(1L, SEM))
                .thenReturn(Optional.of(new SupervisorCapacity(alice, SEM, 4)));
        when(counts.countActiveAllocations(10L, SEM)).thenReturn(1L);

        SupervisorDirectoryItem card = service.getByProfileId(1L, SEM);
        assertThat(card.profileId()).isEqualTo(1L);
        assertThat(card.supervisorId()).isEqualTo(10L);
        assertThat(card.remainingSlots()).isEqualTo(3L);
        assertThat(card.available()).isTrue();

        when(profiles.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getByProfileId(99L, SEM))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
