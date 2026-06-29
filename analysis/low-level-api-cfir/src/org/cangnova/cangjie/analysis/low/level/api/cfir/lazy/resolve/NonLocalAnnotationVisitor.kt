/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirErrorPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.visitors.CfirVisitor

/**
 * Visitor for annotations outside bodies.
 * 仓颉主干没有 Kotlin FIR 的 `ANNOTATION_ARGUMENTS` 独立 phase，
 * 这里仅保留 low-level 所需的“非 body 注解遍历”能力。
 *
 * This visitor is not recursive and processes only the target declaration (without unrelated nested declarations).
 * See [RecursiveNonLocalAnnotationVisitor] for the recursive visitor.
 *
 * @see processAnnotation
 * @see RecursiveNonLocalAnnotationVisitor
 */
internal abstract class NonLocalAnnotationVisitor<T> : CfirVisitor<Unit, T>() {
    /**
     * 处理遍历过程中遇到的单个非 body 注解。
     *
     * 具体实现决定是展开惰性参数、收集信息还是执行其他 annotation 相关动作。
     */
    abstract fun processAnnotation(annotation: CfirAnnotation, data: T)

    /**
     * Skip all [CfirElementWithResolveState] without explicit override
     */
    override fun visitElement(element: CfirElement, data: T) {
        if (element is CfirElementWithResolveState) return

        element.acceptChildren(this, data)
    }

    /**
     * Skip argument list as the compiler do not support annotations inside annotation arguments
     */
    override fun visitArgumentList(argumentList: CfirArgumentList, data: T) {}

    /**
     * 访问已解析类型引用中的非 body 注解。
     */
    override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: T) {
        resolvedTypeRef.acceptChildren(this, data)
    }

    /**
     * 错误类型引用沿用已解析类型引用的遍历规则，以便保留其中可能存在的注解处理。
     */
    override fun visitErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: T) {
        visitResolvedTypeRef(errorTypeRef, data)
    }

    /**
     * 处理当前注解后继续访问其非参数子节点。
     */
    override fun visitAnnotation(annotation: CfirAnnotation, data: T) {
        processAnnotation(annotation, data)
        annotation.acceptChildren(this, data)
    }

    /**
     * 将 annotation call 按普通注解处理。
     */
    override fun visitAnnotationCall(annotationCall: CfirAnnotationCall, data: T) {
        visitAnnotation(annotationCall, data)
    }

    /**
     * 访问文件级注解容器。
     */
    override fun visitFile(file: CfirFile, data: T) {
        visitAnnotationContainer(file, data)
    }

    /**
     * 访问成员声明的类型参数和声明自身注解。
     */
    override fun visitMemberDeclaration(memberDeclaration: CfirMemberDeclaration, data: T) {
        visitTypeParameterRefsOwner(memberDeclaration, data)
        visitAnnotationContainer(memberDeclaration, data)
    }

    /**
     * 访问类型别名的声明注解以及展开类型中的注解。
     */
    override fun visitTypeAlias(typeAlias: CfirTypeAlias, data: T) {
        visitMemberDeclaration(typeAlias, data)
        typeAlias.expandedTypeRef.accept(this, data)
    }

    /**
     * 访问类声明注解、类型参数和父类型引用中的注解。
     */
    override fun visitClass(klass: CfirClass, data: T) {
        visitMemberDeclaration(klass, data)
        klass.superTypeRefs.forEach { it.accept(this, data) }
    }

    /**
     * 访问 callable 的声明注解、类型参数和返回类型注解。
     */
    override fun visitCallableDeclaration(callableDeclaration: CfirCallableDeclaration, data: T) {
        visitMemberDeclaration(callableDeclaration, data)
        callableDeclaration.returnTypeRef.accept(this, data)
    }

    /**
     * 访问注解容器中直接挂载的所有注解。
     */
    override fun visitAnnotationContainer(annotationContainer: CfirAnnotationContainer, data: T) {
        annotationContainer.annotations.forEach { it.accept(this, data) }
    }

    /**
     * 访问类型参数引用拥有者声明的所有类型参数。
     */
    override fun visitTypeParameterRefsOwner(typeParameterRefsOwner: CfirTypeParameterRefsOwner, data: T) {
        typeParameterRefsOwner.typeParameters.forEach { it.accept(this, data) }
    }

    /**
     * 访问类型参数内部的注解和边界等子结构。
     */
    override fun visitTypeParameter(typeParameter: CfirTypeParameter, data: T) {
        typeParameter.acceptChildren(this, data)
    }

    /**
     * 访问函数声明及其值参数上的非 body 注解。
     */
    override fun visitFunction(function: CfirFunction, data: T) {
        visitCallableDeclaration(function, data)
        function.valueParameters.forEach { it.accept(this, data) }
    }

    /**
     * 命名函数沿用通用函数遍历规则。
     */
    override fun visitNamedFunction(namedFunction: CfirNamedFunction, data: T) {
        visitFunction(namedFunction, data)
    }

    /**
     * 构造函数沿用通用函数遍历规则。
     */
    override fun visitConstructor(constructor: CfirConstructor, data: T) {
        visitFunction(constructor, data)
    }

    /**
     * 错误主构造函数沿用通用函数遍历规则。
     */
    override fun visitErrorPrimaryConstructor(errorPrimaryConstructor: CfirErrorPrimaryConstructor, data: T) {
        visitFunction(errorPrimaryConstructor, data)
    }

    /**
     * 属性访问器沿用通用函数遍历规则。
     */
    override fun visitPropertyAccessor(propertyAccessor: CfirPropertyAccessor, data: T) {
        visitFunction(propertyAccessor, data)
    }

    /**
     * 访问变量声明的 callable 级注解和返回类型注解。
     */
    override fun visitVariable(variable: CfirVariable, data: T) {
        visitCallableDeclaration(variable, data)
    }

    /**
     * 访问属性声明及其访问器上的非 body 注解。
     */
    override fun visitProperty(property: CfirProperty, data: T) {
        visitCallableDeclaration(property, data)
        property.getter?.accept(this, data)
        property.setter?.accept(this, data)
    }

    /**
     * 值参数沿用变量遍历规则，以处理其类型和声明注解。
     */
    override fun visitValueParameter(valueParameter: CfirValueParameter, data: T) {
        visitVariable(valueParameter, data)
    }
}
