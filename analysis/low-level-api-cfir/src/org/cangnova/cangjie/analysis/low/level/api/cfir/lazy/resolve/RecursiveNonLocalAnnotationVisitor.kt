/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.util.forEachDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirRegularClass

/**
 * In opposite to [NonLocalAnnotationVisitor] processes not only the target declaration,
 * but also its nested declarations.
 *
 * @see NonLocalAnnotationVisitor
 */
internal abstract class RecursiveNonLocalAnnotationVisitor<T> : NonLocalAnnotationVisitor<T>() {
    override fun visitFile(file: CfirFile, data: T) {
        super.visitFile(file, data)

        file.forEachDeclaration { it.accept(this, data) }
    }

    override fun visitRegularClass(regularClass: CfirRegularClass, data: T) {
        super.visitRegularClass(regularClass, data)

        regularClass.forEachDeclaration { it.accept(this, data) }
    }
}
