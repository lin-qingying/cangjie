package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.restoreCurrentCompiledPsi
import org.cangnova.cangjie.analysis.api.impl.base.symbols.pointers.CaBasePsiSymbolPointer
import org.cangnova.cangjie.analysis.api.impl.base.util.lazyPub
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbolOfType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbolOrUnresolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirOfType
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.CfirResolution
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.CfirUnresolvedSymbolException
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.UnresolvedReason
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.getOrThrow
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.map
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjAnnotated
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjDeclarationWithBody
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjTypeParameterListOwner
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass

/**
 * A [CaCfirSymbol] that is possibly backed by some [PsiElement] and builds [cfirSymbol] lazily (by convention),
 * allowing some variables to be calculated without the need to build a [cfirSymbol].
 */
internal interface CaCfirPsiSymbol<out P : PsiElement, out S : CfirBasedSymbol<*>> : CaCfirSymbol<S> {
    /**
     * The [PsiElement] which can be used as a source of truth for some other property implementations.
     *
     * It can be as an element from a source file, or an element from a library.
     */
    val backingPsi: P?

    /**
     * The lazy implementation of [CfirBasedSymbol].
     *
     * The implementation is either built on top of [backingPsi] or provided during creation.
     *
     * @see cfirSymbol
     */
    val lazyCfirSymbol: Lazy<S>

    /**
     * The origin should be provided without using [cfirSymbol], if possible.
     */
    abstract override val origin: CaSymbolOrigin

    /**
     * 触发并返回懒加载的底层 CFIR 符号。
     */
    override val cfirSymbol: S get() = lazyCfirSymbol.value

    /**
     * 触发懒加载的底层 CFIR 符号；符号不可恢复时返回 `null` 而不是抛异常。
     *
     * 这是 Analysis API 的**符号层降级点**：声明可能在当前会话不可见、反编译 PSI 可能失效、
     * 局部声明没有全局身份——这些都不是编程错误。属性实现应优先使用本入口，
     * 以便在符号缺失时返回错误语义值（error type / 空集合），而不是让整次分析失败。
     *
     * 展示层仍可在此基础上做候选粒度降级（例如补全丢弃单个不可渲染的候选）：
     * 两层职责不同——这里负责「符号不可用不等于异常」，那里负责「一个候选坏了不影响整批」。
     *
     * 缺失结论由 [MemoizedResolutionLazy] 记忆化，本方法的 catch 命中的是已缓存结论，
     * 不会重复执行整条恢复链。
     *
     * 与 Kotlin 的偏离（显式记录）：Kotlin `KaFirSymbol.firSymbol` 只有**一个非空入口**，
     * 因为 FIR 由 PSI 构建、每个声明必有 `FirSymbol`（`KaFirSymbol.kt:37`、`KaFirPsiSymbol.kt:77`），
     * 镜像里既没有 `FirErrorDeclarationSymbol` 也没有可空的 `firSymbol`。CFIR 侧不同：
     * 库声明可能尚未进入索引、反编译 PSI 可能失效，恢复会**真的失败**。
     * 因此按 `CLAUDE.md` 的「无 Kotlin 对应物时新增 Cfir API 并记录偏离」，
     * 在 [CaCfirSymbol] 上提供唯一的可空入口（本接口覆写它），
     * 底层唯一实现仍是返回 [CfirResolution] 的 `resolveToCfirSymbolOrUnresolved`。
     */
    override val cfirSymbolOrNull: S?
        get() = try {
            lazyCfirSymbol.value
        } catch (failure: CfirUnresolvedSymbolException) {
            failure.diagnostics.log(LOG, failure.reason)
            null
        }
}

/**
 * 当前 use-site 不是 library source session 时执行指定动作。
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <R> CaCfirPsiSymbol<*, *>.ifNotLibrarySource(action: () -> R): R? {
    contract {
        callsInPlace(action, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }

    return if (analysisSession.useSiteModule is CaLibrarySourceModule) null else action()
}

/**
 * 当前公开符号来源为源码时执行指定动作。
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <R> CaCfirPsiSymbol<*, *>.ifSource(action: () -> R): R? {
    contract {
        callsInPlace(action, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }

    return if (origin == CaSymbolOrigin.SOURCE) action() else null
}

/**
 * 从源码 callable PSI 直接创建公开 value parameter 符号列表。
 */
internal fun CaCfirCjBasedSymbol<CjCallableDeclaration, *>.createCaValueParameters(): List<CaValueParameterSymbol>? =
    ifNotLibrarySource {
        with(analysisSession) {
            backingPsi?.valueParameters?.map { it.symbol as CaValueParameterSymbol }
        }
    }

