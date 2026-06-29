package org.cangnova.cangjie.analysis.test.framework.services.configuration

import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices

/**
 * Analysis API 测试中 binary library 的索引策略。
 *
 * 对齐 Kotlin `AnalysisApiIndexingConfiguration`：
 * 该服务只描述“测试宿主是否需要为 binary library 建 stub / 包集合索引”，
 * 不把 low-level 或 analysis-api 的平台策略硬编码到生产服务里。
 */
class AnalysisApiIndexingConfiguration(
    /**
     * 当前测试运行中 binary library 的索引模式。
     */
    val binaryLibraryIndexingMode: AnalysisApiBinaryLibraryIndexingMode,
) : TestService

/**
 * binary library 在 Analysis API 测试里的索引模式。
 */
enum class AnalysisApiBinaryLibraryIndexingMode {
    /**
     * binary library 应构建并暴露 stub 索引。
     *
     * 这对应 low-level 通过 stub-based library provider 反序列化符号的测试模式。
     */
    INDEX_STUBS,

    /**
     * binary library 不建立测试态 stub 索引，库声明按 binary-origin 读取。
     */
    NO_INDEXING,
}

/**
 * 当前测试服务容器中的 Analysis API library 索引配置。
 */
val TestServices.libraryIndexingConfiguration: AnalysisApiIndexingConfiguration?
    by TestServices.nullableTestServiceAccessor()
