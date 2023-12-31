import dsl.client
import io.getenv
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AdsTest {

    @Test
    fun gettingAds() = runTest {
        val token = getenv("token")
        if (token.isInvalid()) return@runTest
        val ad = client { }.ads()

        client {
            this.token = token!!
        }.ads()
    }
}