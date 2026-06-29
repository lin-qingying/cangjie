@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.cangnova.cangjie.analysis.api.platform.analysisMessageBus
import org.cangnova.cangjie.analysis.api.platform.modification.CaElementModificationType
import org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationLocality
import org.cangnova.cangjie.analysis.api.platform.modification.publishModuleOutOfBlockModificationEvent
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.getNonLocalContainingOrThisDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirResolvableSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.codeFragment
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.analysis.utils.printer.parentOfType
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.isAncestorOf
import org.cangnova.cangjie.source.psi

/**
 * This service is responsible for processing incoming [PsiElement] changes to reflect them on CFIR tree.
 *
 * For local changes (in-block modification), this service will do all required work
 * and publish [LLCfirDeclarationModificationTopics.IN_BLOCK_MODIFICATION].
 *
 * In case of non-local changes (out-of-block modification), this service will publish a [KotlinModuleOutOfBlockModificationEvent][org.cangnova.cangjie.analysis.api.platform.modification.KotlinModuleOutOfBlockModificationEvent].
 *
 * @see getNonLocalReanalyzableContainingDeclaration
 * @see org.cangnova.cangjie.analysis.api.platform.modification.KotlinModuleOutOfBlockModificationEvent
 * @see LLCfirDeclarationModificationTopics.IN_BLOCK_MODIFICATION
 * @see org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationService
 *
 * @param project 当前低阶 CFIR 服务所属的 IntelliJ 工程，用于访问消息总线、工程结构服务和工程级缓存。
 */
@LLCfirInternals
class LLCfirDeclarationModificationService(val project: Project) : Disposable {
    init {
        ApplicationManager.getApplication().addApplicationListener(
            object : ApplicationListener {
                /**
                 * 在写动作结束后统一冲刷延迟修改队列。
                 *
                 * PSI 事件可能在同一个写动作中连续到达；延迟到写动作尾部处理可以合并重复的块内修改，
                 * 同时避免在 PSI 树仍处于中间状态时访问 CFIR 缓存。
                 */
                override fun writeActionFinished(action: Any) {
                    flushDeferredModifications()
                }
            },
            this,
        )

        project.messageBus.connect(this).subscribe(
            CjCodeFragment.IMPORT_MODIFICATION,
            CangJieCodeFragmentImportModificationListener { codeFragment -> handleOutOfBlockModification(codeFragment) }
        )
    }

    /**
     * 当前写动作内收集到、可以延迟处理的 CFIR 修改。
     *
     * 队列只保存块内修改和空白修改；块外修改会立即发布模块级失效事件，并清理同模块中过期的延迟项。
     * `null` 表示当前没有待处理修改，避免为无修改写动作分配集合。
     */
    private var modificationQueue: MutableSet<LLModificationLocality.Deferrable>? = null

    /**
     * 将可延迟的 [modification] 加入当前写动作队列。
     *
     * 对块内修改，如果对应 PSI 尚未标记为拥有已解析 CFIR body，则无需构建或失效 CFIR，
     * 因为后续查询会从惰性入口自然完成解析。
     */
    private fun addModificationToQueue(modification: LLModificationLocality.Deferrable) {
        // There is no sense to add elements into the queue with an unresolved body.
        if (modification is LLModificationLocality.InBlock && !modification.affectedElement.hasCfirBody) return

        val queue = modificationQueue ?: HashSet<LLModificationLocality.Deferrable>().also { modificationQueue = it }
        queue += modification
    }

    /**
     * We can avoid processing of deferred modifications with the same [CaModule] because the OOBM will invalidate the associated caches
     * anyway.
     */
    private fun dropOutdatedModifications(caModuleWithOutOfBlockModification: CaModule) {
        processQueue { value, iterator ->
            if (value.module == caModuleWithOutOfBlockModification) iterator.remove()
        }
    }

