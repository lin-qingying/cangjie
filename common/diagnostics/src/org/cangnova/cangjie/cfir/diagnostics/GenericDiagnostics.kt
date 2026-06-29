/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.diagnostics

/**
 * 一组同类诊断的通用集合接口。
 */
interface GenericDiagnostics<T : UnboundDiagnostic> : Iterable<T> {
    /**
     * 返回全部诊断。
     */
    fun all(): Collection<T>

    /**
     * 判断集合是否为空。
     */
    fun isEmpty(): Boolean = all().isEmpty()

    /**
     * 返回全部诊断的迭代器。
     */
    override fun iterator(): Iterator<T> = all().iterator()
}
