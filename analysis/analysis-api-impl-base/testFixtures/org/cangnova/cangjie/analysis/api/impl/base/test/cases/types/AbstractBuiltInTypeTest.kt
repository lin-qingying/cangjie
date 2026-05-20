package org.cangnova.cangjie.analysis.api.impl.base.test.cases.types

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue
import org.cangnova.cangjie.test.services.TestServices

abstract class AbstractBuiltInTypeTest : AbstractTypeTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    override fun getType(
        analysisSession: CaSession,
        cjFile: CjFile,
        module: CjTestModule,
        testServices: TestServices,
    ): CaType = with(analysisSession) {
        val classId = ClassId.fromString(module.testModule.directives.singleValue(Directives.BUILTIN_CLASS_ID))
        val symbol = getClassLikeSymbol(classId)
            ?: error("Cannot resolve built-in class-like symbol `${classId.asString()}`.")
        buildClassType(symbol)
    }

    object Directives : SimpleDirectivesContainer() {
        val BUILTIN_CLASS_ID by stringDirective("当前 builtins 测试要恢复的内置类型 ClassId。")
    }
}