    /**
     * Process valid elements in the current modification queue.
     * Non-valid elements will be dropped from the queue during this iteration.
     *
     * @param action will be executed for each valid element in the queue;
     * **value** is a current element;
     * **iterator** is the corresponding iterator for this element.
     */
    private inline fun processQueue(
        action: (value: LLModificationLocality.Deferrable, iterator: MutableIterator<LLModificationLocality.Deferrable>) -> Unit,
    ) {
        val queue = modificationQueue ?: return
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val element = iterator.next()
            val affectedElement = element.affectedElement
            if (!affectedElement.isValid || (affectedElement.containingFile as? CjCodeFragment)?.context?.isValid == false) {
                iterator.remove()
                continue
            }

            action(element, iterator)
        }
    }

    /**
     * Force the service to publish deferred modifications.
     * This action is required to fix inconsistencies in [CfirFile][org.cangnova.cangjie.cfir.declarations.CfirFile] tree.
     */
    fun flushDeferredModifications() {
        ApplicationManager.getApplication().assertWriteIntentLockAcquired()

        processQueue { value, _ ->
            handleDeferredModification(value)
        }

        modificationQueue = null
    }

    /**
     * @see org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationService.detectLocality
     */
    fun detectLocality(element: PsiElement, modificationType: CaElementModificationType): CaSourceModificationLocality {
        if (!element.isValid) {
            // If PSI is not valid, something bad happened. An OOBM won't hurt.
            return LLModificationLocality.OutOfBlock
        }

        if (element is PsiWhiteSpace || element is PsiComment) {
            // `PsiWhiteSpace` is not a `CjElement`, so we cannot invalidate it directly. This also ensures that we get a somewhat stable
            // element instead of the (possibly deleted) whitespace.
            //
            // If there is no `CjElement` ancestor, we have a non-CangJie file. Whitespace changes in such files are invisible to CangJie.
            val affectedElement = element.parentOfType<CjElement>() ?: return LLModificationLocality.Invisible

            return LLModificationLocality.Whitespace(affectedElement, project)
        }

        if (element.language !is CangJieLanguage) {
            // TODO improve for Java KTIJ-21684
            return LLModificationLocality.OutOfBlock
        }

        val inBlockModificationOwner = nonLocalDeclarationForLocalChange(element) ?: return LLModificationLocality.OutOfBlock

        if (inBlockModificationOwner is CjCodeFragment) {
            // All code fragment content is local
            return LLModificationLocality.InBlock(inBlockModificationOwner, project)
        }

        val isOutOfBlockChange = inBlockModificationOwner is CjAnnotated && (
            element.isNewDirectChildOf(inBlockModificationOwner, modificationType) ||
                modificationType.isBackingFieldAccessChange(inBlockModificationOwner)
            )

        return when {
            !isOutOfBlockChange -> LLModificationLocality.InBlock(inBlockModificationOwner, project)
            else -> LLModificationLocality.OutOfBlock
        }
    }

    /**
     * @see org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationService.handleInvalidation
     */
    fun handleInvalidation(element: PsiElement, modificationLocality: CaSourceModificationLocality) {
        ApplicationManager.getApplication().assertWriteIntentLockAcquired()

        require(modificationLocality is LLModificationLocality) {
            "Expected `${LLModificationLocality::class.simpleName}` but instead got `${modificationLocality::class.simpleName}`. The" +
                    " modification locality must be detected by the same service that performs the invalidation."
        }

        when (modificationLocality) {
            is LLModificationLocality.Invisible -> {}
            is LLModificationLocality.Deferrable -> addModificationToQueue(modificationLocality)
            is LLModificationLocality.OutOfBlock -> handleOutOfBlockModification(element)
        }
    }

    /**
     * This check covers cases such as a new body that was added to a function, which should cause an out-of-block modification.
     */
    private fun PsiElement.isNewDirectChildOf(inBlockModificationOwner: CjAnnotated, modificationType: CaElementModificationType): Boolean =
        modificationType == CaElementModificationType.ElementAdded && parent == inBlockModificationOwner

    /**
     * Backing field access changes are always out-of-block modifications.
     *
     * @see potentiallyAffectsPropertyBackingFieldResolution
     */
    private fun CaElementModificationType.isBackingFieldAccessChange(inBlockModificationOwner: CjAnnotated): Boolean =
        inBlockModificationOwner is CjPropertyAccessor &&
                this is CaElementModificationType.ElementRemoved &&
                removedElement.potentiallyAffectsPropertyBackingFieldResolution()

    /**
     * 根据延迟修改的具体局部性执行实际失效逻辑。
     *
     * 空白修改只需要重置文件结构中的诊断相关缓存；块内修改需要回退对应 CFIR body、
     * 清理文件结构缓存，并发布块内修改主题。
     */
    private fun handleDeferredModification(modificationLocality: LLModificationLocality.Deferrable) {
        when (modificationLocality) {
            is LLModificationLocality.Whitespace ->
                handleWhitespaceModification(modificationLocality.affectedElement, modificationLocality.module)

            is LLModificationLocality.InBlock ->
                handleInBlockModification(modificationLocality.affectedElement, modificationLocality.module)
        }
    }

    /**
     * @see LLModificationLocality.Whitespace
     */
    private fun handleWhitespaceModification(element: CjElement, module: CaModule) {
        val resolvableSession = module.getResolutionFacade(project).sessionProvider.getResolvableSession(module)

        val fileStructure = resolvableSession.moduleComponents.fileStructureCache
            .getCachedFileStructure(element.containingCjFile)
            ?: return

        // To reset diagnostics, we have to invalidate the file structure cache for the affected element.
        fileStructure.invalidateElement(element)
    }

    /**
     * @see LLModificationLocality.InBlock
     */
    private fun handleInBlockModification(declaration: CjElement, module: CaModule) {
        val resolutionFacade = module.getResolutionFacade(project)
        val cfirDeclaration = when (declaration) {
            is CjCodeFragment -> declaration.getOrBuildCfirFile(resolutionFacade).codeFragment
            is CjDeclaration -> declaration.resolveToCfirSymbol(resolutionFacade).cfir
            else -> errorWithCfirSpecificEntries(
                "Unexpected declaration kind: ${declaration::class.simpleName}",
                psi = declaration,
            )
        }

        // 1. Invalidate CFIR
        invalidateAfterInBlockModification(cfirDeclaration)
        declaration.hasCfirBody = false

        val moduleSession = cfirDeclaration.llCfirResolvableSession ?: errorWithCfirSpecificEntries(
            "${LLCfirResolvableModuleSession::class.simpleName} is not found",
            cfir = cfirDeclaration,
            psi = declaration,
        ) {
            withEntry("session", resolutionFacade) { it.toString() }
        }

        // 2. Invalidate caches
        moduleSession.moduleComponents
            .fileStructureCache
            .getCachedFileStructure(declaration.containingCjFile)
            ?.invalidateElement(declaration)

        // 3. Publish event
        project.analysisMessageBus
            .syncPublisher(LLCfirDeclarationModificationTopics.IN_BLOCK_MODIFICATION)
            .afterModification(declaration, module)
    }

    /**
     * @see LLModificationLocality.OutOfBlock
     */
    private fun handleOutOfBlockModification(element: PsiElement) {
        val module = CangJieProjectStructureProvider.getModule(project, element, useSiteModule = null)

        // We should check outdated modifications before to avoid cache dropping (e.g., CaModule cache)
        dropOutdatedModifications(module)
        module.publishModuleOutOfBlockModificationEvent()
    }

    /**
     * @see org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationService.ancestorAffectedByInBlockModification
     */
    fun ancestorAffectedByInBlockModification(changedElement: PsiElement): PsiElement? = nonLocalDeclarationForLocalChange(changedElement)

    /**
     * 释放服务生命周期。
     *
     * 当前服务注册的应用监听器和消息总线连接都以本服务作为 [Disposable] 父级，
     * IntelliJ 平台会在释放时统一断开，因此这里不需要额外清理状态。
     */
    override fun dispose() {}

    companion object {
        fun getInstance(project: Project): LLCfirDeclarationModificationService =
            project.getService(LLCfirDeclarationModificationService::class.java)

        /**
         * This function have to be called from Low Level CFIR body transformers.
         * It is fine to have false-positives, but false-negatives are not acceptable.
         */
        internal fun bodyResolved(element: CfirElementWithResolveState, phase: CfirResolvePhase) {
            when (element) {
                is CfirNamedFunction -> {
                    // in-block modifications only applicable to functions with an explicit type,
                    // so we mark only fully resolved functions
                    if (phase != CfirResolvePhase.BODY_RESOLVE) return
                }

                is CfirProperty -> {
                    // in-block modifications only applicable to variables with an explicit type,
                    // but existed backing field can lead to the entire body resolution even on
                    // implicit body phase, so we will mark this phase as fully resolved too to be safe
                    if (phase != CfirResolvePhase.BODY_RESOLVE && phase != CfirResolvePhase.IMPLICIT_TYPES) return
                }

                is CfirCodeFragment -> {
                    // in-block modifications only applicable to fully resolved code fragments
                    if (phase != CfirResolvePhase.BODY_RESOLVE) return
                }

                else -> return
            }

            val declaration = element.source?.psi as? CjAnnotated ?: return
            when (declaration) {
                is CjNamedFunction -> {
                    if (declaration.isReanalyzableContainer()) {
                        declaration.hasCfirBody = true
                    }
                }

                is CjProperty -> {
                    if (declaration.isReanalyzableContainer() || declaration.accessors.any(CjPropertyAccessor::isReanalyzableContainer)) {
                        declaration.hasCfirBody = true
                    }
                }

                is CjCodeFragment -> {
                    declaration.hasCfirBody = true
                }
            }
        }
    }
}

