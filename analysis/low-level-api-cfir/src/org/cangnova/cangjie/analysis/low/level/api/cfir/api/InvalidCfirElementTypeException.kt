/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api

import org.cangnova.cangjie.analysis.api.util.withPsiEntry
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.withCfirSymbolEntry
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.withConeTypeEntry
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.utils.exceptions.CangJieIllegalArgumentExceptionWithAttachments
import org.cangnova.cangjie.utils.exceptions.buildAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry
import java.util.Locale
import kotlin.reflect.KClass

class InvalidCfirElementTypeException(
    actualCfirElement: Any?,
    cjElement: CjElement?,
    expectedCfirClasses: List<KClass<*>>,
) : CangJieIllegalArgumentExceptionWithAttachments("") {
    init {
        buildAttachment {
            when (actualCfirElement) {
                is CfirElement -> withCfirEntry("cfirElement", actualCfirElement)
                is CfirBasedSymbol<*> -> withCfirSymbolEntry("cfirSymbol", actualCfirElement)
                is ConeCangJieType -> withConeTypeEntry("coneType", actualCfirElement)
                null -> {}
                else -> withEntry("element", actualCfirElement) { it.toString() }
            }

            cjElement?.let {
                withPsiEntry("cjElement", cjElement)
            }
        }
    }

    override val message: String = buildString {
        cjElement?.let {
            "For ${cjElement::class.simpleName}, "
        }

        val message = when (expectedCfirClasses.size) {
            0 -> "Unexpected element of type:"
            1 -> "The element of type ${expectedCfirClasses.single()} expected, but"
            else -> "One of [${expectedCfirClasses.joinToString()}] element types expected, but"
        }

        append(if (cjElement == null) message else message.replaceFirstChar { it.lowercase(Locale.getDefault()) })
        if (actualCfirElement != null) {
            append(" ${actualCfirElement::class.simpleName} found")
        } else {
            append(" no element found")
        }
    }
}


fun throwUnexpectedCfirElementError(
    cfirElement: Any?,
    cjElement: CjElement? = null,
    vararg expectedCfirClasses: KClass<*>
): Nothing {
    throw InvalidCfirElementTypeException(cfirElement, cjElement, expectedCfirClasses.toList())
}
