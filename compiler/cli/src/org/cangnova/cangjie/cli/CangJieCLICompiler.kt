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

package org.cangnova.cangjie.cli

import org.cangnova.cangjie.arguments.dsl.base.CangJieCompilerArguments
import org.cangnova.cangjie.arguments.dsl.base.CangJieReleaseVersion
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113FormatException
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113Phase
import org.cangnova.cangjie.chir.core.serializer.OfficialChirV113SemanticPackageDecoder
import org.cangnova.cangjie.arguments.description.cangjieCompilerArguments
import org.cangnova.cangjie.cli.common.CangJieCLICompiler
import org.cangnova.cangjie.cli.common.CliInputPathInspection
import org.cangnova.cangjie.cli.common.CliInputPathInspector
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.configureLanguageVersionSettings
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.packagePaths
import org.cangnova.cangjie.config.parsingDiagnosticSettings
import org.cangnova.cangjie.config.PositionalArtifactInput
import org.cangnova.cangjie.config.PositionalArtifactKind
import org.cangnova.cangjie.config.positionalArtifactInputs
import org.cangnova.cangjie.config.positionalChirInputs
import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.frontend.pipeline.AbstractFrontendPipeline
import org.cangnova.cangjie.frontend.pipeline.CjmpLegacyChirInput
import org.cangnova.cangjie.frontend.pipeline.cjmpLegacyChirInputs
import org.cangnova.cangjie.CjmpLanguageFeature
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.supportsCjmpFeature
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.WarningGroup
import com.intellij.openapi.util.SystemInfoRt
import java.nio.file.Path
import java.io.IOException
import java.nio.file.Files

/**
 * 将已解析的命令行参数接入已有 frontend 管线的 CLI 基类。
 *
 * 具体命令负责把源码参数装配到配置；driver 管理的位置 CHIR、二进制工件与包路径
 * 分别保存在编译配置中，避免它们进入源码根。
 */