/**
 * 寻找 [psi] 所属的、可把局部变更限制在块内重新分析范围的非局部声明。
 *
 * 普通声明通过 [getNonLocalReanalyzableContainingDeclaration] 判断；代码片段没有常规声明容器，
 * 因此允许把包含文件本身作为块内失效锚点。
 */
private fun nonLocalDeclarationForLocalChange(psi: PsiElement): CjElement? {
    return psi.getNonLocalReanalyzableContainingDeclaration() ?: psi.containingFile as? CjCodeFragment
}

/**
 * 低阶 CFIR 对源码修改局部性的内部表示。
 *
 * 该层扩展平台级 [CaSourceModificationLocality]，为可以延迟到写动作结束处理的修改额外携带
 * PSI 锚点、工程与模块信息，使后续失效逻辑无需重新推断变更来源。
 */
private sealed interface LLModificationLocality {
    /**
     * A modification that can be deferred to the next flush point (usually the end of a write action) to avoid excessive processing.
     */
    sealed class Deferrable : LLModificationLocality {
        /**
         * 受本次修改影响的 CangJie PSI 元素。
         *
         * 对块内修改它通常是函数、属性、访问器或代码片段；对空白修改则是距离空白最近的 CangJie PSI 祖先。
         */
        abstract val affectedElement: CjElement

