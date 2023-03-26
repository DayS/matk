package fr.matk.service.android

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import fr.matk.service.Cache
import fr.matk.service.process.Processes
import fr.matk.utils.LoggerDelegate
import fr.matk.utils.Zip
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.http.promisesBody
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File
import java.io.IOException
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Callable

class PlayStore : KoinComponent {
    private val logger by LoggerDelegate()
    private val cache by inject<Cache>()
    private val client by inject<OkHttpClient>()
    private val moshi by inject<Moshi>()
    private val ogAppResultJsonAdapter by lazy { moshi.adapter(OGAppResult::class.java) }
    private val patternTarFile by lazy { Regex(".+\\.tar$", RegexOption.IGNORE_CASE) }

    fun downloadAndExtract(cpuAbi: String, androidVersion: String): Single<File> {
        logger.info("Retrieving Play store for Android {} with CPU {}", androidVersion, cpuAbi)

        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val relativeBasename = "playstore/playstore-${cpuAbi}-${androidVersion}-${date}"
        val fullBasename = cache.cachedFile(relativeBasename)

        return cache.getOrFetch("${relativeBasename}.zip", Single.defer { retrieveLatestZipUrl(cpuAbi, androidVersion) }
            .flatMap { cache.download(it, cache.cachedFile("${relativeBasename}.zip")) }
        )
            // Extract files from Core folder, except he ones prefixed with "setup" to prevent launching setup wizard
            .flatMapObservable {
                Zip.extractFiles(it, fullBasename.absoluteFile) { zipEntry ->
                    zipEntry.name.startsWith("Core/") && !zipEntry.name.startsWith("Core/setup")
                }
            }
            // Inflate Lzip files
            .filter { it.name.endsWith(".lz") }
            .flatMapCompletable { Processes.execute("lzip", "-f", "-d", it.absolutePath).ignoreElements() }
            .andThen(Observable.fromIterable(File(fullBasename, "Core").listFiles()?.toList() ?: emptyList()))
            // Retrieve and extract each Tar files in Core folder
            .filter { it.name.matches(patternTarFile) }
            .concatMapCompletable { Processes.execute("tar", "-x", "--strip-components", "2", "-f", fullBasename.absolutePath + "/Core/" + it.name, "-C", fullBasename.absolutePath).ignoreElements() }
            .toSingleDefault(fullBasename)
    }

    private fun retrieveLatestZipUrl(cpuAbi: String, androidVersion: String): Single<URL> {
        val resourceFactory = Callable {
            logger.debug("Resolving last Play store ZIP URL for Android {} on {}", androidVersion, cpuAbi)

            val request = Request.Builder()
                .get()
                .url("https://api.opengapps.org/list")
                .addHeader("User-Agent", "Matk")
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request)
        }

        val factory = Function<Call, Single<URL>> { call ->
            Single.create { emitter ->
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        emitter.onError(IOException("Unexpected code $response"))
                    } else if (!response.promisesBody()) {
                        emitter.onError(IOException("No content could be retrieved"))
                    } else {


                        response.body!!.source().use { source ->
                            ogAppResultJsonAdapter.fromJson(source)?.let {
                                val dataCpu = it.archs[cpuAbi] ?: return@let emitter.onError(IOException("CPU $cpuAbi is not supported by OpenGapps"))
                                val dataApi = dataCpu.apis[androidVersion] ?: return@let emitter.onError(IOException("Android version $androidVersion with CPU $cpuAbi is not supported by OpenGapp"))
                                retrieveVariantZipUrl(dataApi.variants)?.let {
                                    emitter.onSuccess(it)
                                } ?: {
                                    emitter.onError(IOException("No variant found"))
                                }
                            } ?: {
                                emitter.onError(IOException("Response shouldn't be null"))
                            }
                        }
                    }
                }
            }
        }

        val disposeAction = Consumer<Call> {
            it.cancel()
        }

        return Single.using(resourceFactory, factory, disposeAction)
    }

    private fun retrieveVariantZipUrl(variants: List<OGApVariant>): URL? = variants.firstOrNull { it.name == "pico" }?.zip?.let { URL(it) }
}

@JsonClass(generateAdapter = true)
data class OGAppResult(val archs: Map<String, OGAppArch>)

@JsonClass(generateAdapter = true)
data class OGAppArch(val apis: Map<String, OGApApi>)

@JsonClass(generateAdapter = true)
data class OGApApi(val variants: List<OGApVariant>)

@JsonClass(generateAdapter = true)
data class OGApVariant(val name: String, val zip: String)
