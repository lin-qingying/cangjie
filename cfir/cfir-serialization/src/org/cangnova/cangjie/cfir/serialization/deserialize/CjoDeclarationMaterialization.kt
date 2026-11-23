package org.cangnova.cangjie.cfir.serialization.deserialize

import java.util.ArrayDeque
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** 非法 wire 的失败保留包、索引和读取模式，失败记录不能被当作完成的声明再次发布。 */
class CjoDeclarationDeserializationException(
    val packageName: String,
    val declarationIndex: Int,
    val mode: CjoDeserializationMode,
    cause: Throwable,
) : IllegalStateException("Invalid CJO declaration $packageName[$declarationIndex] ($mode)", cause)

/** 状态中的 Header 是已经绑定真实声明的规范记录，绝不创建临时符号。 */
internal sealed interface CjoDeclarationCacheEntry {
    data object Building : CjoDeclarationCacheEntry
    data class Header(val record: CfirDeserializedDeclaration) : CjoDeclarationCacheEntry
    data class Ready(val record: CfirDeserializedDeclaration) : CjoDeclarationCacheEntry
    data class Failed(val failure: CjoDeclarationDeserializationException) : CjoDeclarationCacheEntry
}

/** 表达式池里的匿名函数同样先绑定签名，再读取正文；正文递归与 FullId 签名重入分别处理。 */
internal sealed interface CjoAnonymousFunctionCacheEntry {
    data object Building : CjoAnonymousFunctionCacheEntry
    data class Header(val function: org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction) : CjoAnonymousFunctionCacheEntry
    data class BodyBuilding(val function: org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction) : CjoAnonymousFunctionCacheEntry
    data class BodyBuilt(val function: org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction) : CjoAnonymousFunctionCacheEntry
    data class Ready(val function: org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction) : CjoAnonymousFunctionCacheEntry
    data class Failed(val failure: Throwable) : CjoAnonymousFunctionCacheEntry
}

/**
 * Kotlin createCacheWithPostCompute 的包级对应：先建立并绑定签名，再执行成员、注解与 initializer。
 * 仓颉 FullId 跨模块递归需要 metadata 图的共同事务；模块独立锁会在 A→B / B→A 时死锁。
 * 外部声明读取只在整次完成事务提交后返回；表达式内部只能取得已绑定的 header symbol。
 */
class CjoDeclarationMaterialization {
    val lock = ReentrantLock()
    private var active = false
    private val postComputations = ArrayDeque<() -> Unit>()
    private val publications = mutableListOf<() -> Unit>()
    private val failures = mutableListOf<(Throwable) -> Unit>()

    fun <T> read(action: () -> T): T = lock.withLock {
        if (active) return@withLock action()
        active = true
        try {
            val value = action()
            while (postComputations.isNotEmpty()) postComputations.removeFirst().invoke()
            publications.forEach { it() }
            value
        } catch (failure: Throwable) {
            failures.forEach { it(failure) }
            throw failure
        } finally {
            postComputations.clear()
            publications.clear()
            failures.clear()
            active = false
        }
    }

    fun postCompute(action: () -> Unit) {
        check(lock.isHeldByCurrentThread && active)
        postComputations.addLast(action)
    }

    fun onCompletion(publish: () -> Unit, fail: (Throwable) -> Unit) {
        check(lock.isHeldByCurrentThread && active)
        publications += publish
        failures += fail
    }

    /** provider 的成功缓存也只在完整事务提交时发布，失败时不留下 header symbol。 */
    fun publishWhenComplete(action: () -> Unit) {
        check(lock.isHeldByCurrentThread && active)
        publications += action
    }
}
