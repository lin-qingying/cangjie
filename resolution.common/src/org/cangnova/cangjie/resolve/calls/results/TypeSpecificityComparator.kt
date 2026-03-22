/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.resolve.calls.results

import org.cangnova.cangjie.type.model.*


@DefaultImplementation(impl = TypeSpecificityComparator.NONE::class)
interface TypeSpecificityComparator : PlatformSpecificExtension<TypeSpecificityComparator> {
    fun isDefinitelyLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean

    object NONE : TypeSpecificityComparator {
        override fun isDefinitelyLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker) = false
    }

    class Composed(val comparators: List<TypeSpecificityComparator>) : TypeSpecificityComparator {
        override fun isDefinitelyLessSpecific(specific: CangJieTypeMarker, general: CangJieTypeMarker): Boolean {
            return comparators.any { it.isDefinitelyLessSpecific(specific, general) }
        }
    }
}