        /**
         * [affectedElement] 所属工程，用于延迟计算模块和访问工程级服务。
         */
        abstract val project: Project

        /**
         * [affectedElement] 对应的项目结构模块。
         *
         * 模块按需惰性计算，保证同一写动作内重复访问时不会多次查询项目结构服务。
         */
        val module: CaModule by lazy(LazyThreadSafetyMode.NONE) {
            CangJieProjectStructureProvider.getModule(project, affectedElement, useSiteModule = null)
        }

        /**
         * 按修改类型和 PSI 锚点合并重复延迟项。
         *
         * 同一个元素上的同类修改只需要处理一次；不同局部性类型不能互相合并，避免空白修改误覆盖块内修改。
         */
        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Deferrable) return false

            return other::class == this::class && other.affectedElement == affectedElement
        }

        /**
         * 基于 PSI 锚点生成哈希值，与 [equals] 中的去重规则保持一致。
         */
        override fun hashCode(): Int = affectedElement.hashCode()
    }

    /**
     * @see CaSourceModificationLocality.Invisible
     */
    object Invisible : LLModificationLocality, CaSourceModificationLocality.Invisible

    /**
     * @see CaSourceModificationLocality.Whitespace
     *
     * @param affectedElement 距离空白或注释变更最近的 CangJie PSI 祖先。
     * @param project [affectedElement] 所属工程。
     */
    class Whitespace(
        /**
         * 距离空白或注释变更最近的 CangJie PSI 祖先。
         */
        override val affectedElement: CjElement,

        /**
         * [affectedElement] 所属工程。
         */
        override val project: Project,
    ) : Deferrable(), CaSourceModificationLocality.Whitespace

    /**
     * @see CaSourceModificationLocality.InBlock
     *
     * @param affectedElement 可按块内修改重新分析的声明或代码片段。
     * @param project [affectedElement] 所属工程。
     */
    class InBlock(
        /**
         * 可按块内修改重新分析的声明或代码片段。
         */
        override val affectedElement: CjElement,

        /**
         * [affectedElement] 所属工程。
         */
        override val project: Project,
    ) : Deferrable(), CaSourceModificationLocality.InBlock

    /**
     * @see CaSourceModificationLocality.OutOfBlock
     */
    object OutOfBlock : LLModificationLocality, CaSourceModificationLocality.OutOfBlock
}

