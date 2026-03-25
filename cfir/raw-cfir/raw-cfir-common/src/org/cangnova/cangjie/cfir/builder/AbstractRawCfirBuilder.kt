package org.cangnova.cangjie.cfir.builder

import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression as buildErrorExpressionNode
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference as buildNamedReferenceNode
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef as buildImplicitTypeRefNode
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

    abstract fun T.toSourceElement(): AbstractCjSourceElement

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
        isVisibilityExplicit: Boolean = false,
        isModalityExplicit: Boolean = false,
        isAbstract: Boolean = false,
        isOpen: Boolean = false,
        isSealed: Boolean = false,
        isStatic: Boolean = false,
        isMut: Boolean = false,
        isOverride: Boolean = false,
        isOperator: Boolean = false,
        isUnsafe: Boolean = false,
        isForeign: Boolean = false,
    ): CfirDeclarationStatus {
        val status = CfirDeclarationStatusImpl(
            visibility = visibility,
            modality = Modality.convertFromFlags(isSealed, isAbstract, isOpen),
        )
        status.isAbstract = isAbstract
        status.isOpen = isOpen
        status.isSealed = isSealed
        status.isVisibilityExplicit = isVisibilityExplicit
        status.isModalityExplicit = isModalityExplicit
        status.isStatic = isStatic
        status.isMut = isMut
        status.isOverride = isOverride
        status.isOperator = isOperator
        status.isUnsafe = isUnsafe
        status.isForeign = isForeign
        return status
    }

    @Suppress("UNUSED_PARAMETER")
    protected fun buildNamedReference(name: Name, source: AbstractCjSourceElement? = null): CfirNamedReference {
        return buildNamedReferenceNode {
            this.source = source as? CjSourceElement
            this.name = name
        }
    }

    @Suppress("UNUSED_PARAMETER")
    protected fun buildErrorExpression(source: AbstractCjSourceElement? = null, reason: String): CfirErrorExpression {
        return buildErrorExpressionNode {
            this.source = source as? CjSourceElement
            this.diagnostic = object : ConeDiagnostic {
                override val reason: String = reason
            }
        }
    }

    protected fun buildImplicitTypeRef(): CfirTypeRef {
        return buildImplicitTypeRefNode()
    }
}
