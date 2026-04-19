package org.cangnova.cangjie.analysis.api.platform.modification

import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.ThreadingAssertions
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.analysisMessageBus
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * Publishes a [KotlinModificationEvent] to the project's [analysisMessageBus]. Must be called in a write action.
 */
@CaPlatformInterface
fun Project.publishModificationEvent(event: KotlinModificationEvent) {
    ThreadingAssertions.assertWriteAccess()

    analysisMessageBus.syncPublisher(KotlinModificationEvent.TOPIC).onModification(event)
}

/**
 * Publishes a [KotlinModuleStateModificationEvent] for this [CaModule]. Must be called in a write action.
 */
@CaPlatformInterface
fun CaModule.publishModuleStateModificationEvent(modificationKind: KotlinModuleStateModificationKind) {
    project.publishModificationEvent(KotlinModuleStateModificationEvent(this, modificationKind))
}

/**
 * Publishes a [KotlinModuleOutOfBlockModificationEvent] for this [CaModule]. Must be called in a write action.
 */
@CaPlatformInterface
fun CaModule.publishModuleOutOfBlockModificationEvent() {
    project.publishModificationEvent(KotlinModuleOutOfBlockModificationEvent(this))
}

/**
 * Publishes a [KotlinGlobalModuleStateModificationEvent]. Must be called in a write action.
 */
@CaPlatformInterface
fun Project.publishGlobalModuleStateModificationEvent() {
    publishModificationEvent(KotlinGlobalModuleStateModificationEvent)
}

/**
 * Publishes a [KotlinGlobalSourceModuleStateModificationEvent]. Must be called in a write action.
 */
@CaPlatformInterface
fun Project.publishGlobalSourceModuleStateModificationEvent() {
    publishModificationEvent(KotlinGlobalSourceModuleStateModificationEvent)
}

/**
 * Publishes a [KotlinGlobalSourceOutOfBlockModificationEvent]. Must be called in a write action.
 */
@CaPlatformInterface
fun Project.publishGlobalSourceOutOfBlockModificationEvent() {
    publishModificationEvent(KotlinGlobalSourceOutOfBlockModificationEvent)
}

/**
 * Publishes a [KotlinCodeFragmentContextModificationEvent] for this [CaModule]. Must be called in a write action.
 */
@CaPlatformInterface
fun CaModule.publishCodeFragmentContextModificationEvent() {
    project.publishModificationEvent(KotlinCodeFragmentContextModificationEvent(this))
}
