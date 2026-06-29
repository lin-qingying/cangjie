package org.cangnova.cangjie.cfir.diagnostics.rendering

import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactoryToRendererMap
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticRenderer

/**
 * 根据诊断对象选择渲染器的工厂接口。
 */
fun interface DiagnosticRendererFactory {
    /**
     * 返回指定诊断对应的渲染器。
     */
    operator fun invoke(diagnostic: CjDiagnostic): CjDiagnosticRenderer?
}

/**
 * 基于诊断工厂到渲染器映射的渲染器工厂基类。
 */
abstract class BaseDiagnosticRendererFactory : DiagnosticRendererFactory {
    /**
     * 根据诊断工厂从映射中查找渲染器。
     */
    override operator fun invoke(diagnostic: CjDiagnostic): CjDiagnosticRenderer? {
        val factory = diagnostic.factory
        @Suppress("UNCHECKED_CAST")
        return MAP[factory]
    }

    /**
     * 当前渲染器工厂持有的工厂到渲染器映射。
     */
    abstract val MAP: CjDiagnosticFactoryToRendererMap
}

/**
 * 无源码诊断渲染器工厂基类。
 */
abstract class BaseSourcelessDiagnosticRendererFactory : BaseDiagnosticRendererFactory() {
    companion object {
        /**
         * 无源码诊断消息参数占位符。
         */
        const val MESSAGE_PLACEHOLDER: String = "{0}"
    }
}

