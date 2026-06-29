

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.openapi.progress.ProgressManager
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder.LLCfirLockProvider
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock

/**
 * 在等待 [Lock] 时周期性检查取消，并在成功加锁后执行 [action]。
 */
internal inline fun <T> Lock.lockWithPCECheck(action: () -> T): T {
    while (true) {
        checkCanceled()
        if (tryLock(LLCfirLockProvider.lockingInterval, TimeUnit.MILLISECONDS)) {
            try {
                checkCanceled()
                return action()
            } finally {
                unlock()
            }
        }
    }
}

/**
 * 执行 IntelliJ 平台的取消检查。
 */
@Suppress("NOTHING_TO_INLINE")
internal inline fun checkCanceled() {
    ProgressManager.checkCanceled()
}

/**
 * 判断 CFIR 元素是否携带诊断 holder，通常表示错误元素。
 */
internal val CfirElement.isErrorElement
    get() = this is CfirDiagnosticHolder

/**
 * 返回 CFIR 声明对应的仓颉 PSI 声明。
 */
internal val CfirDeclaration.cjDeclaration: CjDeclaration
    get() {
        val psi = psi
            ?: errorWithCfirSpecificEntries("PSI element was not found", cfir = this)
        return when (psi) {
            is CjDeclaration -> psi
            else -> errorWithCfirSpecificEntries(
                "CfirDeclaration.psi (${this::class.simpleName}) should be CjDeclaration but was ${psi::class.simpleName}",
                cfir = this,
                psi = psi,
            )
        }
    }

/**
 * 返回 CFIR 声明所在的仓颉文件；无 PSI 或不是仓颉文件时返回 `null`。
 */
internal val CfirDeclaration.containingCjFileIfAny: CjFile?
    get() = psi?.containingFile as? CjFile



/**
 * 判断 PSI 声明是否是非匿名 class-like 声明。
 */
internal fun CjDeclaration.isNonAnonymousClassOrObject() =
    this is CjTypeStatement
