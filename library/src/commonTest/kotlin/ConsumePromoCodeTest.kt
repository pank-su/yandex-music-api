import dsl.client
import io.getenv
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ConsumePromoCodeTest {
    @Test
    fun checkConsume() = runTest {
        val token = getenv("token")
        if (token.isInvalid()) return@runTest
        val client = client {
            this.token = token!!
        }

        println(client.consumePromoCode("test"))
    }
}