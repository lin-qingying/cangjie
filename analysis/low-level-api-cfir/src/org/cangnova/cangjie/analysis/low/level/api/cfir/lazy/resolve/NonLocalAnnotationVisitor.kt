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

    override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: T) {
        resolvedTypeRef.acceptChildren(this, data)
    }

    override fun visitErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: T) {
        visitResolvedTypeRef(errorTypeRef, data)
    }

    override fun visitAnnotation(annotation: CfirAnnotation, data: T) {
        processAnnotation(annotation, data)
        annotation.acceptChildren(this, data)
    }

    override fun visitAnnotationCall(annotationCall: CfirAnnotationCall, data: T) {
        visitAnnotation(annotationCall, data)
    }

    override fun visitFile(file: CfirFile, data: T) {
        visitAnnotationContainer(file, data)
    }

    override fun visitMemberDeclaration(memberDeclaration: CfirMemberDeclaration, data: T) {
        visitTypeParameterRefsOwner(memberDeclaration, data)
        visitAnnotationContainer(memberDeclaration, data)
    }

    override fun visitTypeAlias(typeAlias: CfirTypeAlias, data: T) {
        visitMemberDeclaration(typeAlias, data)
        typeAlias.expandedTypeRef.accept(this, data)
    }

    override fun visitClass(klass: CfirClass, data: T) {
        visitMemberDeclaration(klass, data)
        klass.superTypeRefs.forEach { it.accept(this, data) }
    }

    override fun visitCallableDeclaration(callableDeclaration: CfirCallableDeclaration, data: T) {
        visitMemberDeclaration(callableDeclaration, data)
        callableDeclaration.returnTypeRef.accept(this, data)
    }

    override fun visitAnnotationContainer(annotationContainer: CfirAnnotationContainer, data: T) {
        annotationContainer.annotations.forEach { it.accept(this, data) }
    }

    override fun visitTypeParameterRefsOwner(typeParameterRefsOwner: CfirTypeParameterRefsOwner, data: T) {
        typeParameterRefsOwner.typeParameters.forEach { it.accept(this, data) }
    }

    override fun visitTypeParameter(typeParameter: CfirTypeParameter, data: T) {
        typeParameter.acceptChildren(this, data)
    }

    override fun visitFunction(function: CfirFunction, data: T) {
        visitCallableDeclaration(function, data)
        function.valueParameters.forEach { it.accept(this, data) }
    }

    override fun visitNamedFunction(namedFunction: CfirNamedFunction, data: T) {
        visitFunction(namedFunction, data)
    }

    override fun visitConstructor(constructor: CfirConstructor, data: T) {
        visitFunction(constructor, data)
    }

    override fun visitErrorPrimaryConstructor(errorPrimaryConstructor: CfirErrorPrimaryConstructor, data: T) {
        visitFunction(errorPrimaryConstructor, data)
    }

    override fun visitPropertyAccessor(propertyAccessor: CfirPropertyAccessor, data: T) {
        visitFunction(propertyAccessor, data)
    }

    override fun visitVariable(variable: CfirVariable, data: T) {
        visitCallableDeclaration(variable, data)
    }

    override fun visitProperty(property: CfirProperty, data: T) {
        visitCallableDeclaration(property, data)
        property.getter?.accept(this, data)
        property.setter?.accept(this, data)
    }

    override fun visitValueParameter(valueParameter: CfirValueParameter, data: T) {
        visitVariable(valueParameter, data)
    }
}
