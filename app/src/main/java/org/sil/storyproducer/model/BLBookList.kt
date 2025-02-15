package org.sil.storyproducer.model

import android.os.AsyncTask
import android.util.Xml
import org.sil.storyproducer.App
import org.sil.storyproducer.R
import org.sil.storyproducer.controller.bldownload.BLDownloadActivity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import android.util.Base64
import java.text.SimpleDateFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class BLBook(
    val Title: String,
    val LangCode: String,
    val ThumbnailURL: String,
    val BloomSourceURL: String)

/**
 * BLBookList is used as a collection of books available to download from BloomLibrary
 */
open class BLBookList(var dateUpdated: Date) {

    companion object {

        var booklistLoading = false
        var booklist : List<BLBook>? = null
        private const val fallbackResourceBucket = "sil-storyproducer-resources"
        private const val fallbackResourceDomain = "s3.amazonaws.com"
        private const val fallbackResourceKey = "bl1/bl_samples.xml"

        const val WIFI = "Wi-Fi"
        const val ANY = "Any"

        // Whether there is a Wi-Fi connection.
        private var wifiConnected = true//false
        // Whether there is a mobile connection.
        private var mobileConnected = true//false

        // The user's current network preference setting.
        var sPref: String? = ANY

        private const val fallbackResourceAddr = "https://${fallbackResourceBucket}.${fallbackResourceDomain}/${fallbackResourceKey}"
    }

    // We don't use namespaces
    private val ns: String? = null


    private val accessKey = "AWS_ACCESS_KEY_ID"
    private val secretKey = "AWS_SECRET_ACCESS_KEY"
    private val resourceKey = "AWS_RESOURCE_KEY"
    private val resourceBucket = "sil-storyproducer-resources"
    private val resourceDomain = "s3.amazonaws.com"
    private lateinit var requestDate: String
    private lateinit var authValue : String

    //
    // xml parsing code methods below are adapted from: https://developer.android.com/training/basics/network-ops/xml
    //

