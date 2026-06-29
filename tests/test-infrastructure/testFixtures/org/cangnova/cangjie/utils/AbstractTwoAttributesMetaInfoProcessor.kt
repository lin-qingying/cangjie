package org.cangnova.cangjie.utils

import org.cangnova.cangjie.test.codeMetaInfo.model.CodeMetaInfo
import org.cangnova.cangjie.test.codeMetaInfo.model.ParsedCodeMetaInfo
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.AdditionalMetaInfoProcessor
import org.cangnova.cangjie.test.services.TestServices

/**
 * 表示 `AbstractTwoAttributesMetaInfoProcessor`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
abstract class AbstractTwoAttributesMetaInfoProcessor(testServices: TestServices) : AdditionalMetaInfoProcessor(testServices) {
    /**
     * 保存 `firstAttribute`，供测试基础设施在测试执行期间读取或传递。
     */
    protected abstract val firstAttribute: String
    /**
     * 保存 `secondAttribute`，供测试基础设施在测试执行期间读取或传递。
     */
    protected abstract val secondAttribute: String

    /**
     * 提供 `processorEnabled` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    protected abstract fun processorEnabled(module: TestModule): Boolean
    /**
     * 提供 `firstAttributeEnabled` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    protected abstract fun firstAttributeEnabled(module: TestModule): Boolean

    /**
     * 执行 `processMetaInfos` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun processMetaInfos(module: TestModule, file: TestFile) {
        /*
         * Rules for OI/NI attribute:
         * ┌──────────┬───────┬────────┬──────────┐
         * │          │ first │ second │ nothing  │ <- reported
         * ├──────────┼───────┼────────┼──────────┤
         * │  nothing │  both │  both  │ nothing  │
         * │   first  │ first │  both  │  first   │
         * │  second  │  both │ second │  second  │
         * │   both   │  both │  both  │ opposite │ <- first if second enabled in test and vice versa
         * └──────────┴───────┴────────┴──────────┘
         *       ^ existed
         */
        if (!processorEnabled(module)) return
        val (currentFlag, otherFlag) = when (firstAttributeEnabled(module)) {
            true -> firstAttribute to secondAttribute
            false -> secondAttribute to firstAttribute
        }
        val matchedExistedInfos = mutableSetOf<ParsedCodeMetaInfo>()
        val matchedReportedInfos = mutableSetOf<CodeMetaInfo>()
        val allReportedInfos = globalMetadataInfoHandler.getReportedMetaInfosForFile(file)
        for ((_, reportedInfos) in allReportedInfos.groupBy { Triple(it.start, it.end, it.tag) }) {
            val existedInfos = globalMetadataInfoHandler.getExistingMetaInfosForActualMetadata(file, reportedInfos.first())
            for ((reportedInfo, existedInfo) in reportedInfos.zip(existedInfos)) {
                matchedExistedInfos += existedInfo
                matchedReportedInfos += reportedInfo
                if (currentFlag !in reportedInfo.attributes) continue
                if (currentFlag in existedInfo.attributes) continue
                reportedInfo.attributes.remove(currentFlag)
            }
        }

        if (allReportedInfos.size != matchedReportedInfos.size) {
            for (info in allReportedInfos) {
                if (info !in matchedReportedInfos) {
                    info.attributes.remove(currentFlag)
                }
            }
        }

        val allExistedInfos = globalMetadataInfoHandler.getExistingMetaInfosForFile(file)
        if (allExistedInfos.size == matchedExistedInfos.size) return

        val newInfos = allExistedInfos.mapNotNull {
            if (it in matchedExistedInfos) return@mapNotNull null
            if (currentFlag in it.attributes) return@mapNotNull null
            it.copy().apply {
                if (otherFlag !in attributes) {
                    attributes += otherFlag
                }
            }
        }
        globalMetadataInfoHandler.addMetadataInfosForFile(file, newInfos)
    }
}
