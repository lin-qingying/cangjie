package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.CfirRawDeclarationDataPolicies
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol

/**
 * typealias 构造器合成信息属性 key。
 */
private object TypeAliasConstructorInfoKey : CfirDeclarationDataKey() {
    override val rawCopyPolicy get() = CfirRawDeclarationDataPolicies.forbidden<TypeAliasConstructorInfo<*>>("typealias scope substitutor")
}

/**
 * typealias 构造器合成信息。
 *
 * @property originalConstructor 展开类型上的原始构造器。
 * @property typeAliasSymbol 触发构造器合成的 typealias symbol。
 * @property substitutor 展开类型 scope 使用的类型替换器。
 */
data class TypeAliasConstructorInfo<T : CfirFunction>(
    /**
     * 展开类型上的原始构造器声明。
     */
    val originalConstructor: T,
    /**
     * 触发构造器合成的 typealias symbol。
     */
    val typeAliasSymbol: CfirTypeAliasSymbol,
    /**
     * 展开类型 scope 使用的类型替换器。
     */
    val substitutor: ConeSubstitutor?,
)

/**
 * 构造器声明上携带的 typealias 构造器合成信息。
 */
var <T : CfirFunction> T.typeAliasConstructorInfo: TypeAliasConstructorInfo<T>? by CfirDeclarationDataRegistry.data(TypeAliasConstructorInfoKey)

/**
 * 构造器 symbol 对应的 typealias 构造器合成信息。
 */
val CfirConstructorSymbol.typeAliasConstructorInfo: TypeAliasConstructorInfo<*>?
    get() = cfir.typeAliasConstructorInfo
