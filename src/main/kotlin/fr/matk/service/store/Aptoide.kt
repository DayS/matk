package fr.matk.service.store

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import fr.matk.utils.LoggerDelegate
import fr.matk.utils.Rest
import io.reactivex.Observable
import io.reactivex.Single
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File
import java.io.IOException
import java.net.URL

class Aptoide : KoinComponent {

    private val logger by LoggerDelegate()
    private val rest by inject<Rest>()
    private val moshi by inject<Moshi>()

    fun searchPackages(pattern: String, exactMatch: Boolean = false, countryCode: String = "en"): Observable<SearchResultItem> {
        return rest.restGet(URL("https://web-api-cache.aptoide.com/search?query=$pattern&country=$countryCode&mature=false"))
            .doOnSubscribe { logger.debug("Searching app from store '{}' with query '{}'", countryCode, pattern) }
            .flatMapObservable { json ->
                moshi.adapter(SearchResult::class.java).fromJson(json)?.let { Observable.fromIterable(it.datalist.list) } ?: Observable.error(IOException("Response shouldn't be null"))
            }
            .filter { !exactMatch || it.packageId == pattern }
    }

    fun downloadPackage(url: String, destination: File): Single<File> {
        return rest.download(URL(url), destination)
    }

    @JsonClass(generateAdapter = true)
    data class SearchResult(val datalist: SearchResultDatalist)

    @JsonClass(generateAdapter = true)
    data class SearchResultDatalist(val list: List<SearchResultItem>)

    @JsonClass(generateAdapter = true)
    data class SearchResultItem(val id: Long, @Json(name = "package") val packageId: String, val name: String, val file: SearchResultItemFile)

    @JsonClass(generateAdapter = true)
    data class SearchResultItemFile(val path: String)

}
