package com.thesismate.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.thesismate.model.Phase;
import com.thesismate.model.Track;
import com.thesismate.service.MilestoneTemplates.Item;

class MilestoneTemplatesTest {

    @Test
    void everyTrackHasATemplateWithAllPhases() {
        for (Track track : Track.values()) {
            List<Item> items = MilestoneTemplates.forTrack(track);
            assertFalse(items.isEmpty(), track + " has no template");
            Set<Phase> phases = new HashSet<>();
            items.forEach(i -> phases.add(i.phase()));
            assertEquals(Set.of(Phase.values()), phases, track + " misses a phase");
        }
    }

    @Test
    void keysAreUniqueAcrossAllTemplates() {
        Set<String> keys = new HashSet<>();
        for (Track track : Track.values()) {
            for (Item item : MilestoneTemplates.forTrack(track)) {
                assertTrue(keys.add(item.key()), "duplicate key " + item.key());
                assertFalse(item.title().isBlank());
                assertTrue(item.title().length() <= 200);
            }
        }
    }

    @Test
    void keysFitTheDatabaseColumnAndDoNotClashWithTheFollowUpBucket() {
        for (Track track : Track.values()) {
            for (Item item : MilestoneTemplates.forTrack(track)) {
                assertTrue(item.key().length() <= 60);
                assertFalse(item.key().equals("FOLLOW_UPS"));
            }
        }
    }

    @Test
    void internshipTemplateStartsWithOnboarding() {
        assertEquals("Onboarding", MilestoneTemplates.forTrack(Track.INTERNSHIP).get(0).title());
    }
}
