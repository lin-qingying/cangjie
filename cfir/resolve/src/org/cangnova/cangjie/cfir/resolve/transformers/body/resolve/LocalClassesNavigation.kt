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

class LocalClassesNavigationInfo(
    val parentForClass: Map<CfirClassLikeDeclaration, CfirClassLikeDeclaration?>,
    private val parentClassForFunction: Map<CfirCallableDeclaration, CfirClassLikeDeclaration>,
) {
    val designationMap: Map<CfirCallableDeclaration, List<CfirClassLikeDeclaration>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        parentClassForFunction.keys.associateWith(::pathForCallable)
    }

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

fun CfirClassLikeDeclaration.collectLocalClassesNavigationInfo(): LocalClassesNavigationInfo =
    NavigationInfoVisitor().run {
        this@collectLocalClassesNavigationInfo.accept(this, null)
        LocalClassesNavigationInfo(parentForClass, resultingMap)
    }

private class NavigationInfoVisitor : CfirDefaultVisitor<Unit, Any?>() {
    val resultingMap: MutableMap<CfirCallableDeclaration, CfirClassLikeDeclaration> = mutableMapOf()
    val parentForClass: MutableMap<CfirClassLikeDeclaration, CfirClassLikeDeclaration?> = mutableMapOf()

    private val currentPath: MutableList<CfirClassLikeDeclaration> = mutableListOf()

    override fun visitElement(element: CfirElement, data: Any?) = Unit

    override fun visitClass(klass: CfirClass, data: Any?) {
        visitClassLike(klass)
    }

    override fun visitInterface(`interface`: CfirInterface, data: Any?) {
        visitClassLike(`interface`)
    }

    override fun visitStruct(struct: CfirStruct, data: Any?) {
        visitClassLike(struct)
    }

    override fun visitEnum(enum: CfirEnum, data: Any?) {
        visitClassLike(enum)
    }

    override fun visitTypeAlias(typeAlias: CfirTypeAlias, data: Any?) {
        visitClassLike(typeAlias)
    }

    override fun visitNamedFunction(namedFunction: CfirNamedFunction, data: Any?) {
        visitCallableDeclaration(namedFunction, data)
    }

    override fun visitProperty(property: CfirProperty, data: Any?) {
        visitCallableDeclaration(property, data)
    }

    override fun visitFieldVariable(fieldVariable: CfirFieldVariable, data: Any?) {
        visitCallableDeclaration(fieldVariable, data)
    }

    override fun visitConstructor(constructor: CfirConstructor, data: Any?) {
        visitCallableDeclaration(constructor, data)
    }

    override fun visitCallableDeclaration(callableDeclaration: CfirCallableDeclaration, data: Any?) {
        if (callableDeclaration.returnTypeRefOrNull() !is CfirImplicitTypeRef) return
        val currentClass = currentPath.lastOrNull() ?: return
        resultingMap[callableDeclaration] = currentClass
    }

    private fun visitClassLike(classLikeDeclaration: CfirClassLikeDeclaration) {
        parentForClass[classLikeDeclaration] = currentPath.lastOrNull()
        currentPath += classLikeDeclaration
        classLikeDeclaration.acceptChildren(this, null)
        currentPath.removeAt(currentPath.lastIndex)
    }
}

private fun CfirCallableDeclaration.returnTypeRefOrNull(): CfirTypeRef? = when (this) {
    is CfirFunction -> returnTypeRef
    is CfirProperty -> returnTypeRef
    is CfirFieldVariable -> returnTypeRef
    is CfirPatternBindingVariable -> returnTypeRef
    is CfirPatternVariable -> returnTypeRef
    is CfirValueParameter -> returnTypeRef
    else -> null
}
