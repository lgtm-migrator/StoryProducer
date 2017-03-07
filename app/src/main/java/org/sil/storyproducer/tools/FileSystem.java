package org.sil.storyproducer.tools;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.sil.storyproducer.model.SlideText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class FileSystem {
    private static String language = "ENG"; //ethnologue code for english
    private static final String LOGTAG = "filesystem";

    private static Context context;
    private static final String TEMPLATES_DIR = "templates",
                                NARRATION_PREFIX = "narration",
                                PROJECT_DIR = "projects",
                                SOUNDTRACK_PREFIX = "SoundTrack",
                                TRANSLATION_PREFIX = "translation",
                                LEARN_PRACTICE_PREFIX = "learnPractice",
                                COMMENT_PREFIX = "comment",
                                DRAMATIZATION_PREFIX = "dramatization",
                                MP3_EXTENSION = ".mp3";

    //Paths to template directories from language and story name
    private static Map<String, Map<String, String>> storyPaths;
    private static Map<String, String> projectPaths;

    public enum RENAME_CODES {
        SUCCESS,
        ERROR_LENGTH,
        ERROR_SPECIAL_CHARS,
        ERROR_CONTAINED_DESIGNATOR,
        ERROR_UNDEFINED
    }

    private static final FilenameFilter directoryFilter = new FilenameFilter() {
        @Override
        public boolean accept(File current, String name) {
            return new File(current, name).isDirectory();
        }
    };

    public static void init(Context con) {
        context = con;
        loadStories();
    }

    //Populate storyPaths from files in system
    public static void loadStories() {
        //Reset storyPaths
        storyPaths = new HashMap<>();
        projectPaths=new HashMap<>();

        File[] storeDirs = getStorageDirs();
        for (int storeIndex = 0; storeIndex < storeDirs.length; storeIndex++) {
            File sDir = storeDirs[storeIndex];

            if (sDir != null) {
                File templateDir = new File(sDir, TEMPLATES_DIR);

                //If there is no template directory, move on from this storage device.
                if(!templateDir.exists() || !templateDir.isDirectory()) {
                    continue;
                }

                File[] langDirs = getLanguageDirs(templateDir);
                for (int langIndex = 0; langIndex < langDirs.length; langIndex++) {
                    File lDir = langDirs[langIndex];
                    String lang = lDir.getName();

                    if (!storyPaths.containsKey(lang)) {
                        storyPaths.put(lang, new HashMap<String, String>());
                    }
                    Map<String, String> storyMap = storyPaths.get(lang);

                    File[] storyDirs = getStoryDirs(lDir);
                    for (int storyIndex = 0; storyIndex < storyDirs.length; storyIndex++) {
                        File storyDir = storyDirs[storyIndex];
                        String storyName = storyDir.getName();
                        String storyPath = storyDir.getPath();
                        storyMap.put(storyName, storyPath);

                        //Make sure the corresponding projects directory exists.
                        File storyWriteDir = new File(new File(sDir, PROJECT_DIR), storyName);
                        if(!storyWriteDir.isDirectory()) {
                            storyWriteDir.mkdir();
                        }
                    }
                }

                File projectDir  = new File(sDir, PROJECT_DIR);

                //Make the project directory if it does not exist.
                //The template creator shouldn't have to remember this step.
                if(!projectDir.isDirectory()) {
                    projectDir.mkdir();
                }

                File[] storyDirs = getStoryDirs(projectDir);
                for (int storyIndex = 0; storyIndex < storyDirs.length; storyIndex++) {
                    File storyDir = storyDirs[storyIndex];
                    String storyName = storyDir.getName();
                    String storyPath = storyDir.getPath();
                    projectPaths.put(storyName, storyPath);
                }
            }
        }
    }

    public static void changeLanguage(String lang) {
        language = lang;
    }

    private static File[] getStorageDirs() {
        return ContextCompat.getExternalFilesDirs(context, null);
    }

    private static File[] getLanguageDirs(File storageDir) {
        return storageDir.listFiles(directoryFilter);
    }
    private static File[] getStoryDirs(File dir) {
        return dir.listFiles(directoryFilter);
    }

    private static String getStoryPath(String story){
        Map<String, String> storyMap = storyPaths.get(language);
        if (storyMap != null) {
            return storyMap.get(story);
        }
        return null;
    }

    /**
     * gets the path to the project folder for the story that is passed
     * @param story
     * @return
     */
    private static String getProjectPath(String story) {
        return projectPaths.get(story);
    }

    public static File getNarrationAudio(String story, int i){
        return new File(getStoryPath(story)+"/"+NARRATION_PREFIX+i+".wav");
    }
    public static File getTranslationAudio(String story, int i){
        return new File(getStoryPath(story)+"/"+TRANSLATION_PREFIX+i+MP3_EXTENSION);
    }

    public static File getDramatizedAudio(String story, int i){
        return new File(getStoryPath(story)+"/"+DRAMATIZATION_PREFIX+i+MP3_EXTENSION);
    }

    /**
     * Gets the File for the learn practice recording
     * @param story
     * @return
     */
    public static File getLearnPracticeAudio(String story){
        return new File(getProjectPath(story) + "/" + LEARN_PRACTICE_PREFIX + ".mp3");
    }

    public static File getSoundtrackAudio(String story, int i){
        return new File(getStoryPath(story)+"/"+SOUNDTRACK_PREFIX+i+MP3_EXTENSION);
    }
    
    public static File getSoundtrack(String story){
        return new File(getStoryPath(story)+"/"+SOUNDTRACK_PREFIX+0+MP3_EXTENSION);
    }

    public static File getAudioComment(String story, int slide, String commentTitle) {
        return new File(getStoryPath(story)+"/"+COMMENT_PREFIX+slide+"_"+ commentTitle +MP3_EXTENSION);
    }

    /**
     * deletes the designated audio comment
     * @param story the story the comment comes from
     * @param slide the slide the comment comes from
     * @param commentTitle the name of the comment in question
     */
    public static void deleteAudioComment(String story, int slide, String commentTitle) {
        File file = getAudioComment(story, slide, commentTitle);
        if (file.exists()) {
            file.delete();
        }
    }

    public static boolean doesFileExist(String path) {
        File file = new File(path);
        return file.exists();
    }

    /**
     * renames the designated audio comment if the new name is valid and the file exists
     * @param story the story the comment comes from
     * @param slide the slide of the story the comment comes from
     * @param oldTitle the old title of the comment
     * @param newTitle the proposed new title for the comment
     * @return returns success or error code of renaming
     */
    public static RENAME_CODES renameAudioComment(String story, int slide, String oldTitle, String newTitle) {
        // Requirements for file names:
        //        - must be under 20 characters
        //        - must be only contain alphanumeric characters or spaces/underscores
        //        - must not contain the comment designator such as "comment0"
        if (newTitle.length() > 20) {
            return RENAME_CODES.ERROR_LENGTH;
        }
        if (!newTitle.matches("[A-Za-z0-9\\s_]+")) {
            return RENAME_CODES.ERROR_SPECIAL_CHARS;
        }
        if (newTitle.matches("comment[0-9]+")) {
            return RENAME_CODES.ERROR_CONTAINED_DESIGNATOR;
        }
        File file = getAudioComment(story, slide, oldTitle);
        boolean renamed = false;
        if (file.exists()) {
            String newPathName = file.getAbsolutePath().replace(oldTitle + MP3_EXTENSION, newTitle + MP3_EXTENSION);
            File newFile = new File(newPathName);
            if (!newFile.exists()) {
                renamed = file.renameTo(newFile);
            }
        }
        if (renamed) {
            return RENAME_CODES.SUCCESS;
        } else {
            return RENAME_CODES.ERROR_UNDEFINED;
        }
    }

    /**
     * Returns a list of comment titles for the story and slide in question
     * @param story the story where the comments come from
     * @param slide the slide where the comments come from
     * @return the array of comment titles
     */
    public static String[] getCommentTitles(String story, int slide) {
        ArrayList<String> commentTitles = new ArrayList<String>();
        File storyDirectory = new File(getStoryPath(story));
        File[] storyDirectoryFiles = storyDirectory.listFiles();
        String filename;
        for (int i = 0; i < storyDirectoryFiles.length; i++) {
            filename = storyDirectoryFiles[i].getName();
            if (filename.contains(COMMENT_PREFIX+slide)) {
                filename = filename.replace(COMMENT_PREFIX+slide+"_", "");
                filename = filename.replace(MP3_EXTENSION, "");
                commentTitles.add(filename);
            }
        }
        String[] returnTitlesArray = new String[commentTitles.size()];
        return commentTitles.toArray(returnTitlesArray);
    }

    /**
     * Gets the directory of a particular story in the <b>projects</b> directory.
     * @param story
     * @return
     */
    public static File getProjectDirectory(String story) {
        String path = projectPaths.get(story);
        return new File(path); //will throw a null pointer exception if path is null
    }

    public static String[] getStoryNames() {
        Map<String, String> storyMap = storyPaths.get(language);
        if (storyMap != null) {
            Set<String> keys = storyMap.keySet();
            return keys.toArray(new String[keys.size()]);
        }
        return new String[0];
    }

    public static File getImageFile(String story, int number) {
        return new File(getStoryPath(story), number + ".jpg");
    }

    public static Bitmap getImage(String story, int number) {
        return getImage(story, number, 1);
    }
    public static Bitmap getImage(String story, int number, int sampleSize) {
        String path = getStoryPath(story);
        File f = new File(path);
        File file[] = f.listFiles();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;

        for (int i = 0; i < file.length; i++) {
            if (file[i].getName().equals(number + ".jpg")) {
                return BitmapFactory.decodeFile(path + "/" + file[i].getName(), options);
            }
        }
        return null;
    }

    public static Bitmap getEndImage(String story) {
        return getEndImage(story, 1);
    }

    public static Bitmap getEndImage(String story, int sampleSize) {
        String path = getStoryPath(story);
        File imageFile = new File(path, "end.jpg");
        //If "end.jpg" doen't exist, use the last numbered picture.
        if(!imageFile.exists()) {
            imageFile = new File(path, (getImageAmount(story) - 1) + ".jpg");
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(imageFile.getPath(), options);
    }

//    public static String getAudioPath(String story, int number) {
//        String path = getStoryPath(story);
//        File f = new File(path);
//        File file[] = f.listFiles();
//        String audioName = "narration" + number;
//
//        for (int i = 0; i < file.length; i++) {
//            String[] audioExtensions = {".wav", ".mp3", ".wma"};
//            for (String extension : audioExtensions) {
//                if (file[i].getName().equals(audioName + extension)) {
//                    return file[i].getAbsolutePath();
//                }
//            }
//        }
//        return null;
//    }
    public static int getImageAmount(String storyName) {
        String path = getStoryPath(storyName);
        File f = new File(path);
        File file[] = f.listFiles();
        int count = 0;
        for (int i = 0; i < file.length; i++) {
            if (!file[i].isHidden() && file[i].getName().contains(".jpg")) {
                count++;
            }
        }
        return count;
    }

    public static SlideText getSlideText(String storyName, int slideNum) {
        String[] content;
        File file = new File(getStoryPath(storyName), (slideNum + ".txt"));
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }


        String text1 = text.toString();
        byte[] temp = text1.getBytes();
        for (int i = 0; i < temp.length - 2; i++) {
            //Swap out curly apostrophe with ASCII single quote
            if (temp[i] == -17 && temp[i + 1] == -65 && temp[i + 2] == -67) {
                text = text.replace(i, i + 1, "'");
                text1 = text.toString();
                temp = text1.getBytes();
            }
        }
        content = text.toString().split(Pattern.quote("~"));

        if (content.length == 4) {
            SlideText slideText = new SlideText(content[0], content[1], content[2], content[3]);
            return slideText;
        } else {
            Log.e(LOGTAG, "Text file not found for " + storyName + " slide " + slideNum);
            return new SlideText();
        }
    }

    public static String[] getLanguages() {
        return storyPaths.keySet().toArray(new String[storyPaths.size()]);
    }

    /**
     * This function searches the directory of the story and finds the total number of
     * slides associated with the story. The total number of slides will be determined by
     * the number of .jpg and .txt extensions. The smaller number of .jpg or .txt will be returned.
     *
     * @param storyName The story name that needs to find total number of slides.
     * @return The number of slides total for the story. The smaller number of .txt or .jpg files
     * found in the directory.
     */
    public static int getTotalSlideNum(String storyName) {
        String rootDirectory = getStoryPath(storyName);
        File[] files = new File(rootDirectory).listFiles();
        int totalPics = 0;
        int totalTexts = 0;

        for (File aFile : files) {
            String tempNumber;
            String fileName = aFile.toString();
            if (fileName.contains(".jpg") || fileName.contains(".txt")) {
                String extension = (fileName.contains(".jpg")) ? ".jpg" : ".txt";
                tempNumber = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf(extension));
                if (tempNumber.matches("^([0-9]+)$")) {
                    int checkingNumber = Integer.valueOf(tempNumber);
                    if (extension.equals(".txt")) {
                        totalTexts = (checkingNumber > totalTexts) ? checkingNumber : totalTexts;
                    } else {
                        totalPics = (checkingNumber > totalPics) ? checkingNumber : totalPics;
                    }
                }
            }
        }

        //highest numbered audio/image + 1 is amount
        return ((totalPics < totalTexts) ? totalPics : totalTexts) + 1;
    }
}
