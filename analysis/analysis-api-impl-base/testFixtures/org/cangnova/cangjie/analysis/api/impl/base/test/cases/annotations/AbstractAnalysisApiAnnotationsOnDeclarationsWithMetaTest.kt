package org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList

/**
 * 声明注解及其 meta-annotations 的抽象测试。
 *
 * 该测试复用声明注解测试的目标定位逻辑，仅切换 renderer，额外验证注解 class-like symbol 上的注解链。
 */
abstract class AbstractAnalysisApiAnnotationsOnDeclarationsWithMetaTest : AbstractAnalysisApiAnnotationsOnDeclarationsTest() {
    /**
     * 渲染声明注解并递归展开 meta-annotations。
     *
     * 方法保持与父类相同的输入契约，但输出会包含注解自身类型声明上的注解信息。
     */
    override fun renderAnnotations(analysisSession: CaSession, annotations: CaAnnotationList): String {
        return TestAnnotationRenderer.renderAnnotationsWithMeta(analysisSession, annotations)
    }
}
