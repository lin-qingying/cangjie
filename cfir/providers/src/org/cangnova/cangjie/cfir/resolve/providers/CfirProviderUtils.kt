package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.name.FqName

/**
 * accessor、fake override、delegated override 与 substitution override 都只是 use-site 外壳，
 * 不应该改变 owner/file/visibility/annotation 等声明元数据的来源。
 *
 * provider / symbolProvider 在做归属查询前，必须先把 accessor 还原为 property，再递归剥离
 * override 外壳，最后再次还原可能由 override 链暴露出的 accessor。这个顺序保证属性访问与
 * 直接属性查询最终落到同一个声明身份。
 */
internal fun CfirBasedSymbol<*>.unwrapForDeclarationMetadataLookup(): CfirBasedSymbol<*> {
    return (this as? CfirCallableSymbol<*>)?.unwrapCallableForDeclarationMetadataLookup() ?: this
}

/**
 * 将 callable symbol 归一化为声明元数据查询使用的原始 callable symbol。
 */
internal fun CfirCallableSymbol<*>.unwrapCallableForDeclarationMetadataLookup(): CfirCallableSymbol<*> {
    var current = unwrapTypeAliasConstructor().unwrapPropertyAccessor()
    while (true) {
        val declarationIdentity = current
            .unwrapSubstitutionOverrides()
            .unwrapFakeOverridesOrDelegated()
            .unwrapTypeAliasConstructor()
            .unwrapPropertyAccessor()
        if (declarationIdentity === current) {
            return declarationIdentity.unwrapNonLocalPatternBinding()
        }
        current = declarationIdentity
    }
}

/**
 * synthetic typealias constructor 只承载别名 use-site 的参数替换；构造器可见性、声明文件
 * 与 nominal owner 属于展开类型的真实构造器。alias 自身的可发现性由 classifier collector
 * 独立检查，不能通过 synthetic callable 替代这两个 owner 中的任意一个。
 */
private fun CfirCallableSymbol<*>.unwrapTypeAliasConstructor(): CfirCallableSymbol<*> {
    val originalConstructor = (this as? CfirConstructorSymbol)
        ?.typeAliasConstructorInfo
        ?.originalConstructor
        ?: return this
    return checkNotNull(originalConstructor.symbol as? CfirCallableSymbol<*>) {
        "Typealias constructor original declaration must have a callable symbol: $originalConstructor"
    }
}

/** 将属性访问器归一化为其拥有的属性符号。 */
private fun CfirCallableSymbol<*>.unwrapPropertyAccessor(): CfirCallableSymbol<*> =
    (this as? CfirPropertyAccessorSymbol)?.propertySymbol ?: this

/**
 * 顶层 pattern 声明进入名称作用域的是 binding symbol，但声明注解和 initializer 的 owner
 * 是外层 [org.cangnova.cangjie.cfir.declarations.CfirPatternVariable]。非局部 binding 必须
 * 通过 provider 索引还原到该 owner，才能让所有声明元数据查询看到同一个声明身份。
 */
private fun CfirCallableSymbol<*>.unwrapNonLocalPatternBinding(): CfirCallableSymbol<*> {
    val binding = this as? CfirPatternBindingSymbol ?: return this
    if (binding.cfir.isLocal) return binding
    val owner = checkNotNull(binding.cfir.moduleData.session.cfirProvider.getCfirPatternVariableForBinding(binding)) {
        "Non-local pattern binding `${binding.callableId}` is missing its declaration owner index"
    }
    return owner.symbol
}

/**
 * 统一 provider 侧 container file 查询入口。
 */
fun CfirProvider.getContainingFile(symbol: CfirBasedSymbol<*>): CfirFile? {
    val normalizedSymbol = symbol.unwrapForDeclarationMetadataLookup()
    return when (normalizedSymbol) {
        is CfirCallableSymbol<*> -> getCfirCallableContainerFile(normalizedSymbol)
        is CfirClassLikeSymbol<*> -> getCfirClassifierContainerFileIfAny(normalizedSymbol)
        else -> null
    }
}

/**
 * 从声明符号自身所属的 session/provider 查询容器文件。
 *
 * 声明元数据的 owner 是声明模块，不是当前使用点模块。调用方不得把 use-site provider
 * 传入归属查询，否则依赖模块声明会因主模块 provider 不持有其文件索引而被误判为无归属。
 */
fun CfirBasedSymbol<*>.getContainingFile(): CfirFile? {
    val normalizedSymbol = unwrapForDeclarationMetadataLookup()
    return normalizedSymbol.cfir.moduleData.session.cfirProvider.getContainingFile(normalizedSymbol)
}

/**
 * 从声明符号自身所属的 session/provider 查询外层 class-like 声明。
 */
fun CfirBasedSymbol<*>.getContainingClass(): CfirClassLikeSymbol<*>? {
    val normalizedSymbol = unwrapForDeclarationMetadataLookup()
    return normalizedSymbol.cfir.moduleData.session.cfirProvider.getContainingClass(normalizedSymbol)
}

/**
 * 从 callable 原始声明所属的 session/provider 查询外层 extend 声明。
 */
fun CfirCallableSymbol<*>.getContainingExtend(): CfirExtend? {
    val normalizedSymbol = unwrapCallableForDeclarationMetadataLookup()
    return normalizedSymbol.cfir.moduleData.session.extendProviderOrNull?.getContainingExtend(normalizedSymbol)
}

/**
 * 从 extend 声明自身所属的 session/provider 查询声明包。
 *
 * extend 可能来自依赖 session 或反序列化 provider；使用点 session 只负责消费声明，
 * 不能作为声明归属元数据的 owner。
 */
fun CfirExtend.getDeclarationPackage(): FqName? =
    moduleData.session.extendProviderOrNull?.getPackageFqName(this)

/**
 * 从 extend 声明自身所属的 session/provider 查询容器文件。
 *
 * 反序列化声明没有源码文件时返回 `null`，调用方不得据此放宽 private 访问。
 */
fun CfirExtend.getContainingFile(): CfirFile? =
    moduleData.session.extendProviderOrNull?.getContainingFile(this)

/**
 * 返回声明符号的稳定包身份。
 *
 * class-like 以 [org.cangnova.cangjie.name.ClassId] 为准；callable 优先采用真实
 * owner extend 的包，其次采用非 local CallableId。只有没有稳定 id 的声明才回退到
 * 声明自身 session 的文件索引。这里始终查询 declaration session，不读取 use-site session。
 */
fun CfirBasedSymbol<*>.getDeclarationPackage(): FqName? {
    val normalizedSymbol = unwrapForDeclarationMetadataLookup()
    return when (normalizedSymbol) {
        is CfirClassLikeSymbol<*> -> normalizedSymbol.classId.packageFqName
        is CfirCallableSymbol<*> -> normalizedSymbol.getContainingExtend()
            ?.getDeclarationPackage()
            ?: normalizedSymbol.callableId.packageName.takeUnless { normalizedSymbol.callableId.isLocal }
            ?: normalizedSymbol.getContainingFile()?.packageDirective?.packageFqName

        else -> normalizedSymbol.getContainingFile()?.packageDirective?.packageFqName
    }
}
