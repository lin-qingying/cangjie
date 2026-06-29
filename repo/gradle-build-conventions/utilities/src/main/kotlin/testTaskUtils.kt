import com.sun.management.OperatingSystemMXBean
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.lang.management.ManagementFactory

/**
 * 为操作系统和 IDE 进程预留的内存容量，单位 MB。
 */
private const val RESERVED_MEMORY_MB = 9000

/**
 * 当前机器可分配给测试 JVM 的最大总内存估算值，单位 MB。
 *
 * 结果按 1024MB 对齐，便于调用方按 fork 数或测试任务数拆分 JVM 内存。
 */
val totalMaxMemoryForTestsMb: Int
    get() {
        val mxbean = ManagementFactory.getOperatingSystemMXBean() as OperatingSystemMXBean
        val availableMemoryMb = (mxbean.totalPhysicalMemorySize / 1048576 - RESERVED_MEMORY_MB).toInt()
        return availableMemoryMb - (availableMemoryMb % 1024)
    }

/**
 * 返回测试运行时使用的临时 IDEA home 目录。
 */
fun Project.ideaHomePathForTests(): Provider<java.io.File> {
    return layout.buildDirectory.dir("test-idea-home").map { it.asFile }
}
