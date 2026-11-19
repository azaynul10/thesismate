package com.thesismate.service;

import java.util.ArrayList;
import java.util.List;

import com.thesismate.model.Phase;
import com.thesismate.model.Track;

/**
 * F16: the starting milestones for each track. A group copies its track's template once and then
 * adds, edits or removes milestones freely. The key is stored with the copy so it can never be applied twice.
 */
public final class MilestoneTemplates {

    /** One milestone of a template. */
    public record Item(String key, String title, String description, Phase phase) {
    }

    private MilestoneTemplates() {
    }

    public static List<Item> forTrack(Track track) {
        List<Item> items = new ArrayList<>();
        switch (track) {
            case THESIS -> {
                add(items, "T1", "Literature review", "Read and summarise the related work.", Phase.PRE_THESIS_1);
                add(items, "T2", "Research problem and objectives", "Define the problem, research questions and scope.", Phase.PRE_THESIS_1);
                add(items, "T3", "Pre-Thesis 1 report", "Write the Pre-Thesis 1 report and run the Turnitin checks.", Phase.PRE_THESIS_1);
                add(items, "T4", "Methodology and experiment design", "Design the method, datasets and evaluation.", Phase.PRE_THESIS_2);
                add(items, "T5", "Implementation and preliminary results", "Build the approach and collect first results.", Phase.PRE_THESIS_2);
                add(items, "T6", "Pre-Thesis 2 report and poster", "Write the Pre-Thesis 2 report and prepare the poster.", Phase.PRE_THESIS_2);
                add(items, "T7", "Final experiments and analysis", "Finish the experiments and analyse the results.", Phase.FINAL);
                add(items, "T8", "Thesis report and IEEE paper", "Write the thesis report and the IEEE format paper.", Phase.FINAL);
                add(items, "T9", "Final defense preparation", "Prepare the slides and rehearse the defense.", Phase.FINAL);
            }
            case PROJECT -> {
                add(items, "P1", "Requirements and scope", "Collect and agree the requirements.", Phase.PRE_THESIS_1);
                add(items, "P2", "System design", "Architecture, data model and interface design.", Phase.PRE_THESIS_1);
                add(items, "P3", "Pre-Thesis 1 technical report", "Write the Pre-Thesis 1 technical report.", Phase.PRE_THESIS_1);
                add(items, "P4", "Prototype implementation", "Build the working prototype.", Phase.PRE_THESIS_2);
                add(items, "P5", "Testing and refinement", "Test the prototype and fix the problems found.", Phase.PRE_THESIS_2);
                add(items, "P6", "Prototype release and report", "Publish the prototype release and write the report.", Phase.PRE_THESIS_2);
                add(items, "P7", "Final release and demo", "Prepare the final release and the demo.", Phase.FINAL);
                add(items, "P8", "Project report", "Write the final project report.", Phase.FINAL);
            }
            case INTERNSHIP -> {
                add(items, "I1", "Onboarding", "Placement paperwork, introductions and the learning plan.", Phase.PRE_THESIS_1);
                add(items, "I2", "First assigned work", "Complete the first tasks given by the company.", Phase.PRE_THESIS_1);
                add(items, "I3", "Main assigned work", "Deliver the main work of the internship.", Phase.PRE_THESIS_2);
                add(items, "I4", "Internship report draft", "Write a draft of the internship report.", Phase.PRE_THESIS_2);
                add(items, "I5", "Internship report", "Finish the internship report.", Phase.FINAL);
                add(items, "I6", "Internship presentation", "Prepare and give the presentation.", Phase.FINAL);
            }
        }
        return items;
    }

    private static void add(List<Item> items, String key, String title, String description, Phase phase) {
        items.add(new Item(key, title, description, phase));
    }
}