/**
 * 从源码类型参数 owner PSI 直接创建公开 type parameter 符号列表。
 */
internal fun CaCfirCjBasedSymbol<CjTypeParameterListOwner, *>.createCaTypeParameters(): List<CaTypeParameterSymbol>? =
    ifNotLibrarySource {
        with(analysisSession) {
            backingPsi?.typeParameters?.map { it.symbol }
        }
    }

/**
 * 由仓颉 PSI 元素承载的 CFIR 符号协议。
 */
internal interface CaCfirCjBasedSymbol<out P : CjElement, out S : CfirBasedSymbol<*>> : CaCfirPsiSymbol<P, S> {
    /**
     * 优先根据 PSI 来源推导公开符号 origin。
     */
    override val origin: CaSymbolOrigin get() = withValidityAssertion { psiOrSymbolOrigin() }
}

/**
 * 为声明 PSI 构造延迟解析 CFIR 符号的包装。
 *
 * 缓存的是**恢复结果**而不是符号本身：`Lazy<S>` 的初始化一旦抛异常，Kotlin 的 `lazy`
 * 不会写入缓存，于是每次访问都会把整条恢复链（含 `getOrBuildCfir` 级别的解析）重跑一遍，
 * 并重复构造异常、重复打日志。把 [CfirResolution] 当成正常值 memoize 之后，
 * 缺失只计算一次，[CaCfirPsiSymbol.cfirSymbolOrNull] 的 catch 命中的是已缓存的结论。
 */
internal inline fun <reified S : CfirBasedSymbol<*>> lazyCfirSymbol(
    declaration: CjDeclaration,
    session: CaCfirSession,
): Lazy<S> = memoizedResolutionLazy {
    declaration.resolveToCfirSymbolOrUnresolved(session.resolutionFacade).map { symbol ->
        symbol as? S ?: error(
            "Declaration ${declaration::class.simpleName} is resolved to " +
                "${symbol::class.simpleName} instead of ${S::class.simpleName}",
        )
    }
}

/**
 * 记忆化「符号恢复结果」的 [Lazy] 实现。
 *
 * 记忆化必须区分两类缺失——这正是 [UnresolvedReason.mayRecoverLater] 的用途：
 *
 * - **可恢复**（索引未就绪、反编译 PSI 待重建）：结论只在 [recoverableMissTtlNanos] 内有效，
 *   到期后重算。若永久记忆，一次「索引还没准备好」就会被冻结成整个会话的缺失
 *   （session 并不因 dumb 模式结束而重建）；若完全不记忆，补全里一个形参的多次属性访问
 *   就会反复跑整条恢复链。
 * - **不可恢复**（恢复链未覆盖的声明形态）：这是代码缺陷，永久记忆避免重复计算与重复告警。
 *
 * 成功结果同样永久记忆。**两类缺失都进入记忆化**：可恢复缺失靠 TTL 到期重算，
 * 而不是靠「不写入」重算——后者会让 TTL 分支永远走不到，等于每秒重算十几次。
 */
private class MemoizedResolutionLazy<S : CfirBasedSymbol<*>>(
    private val compute: () -> CfirResolution<S>,
    private val recoverableMissTtlNanos: Long = RECOVERABLE_MISS_TTL_NANOS,
) : Lazy<S> {
    private val lock = Any()

    @Volatile
    private var settled: CfirResolution<S>? = null

    @Volatile
    private var settledAtNanos: Long = 0L

    override val value: S
        get() = currentResolution().getOrThrow()

    override fun isInitialized(): Boolean = settled != null

    /**
     * 取当前仍然有效的结论；可恢复缺失在 TTL 到期后重新计算。
     */
    private fun currentResolution(): CfirResolution<S> {
        freshSettlement()?.let { return it }

        synchronized(lock) {
            freshSettlement()?.let { return it }

            val computed = compute()
            // 所有结论都记忆化：可恢复缺失靠 [freshSettlement] 的 TTL 到期重算，
            // 而不是靠「不记忆」来重算。后者会让 TTL 变成死代码——每次属性访问都重跑
            // 整条恢复链（补全渲染一次可能访问十几次），正是本类要消除的形态。
            settledAtNanos = System.nanoTime()
            settled = computed
            return computed
        }
    }

    /**
     * 已记忆化且未过期的结论。
     */
    private fun freshSettlement(): CfirResolution<S>? {
        val current = settled ?: return null
        if (current is CfirResolution.Unresolved && current.reason.mayRecoverLater) {
            val age = System.nanoTime() - settledAtNanos
            if (age >= recoverableMissTtlNanos) return null
        }
        return current
    }

    private companion object {
        /**
         * 可恢复缺失的记忆化窗口。
         *
         * 取值目标是「既不把缺失冻结到会话结束，也不让一次补全反复重算」：
         * 库索引就绪通常在百毫秒级，500ms 既能吸收补全期间的重复访问，
         * 又能让索引就绪后的下一次查询立刻恢复。
         */
        val RECOVERABLE_MISS_TTL_NANOS: Long = java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(500)
    }
}

