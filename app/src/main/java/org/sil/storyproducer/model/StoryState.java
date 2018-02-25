package org.sil.storyproducer.model;

import android.content.Context;
import android.content.SharedPreferences;

import org.sil.storyproducer.R;
import org.sil.storyproducer.controller.RegistrationActivity;
import org.sil.storyproducer.controller.export.CreateActivity;;
import org.sil.storyproducer.controller.export.ShareActivity;
import org.sil.storyproducer.controller.learn.LearnActivity;
import org.sil.storyproducer.controller.pager.PagerBaseActivity;
import org.sil.storyproducer.controller.remote.WholeStoryBackTranslationActivity;
import org.sil.storyproducer.tools.StorySharedPreferences;

/**
 * StoryState is a static class that holds the information of the state of a story project
 */
public final class StoryState {

    public static final String LEARN_PHASE = "Learn";       //used in StorySharedPreferences

    private static Context context;
    private static String storyName;
    private static Phase currentPhase;
    private static Phase[] phases;
    private static int currentPhaseIndex = 0;
    private static int currentStorySlide = 0;

    //TODO: add saving state in prefrences for each story

    /**
     * initializes the StoryState variables
     * @param con the application context so that the colors can be grabbed from the resources
     */
    public static void init(Context con) {
        context = con;
        currentPhase = new Phase(context.getResources().getString(R.string.learnTitle), R.color.learn_phase, LearnActivity.class, Phase.Type.LEARN);
        String[] phaseMenuArray;
        //Local

        SharedPreferences prefs = con.getSharedPreferences(con.getString(R.string.registration_filename), con.MODE_PRIVATE);
        SharedPreferences.Editor preferenceEditor = con.getSharedPreferences(con.getString(R.string.registration_filename), con.MODE_PRIVATE).edit();
        String remote = prefs.getString("consultant_location_type", null);
        boolean isRemote = false;
        if(remote.equals("Remote")) {
             isRemote = true;
        }

        if(!isRemote) {
            phaseMenuArray = con.getResources().getStringArray(R.array.local_phases_menu_array);
            phases = new Phase[]{new Phase(phaseMenuArray[0], R.color.learn_phase, LearnActivity.class, Phase.Type.LEARN),
                    new Phase(phaseMenuArray[1], R.color.draft_phase, PagerBaseActivity.class, Phase.Type.DRAFT),
                    new Phase(phaseMenuArray[2], R.color.comunity_check_phase, PagerBaseActivity.class, Phase.Type.COMMUNITY_CHECK),
                    new Phase(phaseMenuArray[3], R.color.consultant_check_phase, PagerBaseActivity.class, Phase.Type.CONSULTANT_CHECK),
                    new Phase(phaseMenuArray[4], R.color.dramatization_phase, PagerBaseActivity.class, Phase.Type.DRAMATIZATION),
                    new Phase(phaseMenuArray[5], R.color.create_phase, CreateActivity.class, Phase.Type.CREATE),
                    new Phase(phaseMenuArray[6], R.color.share_phase, ShareActivity.class, Phase.Type.SHARE)

            };
        }
        //Remote
        else{
            phaseMenuArray = con.getResources().getStringArray(R.array.remote_phases_menu_array);
            phases = new Phase[]{new Phase(phaseMenuArray[0], R.color.learn_phase, LearnActivity.class, Phase.Type.LEARN),
                    new Phase(phaseMenuArray[1], R.color.draft_phase, PagerBaseActivity.class, Phase.Type.DRAFT),
                    new Phase(phaseMenuArray[2], R.color.comunity_check_phase, PagerBaseActivity.class, Phase.Type.COMMUNITY_CHECK),
                    new Phase(phaseMenuArray[3], R.color.whole_story_phase, WholeStoryBackTranslationActivity.class, Phase.Type.WHOLE_STORY),
                    new Phase(phaseMenuArray[4], R.color.backT_phase, PagerBaseActivity.class, Phase.Type.BACKT),
                    new Phase(phaseMenuArray[5], R.color.remote_check_phase, PagerBaseActivity.class, Phase.Type.REMOTE_CHECK),
                    new Phase(phaseMenuArray[6], R.color.dramatization_phase, PagerBaseActivity.class, Phase.Type.DRAMATIZATION),
                    new Phase(phaseMenuArray[7], R.color.create_phase, CreateActivity.class, Phase.Type.CREATE),
                    new Phase(phaseMenuArray[8], R.color.share_phase, ShareActivity.class, Phase.Type.SHARE)
            };
        }
    }

    /**
     * Returns the story's name
     * @return String for storyName
     */
    public static String getStoryName() {
        return storyName;
    }


    /**
     * Sets the storyName
     * @param name String to set storyName with
     */
    public static void setStoryName(String name) {
        storyName = name;
    }

    /**
     * Return the current Phase the story is in
     * @return Phase
     */
    public static Phase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * sets the current phase as well as the phaseIndex
     * @param p Phase
     */
    public static void setCurrentPhase(Phase p) {
        for(int k = 0; k < phases.length; k++) {
            if(p.getTitle().equals(phases[k].getTitle())) {
                currentPhaseIndex = k;
            }
        }
        currentPhase = p;
        StorySharedPreferences.setPhaseForStory(p.getTitle(), storyName);   //set the stored preferences
    }

    public static Phase getSavedPhase() {
        String phaseTitle = StorySharedPreferences.getPhaseForStory(storyName);
        Phase phase = null;
        for(int k = 0; k < phases.length; k++) {
            if(phaseTitle.equals(phases[k].getTitle())) {
                phase = phases[k];
            }
        }
        currentPhase = phase;
        return phase;
    }

    /**
     * get the current stories Slide that the user is on
     * @return
     */
    public static int getCurrentStorySlide() {
        return currentStorySlide;
    }

    /**
     * set the current story slide
     */
    public static void setCurrentStorySlide(int num) {
        currentStorySlide = num;
    }

    /**
     * returns the index for the current story phase
     * @return int
     */
    public static int getCurrentPhaseIndex() {
        return currentPhaseIndex;
    }

    public static Phase[] getPhases() {
        return phases;
    }

    /**
     * gets the next phase and sets the current phase to that phase
     * @return Phase returns the next phase
     */
    public static Phase advanceNextPhase() {
        if(currentPhaseIndex < phases.length - 1) {
            currentPhaseIndex++;
        }
        currentPhase = phases[currentPhaseIndex];
        StorySharedPreferences.setPhaseForStory(currentPhase.getTitle(), storyName);
        return currentPhase;
    }

    /**
     * gets the previous Phase and sets the current phase to that phase
     * @return Phase returns the previous phase
     */
    public static Phase advancePrevPhase() {
        if(currentPhaseIndex > 0) {
            currentPhaseIndex--;
        }
        currentPhase = phases[currentPhaseIndex];
        StorySharedPreferences.setPhaseForStory(currentPhase.getTitle(), storyName);
        return currentPhase;
    }
}