/**
 * On in-block modification, we only have to invalidate a CFIR body if it exists. If the corresponding CFIR element doesn't exist or is in an
 * earlier phase, there is nothing to invalidate.
 *
 * [hasCfirBody] tracks whether a CFIR body exists with user data on the PSI element. This allows us to avoid CFIR building to perform this
 * check.
 *
 * [CjProperty] is used as an anchor for [CjPropertyAccessor]s to avoid extra memory consumption.
 */
private var CjElement.hasCfirBody: Boolean
    get() = when (this) {
        is CjNamedFunction, is CjProperty, is CjCodeFragment -> getUserData(hasCfirBodyKey) == true
        is CjPropertyAccessor -> property.hasCfirBody
        else -> false
    }
    set(value) {
        val declarationAnchor = if (this is CjPropertyAccessor) property else this
        declarationAnchor.putUserData(
            hasCfirBodyKey,
            value.takeIf { it },
        )
    }

/**
 * 标记 PSI 元素是否已经拥有可被块内修改失效的 CFIR body。
 *
 * 使用 nullable 布尔值是为了在 `false` 时直接移除 user data，减少 PSI 上的常驻状态。
 */
private val hasCfirBodyKey = Key.create<Boolean?>("HAS_CFIR_BODY")

/**
 * Covered by org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.AbstractInBlockModificationTest
 * on the compiler side and by
 * org.cangnova.cangjie.idea.cfir.analysis.providers.trackers.AbstractProjectWideOutOfBlockKotlinModificationTrackerTest
 * on the plugin part
 *
 * @return The declaration in which a change of the passed receiver parameter can be treated as in-block modification
 */
internal fun PsiElement.getNonLocalReanalyzableContainingDeclaration(): CjDeclaration? {
    return when (val declaration = getNonLocalContainingOrThisDeclaration()) {
        is CjNamedFunction -> declaration.takeIf { function ->
            function.isReanalyzableContainer() && isElementInsideBody(
                declaration = function,
                child = this,
                canHaveBackingFieldAccess = false,
            )
        }

        is CjPropertyAccessor -> declaration.takeIf { accessor ->
            accessor.isReanalyzableContainer() && isElementInsideBody(
                declaration = accessor,
                child = this,
                canHaveBackingFieldAccess = true,
            )
        }

        is CjProperty -> declaration.takeIf { property ->
            property.isReanalyzableContainer() && property.initializer?.isAncestorOf(this) == true
        }

        else -> null
    }
}

