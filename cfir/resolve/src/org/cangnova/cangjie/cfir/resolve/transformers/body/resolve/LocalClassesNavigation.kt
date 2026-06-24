package org.cangnova.cangjie.cfir.resolve.transformers.body.resolve

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitor

/**
 * 局部类导航信息。
 *
 * 该结构记录局部 class-like 声明的父链，以及隐式返回类型 callable 所在的 class-like 路径。
 */
class LocalClassesNavigationInfo(
    /**
     * class-like 声明到其父 class-like 声明的映射。
     */
    val parentForClass: Map<CfirClassLikeDeclaration, CfirClassLikeDeclaration?>,
    /**
     * 隐式返回类型 callable 到其直接所在 class-like 声明的映射。
     */
    private val parentClassForFunction: Map<CfirCallableDeclaration, CfirClassLikeDeclaration>,
) {
    /**
     * callable 到完整 class-like designation 路径的懒映射。
     */
    val designationMap: Map<CfirCallableDeclaration, List<CfirClassLikeDeclaration>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        parentClassForFunction.keys.associateWith(::pathForCallable)
    }

    /**
     * 计算 callable 所在 class-like 声明从外到内的路径。
     */
    fun pathForCallable(callableDeclaration: CfirCallableDeclaration): List<CfirClassLikeDeclaration> {
        val result = mutableListOf<CfirClassLikeDeclaration>()
        var current = parentClassForFunction[callableDeclaration]

        while (current != null) {
            result += current
            current = parentForClass[current]
        }

        return result.asReversed()
    }
}

/**
 * 收集 class-like 声明内部局部类与隐式返回类型 callable 的导航信息。
 */
fun CfirClassLikeDeclaration.collectLocalClassesNavigationInfo(): LocalClassesNavigationInfo =
    NavigationInfoVisitor().run {
        this@collectLocalClassesNavigationInfo.accept(this, null)
        LocalClassesNavigationInfo(parentForClass, resultingMap)
    }

/**
 * 遍历 class-like 声明树并收集局部类导航信息的 visitor。
 */
private class NavigationInfoVisitor : CfirDefaultVisitor<Unit, Any?>() {
    /**
     * 隐式返回类型 callable 到其直接父 class-like 的映射。
     */
    val resultingMap: MutableMap<CfirCallableDeclaration, CfirClassLikeDeclaration> = mutableMapOf()
    /**
     * class-like 声明到其直接父 class-like 的映射。
     */
    val parentForClass: MutableMap<CfirClassLikeDeclaration, CfirClassLikeDeclaration?> = mutableMapOf()

    /**
     * 当前遍历路径上的 class-like 声明栈。
     */
    private val currentPath: MutableList<CfirClassLikeDeclaration> = mutableListOf()

    /**
     * 默认不处理普通元素。
     */
    override fun visitElement(element: CfirElement, data: Any?) = Unit

    /**
     * 访问 class 声明。
     */
    override fun visitClass(klass: CfirClass, data: Any?) {
        visitClassLike(klass)
    }

    /**
     * 访问 interface 声明。
     */
    override fun visitInterface(`interface`: CfirInterface, data: Any?) {
        visitClassLike(`interface`)
    }

    /**
     * 访问 struct 声明。
     */
    override fun visitStruct(struct: CfirStruct, data: Any?) {
        visitClassLike(struct)
    }

    /**
     * 访问 enum 声明。
     */
    override fun visitEnum(enum: CfirEnum, data: Any?) {
        visitClassLike(enum)
    }

    /**
     * 访问 typealias 声明。
     */
    override fun visitTypeAlias(typeAlias: CfirTypeAlias, data: Any?) {
        visitClassLike(typeAlias)
    }

    /**
     * 访问具名函数声明。
     */
    override fun visitNamedFunction(namedFunction: CfirNamedFunction, data: Any?) {
        visitCallableDeclaration(namedFunction, data)
    }

    /**
     * 访问属性声明。
     */
    override fun visitProperty(property: CfirProperty, data: Any?) {
        visitCallableDeclaration(property, data)
    }

    /**
     * 访问字段变量声明。
     */
    override fun visitFieldVariable(fieldVariable: CfirFieldVariable, data: Any?) {
        visitCallableDeclaration(fieldVariable, data)
    }

    /**
     * 访问构造器声明。
     */
    override fun visitConstructor(constructor: CfirConstructor, data: Any?) {
        visitCallableDeclaration(constructor, data)
    }

    /**
     * 记录隐式返回类型 callable 的直接父 class-like。
     */
    override fun visitCallableDeclaration(callableDeclaration: CfirCallableDeclaration, data: Any?) {
        if (callableDeclaration.returnTypeRefOrNull() !is CfirImplicitTypeRef) return
        val currentClass = currentPath.lastOrNull() ?: return
        resultingMap[callableDeclaration] = currentClass
    }

    /**
     * 进入 class-like 声明并递归访问其子节点。
     */
    private fun visitClassLike(classLikeDeclaration: CfirClassLikeDeclaration) {
        parentForClass[classLikeDeclaration] = currentPath.lastOrNull()
        currentPath += classLikeDeclaration
        classLikeDeclaration.acceptChildren(this, null)
        currentPath.removeAt(currentPath.lastIndex)
    }
}

/**
 * 返回 callable 声明携带的返回类型引用。
 */
private fun CfirCallableDeclaration.returnTypeRefOrNull(): CfirTypeRef? = when (this) {
    is CfirFunction -> returnTypeRef
    is CfirProperty -> returnTypeRef
    is CfirFieldVariable -> returnTypeRef
    is CfirPatternBindingVariable -> returnTypeRef
    is CfirPatternVariable -> returnTypeRef
    is CfirValueParameter -> returnTypeRef
    else -> null
}