public abstract class CangJieFrontendCLICompiler<A : CommonCompilerArguments>(
    private val frontendPipeline: AbstractFrontendPipeline<A>,
    schema: CangJieCompilerArguments = cangjieCompilerArguments,
    compilerRelease: CangJieReleaseVersion = CangJieReleaseVersion.V_1_1_3,
    private val legacyCjmpCompilationDriver: LegacyCjmpCompilationDriver = DefaultLegacyCjmpCompilationDriver(),
) : CangJieCLICompiler<A>(schema, compilerRelease) {
    private companion object {
        const val CHIR_FILE_SUFFIX = ".chir"
        const val CJO_FILE_EXTENSION = "cjo"
        const val LLVM_BITCODE_FILE_EXTENSION = "bc"
        const val OBJECT_FILE_EXTENSION = "o"
        const val ARCHIVE_FILE_EXTENSION = "a"
        const val COFF_OBJECT_FILE_EXTENSION = "obj"
    }

    /** 把已剔除位置 CHIR 工件的自由参数装配为当前 frontend 的源码输入。 */
    protected abstract fun configureSourceArguments(
        freeArguments: List<String>,
        configuration: CompilerConfiguration,
    ): Boolean

    override fun prepareConfiguration(configuration: CompilerConfiguration) {
        configuration.positionalChirInputs = emptyList()
        configuration.cjmpLegacyChirInputs = emptyList()
        configuration.positionalArtifactInputs = emptyList()
        configuration.packagePaths = emptyList()
    }

    final override fun executeParsedArguments(
        arguments: A,
        freeArguments: List<String>,
        configuration: CompilerConfiguration,
    ): Boolean {
        if (!configuration.configureLanguageVersionSettings(arguments.languageVersion)) return false
        val sourceArguments = mutableListOf<String>()

        val legacyCjmpDriver =
            configuration.languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.CommonSpecificDeclarations) &&
                !configuration.languageVersionSettings.supportsCjmpFeature(CjmpLanguageFeature.DedicatedCommonPartChirInputs)
        val positionalChirArguments = freeArguments.filter {
            classifyPositionalInput(it, arguments.compileCjd, legacyCjmpDriver) == PositionalInputKind.CHIR
        }
        val chirPaths = mutableListOf<Path>()
        val packagePaths = mutableListOf<Path>()
        val artifactInputs = mutableListOf<PositionalArtifactInput>()
        for (argument in freeArguments) {
            val inputKind = classifyPositionalInput(argument, arguments.compileCjd, legacyCjmpDriver)
            when (inputKind) {
                PositionalInputKind.SOURCE -> {
                    val inspected = inspectRequiredInput(configuration, argument) ?: return false
                    if (fileExtension(inspected.path) != CjSourceKind.SOURCE_SUFFIX.removePrefix(".")) {
                        reportUnexpectedExtension(configuration, CjSourceKind.SOURCE_SUFFIX, argument, inspected.path)
                        continue
                    }
                    sourceArguments.add(argument)
                }

                PositionalInputKind.DECLARATION -> {
                    inspectRequiredInput(configuration, argument) ?: return false
                    sourceArguments.add(argument)
                }

                PositionalInputKind.CHIR -> {
                    val inspected = inspectLegacyChirInput(configuration, argument) ?: return false
                    if (fileExtension(inspected.path) != CHIR_FILE_SUFFIX.removePrefix(".")) {
                        reportUnexpectedExtension(configuration, CHIR_FILE_SUFFIX, argument, inspected.path)
                        continue
                    }
                    chirPaths.add(inspected.path)
                }

                PositionalInputKind.CJO,
                PositionalInputKind.LLVM_BITCODE,
                PositionalInputKind.OBJECT,
                PositionalInputKind.ARCHIVE,
                PositionalInputKind.COFF_OBJECT,
                -> {
                    val inspected = inspectRequiredInput(configuration, argument) ?: return false
                    if (fileExtension(inspected.path) != inputKind.extension) {
                        reportUnexpectedExtension(configuration, inputKind.suffix, argument, inspected.path)
                        continue
                    }
                    if (inputKind == PositionalInputKind.CJO &&
                        artifactInputs.any { it.kind == PositionalArtifactKind.CJO }
                    ) {
                        reportInputPathError(configuration, "cli.error.positional.cjo.multiple", argument)
                        return false
                    }
                    artifactInputs.add(
                        PositionalArtifactInput(checkNotNull(inputKind.artifactKind), argument, inspected.path),
                    )
                }

                PositionalInputKind.FALLBACK -> {
                    when (
                        val inspected = CliInputPathInspector.inspect(
                            argument,
                            resolveRealPath = true,
                            checkResolvedPathLength = false,
                        )
                    ) {
                        is CliInputPathInspection.PathTooLong -> {
                            reportInputPathError(
                                configuration,
                                "cli.error.positional.input.path.too.long",
                                argument,
                                inspected.maximumLength,
                            )
                            return false
                        }

                        is CliInputPathInspection.Existing -> {
                            if (inspected.isDirectory) {
                                val packagePath = Path.of(argument)
                                packagePaths.add(packagePath)
                                configuration.packagePaths = packagePaths
                                reportInputPathError(
                                    configuration,
                                    "cli.error.positional.input.directory.unsupported",
                                    argument,
                                )
                                return false
                            } else {
                                reportUnusedInput(configuration, argument)
                            }
                        }

                        is CliInputPathInspection.InvalidPath,
                        is CliInputPathInspection.Missing,
                        is CliInputPathInspection.Inaccessible,
                        -> {
                            reportInputPathError(
                                configuration,
                                positionalInputPathFailureMessageKey(inspected),
                                argument,
                            )
                            return false
                        }
                    }
                }
            }
        }

        if (legacyCjmpDriver && positionalChirArguments.isNotEmpty() && arguments.cjmpCommonPart.isEmpty()) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                CangJieParsingBundle.message("cli.error.positional.cjmp.cjo.required"),
            )
            return false
        }
        if (legacyCjmpDriver && arguments.cjmpCommonPart.isNotEmpty() && positionalChirArguments.isEmpty()) {
            configuration.messageCollector.report(
                CompilerMessageSeverity.ERROR,
                CangJieParsingBundle.message("cli.error.positional.cjmp.chir.required"),
            )
            return false
        }
        if (legacyCjmpDriver && chirPaths.size > 1) {
            reportInputPathError(
                configuration,
                "cli.error.positional.cjmp.chir.multiple",
                chirPaths.size,
            )
            return false
        }

        if (legacyCjmpDriver && chirPaths.isNotEmpty() && !importLegacyChirInputs(configuration, chirPaths)) {
            return false
        }

        configuration.positionalChirInputs = chirPaths
        configuration.packagePaths = packagePaths
        configuration.positionalArtifactInputs = artifactInputs

        if (!configureSourceArguments(sourceArguments, configuration)) return false
        val frontendResult = frontendPipeline.executeWithArtifact(arguments, configuration)
        if (!frontendResult.succeeded) return false
        if (!legacyCjmpDriver || chirPaths.isEmpty()) return true

        val legacyChirInput = configuration.cjmpLegacyChirInputs.single()
        return legacyCjmpCompilationDriver.compile(
            arguments = arguments,
            configuration = configuration,
            frontendArtifact = frontendResult.artifact,
            legacyChirInput = legacyChirInput,
        )
    }

    private fun importLegacyChirInputs(
        configuration: CompilerConfiguration,
        paths: List<Path>,
    ): Boolean {
        val importedInputs = ArrayList<CjmpLegacyChirInput>(paths.size)
        for (path in paths) {
            val bytes = try {
                Files.readAllBytes(path)
            } catch (exception: IOException) {
                reportInputPathError(
                    configuration,
                    "cli.error.positional.cjmp.chir.read.failed",
                    path,
                    exception.message.orEmpty(),
                )
                return false
            } catch (exception: SecurityException) {
                reportInputPathError(
                    configuration,
                    "cli.error.positional.cjmp.chir.read.failed",
                    path,
                    exception.message.orEmpty(),
                )
                return false
            }

            val semanticPackage = try {
                OfficialChirV113SemanticPackageDecoder.decode(bytes)
            } catch (exception: OfficialChirV113FormatException) {
                reportInputPathError(
                    configuration,
                    "cli.error.positional.cjmp.chir.invalid.file",
                    path,
                    exception.message.orEmpty(),
                )
                return false
            }
            if (semanticPackage.metadata.phase != OfficialChirV113Phase.RAW) {
                reportInputPathError(
                    configuration,
                    "cli.error.positional.cjmp.chir.phase.unsupported",
                    path,
                    semanticPackage.metadata.phase,
                )
                return false
            }
            importedInputs += CjmpLegacyChirInput(path, semanticPackage)
        }
        configuration.cjmpLegacyChirInputs = importedInputs
        return true
    }

    private fun reportInputPathError(
        configuration: CompilerConfiguration,
        messageKey: String,
        vararg parameters: Any,
    ) {
        configuration.messageCollector.report(
            CompilerMessageSeverity.ERROR,
            CangJieParsingBundle.message(messageKey, *parameters),
        )
    }

    private fun hasDeclarationSuffix(argument: String): Boolean {
        val suffix = CjSourceKind.DECLARATION_SUFFIX
        return argument.length > suffix.length && argument.endsWith(suffix)
    }

    private enum class PositionalInputKind(
        val suffix: String = "",
        val artifactKind: PositionalArtifactKind? = null,
    ) {
        SOURCE(CjSourceKind.SOURCE_SUFFIX),
        DECLARATION(CjSourceKind.DECLARATION_SUFFIX),
        CHIR(CHIR_FILE_SUFFIX),
        CJO(".$CJO_FILE_EXTENSION", PositionalArtifactKind.CJO),
        LLVM_BITCODE(".$LLVM_BITCODE_FILE_EXTENSION", PositionalArtifactKind.LLVM_BITCODE),
        OBJECT(".$OBJECT_FILE_EXTENSION", PositionalArtifactKind.OBJECT),
        ARCHIVE(".$ARCHIVE_FILE_EXTENSION", PositionalArtifactKind.ARCHIVE),
        COFF_OBJECT(".$COFF_OBJECT_FILE_EXTENSION", PositionalArtifactKind.COFF_OBJECT),
        FALLBACK;

        val extension: String
            get() = suffix.removePrefix(".")
    }

    private fun classifyPositionalInput(
        argument: String,
        compileCjd: Boolean,
        legacyCjmpDriver: Boolean,
    ): PositionalInputKind {
        val extension = fileExtension(argument)
        return when {
            extension == OBJECT_FILE_EXTENSION -> PositionalInputKind.OBJECT
            extension == ARCHIVE_FILE_EXTENSION -> PositionalInputKind.ARCHIVE
            extension == COFF_OBJECT_FILE_EXTENSION -> PositionalInputKind.COFF_OBJECT
            extension == CjSourceKind.SOURCE_SUFFIX.removePrefix(".") && !compileCjd -> PositionalInputKind.SOURCE
            extension == LLVM_BITCODE_FILE_EXTENSION -> PositionalInputKind.LLVM_BITCODE
            legacyCjmpDriver && extension == CHIR_FILE_SUFFIX.removePrefix(".") -> PositionalInputKind.CHIR
            extension == CJO_FILE_EXTENSION -> PositionalInputKind.CJO
            compileCjd && hasDeclarationSuffix(argument) -> PositionalInputKind.DECLARATION
            else -> PositionalInputKind.FALLBACK
        }
    }

    private fun fileExtension(path: Path): String = fileExtension(path.toString())

    private fun fileExtension(path: String): String {
        val lastDot = path.lastIndexOf('.')
        if (lastDot <= 0) return ""
        val lastDirectorySeparator = if (SystemInfoRt.isWindows) {
            maxOf(path.lastIndexOf('/'), path.lastIndexOf('\\'))
        } else {
            path.lastIndexOf('/')
        }
        if (lastDirectorySeparator >= lastDot || lastDot == lastDirectorySeparator + 1) return ""
        return path.substring(lastDot + 1)
    }

    private fun inspectLegacyChirInput(
        configuration: CompilerConfiguration,
        argument: String,
    ): CliInputPathInspection.Existing? {
        when (val inspected = CliInputPathInspector.inspect(argument, resolveRealPath = true)) {
            is CliInputPathInspection.Existing -> {
                if (inspected.isDirectory) {
                    reportInputPathError(configuration, "cli.error.positional.cjmp.chir.not.file", argument)
                    return null
                }
                if (!inspected.isReadable) {
                    reportInputPathError(configuration, "cli.error.positional.cjmp.chir.permission.denied", argument)
                    return null
                }
                return inspected
            }

            is CliInputPathInspection.InvalidPath -> {
                reportInputPathError(configuration, "cli.error.positional.cjmp.chir.invalid.path", argument)
            }

            is CliInputPathInspection.PathTooLong -> {
                reportInputPathError(
                    configuration,
                    "cli.error.positional.cjmp.chir.path.too.long",
                    argument,
                    inspected.maximumLength,
                )
            }

            is CliInputPathInspection.Missing -> {
                reportInputPathError(configuration, "cli.error.positional.cjmp.chir.not.found", argument)
            }

            is CliInputPathInspection.Inaccessible -> {
                reportInputPathError(configuration, "cli.error.positional.cjmp.chir.inaccessible", argument)
            }
        }
        return null
    }

    private fun inspectRequiredInput(
        configuration: CompilerConfiguration,
        argument: String,
    ): CliInputPathInspection.Existing? {
        when (val inspected = CliInputPathInspector.inspect(argument, resolveRealPath = true)) {
            is CliInputPathInspection.Existing -> {
                if (inspected.isDirectory) {
                    reportInputPathError(configuration, "cli.error.positional.input.not.file", argument)
                    return null
                }
                if (!inspected.isReadable) {
                    reportInputPathError(configuration, "cli.error.positional.input.permission.denied", argument)
                    return null
                }
                return inspected
            }

            is CliInputPathInspection.InvalidPath -> {
                reportInputPathError(configuration, "cli.error.positional.input.invalid.path", argument)
            }

            is CliInputPathInspection.PathTooLong -> {
                reportInputPathError(
                    configuration,
                    "cli.error.positional.input.path.too.long",
                    argument,
                    inspected.maximumLength,
                )
            }

            is CliInputPathInspection.Missing -> {
                reportInputPathError(configuration, "cli.error.positional.input.not.found", argument)
            }

            is CliInputPathInspection.Inaccessible -> {
                reportInputPathError(configuration, "cli.error.positional.input.inaccessible", argument)
            }
        }
        return null
    }

    private fun reportUnexpectedExtension(
        configuration: CompilerConfiguration,
        expectedExtension: String,
        argument: String,
        resolvedPath: Path,
    ) {
        if (configuration.parsingDiagnosticSettings.isSuppressed(WarningGroup.DRIVER_ARG)) return
        configuration.messageCollector.report(
            CompilerMessageSeverity.WARNING,
            CangJieParsingBundle.message(
                "cli.warning.positional.input.extension",
                expectedExtension,
                argument,
                resolvedPath,
            ),
        )
    }

    private fun reportUnusedInput(configuration: CompilerConfiguration, argument: String) {
        if (configuration.parsingDiagnosticSettings.isSuppressed(WarningGroup.DRIVER_ARG)) return
        configuration.messageCollector.report(
            CompilerMessageSeverity.WARNING,
            CangJieParsingBundle.message("cli.warning.positional.input.unused", argument),
        )
    }
}

/** 将路径检查失败映射到对应的 CLI 诊断定义。 */
internal fun positionalInputPathFailureMessageKey(inspection: CliInputPathInspection): String = when (inspection) {
    is CliInputPathInspection.InvalidPath -> "cli.error.positional.input.invalid.path"
    is CliInputPathInspection.Missing -> "cli.error.positional.input.not.found"
    is CliInputPathInspection.Inaccessible -> "cli.error.positional.input.inaccessible"
    else -> error("Only positional path failures have a failure diagnostic")
}
