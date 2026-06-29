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

/**
 * builtins class-like 类型的公开类型快照测试。
 *
 * 测试通过 `ClassId` 恢复内置 class-like symbol，再使用公开 `buildClassType(symbol)` 构造 `CaType`。
 */
abstract class AbstractBuiltInTypeTest : AbstractTypeTest() {
    /**
     * 当前测试额外注册的 builtins ClassId 指令。
     *
     * 公共类型测试提供输出快照，本测试只补充要构造的内置类型身份。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    /**
     * 根据测试模块指令构造内置 class-like 类型。
     *
     * 方法先恢复 builtins symbol，再通过公开 type creator 构造可渲染、可展开的 `CaType`。
     */
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

    /**
     * builtins 类型测试的专用指令集合。
     *
     * 该容器只负责声明要恢复的内置类型 `ClassId`。
     */
    object Directives : SimpleDirectivesContainer() {
        /**
         * 当前 builtins 测试要恢复并构造的内置类型 `ClassId`。
         *
         * 例如基础类型、集合类型或其它标准库内置声明都通过该字段指定。
         */
        val BUILTIN_CLASS_ID by stringDirective("当前 builtins 测试要恢复的内置类型 ClassId。")
    }
}
