package fr.matk.service.process

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

}
