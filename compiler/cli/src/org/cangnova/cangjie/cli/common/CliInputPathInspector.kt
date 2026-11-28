/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cangnova.cangjie.cli.common

import com.intellij.openapi.util.SystemInfoRt
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

/**
 * CLI 路径检查的结构化结果，供不同输入协议按自身语义映射诊断。
 */
internal sealed interface CliInputPathInspection {
    data class Existing(
        val path: Path,
        val isDirectory: Boolean,
        val isReadable: Boolean,
    ) : CliInputPathInspection

    data class InvalidPath(val input: String) : CliInputPathInspection
    data class PathTooLong(val input: String, val maximumLength: Int) : CliInputPathInspection
    data class Missing(val input: String) : CliInputPathInspection
    data class Inaccessible(val input: String) : CliInputPathInspection
}

/**
 * 统一执行编译器输入路径的长度、存在性、可读性和绝对路径检查。
 *
 * 仓颉官方 driver 在 Windows 使用 MAX_PATH，在其他平台使用 PATH_MAX；
 * legacy CHIR 输入还需要与 driver 一致地保存绝对路径。
 */
internal object CliInputPathInspector {
    private const val WINDOWS_PATH_MAX = 260
    private const val POSIX_PATH_MAX = 4096

    private val pathMax: Int
        get() = if (SystemInfoRt.isWindows) WINDOWS_PATH_MAX else POSIX_PATH_MAX

    fun inspect(
        input: String,
        resolveRealPath: Boolean = false,
        checkResolvedPathLength: Boolean = true,
    ): CliInputPathInspection {
        val maximumLength = pathMax
        if (input.length > maximumLength) {
            return CliInputPathInspection.PathTooLong(input, maximumLength)
        }

        val suppliedPath = try {
            Path.of(input)
        } catch (_: InvalidPathException) {
            return CliInputPathInspection.InvalidPath(input)
        }

        val absolutePath = try {
            suppliedPath.toAbsolutePath().normalize()
        } catch (_: IOException) {
            return CliInputPathInspection.InvalidPath(input)
        } catch (_: InvalidPathException) {
            return CliInputPathInspection.InvalidPath(input)
        } catch (_: SecurityException) {
            return CliInputPathInspection.Inaccessible(input)
        }
        if (checkResolvedPathLength && absolutePath.toString().length > maximumLength) {
            return CliInputPathInspection.PathTooLong(input, maximumLength)
        }

        val exists = try {
            Files.exists(suppliedPath)
        } catch (_: SecurityException) {
            return CliInputPathInspection.Inaccessible(input)
        }
        if (!exists) {
            val notExists = try {
                Files.notExists(suppliedPath)
            } catch (_: SecurityException) {
                false
            }
            return if (notExists) CliInputPathInspection.Missing(input) else CliInputPathInspection.Inaccessible(input)
        }

        val isDirectory = try {
            Files.isDirectory(suppliedPath)
        } catch (_: SecurityException) {
            return CliInputPathInspection.Inaccessible(input)
        }
        val isReadable = try {
            Files.isReadable(suppliedPath)
        } catch (_: SecurityException) {
            false
        }
        val resolvedPath = if (resolveRealPath && !SystemInfoRt.isWindows) {
            try {
                suppliedPath.toRealPath()
            } catch (_: IOException) {
                return CliInputPathInspection.Inaccessible(input)
            } catch (_: SecurityException) {
                return CliInputPathInspection.Inaccessible(input)
            }
        } else {
            absolutePath
        }
        return CliInputPathInspection.Existing(resolvedPath, isDirectory, isReadable)
    }
}
