/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.cangnova.cangjie.analysis.api.utils.errors.withPsiEntry
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.types.ConeKotlinType
import org.cangnova.cangjie.cfir.utils.exceptions.withConeTypeEntry
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.buildErrorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun errorWithCfirSpecificEntries(
    message: String,
    cause: Exception? = null,
    fir: CfirElement? = null,
    coneType: ConeKotlinType? = null,
    psi: PsiElement? = null,
    additionalInfos: ExceptionAttachmentBuilder.() -> Unit = {},
): Nothing {
    throw buildErrorWithCfirSpecificEntries(message, cause, fir, coneType, psi, additionalInfos)
}

fun buildErrorWithCfirSpecificEntries(
    message: String,
    cause: Exception? = null,
    fir: CfirElement? = null,
    coneType: ConeKotlinType? = null,
    psi: PsiElement? = null,
    additionalInfos: ExceptionAttachmentBuilder.() -> Unit = {},
): Throwable =
    buildErrorWithAttachment(message, cause) {
        if (fir != null) {
            withCfirEntry("fir", fir)
        }

        if (psi != null) {
            withPsiEntry("psi", psi, KotlinProjectStructureProvider.getModule(psi.project, psi, useSiteModule = null))
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
