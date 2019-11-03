package org.sil.storyproducer.tools.file


import android.content.Context
import com.crashlytics.android.Crashlytics
import org.sil.storyproducer.model.*
import org.sil.storyproducer.model.PROJECT_DIR
import java.util.*
import kotlin.math.max

/**
 * AudioFiles represents an abstraction of the audio resources for story templates and project files.
 */

internal const val AUDIO_EXT = ".m4a"

/**
 * Creates a relative path for recorded audio based upon the phase, slide number and timestamp.
 * Records the path in the story object.
 * If there is a failure in generating the path, an empty string is returned.
 * @return the path generated, or an empty string if there is a failure.
 */

fun getChosenFilename(slideNum: Int = Workspace.activeSlideNum): String? {
    return getChosenRecording(slideNum)?.fileName
}

fun getChosenDisplayName(slideNum: Int = Workspace.activeSlideNum): String? {
    return getChosenRecording(slideNum)?.displayName
}

fun getChosenRecording(slideNum: Int = Workspace.activeSlideNum): Recording? {
    return when (Workspace.activePhase.phaseType) {
        PhaseType.LEARN -> Workspace.activeStory.learnAudioFile
        PhaseType.DRAFT -> Workspace.activeStory.slides[slideNum].draftRecordings.selectedFile
        PhaseType.DRAMATIZATION -> Workspace.activeStory.slides[slideNum].dramatizationRecordings.selectedFile
        PhaseType.BACKT -> Workspace.activeStory.slides[slideNum].backTranslationRecordings.selectedFile
        PhaseType.COMMUNITY_CHECK -> Workspace.activeStory.slides[slideNum].backTranslationRecordings.selectedFile
        PhaseType.WHOLE_STORY -> Workspace.activeStory.wholeStoryBackTAudioFile
        else -> throw Exception("Unsupported stage to get the audio file for")
    }
}

fun getRecordedDisplayNames(slideNum: Int = Workspace.activeSlideNum): List<String> {
    return Workspace.activePhase.getCombNames(slideNum).map { it.displayName }
}

fun getRecordedAudioFiles(slideNum: Int = Workspace.activeSlideNum): List<String> {
    return Workspace.activePhase.getCombNames(slideNum).map { it.fileName }
}

fun assignNewAudioRelPath(): String {
    val recording = createRecordingCombinedName()
    addCombinedName(recording)
    return recording.fileName
}

fun updateDisplayName(position: Int, newName: String) {
    //make sure to update the actual list, not a copy.
    val recordings = Workspace.activePhase.getRecordings().getFiles()
    recordings[position].displayName = newName
}

fun deleteAudioFileFromList(context: Context, pos: Int) {
    //make sure to update the actual list, not a copy.
    val filenames = Workspace.activePhase.getRecordings()
    val filename = filenames.getFiles()[pos].fileName
    filenames.removeAt(pos)
    deleteStoryFile(context, filename)
}

fun createRecordingCombinedName(): Recording {
    //Example: project/communityCheck_3_2018-03-17T11:14;31.542.md4
    //This is the file name generator for all audio files for the app.

    //the extension is added in the "when" statement because wav files are easier to concatenate, so
    //they are used for the stages that do that.
    return when (Workspace.activePhase.phaseType) {
        //just one file.  Overwrite when you re-record.
        PhaseType.LEARN, PhaseType.WHOLE_STORY -> Recording(
                "$PROJECT_DIR/${Workspace.activePhase.getShortName()}$AUDIO_EXT",
                Workspace.activePhase.getDisplayName())
        //Make new files every time.  Don't append.
        PhaseType.DRAFT, PhaseType.COMMUNITY_CHECK,
        PhaseType.DRAMATIZATION, PhaseType.CONSULTANT_CHECK -> {
            //find the next number that is available for saving files at.
            val names = getRecordedDisplayNames()
            val rNameNum = "${Workspace.activePhase.getDisplayName()} ([0-9]+)".toRegex()
            var maxNum = 0
            for (n in names) {
                try {
                    val num = rNameNum.find(n)
                    if (num != null)
                        maxNum = max(maxNum, num.groupValues[1].toInt())
                } catch (e: Exception) {
                    //If there is a crash (such as a bad int parse) just keep going.
                    Crashlytics.logException(e)
                }
            }
            val displayName = "${Workspace.activePhase.getDisplayName()} ${maxNum + 1}"
            val fileName = "${Workspace.activeDir}/${Workspace.activeFilenameRoot}_${Date().time}$AUDIO_EXT"
            Recording(fileName, displayName)
        }
        else -> throw Exception("Unsupported phase to create recordings for")
    }
}

fun addCombinedName(recording: Recording) {
    //register it in the story data structure.
    when (Workspace.activePhase.phaseType) {
        PhaseType.LEARN -> Workspace.activeStory.learnAudioFile = recording
        PhaseType.WHOLE_STORY -> Workspace.activeStory.wholeStoryBackTAudioFile = recording
        PhaseType.COMMUNITY_CHECK -> Workspace.activeSlide!!.communityCheckRecordings.add(recording)
        PhaseType.CONSULTANT_CHECK -> Workspace.activeSlide!!.consultantCheckRecordings.add(recording)
        PhaseType.DRAFT -> Workspace.activeSlide!!.draftRecordings.add(recording)
        PhaseType.DRAMATIZATION -> Workspace.activeSlide!!.dramatizationRecordings.add(recording)
        else -> throw Exception("Unsupported phase to add an audio file to")
    }
}

fun getTempAppendAudioRelPath(): String {
    return "$PROJECT_DIR/temp$AUDIO_EXT"
}