/**
 * 用 [MemoizedResolutionLazy] 把恢复结果包装成 [Lazy]。
 */
internal fun <S : CfirBasedSymbol<*>> memoizedResolutionLazy(compute: () -> CfirResolution<S>): Lazy<S> =
    MemoizedResolutionLazy(compute)

/**
 * 为普通 PSI 元素构造延迟提取 CFIR 符号的包装。
 */
internal inline fun <reified E : CfirElement, reified S : CfirBasedSymbol<*>> lazyCfirSymbol(
    element: CjElement,
    session: CaCfirSession,
    crossinline symbol: (E) -> S,
): Lazy<S> = lazyPub {
    symbol(element.getOrBuildCfirOfType<E>(session.resolutionFacade))
}

/**
 * 统一把符号缓存里的 compiled PSI 恢复到当前 live PSI。
 *
 * decompiled `.cjo` 在 IDE 生命周期里可能因为 view provider / document 重建而失效，
 * 但 Analysis API / CFIR 缓存仍会暂时持有旧的 `backingPsi`。
 * 任何公开 `symbol.psi` 的出口都必须先经过这里，避免把陈旧 offset 的 compiled PSI
 * 再次泄漏给导航、文档和 target extraction。
 */
internal inline fun CaCfirPsiSymbol<out PsiElement, *>.backingPsiOrFindCurrentPsi(
    findPsi: () -> PsiElement?,
): PsiElement? {
    val currentBackingPsi = when (val psi = backingPsi) {
        is CjElement -> psi.restoreCurrentCompiledPsi(analysisSession.project)
        else -> psi
    }

    return currentBackingPsi ?: findPsi()
}

/**
 * 按 PSI 优先、CFIR 符号兜底的方式计算符号 hashCode。
 *
 * 等值与哈希必须是**全函数**：符号不可恢复时 `cfirSymbol` 会抛异常，若这里直接透传，
 * 一个不可用符号进入任何 HashSet/HashMap（缓存、去重、作用域名集合）都会在哈希阶段炸掉，
 * 把「符号缺失」升级成「容器操作失败」。因此缺失时退回对象身份哈希：
 * 与 [psiOrSymbolEquals] 的判定保持契约一致（不等的对象允许哈希碰撞，
 * 同一 PSI 对象的两侧哈希仍同源）。
 */
internal fun CaCfirPsiSymbol<*, *>.psiOrSymbolHashCode(): Int =
    backingPsi?.hashCode() ?: cfirSymbolOrNull?.hashCode() ?: System.identityHashCode(this)

/**
 * 按 PSI 身份和 CFIR 符号身份组合判断两个公开符号是否相等。
 *
 * 契约要求等值判断是**全函数**：符号不可恢复时 `cfirSymbol` 会抛异常，若直接比较，
 * 一次等值判断就能把「符号缺失」升级成「容器操作失败」。
 *
 * 缺失时的判定规则是「不猜相等」：没有 PSI 也没有符号就没有可比对的声明身份，
 * 此时返回 `false`——把两个不同声明折叠成一个，比多出一条候选危险得多；
 * 唯一的例外是两侧确是同一个库 PSI 对象（同一个反编译声明），那本身就足以证明相等。
 */
internal fun CaCfirPsiSymbol<*, *>.psiOrSymbolEquals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || other::class != this::class) return false

    val otherSymbol = other as CaCfirPsiSymbol<*, *>
    val backingPsi = backingPsi
    val otherBackingPsi = otherSymbol.backingPsi
    return when {
        backingPsi == null && otherBackingPsi == null -> sameSymbolIdentity(otherSymbol)
        backingPsi !== otherBackingPsi -> false
        backingPsi !is CjElement -> sameSymbolIdentity(otherSymbol)
        !backingPsi.cameFromCangJieLibrary -> true
        // 两侧是同一个库 PSI 对象：能比较符号时比较符号，符号不可恢复时对象身份本身
        // 就足以证明相等（哈希同源为 backingPsi，契约仍然成立）。
        else -> cfirSymbolOrNull?.let { identity -> identity == otherSymbol.cfirSymbolOrNull } ?: true
    }
}

/**
 * 两个符号的底层 CFIR 符号身份是否相同；任一侧不可恢复时视为不同。
 */
