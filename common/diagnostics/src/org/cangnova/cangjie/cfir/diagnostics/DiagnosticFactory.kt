/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.DiagnosticRenderer
import java.lang.IllegalArgumentException

/**
 * 旧式 unbound diagnostic 工厂基类。
 */
abstract class DiagnosticFactory<D : UnboundDiagnostic> protected constructor(
    /**
     * 可延迟初始化的诊断工厂名称。
     */
    private var _name: String?,
    /**
     * 诊断默认严重级别。
     */
    open val severity: Severity
) {
    /**
     * 诊断工厂名称。
     */
    open val name: String
        get() = _name!!

    /**
     * 初始化诊断工厂名称。
     */
    fun initializeName(name: String) {
        _name = name
    }

    /**
     * 该工厂的默认诊断渲染器。
     */
    open var defaultRenderer: DiagnosticRenderer<D>? = null

    protected constructor(severity: Severity) : this(null, severity)

    /**
     * 初始化默认渲染器并转换为当前诊断类型。
     */
    @Suppress("UNCHECKED_CAST")
    fun initDefaultRenderer(defaultRenderer: DiagnosticRenderer<*>?) {
        this.defaultRenderer = defaultRenderer as DiagnosticRenderer<D>?
    }

    /**
     * 校验诊断由当前工厂创建并转换为具体诊断类型。
     */
    fun cast(diagnostic: UnboundDiagnostic): D {
        require(!(diagnostic.factory !== this)) { "Factory mismatch: expected " + this + " but was " + diagnostic.factory }
        @Suppress("UNCHECKED_CAST")
        return diagnostic as D
    }

    /**
     * 返回诊断工厂名称或匿名占位。
     */
    override fun toString(): String {
        return _name ?: "<Anonymous DiagnosticFactory>"
    }

    companion object {
        /**
         * 在多个候选工厂中查找匹配工厂并转换诊断。
         */
        @SafeVarargs
        fun <D : UnboundDiagnostic> cast(diagnostic: UnboundDiagnostic, vararg factories: DiagnosticFactory<out D>): D {
            return cast(diagnostic, listOf(*factories))
        }

        /**
         * 在候选工厂集合中查找匹配工厂并转换诊断。
         */
        fun <D : UnboundDiagnostic> cast(diagnostic: UnboundDiagnostic, factories: Collection<DiagnosticFactory<out D>>): D {
            for (factory in factories) {
                if (diagnostic.factory === factory) return factory.cast(diagnostic)
            }
            throw IllegalArgumentException("Factory mismatch: expected one of " + factories + " but was " + diagnostic.factory)
        }
    }
}
