package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjTypeParameterListOwner
import org.cangnova.cangjie.psi.CjClassLikeDeclaration as CjPsiClassLikeDeclaration
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.toCjPsiSourceElement

/**
 * CJMapping（Java）语义检查器
 *
 * 对齐 C++ sema_cjmapping_* 系列
 *
 * 注册为 classLikeCheckers
 */
object CfirCJMappingChecker : CfirClassLikeChecker() {
    /**
     * CJMapping 注解名。
     */
    private val CJ_MAPPING = Name.identifier("CJMapping")

    /**
     * 检查带 `@CJMapping` 的声明及其成员类型限制。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (!declaration.hasAnnotation(CJ_MAPPING)) return

        if (declaration is CfirStruct) {
            if (declaration.typeParameters.isNotEmpty()) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.CJMAPPING_STRUCT_GENERIC_NOT_SUPPORTED,
                    a = declaration.typeParameters.joinToString { it.name.asString() },
                )
            }
            if (declaration.superTypeRefs.isNotEmpty()) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.CJMAPPING_STRUCT_INHERITANCE_INTERFACE_NOT_SUPPORTED,
                )
            }
        }

        // 不支持的声明类型检查（enum 不能作为 CJMapping）
        if (declaration is CfirEnum) {
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.CJMAPPING_DECL_NOT_SUPPORTED,
                a = "enum",
            )
        }

        // 成员函数参数和返回类型检查
        for (member in declaration.declarations) {
            if (member !is CfirNamedFunction) continue

            // 检查函数参数类型
            for (param in member.valueParameters) {
                val paramType = (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
                if (paramType !is ConePrimitiveType && paramType !is ConeClassLikeType) {
                    reporter.reportOn(
                        source = param.source ?: member.source ?: declaration.source,
                        factory = CfirErrors.CJMAPPING_METHOD_ARG_NOT_SUPPORTED,
                    )
                }
            }

            // 检查函数返回类型
            val returnType = (member.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            if (returnType != null && !returnType.isUnit && returnType !is ConePrimitiveType && returnType !is ConeClassLikeType) {
                val containerKind = when (declaration) {
                    is CfirClass -> "class"
                    is CfirStruct -> "struct"
                    is CfirInterface -> "interface"
                    else -> "type"
                }
                reporter.reportOn(
                    source = member.returnTypeRef.source ?: member.source ?: declaration.source,
                    factory = CfirErrors.CJMAPPING_METHOD_RET_UNSUPPORTED,
                    a = returnType,
                    b = containerKind,
                )
            }
        }
    }
}

/**
 * ObjC CJMapping 语义检查器
 *
 * 注册为 classLikeCheckers
 */
object CfirObjCCJMappingChecker : CfirClassLikeChecker() {
    /**
     * ObjC CJMapping 注解名。
     */
    private val OBJC_CJ_MAPPING = Name.identifier("ObjCCJMapping")

