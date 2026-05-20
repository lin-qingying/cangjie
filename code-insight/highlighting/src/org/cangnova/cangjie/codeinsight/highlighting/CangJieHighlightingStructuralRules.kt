/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.codeinsight.highlighting

import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjVariable
import org.cangnova.cangjie.psi.psiUtil.isAbstract

/**
 * 仓颉 PSI 结构元素到 HighlightInfoType 的共享规则。
 *
 * IntelliJ 的高亮访问器负责“什么时候遍历、把高亮信息写到哪里”；本对象只回答
 * “这个仓颉 PSI 元素应该归到哪个高亮类型”。这样结构高亮规则只有一份。
 */
object CangJieHighlightingStructuralRules {
    fun highlightInfoTypeForElement(element: PsiElement): HighlightInfoType? =
        highlightInfoTypeForTypeDeclaration(element)
            ?: highlightInfoTypeForFunction(element)
            ?: highlightInfoTypeForPropertyDeclaration(element)

    fun highlightInfoTypeForVariableDeclaration(variable: CjVariable<*>): HighlightInfoType = when (variable) {
        is CjPatternVariable -> when {
            variable.isLocal -> CangJieHighlightInfoTypeSemanticNames.LOCAL_VARIABLE
            variable.isTopLevel -> CangJieHighlightInfoTypeSemanticNames.PACKAGE_VARIABLE
            else -> CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY
        }

        is CjFieldVariable -> CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY
        else -> CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY
    }

    fun highlightInfoTypeForPropertyDeclaration(property: CjProperty): HighlightInfoType = when {
        property.isLocal -> CangJieHighlightInfoTypeSemanticNames.LOCAL_VARIABLE
        property.isCustomPropertyDeclaration() ->
            CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION

        else -> CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY
    }

    fun highlightInfoTypeForParameterDeclaration(parameter: CjParameter): HighlightInfoType = when {
        parameter.letOrVarKeyword != null -> CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY
        else -> CangJieHighlightInfoTypeSemanticNames.PARAMETER
    }

    fun highlightInfoTypeForPropertyDeclaration(declaration: PsiElement): HighlightInfoType? = when (declaration) {
        is CjProperty -> highlightInfoTypeForPropertyDeclaration(declaration)
        is CjParameter -> highlightInfoTypeForParameterDeclaration(declaration)
        is CjVariable<*> -> highlightInfoTypeForVariableDeclaration(declaration)
        else -> null
    }

    fun highlightInfoTypeForFunction(function: PsiElement): HighlightInfoType? = when (function) {
        is CjFunction -> CangJieHighlightInfoTypeSemanticNames.FUNCTION_DECLARATION
        else -> null
    }

    fun highlightInfoTypeForTypeDeclaration(declaration: PsiElement): HighlightInfoType? = when {
        declaration is CjTypeParameter -> CangJieHighlightInfoTypeSemanticNames.TYPE_PARAMETER
        declaration is CjTypeAlias -> CangJieHighlightInfoTypeSemanticNames.TYPE_ALIAS
        declaration is CjTypeStatement -> highlightInfoTypeForClass(declaration)
        else -> null
    }

    fun highlightInfoTypeForClass(cclass: CjTypeStatement): HighlightInfoType = when {
        cclass.isInterface() -> CangJieHighlightInfoTypeSemanticNames.INTERFACE
        cclass.isStruct() -> CangJieHighlightInfoTypeSemanticNames.STRUCT
        cclass.isEnum() -> CangJieHighlightInfoTypeSemanticNames.ENUM
        cclass.isAbstract() -> CangJieHighlightInfoTypeSemanticNames.ABSTRACT_CLASS
        else -> CangJieHighlightInfoTypeSemanticNames.CLASS
    }

    private fun CjProperty.isCustomPropertyDeclaration(): Boolean =
        getter?.bodyExpression != null || setter?.bodyExpression != null
}

fun textAttributesKeyForCjElement(element: PsiElement): HighlightInfoType? =
    CangJieHighlightingStructuralRules.highlightInfoTypeForElement(element)

fun textAttributesForCjVariableDeclaration(variable: CjVariable<*>): HighlightInfoType =
    CangJieHighlightingStructuralRules.highlightInfoTypeForVariableDeclaration(variable)

fun textAttributesForCjPropertyDeclaration(property: CjProperty): HighlightInfoType =
    CangJieHighlightingStructuralRules.highlightInfoTypeForPropertyDeclaration(property)

fun textAttributesForCjParameterDeclaration(parameter: CjParameter): HighlightInfoType =
    CangJieHighlightingStructuralRules.highlightInfoTypeForParameterDeclaration(parameter)

fun textAttributesKeyForPropertyDeclaration(declaration: PsiElement): HighlightInfoType? =
    CangJieHighlightingStructuralRules.highlightInfoTypeForPropertyDeclaration(declaration)

fun textAttributesKeyForCjFunction(function: PsiElement): HighlightInfoType? =
    CangJieHighlightingStructuralRules.highlightInfoTypeForFunction(function)

fun textAttributesKeyForTypeDeclaration(declaration: PsiElement): HighlightInfoType? =
    CangJieHighlightingStructuralRules.highlightInfoTypeForTypeDeclaration(declaration)

fun textAttributesForClass(cclass: CjTypeStatement): HighlightInfoType =
    CangJieHighlightingStructuralRules.highlightInfoTypeForClass(cclass)
