package org.cangnova.cangjie.cfir.resovle.calls

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.type.model.TypeVariableInferenceScope
import org.cangnova.cangjie.type.model.TypeVariableWithInferenceScope

/**
 * 延迟解析原子对应的临时类型变量。
 *
 * 该变量用于把 postponed atom 放入约束系统，等待 lambda、callable reference 等结构完成后再固定。
 */
class ConeTypeVariableForPostponedAtom(name: String) : ConeTypeVariable(name)

/**
 * lambda 形参类型对应的临时类型变量。
 */
class ConeTypeVariableForLambdaParameterType(name: String) : ConeTypeVariable(name)

/**
 * lambda 返回类型对应的临时类型变量。
 *
 * @property argument 产生该返回类型变量的 CFIR 声明。
 */
class ConeTypeVariableForLambdaReturnType(argument: CfirDeclaration, name: String) : ConeTypeVariable(name) {
    @Volatile
    private var argumentOrigin: CfirDeclaration = argument

    /** 产生该返回变量的当前声明；getter 与原构造签名保持一致。 */
    val argument: CfirDeclaration
        get() = argumentOrigin

    /** 保留 TV/defaultType/constructor 身份，仅按准确的旧声明重定位来源。 */
    @Synchronized
    fun relocateArgument(expected: CfirDeclaration, target: CfirDeclaration): Unit {
        check(argumentOrigin === expected) { "Lambda return type variable origin does not match the expected declaration" }
        argumentOrigin = target
    }
}

/**
 * 声明侧类型参数对应的约束系统类型变量。
 *
 * @property typeParameterSymbol 被转换为类型变量的类型参数符号。
 */
class ConeTypeParameterBasedTypeVariable(
    /**
     * 被转换为约束系统类型变量的类型参数符号。
     */
    typeParameterSymbol: CfirTypeParameterSymbol,
    /** 当前调用实例化的求解范围，不能用声明或参数名替代。 */
    override val inferenceScope: TypeVariableInferenceScope,
) : ConeTypeVariable(SpecialNames.safeIdentifier(typeParameterSymbol.name).identifier, typeParameterSymbol.toLookupTag()),
    TypeVariableWithInferenceScope {
    /**
     * constructor lookup 是类型参数来源的唯一状态，两个公开视图共同读取一次 origin。
     * 不保留可能与 constructor 脱节的第二份 symbol 字段。
     */
    val typeParameterSymbol: CfirTypeParameterSymbol
        get() = (typeConstructor.originalTypeParameter as ConeTypeParameterLookupTag).typeParameterSymbol

    /** 原子重定位 symbol getter 与 constructor lookup，不创建新的 TV 或构造器。 */
    fun relocateTypeParameterOrigin(expected: CfirTypeParameterSymbol, target: CfirTypeParameterSymbol): Unit {
        check(typeParameterSymbol === expected) { "Type-parameter-based variable origin does not match the expected symbol" }
        typeConstructor.relocateOriginalTypeParameter(expected.toLookupTag(), target.toLookupTag())
    }
}
