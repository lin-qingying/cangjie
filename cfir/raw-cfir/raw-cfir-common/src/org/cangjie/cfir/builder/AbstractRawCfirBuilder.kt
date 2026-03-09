package org.cangjie.cfir.builder

import com.intellij.psi.tree.IElementType
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.common.moduleData
import org.cangjie.cfir.declarations.*
import org.cangjie.cfir.expressions.*
import org.cangjie.cfir.references.CfirNamedReference
import org.cangjie.cfir.session.CfirSession

import org.cangjie.cfir.types.*
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.Name

/**
 * Raw CFIR 构建器抽象基类（对齐 Kotlin 的 AbstractRawFirBuilder<T>）。
 *
 * 使用泛型参数 T 表示源码树节点类型：
 * - T = PsiElement → PsiRawCfirBuilder（psi2cfir）
 * - T = LighterASTNode → LightTreeRawCfirBuilder（light-tree2cfir，未来）
 *
 * 子类需实现抽象方法以适配各自的树遍历方式。
 */
abstract class AbstractRawCfirBuilder<T : Any>(
    val  baseSession: CfirSession,
    val context: Context<T> = Context(),
) {
    val baseModuleData: CfirModuleData = baseSession.moduleData

    // ===== 抽象方法：子类按 T 类型实现 =====

    /** 获取节点的源码位置信息 */
    abstract fun T.toSourceElement(): CfirSourceElement

    /** 获取节点的元素类型 */
    abstract fun T.elementType(): IElementType

    /** 获取节点文本 */
    abstract fun T.asText(): String

    // ===== 共享的构建方法 =====

    /**
     * 构建声明状态（可见性、修饰符等）。
     * 默认实现返回 DEFAULT，子类可按需覆盖。
     */
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

    /**
     * 创建命名引用（未解析）。
     */
    protected fun buildNamedReference(name: Name, source: CfirSourceElement? = null): CfirNamedReference {
        return CfirNamedReference(source = source, name = name)
    }

    /**
     * 创建错误表达式。
     */
    protected fun buildErrorExpression(source: CfirSourceElement? = null, reason: String): CfirErrorExpression {
        return CfirErrorExpression(source = source, reason = reason)
    }

    /**
     * 创建隐式类型引用。
     */
    protected fun buildImplicitTypeRef(): CfirTypeRef {
        return CfirImplicitTypeRef.INSTANCE
    }
}
