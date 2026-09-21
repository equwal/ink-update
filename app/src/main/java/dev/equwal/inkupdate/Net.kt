// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 equwal
package dev.equwal.inkupdate

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * One HTTP GET, with java.net and nothing else.
 *
 * The app sends no identifier and no cookie. It sends the package name or the
 * repository name in the path, and a User-Agent that names the app. Call this
 * from a background thread only.
 */
object Net {

    const val AGENT = "InkUpdate/" + BuildConfig.VERSION_NAME +
        " (+https://github.com/equwal/ink-update)"

    private const val CONNECT_MS = 10_000
    private const val READ_MS = 15_000

    /** The most bytes to read from one answer. An answer is a few kilobytes. */
    private const val MAX_BYTES = 512 * 1024

    /**
     * @param code the HTTP status, or 0 when the request did not finish.
     * @param body the text of the answer, or null.
     */
    data class Answer(val code: Int, val body: String?)

    fun get(url: String, accept: String? = null): Answer {
        var c: HttpURLConnection? = null
        return try {
            c = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_MS
                readTimeout = READ_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", AGENT)
                if (accept != null) setRequestProperty("Accept", accept)
            }
            val code = c.responseCode
            // F-Droid answers 404 with a body that says NOT_FOUND, so the error
            // stream matters as much as the input stream.
            val stream: InputStream? = if (code in 200..299) c.inputStream else c.errorStream
            Answer(code, stream?.let { read(it) })
        } catch (e: Exception) {
            Answer(0, null)
        } finally {
            c?.disconnect()
        }
    }

    // The bytes go into one buffer and are made into text once. A character of
    // more than one byte can lie across two reads.
    private fun read(stream: InputStream): String? = try {
        stream.use { s ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8 * 1024)
            while (out.size() < MAX_BYTES) {
                val n = s.read(buf)
                if (n <= 0) break
                out.write(buf, 0, n)
            }
            String(out.toByteArray(), Charsets.UTF_8)
        }
    } catch (e: Exception) {
        null
    }
}
