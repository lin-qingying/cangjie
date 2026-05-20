package org.cangnova.cangjie.analysis.api.impl.base.test.cases.annotations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList

abstract class AbstractAnalysisApiAnnotationsOnDeclarationsWithMetaTest : AbstractAnalysisApiAnnotationsOnDeclarationsTest() {
    override fun renderAnnotations(analysisSession: CaSession, annotations: CaAnnotationList): String {
        return TestAnnotationRenderer.renderAnnotationsWithMeta(analysisSession, annotations)
    }
}