    // parse xml from inputStream
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream) : List<BLBook> {
        inputStream.use { inStream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inStream, null)
            parser.nextTag()
            return readFeed(parser)
        }
    }

    // parse individual entries when start tag found
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser): List<BLBook> {
        val entries = mutableListOf<BLBook>()

        parser.require(XmlPullParser.START_TAG, ns, "feed")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the entry tag
            if (parser.name == "entry") {
                entries.add(readEntry(parser))
            } else {
                skip(parser)
            }
        }
        return entries
    }

    // Parses the contents of an entry. If it encounters a title, language, or one of two link tags, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readEntry(parser: XmlPullParser): BLBook {
        parser.require(XmlPullParser.START_TAG, ns, "entry")
        var title: String? = null
        var langCode: String? = null
        var thumbLink: String? = null
        var bookLink: String? = null
        var numLink = 0;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "title" -> title = readTitle(parser)
                "dcterms:language" -> langCode = readLanguage(parser)
                "link" -> { numLink++
                            if (numLink == 1)
                                // this relies on the thumbnail link being the first of the two
                                thumbLink = readThumbnailLink(parser)
                            else
                                // this relies on the bloom book link being the second of the two
                                bookLink = readBLBookLink(parser)
                }
                else -> skip(parser)
            }
        }
        return BLBook(title!!, langCode!!, thumbLink!!, bookLink!!)
    }

    // Processes title tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTitle(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "title")
        val title = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "title")
        return title
    }

    // Processes thumbnail link tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readThumbnailLink(parser: XmlPullParser): String {
        var link = ""
        parser.require(XmlPullParser.START_TAG, ns, "link")
        val tag = parser.name
        val relType = parser.getAttributeValue(null, "rel")
        if (tag == "link") {
            if (relType == "http://opds-spec.org/image") {
                link = parser.getAttributeValue(null, "href")
                parser.nextTag()
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, "link")
        return link
    }

    // Processes book link tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readBLBookLink(parser: XmlPullParser): String {
        var link = ""
        parser.require(XmlPullParser.START_TAG, ns, "link")
        val tag = parser.name
        val relType = parser.getAttributeValue(null, "rel")
        if (tag == "link") {
            if (relType == "http://opds-spec.org/acquisition/open-access") {
                link = parser.getAttributeValue(null, "href")
                parser.nextTag()
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, "link")
        return link
    }

    // Processes language code tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLanguage(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "dcterms:language")
        val summary = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "dcterms:language")
        return summary
    }

    // For the tags title and language code, extracts their text values.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    // skips tags until the end of the matching end tag
    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    // Downloads a bloom catalog and parses the xml using the downloaded InputStream
    @Throws(XmlPullParserException::class, IOException::class)
    private fun loadXmlFromNetwork(urlString: String) {

        if (accessKey.compareTo("AWS_ACCESS_KEY_ID") == 0) {
            booklist = downloadUrl(urlString)?.use { stream ->
                // Instantiate the fallback samples parser
                parse(stream)
            } ?: emptyList()
        } else {
            booklist = doAuthInBackground()?.use { stream ->
                // Instantiate the full catalog parser
                parse(stream)
            } ?: emptyList()
        }

    }

    // Given a string representation of a URL, sets up a connection and gets
    // an input stream.
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream? {
        val url = URL(urlString)
        return (url.openConnection() as? HttpURLConnection)?.run {
            readTimeout = 10000
            connectTimeout = 15000
            requestMethod = "GET"
            doInput = true
            // Starts the query
            connect()
            inputStream
        }
    }

    @Throws(IOException::class)
    private fun doAuthInBackground(): InputStream? {
        createRequestDate()
        createAuthValue()
        return getCatalogStream()
    }

    private fun createRequestDate() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        requestDate = dateFormat.format(calendar.time)
    }

    private fun createAuthValue()  {
        val message = "GET\n\n\n${requestDate}\n/${resourceBucket}/${resourceKey}"
        val mac = Mac.getInstance("HmacSHA1")
        val secret = SecretKeySpec(secretKey.toByteArray(), "HmacSHA1")
        mac.init(secret)
        val encoded = Base64.encodeToString(mac.doFinal(message.toByteArray()), Base64.DEFAULT).trimEnd()
        authValue = "AWS ${accessKey}:${encoded}"
    }

    private fun getCatalogStream(): InputStream? {

        val url = URL("https://${resourceBucket}.${resourceDomain}/${resourceKey}")
        with(url.openConnection() as HttpURLConnection) {
            requestMethod = "GET"
            addRequestProperty("Date", requestDate)
            addRequestProperty("Authorization", authValue)
            return inputStream
        }
    }

// Implementation of AsyncTask used to download XML feed from the bloom catalog url.
    private inner class DownloadXmlTask : AsyncTask<String, Void, String>() {

        val resources = App.appContext.resources

        override fun doInBackground(vararg urls: String) : String {
            return try {
                loadXmlFromNetwork(urls[0])
                return ""
            } catch (e: IOException) {
                resources.getString(R.string.bloom_connection_error)
            } catch (e: XmlPullParserException) {
                resources.getString(R.string.bloom_xml_error)
            }
        }

        // called when the background network xml load has completed with any error code
        override fun onPostExecute(result: String) {

            booklistLoading = false

            if (result.isEmpty()) {
                BLDownloadActivity.bldlActivity.onDownloadXmlBloomCatalogSuccess()
            }
            else {
                BLDownloadActivity.bldlActivity.onDownloadXmlBloomCatalogFailure(result)
            }
        }
    }

    // Uses AsyncTask subclass to download the XML feed from bloom catalog url.
    fun loadPage() {

        if (sPref.equals(ANY) && (wifiConnected || mobileConnected)) {
            DownloadXmlTask().execute(fallbackResourceAddr)
        } else if (sPref.equals(WIFI) && wifiConnected) {
            DownloadXmlTask().execute(fallbackResourceAddr)
        } else {
            // show error
        }
    }
}

    // returns any existing book list - otherwise launches the network xml loading code
fun parseOPDSfile(): MutableList<BLBook>? {
    if (BLBookList.booklist?.isNotEmpty() == true) {
        return (BLBookList.booklist as MutableList<BLBook>?)!!;
    }
    if (!BLBookList.booklistLoading) {
        BLBookList.booklistLoading = true
        BLBookList(Date()).loadPage()
    }
    return null
}
