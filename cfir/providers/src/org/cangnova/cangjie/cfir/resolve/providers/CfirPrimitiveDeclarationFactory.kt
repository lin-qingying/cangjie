package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.nullableModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.initDefaultResolveState
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.isExposedBuiltinClassifier
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * primitive 类型合成声明的**会话级单例工厂**。
 *
 * primitive 在符号层是合成 class-like（[CfirPrimitiveTypeDeclaration]），但它的
 * 消费方不止一处：symbol provider（名称解析）、builtin primitive scope（按名
 * 直查）与后续 raw 层的 qualifier 身份还原都需要拿到**同一个** classId → symbol
 * 绑定。若各消费方自行构造，会产出多个不 `==` 的 symbol / declaration，身份比较
 * 类逻辑（lookup tag 绑定缓存、scope 缓存键、可达性判定）随之失真。
 *
 * 因此本组件是 session 内 primitive 身份的**唯一工厂**：
 * - [getDeclaration] 按需构造并缓存，同 session 内同 classId 永远返回同一声明；
 * - [getExposedSymbolsByName] 供名称 scope 直接按短名取 symbol，构造路径与
 *   [getDeclaration] 完全一致，不存在第二套来源；
 * - 声明 `moduleData` 统一落到本 session 的合成 builtins 宿主（[builtinModuleData]），
 *   与 [CfirBuiltinSymbolProvider] 的既有行为一致。
 *
 * 该组件必须满足 `CfirSession.registerAnalysisBaseComponents` 之后的装配时序：
 * 声明合成本身不依赖 base 组件，但声明所属 session 在被 checkers / resolve 触达时
 * 必须已具备分析基础组件集。
 */
class CfirPrimitiveDeclarationFactory(
    /**
     * 合成声明所属的会话。
     */
    private val session: CfirSession,
) {
    /**
     * 合成 builtin 声明的宿主 module data。
     *
     * 若 session 已经绑定 module data，则沿用当前 module；否则创建二进制依赖
     * module data 作为合成 builtin 宿主，与 [CfirBuiltinSymbolProvider] 保持一致。
     */
    private val builtinModuleData: CfirModuleData by lazy(LazyThreadSafetyMode.PUBLICATION) {
        session.nullableModuleData
            ?: CfirBinaryDependenciesModuleData(Name.identifier("<builtins>")).also { it.bindSession(session) }
    }

    /**
     * primitive 声明缓存：classId → 已合成的 class-like 声明。
     */
    private val declarationsByClassId: Map<ClassId, CfirPrimitiveTypeDeclaration> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        PrimitiveTypeKind.entries.associateBy(
            keySelector = { kind -> kind.classId },
            valueTransform = ::buildPrimitiveDeclaration,
        )
    }

    /**
     * 返回指定 classId 对应的 primitive 合成声明；非 primitive classId 返回 `null`。
     */
    fun getDeclaration(classId: ClassId): CfirPrimitiveTypeDeclaration? =
        declarationsByClassId[classId]

    /**
     * 返回指定 primitive 种类的合成声明。
     */
    fun getDeclaration(kind: PrimitiveTypeKind): CfirPrimitiveTypeDeclaration =
        requireNotNull(declarationsByClassId[kind.classId]) {
            "Primitive declaration for `${kind.typeName}` must be prebuilt"
        }

    /**
     * 按语言层短名返回暴露的 primitive symbol；未暴露种类（Ideal*）返回 `null`。
     */
    fun getExposedSymbolByName(typeName: String): CfirPrimitiveTypeSymbol? {
        val kind = PrimitiveTypeKind.entries.firstOrNull {
            it.isExposedBuiltinClassifier && it.typeName == typeName
        } ?: return null
        return getDeclaration(kind).symbol
    }

    /**
     * 按名称枚举所有暴露的 primitive symbol；供名称 scope 构建索引。
     */
    fun exposedSymbolsByName(): Map<String, CfirPrimitiveTypeSymbol> = buildMap {
        for (kind in PrimitiveTypeKind.entries.filter(PrimitiveTypeKind::isExposedBuiltinClassifier)) {
            put(kind.typeName, getDeclaration(kind).symbol)
        }
    }

    /**
     * 构造一个 primitive 类型的合成 class-like 声明。
     *
     * 与 [CfirBuiltinSymbolProvider] 的构造契约保持一致：同样的 origin、
     * operator 成员集合与默认 resolve 状态；二者现在共用同一工厂，不再各自为政。
     */
    private fun buildPrimitiveDeclaration(kind: PrimitiveTypeKind): CfirPrimitiveTypeDeclaration {
        val symbol = CfirPrimitiveTypeSymbol(kind.classId, kind)
        val declaration = CfirPrimitiveTypeDeclaration(
            moduleData = builtinModuleData,
            symbol = symbol,
            name = kind.classId.shortClassName,
            kind = kind,
            scopeProvider = session.cangjieScopeProvider,
            origin = CfirDeclarationOrigin.Synthetic.Default,
            attributes = CfirDeclarationAttributes.EMPTY,
            declarations = CfirBuiltinPrimitiveMembers.build(kind, builtinModuleData).toMutableList(),
            superTypeRefs = mutableListOf(),
        )
        declaration.initDefaultResolveState()

        return declaration
    }
}
