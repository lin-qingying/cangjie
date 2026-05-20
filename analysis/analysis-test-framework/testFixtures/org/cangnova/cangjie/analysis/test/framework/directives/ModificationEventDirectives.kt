@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.test.framework.directives

import com.intellij.openapi.application.ApplicationManager
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEventKind
import org.cangnova.cangjie.analysis.api.platform.modification.KotlinModuleStateModificationKind
import org.cangnova.cangjie.analysis.api.platform.modification.isModuleLevel
import org.cangnova.cangjie.analysis.api.platform.modification.publishCodeFragmentContextModificationEvent
import org.cangnova.cangjie.analysis.api.platform.modification.publishGlobalModuleStateModificationEvent
import org.cangnova.cangjie.analysis.api.platform.modification.publishGlobalSourceModuleStateModificationEvent
import org.cangnova.cangjie.analysis.api.platform.modification.publishGlobalSourceOutOfBlockModificationEvent
import org.cangnova.cangjie.analysis.api.platform.modification.publishModuleOutOfBlockModificationEvent
import org.cangnova.cangjie.analysis.api.platform.modification.publishModuleStateModificationEvent
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructure
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleOrZeroValue

/**
 * Analysis API 测试中的修改事件指令。
 *
 * 这组指令对齐 Kotlin `ModificationEventDirectives` 的职责：
 * 1. 用模块级或全局级事件驱动 session 失效测试；
 * 2. 让同一份 testData 能被多个事件种类重复复用；
 * 3. 明确事件目标模块，而不是在抽象测试里手写模块名。
 */
object ModificationEventDirectives : SimpleDirectivesContainer() {
    val MODIFICATION_EVENT by enumDirective<KotlinModificationEventKind>(
        description = "当前测试模块应发布的修改事件种类。",
    )

    val WILDCARD_MODIFICATION_EVENT by directive(
        description = "测试数据声明会发布修改事件，但具体事件种类由 generated test 固定。",
    )

    val MODIFICATION_EVENT_TARGET by enumDirective<ModificationEventDirectiveTarget>(
        description = "模块级修改事件的目标模块。",
        applicability = DirectiveApplicability.Module,
    )
}

/**
 * 模块级修改事件的目标模块。
 */
enum class ModificationEventDirectiveTarget {
    SELF,
    FALLBACK_DEPENDENCIES,
}

fun CjTestModule.publishModificationEventByDirective(isOptional: Boolean = false) {
    val modificationEventKinds = testModule.directives[ModificationEventDirectives.MODIFICATION_EVENT]
    val modificationEventKind = when (modificationEventKinds.size) {
        0 -> {
            if (isOptional) return
            error("Expected `${ModificationEventDirectives.MODIFICATION_EVENT.name}` in test module `$name`.")
        }

        1 -> modificationEventKinds.single()
        else -> error("Test module `$name` must not declare multiple modification events.")
    }

    publishModificationEvent(modificationEventKind)
}

fun CjTestModule.publishWildcardModificationEventByDirectiveIfPresent(modificationEventKind: KotlinModificationEventKind) {
    if (ModificationEventDirectives.WILDCARD_MODIFICATION_EVENT in testModule.directives) {
        publishModificationEvent(modificationEventKind)
    }
}

fun CjTestModuleStructure.publishWildcardModificationEventsByDirective(modificationEventKind: KotlinModificationEventKind) {
    if (modificationEventKind.isModuleLevel) {
        mainModules.forEach { module ->
            module.publishWildcardModificationEventByDirectiveIfPresent(modificationEventKind)
        }
    } else {
        if (!testModuleStructure.allDirectives.contains(ModificationEventDirectives.WILDCARD_MODIFICATION_EVENT)) {
            return
        }

        publishGlobalModificationEvent(modificationEventKind)
    }
}

private fun CjTestModule.publishModificationEvent(modificationEventKind: KotlinModificationEventKind) {
    val targetModule = when (modificationEventDirectiveTarget) {
        ModificationEventDirectiveTarget.SELF -> caModule
        ModificationEventDirectiveTarget.FALLBACK_DEPENDENCIES -> caModule.getFallbackDependenciesModule()
    }

    publishModificationEventByKind(modificationEventKind, targetModule)
}

private val CjTestModule.modificationEventDirectiveTarget: ModificationEventDirectiveTarget
    get() = testModule.directives.singleOrZeroValue(ModificationEventDirectives.MODIFICATION_EVENT_TARGET)
        ?: ModificationEventDirectiveTarget.SELF

private fun CaModule.getFallbackDependenciesModule(): CaModule {
    require(this is CaLibraryModule || this is CaLibrarySourceModule) {
        "MODIFICATION_EVENT_TARGET=${ModificationEventDirectiveTarget.FALLBACK_DEPENDENCIES.name} " +
            "只能用于 library / library source 模块，但 `$moduleDescription` 不是。"
    }

    return directRegularDependencies.singleOrNull { dependency -> dependency is CaLibraryFallbackDependenciesModule }
        ?: error("Module `$moduleDescription` does not expose a fallback dependencies module.")
}

private fun CjTestModuleStructure.publishGlobalModificationEvent(modificationEventKind: KotlinModificationEventKind) {
    val application = ApplicationManager.getApplication()
    application.runWriteAction {
        when (modificationEventKind) {
            KotlinModificationEventKind.GLOBAL_MODULE_STATE_MODIFICATION ->
                project.publishGlobalModuleStateModificationEvent()

            KotlinModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION ->
                project.publishGlobalSourceModuleStateModificationEvent()

            KotlinModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION ->
                project.publishGlobalSourceOutOfBlockModificationEvent()

            KotlinModificationEventKind.MODULE_STATE_MODIFICATION,
            KotlinModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION,
            KotlinModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION ->
                error("Cannot publish module-level event `$modificationEventKind` without a target module.")
        }
    }
}

private fun publishModificationEventByKind(
    modificationEventKind: KotlinModificationEventKind,
    module: CaModule,
) {
    val application = ApplicationManager.getApplication()
    application.runWriteAction {
        when (modificationEventKind) {
            KotlinModificationEventKind.MODULE_STATE_MODIFICATION ->
                module.publishModuleStateModificationEvent(KotlinModuleStateModificationKind.UPDATE)

            KotlinModificationEventKind.MODULE_OUT_OF_BLOCK_MODIFICATION ->
                module.publishModuleOutOfBlockModificationEvent()

            KotlinModificationEventKind.CODE_FRAGMENT_CONTEXT_MODIFICATION ->
                module.publishCodeFragmentContextModificationEvent()

            KotlinModificationEventKind.GLOBAL_MODULE_STATE_MODIFICATION,
            KotlinModificationEventKind.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION,
            KotlinModificationEventKind.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION ->
                error("Global event `$modificationEventKind` must be published without a target module.")
        }
    }
}
