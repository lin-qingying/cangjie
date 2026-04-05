package org.cangnova.cangjie.cfir.builder

import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression as buildErrorExpressionNode
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference as buildNamedReferenceNode
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef as buildImplicitTypeRefNode
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
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

    protected val inLocalContext: Boolean
        get() = context.inLocalContext

    protected fun pushContainerSymbol(symbol: CfirSymbol<*>) = context.pushContainerSymbol(symbol)

    protected fun popContainerSymbol(symbol: CfirSymbol<*>) = context.popContainerSymbol(symbol)

    protected val containerSymbolIfAny: CfirSymbol<*>?
        get() = context.containerSymbolIfAny

    protected val containerSymbol: CfirSymbol<*>
        get() = context.containerSymbol

    protected inline fun <R> withContainerSymbol(symbol: CfirSymbol<*>, block: () -> R): R {
        pushContainerSymbol(symbol)
        return try {
            block()
        } finally {
            popContainerSymbol(symbol)
        }
    }

    protected fun callableIdFor(name: Name): CallableId {
        if (context.inLocalContext) return CallableId(name)

        val containingClass = containerSymbolIfAny as? CfirClassLikeSymbol<*>
        return if (containingClass != null) {
            CallableId(containingClass.classId, name)
        } else {
            CallableId(packageFqName, name)
        }
    }

    /**
     * 仓颉只有顶层 class-like 声明具备稳定的 `ClassId`。
     * 一旦位于局部作用域或另一个 class-like 容器内，就不应再构造 `ClassId`。
     */
    protected fun canDeclareTopLevelClassLike(): Boolean {
        return !context.inLocalContext && containerSymbolIfAny !is CfirClassLikeSymbol<*>
    }

    /**
     * 为顶层 class-like 声明创建 `ClassId`。
     * 调用方必须先通过 [canDeclareTopLevelClassLike] 校验语境。
     */
    protected fun topLevelClassId(name: Name): ClassId {
        check(canDeclareTopLevelClassLike()) {
            "Only top-level class-like declarations may have a ClassId in Cangjie: $name"
        }
        return ClassId(packageFqName, name)
    }

    protected fun currentDispatchReceiverType(): ConeSimpleCangJieType? {
        if (context.inLocalContext) return null

        val containingClass = containerSymbolIfAny as? CfirClassLikeSymbol<*> ?: return null
        return when (containingClass) {
            is CfirInterfaceSymbol -> ConeClassLikeType(containingClass.classId.toLookupTag(), isInterface = true)
            is CfirStructSymbol -> ConeStructType(containingClass.classId.toLookupTag())
            is CfirEnumSymbol -> ConeEnumType(
                containingClass.classId.toLookupTag(),
                isRefEnum = containingClass.isRefEnum,
            )
            else -> ConeClassLikeType(containingClass.classId.toLookupTag())
        }
    }



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
        isRedef: Boolean = false,
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
        status.isRedef = isRedef
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
