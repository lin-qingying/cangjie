import com.sun.management.OperatingSystemMXBean
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.lang.management.ManagementFactory

private const val RESERVED_MEMORY_MB = 9000

val totalMaxMemoryForTestsMb: Int
    get() {
        val mxbean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val availableMemoryMb = (mxbean.totalPhysicalMemorySize / 1048576 - RESERVED_MEMORY_MB).toInt()
        return availableMemoryMb - (availableMemoryMb % 1024)
    }

fun Project.ideaHomePathForTests(): Provider<java.io.File> {
    return layout.buildDirectory.dir("test-idea-home").map { it.asFile }
}
