import dsl.client
import io.getenv
import kotlinx.coroutines.test.runTest
import model.landing.ChartOption
import kotlin.test.Test

class ChartTest {
    @Test
    fun parsingDataIsValid() = runTest {
        val token = getenv("token")
        if (token.isInvalid()) return@runTest


        val chart = client { }.chart(ChartOption.World)
        println(chart)
    }
}