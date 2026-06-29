package org.cangnova.cangjie.analysis.api.impl.base.components

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaClassTypeBuilder
import org.cangnova.cangjie.analysis.api.components.CaTypeCreator
import org.cangnova.cangjie.analysis.api.components.CaTypeParameterTypeBuilder
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection
import org.cangnova.cangjie.name.ClassId

/**
 * impl-base 的类型创建组件基类。
 */
@CaImplementationDetail
abstract class CaBaseTypeCreator<T : CaSession> : CaBaseSessionComponent<T>(), CaTypeCreator {



}

/**
 * class-like 类型构造请求的基础 builder。
 */
@CaImplementationDetail
sealed class CaBaseClassTypeBuilder : CaClassTypeBuilder {
    /**
     * 当前 builder 收集到的类型实参。
     */
    private val backingArguments = mutableListOf<CaTypeProjection>()



    /**
     * 返回当前 builder 收集到的类型实参。
     */
    override val arguments: List<CaTypeProjection> get() = withValidityAssertion { backingArguments }

    /**
     * 向 builder 添加一个已经构造好的类型投影。
     */
    override fun argument(argument: CaTypeProjection): Unit = withValidityAssertion {
        backingArguments += argument
    }

    /**
     * 以 invariant projection 形式向 builder 添加类型实参。
     */
    override fun argument(type: CaType ): Unit = withValidityAssertion {
        backingArguments += CaTypeProjection(type, type.token)
    }

    /**
     * 通过 classId 描述目标 class-like 类型的 builder。
     */
    class ByClassId(
        classId: ClassId,
        /**
         * 当前 builder 绑定的 lifetime token。
         */
        override val token: CaLifetimeToken,
    ) : CaBaseClassTypeBuilder() {
        /**
         * 目标 class-like 类型的 classId。
         */
        private val backingClassId: ClassId = classId

        /**
         * 返回目标 class-like 类型的 classId。
         */
        val classId: ClassId get() = withValidityAssertion { backingClassId }
    }

    /**
     * 通过 class-like 符号描述目标类型的 builder。
     */
    class BySymbol(
        symbol: CaClassLikeSymbol,
        /**
         * 当前 builder 绑定的 lifetime token。
         */
        override val token: CaLifetimeToken,
    ) : CaBaseClassTypeBuilder() {
        /**
         * 目标 class-like 符号。
         */
        private val backingSymbol: CaClassLikeSymbol = symbol

        /**
         * 返回目标 class-like 符号。
         */
        val symbol: CaClassLikeSymbol get() = withValidityAssertion { backingSymbol }
    }
}


/**
 * type-parameter 类型构造请求的基础 builder。
 */
@CaImplementationDetail
sealed class CaBaseTypeParameterTypeBuilder : CaTypeParameterTypeBuilder {

    /**
     * 通过 type-parameter 符号描述目标类型的 builder。
     */
    class BySymbol(
        symbol: CaTypeParameterSymbol,
        /**
         * 当前 builder 绑定的 lifetime token。
         */
        override val token: CaLifetimeToken,
    ) : CaBaseTypeParameterTypeBuilder() {
        /**
         * 目标 type-parameter 符号。
         */
        private val backingSymbol: CaTypeParameterSymbol = symbol

        /**
         * 返回目标 type-parameter 符号。
         */
        val symbol: CaTypeParameterSymbol get() = withValidityAssertion { backingSymbol }
    }
}
