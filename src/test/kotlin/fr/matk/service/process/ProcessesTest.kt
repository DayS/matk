package fr.matk.service.process

import fr.matk.getResourceAsFile
import fr.matk.getResourceAsText
import io.reactivex.Single
import org.junit.jupiter.api.Test

internal class ProcessesTest {

    @Test
    fun executeUnknownCommand() {
        Processes.execute("unknowncommand").test()
            .assertNoValues()
            .assertError(StartProcessException::class.java)
            .await()
    }

    @Test
    fun executeErrorOutput() {
        Processes.execute("ls", "<malformed>").test()
            .assertError(ExecProcessException::class.java)
            .await()
    }

    @Test
    fun execute() {
        Processes.execute("date", "-r", "60", "+%Y%m%d").test()
            .assertNoErrors()
            .assertValueCount(1)
            .assertValue("19700101")
            .await()
    }

    @Test
    fun execute_listOutput() {
        val expectedContent = getResourceAsText("/lorem_ipsum.txt")?.split("\n") ?: throw IllegalStateException("Test resource file not found")
        val file = getResourceAsFile("/lorem_ipsum.txt")

        Processes.execute("cat", file!!.absolutePath)
            .concatWith(Single.just(""))         // Append new line as "cat" seems to trim the end
            .test()
            .assertNoErrors()
            .assertValues(*expectedContent.toTypedArray())
            .await()
    }

}
