/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.util.withPsiEntry
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.withConeTypeEntry
import org.cangnova.cangjie.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.buildErrorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun errorWithCfirSpecificEntries(
    message: String,
    cause: Exception? = null,
    cfir: CfirElement? = null,
    coneType: ConeCangJieType? = null,
    psi: PsiElement? = null,
    additionalInfos: ExceptionAttachmentBuilder.() -> Unit = {},
): Nothing {
    throw buildErrorWithCfirSpecificEntries(message, cause, cfir, coneType, psi, additionalInfos)
}

fun buildErrorWithCfirSpecificEntries(
    message: String,
    cause: Exception? = null,
    cfir: CfirElement? = null,
    coneType: ConeCangJieType? = null,
    psi: PsiElement? = null,
    additionalInfos: ExceptionAttachmentBuilder.() -> Unit = {},
): Throwable =
    buildErrorWithAttachment(message, cause) {
        if (cfir != null) {
            withCfirEntry("cfir", cfir)
        }

        if (psi != null) {
            withPsiEntry("psi", psi, CangJieProjectStructureProvider.getModule(psi.project, psi, useSiteModule = null))
        }

        if (coneType != null) {
            withConeTypeEntry("coneType", coneType)
        }
        additionalInfos()
    }

@OptIn(ExperimentalContracts::class)
inline fun <reified R> Any.requireTypeIntersectionWith() {
    contract { returns() implies (this@requireTypeIntersectionWith is R) }

    requireWithAttachment(
        this is R,
        { "${this::class.simpleName} must be ${R::class.simpleName}" },
    ) {
        if (this@requireTypeIntersectionWith is CfirElement) {
            withCfirEntry("container", this@requireTypeIntersectionWith)
        }
    }
}