    /**
     * 检查带 `@ObjCCJMapping` 的声明是否包含非法继承或泛型参数。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (!declaration.hasAnnotation(OBJC_CJ_MAPPING)) return

        if (declaration.superTypeRefs.isNotEmpty()) {
            reporter.reportOn(
                source = declaration.nameDiagnosticSource(),
                factory = CfirErrors.OBJC_CJMAPPING_INHERITANCE_INTERFACE_NOT_SUPPORTED,
            )
        }

        val typeParams = when (declaration) {
            is CfirClass -> declaration.typeParameters
            is CfirStruct -> declaration.typeParameters
            is CfirEnum -> declaration.typeParameters
            is CfirInterface -> declaration.typeParameters
            else -> emptyList()
        }
        if (typeParams.isNotEmpty()) {
            reporter.reportOn(
                source = declaration.nameDiagnosticSource(includeTypeParameters = true),
                factory = CfirErrors.OBJC_CJMAPPING_GENERIC_NOT_SUPPORTED,
                a = typeParams.joinToString { it.name.asString() },
            )
        }
    }
}

/**
 * 取得 class-like 声明名称或名称加类型参数的诊断范围。
 */
private fun CfirClassLikeDeclaration.nameDiagnosticSource(
    includeTypeParameters: Boolean = false,
): AbstractCjSourceElement? {
    source?.psi?.let { psi ->
        val classLikePsi = when (psi) {
            is CjPsiClassLikeDeclaration -> psi
            else -> PsiTreeUtil.getParentOfType(psi, CjPsiClassLikeDeclaration::class.java, false)
                ?: PsiTreeUtil.findChildOfType(psi, CjPsiClassLikeDeclaration::class.java)
        }
        classLikePsi?.classLikeNameSource(includeTypeParameters)?.let { return it }
    }
    return (source as? CjSourceElement)?.findClassLikeNameSource(symbol.name, includeTypeParameters) ?: source
}

/**
 * 从 PSI class-like 声明中取得名称诊断范围。
 */
private fun CjPsiClassLikeDeclaration.classLikeNameSource(
    includeTypeParameters: Boolean,
): AbstractCjSourceElement? {
    val nameIdentifier = nameIdentifier ?: return null
    if (!includeTypeParameters) return nameIdentifier.toCjPsiSourceElement()

    val typeParameterList = (this as? CjTypeParameterListOwner)?.typeParameterList
        ?: return nameIdentifier.toCjPsiSourceElement()
    return CjOffsetsOnlySourceElement(
        startOffset = nameIdentifier.textRange.startOffset,
        endOffset = typeParameterList.textRange.endOffset,
    )
}

/**
 * 从轻量 AST 源码中查找 class-like 名称诊断范围。
 */
private fun CjSourceElement.findClassLikeNameSource(
    name: Name,
    includeTypeParameters: Boolean,
): AbstractCjSourceElement? {
    val tokens = mutableListOf<LighterASTNode>()

    fun collectLeaves(node: LighterASTNode) {
        val children = treeStructure.children(node)
        if (children.isEmpty()) {
            tokens += node
            return
        }
        children.forEach(::collectLeaves)
    }

    collectLeaves(lighterASTNode)

    for ((index, token) in tokens.withIndex()) {
        if (token.tokenType !in classLikeDeclarationKeywords) continue
        val nameToken = tokens.asSequence()
            .drop(index + 1)
            .firstOrNull { it.tokenType == CjTokens.IDENTIFIER && treeStructure.toString(it).toString() == name.asString() }
            ?: continue
        if (includeTypeParameters) {
            val endToken = tokens.asSequence()
                .drop(tokens.indexOf(nameToken) + 1)
                .takeWhile { it.tokenType != CjTokens.LTCOLON && it.tokenType != CjTokens.LBRACE }
                .filter { treeStructure.toString(it).toString().isNotBlank() }
                .lastOrNull()
            if (endToken != null && treeStructure.getEndOffset(endToken) > treeStructure.getEndOffset(nameToken)) {
                return CjOffsetsOnlySourceElement(
                    startOffset = treeStructure.getStartOffset(nameToken),
                    endOffset = treeStructure.getEndOffset(endToken),
                )
            }
        }
        return CjOffsetsOnlySourceElement(
            startOffset = treeStructure.getStartOffset(nameToken),
            endOffset = treeStructure.getEndOffset(nameToken),
        )
    }

    return null
}

/**
 * 可引入 class-like 声明名称的关键字集合。
 */
private val classLikeDeclarationKeywords = setOf(
    CjTokens.CLASS_KEYWORD,
    CjTokens.STRUCT_KEYWORD,
    CjTokens.INTERFACE_KEYWORD,
    CjTokens.ENUM_KEYWORD,
)

/**
 * 读取轻量 AST 节点的子节点列表。
 */
private fun FlyweightCapableTreeStructure<LighterASTNode>.children(
    node: LighterASTNode,
): List<LighterASTNode> {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.filterNotNull().orEmpty()
}
