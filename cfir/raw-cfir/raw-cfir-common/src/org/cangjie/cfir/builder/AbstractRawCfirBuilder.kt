package org.cangjie.cfir.builder

import com.intellij.psi.tree.IElementType
import org.cangjie.cfir.CfirElement
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.common.moduleData
import org.cangjie.cfir.declarations.CfirDeclaration
import org.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.expressions.CfirErrorExpression
import org.cangjie.cfir.expressions.CfirExpression
import org.cangjie.cfir.expressions.builder.buildErrorExpression as buildErrorExpressionNode
import org.cangjie.cfir.references.CfirNamedReference
import org.cangjie.cfir.references.builder.buildNamedReference as buildNamedReferenceNode
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.builder.buildImplicitTypeRef as buildImplicitTypeRefNode
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

abstract class AbstractRawCfirBuilder<T : Any>(
    val baseSession: CfirSession,
    val context: Context<T> = Context(),
) {
    val baseModuleData: CfirModuleData = baseSession.moduleData

    protected var packageFqName: FqName
        get() = context.packageFqName
        set(value) {
            context.packageFqName = value
        }

    protected fun <R> withPackageContext(fqName: FqName, block: () -> R): R = context.withPackage(fqName, block)

    protected fun <R> withLocalContext(block: () -> R): R = context.withLocalContext(block)

    abstract fun T.toSourceElement(): CfirSourceElement

    abstract fun T.elementType(): IElementType

    abstract fun T.asText(): String

    open fun buildElement(element: T): CfirElement {
        error("Unsupported build element entry: ${element::class.qualifiedName}")
    }

    open fun buildFile(file: T): CfirFile {
        error("Unsupported build file entry: ${file::class.qualifiedName}")
    }

    open fun buildDeclaration(declaration: T): CfirDeclaration {
        error("Unsupported build declaration entry: ${declaration::class.qualifiedName}")
    }

    open fun buildExpression(expression: T): CfirExpression {
        error("Unsupported build expression entry: ${expression::class.qualifiedName}")
    }

    protected open fun buildDeclarationStatus(
        visibility: Visibility,
        isAbstract: Boolean = false,
        isOpen: Boolean = false,
        isSealed: Boolean = false,
        isStatic: Boolean = false,
        isMut: Boolean = false,
        isOverride: Boolean = false,
        isOperator: Boolean = false,
        isInline: Boolean = false,
        isUnsafe: Boolean = false,
        isForeign: Boolean = false,
    ): CfirDeclarationStatus {
        return CfirDeclarationStatus(
            visibility = visibility,
            isAbstract = isAbstract,
            isOpen = isOpen,
            isSealed = isSealed,
            isStatic = isStatic,
            isMut = isMut,
            isOverride = isOverride,
            isOperator = isOperator,
            isInline = isInline,
            isUnsafe = isUnsafe,
            isForeign = isForeign,
        )
    }

    @Suppress("UNUSED_PARAMETER")
    protected fun buildNamedReference(name: Name, source: CfirSourceElement? = null): CfirNamedReference {
        return buildNamedReferenceNode {
            this.name = name
        }
    }

    @Suppress("UNUSED_PARAMETER")
    protected fun buildErrorExpression(source: CfirSourceElement? = null, reason: String): CfirErrorExpression {
        return buildErrorExpressionNode {
            this.reason = reason
        }
    }

    protected fun buildImplicitTypeRef(): CfirTypeRef {
        return buildImplicitTypeRefNode()
    }
}

