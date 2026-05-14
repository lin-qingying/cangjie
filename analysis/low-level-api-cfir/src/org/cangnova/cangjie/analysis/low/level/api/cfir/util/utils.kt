/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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

@Suppress("NOTHING_TO_INLINE")
internal inline fun checkCanceled() {
    ProgressManager.checkCanceled()
}

internal val CfirElement.isErrorElement
    get() = this is CfirDiagnosticHolder

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

internal val CfirDeclaration.containingCjFileIfAny: CjFile?
    get() = psi?.containingFile as? CjFile



internal fun CjDeclaration.isNonAnonymousClassOrObject() =
    this is CjTypeStatement