/**
 * # Regular access
 *
 * ```kotlin
 * val i: Int
 *   get() {
 *     field // Depending on the existence of this access, the property will have or not the backing field
 *     return 0
 *   }
 * ```
 *
 * # Leading local declaration
 *
 * ```kotlin
 * val i: Int
 *   get() {
 *     // Also, we cannot just ignore such local declarations existence as they may change the resolution
 *     // of backing field. With this declaration,
 *     // the next `field` access will be resolved into this local property,
 *     // so there will be no any access to the backing field and,
 *     // as the result, there will be no backing field at all
 *     val field = 1
 *     field
 *     return 0
 *   }
 * ```
 *
 * # Implicit receiver
 *
 * ```kotlin
 * class MyClass(val field: String)
 * fun action(block: () -> Unit) {}
 * fun actionWithReceiver(block: MyClass.() -> Unit) {}
 *
 * val prop: Int
 *   get() {
 *     // Here we can safely change `action` to `actionWithReceiver` and vise versa
 *     // as `field` in both cases will be resolved into the backing field
 *     // as it has higher priority than a property from an implicit receiver
 *     action {
 *       field
 *     }
 *
 *     return 0
 *   }
 * ```
 */
private fun PsiElement.potentiallyAffectsPropertyBackingFieldResolution(): Boolean {
    var hasFieldText = false
            this.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is LeafPsiElement && element.textMatches("field")) {
                hasFieldText = true
                stopWalking()
            } else {
                super.visitElement(element)
            }
        }
    })

    return hasFieldText
}

/**
 * 判断 [child] 是否位于 [declaration] 的可块内重新分析 body 范围内。
 *
 * 当 [canHaveBackingFieldAccess] 为 `true` 时，还需要排除可能影响属性 backing field 解析的修改；
 * 这类修改会改变声明结构语义，必须升级为块外修改。
 */
private fun isElementInsideBody(declaration: CjDeclarationWithBody, child: PsiElement, canHaveBackingFieldAccess: Boolean): Boolean {
    val body = declaration.bodyExpression ?: return false
    return when {
        !body.isAncestorOf(child) -> false
        canHaveBackingFieldAccess && child.potentiallyAffectsPropertyBackingFieldResolution() -> false
        else -> true
    }
}

/**
 * 判断命名函数是否具备稳定签名，使函数体变化可以按块内修改处理。
 *
 * 块体函数或显式返回类型函数的 body 修改不会改变对外类型契约，因此可以只失效 body。
 */
private fun CjNamedFunction.isReanalyzableContainer(): Boolean = hasBlockBody() || typeReference != null

/**
 * 判断属性访问器是否具备可局部重分析的边界。
 *
 * setter、块体访问器或所属属性带显式类型时，访问器 body 的修改不会要求重新推断属性对外类型。
 */
private fun CjPropertyAccessor.isReanalyzableContainer(): Boolean = isSetter || hasBlockBody() || property.typeReference != null

/**
 * 判断属性初始化器是否可以按块内修改处理。
 *
 * 只有显式声明类型的属性才能保证初始化器变更不会改变属性暴露类型。
 */
private fun CjProperty.isReanalyzableContainer(): Boolean = typeReference != null

/**
 * Detects the modification locality of [element] and handles the corresponding cache invalidation.
 *
 * This is a convenience function that combines [LLCfirDeclarationModificationService.detectLocality] and
 * [LLCfirDeclarationModificationService.handleInvalidation].
 *
 * The function must be called from a write action.
 *
 * @see LLCfirDeclarationModificationService.detectLocality
 * @see LLCfirDeclarationModificationService.handleInvalidation
 */
@LLCfirInternals
fun LLCfirDeclarationModificationService.handleElementModification(element: PsiElement, modificationType: CaElementModificationType) {
    val modificationLocality = detectLocality(element, modificationType)
    handleInvalidation(element, modificationLocality)
}
