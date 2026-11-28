package org.cangnova.cangjie.analysis.realproject

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Comparator

/**
 * 把真实项目检出到本地缓存目录，并固定到指定提交。
 *
 * 复用规则：
 * - `ref` 是 SHA 且当前 HEAD 已匹配：直接复用，不访问网络；
 * - `-PrealProjectRefresh=false`：复用现有检出，不 fetch（离线场景）；
 * - 否则 fetch 后按提交强制检出；按 SHA 直取失败时退回完整 fetch 再解析；
 * - 检出目录损坏或无法更新：删除后重新 clone。
 *
 * 同一缓存目录的 clone/fetch 用文件锁串行化，避免并发测试互相踩踏。
 */
object GitFixture {

    private val SHA_REF = Regex("^[0-9a-fA-F]{7,40}$")

    /** 确保 [config] 指向的项目已检出，返回检出目录。 */
    fun prepare(config: RealProjectConfig): Path {
        val dir = config.checkoutDir
        Files.createDirectories(config.workspaceRoot)
        FileChannel.open(
            config.workspaceRoot.resolve(".checkout.lock"),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
        ).use { channel ->
            channel.lock().use {
                return prepareLocked(config, dir)
            }
        }
    }

    private fun prepareLocked(config: RealProjectConfig, dir: Path): Path {
        if (Files.isDirectory(dir.resolve(".git"))) {
            if (updateExisting(config, dir)) return dir
            deleteRecursively(dir)
        }
        cloneFresh(config, dir)
        return dir
    }

    /** 返回 true 表示现有检出可用且已对齐目标提交。 */
    private fun updateExisting(config: RealProjectConfig, dir: Path): Boolean {
        val head = runCatching { capture(dir, "rev-parse", "HEAD") }.getOrNull() ?: return false

        // 不可变提交：HEAD 已匹配就不需要网络。
        if (SHA_REF.matches(config.ref) && head.startsWith(config.ref, ignoreCase = true)) {
            return true
        }

        if (!config.refresh) {
            // 离线复用：不 fetch，直接检出缓存里已有的提交。
            val commit = resolveCommit(dir, config.ref)
                ?: error("离线复用模式（-PrealProjectRefresh=false）下缓存中没有 `${config.ref}`: $dir")
            exec(dir, "checkout", "--force", commit)
            return true
        }

        return runCatching {
            fetchAndCheckout(dir, config.ref)
            true
        }.getOrElse { false }
    }

    private fun cloneFresh(config: RealProjectConfig, dir: Path) {
        Files.createDirectories(dir.parent)
        exec(dir.parent, "clone", "--no-checkout", config.repo, dir.toString())
        fetchAndCheckout(dir, config.ref)
    }

    /** 先按目标 ref 浅取；失败时退回完整 fetch，再按可用提交检出。 */
    private fun fetchAndCheckout(dir: Path, ref: String) {
        val directFetch = runCatching {
            exec(dir, "fetch", "--depth", "1", "origin", ref)
        }.isSuccess
        if (directFetch) {
            exec(dir, "checkout", "--force", "FETCH_HEAD")
            return
        }

        exec(dir, "fetch", "origin")
        val commit = resolveCommit(dir, ref) ?: error("git fetch 后仍找不到提交 `$ref`（目录: $dir）")
        exec(dir, "checkout", "--force", commit)
    }

    private fun resolveCommit(dir: Path, ref: String): String? =
        runCatching { capture(dir, "rev-parse", "--verify", "$ref^{commit}") }.getOrNull()

    private fun exec(workingDir: Path, vararg args: String) {
        run(listOf("git") + args, workingDir, failOnError = true)
    }

    private fun capture(workingDir: Path, vararg args: String): String {
        val output = run(listOf("git") + args, workingDir, failOnError = false)
            ?: error("git 命令失败: ${args.joinToString(" ")}")
        return output.trim()
    }

    private fun run(command: List<String>, workingDir: Path, failOnError: Boolean): String? {
        val process = ProcessBuilder(command)
            .directory(workingDir.toFile())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            if (failOnError) {
                error("git 命令失败 (exit=$exitCode): ${command.joinToString(" ")}\n${output.takeLast(4000)}")
            }
            return null
        }
        return output
    }

    private fun deleteRecursively(dir: Path) {
        if (!Files.exists(dir)) return
        Files.walk(dir).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
