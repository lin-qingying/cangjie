package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 类型解析服务抽象。
 *
 * 对齐 Kotlin: `org.jetbrains.kotlin.fir.resolve.FirTypeResolver`（会话级组件形态）。
 */
abstract class CfirTypeResolver : CfirSessionComponent {
    /**
     * 将类型引用解析为类声明；无法解析时返回 `null`。
     */
    abstract fun resolveClass(typeRef: CfirTypeRef): CfirClass?

    /**
     * 按 `ClassId` 解析类声明；无法解析时返回 `null`。
     */
    abstract fun resolveClass(classId: ClassId): CfirClass?
}

/**
 * `CfirTypeResolver` 的默认会话实现。
 */
  class CfirTypeResolverImpl(
    private val session: CfirSession,
) : CfirTypeResolver() {
    /** 基于用户类型引用路径计算 `ClassId` 并解析。 */
    override fun resolveClass(typeRef: CfirTypeRef): CfirClass? {
        val userTypeRef = typeRef as? CfirUserTypeRef ?: return null
        if (userTypeRef.qualifier.isEmpty()) return null

        val className = userTypeRef.qualifier.last()
        val packageName = userTypeRef.qualifier.dropLast(1).joinToString(".") { it.asString() }
        val packageFqName = if (packageName.isEmpty()) FqName.ROOT else FqName(packageName)
        return resolveClass(ClassId(packageFqName, className))
    }

    /** 直接走 provider 进行类查找，fallback 到 symbolProvider 查依赖库。 */
    override fun resolveClass(classId: ClassId): CfirClass? {
        // 优先查本 session 的 cfirProvider（源码类）
        session.cfirProvider.getClassByClassId(classId)?.let { return it }
        // fallback: 通过 symbolProvider 查依赖库
        return session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir as? CfirClass
    }
}

