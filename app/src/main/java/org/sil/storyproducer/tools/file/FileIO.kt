@file:JvmName("FileIO")
package org.sil.storyproducer.tools.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract

import java.io.File
import android.support.v4.provider.DocumentFile
import org.sil.storyproducer.model.Workspace
import org.sil.storyproducer.model.*
import java.io.FileDescriptor
import java.io.InputStream
import java.io.OutputStream


fun getStoryImage(context: Context, slideNum: Int = Workspace.activeSlideNum, sampleSize: Int = 1, story: Story = Workspace.activeStory): Bitmap? {
    if(story.title == "") return null
    val imName = story.slides[slideNum].imageFile
    val iStream = getStoryChildInputStream(context,imName,story.title)
    if(iStream != null){
        val options = BitmapFactory.Options()
        options.inSampleSize = sampleSize
        return BitmapFactory.decodeStream(iStream, null, options)
    }
    return null
}

fun getStoryChildOutputStream(context: Context, relPath: String, mimeType: String = "", storyTitle: String = Workspace.activeStory.title) : OutputStream? {
    if (storyTitle == "") return null
    return getChildOutputStream(context, "$storyTitle/$relPath", mimeType)
}

fun storyRelPathExists(context: Context, relPath: String, storyTitle: String = Workspace.activeStory.title) : Boolean{
    if(getChildInputStream(context, "$storyTitle/$relPath" ) == null)
        return false
    return true
}

fun getStoryUri(relPath: String, storyTitle: String = Workspace.activeStory.title) : Uri? {
    if (storyTitle == "") return null
    return Uri.parse(Workspace.workspace.uri.toString() +
            Uri.encode("/$storyTitle/$relPath"))
}

fun getStoryFileDescriptor(context: Context, relPath: String, storyTitle: String = Workspace.activeStory.title) : FileDescriptor? {
    if (storyTitle == "") return null
    val pfd = getChildOuputPFD(context, "$storyTitle/$relPath") ?: return null
    return pfd.fileDescriptor
}

fun getStoryText(context: Context, relPath: String, storyTitle: String = Workspace.activeStory.title) : String? {
    val iStream = getStoryChildInputStream(context, relPath, storyTitle)
    if (iStream != null)
        return iStream.reader().use {
            it.readText() }
    return null
}

fun getStoryChildInputStream(context: Context, relPath: String, storyTitle: String = Workspace.activeStory.title) : InputStream? {
    if (storyTitle == "") return null
    return getChildInputStream(context, "$storyTitle/$relPath")
}

fun getText(context: Context, relPath: String) : String? {
    val iStream = getChildInputStream(context, relPath)
    if (iStream != null)
        return iStream.reader().use {
            it.readText() }
    return null
}

fun getChildOutputStream(context: Context, relPath: String, mimeType: String = "") : OutputStream? {
    val pfd = getChildOuputPFD(context, relPath, mimeType)
    return ParcelFileDescriptor.AutoCloseOutputStream(pfd)
}

fun getChildOuputPFD(context: Context, relPath: String, mimeType: String = "") : ParcelFileDescriptor? {
    if (!Workspace.workspace.isDirectory) return null
    //build the document tree if it is needed
    val segments = relPath.split("/")
    var df = Workspace.workspace
    var df_new : DocumentFile?
    for (i in 0 .. segments.size-2){
        df_new = df.findFile(segments[i])
        when(df_new == null){
            true ->  df = df.createDirectory(segments[i])
            false -> df = df_new
        }
    }
    //create the file if it is needed
    df_new = df.findFile(segments.last())
    when(df_new == null){
        true -> {
            //find the mime type by extension
            var mType = mimeType
            if(mType == "") {
                when (File(df.uri.path).extension) {
                    "json" -> mType = "application/json"
                    "mp3"  -> mType = "audio/x-mp3"
                    "wav" -> mType = "audio/w-wav"
                    "txt" -> mType = "plain/text"
                    else -> mType = "*/*"
                }
            }
            df = df.createFile(mType,segments.last())
        }
        false -> df = df_new
    }
    return context.contentResolver.openFileDescriptor(df.uri,"w")
}

fun getChildInputStream(context: Context, relPath: String) : InputStream? {
    val childUri = Uri.parse(Workspace.workspace.uri.toString() +
            Uri.encode("/$relPath"))
    //check if the file exists by checking for permissions
    try {
        //TODO Why is DocumentsContract.isDocument not working right?
        val pfd: ParcelFileDescriptor? = context.contentResolver.openFileDescriptor(
                childUri, "r") ?: return null
        return ParcelFileDescriptor.AutoCloseInputStream(pfd)
    } catch (e: Exception) {
        //The file does not exist.
        return null
    }
}

fun deleteStoryFile(context: Context, relPath: String, storyTitle: String = Workspace.activeStory.title) : Boolean {
    if(storyRelPathExists(context, relPath, storyTitle)){
        val uri = getStoryUri(relPath,storyTitle)
        return DocumentsContract.deleteDocument(context.contentResolver,uri)
    }
    return false
}

fun renameStoryFile(context: Context, relPath: String, newFilename: String, storyTitle: String = Workspace.activeStory.title) : Boolean {
    if(storyRelPathExists(context, relPath, storyTitle)){
        val uri = getStoryUri(relPath,storyTitle)
        val newUri = DocumentsContract.renameDocument(context.contentResolver,uri,newFilename)
        if(newUri != null) return true
    }
    return false
}


