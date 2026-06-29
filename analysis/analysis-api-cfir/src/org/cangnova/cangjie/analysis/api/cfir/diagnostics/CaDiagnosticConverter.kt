package org.cangnova.cangjie.analysis.api.cfir.diagnostics

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory0
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory1
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory2
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory3
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory4
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactoryN
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters1
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters2
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters3
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithParameters4
import org.cangnova.cangjie.cfir.diagnostics.CjSimpleDiagnostic

/**
 * CFIR 诊断工厂到公开 typed diagnostic 的创建器标记接口。
 */
internal interface CaCfirDiagnosticCreator

/**
 * 无参数 CFIR 诊断的公开诊断创建器。
 */
internal fun interface CaCfirDiagnostic0Creator : CaCfirDiagnosticCreator {
    /**
     * 在当前 CFIR Analysis API 会话中创建公开诊断。
     */
    fun CaCfirSession.create(diagnostic: CjSimpleDiagnostic): CaCfirDiagnostic<*>
}

/**
 * 单参数 CFIR 诊断的公开诊断创建器。
 */
internal fun interface CaCfirDiagnostic1Creator<A> : CaCfirDiagnosticCreator {
    /**
     * 在当前 CFIR Analysis API 会话中创建公开诊断。
     */
    fun CaCfirSession.create(diagnostic: CjDiagnosticWithParameters1<A>): CaCfirDiagnostic<*>
}

/**
 * 双参数 CFIR 诊断的公开诊断创建器。
 */
internal fun interface CaCfirDiagnostic2Creator<A, B> : CaCfirDiagnosticCreator {
    /**
     * 在当前 CFIR Analysis API 会话中创建公开诊断。
     */
    fun CaCfirSession.create(diagnostic: CjDiagnosticWithParameters2<A, B>): CaCfirDiagnostic<*>
}

/**
 * 三参数 CFIR 诊断的公开诊断创建器。
 */
internal fun interface CaCfirDiagnostic3Creator<A, B, C> : CaCfirDiagnosticCreator {
    /**
     * 在当前 CFIR Analysis API 会话中创建公开诊断。
     */
    fun CaCfirSession.create(diagnostic: CjDiagnosticWithParameters3<A, B, C>): CaCfirDiagnostic<*>
}

/**
 * 四参数 CFIR 诊断的公开诊断创建器。
 */
internal fun interface CaCfirDiagnostic4Creator<A, B, C, D> : CaCfirDiagnosticCreator {
    /**
     * 在当前 CFIR Analysis API 会话中创建公开诊断。
     */
    fun CaCfirSession.create(diagnostic: CjDiagnosticWithParameters4<A, B, C, D>): CaCfirDiagnostic<*>
}

/**
 * CFIR 诊断到 Analysis API typed diagnostics 的转换注册表。
 *
 * 该转换器只接受生成器显式注册过的 CFIR 诊断工厂；新增诊断或参数类型时，
 * 必须由生成器补齐具体转换，不在这里提供泛型兜底。
 */
internal class CaDiagnosticConverter(
    /**
     * CFIR 诊断工厂到公开诊断创建器的映射。
     */
    private val conversions: Map<CjDiagnosticFactoryN, CaCfirDiagnosticCreator>,
) {
    /**
     * 将一个 CFIR 诊断转换为公开 typed diagnostic。
     */
    fun convert(analysisSession: CaCfirSession, diagnostic: CjDiagnostic): CaCfirDiagnostic<*> {
        val factory = diagnostic.factory as? CjDiagnosticFactoryN
            ?: error("Analysis API only supports source diagnostics, got ${diagnostic.factory.name}")
        val creator = conversions[factory]
            ?: error("No Analysis API diagnostic conversion registered for ${factory.name}")

        @Suppress("UNCHECKED_CAST")
        return with(analysisSession) {
            when (creator) {
                is CaCfirDiagnostic0Creator -> with(creator) {
                    create(diagnostic as CjSimpleDiagnostic)
                }
                is CaCfirDiagnostic1Creator<*> -> with(creator as CaCfirDiagnostic1Creator<Any?>) {
                    create(diagnostic as CjDiagnosticWithParameters1<Any?>)
                }
                is CaCfirDiagnostic2Creator<*, *> -> with(creator as CaCfirDiagnostic2Creator<Any?, Any?>) {
                    create(diagnostic as CjDiagnosticWithParameters2<Any?, Any?>)
                }
                is CaCfirDiagnostic3Creator<*, *, *> -> with(creator as CaCfirDiagnostic3Creator<Any?, Any?, Any?>) {
                    create(diagnostic as CjDiagnosticWithParameters3<Any?, Any?, Any?>)
                }
                is CaCfirDiagnostic4Creator<*, *, *, *> -> with(creator as CaCfirDiagnostic4Creator<Any?, Any?, Any?, Any?>) {
                    create(diagnostic as CjDiagnosticWithParameters4<Any?, Any?, Any?, Any?>)
                }
                else -> error("Invalid CaCfirDiagnosticCreator ${creator::class.simpleName}")
            }
        }
    }
}

/**
 * 诊断转换器的注册表 builder。
 */
internal class CaDiagnosticConverterBuilder private constructor() {
    /**
     * 构建期间收集的诊断工厂映射。
     */
    private val conversions = mutableMapOf<CjDiagnosticFactoryN, CaCfirDiagnosticCreator>()

    /**
     * 注册无参数诊断转换。
     */
    fun add(diagnostic: CjDiagnosticFactory0, creator: CaCfirDiagnostic0Creator) {
        conversions[diagnostic] = creator
    }

    /**
     * 注册单参数诊断转换。
     */
    fun <A> add(diagnostic: CjDiagnosticFactory1<A>, creator: CaCfirDiagnostic1Creator<A>) {
        conversions[diagnostic] = creator
    }

    /**
     * 注册双参数诊断转换。
     */
    fun <A, B> add(diagnostic: CjDiagnosticFactory2<A, B>, creator: CaCfirDiagnostic2Creator<A, B>) {
        conversions[diagnostic] = creator
    }

    /**
     * 注册三参数诊断转换。
     */
    fun <A, B, C> add(diagnostic: CjDiagnosticFactory3<A, B, C>, creator: CaCfirDiagnostic3Creator<A, B, C>) {
        conversions[diagnostic] = creator
    }

    /**
     * 注册四参数诊断转换。
     */
    fun <A, B, C, D> add(diagnostic: CjDiagnosticFactory4<A, B, C, D>, creator: CaCfirDiagnostic4Creator<A, B, C, D>) {
        conversions[diagnostic] = creator
    }

    /**
     * 完成构建并冻结当前转换映射。
     */
    private fun build(): CaDiagnosticConverter = CaDiagnosticConverter(conversions)

    /**
     * 转换器构建入口。
     */
    companion object {
        /**
         * 使用 DSL 注册诊断转换并返回转换器。
         */
        inline fun buildConverter(init: CaDiagnosticConverterBuilder.() -> Unit): CaDiagnosticConverter =
            CaDiagnosticConverterBuilder().apply(init).build()
    }
}
