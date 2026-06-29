/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.util.forEachDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * In opposite to [NonLocalAnnotationVisitor] processes not only the target declaration,
 * but also its nested declarations.
 *
 * @see NonLocalAnnotationVisitor
 */
internal abstract class RecursiveNonLocalAnnotationVisitor<T> : NonLocalAnnotationVisitor<T>() {
    /**
     * 访问文件级注解后继续递归处理文件中的所有顶层声明。
     */
    override fun visitFile(file: CfirFile, data: T) {
        super.visitFile(file, data)

        file.forEachDeclaration { it.accept(this, data) }
    }

    /**
     * 访问类声明自身注解后继续递归处理类中的嵌套声明。
     */
    override fun visitClass(klass: CfirClass, data: T) {
        super.visitClass(klass, data)

        klass.forEachDeclaration { it.accept(this, data) }
    }
}
