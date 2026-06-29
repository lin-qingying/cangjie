package org.cangnova.cangjie.analysis.api.impl.base.references

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieCompilerPluginsProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.idea.references.CjSimpleNameReference
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorConventions
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.CjOperationReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations
import org.cangnova.cangjie.utils.runIf

/**
 * simple-name reference 的 impl-base 基类。
 */
@CaImplementationDetail
abstract class CaBaseSimpleNameReference(expression: CjSimpleNameExpression) : CjSimpleNameReference(expression) {
    /**
     * 返回该引用可能解析到的名称集合。
     */
    override val resolvesByNames: Collection<Name>
        get() {
            val element = element
            val specialNames = when (element) {
                is CjOperationReferenceExpression -> operatorNames(element)

                // According to the KDoc, labels and `this`/`super` references cannot be properly expressed in terms of this API
                is CjNameReferenceExpression if (element.parent is CjInstanceExpressionWithLabel) -> emptyList()
                is CjLabelReferenceExpression -> emptyList()
                else -> null
            }

            if (specialNames != null) {
                return specialNames
            }

            return listOf(element.referencedNameAsName)
        }
}

/**
 * 根据操作符引用恢复可能解析到的操作函数名称。
 */
private fun operatorNames(expression: CjOperationReferenceExpression): Collection<Name>? = buildList {
    val tokenType = expression.operationSignTokenType ?: return null

    val parent = expression.parent
    val name = OperatorConventions.getNameForOperationSymbol(
        tokenType, parent is CjUnaryExpression, parent is CjBinaryExpression
    ) /*?: (parent as? CjBinaryExpression)?.let {
        runIf(it.operationToken == CjTokens.EQ && isAssignmentResolved(expression.project, it)) { ASSIGN_METHOD }
    }*/

    if (name != null) {
        add(name)
        val counterpart = OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS[tokenType]
        if (counterpart != null) {
            val counterpartName = OperatorConventions.getNameForOperationSymbol(counterpart, false, true)!!
            add(counterpartName)
        }
    }

    val isArrayModification = when (parent) {
        is CjBinaryExpression if tokenType in CjTokens.ALL_ASSIGNMENTS ->
            parent.left?.unwrapParenthesesLabelsAndAnnotations()

        is CjUnaryExpression if tokenType in CjTokens.INCREMENT_AND_DECREMENT ->
            parent.baseExpression?.unwrapParenthesesLabelsAndAnnotations()

        else -> null
    } is CjArrayAccessExpression

    if (isArrayModification) {
        add(OperatorNameConventions.SET)
    }
}

/**
 * 判断赋值表达式是否可能由插件提供的 assignment 语义解析。
 */
@OptIn(CaPlatformInterface::class)
private fun isAssignmentResolved(project: Project, binaryExpression: CjBinaryExpression): Boolean {
    val sourceModule = CangJieProjectStructureProvider.getModule(project, binaryExpression, useSiteModule = null)
    if (sourceModule !is CaSourceModule) {
        return false
    }

    val reference = binaryExpression.operationReference.reference ?: return false
    val compilerPluginsProvider = CangJieCompilerPluginsProvider.getInstance(project) ?: return false
    return compilerPluginsProvider.isPluginOfTypeRegistered(sourceModule, CangJieCompilerPluginsProvider.CompilerPluginType.ASSIGNMENT)
            /*&& (reference.resolve() as? CjNamedFunction)?.nameAsName == ASSIGN_METHOD*/
}