private fun CaCfirPsiSymbol<*, *>.sameSymbolIdentity(other: CaCfirPsiSymbol<*, *>): Boolean {
    val identity = cfirSymbolOrNull ?: return false
    return identity == other.cfirSymbolOrNull
}

/**
 * 当前编译文件的来源身份会随仓库 [LLCfirSessionCache.getSession] 的 `preferBinary` 参数变化，
 * 因而同一反编译文件可能被表示为库来源或非库来源。
 *
 * So, depending on it, we may represent one decompiled file as [CaSymbolOrigin.SOURCE] and as [CaSymbolOrigin.LIBRARY]
 * at the same time.
 */
internal fun <P : CjElement> CaCfirPsiSymbol<P, *>.psiOrSymbolOrigin(): CaSymbolOrigin {
    val backingPsi = backingPsi
    return when {
        backingPsi == null -> symbolOrigin()
        backingPsi.cameFromCangJieLibrary -> symbolOrigin()
        else -> CaSymbolOrigin.SOURCE
    }
}

/**
 * 为由注解 PSI 承载的符号创建公开注解列表。
 */
internal fun CaCfirCjBasedSymbol<CjAnnotated, *>.psiOrSymbolAnnotationList(): CaAnnotationList {
    return CaCfirAnnotationListForDeclaration.create(cfirSymbol, builder)
}

/**
 * 判断仓颉 PSI 元素是否来自已编译库文件。
 */
internal val CjElement.cameFromCangJieLibrary: Boolean get() = containingCjFile.isCompiled
/**
 * 在允许使用 PSI 的 origin 下返回 CFIR 符号对应的真实 PSI。
 */
internal val CfirBasedSymbol<*>.backingPsiIfApplicable: PsiElement?
    get() {
        if (origin == CfirDeclarationOrigin.Synthetic.TypeAliasConstructor) return null

        return cfir.realPsi
    }


/**
 * 为函数体声明创建公开返回类型。
 *
 * 省略返回类型在仓颉表示“由函数体推导”，必须读 CFIR 已解析的返回类型。这里不能沿用 Kotlin
 * “block body 即 Unit”的假设，否则省略返回类型的函数在公开符号层一律塌成 Unit，与同一次
 * 分析里 `expressionType` 读到的调用点类型自相矛盾。
 */
internal fun CaCfirCjBasedSymbol<CjDeclarationWithBody, CfirCallableSymbol<*>>.createReturnType(): CaType =
    cfirSymbol.returnType(builder)

/**
 * callable 的 override 标记在源码 PSI 可直接判定。
 *
 * 对齐 Kotlin `isOverrideWithWorkaround` 的职责边界：source PSI 不为普通状态位
 * 强制恢复 CFIR；没有 source PSI 时才读取 CFIR 状态。
 */
internal val CaCfirCjBasedSymbol<CjCallableDeclaration, CfirCallableSymbol<*>>.isOverrideWithWorkaround: Boolean
    get() {
        val sourcePsi = ifSource { backingPsi }
        return sourcePsi?.hasModifier(CjTokens.OVERRIDE_KEYWORD) ?: cfirSymbol.rawStatus.isOverride
    }

/**
 * 为 source PSI 符号创建指定公开符号类型的 PSI-based pointer。
 */
internal inline fun <reified S : CaSymbol> CaCfirPsiSymbol<out CjElement, *>.psiBasedSymbolPointerOfTypeIfSource(
    noinline restoreSymbolByPsi: org.cangnova.cangjie.analysis.api.CaSession.(CjElement) -> CaSymbol?,
): CaSymbolPointer<S>? {
    return psiBasedSymbolPointerOfTypeIfSource(S::class, restoreSymbolByPsi)
}

@OptIn(CaImplementationDetail::class)
/**
 * 为 source PSI 符号创建带运行时类型校验的 PSI-based pointer。
 */
internal fun <S : CaSymbol> CaCfirPsiSymbol<out CjElement, *>.psiBasedSymbolPointerOfTypeIfSource(
    expectedClass: KClass<S>,
    restoreSymbolByPsi: org.cangnova.cangjie.analysis.api.CaSession.(CjElement) -> CaSymbol?,
): CaSymbolPointer<S>? {
    val symbol = this as? S ?: return null
    return ifSource {
        CaBasePsiSymbolPointer.createForSymbolFromSource(
            symbol = symbol,
            expectedClass = expectedClass,
            restoreSymbolByPsi = restoreSymbolByPsi,
        )
    }
}

/**
 * 符号恢复失败时的日志对象。
 *
 * 缺失按原因分级：可预期的缺失走 debug，来源/PSI 生命周期问题走 warn。
 */
private val LOG = Logger.getInstance("org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPsiSymbol")
