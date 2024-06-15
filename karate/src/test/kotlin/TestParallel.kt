import com.intuit.karate.Runner
import net.masterthought.cucumber.Configuration
import net.masterthought.cucumber.ReportBuilder
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class TestParallel {

    @Test
    fun testParallel() {
        val results = Runner.path("classpath:payments", "classpath:profile") ///provide folder name.
            .tags("@e2e")
            .outputCucumberJson(true)
            .parallel(5)
        generateReport(results.reportDir)
        assertEquals(0, results.failCount, results.errorMessages)
    }

    private fun generateReport(karateOutputPath: String) {
       val jsonPaths = FileUtils.listFiles(File(karateOutputPath), arrayOf("json"), true).map { it.absolutePath }
        val config = Configuration(File("target"), "payment")
        val reportBuilder = ReportBuilder(jsonPaths, config)
        reportBuilder.generateReports()
    }
}
