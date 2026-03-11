package org.cangjie.cfir.builder

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.cangjie.cfir.common.CfirRealSourceElement
import org.cangjie.cfir.common.CfirSourceElement
import org.cangjie.cfir.declarations.*
import org.cangjie.cfir.expressions.*
import org.cangjie.cfir.patterns.*
import org.cangjie.cfir.references.CfirNamedReference
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.types.*
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*

/**
 * PSI → Raw CFIR 构建器（对齐 Kotlin 的 PsiRawFirBuilder）。
 *
 * 遍历 PSI 语法树，生成 Raw CFIR 中间表示。
 *
 * 在 RAW_CFIR 阶段：
 * - 所有类型引用为 CfirUserTypeRef（未解析）
 * - 所有符号引用为 CfirNamedReference（未绑定）
 * - 不做类型推断、重载解析（那是 CFIR_RESOLVE 的工作）
 */
class PsiRawCfirBuilder(
    session: CfirSession,
    private val bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
) : AbstractRawCfirBuilder<PsiElement>(session) {

    // ===== AbstractRawCfirBuilder 抽象方法实现 =====

    override fun PsiElement.toSourceElement(): CfirSourceElement {
        val range = textRange
        val filePath = (containingFile as? CjFile)?.virtualFile?.path
        return CfirRealSourceElement(
            startOffset = range.startOffset,
            endOffset = range.endOffset,
            filePath = filePath,
        )
    }

    override fun PsiElement.elementType(): IElementType = node.elementType

    override fun PsiElement.asText(): String = text

    // ===== Public API =====

    /**
     * 构建 CfirFile（文件级入口点）。
     */
    fun buildCfirFile(file: CjFile): CfirFile {
        context.packageFqName = file.packageDirective?.fqName ?: FqName.ROOT
        val packageDirective = buildPackageDirective(file.packageDirective)
        val imports = buildImports(file)
        val declarations = file.declarations.map { visitor.convertDeclaration(it) }

        return CfirFile(
            source = file.toSourceElement(),
            moduleData = baseModuleData,
            name = file.name,
            packageDirective = packageDirective,
            imports = imports,
            declarations = declarations.toMutableList(),
        )
    }

    // ===== Visitor（内部类，对齐 Kotlin 的 PsiRawFirBuilder.Visitor） =====

    private val visitor = Visitor()

    /**
     * Visitor 负责 PSI 节点到 CFIR 节点的分发转换。
     *
     * 对齐 Kotlin 的 PsiRawFirBuilder.Visitor : KtVisitor<FirElement, FirElement?>
     */
    inner class Visitor {

        // ===== 声明转换 =====

        fun convertDeclaration(psi: CjDeclaration): CfirDeclaration = when (psi) {
            is CjClass -> convertClass(psi, CfirClassKind.CLASS)
            is CjInterface -> convertClass(psi, CfirClassKind.INTERFACE)
            is CjStruct -> convertClass(psi, CfirClassKind.STRUCT)
            is CjEnum -> convertClass(psi, CfirClassKind.ENUM)
            is CjExtend -> convertExtend(psi)
            is CjNamedFunction -> convertFunction(psi)
            is CjProperty -> convertProperty(psi)
            is CjFieldVariable -> convertFieldVariable(psi)
            is CjPatternVariable -> convertPatternVariable(psi)
            is CjPrimaryConstructor -> convertConstructor(psi, isPrimary = true)
            is CjSecondaryConstructor -> convertConstructor(psi, isPrimary = false)
            is CjTypeAlias -> convertTypeAlias(psi)
            else -> CfirProperty(
                source = psi.toSourceElement(),
                moduleData = baseModuleData,
                returnTypeRef = CfirImplicitTypeRef.INSTANCE,
                name = Name.special("<error-declaration>"),
            )
        }

        private fun convertClass(psi: CjClassLikeDeclaration, classKind: CfirClassKind): CfirClass {
            val name = psi.nameAsSafeName
            return CfirClass(
                source = psi.toSourceElement(),
                origin = CfirDeclarationOrigin.Source,
                moduleData = baseModuleData,
                status = convertDeclarationStatus(psi),
                typeParameters = convertTypeParameters(psi),
                superTypeRefs = convertSuperTypeRefs(psi).toMutableList(),
                declarations = convertClassMembers(psi).toMutableList(),
                name = name,
                classKind = classKind,
            )
        }

        private fun convertExtend(psi: CjExtend): CfirExtend {
            val extendedTypeRef = convertTypeRef(psi.receiverTypeReceiver)
            val superTypes = psi.superTypeListEntries.map { convertTypeRef(it.typeReference) }
            val members = psi.body?.declarations?.map { convertDeclaration(it) } ?: emptyList()

            return CfirExtend(
                source = psi.toSourceElement(),
                origin = CfirDeclarationOrigin.Source,
                moduleData = baseModuleData,
                status = convertDeclarationStatus(psi),
                typeParameters = convertTypeParameters(psi),
                extendedTypeRef = extendedTypeRef,
                superTypeRefs = superTypes.toMutableList(),
                declarations = members.toMutableList(),
            )
        }

        fun convertFunction(psi: CjNamedFunction): CfirFunction {
            val name = psi.nameAsSafeName
            val returnTypeRef = convertTypeRef(psi.typeReference)
            val valueParams = psi.valueParameters.map { convertValueParameter(it) }
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                psi.bodyBlockExpression?.let { convertBlock(it) }
            }

            return CfirFunction(
                source = psi.toSourceElement(),
                origin = CfirDeclarationOrigin.Source,
                moduleData = baseModuleData,
                status = convertDeclarationStatus(psi),
                typeParameters = convertFunctionTypeParameters(psi),
                returnTypeRef = returnTypeRef,
                name = name,
                valueParameters = valueParams,
                body = body,
                isMut = psi.isMut,
            )
        }

        private fun convertProperty(psi: CjProperty): CfirProperty {
            val name = psi.nameAsSafeName
            val typeRef = convertTypeRef(psi.typeReference)
            val initializer = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                psi.initializer?.let { convertExpression(it) }
            }

            return CfirProperty(
                source = psi.toSourceElement(),
                origin = CfirDeclarationOrigin.Source,
                moduleData = baseModuleData,
                status = convertDeclarationStatus(psi),
                returnTypeRef = typeRef,
                name = name,
                initializer = initializer,
                isVar = psi.isVar,
            )
        }

        private fun convertFieldVariable(psi: CjFieldVariable): CfirVariable {
            return CfirVariable(
                source = psi.toSourceElement(),
                origin = CfirDeclarationOrigin.Source,
                moduleData = baseModuleData,
                status = convertDeclarationStatus(psi),
                returnTypeRef = convertTypeRef(psi.typeReference),
                name = psi.nameAsSafeName,
                initializer = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                    null
                } else {
                    psi.initializer?.let { convertExpression(it) }
                },
                isVar = psi.isVar,
            )
        }

        private fun convertPatternVariable(psi: CjPatternVariable): CfirPatternVariable {
            return CfirPatternVariable(
                source = psi.toSourceElement(),
                origin = CfirDeclarationOrigin.Source,
                moduleData = baseModuleData,
                status = convertDeclarationStatus(psi),
                returnTypeRef = convertTypeRef(psi.typeReference),
                pattern = convertCasePattern(psi.pattern),
                initializer = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                    null
                } else {
                    psi.initializer?.let { convertExpression(it) }
                },
                isVar = psi.isVar,
            )
        }

        private fun convertConstructor(psi: CjConstructor<*>, isPrimary: Boolean): CfirConstructor {
            val valueParams = psi.valueParameters.map { convertValueParameter(it) }
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                psi.bodyBlockExpression?.let { convertBlock(it) }
            }

            return CfirConstructor(
                source = psi.toSourceElement(),
                origin = CfirDeclarationOrigin.Source,
                moduleData = baseModuleData,
                status = convertDeclarationStatus(psi),
                returnTypeRef = CfirImplicitTypeRef.INSTANCE,
                valueParameters = valueParams,
                body = body,
                isPrimary = isPrimary,
            )
        }

        private fun convertTypeAlias(psi: CjTypeAlias): CfirTypeAlias {
            val name = psi.nameAsSafeName
            val expandedType = convertTypeRef(psi.getTypeReference())

            return CfirTypeAlias(
                source = psi.toSourceElement(),
                origin = CfirDeclarationOrigin.Source,
                moduleData = baseModuleData,
                status = convertDeclarationStatus(psi),
                typeParameters = convertTypeAliasTypeParameters(psi),
                name = name,
                expandedTypeRef = expandedType,
            )
        }

        // ===== 参数转换 =====

        fun convertValueParameter(psi: CjParameter): CfirValueParameter {
            return CfirValueParameter(
                source = psi.toSourceElement(),
                origin = CfirDeclarationOrigin.Source,
                moduleData = baseModuleData,
                returnTypeRef = convertTypeRef(psi.typeReference),
                name = psi.nameAsSafeName,
                defaultValue = psi.defaultValue?.let { convertExpression(it) },
            )
        }

        private fun convertTypeParameter(psi: CjTypeParameter): CfirTypeParameter {
            val name = Name.identifier(psi.name ?: "<error>")
            val bounds = psi.extendsBound?.let { listOf(convertTypeRef(it)) } ?: emptyList()

            return CfirTypeParameter(
                source = psi.toSourceElement(),
                origin = CfirDeclarationOrigin.Source,
                moduleData = baseModuleData,
                name = name,
                bounds = bounds.toMutableList(),
            )
        }

        // ===== 表达式转换 =====

        fun convertExpression(psi: CjExpression): CfirExpression = when (psi) {
            is CjBlockExpression -> convertBlock(psi)
            is CjConstantExpression -> convertLiteral(psi)
            is CjStringTemplateExpression -> convertStringTemplate(psi)
            is CjBinaryExpression -> convertBinary(psi)
            is CjPrefixExpression -> convertPrefix(psi)
            is CjPostfixExpression -> convertPostfix(psi)
            is CjCallExpression -> convertCall(psi)
            is CjDotQualifiedExpression -> convertDotQualified(psi)
            is CjSafeQualifiedExpression -> convertDotQualified(psi)
            is CjNameReferenceExpression -> convertNameReference(psi)
            is CjIfExpression -> convertIf(psi)
            is CjMatchExpression -> convertMatch(psi)
            is CjForExpression -> convertFor(psi)
            is CjWhileExpression -> convertWhile(psi)
            is CjDoWhileExpression -> convertDoWhile(psi)
            is CjReturnExpression -> convertReturn(psi)
            is CjBreakExpression -> CfirJumpExpression(source = psi.toSourceElement(), kind = CfirJumpKind.BREAK)
            is CjContinueExpression -> CfirJumpExpression(source = psi.toSourceElement(), kind = CfirJumpKind.CONTINUE)
            is CjThrowExpression -> convertThrow(psi)
            is CjTryExpression -> convertTry(psi)
            is CjLambdaExpression -> convertLambda(psi)
            is CjParenthesizedExpression -> psi.expression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Empty parenthesized expression")
            is CjArrayAccessExpression -> convertSubscript(psi)
            is CjCollectionLiteralExpression -> convertArrayLiteral(psi)
            is CjTupleExpression -> convertTupleLiteral(psi)
            is CjIsExpression -> convertTypeCheck(psi)
            is CjThisExpression -> CfirQualifiedAccess(
                source = psi.toSourceElement(),
                calleeReference = buildNamedReference(Name.special("<this>")),
            )
            is CjSuperExpression -> CfirQualifiedAccess(
                source = psi.toSourceElement(),
                calleeReference = buildNamedReference(Name.special("<super>")),
            )
            else -> buildErrorExpression(psi.toSourceElement(), "Unsupported expression: ${psi.javaClass.simpleName}")
        }

        fun convertBlock(psi: CjBlockExpression): CfirBlock {
            if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                return CfirBlock(source = psi.toSourceElement(), statements = mutableListOf())
            }
            val statements = psi.statements.map { stmt ->
                when (stmt) {
                    is CjProperty -> convertLocalVariable(stmt)
                    is CjNamedFunction -> convertFunction(stmt)
                    is CjDeclaration -> convertDeclaration(stmt)
                    else -> convertExpression(stmt)
                }
            }
            return CfirBlock(source = psi.toSourceElement(), statements = statements.toMutableList())
        }

        private fun convertLocalVariable(psi: CjProperty): CfirVariable {
            return CfirVariable(
                source = psi.toSourceElement(),
                origin = CfirDeclarationOrigin.Source,
                moduleData = baseModuleData,
                returnTypeRef = convertTypeRef(psi.typeReference),
                name = psi.nameAsSafeName,
                initializer = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                    null
                } else {
                    psi.initializer?.let { convertExpression(it) }
                },
                isVar = psi.isVar,
            )
        }

        // ---- Literal ----

        private fun convertLiteral(psi: CjConstantExpression): CfirLiteralExpression {
            val text = psi.text
            val elementType = psi.node.elementType
            val (kind, value) = when (elementType) {
                CjTokens.INTEGER_LITERAL -> CfirLiteralKind.INT to text
                CjTokens.FLOAT_LITERAL -> CfirLiteralKind.FLOAT to text
                CjTokens.RUNE_LITERAL -> CfirLiteralKind.RUNE to text
                CjTokens.TRUE_KEYWORD -> CfirLiteralKind.BOOLEAN to true
                CjTokens.FALSE_KEYWORD -> CfirLiteralKind.BOOLEAN to false
                CjTokens.UNIT_LITERAL -> CfirLiteralKind.UNIT to null
                else -> CfirLiteralKind.STRING to text
            }
            return CfirLiteralExpression(source = psi.toSourceElement(), kind = kind, value = value)
        }

        private fun convertStringTemplate(psi: CjStringTemplateExpression): CfirExpression {
            if (!psi.hasInterpolation()) {
                return CfirLiteralExpression(
                    source = psi.toSourceElement(),
                    kind = CfirLiteralKind.STRING,
                    value = psi.stringContent,
                )
            }
            val parts = psi.entries.mapNotNull { entry ->
                when (entry) {
                    is CjStringTemplateEntryWithExpression ->
                        entry.expression?.let { convertExpression(it) }
                    else -> CfirLiteralExpression(
                        source = entry.toSourceElement(),
                        kind = CfirLiteralKind.STRING,
                        value = entry.text,
                    )
                }
            }
            return CfirStringInterpolation(source = psi.toSourceElement(), parts = parts)
        }

        // ---- Binary & Unary ----

        private fun convertBinary(psi: CjBinaryExpression): CfirExpression {
            if (psi is CjRangeExpression) return convertRange(psi)

            val left = psi.left?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing left operand")
            val right = psi.right?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing right operand")
            val opToken = psi.operationToken

            // 赋值
            if (opToken.isAssignmentToken()) {
                if (opToken == CjTokens.EQ) {
                    return CfirAssignment(source = psi.toSourceElement(), lValue = left, rValue = right)
                }
                val opName = opToken.toCompoundAssignName()?.asString() ?: "<error>"
                return CfirAssignment(
                    source = psi.toSourceElement(),
                    lValue = left,
                    rValue = CfirFunctionCall(
                        calleeReference = buildNamedReference(Name.identifier(opName)),
                        explicitReceiver = left,
                        arguments = mutableListOf(right),
                    ),
                )
            }

            // 逻辑/空合/管道
            opToken.toBinaryOpKind()?.let { kind ->
                return CfirBinaryOp(source = psi.toSourceElement(), kind = kind, left = left, right = right)
            }

            // 比较
            opToken.toComparisonOp()?.let { op ->
                return CfirComparisonExpression(source = psi.toSourceElement(), operation = op, left = left, right = right)
            }

            // 可重载运算符 → 函数调用
            val operatorName = opToken.toBinaryName() ?: Name.identifier("<op:$opToken>")
            return CfirFunctionCall(
                source = psi.toSourceElement(),
                calleeReference = buildNamedReference(operatorName),
                explicitReceiver = left,
                arguments = mutableListOf(right),
            )
        }

        private fun convertRange(psi: CjRangeExpression): CfirRangeExpression {
            val start = psi.left?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing range start")
            val end = psi.right?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing range end")
            return CfirRangeExpression(
                source = psi.toSourceElement(),
                start = start,
                end = end,
                isInclusive = psi.operationToken == CjTokens.RANGEEQ,
            )
        }

        private fun convertPrefix(psi: CjPrefixExpression): CfirExpression {
            val base = psi.baseExpression?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing prefix operand")
            val opName = psi.operationToken.toPrefixUnaryName() ?: Name.identifier("<prefix>")
            return CfirFunctionCall(
                source = psi.toSourceElement(),
                calleeReference = buildNamedReference(opName),
                explicitReceiver = base,
            )
        }

        private fun convertPostfix(psi: CjPostfixExpression): CfirExpression {
            val base = psi.baseExpression?.let { convertExpression(it) }
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing postfix operand")
            val opName = psi.operationToken.toPostfixUnaryName() ?: Name.identifier("<postfix>")
            return CfirFunctionCall(
                source = psi.toSourceElement(),
                calleeReference = buildNamedReference(opName),
                explicitReceiver = base,
            )
        }

        // ---- Call & Access ----

        private fun convertCall(psi: CjCallExpression): CfirExpression {
            if (psi is CjSpawnExpression) {
                val lambda = psi.lambdaExpression
                val body = lambda?.bodyExpression?.let { convertBlock(it) } ?: CfirBlock()
                return CfirSpawnExpression(source = psi.toSourceElement(), body = body)
            }

            val callee = psi.calleeExpression
            val arguments = psi.valueArguments.mapNotNull { it.getArgumentExpression()?.let { e -> convertExpression(e) } }
            val typeArgs = psi.typeArguments.map { convertTypeRef(it.typeReference) }
            val lambdaArgs = psi.lambdaArguments.mapNotNull { it.getLambdaExpression()?.let { l -> convertLambda(l) } }
            val allArgs = (arguments + lambdaArgs).toMutableList()

            val (receiver, reference) = resolveCalleeReference(callee)

            return CfirFunctionCall(
                source = psi.toSourceElement(),
                calleeReference = reference,
                explicitReceiver = receiver,
                arguments = allArgs,
                typeArguments = typeArgs,
            )
        }

        private fun resolveCalleeReference(callee: CjExpression?): Pair<CfirExpression?, CfirNamedReference> {
            return when (callee) {
                is CjNameReferenceExpression -> null to CfirNamedReference(
                    source = callee.toSourceElement(),
                    name = callee.referencedNameAsName,
                )
                is CjDotQualifiedExpression -> {
                    val recv = convertExpression(callee.receiverExpression)
                    val selector = callee.selectorExpression
                    val ref = if (selector is CjSimpleNameExpression) {
                        CfirNamedReference(source = selector.toSourceElement(), name = selector.referencedNameAsName)
                    } else {
                        CfirNamedReference(name = Name.identifier("<error>"))
                    }
                    recv to ref
                }
                else -> null to CfirNamedReference(name = Name.identifier(callee?.text ?: "<error>"))
            }
        }

        private fun convertDotQualified(psi: CjQualifiedExpression): CfirExpression {
            val receiver = convertExpression(psi.receiverExpression)
            val selector = psi.selectorExpression
                ?: return buildErrorExpression(psi.toSourceElement(), "Missing selector")

            if (selector is CjCallExpression) {
                val arguments = selector.valueArguments.mapNotNull { it.getArgumentExpression()?.let { e -> convertExpression(e) } }
                val typeArgs = selector.typeArguments.map { convertTypeRef(it.typeReference) }
                val callee = selector.calleeExpression
                val ref = if (callee is CjSimpleNameExpression) {
                    CfirNamedReference(source = callee.toSourceElement(), name = callee.referencedNameAsName)
                } else {
                    CfirNamedReference(name = Name.identifier(callee?.text ?: "<error>"))
                }
                val lambdaArgs = selector.lambdaArguments.mapNotNull { it.getLambdaExpression()?.let { l -> convertLambda(l) } }

                return CfirFunctionCall(
                    source = psi.toSourceElement(),
                    calleeReference = ref,
                    explicitReceiver = receiver,
                    arguments = (arguments + lambdaArgs).toMutableList(),
                    typeArguments = typeArgs,
                )
            }

            if (selector is CjSimpleNameExpression) {
                return CfirPropertyAccess(
                    source = psi.toSourceElement(),
                    calleeReference = CfirNamedReference(source = selector.toSourceElement(), name = selector.referencedNameAsName),
                    explicitReceiver = receiver,
                )
            }

            return buildErrorExpression(psi.toSourceElement(), "Unsupported selector: ${selector.javaClass.simpleName}")
        }

        private fun convertNameReference(psi: CjNameReferenceExpression): CfirQualifiedAccess {
            return CfirQualifiedAccess(
                source = psi.toSourceElement(),
                calleeReference = CfirNamedReference(source = psi.toSourceElement(), name = psi.referencedNameAsName),
            )
        }

        // ---- Control Flow ----

        private fun convertIf(psi: CjIfExpression): CfirIfExpression {
            val condition = psi.condition?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing if condition")
            val thenBranch = psi.then?.let { toBlock(it) } ?: CfirBlock()
            val elseBranch = psi.`else`?.let { convertExpression(it) }

            return CfirIfExpression(
                source = psi.toSourceElement(),
                condition = condition,
                thenBranch = thenBranch,
                elseBranch = elseBranch,
            )
        }

        private fun convertMatch(psi: CjMatchExpression): CfirMatchExpression {
            val subject = psi.subjectExpression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing match subject")
            val branches = psi.entries.map { entry ->
                val pattern = if (entry.isElse) {
                    CfirWildcardPattern(source = entry.toSourceElement())
                } else {
                    val conditions = entry.conditions
                    if (conditions.isEmpty()) {
                        CfirWildcardPattern(source = entry.toSourceElement())
                    } else {
                        val expr = conditions.first().children.filterIsInstance<CjExpression>().firstOrNull()
                        if (expr != null) CfirConstPattern(source = entry.toSourceElement(), expression = convertExpression(expr))
                        else CfirWildcardPattern(source = entry.toSourceElement())
                    }
                }
                val guard = entry.patternGuard?.children?.filterIsInstance<CjExpression>()?.firstOrNull()?.let { convertExpression(it) }
                val body = entry.expression?.let { convertBlock(it) }
                    ?: entry.body?.let { convertBlock(it) }
                    ?: CfirBlock()

                CfirMatchBranch(source = entry.toSourceElement(), pattern = pattern, guard = guard, body = body)
            }

            return CfirMatchExpression(source = psi.toSourceElement(), subject = subject, branches = branches.toMutableList())
        }

        // ---- Loops ----

        private fun convertFor(psi: CjForExpression): CfirForInExpression {
            val loopParam = psi.loopParameter
            val variable = CfirVariable(
                source = loopParam?.toSourceElement(),
                moduleData = baseModuleData,
                returnTypeRef = if (loopParam != null) convertTypeRef(loopParam.typeReference) else CfirImplicitTypeRef.INSTANCE,
                name = loopParam?.nameAsSafeName ?: Name.special("<anonymous>"),
            )
            val iterable = psi.loopRange?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing for-in iterable")
            val body = psi.body?.let { toBlock(it) } ?: CfirBlock()

            return CfirForInExpression(source = psi.toSourceElement(), variable = variable, iterable = iterable, body = body)
        }

        private fun convertWhile(psi: CjWhileExpression): CfirLoopExpression {
            val condition = psi.condition?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing while condition")
            return CfirLoopExpression(source = psi.toSourceElement(), condition = condition, body = psi.body?.let { toBlock(it) } ?: CfirBlock())
        }

        private fun convertDoWhile(psi: CjDoWhileExpression): CfirLoopExpression {
            val condition = psi.condition?.let { convertExpression(it) }
                ?: buildErrorExpression(reason = "Missing do-while condition")
            return CfirLoopExpression(source = psi.toSourceElement(), condition = condition, body = psi.body?.let { toBlock(it) } ?: CfirBlock(), isDoWhile = true)
        }

        // ---- Jump & Exception ----

        private fun convertReturn(psi: CjReturnExpression): CfirReturnExpression {
            return CfirReturnExpression(source = psi.toSourceElement(), result = psi.returnedExpression?.let { convertExpression(it) })
        }

        private fun convertThrow(psi: CjThrowExpression): CfirThrowExpression {
            val exception = psi.thrownExpression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing thrown expression")
            return CfirThrowExpression(source = psi.toSourceElement(), exception = exception)
        }

        private fun convertTry(psi: CjTryExpression): CfirTryExpression {
            val tryBlock = convertBlock(psi.tryBlock)
            val catches = psi.catchClauses.map { clause ->
                val catchParam = clause.catchParameter
                val parameter = CfirValueParameter(
                    source = catchParam?.toSourceElement(),
                    origin = CfirDeclarationOrigin.Source,
                    moduleData = baseModuleData,
                    returnTypeRef = if (catchParam != null) convertTypeRef(catchParam.typeReference) else CfirImplicitTypeRef.INSTANCE,
                    name = catchParam?.name?.let { Name.identifier(it) } ?: Name.special("<error>"),
                )
                val body = clause.catchBody?.let { if (it is CjBlockExpression) convertBlock(it) else CfirBlock() } ?: CfirBlock()
                CfirCatch(source = clause.toSourceElement(), parameter = parameter, body = body)
            }
            val finallyBlock = psi.finallyBlock?.let { section ->
                val expr = section.finalExpression
                if (expr is CjBlockExpression) convertBlock(expr) else null
            }

            return CfirTryExpression(source = psi.toSourceElement(), tryBlock = tryBlock, catches = catches.toMutableList(), finallyBlock = finallyBlock)
        }

        // ---- Lambda ----

        private fun convertLambda(psi: CjLambdaExpression): CfirLambdaExpression {
            val valueParams = psi.valueParameters.map { convertValueParameter(it) }
            val body = if (bodyBuildingMode == BodyBuildingMode.LAZY_BODIES) {
                null
            } else {
                psi.bodyExpression?.let { convertBlock(it) }
            }

            val anonymousFunction = CfirFunction(
                source = psi.toSourceElement(),
                origin = CfirDeclarationOrigin.Source,
                moduleData = baseModuleData,
                status = CfirDeclarationStatus.DEFAULT,
                returnTypeRef = CfirImplicitTypeRef.INSTANCE,
                name = Name.special("<anonymous>"),
                valueParameters = valueParams,
                body = body,
            )
            return CfirLambdaExpression(source = psi.toSourceElement(), anonymousFunction = anonymousFunction)
        }

        // ---- Misc ----

        private fun convertSubscript(psi: CjArrayAccessExpression): CfirSubscriptExpression {
            val receiver = psi.arrayExpression?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing subscript receiver")
            return CfirSubscriptExpression(
                source = psi.toSourceElement(),
                receiver = receiver,
                indices = psi.indexExpressions.map { convertExpression(it) }.toMutableList(),
            )
        }

        private fun convertArrayLiteral(psi: CjCollectionLiteralExpression): CfirArrayLiteral {
            return CfirArrayLiteral(
                source = psi.toSourceElement(),
                elements = psi.innerExpressions.map { convertExpression(it) }.toMutableList(),
            )
        }

        private fun convertTupleLiteral(psi: CjTupleExpression): CfirTupleLiteral {
            return CfirTupleLiteral(
                source = psi.toSourceElement(),
                elements = psi.expressions.map { convertExpression(it) }.toMutableList(),
            )
        }

        private fun convertTypeCheck(psi: CjIsExpression): CfirTypeOperator {
            val argument = psi.leftHandSide?.let { convertExpression(it) }
                ?: buildErrorExpression(psi.toSourceElement(), "Missing is-check operand")
            return CfirTypeOperator(
                source = psi.toSourceElement(),
                operation = CfirTypeOperationKind.IS,
                argument = argument,
                typeRef = convertTypeRef(psi.typeReference),
            )
        }

        private fun convertCasePattern(pattern: CjCasePatternElement?): CfirPattern {
            return when (pattern) {
                is CjBindingPattern -> CfirBindingPattern(
                    source = pattern.toSourceElement(),
                    name = pattern.nameAsSafeName,
                )
                is CjTypePattern -> CfirTypePattern(
                    source = pattern.toSourceElement(),
                    typeRef = convertTypeRef(pattern.typeReference),
                    bindingName = pattern.nameAsName,
                )
                is CjTuplePattern -> CfirTuplePattern(
                    source = pattern.toSourceElement(),
                    elements = pattern.patterns.map { convertCasePattern(it) },
                )
                is CjEnumPattern -> CfirEnumPattern(
                    source = pattern.toSourceElement(),
                    constructorReference = CfirNamedReference(
                        source = pattern.toSourceElement(),
                        name = Name.special(pattern.expression?.text ?: "<enum-pattern>"),
                    ),
                    arguments = pattern.patterns.map { convertCasePattern(it) },
                )
                is CjConstantPattern -> CfirConstPattern(
                    source = pattern.toSourceElement(),
                    expression = pattern.expression?.let { convertExpression(it) }
                        ?: buildErrorExpression(pattern.toSourceElement(), "Missing constant pattern expression"),
                )
                is CjWildcardPattern -> CfirWildcardPattern(pattern.toSourceElement())
                else -> CfirWildcardPattern(pattern?.toSourceElement())
            }
        }

        // ===== 辅助方法 =====

        private fun toBlock(psi: CjExpression): CfirBlock {
            if (psi is CjBlockExpression) return convertBlock(psi)
            return CfirBlock(source = psi.toSourceElement(), statements = mutableListOf(convertExpression(psi)))
        }

        private fun convertTypeRef(psi: CjTypeReference?): CfirTypeRef {
            return psi.toFirOrImplicitTypeRef { it.toSourceElement() }
        }

        private fun convertTypeParameters(psi: CjClassLikeDeclaration): List<CfirTypeParameter> {
            return (psi as? CjTypeParameterListOwner)?.typeParameters?.map { convertTypeParameter(it) } ?: emptyList()
        }

        private fun convertTypeParameters(psi: CjExtend): List<CfirTypeParameter> {
            return (psi as? CjTypeParameterListOwner)?.typeParameters?.map { convertTypeParameter(it) } ?: emptyList()
        }

        private fun convertTypeAliasTypeParameters(psi: CjTypeAlias): List<CfirTypeParameter> {
            return (psi as? CjTypeParameterListOwner)?.typeParameters?.map { convertTypeParameter(it) } ?: emptyList()
        }

        private fun convertFunctionTypeParameters(psi: CjNamedFunction): List<CfirTypeParameter> {
            return psi.typeParameters.map { convertTypeParameter(it) }
        }

        private fun convertSuperTypeRefs(psi: CjClassLikeDeclaration): List<CfirTypeRef> {
            val typeStatement = psi as? CjTypeStatement ?: return emptyList()
            return typeStatement.superTypeListEntries.map { convertTypeRef(it.typeReference) }
        }

        private fun convertClassMembers(psi: CjClassLikeDeclaration): List<CfirDeclaration> {
            val typeStatement = psi as? CjTypeStatement ?: return emptyList()
            return typeStatement.body?.declarations?.map { convertDeclaration(it) } ?: emptyList()
        }

        private fun convertDeclarationStatus(psi: CjDeclaration): CfirDeclarationStatus {
            val owner = psi as? CjModifierListOwner ?: return CfirDeclarationStatus.DEFAULT
            val modifiers = owner.modifierList ?: return CfirDeclarationStatus.DEFAULT

            return buildDeclarationStatus(
                visibility = when {
                    modifiers.hasModifier(CjTokens.PUBLIC_KEYWORD) -> Visibilities.Public
                    modifiers.hasModifier(CjTokens.PRIVATE_KEYWORD) -> Visibilities.Private
                    modifiers.hasModifier(CjTokens.PROTECTED_KEYWORD) -> Visibilities.Protected
                    modifiers.hasModifier(CjTokens.INTERNAL_KEYWORD) -> Visibilities.Internal
                    else -> Visibilities.Public
                },
                isAbstract = modifiers.hasModifier(CjTokens.ABSTRACT_KEYWORD),
                isOpen = modifiers.hasModifier(CjTokens.OPEN_KEYWORD),
                isSealed = modifiers.hasModifier(CjTokens.SEALED_KEYWORD),
                isStatic = modifiers.hasModifier(CjTokens.STATIC_KEYWORD),
                isMut = modifiers.hasModifier(CjTokens.MUT_KEYWORD),
                isOverride = modifiers.hasModifier(CjTokens.OVERRIDE_KEYWORD),
                isOperator = modifiers.hasModifier(CjTokens.OPERATOR_KEYWORD),
                isUnsafe = modifiers.hasModifier(CjTokens.UNSAFE_KEYWORD),
                isForeign = modifiers.hasModifier(CjTokens.FOREIGN_KEYWORD),
            )
        }
    }

    // ===== 文件级构建辅助 =====

    private fun buildPackageDirective(psi: CjPackageDirective?): CfirPackageDirective {
        val fqName = psi?.fqName ?: FqName.ROOT
        return CfirPackageDirective(
            packageFqName = fqName,
            source = psi?.toSourceElement(),
        )
    }

    private fun buildImports(file: CjFile): List<CfirImport> {
        val importDirectives = file.importDirectives
        return importDirectives.flatMap { directive ->
            directive.importItems.mapNotNull { item ->
                val fqName = item.importedFqName ?: return@mapNotNull null
                CfirImport(
                    importedFqName = normalizeImportFqName(fqName),
                    isAllUnder = item.isAllUnder,
                    aliasName = item.aliasName?.let { Name.identifier(it) },
                    source = item.toSourceElement(),
                )
            }
        }
    }

    private fun normalizeImportFqName(fqName: FqName): FqName {
        val segments = fqName.pathSegments().map { it.asString() }
        for (prefixLength in 1..(segments.size / 2)) {
            val firstPrefix = segments.subList(0, prefixLength)
            val secondPrefix = segments.subList(prefixLength, prefixLength * 2)
            if (firstPrefix == secondPrefix) {
                return FqName((firstPrefix + segments.drop(prefixLength * 2)).joinToString("."))
            }
        }
        return fqName
    }

    companion object
}
