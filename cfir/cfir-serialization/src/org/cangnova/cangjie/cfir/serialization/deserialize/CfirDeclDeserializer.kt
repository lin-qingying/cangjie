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

package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.*
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.annotations.CangjieCallingConvention
import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.BuiltInAnnotationDescriptor
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.annotations.CangjieAnnotationOrigin
import org.cangnova.cangjie.annotations.CangjieAnnotationTarget
import org.cangnova.cangjie.annotations.PlatformAnnotationDescriptor
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.containingClassForStaticMemberAttr
import org.cangnova.cangjie.cfir.copy
import org.cangnova.cangjie.cfir.isMemberParameter
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.addDefaultBoundIfNecessary
import org.cangnova.cangjie.cfir.declarations.utils.constructedClassTypeParameterRefs
import org.cangnova.cangjie.cfir.declarations.utils.hasSerializedInitializer
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildFinalizer
import org.cangnova.cangjie.cfir.declarations.builder.buildPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.impl.*
import org.cangnova.cangjie.cfir.declarations.publishInitializationDependencies
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentStatus
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentView
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentViewEntry
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationResolveState
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.resolve.constants.CfirFloatConstantEvalUtils
import org.cangnova.cangjie.cfir.expressions.buildResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.toAnnotationArgumentView
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.expressions.builder.buildAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOpKind
import org.cangnova.cangjie.cfir.expressions.CfirComparisonOp
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildTypeConversion
import org.cangnova.cangjie.cfir.expressions.builder.buildBinaryOp
import org.cangnova.cangjie.cfir.expressions.builder.buildComparisonExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildIfExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTupleLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildUnsafeExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildInoutArgumentExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildArrayLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildExpressionStub
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.impl.CfirAnnotationArgumentMappingImpl
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.patterns.builder.*
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirInteropTarget
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.name.OperatorNameConventions.asOperatorName
import java.math.BigInteger
import java.util.IdentityHashMap

/**
 * 声明反序列化器：Decl → CfirDeclaration。
 *
 * 将 .cjo 中的 FlatBuffers Decl 表转换为 CFIR 声明树。
 * 所有反序列化声明的 origin 为 Library，签名与所请求的内容通过同一绑定事务发布。
 */
@OptIn(CfirImplementationDetail::class)
class CfirDeclDeserializer private constructor(
    /** 当前 `.cjo` 包的反序列化上下文、缓存与跨包解析入口。 */
    context: CfirDeserializationContext,
    /** 与当前声明反序列化器配套的类型反序列化器。 */
    private val typeDeserializer: CfirTypeDeserializer,
    /** compiled-file stub 只需要声明签名，不物化字段初始化表达式。 */
    private var loadFieldVariableInitializers: Boolean,
) {
    /** 为 CFIR 符号解析创建完整的声明反序列化器。 */
    constructor(context: CfirDeserializationContext, typeDeserializer: CfirTypeDeserializer) :
        this(context, typeDeserializer, loadFieldVariableInitializers = context.mode == CjoDeserializationMode.FULL)

    private val context = context.canonical()

    /** 当前正在反序列化的 enum owner 上下文。 */
    private data class EnumOwnerContext(
        /** enum 声明的 classId，用于构造 enum constructor callable id 与返回类型。 */
        val classId: ClassId,
        /** enum 声明自身的类型参数，enum constructor 返回类型需要复用这些 symbol。 */
        val typeParameters: List<CfirTypeParameter>,
        /** 是否为 ref enum，用于恢复 [ConeEnumType] 语义。 */
        val isRefEnum: Boolean,
    )

    /** 当前正在反序列化的 class-like owner 上下文。 */
    private data class ClassLikeOwnerContext(
        /** class-like 声明的 classId，用于构造构造器返回类型。 */
        val classId: ClassId,
        /** class-like 声明自身的类型参数，构造器返回类型需要复用这些 symbol。 */
        val typeParameters: List<CfirTypeParameterRef>,
        /** 构造器的语义结果是拥有者 self；独立于 FuncBody.retType 的语法槽。 */
        val selfType: ConeCangJieType,
    )

    /** 当前包完整包名。 */
    private val packageFqName: FqName = FqName(context.header.fullPkgName)
    private val bodyDeserializer by lazy { CfirBodyDeserializer(context, this) }

    /** 内容 helper 回调同一 declaration owner；借用目标只读取 header，policy 在退出时恢复。 */
    internal fun <T> withSourceDeclarationHeaders(action: () -> T): T {
        val previous = loadFieldVariableInitializers
        loadFieldVariableInitializers = false
        return try { action() } finally { loadFieldVariableInitializers = previous }
    }
    init {
        context.registerInitializationRestorer { completedOwners ->
            CfirDeclDeserializer(context, context.createTypeDeserializer(), loadFieldVariableInitializers = false)
                .restorePackageInitializationDependencies(completedOwners)
        }
    }
    /** 当前调用栈正在反序列化的 `allDecls` 索引集合，用于避免声明递归重入。 */
    private val declsUnderDeserialization = HashSet<Int>()
    private val canonicalOwnersInProgress = HashSet<Int>()
    /** enum owner 栈，用于 enum constructor 和 enum 成员反序列化。 */
    private val enumOwnerStack = mutableListOf<EnumOwnerContext>()
    /** class-like owner 栈，用于构造器返回类型和成员 callableId 构造。 */
    private val classLikeOwnerStack = mutableListOf<ClassLikeOwnerContext>()
    /**
     * 记录“当前正在为哪个声明反序列化子声明”。
     *
     * `GenericParamDecl` / `FuncParam` 在 FlatBuffers 中是独立 Decl，
     * 但它们的 CFIR 语义必须绑定到真实宿主声明符号，不能退化为自引用。
     */
    private val containingDeclarationSymbolStack = mutableListOf<CfirBasedSymbol<*>>()
    /** SOURCE 定义位置由真实 Block/Pattern 拥有槽传入，不由外围词法 owner 的种类推算。 */
    private var sourceDeclarationIsLocal: Boolean = false

    /**
     * 反序列化指定索引的声明。
     * @param declIndex allDecls 中的索引（0-based）
     */
    /**
     * 记录反序列化声明的原始 CJO 源码位置，供 common-side 报告和候选 related-information 共用。
     * 各位置分别按自己的包与文件索引解码，不将声明起点的文件套用于名称锚点。
     */
    private fun recordCjoDeclarationPosition(
        declarationIndex: Int,
        decl: PackageFormat.Decl,
        result: CfirDeclaration,
    ) {
        context.sourcePath?.let { sourcePath ->
            result.cjoDeclarationOrigin = CfirCjoDeclarationOrigin(
                packagePath = sourcePath.toAbsolutePath().normalize().toString(),
                declarationIndex = declarationIndex,
                isTopLevel = decl.isTopLevel,
            )
        }
        val position = CfirCjoDeclarationPosition(
            declarationLocation = context.sourceLocation(decl.begin),
            endLocation = context.sourceLocation(decl.end),
            identifierLocation = context.sourceLocation(decl.identifierPos),
        )
        result.cjoDeclarationPosition = position
        result.cjoDeclarationSourceElement = CfirCjoDeclarationSourceElement(position, result.symbol)
    }

    fun deserializeDecl(declIndex: Int): CfirDeclaration? = context.materialize {
        if (declIndex !in 0 until context.pkg.allDeclsLength) return@materialize null
        context.headerRecordOrNull(declIndex) // 重复查询保留首次 wire failure 和真实索引。
        check(!context.isNormalReferenceInitializerPending(declIndex)) {
            "CJO declaration still has a pending normal initializer: $declIndex"
        }
        val declaration = CfirSourceGraphLoader(context).headerDeclaration(declIndex)
        ensureNormalHeaderFamily((declIndex + 1).toUInt(), loadFieldVariableInitializers)
        declaration
    }

    fun deserializeSymbol(declIndex: Int): CfirBasedSymbol<*>? =
        deserializeDecl(declIndex)?.wireSymbol(declIndex)

    /** SOURCE 借用者只取其授权 header；普通 FULL 读者在共享事务中完成实际变量的 initializer。 */
    internal fun deserializeSymbolForReference(declIndex: Int): CfirBasedSymbol<*>? = context.materialization.read {
        val sourceSelection = context.currentSourceSelection
        val previouslyBound = if (sourceSelection == null) context.headerRecordOrNull(declIndex) else null
        val declaration = previouslyBound?.declarationRoot ?: deserializeCanonicalDeclaration(declIndex) ?: return@read null
        val wire = declaration.wireSymbol(declIndex)
        if (sourceSelection == null && loadFieldVariableInitializers && declaration is CfirPatternVariable &&
            context.pkg.allDecls(declIndex)?.kind == DeclKind.VarDecl && declaration.hasSerializedInitializer &&
            context.publishedContentSource.snapshot().initializer(declaration) == null
        ) {
            context.enqueueNormalReferenceInitializer(declIndex) {
                context.materialize {
                    ensureNormalHeaderFamily((declIndex + 1).toUInt(), includeInitializers = true)
                }
            }
        }
        wire
    }

    private fun CfirDeclaration.wireSymbol(declIndex: Int): CfirBasedSymbol<*> =
        cjoWireDeclaration(context.pkg.allDecls(declIndex)?.kind).symbol

    /** 正常header按真实owning槽选择自己的参数、TP、成员及helper；引用的classifier不获得内容授权。 */
    internal fun deserializeHeaderContents(declaration: CfirDeclaration) = context.materialize {
        check(context.isPublishedDeclaration(declaration))
        withSourceDeclarationHeaders {
            val index = context.declarations.entries.singleOrNull { it.value === declaration }?.key
            if (index != null) ensureNormalHeaderFamily((index + 1).toUInt())
            else {
                check(declaration is CfirAnonymousFunction && context.sourceFunctions.values.any { it === declaration })
                declaration.valueParameters.forEach { parameter ->
                    val parameterIndex = context.declarations.entries.single { it.value === parameter }.key
                    ensureNormalHeaderFamily((parameterIndex + 1).toUInt())
                }
            }
        }
    }

    /** normal全header产品的owning范围，与仅metadata或单个内容槽分开。 */
    private fun ensureNormalHeaderFamily(root: UInt, includeInitializers: Boolean = false) {
        val registry = context.sourceHeaderRegistry
        val children = HashMap<UInt, MutableList<UInt>>()
        registry.declarations.values.forEach { entry ->
            val owner = when (val parent = entry.parent) {
                is CjoSourceOwningParent.Declaration -> parent.reference
                is CjoSourceOwningParent.ParameterList -> registry.functions.getValue(parent.function).declaration
                is CjoSourceOwningParent.Function -> registry.functions.getValue(parent.identity).declaration
                else -> null
            }
            if (owner != null) children.getOrPut(owner) { mutableListOf() }.add(entry.reference)
        }
        val pending = ArrayDeque<UInt>()
        val visited = HashSet<UInt>()
        pending.add(root)
        while (pending.isNotEmpty()) {
            val reference = pending.removeFirst()
            check(visited.add(reference)) { "Normal header family has conflicting owning paths" }
            val index = reference.toInt() - 1
            val declaration = context.declarations.getValue(index)
            val wire = registry.declarations.getValue(reference).wire
            if (declaration is CfirValueParameter && wire.infoType == DeclInfo.ParamInfo) {
                val defaultReference = (wire.info(ParamInfo()) as ParamInfo).defaultVal
                check(defaultReference == 0u || defaultReference <= context.pkg.allExprsLength.toUInt()) {
                    "Required CJO parameter default has an invalid saved expression reference: $defaultReference"
                }
            }
            val needsDefault = declaration is CfirValueParameter && wire.infoType == DeclInfo.ParamInfo &&
                (wire.info(ParamInfo()) as ParamInfo).defaultVal != 0u && declaration !in context.bodyState.defaults
            val needsInitializer = includeInitializers && declaration is CfirVariable &&
                declaration !is CfirValueParameter && wire.infoType == DeclInfo.VarInfo &&
                (wire.info(VarInfo()) as VarInfo).initializer.isSavedInitializer() &&
                declaration !in context.bodyState.initializers
            if (needsDefault || needsInitializer || declaration !in context.bodyState.annotationContents) {
                val roots = buildList {
                    add(CjoSourceContentRoot.DeclarationHeader(reference))
                    if (needsDefault) add(CjoSourceContentRoot.ParameterDefault(reference))
                    if (needsInitializer) add(CjoSourceContentRoot.Initializer(reference))
                    if (declaration !in context.bodyState.annotationContents) add(CjoSourceContentRoot.DeclarationAnnotations(reference))
                }
                val selection = CjoSourceOwnershipPlan.request(context.sourceHeaderRegistry, roots)
                context.registerSourceSelection(selection)
                context.enqueueHeaderContents(declaration, needsInitializer, selection) { includeInitializers ->
                    materializeSavedDeclarationContent(wire, declaration, includeInitializers)
                    if (declaration !in context.bodyState.annotationContents) {
                        val annotationOwner = declaration.wireSymbol(index)
                        context.stageAnnotations(declaration, materializeAnnotations(wire, reference, annotationOwner))
                        context.enqueueDeclarationMetadata(declaration) { completeDeclarationMetadata(index, declaration) }
                    }
                }
            }
            children[reference]?.forEach(pending::addLast)
        }
    }

    /** 正式单 e 操作入口；引用所属域来自本包拥有索引，实际普通节点由配套正文 reader 完成。 */
    internal fun deserializeParameterDefaults(indices: List<Int>): Map<CfirValueParameter, CfirExpression?> = context.materialize {
        indices.associate { index ->
            val parameter = CfirSourceGraphLoader(context).headerDeclaration(index) as CfirValueParameter
            parameter to bodyDeserializer.deserializeDefault(parameter)
        }
    }

    /** 已登记保存槽的普通表达式请求，绝不恢复冷裸e的拥有者。 */
    fun deserializeSourceExpression(expressionReference: UInt): CfirExpression = context.materialize {
        require(expressionReference != 0u && expressionReference <= context.pkg.allExprsLength.toUInt())
        val occurrence = context.sourceHeaderRegistry.expressionContexts.getValue(expressionReference).single()
        bodyDeserializer.deserializeExpression(expressionReference, CjoSourceContentRoot.SelectedExpression(occurrence))
    }

    /** 显式正文请求复用原 header、参数及 symbol；完成后普通 visitor 读取同一声明的真实 body。 */
    fun deserializeFunctionBody(declIndex: Int): CfirBlock? = context.materialize {
        val function = CfirSourceGraphLoader(context).headerDeclaration(declIndex) as CfirCallableDeclaration
        check(function is CfirFunction || function is CfirEnumConstructor)
        bodyDeserializer.deserializeBody(function)
    }

    /** 已发表的真实 F 可没有 d 下标（匿名函数）；请求仍由原包的配套正文 helper 完成。 */
    internal fun deserializeFunctionBody(function: CfirCallableDeclaration): CfirBlock? = context.materialize {
        check((function is CfirFunction || function is CfirEnumConstructor) && context.isPublishedDeclaration(function))
        bodyDeserializer.deserializeBody(function)
    }

    /** 显式 initializer 请求与正文共用同代事务，已有 Library header 不通过普通 setter 改写。 */
    fun deserializeVariableInitializer(declIndex: Int): CfirExpression? = context.materialize {
        val variable = CfirSourceGraphLoader(context).headerDeclaration(declIndex) as CfirVariable
        check(variable !is CfirValueParameter)
        bodyDeserializer.deserializeInitializer(variable)
    }

    internal fun deserializeVariableInitializer(variable: CfirVariable): CfirExpression? = context.materialize {
        if (context.mode == CjoDeserializationMode.SIGNATURE_ONLY) return@materialize null
        val owner = checkNotNull(context.publishedDeclarationRoot(variable)) as CfirVariable
        check(owner !is CfirValueParameter)
        bodyDeserializer.deserializeInitializer(owner)
    }

    /** FullId 参数/成员入口先恢复真实 owner；不能以顶层方式另造一个参数声明。 */
    internal fun deserializeCanonicalDeclaration(declIndex: Int): CfirDeclaration? {
        if (declIndex !in 0 until context.pkg.allDeclsLength) return null
        if (context.currentSourceSelection == null) return CfirSourceGraphLoader(context).headerDeclaration(declIndex)
        return context.materialize {
            context.requireSourceDeclaration(declIndex)
            context.declarations[declIndex]?.let { return@materialize it }
            context.reservedTypeParameters[declIndex]?.let { return@materialize it }
            check(canonicalOwnersInProgress.add(declIndex)) { "CJO declaration ownership cycle" }
            try {
                val desugarOwner = context.fullIdResolver.resolveParameterDesugarOwnerIndex(declIndex)
                if (desugarOwner != null) {
                    deserializeCanonicalDeclaration(desugarOwner)
                    return@materialize checkNotNull(context.declarations[declIndex]) {
                        "A parameter desugar must be materialized by its actual function header"
                    }
                }
                val parent = context.fullIdResolver.resolveContainingDeclarationIndex(declIndex)
                if (parent == null) {
                    val wire = checkNotNull(context.pkg.allDecls(declIndex))
                    if (wire.isTopLevel) deserializeDecl(declIndex, parameterDefaults = null)
                    else CfirSourceGraphLoader(context).headerDeclaration(declIndex)
                } else {
                    deserializeCanonicalDeclaration(parent)
                    context.declarations[declIndex] ?: context.reservedTypeParameters[declIndex]
                }
            } finally {
                canonicalOwnersInProgress.remove(declIndex)
            }
        }
    }

    internal fun deserializeSourceDeclaration(index: Int, lexicalOwner: CfirDeclaration?, isLocal: Boolean): CfirDeclaration = context.materialize {
        context.declarations[index]?.let { return@materialize it }
        withSourceDeclarationContext(lexicalOwner, isLocal) {
            check(context.pkg.allDecls(index)?.kind != DeclKind.FuncParam) {
                "A SOURCE parameter must be loaded through its actual FuncBody header"
            }
            checkNotNull(deserializeDecl(index, parameterDefaults = null))
        }
    }

    /** F 的参数与 named 函数复用同一完整 header reader，不先发布缺 helper 的参数壳。 */
    internal fun deserializeSourceLambda(identity: CjoSourceFunctionIdentity, body: FuncBody, type: ConeFunctionType, lexicalOwner: CfirDeclaration?): CfirFunction =
        context.materialize {
            context.sourceFunctions[identity]?.let { return@materialize it as CfirAnonymousFunction }
            val symbol = CfirAnonymousFunctionSymbol()
            val function = org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction {
                moduleData = context.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                this.symbol = symbol
                returnTypeRef = buildResolvedTypeRef { coneType = type.returnType }
                typeRef = buildResolvedTypeRef { coneType = type }
                hasExplicitParameterList = true
                isLambda = true
                contentSource = context.publishedContentSource
                this.body = null
            }
            // 实际 F 先登记，参数/default 的合法重入只能取得这一真实 owner；根事务不发布未完成 header。
            check(context.sourceFunctions.putIfAbsent(identity, function) == null)
            withSourceDeclarationContext(lexicalOwner, isLocal = true) {
                function.replaceValueParameters(deserializeFunctionParameters(body, symbol))
            }
            function.markResolved()
            context.registerCallableSignature(function,
                org.cangnova.cangjie.cfir.source.CfirSourceCallableSignature.Resolved(type.returnType))
            CfirSourceGraphLoader(context, this).header(function, body)
            // 原FuncBody没有声明Anno槽；匿名F的真实空内容完成不等同于未选的Decl.annotations。
            context.stageAnnotations(function, emptyList())
            context.publishAnnotations(function, org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Complete(emptyList()))
            function
        }

    /** Pattern 的定义入口和 VarWithPattern header 共用 binding d，不降为普通 field-variable。 */
    internal fun deserializeSourcePatternBinding(index: Int, lexicalOwner: CfirDeclaration?, isLocal: Boolean): CfirPatternBindingVariable =
        context.materialize {
            withSourceDeclarationContext(lexicalOwner, isLocal) {
                val wire = checkNotNull(context.pkg.allDecls(index))
                check(wire.kind == DeclKind.VarDecl)
                val info = wire.info(VarInfo()) as? VarInfo
                deserializePatternBindingVariable(
                    rawDeclRef = (index + 1).toUInt(),
                    outerIsLocal = isLocal,
                )
            }
        }

    /** null 是明确的包级词法环境；定义位置及 class/enum 环境不继承其它请求者的临时栈。 */
    private inline fun <T> withSourceDeclarationContext(owner: CfirDeclaration?, isLocal: Boolean, action: () -> T): T {
        val previousLocal = sourceDeclarationIsLocal
        val previousSymbols = containingDeclarationSymbolStack.toList()
        val previousClasses = classLikeOwnerStack.toList()
        val previousEnums = enumOwnerStack.toList()
        sourceDeclarationIsLocal = isLocal
        containingDeclarationSymbolStack.clear()
        classLikeOwnerStack.clear()
        enumOwnerStack.clear()
        try {
            if (owner == null) return action()
            return withContainingDeclarationSymbol(owner.symbol) {
                if (owner is CfirClassLikeDeclaration) {
                    val index = context.declarations.entries.single { it.value === owner }.key
                    val wire = checkNotNull(context.pkg.allDecls(index))
                    withClassLikeOwner(ClassLikeOwnerContext(owner.symbol.classId, owner.typeParameters,
                        typeDeserializer.deserializeTypeFromField(wire.type))) {
                        if (owner is CfirEnum) withEnumOwner(EnumOwnerContext(owner.symbol.classId,
                            owner.typeParameters, owner.isRefEnum)) { action() }
                        else action()
                    }
                } else if (owner is CfirExtend) {
                    withExtendReceiverType(owner.extendedTypeRef.coneTypeOrNull as? ConeSimpleCangJieType, action)
                } else action()
            }
        } finally {
            sourceDeclarationIsLocal = previousLocal
            containingDeclarationSymbolStack.clear()
            containingDeclarationSymbolStack.addAll(previousSymbols)
            classLikeOwnerStack.clear()
            classLikeOwnerStack.addAll(previousClasses)
            enumOwnerStack.clear()
            enumOwnerStack.addAll(previousEnums)
        }
    }

    /** 按所属 provider 的 package cache、先 materialize owner 再返回 FullId 对应声明。 */
    internal fun deserializeInitializationDependency(
        ownerDeclarationIndex: Int,
        declarationIndex: Int,
    ): CfirDeclaration? = context.materialize {
        val owner = deserializeDecl(ownerDeclarationIndex, parameterDefaults = null) as? CfirClassLikeDeclaration
            ?: return@materialize null
        check(owner.declarations.any { context.declarations[declarationIndex] === it }) {
            "FullId target index $declarationIndex is not a direct member of owner index $ownerDeclarationIndex"
        }
        context.declarations[declarationIndex]
    }

    /** 参数的默认辅助函数必须在其 canonical 节点发布前准备完毕。 */
    private fun deserializeDecl(declIndex: Int, parameterDefaults: CfirParameterDefaultValueMetadata?): CfirDeclaration? =
        context.materialize {
            context.declarations[declIndex]?.let { return@materialize it }
            if (declIndex !in 0 until context.pkg.allDeclsLength) return@materialize null
            if (!declsUnderDeserialization.add(declIndex)) {
                return@materialize context.declarations[declIndex]
            }
            try {
                val decl = try {
                    context.pkg.allDecls(declIndex)
                } catch (_: IndexOutOfBoundsException) {
                    null
                } ?: return@materialize null
                val classLike = decl.kind == DeclKind.ClassDecl || decl.kind == DeclKind.InterfaceDecl || decl.kind == DeclKind.StructDecl ||
                    decl.kind == DeclKind.EnumDecl || decl.kind == DeclKind.TypeAliasDecl || decl.kind == DeclKind.BuiltInDecl
                if (classLike) check(context.classLikeHeadersInProgress.add(declIndex)) { "CJO classifier header reentry" }
                val result = convertDecl(declIndex, decl, parameterDefaults) ?: return@materialize null
                // CJO keeps part of the interop contract in declaration
                // attributes/FuncInfo instead of `Anno`.  Attach those facts
                // before publishing the canonical declaration snapshot so
                // binary consumers follow the same producer as source CFIR.
                check(context.declarations.putIfAbsent(declIndex, result).let { it == null || it === result }) {
                    "A canonical declaration cannot be rebound while completing its header"
                }
                publishDeserializedDeclaration(declIndex, decl, result)
                if (result is CfirClassLikeDeclaration) context.completeClassLikeHeader(declIndex, result)
                context.declarations[declIndex] ?: result
            } finally {
                declsUnderDeserialization.remove(declIndex)
                context.classLikeHeadersInProgress.remove(declIndex)
            }
        }

    /** 普通声明与直接构造的 pattern binding 共用唯一的 pre-publication 接缝。 */
    private fun publishDeserializedDeclaration(index: Int, decl: Decl, result: CfirDeclaration) {
        if (result is CfirFunction && decl.kind == DeclKind.FuncDecl) {
            // 固定 native 只对 named F 的第一参数表建立 ownerFunc；lambda/其它参数组不伪造该字段。
            val first = (decl.info(FuncInfo()) as? FuncInfo)?.funcBody?.let {
                if (it.paramListsLength == 0) null else checkNotNull(it.paramLists(0))
            }
            if (first != null) for (ordinal in 0 until first.paramsLength) {
                val parameter = checkNotNull(context.declarations[checkNotNull(decodeDeclRef(first.params(ordinal)))] as? CfirValueParameter)
                parameter.defaultValueMetadata?.function?.let { helper ->
                    val previous = helper.defaultParameterHelperInfo
                    check(previous == null || previous.owner === result && previous.parameterIndex == ordinal)
                    if (previous == null) {
                        check(context.declCache.values.none { it === helper }) { "A completed helper cannot acquire a new owner" }
                        helper.defaultParameterHelperInfo = CfirDefaultParameterHelperInfo(result, ordinal)
                    }
                }
            }
        }
        publishDeclarationMetadata(index, decl, result)
        recordCjoDeclarationPosition(index, decl, result)
    }

    private fun publishDeclarationMetadata(index: Int, decl: Decl, result: CfirDeclaration) {
        result.isFromCommonPart = testAttr(decl, AttrBit.FROM_COMMON_PART)
        result.hasGenericNode = decl.generic != null
        // CJO 不含 Decl.linkage；IMPORTED 是 reader 已知的来源事实，其余位从实际记录读取。
        result.exportFacts = CfirDeclarationExportFacts(
            isImported = !(result.isFromCommonPart && context.sourcePath?.let(context.cjoManager::isExplicitCommonPartPath) == true) || testAttr(decl, AttrBit.IMPORTED),
            isImplicitlyUsed = testAttr(decl, AttrBit.IMPLICIT_USED),
            isGenericInstantiated = testAttr(decl, AttrBit.GENERIC_INSTANTIATED),
            isInterfaceImplementation = testAttr(decl, AttrBit.INTERFACE_IMPL),
            isInline = if (result is CfirFunction) (decl.info(FuncInfo()) as? FuncInfo)?.isInline == true && context.cjoManager.canInline else false,
            isGlobal = testAttr(decl, AttrBit.GLOBAL),
            isInitialized = testAttr(decl, Attribute.INITIALIZED.ordinal),
            enumConstructorHasParameterList = result is CfirEnumConstructor && decl.kind == DeclKind.FuncDecl,
            hasUnexportedSignature = testAttr(decl, Attribute.UNREACHABLE.ordinal) && decl.type == 0u,
            isToolAdded = testAttr(decl, AttrBit.TOOL_ADD),
            isCompilerAdded = testAttr(decl, Attribute.COMPILER_ADD.ordinal),
            hasInitial = testAttr(decl, Attribute.HAS_INITIAL.ordinal),
            isImplicitlyAdded = testAttr(decl, Attribute.IMPLICIT_ADD.ordinal),
            hasNoReflectInfo = testAttr(decl, Attribute.NO_REFLECT_INFO.ordinal),
        )
        if (result is CfirFunction || result is CfirEnumConstructor) {
            val callable = result as CfirCallableDeclaration
            context.registerCallableSignature(callable, if (result.exportFacts.hasUnexportedSignature)
                org.cangnova.cangjie.cfir.source.CfirSourceCallableSignature.Unexported
            else org.cangnova.cangjie.cfir.source.CfirSourceCallableSignature.Resolved(callable.returnTypeRef.coneType))
            if (decl.kind == DeclKind.FuncDecl) {
                (decl.info(FuncInfo()) as FuncInfo).funcBody?.let { body ->
                    val identity = CjoSourceFunctionIdentity((decl.info(CjoSourceTable()) as CjoSourceTable).tablePosition(4))
                    check(context.sourceFunctions.putIfAbsent(identity, callable).let { it == null || it === callable }) {
                        "A saved FuncBody must retain its first bound callable owner"
                    }
                    CfirSourceGraphLoader(context, this).header(callable, body)
                }
            }
        }
        if (result is CfirCallableDeclaration &&
            (result.status.isStatic || result is CfirConstructor || result is CfirEnumConstructor)
        ) {
            result.initContainingClassAttr()
        }
        result.cjoDeclarationIndex = index
        val wire = result.wireSymbol(index)
        if (wire !== result.symbol) {
            wire.cfir.cjoDeclarationIndex = index
            recordCjoDeclarationPosition(index, decl, wire.cfir)
        }
        context.recordWireHeader(index, result, wire)
        result.serializedDeclarationAttributes = CfirSerializedDeclarationAttributes(
            words = (0 until decl.attributesLength).map(decl::attributes),
        )
        result.serializedInteropFacts = serializedInteropFacts(decl)
        result.serializedAnnotationHeaderInfo = serializedAnnotationInfo(decl)
        if (wire !== result.symbol) {
            val wireOwner = wire.cfir
            wireOwner.isFromCommonPart = result.isFromCommonPart
            wireOwner.hasGenericNode = result.hasGenericNode
            wireOwner.exportFacts = result.exportFacts
            wireOwner.serializedDeclarationAttributes = result.serializedDeclarationAttributes
            wireOwner.serializedInteropFacts = result.serializedInteropFacts
            wireOwner.serializedAnnotationHeaderInfo = result.serializedAnnotationHeaderInfo
        }
        context.enqueueHeaderDependencies()
    }

    /** 标准注解、sidecar及弃用/ABI派生信息共用绑定后的完成阶段，不发布半成品header。 */
    private fun materializeSavedDeclarationContent(decl: Decl, result: CfirDeclaration, includeInitializers: Boolean) {
        if (result is CfirVariable && result !is CfirValueParameter && decl.infoType == DeclInfo.VarInfo && includeInitializers) {
            val reference = (decl.info(VarInfo()) as VarInfo).initializer
            if (reference.isSavedInitializer()) bodyDeserializer.deserializeInitializer(result)
        }
        if (result is CfirValueParameter) {
            if (decl.infoType == DeclInfo.NONE) return
            check(decl.infoType == DeclInfo.ParamInfo) { "FuncParam has an invalid additional-info role" }
            val defaultReference = (decl.info(ParamInfo()) as ParamInfo).defaultVal
            if (defaultReference != 0u) bodyDeserializer.deserializeDefault(result)
        }
    }

    private fun completeDeclarationMetadata(index: Int, result: CfirDeclaration) {
        val wireOwner = result.wireSymbol(index).cfir
        val additional = context.sidecar?.annotations(index, wireOwner).orEmpty()
        val annotations = appendSerializedAnnotationMarker(wireOwner, context.bodyState.annotationDrafts.getValue(result) + additional)
        if (wireOwner is CfirMemberDeclaration) {
            val target = wireOwner.annotationTargetFor()
            annotations.forEach { it.replaceAnnotationTarget(target) }
        }
        val serializedInfo = wireOwner.serializedAnnotationHeaderInfo
        val annotationProduct = if (wireOwner is CfirMemberDeclaration && (annotations.isNotEmpty() || serializedInfo != null))
            wireOwner.computeAnnotationInfo(context.moduleData.session, annotations, serializedInfo) else serializedInfo
        val interopProduct = wireOwner.computeInteropInfo(context.moduleData.session, annotations, annotationProduct)
        context.publishAnnotations(
            result,
            org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Complete(
                annotations, deprecationsProviderFor(annotations), annotationProduct, interopProduct,
            ),
            wireOwner,
        )
    }

    /**
     * 在声明 owner 与 FullId 目标都完成物化后，恢复 common CJO 的实例字段依赖。
     *
     * 先收集所有 source owner 和目标 owner 并完成 class-root materialization，随后才发布依赖边；
     * 这样 sibling 字段使用同一 `declCache` 中由真实 class owner 构造的 canonical symbol。
     */
    private fun restorePackageInitializationDependencies(completedOwners: Set<Int>): Set<Int> {
        if (!context.header.isCjmpCommonPart) return emptySet()

        val localOwnerIndices = linkedSetOf<Int>()
        fun addLocalOwner(declarationIndex: Int) {
            val declaration = context.pkg.allDecls(declarationIndex) ?: return
            if (declaration.isClassLikeCjoDeclaration()) {
                localOwnerIndices += declarationIndex
            } else {
                context.fullIdResolver.resolveContainingClassDeclarationIndex(declarationIndex)
                    ?.let(localOwnerIndices::add)
            }
        }
        val requestedIndices = checkNotNull(context.currentSourceSelection).borrowedContexts.declarations.keys.map { it.toInt() - 1 }
        requestedIndices.forEach(::addLocalOwner)

        data class ImportedTarget(val sourcePath: java.nio.file.Path?, val packageName: String, val ownerIndex: Int, val declarationIndex: Int)
        val importedTargets = linkedSetOf<ImportedTarget>()
        // 恢复只能消费本次已绑定header及其声明的FullId需求，不能发现未请求的整包owner。
        for (declarationIndex in requestedIndices) {
            val declaration = context.pkg.allDecls(declarationIndex) ?: continue
            if (declaration.dependenciesLength == 0) continue
            // Decl.dependencies is shared by CJO declarations such as functions and static fields.
            // Only common instance fields feed the CFIR instance-initialization order graph.
            if (declaration.kind != DeclKind.VarDecl ||
                !testAttr(declaration, AttrBit.FROM_COMMON_PART) ||
                testAttr(declaration, AttrBit.STATIC)
            ) continue
            val sourceOwnerIndex = context.fullIdResolver.resolveContainingClassDeclarationIndex(declarationIndex)
                ?: continue
            if (sourceOwnerIndex in completedOwners) continue
            localOwnerIndices += sourceOwnerIndex
            for (dependencyIndex in 0 until declaration.dependenciesLength) {
                val fullId = requireNotNull(declaration.dependencies(dependencyIndex)) {
                    "CJO dependency vector contains a null FullId (declaration=$declarationIndex)"
                }
                val resolved = context.fullIdResolver.resolve(fullId)
                check(resolved is ResolvedFullId.Declaration) {
                    "Cannot restore initialization dependency: ${context.fullIdResolver.describe(fullId)}"
                }
                if (resolved.declaration.decl.kind != DeclKind.VarDecl) continue
                val targetOwnerIndex = resolved.packageIndex.resolveContainingClassDeclarationIndex(
                    resolved.declaration.zeroBasedIndex,
                ) ?: continue
                if (resolved.source == ResolvedFullId.Declaration.Source.CURRENT_PACKAGE) {
                    localOwnerIndices += targetOwnerIndex
                } else {
                    importedTargets += ImportedTarget(
                        resolved.packageIndex.sourcePath,
                        resolved.packageIndex.packageFqName.asString(),
                        targetOwnerIndex,
                        resolved.declaration.zeroBasedIndex,
                    )
                }
            }
        }

        localOwnerIndices.removeAll(completedOwners)
        // 所有目标 owner 先完成 materialization，恢复阶段不再递归创建 sibling。
        localOwnerIndices.forEach { ownerIndex ->
            deserializeDecl(ownerIndex, parameterDefaults = null)
        }
        for ((sourcePath, packageName, ownerIndex, declarationIndex) in importedTargets) {
            requireNotNull(
                context.deserializeInitializationDependency(sourcePath, packageName, ownerIndex, declarationIndex),
            ) {
                "Cannot materialize canonical CJO dependency target $packageName[$declarationIndex]"
            }
        }

        for (ownerIndex in localOwnerIndices) {
            val ownerDecl = requireNotNull(context.pkg.allDecls(ownerIndex))
            for (memberIndex in ownerDecl.classLikeMemberDeclarationIndices()) {
                val memberDecl = requireNotNull(context.pkg.allDecls(memberIndex))
                if (!testAttr(memberDecl, AttrBit.FROM_COMMON_PART)) continue
                val field = context.declarations[memberIndex] as? CfirFieldVariable ?: continue
                if (field.status.isStatic) continue

                val dependencies = mutableListOf<CfirBasedSymbol<*>>()
                val seen = IdentityHashMap<CfirBasedSymbol<*>, Boolean>()
                for (dependencyIndex in 0 until memberDecl.dependenciesLength) {
                    val fullId = requireNotNull(memberDecl.dependencies(dependencyIndex)) {
                        "CJO dependency vector contains a null FullId (declaration=$memberIndex)"
                    }
                    val resolved = context.fullIdResolver.resolve(fullId)
                    check(resolved is ResolvedFullId.Declaration) {
                        "Cannot restore initialization dependency: ${context.fullIdResolver.describe(fullId)}"
                    }
                    if (resolved.declaration.decl.kind != DeclKind.VarDecl) continue
                    val targetOwnerIndex = resolved.packageIndex.resolveContainingClassDeclarationIndex(
                        resolved.declaration.zeroBasedIndex,
                    ) ?: continue
                    val target = if (resolved.source == ResolvedFullId.Declaration.Source.CURRENT_PACKAGE) {
                        context.declarations[resolved.declaration.zeroBasedIndex] as? CfirFieldVariable
                    } else {
                        context.deserializeInitializationDependency(
                            resolved.packageIndex.sourcePath,
                            resolved.packageIndex.packageFqName.asString(),
                            targetOwnerIndex,
                            resolved.declaration.zeroBasedIndex,
                        ) as? CfirFieldVariable
                    }
                        ?: continue
                    if (target.status.isStatic) continue
                    if (seen.put(target.symbol, true) == null) dependencies += target.symbol
                }
                check(context.declCache.values.none { it === field }) {
                    "Initialization dependency restoration cannot rewrite a completed field"
                }
                field.publishInitializationDependencies(dependencies)
            }
        }
        return localOwnerIndices.toSet()
    }

    private fun Decl.isClassLikeCjoDeclaration(): Boolean = when (kind) {
        DeclKind.ClassDecl, DeclKind.InterfaceDecl, DeclKind.StructDecl, DeclKind.EnumDecl -> true
        else -> false
    }

    private fun Decl.classLikeMemberDeclarationIndices(): List<Int> {
        val (length, getter) = when (kind) {
            DeclKind.ClassDecl -> (info(ClassInfo()) as? ClassInfo)?.let { info -> info.bodyLength to info::body }
            DeclKind.InterfaceDecl -> (info(InterfaceInfo()) as? InterfaceInfo)?.let { info -> info.bodyLength to info::body }
            DeclKind.StructDecl -> (info(StructInfo()) as? StructInfo)?.let { info -> info.bodyLength to info::body }
            DeclKind.EnumDecl -> (info(EnumInfo()) as? EnumInfo)?.let { info -> info.bodyLength to info::body }
            else -> null
        } ?: return emptyList()
        return (0 until length).mapNotNull { index -> decodeDeclRef(getter(index)) }
    }

    /** 按 FlatBuffers [Decl.kind] 分派到具体 CFIR 声明转换器。 */
    private fun convertDecl(index: Int, decl: Decl, parameterDefaults: CfirParameterDefaultValueMetadata?): CfirDeclaration? {
        return when (decl.kind) {
            DeclKind.ClassDecl -> convertClass(index, decl)
            DeclKind.InterfaceDecl -> convertInterface(index, decl)
            DeclKind.StructDecl -> convertStruct(index, decl)
            DeclKind.EnumDecl -> convertEnum(index, decl)
            DeclKind.FuncDecl -> convertFunctionOrEnumConstructor(decl)
            DeclKind.PropDecl -> convertProperty(decl)
            DeclKind.VarDecl -> convertVariableOrEnumConstructor(decl)
            DeclKind.VarWithPatternDecl -> convertVariableWithPattern(decl)
            DeclKind.ExtendDecl -> convertExtend(decl)
            DeclKind.TypeAliasDecl -> convertTypeAlias(index, decl)
            DeclKind.GenericParamDecl -> convertTypeParameter(decl)
            DeclKind.FuncParam -> convertValueParameter(decl, requireNotNull(parameterDefaults) {
                "A FuncParam must be loaded through its owning FuncParamList"
            })
            DeclKind.BuiltInDecl -> convertBuiltIn(index, decl)
            else -> null // InvalidDecl
        }
    }
    // ---- 属性位域解析 ----

    /**
     * common-part `.cjo` 的 Decl.attributes 直接序列化自 AST AttributePack。
     *
     * 官方 ASTWriter 直接写 `AttributePack.GetRawAttrs()`，
     * ASTLoader 也按同一组 bitset 原样恢复，因此这里必须使用 AST Attribute 的枚举位，
     * 不能套用 CHIR 的 `Attribute::VIRTUAL` 等另一套位布局。
     */
    private object AttrBit {
        /** 声明的原生导出及来源位，与语言可见性分别保存。 */
        val IMPORTED = Attribute.IMPORTED.ordinal
        val IMPLICIT_USED = Attribute.IMPLICIT_USED.ordinal
        val TOOL_ADD = Attribute.TOOL_ADD.ordinal
        val GENERIC_INSTANTIATED = Attribute.GENERIC_INSTANTIATED.ordinal
        val INTERFACE_IMPL = Attribute.INTERFACE_IMPL.ordinal
        val GLOBAL = Attribute.GLOBAL.ordinal
        /** `static` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val STATIC = Attribute.STATIC.ordinal
        /** `public` 可见性在 AST AttributePack 中的 bit 下标。 */
        val PUBLIC = Attribute.PUBLIC.ordinal
        /** `private` 可见性在 AST AttributePack 中的 bit 下标。 */
        val PRIVATE = Attribute.PRIVATE.ordinal
        /** `protected` 可见性在 AST AttributePack 中的 bit 下标。 */
        val PROTECTED = Attribute.PROTECTED.ordinal
        /** `internal` 可见性在 AST AttributePack 中的 bit 下标。 */
        val INTERNAL = Attribute.INTERNAL.ordinal
        /** `override` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val OVERRIDE = Attribute.OVERRIDE.ordinal
        /** `redef` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val REDEF = Attribute.REDEF.ordinal
        /** `abstract` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val ABSTRACT = Attribute.ABSTRACT.ordinal
        /** 接口默认实现状态必须从 CJO 属性恢复，不能依赖未反序列化的函数体。 */
        val DEFAULT = Attribute.DEFAULT.ordinal
        /** `sealed` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val SEALED = Attribute.SEALED.ordinal
        /** `open` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val OPEN = Attribute.OPEN.ordinal
        /** `operator` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val OPERATOR = Attribute.OPERATOR.ordinal
        /** `foreign` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val FOREIGN = Attribute.FOREIGN.ordinal
        /** C ABI attribute materialized from source `@C` or foreign defaulting. */
        val C = Attribute.C.ordinal
        /** STDCALL calling convention attribute. */
        val STD_CALL = Attribute.STD_CALL.ordinal
        /** Intrinsic declaration attribute. */
        val INTRINSIC = Attribute.INTRINSIC.ordinal
        /** Mock capability materialized by MockSupportManager/ASTLoader. */
        val MOCK_SUPPORTED = Attribute.MOCK_SUPPORTED.ordinal
        /** Annotation declaration marker. */
        val IS_ANNOTATION = Attribute.IS_ANNOTATION.ordinal
        /** common/specific declaration provenance. */
        val COMMON = Attribute.COMMON.ordinal
        val FROM_COMMON_PART = Attribute.FROM_COMMON_PART.ordinal
        val COMMON_NON_EXHAUSTIVE = Attribute.COMMON_NON_EXHAUSTIVE.ordinal
        val SPECIFIC = Attribute.SPECIFIC.ordinal
        val COMMON_WITH_DEFAULT = Attribute.COMMON_WITH_DEFAULT.ordinal
        /** Java mirror and generated declaration attributes. */
        val JAVA_APP = Attribute.JAVA_APP.ordinal
        val JAVA_EXT = Attribute.JAVA_EXT.ordinal
        val JAVA_MIRROR = Attribute.JAVA_MIRROR.ordinal
        val JAVA_MIRROR_SUBTYPE = Attribute.JAVA_MIRROR_SUBTYPE.ordinal
        val JAVA_HAS_DEFAULT = Attribute.JAVA_HAS_DEFAULT.ordinal
        val JAVA_MIRROR_SYNTHETIC_WRAPPER = Attribute.JAVA_MIRROR_SYNTHETIC_WRAPPER.ordinal
        /** Objective-C mirror and generated declaration attributes. */
        val OBJ_C_MIRROR = Attribute.OBJ_C_MIRROR.ordinal
        val OBJ_C_MIRROR_SUBTYPE = Attribute.OBJ_C_MIRROR_SUBTYPE.ordinal
        val OBJ_C_INIT = Attribute.OBJ_C_INIT.ordinal
        val OBJ_C_OPTIONAL = Attribute.OBJ_C_OPTIONAL.ordinal
        val OBJ_C_MIRROR_SYNTHETIC_WRAPPER = Attribute.OBJ_C_MIRROR_SYNTHETIC_WRAPPER.ordinal
        /** CJMapping and interface-forward attributes. */
        val JAVA_CJ_MAPPING = Attribute.JAVA_CJ_MAPPING.ordinal
        val OBJ_C_CJ_MAPPING = Attribute.OBJ_C_CJ_MAPPING.ordinal
        val CJ_MIRROR_JAVA_INTERFACE_FWD = Attribute.CJ_MIRROR_JAVA_INTERFACE_FWD.ordinal
        val DESUGARED_MIRROR_FIELD = Attribute.DESUGARED_MIRROR_FIELD.ordinal
        val HAS_INITED_FIELD = Attribute.HAS_INITED_FIELD.ordinal
        val CJ_MIRROR_JAVA_INTERFACE_DEFAULT = Attribute.CJ_MIRROR_JAVA_INTERFACE_DEFAULT.ordinal
        val CJ_MIRROR_OBJC_INTERFACE_FWD = Attribute.CJ_MIRROR_OBJC_INTERFACE_FWD.ordinal
        /** `unsafe` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val UNSAFE = Attribute.UNSAFE.ordinal
        /** `mut` 修饰符在 AST AttributePack 中的 bit 下标。 */
        val MUT = Attribute.MUT.ordinal
        /** 主构造器标记在 AST AttributePack 中的 bit 下标。 */
        val PRIMARY_CONSTRUCTOR = Attribute.PRIMARY_CONSTRUCTOR.ordinal
        /** 构造器标记在 AST AttributePack 中的 bit 下标。 */
        val CONSTRUCTOR = Attribute.CONSTRUCTOR.ordinal
        /** enum constructor 标记在 AST AttributePack 中的 bit 下标。 */
        val ENUM_CONSTRUCTOR = Attribute.ENUM_CONSTRUCTOR.ordinal
        /** finalizer 独立身份位；与同样写作 FuncDecl 的 constructor 区分。 */
        val FINALIZER = Attribute.FINALIZER.ordinal
        /** 参数具有默认值的标记；非字面量默认表达式不会写入 ParamInfo.defaultVal。 */
        val HAS_INITIAL = Attribute.HAS_INITIAL.ordinal
    }

    /** 测试 [decl] 的原始 AttributePack 中是否包含指定 bit。 */
    private fun testAttr(decl: Decl, bit: Int): Boolean {
        val wordIndex = bit / 64
        val bitIndex = bit % 64
        if (wordIndex >= decl.attributesLength) return false
        return (decl.attributes(wordIndex).toLong() shr bitIndex) and 1L == 1L
    }

    /**
     * 解码 PackageFormat 中 1-based 的声明引用索引。
     *
     * `0` 与 [UInt.MAX_VALUE] 都表示无效引用；返回值是 `allDecls` 的 0-based 下标。
     */
    private fun decodeDeclRef(rawIndex: UInt): Int? {
        if (rawIndex == 0u || rawIndex == UInt.MAX_VALUE) return null
        val decoded = rawIndex.toInt() - 1
        if (decoded !in 0 until context.pkg.allDeclsLength) return null
        return decoded
    }

    /**
     * 解码 PackageFormat 中 1-based 的表达式引用索引。
     *
     * `0` 与 [UInt.MAX_VALUE] 都表示无效引用；返回值是 `allExprs` 的 0-based 下标。
     */
    private fun decodeExprRef(rawIndex: UInt): Int? {
        if (rawIndex == 0u || rawIndex == UInt.MAX_VALUE) return null
        val decoded = rawIndex.toInt() - 1
        if (decoded !in 0 until context.pkg.allExprsLength) return null
        return decoded
    }

    /**
     * 将 CJO 声明上的全部注解恢复为 CFIR annotation call。
     *
     * target FullId 是注解的语义身份；identifier 只用于构造可读 callee 名，不参与平台注解匹配。
     * FlatBuffers 当前只序列化 LitConstExpr，因此这里完整恢复其支持的字面量及命名/位置参数。
     */
    private fun materializeAnnotations(
        decl: Decl,
        declarationReference: UInt,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
    ): MutableOrEmptyList<CfirAnnotation> {
        check(containingDeclarationSymbol.isBound) { "Annotation materialization requires its actual bound header" }
        if (decl.annotationsLength == 0) return MutableOrEmptyList.empty()

        val result = mutableListOf<CfirAnnotation>()
        for (index in 0 until decl.annotationsLength) {
            val serialized = checkNotNull(decl.annotations(index)) {
                "CJO declaration annotation #$index is absent"
            }
            result += checkNotNull(deserializeAnnotation(serialized, declarationReference, containingDeclarationSymbol, index)) {
                "CJO declaration annotation #$index could not be materialized"
            }
        }
        return result.toMutableOrEmpty()
    }

    /**
     * 根据反序列化注解构建弃用信息提供者。
     *
     * 库声明的弃用信息必须从反序列化注解重建：`@Deprecated` 等内置注解语义
     * 不能依赖源码侧 raw annotations 的保留。无匹配注解时返回 [EmptyDeprecationsProvider]。
     */
    private fun deprecationsProviderFor(annotations: List<CfirAnnotation>): DeprecationsProvider =
        buildDeprecationsProvider(annotations)

    /** 恢复单个 CJO 注解调用。 */
    private fun deserializeAnnotation(
        serialized: Anno,
        declarationReference: UInt,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
        annotationIndex: Int,
    ): CfirAnnotationCall? {
        // Keep the Cangjie v1.0.0 loader contract at the wire boundary.  The
        // official ASTLoader rejects an annotation kind it does not know; it
        // does not turn an unknown enum value into a custom annotation.  Doing
        // that here would publish a semantically usable CFIR node whose
        // identity was invented by the deserializer, and every downstream
        // checker would then be forced to guess what the producer meant.
        require(serialized.kind in supportedAnnotationKinds) {
            "Unhandled CJO annotation kind ${serialized.kind}"
        }
        val rawIdentifier = checkNotNull(serialized.identifier) {
            "CJO annotation identifier is absent"
        }
        require(rawIdentifier.isNotBlank()) {
            "CJO annotation identifier is blank"
        }
        val customTarget = if (serialized.kind == AnnoKind.Custom) serialized.target else null
        val targetResolution = customTarget?.let { target ->
            context.fullIdResolver.resolve(target) as? ResolvedFullId.Declaration
                ?: error("CJO annotation target must identify its actual declaration")
        }
        val targetDeclaration = targetResolution?.let { resolved ->
            if (resolved.source == ResolvedFullId.Declaration.Source.CURRENT_PACKAGE) {
                checkNotNull(context.createDeclDeserializer().deserializeCanonicalDeclaration(resolved.declaration.zeroBasedIndex))
            } else checkNotNull(context.materializeExternalDeclaration(resolved))
        }
        // For a custom annotation the official CJO writer stores the annotation
        // constructor FullId in Anno.target.  That FullId points to a child
        // FuncDecl, so resolving it as a class-like declaration loses the
        // annotation class entirely.  Resolve a direct class target first and
        // then walk the declaration parent chain for the constructor owner.
        val targetClassId = customTarget?.let { target ->
            context.fullIdResolver.resolveClassId(target)
                ?: context.fullIdResolver.resolveContainingClassId(target)
        }
        val sourceSpelling = rawIdentifier
            .removePrefix("@!")
            .removePrefix("@")
            .takeIf(String::isNotBlank)
        val shortName = targetClassId?.shortClassName
            ?: sourceSpelling?.substringAfterLast('.')?.let { Name.identifierIfValid(it) }
            ?: Name.ERROR_NAME
        val builtin = serializedBuiltinDescriptor(serialized)
        val system = targetClassId?.asSingleFqName()?.let(BuiltInAnnotationRegistry::findSystemAnnotation)
        // Official CJO AnnoKind carries platform identities even on format
        // revisions where Anno.target is absent.  Recover only the
        // unambiguous kind-to-ClassId mappings here; do not infer ForeignName
        // from its short spelling because Java and ObjC share that name.
        val platform = targetClassId?.asSingleFqName()?.let(BuiltInAnnotationRegistry::findPlatformAnnotation)
            ?: serializedPlatformDescriptor(serialized)
        val platformClassId = platform?.classFqName?.let(ClassId::topLevel)

        val annotationTypeRef: CfirTypeRef = if (targetClassId != null || platformClassId != null) {
            buildResolvedTypeRef {
                customRenderer = false
                coneType = ConeClassLikeType(
                    lookupTag = ConeClassLikeLookupTagImpl(checkNotNull(targetClassId ?: platformClassId)),
                    typeArguments = emptyList(),
                )
            }
        } else {
            buildImplicitTypeRef {
                customRenderer = false
            }
        }

        val arguments = buildList {
            for (argumentIndex in 0 until serialized.argsLength) {
                val serializedArgument = checkNotNull(serialized.args(argumentIndex)) {
                    "CJO annotation argument #$argumentIndex is absent"
                }
                val expression = deserializeRequiredExpression(
                    serializedArgument.expr,
                    "annotation argument #$argumentIndex",
                    CjoSourceContentRoot.AnnotationArgument(declarationReference, annotationIndex, argumentIndex),
                )
                val argumentName = serializedArgument.name?.takeIf(String::isNotBlank)
                add(
                    if (argumentName == null) {
                        expression
                    } else {
                        buildNamedArgumentExpression {
                            this.expression = expression
                            this.argumentName = Name.identifier(argumentName)
                        }.also { context.registerSourceOrigin(it, serializedArgument.expr) }
                    }
                )
            }
        }

        val annotation = buildAnnotationCall {
            typeRef = annotationTypeRef
            annotationSourceName = sourceSpelling
            annotationClassId = targetClassId ?: platformClassId
            annotationKind = builtin?.kind
            forcedCustom = serialized.kind == AnnoKind.Custom
            isCompileTimeVisible = serialized.kind == AnnoKind.Custom
            annotationOrigin = when {
                builtin != null -> builtin.origin
                platform != null -> CangjieAnnotationOrigin.PLATFORM_DERIVED
                system != null -> system.origin
                else -> CangjieAnnotationOrigin.CUSTOM
            }
            annotationIdentity = when {
                builtin != null && builtin.origin == CangjieAnnotationOrigin.PACKAGE_DIRECTIVE ->
                    CangjieAnnotationIdentity.PackageDirective(
                        org.cangnova.cangjie.annotations.CangjiePackageDirectiveKind.NON_PRODUCT,
                        builtin.sourceName,
                    )
                builtin != null -> CangjieAnnotationIdentity.LanguageBuiltIn(
                    requireNotNull(builtin.kind),
                    builtin.sourceName,
                )
                platform != null -> CangjieAnnotationIdentity.PlatformDerived(
                    kind = platform.platformKind,
                    classFqName = platform.classFqName,
                    sourceName = platform.sourceName,
                )
                system != null -> CangjieAnnotationIdentity.SystemMacro(system.classFqName, system.sourceName)
                targetClassId != null -> CangjieAnnotationIdentity.Custom(targetClassId.asSingleFqName())
                else -> CangjieAnnotationIdentity.Unknown
            }
            argumentList = buildArgumentList {
                this.arguments.addAll(arguments)
            }
            calleeReference = buildNamedReference {
                name = shortName
            }
            this.containingDeclarationSymbol = containingDeclarationSymbol
            // CJO carries serialized identity/argument facts, not a proof that this
            // declaration has passed the current CFIR semantic checker set.  Keep
            // the annotation at TYPE_RESOLVED; the declaration owner is the only
            // producer allowed to publish SEMANTIC_RESOLVED.
            annotationResolveState = if (builtin != null || targetClassId != null || platform != null) {
                CfirAnnotationResolveState.TYPE_RESOLVED
            } else {
                CfirAnnotationResolveState.ERROR
            }
        }

        targetDeclaration?.let { target ->
            annotation.replaceCalleeReference(buildResolvedNamedReference {
                name = shortName
                resolvedSymbol = target.symbol
            })
        }
        // Anno.target保存的是实际目标；参数映射不能以同名class中的第一个constructor代替它。
        val constructor = targetDeclaration as? CfirConstructor
        context.enqueueAnnotationBinding(containingDeclarationSymbol.cfir) {
            if (builtin != null) {
                // Built-in Anno entries have no constructor declaration in CJO.  Their
                // parameter names still come from the canonical common schema; preserve
                // that mapping instead of dropping all serialized arguments.
                val mapping = arguments.mapIndexedNotNull { index, argument ->
                    val named = argument as? org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
                    val parameterName = named?.argumentName?.asString()
                        ?: builtin.argumentSchema.positionalParameter?.name
                        ?: builtin.argumentSchema.parameters.getOrNull(index)?.name
                    parameterName?.let {
                        Name.identifier(it) to (named?.expression ?: argument)
                    }
                }.toMap(LinkedHashMap())
                annotation.replaceArgumentMapping(CfirAnnotationArgumentMappingImpl(annotation.source, mapping))
                annotation.replaceArgumentView(
                    CfirAnnotationArgumentView(
                        arguments.mapIndexed { index, argument ->
                            val named = argument as? org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
                            CfirAnnotationArgumentViewEntry(
                                sourceOrder = index,
                                explicitName = named?.argumentName,
                                argument = argument,
                                resolvedParameter = null,
                                status = CfirAnnotationArgumentStatus.RESOLVED,
                                isDefaultOrigin = false,
                                constantExpression = named?.expression ?: argument,
                                source = argument.source,
                            )
                        },
                    ),
                )
            } else {
                val resolvedArguments = buildResolvedArgumentList(
                    annotation.argumentList,
                    LinkedHashMap<CfirExpression, CfirValueParameter>().also { mapping ->
                        var positionalIndex = 0
                        for (argument in arguments) {
                            val namedArgument = argument as? org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
                            val parameter = if (namedArgument != null) {
                                constructor?.valueParameters?.firstOrNull { it.name == namedArgument.argumentName }
                            } else {
                                constructor?.valueParameters?.getOrNull(positionalIndex++)
                            }
                            if (parameter != null) mapping[argument] = parameter
                        }
                    },
                )
                annotation.replaceArgumentMapping(
                    CfirAnnotationArgumentMappingImpl(
                        annotation.source,
                        resolvedArguments.mapping.entries.associate { (argument, parameter) ->
                            parameter.name to ((argument as? org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression)?.expression ?: argument)
                        },
                    ),
                )
                val defaultParameters = constructor?.valueParameters.orEmpty()
                val omittedParameters = defaultParameters.filter { it !in resolvedArguments.mapping.values }
                if (omittedParameters.isNotEmpty()) {
                    val target = checkNotNull(targetResolution)
                    if (target.source == ResolvedFullId.Declaration.Source.CURRENT_PACKAGE)
                        omittedParameters.forEach { bodyDeserializer.deserializeDefault(it) }
                    else {
                        val values = context.materializeExternalDefaults(target, omittedParameters)
                        check(values.keys == omittedParameters.toSet()) { "External defaults changed their actual parameter identities" }
                    }
                }
                val externalSnapshots = java.util.IdentityHashMap<org.cangnova.cangjie.cfir.CfirContentSource.Published, org.cangnova.cangjie.cfir.CfirPublishedContentSnapshot>()
                val defaultValues = defaultParameters.associateWith { parameter ->
                    when (val source = parameter.contentSource) {
                        context.publishedContentSource -> context.bodyState.defaults[parameter]
                        org.cangnova.cangjie.cfir.CfirContentSource.Owned -> parameter.defaultValue
                        is org.cangnova.cangjie.cfir.CfirContentSource.Published ->
                            externalSnapshots.getOrPut(source) { source.snapshot() }.defaultValue(parameter)
                    }
                }
                annotation.replaceArgumentView(resolvedArguments.toAnnotationArgumentView(defaultParameters, defaultValues))
                if (constructor != null) {
                    annotation.replaceCalleeReference(buildResolvedNamedReference {
                        name = constructor.symbol.callableId.callableName
                        resolvedSymbol = constructor.symbol
                    })
                }
            }
        }
        return annotation
    }

    /** Resolve the serialized `AnnoKind`; source spelling is never used as its semantic key. */
    private fun serializedBuiltinDescriptor(serialized: Anno): BuiltInAnnotationDescriptor? {
        val kind = when (serialized.kind) {
            AnnoKind.Deprecated -> BuiltInAnnotationKind.DEPRECATED
            AnnoKind.TestRegistration -> BuiltInAnnotationKind.ATTRIBUTE
            AnnoKind.Frozen -> BuiltInAnnotationKind.FROZEN
            AnnoKind.Annotation -> BuiltInAnnotationKind.ANNOTATION
            else -> null
        } ?: return null
        return BuiltInAnnotationRegistry.findLanguageBuiltIn(kind)
    }

    /**
     * Annotation kinds understood by this repository's decoder.
     *
     * The first four are the official v1.0.0 ModuleFormat domain.  The
     * remaining values are the repository's additive CJO profile and are
     * decoded by the platform/builtin identity paths below.  Unknown wire
     * values remain hard errors; they must never be silently reclassified as
     * custom annotations.
     */
    companion object {
        /** 从 `.cjo` 构建 PSI stub 时只物化声明形状，保留声明式常量元数据。 */
        fun forCompiledFileStub(
            context: CfirDeserializationContext,
            typeDeserializer: CfirTypeDeserializer,
        ): CfirDeclDeserializer {
            val signatures = context.withMode(CjoDeserializationMode.SIGNATURE_ONLY)
            return CfirDeclDeserializer(signatures, signatures.createTypeDeserializer(), loadFieldVariableInitializers = false)
        }

        internal fun forSourceGraph(context: CfirDeserializationContext): CfirDeclDeserializer =
            CfirDeclDeserializer(context, context.createTypeDeserializer(), loadFieldVariableInitializers = false)

        private val supportedAnnotationKinds = setOf(
            AnnoKind.Deprecated,
            AnnoKind.TestRegistration,
            AnnoKind.Frozen,
            AnnoKind.Custom,
            AnnoKind.JavaMirror,
            AnnoKind.JavaImpl,
            AnnoKind.ObjCMirror,
            AnnoKind.ObjCImpl,
            AnnoKind.ForeignName,
            AnnoKind.JavaHasDefault,
            AnnoKind.Annotation,
        )
    }

    /**
     * Restore platform annotation identity from the official CJO kind when the
     * older binary schema omitted Anno.target.  ForeignName is intentionally
     * excluded because its Java/ObjC owner cannot be recovered from the kind.
     */
    private fun serializedPlatformDescriptor(serialized: Anno): PlatformAnnotationDescriptor? = when (serialized.kind) {
        AnnoKind.JavaMirror -> BuiltInAnnotationRegistry.findPlatformAnnotation(
            FqName("interoplib.interop.JavaMirror"),
        )
        AnnoKind.JavaImpl -> BuiltInAnnotationRegistry.findPlatformAnnotation(
            FqName("interoplib.interop.JavaImpl"),
        )
        AnnoKind.JavaHasDefault -> BuiltInAnnotationRegistry.findPlatformAnnotation(
            FqName("interoplib.interop.JavaHasDefault"),
        )
        AnnoKind.ObjCMirror -> BuiltInAnnotationRegistry.findPlatformAnnotation(
            FqName("objc.lang.ObjCMirror"),
        )
        AnnoKind.ObjCImpl -> BuiltInAnnotationRegistry.findPlatformAnnotation(
            FqName("objc.lang.ObjCImpl"),
        )
        else -> null
    }

    /**
     * Restore the expression subset represented by the current CJO expression
     * producer.  This is shared by annotation arguments and declaration
     * initializers; declaration initializers use the required wrapper below so
     * an unsupported wire node cannot silently become a missing initializer.
     */
    /** 仅真实缺失的零槽返回 null；其余角色统一由完整正文 reader 材料化。 */
    private fun deserializeRequiredExpression(rawExprIndex: UInt, owner: String, root: CjoSourceContentRoot): CfirExpression {
        check(rawExprIndex != 0u && rawExprIndex <= context.pkg.allExprsLength.toUInt()) {
            "CJO " + owner + " requires a valid nonzero expression reference: " + rawExprIndex
        }
        return bodyDeserializer.deserializeExpression(rawExprIndex, root)
    }

    /** 判断声明是否被官方 AST AttributePack 标记为 enum constructor。 */
    private fun isEnumConstructorDecl(decl: Decl): Boolean =
        testAttr(decl, AttrBit.ENUM_CONSTRUCTOR)

    /** CJO visibility 只有唯一属性位时才是已知值；缺失或冲突位保留为 UNKNOWN。 */
    private fun resolveVisibility(decl: Decl): Visibility {
        val visibilityBits = buildList {
            if (testAttr(decl, AttrBit.PUBLIC)) add(Visibilities.Public)
            if (testAttr(decl, AttrBit.PRIVATE)) add(Visibilities.Private)
            if (testAttr(decl, AttrBit.PROTECTED)) add(Visibilities.Protected)
            if (testAttr(decl, AttrBit.INTERNAL)) add(Visibilities.Internal)
        }
        return visibilityBits.singleOrNull() ?: Visibilities.Unknown
    }

    /** 从 attributes 位域解析 modality */
    private fun resolveModality(decl: Decl): Modality = Modality.convertFromFlags(
        sealed = testAttr(decl, AttrBit.SEALED),
        abstract = testAttr(decl, AttrBit.ABSTRACT),
        open = testAttr(decl, AttrBit.OPEN),
    )

    /** CJO 已保存完成的 visibility/modality；库声明进入公共图时直接发布 resolved 状态及全部原始位。 */
    private fun buildStatus(decl: Decl): CfirResolvedDeclarationStatus {
        val visibility = resolveVisibility(decl)
        val modality = resolveModality(decl)
        val status = CfirDeclarationStatusImpl(
            visibility = visibility,
            modality = modality,
        )
        status.isVisibilityExplicit = false
        status.isModalityExplicit = false
        status.visibilityExplicitness = CfirModifierExplicitness.UNKNOWN
        status.modalityExplicitness = CfirModifierExplicitness.UNKNOWN
        status.isAbstract = testAttr(decl, AttrBit.ABSTRACT)
        status.isDefault = testAttr(decl, AttrBit.DEFAULT)
        status.isOpen = testAttr(decl, AttrBit.OPEN)
        status.isSealed = testAttr(decl, AttrBit.SEALED)
        status.isStatic = testAttr(decl, AttrBit.STATIC)
        status.isConst = deserializeConstStatus(decl)
        status.isOverride = testAttr(decl, AttrBit.OVERRIDE)
        status.isOperator = testAttr(decl, AttrBit.OPERATOR)
        status.isForeign = testAttr(decl, AttrBit.FOREIGN)
        // Official SetForeignABIAttr materializes the backend default C ABI
        // for `foreign`; an explicit @C declaration also carries this bit.
        // `foreign` is an independent declaration modifier; the serialized
        // explicit-C attribute belongs to the builtin annotation feature gate.
        status.isC = (testAttr(decl, AttrBit.C) &&
            context.languageVersionSettings.supportsFeature(org.cangnova.cangjie.LanguageFeature.BuiltInAnnotations)) ||
            status.isForeign
        status.isCommon = testAttr(decl, AttrBit.COMMON)
        status.isFromCommonPart = testAttr(decl, AttrBit.FROM_COMMON_PART)
        status.isSpecific = testAttr(decl, AttrBit.SPECIFIC)
        // 官方 ASTLoader 原样恢复 AttributePack；COMMON_WITH_DEFAULT 决定跨模块验证豁免
        //（CheckCJMP.cpp MustMatchWithPlatform），必须随位恢复而不是从函数体推导——
        // 反序列化声明没有函数体。
        status.isCommonWithDefault = testAttr(decl, AttrBit.COMMON_WITH_DEFAULT)
        status.isUnsafe = testAttr(decl, AttrBit.UNSAFE)
        status.isMut = testAttr(decl, AttrBit.MUT)
        status.isRedef = testAttr(decl, AttrBit.REDEF)
        // CJO 声明直接发布为 BODY_RESOLVE，解码出的有效状态必须满足 resolved 状态不变量。
        return status.resolvedWithEffectiveModality()
    }

    /** Reconstruct non-Anno interop facts written by the official CJO writer. */
    private fun serializedInteropFacts(decl: Decl): CfirSerializedInteropFacts {
        val isForeign = testAttr(decl, AttrBit.FOREIGN)
        val funcInfo = if (decl.infoType == DeclInfo.FuncInfo) decl.info(FuncInfo()) as? FuncInfo else null
        return CfirSerializedInteropFacts(
            hasCAnnotationAttribute = testAttr(decl, AttrBit.C),
            hasExplicitC = testAttr(decl, AttrBit.C) && !isForeign,
            callingConvention = if (testAttr(decl, AttrBit.STD_CALL)) {
                CangjieCallingConvention.STDCALL
            } else {
                null
            },
            overflowStrategy = funcInfo?.overflowPolicy?.let { policy ->
                when (policy) {
                    OverflowPolicy.Checked -> CangjieOverflowStrategy.CHECKED
                    OverflowPolicy.Wrapping -> CangjieOverflowStrategy.WRAPPING
                    OverflowPolicy.Throwing -> CangjieOverflowStrategy.THROWING
                    OverflowPolicy.Saturating -> CangjieOverflowStrategy.SATURATING
                    else -> null
                }
            },
            isIntrinsic = testAttr(decl, AttrBit.INTRINSIC),
            isFastNative = funcInfo?.isFastNative == true,
            isMockSupported = testAttr(decl, AttrBit.MOCK_SUPPORTED),
            isJavaMirror = testAttr(decl, AttrBit.JAVA_MIRROR),
            isJavaMirrorSubtype = testAttr(decl, AttrBit.JAVA_MIRROR_SUBTYPE),
            hasJavaDefault = testAttr(decl, AttrBit.JAVA_HAS_DEFAULT),
            isJavaMirrorSyntheticWrapper = testAttr(decl, AttrBit.JAVA_MIRROR_SYNTHETIC_WRAPPER),
            isJavaApplication = testAttr(decl, AttrBit.JAVA_APP),
            isJavaExtension = testAttr(decl, AttrBit.JAVA_EXT),
            isJavaCjMapping = testAttr(decl, AttrBit.JAVA_CJ_MAPPING),
            isJavaInterfaceForward = testAttr(decl, AttrBit.CJ_MIRROR_JAVA_INTERFACE_FWD),
            isJavaInterfaceDefault = testAttr(decl, AttrBit.CJ_MIRROR_JAVA_INTERFACE_DEFAULT),
            isObjCMirror = testAttr(decl, AttrBit.OBJ_C_MIRROR),
            isObjCMirrorSubtype = testAttr(decl, AttrBit.OBJ_C_MIRROR_SUBTYPE),
            isObjCInit = testAttr(decl, AttrBit.OBJ_C_INIT),
            isObjCOptional = testAttr(decl, AttrBit.OBJ_C_OPTIONAL),
            isObjCMirrorSyntheticWrapper = testAttr(decl, AttrBit.OBJ_C_MIRROR_SYNTHETIC_WRAPPER),
            isObjCCjMapping = testAttr(decl, AttrBit.OBJ_C_CJ_MAPPING),
            isObjCInterfaceForward = testAttr(decl, AttrBit.CJ_MIRROR_OBJC_INTERFACE_FWD),
            attributeNames = buildList {
                for (index in 0 until decl.annotationsLength) {
                    if (decl.annotations(index)?.kind == AnnoKind.TestRegistration) {
                        add("TEST_REGISTER")
                    }
                }
            },
            cjmpTarget = when {
                testAttr(decl, AttrBit.JAVA_CJ_MAPPING) -> CfirInteropTarget.JAVA
                testAttr(decl, AttrBit.OBJ_C_CJ_MAPPING) -> CfirInteropTarget.OBJC
                else -> null
            },
        )
    }

    /** Restore ClassInfo's compact annotation declaration metadata. */
    private fun serializedAnnotationInfo(decl: Decl): CfirDeclarationAnnotationInfo? {
        if (decl.infoType != DeclInfo.ClassInfo) return null
        val info = decl.info(ClassInfo()) as? ClassInfo ?: return null
        if (!testAttr(decl, AttrBit.IS_ANNOTATION) && !info.isAnno) return null
        val firstMask = info.annoTargets.toInt()
        val mask = if (firstMask == 0xff) {
            CangjieAnnotationTarget.entries.fold(0) { result, target -> result or (1 shl target.bitPosition) }
        } else {
            firstMask or (info.annoTargets2.toInt() shl 7)
        }
        val targets = CangjieAnnotationTarget.entries
            .filterTo(linkedSetOf()) { mask and (1 shl it.bitPosition) != 0 }
        return CfirDeclarationAnnotationInfo(
            isAnnotation = true,
            annotationTargets = targets,
            isIntrinsic = testAttr(decl, AttrBit.INTRINSIC),
            runtimeVisible = info.runtimeVisible,
        )
    }

    /**
     * 恢复官方 ASTLoader 从 ClassInfo 合成的 `@Annotation` 节点。
     *
     * 当 annotation class 没有显式 target 参数时，ASTWriter 不写普通 Anno，
     * 而是在 ClassInfo 中保存 target 位集和 runtime-visible 位。CJO loader
     * 会在声明注解列表中补回一个 Annotation 节点；CFIR 也必须保留该节点，
     * 否则源码与二进制声明会在 annotation identity 和目标检查入口上分叉。
     */
    private fun appendSerializedAnnotationMarker(declaration: CfirDeclaration, annotations: List<CfirAnnotation>): List<CfirAnnotation> {
        if (declaration.annotationInfo?.isAnnotation != true) return annotations
        if (annotations
                .filterIsInstance<CfirAnnotationCall>()
                .any { it.annotationKind == org.cangnova.cangjie.annotations.BuiltInAnnotationKind.ANNOTATION }
        ) return annotations

        val annotationBaseClassId = ClassId.topLevel(StandardNames.FqNames.annotation)
        val annotationTypeRef = buildResolvedTypeRef {
            customRenderer = false
            coneType = ConeClassLikeType(
                lookupTag = annotationBaseClassId.toLookupTag(),
                typeArguments = emptyList(),
            )
        }
        val marker = buildAnnotationCall {
            source = null
            typeRef = annotationTypeRef
            coneTypeOrNull = annotationTypeRef.coneType
            argumentList = buildArgumentList {}
            calleeReference = buildNamedReference {
                name = Name.identifier(StandardNames.FqNames.annotation.shortName().asString())
            }
            annotationClassId = annotationBaseClassId
            annotationTarget = declaration.annotationTargetFor()
            forcedCustom = false
            annotationSourceName = StandardNames.FqNames.annotation.shortName().asString()
            annotationKind = org.cangnova.cangjie.annotations.BuiltInAnnotationKind.ANNOTATION
            annotationIdentity = CangjieAnnotationIdentity.LanguageBuiltIn(
                org.cangnova.cangjie.annotations.BuiltInAnnotationKind.ANNOTATION,
                StandardNames.FqNames.annotation.shortName().asString(),
            )
            annotationOrigin = CangjieAnnotationOrigin.LANGUAGE_BUILT_IN
            isCompileTimeVisible = false
            containingDeclarationSymbol = declaration.symbol
            // The ClassInfo marker restores serialized annotation identity and
            // target metadata only.  It is not produced by the declaration
            // annotation semantic owner, so it must remain below the semantic
            // terminal state until that owner validates it.
            annotationResolveState = CfirAnnotationResolveState.TYPE_RESOLVED
        }
        return annotations + marker
    }

    /**
     * 从声明专属 info 中恢复 const 语义。
     *
     * PackageFormat 不把 const 放进 AttributePack，而是分别保存在函数、变量、模式变量和
     * 属性的 info 表中。这里集中恢复该状态，保证 Library CFIR 与 Source CFIR 的声明状态等价，
     * 下游常量求值、构造器和成员检查器无需重新猜测序列化语义。
     */
    private fun deserializeConstStatus(decl: Decl): Boolean = when (decl.infoType) {
        DeclInfo.FuncInfo -> checkNotNull(decl.info(FuncInfo()) as? FuncInfo) {
            "FuncInfo is missing for declaration '${decl.identifier ?: "<anonymous>"}'"
        }.isConst

        DeclInfo.VarInfo -> checkNotNull(decl.info(VarInfo()) as? VarInfo) {
            "VarInfo is missing for declaration '${decl.identifier ?: "<anonymous>"}'"
        }.isConst

        DeclInfo.VarWithPatternInfo -> checkNotNull(decl.info(VarWithPatternInfo()) as? VarWithPatternInfo) {
            "VarWithPatternInfo is missing for declaration '${decl.identifier ?: "<anonymous>"}'"
        }.isConst

        DeclInfo.PropInfo -> checkNotNull(decl.info(PropInfo()) as? PropInfo) {
            "PropInfo is missing for declaration '${decl.identifier ?: "<anonymous>"}'"
        }.isConst

        else -> false
    }

    /** 将 type 字段值转换为 CfirResolvedTypeRef */
    private fun buildTypeRef(typeFieldValue: UInt): CfirResolvedTypeRef {
        val coneType = typeDeserializer.deserializeTypeFromField(typeFieldValue)
        return CfirResolvedTypeRefImpl(
            source = null,
            annotations = MutableOrEmptyList.empty(),
            customRenderer = false,
            typeUseOrigin = org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin.COMPILER_ADDED,
            coneType = coneType,
            delegatedTypeRef = null,
        )
    }

    /**
     * `FuncDecl.type` 是完整函数类型 `(P1, P2, ...) -> R`，而 CFIR `CfirFunction.returnTypeRef`
     * 只表示 callable 的结果类型 `R`。官方 ASTLoader 也是把 `decl.ty` 与
     * `funcBody.retType` 分开恢复，这里必须保持同样的声明形状。
     */
    private fun buildFunctionReturnTypeRef(decl: Decl): CfirResolvedTypeRef {
        val funcInfo = decl.info(FuncInfo()) as? FuncInfo
            ?: error("FuncDecl '${decl.identifier ?: "<anonymous>"}' must contain FuncInfo")
        val body = checkNotNull(funcInfo.funcBody) { "FuncDecl must retain its actual FuncBody even when body=0" }
        if (testAttr(decl, Attribute.UNREACHABLE.ordinal) && decl.type == 0u) {
            val header = body
            check(header.paramListsLength == 0 && header.retType == 0u && header.body == 0u && !header.always && header.captureKind == 0.toUByte()) {
                "UNREACHABLE signature omission requires the native partial empty FuncBody"
            }
            return buildResolvedTypeRef {
                coneType = ConeInvalidType
                typeUseOrigin = org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin.SEMANTIC
            }
        }
        val functionType = typeDeserializer.deserializeTypeFromField(decl.type) as? ConeFunctionType
            ?: error("FuncDecl '${decl.identifier ?: "<anonymous>"}' must carry FuncTy in Decl.type")
        val encodedReturnType = body.retType
        if (encodedReturnType != 0u) {
            return buildTypeRef(encodedReturnType)
        }

        return buildResolvedTypeRef {
            customRenderer = false
            coneType = functionType.returnType
        }
    }

    /**
     * 对位 Kotlin FirMemberDeserializer.loadConstructor 的 constructed-class 引用：
     * 构造器与 enum 构造项引用所属名义类型的参数 symbol，不拥有、不重新绑定参数声明。
     */
    private fun constructedClassTypeParameterRefs(
        ownerTypeParameters: List<CfirTypeParameterRef>,
    ): List<CfirTypeParameterRef> = constructedClassTypeParameterRefs(ownerTypeParameters.map { it.symbol })

    /** 名义身份预留阶段只登记自身 TP/type，不将尚未完成的 bounds 当作最终空集合。 */
    internal fun reserveClassLikeTypeParameters(index: Int, owner: CfirClassLikeSymbol<*>): Unit = context.materialize {
        withContainingDeclarationSymbol(owner) { reserveTypeParameters(checkNotNull(context.pkg.allDecls(index))) }
        Unit
    }

    private fun reserveTypeParameters(decl: Decl): MutableList<CfirTypeParameter> {
        val generic = decl.generic ?: return mutableListOf()
        val owner = checkNotNull(currentContainingDeclarationSymbol)
        val indices = (0 until generic.typeParametersLength).map { checkNotNull(decodeDeclRef(generic.typeParameters(it))) }
        check(indices.distinct().size == indices.size) { "Duplicate owning generic parameter slot" }
        val parameters = indices.mapTo(mutableListOf()) { index ->
            val encoded = checkNotNull(context.pkg.allDecls(index))
            check(encoded.kind == DeclKind.GenericParamDecl)
            val existing = context.declarations[index] as? CfirTypeParameter ?: context.reservedTypeParameters[index]
            if (existing != null) {
                check(existing.containingDeclarationSymbol === owner)
                existing
            } else {
                val symbol = CfirTypeParameterSymbol()
                CfirTypeParameterImpl(
                    contentSource = context.publishedContentSource,
                    source = null,
                    moduleData = context.moduleData,
                    resolvePhase = CfirResolvePhase.BODY_RESOLVE,
                    annotations = MutableOrEmptyList.empty(),
                    origin = CfirDeclarationOrigin.Library,
                    attributes = CfirDeclarationAttributes.EMPTY,
                    containingDeclarationSymbol = owner,
                    symbol = symbol,
                    name = Name.identifier(checkNotNull(encoded.identifier)),
                    bounds = mutableListOf(),
                ).also { context.reservedTypeParameters[index] = it }
            }
        }
        parameters.forEachIndexed { ordinal, parameter ->
            val index = indices[ordinal]
            val identity = context.genericTypes.getOrPut(parameter.symbol) { ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag()) }
            context.genericTypeIndicesByParameter[index].orEmpty().forEach { typeIndex ->
                context.requireSourceTypeIdentity(typeIndex)
                val previous = context.types.putIfAbsent(typeIndex, identity)
                check(previous == null || (previous as ConeTypeParameterType).lookupTag.typeParameterSymbol === parameter.symbol)
                if (!context.typeCache.containsKey(typeIndex)) context.enqueueGenericType(typeIndex)
            }
        }
        return parameters
    }

    /** 所有自身 TP/Generic 身份已登记后，显式完成每份保存的 bounds/constraints，再发布 header。 */
    private fun deserializeTypeParameters(decl: Decl): MutableList<CfirTypeParameter> {
        val generic = decl.generic ?: return mutableListOf()
        val indices = (0 until generic.typeParametersLength).map { checkNotNull(decodeDeclRef(generic.typeParameters(it))) }
        val parameters = reserveTypeParameters(decl)
        parameters.forEachIndexed { ordinal, parameter ->
            val index = indices[ordinal]
            if (!context.declarations.containsKey(index)) {
                val encoded = checkNotNull(context.pkg.allDecls(index))
                if (encoded.type != 0u) {
                    val type = typeDeserializer.deserializeTypeFromField(encoded.type) as? ConeTypeParameterType
                        ?: error("GenericParamDecl must carry its canonical Generic type")
                    check(type.lookupTag.typeParameterSymbol === parameter.symbol)
                    typeDeserializer.completeGenericType(encoded.type.toInt() - 1)
                }
                context.genericTypeIndicesByParameter[index].orEmpty().forEach { typeIndex ->
                    val type = typeDeserializer.completeGenericType(typeIndex) as ConeTypeParameterType
                    check(type.lookupTag.typeParameterSymbol === parameter.symbol)
                }
            }
        }
        applyOwnerGenericConstraints(generic, parameters)
        parameters.forEachIndexed { ordinal, parameter ->
            val index = indices[ordinal]
            if (!context.declCache.containsKey(index)) {
                context.genericBounds[parameter.symbol]?.let(parameter::replaceBounds)
                parameter.markResolved()
                context.declarations[index] = parameter
                publishDeserializedDeclaration(index, checkNotNull(context.pkg.allDecls(index)), parameter)
                context.reservedTypeParameters.remove(index)
            }
        }
        return parameters
    }

    /**
     * `.cjo` 的泛型约束挂在 owner `Decl.generic.constraints` 上，而不是
     * `GenericParamDecl.generic` 上。constraint.type 是被约束的类型参数，
     * constraint.uppers 是原组的 RHS，独立保存；不能追加到 GenericTyInfo 的最终集合。
     */
    private fun applyOwnerGenericConstraints(
        generic: Generic,
        typeParameters: List<CfirTypeParameter>,
    ) {
        val owner = checkNotNull(currentContainingDeclarationSymbol)
        val originalGroups = mutableListOf<CfirDeserializedGenericConstraint>()
        for (i in 0 until generic.constraintsLength) {
            val constraint = checkNotNull(generic.constraints(i)) { "Missing owner generic constraint" }
            val constrainedType = typeDeserializer.deserializeTypeFromField(constraint.type)
            val bounds = (0 until constraint.uppersLength).map { buildTypeRef(constraint.uppers(it)) }
            originalGroups += CfirDeserializedGenericConstraint(
                subject = constrainedType,
                upperBounds = java.util.Collections.unmodifiableList(bounds),
                isImplicitlyIntroduced = constraint.isImplicitlyIntroduced,
                begin = context.sourceLocation(constraint.begin),
                end = context.sourceLocation(constraint.end),
            )
        }
        check(typeParameters.all { it.containingDeclarationSymbol === owner })
        context.constraints[owner] = java.util.Collections.unmodifiableList(originalGroups)
    }

    /** 反序列化继承类型列表 */
    private fun deserializeInheritedTypes(
        getter: (Int) -> UInt,
        length: Int,
    ): MutableList<CfirTypeRef> {
        if (length == 0) return mutableListOf()
        return (0 until length).mapTo(mutableListOf()) { buildTypeRef(getter(it)) }
    }

    /** 反序列化成员声明列表（延迟加载） */
    private fun deserializeBody(
        getter: (Int) -> UInt,
        length: Int,
    ): MutableList<CfirDeclaration> {
        if (length == 0) return mutableListOf()
        return (0 until length).mapNotNullTo(mutableListOf()) { i ->
            val declIndex = decodeDeclRef(getter(i)) ?: return@mapNotNullTo null
            deserializeDecl(declIndex, parameterDefaults = null)
        }
    }

    /** 在 enum 声明反序列化期间压入 owner 上下文。 */
    private inline fun <R> withEnumOwner(owner: EnumOwnerContext, block: () -> R): R {
        enumOwnerStack += owner
        return try {
            block()
        } finally {
            enumOwnerStack.removeAt(enumOwnerStack.lastIndex)
        }
    }

    /** 当前 enum owner；不在 enum 声明内部时为 null。 */
    private val currentEnumOwner: EnumOwnerContext?
        get() = enumOwnerStack.lastOrNull()

    /**
     * `extend` 成员反序列化期间的被扩展类型栈。
     *
     * Cangjie 的 `extend Foo { func bar() }` 与 Kotlin class 成员同构：`bar` 的接收者是 `Foo`。
     * 库侧成员必须与源码侧共享同一份 dispatch receiver 语义，否则符号层无法区分
     * 不同 extend 上的同名成员（如 std.core 的多个 `toString`），导航/文档/补全都会退化。
     */
    private val extendReceiverTypeStack = mutableListOf<ConeSimpleCangJieType>()

    /** 在 [type] 作为当前 extend 接收者类型的上下文中执行 [block]。 */
    private inline fun <R> withExtendReceiverType(type: ConeSimpleCangJieType?, block: () -> R): R {
        if (type == null) return block()
        extendReceiverTypeStack += type
        return try {
            block()
        } finally {
            extendReceiverTypeStack.removeAt(extendReceiverTypeStack.lastIndex)
        }
    }

    /** 当前 extend 的接收者类型；不在 extend 成员内部或无法推断时为 null。 */
    private val currentExtendReceiverType: ConeSimpleCangJieType?
        get() = extendReceiverTypeStack.lastOrNull()

    /** 在 class-like 声明反序列化期间压入 owner 上下文。 */
    private inline fun <R> withClassLikeOwner(owner: ClassLikeOwnerContext, block: () -> R): R {
        classLikeOwnerStack += owner
        return try {
            block()
        } finally {
            classLikeOwnerStack.removeAt(classLikeOwnerStack.lastIndex)
        }
    }

    /** 当前 class-like owner；不在 class/interface/struct/enum 内部时为 null。 */
    private val currentClassLikeOwner: ClassLikeOwnerContext?
        get() = classLikeOwnerStack.lastOrNull()

    /**
     * 在反序列化子声明期间压入真实宿主声明符号。
     *
     * 类型参数和值参数在二进制里是独立 Decl，但 CFIR 必须把它们绑定回函数、构造器或 class-like symbol。
     */
    private inline fun <R> withContainingDeclarationSymbol(
        symbol: CfirBasedSymbol<*>,
        block: () -> R,
    ): R {
        containingDeclarationSymbolStack += symbol
        return try {
            block()
        } finally {
            containingDeclarationSymbolStack.removeAt(containingDeclarationSymbolStack.lastIndex)
        }
    }

    /** 当前子声明应绑定到的宿主声明符号。 */
    private val currentContainingDeclarationSymbol: CfirBasedSymbol<*>?
        get() = containingDeclarationSymbolStack.lastOrNull()

    /** 库声明的解析阶段在签名事务发布前完成。 */
    private fun CfirDeclaration.markResolved() {
        initDefaultResolveState()
        replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
    }

    // ---- 声明转换方法 ----

    /**
     * `BuiltInDecl` → [CfirBuiltInDeclaration]。
     *
     * 官方 `ASTLoader::LoadBuiltInDecl` 只接受 `BuiltInInfo` union，并按 FlatBuffers
     * `BuiltInType` 恢复声明种类、通用参数和约束。这里保持同样的严格边界：格式中的
     * union 类型、内建编号、名称/class id、泛型参数数量和约束形状任一不一致都属于
     * CJO 格式错误，不能静默跳过或降级为普通 class-like 声明。
     */
    private fun convertBuiltIn(index: Int, decl: Decl): CfirBuiltInDeclaration {
        check(decl.infoType == DeclInfo.BuiltInInfo) {
            "BuiltInDecl '${decl.identifier ?: "<anonymous>"}' must contain BuiltInInfo, " +
                "actual infoType=${decl.infoType}"
        }
        val info = decl.info(BuiltInInfo()) as? BuiltInInfo
            ?: error("BuiltInDecl '${decl.identifier ?: "<anonymous>"}' has no BuiltInInfo payload")
        val kind = when (info.builtInType) {
            BuiltInType.Array -> CfirBuiltInTypeKind.ARRAY
            BuiltInType.VArray -> CfirBuiltInTypeKind.VARRAY
            BuiltInType.CPointer -> CfirBuiltInTypeKind.CPOINTER
            BuiltInType.CString -> CfirBuiltInTypeKind.CSTRING
            BuiltInType.CFunc -> CfirBuiltInTypeKind.CFUNC
            else -> error(
                "Unknown BuiltInType=${info.builtInType} for '${decl.identifier ?: "<anonymous>"}'",
            )
        }
        val name = decl.classLikeName()
        check(name == kind.classId.shortClassName) {
            "BuiltInType ${kind.name} is encoded as '${name.asString()}', " +
                "expected '${kind.classId.shortClassName.asString()}'"
        }
        val classId = resolveClassId(decl, name)
        check(classId == kind.classId) {
            "BuiltInType ${kind.name} has classId $classId, expected ${kind.classId}"
        }

        val generic = decl.generic
        val serializedTypeParameterCount = generic?.typeParametersLength ?: 0
        check(serializedTypeParameterCount == kind.typeParameterCount) {
            "BuiltInType ${kind.name} has $serializedTypeParameterCount serialized type parameters, " +
                "expected ${kind.typeParameterCount}"
        }
        val expectedConstraintCount = if (kind == CfirBuiltInTypeKind.CPOINTER) 1 else 0
        val serializedConstraintCount = generic?.constraintsLength ?: 0
        check(serializedConstraintCount == expectedConstraintCount) {
            "BuiltInType ${kind.name} has $serializedConstraintCount serialized constraints, " +
                "expected $expectedConstraintCount"
        }

        val symbol = context.reserveClassLikeSymbol(index) as CfirBuiltInTypeSymbol
        check(symbol.classId == classId)
        val typeParameters = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        check(typeParameters.size == kind.typeParameterCount) {
            "BuiltInType ${kind.name} materialized ${typeParameters.size} type parameters, " +
                "expected ${kind.typeParameterCount}"
        }
        // 内建 normal header 与正式 builtin factory 同形，不改写独立的 native final bounds 产品。
        when (kind) {
            CfirBuiltInTypeKind.ARRAY, CfirBuiltInTypeKind.VARRAY, CfirBuiltInTypeKind.CFUNC ->
                typeParameters.forEach(CfirTypeParameter::addDefaultBoundIfNecessary)
            CfirBuiltInTypeKind.CPOINTER, CfirBuiltInTypeKind.CSTRING -> Unit
        }
        val typeParameterRefs: MutableList<CfirTypeParameterRef> = typeParameters
            .mapTo(mutableListOf()) { it }

        val declaration = CfirBuiltInDeclaration(
            contentSource = context.publishedContentSource,
            moduleData = context.moduleData,
            symbol = symbol,
            name = name,
            kind = kind,
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
            annotations = MutableOrEmptyList.empty<CfirAnnotation>(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            typeParameters = typeParameterRefs,
            status = buildStatus(decl),
            deprecationsProvider = UnresolvedDeprecationProvider,
            declarations = mutableListOf(),
            superTypeRefs = mutableListOf(),
        )
        declaration.markResolved()
        return declaration
    }

    /** ClassDecl → CfirClass */
    private fun convertClass(index: Int, decl: Decl): CfirClass {
        val name = decl.classLikeName()
        val classId = resolveClassId(decl, name)
        val symbol = context.reserveClassLikeSymbol(index) as CfirClassSymbol
        check(symbol.classId == classId)
        val status = buildStatus(decl).resolvedForStatuslessDeclaration()
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(ClassInfo()) as? ClassInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let {
            withClassLikeOwner(ClassLikeOwnerContext(classId, typeParams, typeDeserializer.deserializeTypeFromField(decl.type))) {
                withContainingDeclarationSymbol(symbol) {
                    deserializeBody(it::body, it.bodyLength)
                }
            }
        } ?: mutableListOf()

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val cfirClass = CfirClassImpl(
            contentSource = context.publishedContentSource,
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = UnresolvedDeprecationProvider,
            status = status,
            typeParameters = typeParams,
            symbol = symbol,
            superTypeRefs = superTypeRefs,
            declarations = members,
            name = name,
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
        )
        symbol.bind(cfirClass)
        cfirClass.markResolved()
        return cfirClass
    }

    /** InterfaceDecl → CfirInterface */
    private fun convertInterface(index: Int, decl: Decl): CfirInterface {
        val name = decl.classLikeName()
        val classId = resolveClassId(decl, name)
        check(classId.relativeClassName.asString().isNotBlank()) {
            "Blank interface ClassId for decl.identifier='${decl.identifier}' pkg='${packageFqName.asString()}'"
        }
        val symbol = context.reserveClassLikeSymbol(index) as CfirInterfaceSymbol
        check(symbol.classId == classId)
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(InterfaceInfo()) as? InterfaceInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let {
            withClassLikeOwner(ClassLikeOwnerContext(classId, typeParams, typeDeserializer.deserializeTypeFromField(decl.type))) {
                withContainingDeclarationSymbol(symbol) {
                    deserializeBody(it::body, it.bodyLength)
                }
            }
        } ?: mutableListOf()

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val cfirInterface = CfirInterfaceImpl(
            contentSource = context.publishedContentSource,
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = UnresolvedDeprecationProvider,
            declarations = members,
            status = status,
            typeParameters = typeParams,
            symbol = symbol,
            superTypeRefs = superTypeRefs,
            name = name,
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
        )
        symbol.bind(cfirInterface)
        cfirInterface.markResolved()
        return cfirInterface
    }

    /** StructDecl → CfirStruct */
    private fun convertStruct(index: Int, decl: Decl): CfirStruct {
        val name = decl.classLikeName()
        val classId = resolveClassId(decl, name)
        val symbol = context.reserveClassLikeSymbol(index) as CfirStructSymbol
        check(symbol.classId == classId)
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(StructInfo()) as? StructInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let {
            withClassLikeOwner(ClassLikeOwnerContext(classId, typeParams, typeDeserializer.deserializeTypeFromField(decl.type))) {
                withContainingDeclarationSymbol(symbol) {
                    deserializeBody(it::body, it.bodyLength)
                }
            }
        } ?: mutableListOf()

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val cfirStruct = CfirStructImpl(
            contentSource = context.publishedContentSource,
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = UnresolvedDeprecationProvider,
            status = status,
            typeParameters = typeParams,
            symbol = symbol,
            superTypeRefs = superTypeRefs,
            declarations = members,
            name = name,
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
        )
        symbol.bind(cfirStruct)
        cfirStruct.markResolved()
        return cfirStruct
    }

    /** EnumDecl → CfirEnum */
    private fun convertEnum(index: Int, decl: Decl): CfirEnum {
        val name = decl.classLikeName()
        val isRefEnum = false
        val classId = resolveClassId(decl, name)
        val symbol = context.reserveClassLikeSymbol(index) as CfirEnumSymbol
        check(symbol.classId == classId)
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(EnumInfo()) as? EnumInfo
        val isNonExhaustive = info?.nonExhaustive == true
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = info?.let {
            withClassLikeOwner(ClassLikeOwnerContext(classId, typeParams, typeDeserializer.deserializeTypeFromField(decl.type))) {
                withEnumOwner(EnumOwnerContext(classId, typeParams, isRefEnum)) {
                    withContainingDeclarationSymbol(symbol) {
                        deserializeBody(it::body, it.bodyLength)
                    }
                }
            }
        } ?: mutableListOf()

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val cfirEnum = CfirEnumImpl(
            contentSource = context.publishedContentSource,
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = UnresolvedDeprecationProvider,
            status = status,
            typeParameters = typeParams,
            symbol = symbol,
            superTypeRefs = superTypeRefs,
            declarations = members,
            name = name,
            isRefEnum = isRefEnum,
            isNonExhaustive = isNonExhaustive,
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
        )
        symbol.bind(cfirEnum)
        cfirEnum.markResolved()
        return cfirEnum
    }

    /** FuncDecl → CfirFunction */
    private fun convertFunctionOrEnumConstructor(decl: Decl): CfirDeclaration {
        checkNotNull((decl.info(FuncInfo()) as? FuncInfo)?.funcBody) {
            "FuncDecl requires a real FuncInfo/FuncBody; an empty body index is a different capability"
        }
        val finalizer = testAttr(decl, AttrBit.FINALIZER)
        // wire 保存源码 ~init，CFIR symbol 使用内部 SpecialNames.END_INIT（<~init>）。
        val finalizerName = decl.identifier == "~init"
        check(finalizer == finalizerName) { "CJO finalizer name and FINALIZER attribute disagree" }
        if (finalizer) return convertFinalizer(decl)
        convertConstructorIfNeeded(decl)?.let { return it }
        if (isEnumConstructorDecl(decl)) {
            return convertEnumConstructorFromFunctionDecl(decl)
        }

        val status = buildStatus(decl)
        val name = normalizedLibraryFunctionName(decl, status)
        val symbol = CfirNamedFunctionSymbol(callableIdForCurrentOwner(name))
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val returnTypeRef = buildFunctionReturnTypeRef(decl)

        val valueParams = deserializeFunctionParameters(decl, symbol)

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val cfirFunc = CfirNamedFunctionImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = sourceDeclarationIsLocal,
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status),
            status = status,
            deprecationsProvider = UnresolvedDeprecationProvider,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            valueParameters = valueParams,
            hasVariableLenArg = false,
            contentSource = context.publishedContentSource,
            body = null, // 库声明不加载函数体
            isMut = testAttr(decl, AttrBit.MUT),
        )
        cfirFunc.markResolved()
        return cfirFunc
    }

    /** 在首次声明 header 建立时恢复 finalizer；原 F、body 与词法子图只绑定这一实体。 */
    private fun convertFinalizer(decl: Decl): CfirFinalizer {
        check(!testAttr(decl, AttrBit.CONSTRUCTOR) && !testAttr(decl, AttrBit.PRIMARY_CONSTRUCTOR) &&
            !testAttr(decl, AttrBit.ENUM_CONSTRUCTOR)) { "A CJO finalizer cannot carry constructor attributes" }
        check(currentContainingDeclarationSymbol is CfirClassSymbol) { "A CJO finalizer must be owned by a class" }
        check(!sourceDeclarationIsLocal && (decl.generic?.typeParametersLength ?: 0) == 0) {
            "A CJO finalizer cannot be local or own type parameters"
        }
        val status = buildStatus(decl)
        check(!status.isStatic) { "A CJO finalizer cannot be static" }
        val symbol = CfirFinalizerSymbol(callableIdForCurrentOwner(SpecialNames.END_INIT))
        val returnType = buildFunctionReturnTypeRef(decl)
        check(returnType.coneTypeOrNull == ConePrimitiveType.UNIT || returnType.coneTypeOrNull == ConeInvalidType) {
            "A CJO finalizer must retain Unit or its explicit unexported signature"
        }
        val wireBody = checkNotNull((decl.info(FuncInfo()) as FuncInfo).funcBody)
        if (returnType.coneTypeOrNull != ConeInvalidType) {
            val signature = checkNotNull(typeDeserializer.deserializeTypeFromField(decl.type) as? ConeFunctionType)
            check(signature.parameterTypes.isEmpty() && !signature.hasVariableLenArg && signature.returnType == ConePrimitiveType.UNIT) {
                "A normal CJO finalizer must retain its complete zero-parameter Unit FuncTy"
            }
            check(wireBody.paramListsLength == 1 && checkNotNull(wireBody.paramLists(0)).paramsLength == 0) {
                "A normal CJO finalizer must have its actual single empty parameter list"
            }
        }
        val parameters = deserializeFunctionParameters(decl, symbol)
        check(parameters.isEmpty()) { "A CJO finalizer cannot have value parameters" }
        val annotations = MutableOrEmptyList.empty<CfirAnnotation>()
        return buildFinalizer {
            source = null
            moduleData = context.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            this.symbol = symbol
            this.status = status
            returnTypeRef = returnType
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status)
            deprecationsProvider = UnresolvedDeprecationProvider
            contentSource = context.publishedContentSource
            body = null
        }.also { it.initContainingClassAttr(); it.markResolved() }
    }

    /**
     * `.cjo` 中 operator 函数的 identifier 仍是源代码文本（如 `==`、`!=`、`[]`）。
     * PSI 与 LightTree raw CFIR 会先归一化到 `OperatorNameConventions`，库声明也必须恢复成同一名称，
     * 否则 std.core 中的真实 operator 成员无法被调用解析命中。
     */
    private fun normalizedLibraryFunctionName(decl: Decl, status: CfirDeclarationStatus): Name {
        val rawName = decl.identifier ?: "???"
        if (!status.isOperator) return Name.identifier(rawName)

        val valueParameterDecls = functionValueParameterDecls(decl)
        return when (rawName) {
            "-", OperatorNameConventions.MINUS.asString(), OperatorNameConventions.UNARY_MINUS.asString() ->
                if (valueParameterDecls.isEmpty()) OperatorNameConventions.UNARY_MINUS else OperatorNameConventions.MINUS

            "+", OperatorNameConventions.PLUS.asString(), OperatorNameConventions.UNARY_PLUS.asString() ->
                if (valueParameterDecls.isEmpty()) OperatorNameConventions.UNARY_PLUS else OperatorNameConventions.PLUS

            "[]", OperatorNameConventions.GET.asString(), OperatorNameConventions.SET.asString() ->
                if (valueParameterDecls.lastOrNull().isNamedValueParameter()) OperatorNameConventions.SET else OperatorNameConventions.GET

            else -> rawName.asOperatorName()
        }
    }

    /** 读取函数声明所有参数列表中的原始参数 Decl。 */
    private fun functionValueParameterDecls(decl: Decl): List<Decl> {
        val funcInfo = decl.info(FuncInfo()) as? FuncInfo ?: return emptyList()
        val funcBody = funcInfo.funcBody ?: return emptyList()
        val parameterDecls = mutableListOf<Decl>()

        for (i in 0 until funcBody.paramListsLength) {
            val paramList = funcBody.paramLists(i) ?: continue
            for (j in 0 until paramList.paramsLength) {
                val paramIndex = decodeDeclRef(paramList.params(j)) ?: continue
                val paramDecl = try {
                    context.pkg.allDecls(paramIndex)
                } catch (_: IndexOutOfBoundsException) {
                    null
                } ?: continue
                parameterDecls += paramDecl
            }
        }

        return parameterDecls
    }

    /** 判断参数 Decl 是否为命名参数 `value!`，用于区分 `[]` 的 get/set operator。 */
    private fun Decl?.isNamedValueParameter(): Boolean {
        val param = this ?: return false
        val info = param.info(ParamInfo()) as? ParamInfo ?: return false
        return info.isNamedParam && param.identifier == "value"
    }

    /** 根据当前 owner 上下文构造 callable id，成员 callable 使用 classId，顶层 callable 使用包名。 */
    private fun callableIdForCurrentOwner(name: Name): CallableId {
        if (sourceDeclarationIsLocal) return CallableId(name)
        val containingClass = currentContainingDeclarationSymbol as? CfirClassLikeSymbol<*>
        return containingClass?.let { CallableId(it.classId, name) } ?: CallableId(packageFqName, name)
    }

    /**
     * 库实例成员与源码成员共享声明接收者类型，类型参数复用当前 owner 的符号。
     * 对齐 FIR 反序列化上下文的 dispatchReceiver；此时 owner 尚未 bind，不能读取其声明体。
     */
    private fun dispatchReceiverTypeForCurrentOwner(status: CfirDeclarationStatus): ConeSimpleCangJieType? {
        if (status.isStatic) return null
        val ownerSymbol = currentContainingDeclarationSymbol as? CfirClassLikeSymbol<*>
        if (ownerSymbol == null) {
            // 普通 class-like 成员走上面的 owner 分支；`extend` 成员没有 class-like owner，
            // 其接收者就是被扩展类型（对齐 Kotlin 中「成员接收者 = 声明所在类型」的语义）。
            return currentExtendReceiverType
        }
        val owner = checkNotNull(currentClassLikeOwner)
        check(owner.classId == ownerSymbol.classId)
        return ownerSymbol.constructType(owner.typeParameters.map { it.symbol.constructType() }) as ConeSimpleCangJieType
    }

    /** 从反序列化结构上下文恢复 constructor/static callable 的名义 owner，保留声明站点身份。 */
    private fun CfirCallableDeclaration.initContainingClassAttr() {
        containingClassForStaticMemberAttr =
            (containingDeclarationSymbolStack.lastOrNull { it is CfirClassLikeSymbol<*> } as? CfirClassLikeSymbol<*>)?.toLookupTag()
    }

    /**
     * common-part `.cjo` 会把普通构造器序列化成 `FuncDecl`，
     * 但语义身份仍由 `Attribute.CONSTRUCTOR` / `Attribute.PRIMARY_CONSTRUCTOR` 标注。
     * 这里必须把 primary/secondary 形态一并恢复，后续作用域、检查器、analysis API 才能看到真实 constructor shape。
     */
    private fun convertConstructorIfNeeded(decl: Decl): CfirConstructor? {
        if (!testAttr(decl, AttrBit.CONSTRUCTOR)) return null
        val containingClass = checkNotNull(currentContainingDeclarationSymbol as? CfirClassLikeSymbol<*>) { "Constructor requires its exact wire class-like parent" }
        val isPrimary = testAttr(decl, AttrBit.PRIMARY_CONSTRUCTOR)

        val symbol = CfirConstructorSymbol(CallableId(containingClass.classId, SpecialNames.INIT))
        val status = buildStatus(decl)
        // 官方构造器不能声明泛型；实例构造器借用所属名义类型参数，static init 不借用。
        check((decl.generic?.typeParametersLength ?: 0) == 0) {
            "A Cangjie constructor cannot own generic parameters: ${containingClass.classId}"
        }
        val typeParams = if (status.isStatic) emptyList()
            else constructedClassTypeParameterRefs(currentClassLikeOwner?.typeParameters.orEmpty())
        val valueParams = deserializeFunctionParameters(decl, symbol)
        // constructor 的语义结果取实际 owner self，不能把 wire retType 的 Unit 当作对象结果。
        val returnTypeRef = buildResolvedTypeRef { coneType = checkNotNull(currentClassLikeOwner).selfType }

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val constructor = if (isPrimary) {
            buildPrimaryConstructor {
                source = null
                moduleData = context.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                annotations.addAll(deserializedAnnotations)
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                deprecationsProvider = UnresolvedDeprecationProvider
                dispatchReceiverType = null
                this.status = status
                this.typeParameters.addAll(typeParams)
                this.returnTypeRef = returnTypeRef
                this.valueParameters.addAll(valueParams)
                contentSource = context.publishedContentSource
                body = null
                this.symbol = symbol
            }
        } else {
            buildConstructor {
                source = null
                moduleData = context.moduleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                annotations.addAll(deserializedAnnotations)
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                deprecationsProvider = UnresolvedDeprecationProvider
                dispatchReceiverType = null
                this.status = status
                this.typeParameters.addAll(typeParams)
                this.returnTypeRef = returnTypeRef
                this.valueParameters.addAll(valueParams)
                contentSource = context.publishedContentSource
                body = null
                this.symbol = symbol
            }
        }
        constructor.markResolved()
        return constructor
    }

    /** PropDecl → CfirProperty */
    private fun convertProperty(decl: Decl): CfirProperty {
        val name = Name.identifier(decl.identifier ?: "???")
        val symbol = CfirPropertySymbol(callableIdForCurrentOwner(name))
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val returnTypeRef = buildTypeRef(decl.type)
        val propertyInfo = if (decl.infoType == DeclInfo.PropInfo) decl.info(PropInfo()) as? PropInfo else null
        require(propertyInfo == null || propertyInfo.gettersLength <= 1) {
            "PropDecl '${decl.identifier}' has ${propertyInfo?.gettersLength} getters; CFIR property has one getter slot"
        }
        require(propertyInfo == null || propertyInfo.settersLength <= 1) {
            "PropDecl '${decl.identifier}' has ${propertyInfo?.settersLength} setters; CFIR property has one setter slot"
        }
        val getter = propertyInfo
            ?.takeIf { it.gettersLength > 0 }
            ?.getters(0)
            ?.let(::decodeDeclRef)
            ?.let { deserializePropertyAccessor(it, symbol, isGetter = true) }
        val setter = propertyInfo
            ?.takeIf { it.settersLength > 0 }
            ?.setters(0)
            ?.let(::decodeDeclRef)
            ?.let { deserializePropertyAccessor(it, symbol, isGetter = false) }

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val cfirProp = CfirPropertyImpl(
            contentSource = context.publishedContentSource,
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status),
            status = status,
            deprecationsProvider = UnresolvedDeprecationProvider,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            getter = getter,
            setter = setter,
            bodyResolveState = CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED,
        )
        symbol.bind(cfirProp)
        cfirProp.markResolved()
        return cfirProp
    }

    /** Restore an accessor declaration referenced by PropInfo. */
    private fun deserializePropertyAccessor(
        declIndex: Int,
        propertySymbol: CfirPropertySymbol,
        isGetter: Boolean,
    ): CfirPropertyAccessor? {
        context.declarations[declIndex]?.let { existing ->
            val accessor = checkNotNull(existing as? CfirPropertyAccessor) { "CJO accessor d lost its canonical role" }
            check(accessor.propertySymbol === propertySymbol && accessor.isGetter == isGetter)
            return accessor
        }
        val decl = context.pkg.allDecls(declIndex) ?: return null
        if (decl.infoType != DeclInfo.FuncInfo) return null
        val symbol = CfirPropertyAccessorSymbol()
        val status = buildStatus(decl)
        val returnTypeRef = buildFunctionReturnTypeRef(decl)
        val typeParameters = withContainingDeclarationSymbol(symbol) { deserializeTypeParameters(decl) }
        val valueParameters = deserializeFunctionParameters(decl, symbol)
        val annotations = MutableOrEmptyList.empty<CfirAnnotation>()
        return buildPropertyAccessor {
            source = null
            moduleData = context.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            this.annotations += annotations
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            deprecationsProvider = UnresolvedDeprecationProvider
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status)
            this.status = status
            this.typeParameters += typeParameters
            this.returnTypeRef = returnTypeRef
            this.valueParameters += valueParameters
            hasVariableLenArg = false
            contentSource = context.publishedContentSource
            body = null
            this.symbol = symbol
            this.propertySymbol = propertySymbol
            this.isGetter = isGetter
        }.also { accessor ->
            accessor.initContainingClassAttr()
            accessor.markResolved()
            check(context.declarations.putIfAbsent(declIndex, accessor) == null)
            publishDeserializedDeclaration(declIndex, decl, accessor)
        }
    }

    /** VarDecl → CfirVariable */
    private fun convertVariableOrEnumConstructor(decl: Decl): CfirDeclaration {
        if (isEnumConstructorDecl(decl)) {
            return convertEnumConstructorFromVariableDecl(decl)
        }

        val name = Name.identifier(checkNotNull(decl.identifier))
        val isField = !decl.isTopLevel && (
            testAttr(decl, Attribute.IN_CLASSLIKE.ordinal) || testAttr(decl, Attribute.IN_STRUCT.ordinal) ||
                currentContainingDeclarationSymbol is CfirClassLikeSymbol<*>
            )
        if (isField) check(currentContainingDeclarationSymbol is CfirClassLikeSymbol<*>) {
            "Field '${decl.identifier}' requires its exact wire class-like parent"
        }
        if (decl.isTopLevel && !sourceDeclarationIsLocal) return convertPlainPackageVariable(decl, name)
        val symbol = CfirFieldVariableSymbol(callableIdForCurrentOwner(name))
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val returnTypeRef = buildTypeRef(decl.type)

        val varInfo = if (decl.infoType == DeclInfo.VarInfo) decl.info(VarInfo()) as? VarInfo else null
        val isVar = varInfo?.isVar ?: false
        // 声明 header 先登记；真实 initializer 在共同完成队列中读取。
        val initializer: CfirExpression? = null

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val cfirVar = CfirFieldVariableImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = sourceDeclarationIsLocal,
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status),
            status = status,
            deprecationsProvider = UnresolvedDeprecationProvider,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            contentSource = context.publishedContentSource,
            initializer = initializer,
            isVar = isVar,
            isTypeImplicit = false,
        )
        cfirVar.hasSerializedInitializer = varInfo?.initializer.isSavedInitializer()
        cfirVar.isMemberParameter = varInfo?.isMemberParam == true
        restoreSerializedConstantValue(cfirVar, varInfo, decl.type,
            varInfo?.let { (decl.info(CjoSourceTable()) as CjoSourceTable).position() })
        cfirVar.markResolved()
        return cfirVar
    }

    /** 普通包变量的 wire symbol 与 CFIR 声明根共用同一存储定义。 */
    private fun convertPlainPackageVariable(decl: Decl, name: Name): CfirPatternVariable {
        val returnTypeRef = buildTypeRef(decl.type)
        val status = buildStatus(decl)
        val info = if (decl.infoType == DeclInfo.VarInfo) decl.info(VarInfo()) as? VarInfo else null
        val bindingSymbol = CfirPatternBindingSymbol(CallableId(packageFqName, name))
        val binding = CfirPatternBindingVariableImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            status = status.copy(),
            deprecationsProvider = UnresolvedDeprecationProvider,
            contentSource = context.publishedContentSource,
            isVar = info?.isVar == true,
            isTypeImplicit = false,
            symbol = bindingSymbol,
            typeParameters = mutableListOf(),
            returnTypeRef = returnTypeRef,
            name = name,
        )
        val pattern = buildBindingPattern {
            this.name = name
            bindingVariable = binding
            semanticTypeRef = returnTypeRef
        }
        val symbol = CfirPatternVariableSymbol(CallableId(Name.special("<pattern-variable>")))
        val typeParameters = withContainingDeclarationSymbol(symbol) { deserializeTypeParameters(decl) }
        val result = CfirPatternVariableImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            status = status,
            deprecationsProvider = UnresolvedDeprecationProvider,
            contentSource = context.publishedContentSource,
            initializer = null,
            isVar = binding.isVar,
            isTypeImplicit = false,
            symbol = symbol,
            typeParameters = typeParameters,
            returnTypeRef = returnTypeRef,
            pattern = pattern,
        )
        val hasInitializer = info?.initializer.isSavedInitializer()
        result.hasSerializedInitializer = hasInitializer
        binding.hasSerializedInitializer = hasInitializer
        restoreSerializedConstantValue(binding, info, decl.type,
            info?.let { (decl.info(CjoSourceTable()) as CjoSourceTable).position() })
        return result
    }

    private fun UInt?.isSavedInitializer(): Boolean =
        this != null && this != 0u && this != UInt.MAX_VALUE

    /** VarInfo 值先登记；真实 classifier/成员 header 完成后由 context 的唯一值产品材料化。 */
    private fun restoreSerializedConstantValue(variable: CfirVariable, info: VarInfo?, typeReference: UInt, hostPosition: Int?) {
        if (info != null && info.valueType != ConstValue.NONE) context.enqueueConstantInitializer(variable, info, typeReference, checkNotNull(hostPosition))
    }

    /** ExtendDecl → CfirExtend */
    private fun convertVariableWithPattern(decl: Decl): CfirPatternVariable {
        val status = buildStatus(decl)
        val returnTypeRef = buildTypeRef(decl.type)

        val info = if (decl.infoType == DeclInfo.VarWithPatternInfo) {
            decl.info(VarWithPatternInfo()) as? VarWithPatternInfo
        } else {
            null
        }
        val fallbackName = decl.identifier?.let(Name::identifier) ?: Name.special("<pattern>")
        val symbol = CfirPatternVariableSymbol(CallableId(Name.special("<pattern-variable>")))
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val pattern = deserializeIrrefutablePattern(
            position = (decl.info(CjoSourceTable()) as CjoSourceTable).tablePosition(8),
            outerStatus = status,
            outerIsLocal = sourceDeclarationIsLocal,
            outerIsVar = info?.isVar ?: false,
        )

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val cfirVar = CfirPatternVariableImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = sourceDeclarationIsLocal,
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status),
            status = status,
            deprecationsProvider = UnresolvedDeprecationProvider,
            contentSource = context.publishedContentSource,
            initializer = null,
            isVar = info?.isVar ?: false,
            isTypeImplicit = false,
            symbol = symbol,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            pattern = pattern,
        )
        cfirVar.markResolved()
        return cfirVar
    }

    /**
     * 反序列化不可反驳模式。
     *
     * 支持变量、tuple、类型、enum 与 wildcard 模式；缺失或未知模式使用绑定模式恢复声明骨架。
     */
    /** Header P 与正文使用同一个位置/token/普通节点域；零槽和未知 kind 是损坏格式。 */
    private fun deserializeIrrefutablePattern(
        position: Int,
        outerStatus: CfirDeclarationStatus,
        outerIsLocal: Boolean,
        outerIsVar: Boolean,
    ): CfirPattern {
        val table = CjoSourceTable().at(position, context.pkg.getByteBuffer())
        val wire = Pattern().__assign(position, context.pkg.getByteBuffer())
        val identity = context.bodyState.patternIdentities.getOrPut(position) {
            org.cangnova.cangjie.cfir.source.CfirSourcePatternIdentity.create()
        }
        context.bodyState.patterns[identity]?.let { return it }
        check(context.bodyState.patternsInProgress.add(identity)) { "Header pattern owning cycle" }
        try {
            check(wire.typesLength >= 1) { "A saved pattern must retain its own type slot" }
            val type = buildTypeRef(wire.types(0))
            fun child(ordinal: Int): CfirPattern = deserializeIrrefutablePattern(
                table.tablePosition(10, ordinal), outerStatus, outerIsLocal, outerIsVar,
            )
            fun operand(ordinal: Int): CfirExpression =
                deserializeRequiredExpression(wire.exprs(ordinal), "pattern expression", CjoSourceContentRoot.SelectedExpression(
                    context.sourceHeaderRegistry.expressionContexts.getValue(wire.exprs(ordinal)).single()))
            val result = when (wire.kind) {
                PatternKind.VarPattern -> {
                    check(wire.exprsLength == 1 && wire.patternsLength == 0)
                    val index = checkNotNull(decodeDeclRef(wire.exprs(0)))
                    val declaration = checkNotNull(context.pkg.allDecls(index))
                    check(declaration.kind == DeclKind.VarDecl)
                    val variable = deserializePatternBindingVariable(
                        rawDeclRef = wire.exprs(0),
                        outerIsLocal = outerIsLocal,
                    )
                    buildBindingPattern { name = variable.name; bindingVariable = variable; semanticTypeRef = type }
                }
                PatternKind.WildcardPattern -> {
                    check(wire.exprsLength == 0 && wire.patternsLength == 0)
                    buildWildcardPattern { semanticTypeRef = type }
                }
                PatternKind.TuplePattern -> buildTuplePattern {
                    semanticTypeRef = type
                    elements += (0 until wire.patternsLength).map(::child)
                }
                PatternKind.TypePattern -> {
                    check(wire.typesLength == 1 && wire.patternsLength == 1 && wire.exprsLength == 0)
                    buildTypePattern {
                        semanticTypeRef = type; typeRef = type; nestedPattern = child(0)
                        matchBeforeRuntime = wire.matchBeforeRuntime; needRuntimeTypeCheck = wire.needRuntimeTypeCheck
                    }
                }
                PatternKind.ConstPattern -> {
                    check(wire.exprsLength in 1..2 && wire.patternsLength == 0)
                    buildConstPattern {
                        semanticTypeRef = type; expression = operand(0)
                        operatorCall = if (wire.exprsLength == 2) operand(1) else null
                    }
                }
                PatternKind.EnumPattern -> {
                    check(wire.exprsLength == 1)
                    buildEnumPattern {
                        semanticTypeRef = type; constructor = operand(0)
                        arguments += (0 until wire.patternsLength).map(::child)
                    }
                }
                PatternKind.ExceptTypePattern -> {
                    check(wire.patternsLength == 1 && wire.exprsLength == 0)
                    buildCatchPattern {
                        semanticTypeRef = type; nestedPattern = child(0)
                        typeRefs += (1 until wire.typesLength).map { buildTypeRef(wire.types(it)) }
                    }
                }
                PatternKind.CommandTypePattern -> {
                    check(wire.patternsLength == 1 && wire.exprsLength == 0)
                    buildCommandTypePattern {
                        semanticTypeRef = type; nestedPattern = child(0)
                        typeRefs += (1 until wire.typesLength).map { buildTypeRef(wire.types(it)) }
                    }
                }
                else -> error("Unsupported saved pattern kind " + wire.kind)
            }
            result.replaceConstantValues(context.constDeserializer.read(wire, position))
            val sourcePosition = context.sourcePositions(wire.begin, wire.end)
            result.replaceSource(sourcePosition.begin?.let {
                object : org.cangnova.cangjie.source.CjBinarySourceElement(
                    "CJO pattern", it.filePath, identity, it,
                ) {}
            })
            check(context.bodyState.patterns.put(identity, result) == null)
            return result
        } finally { check(context.bodyState.patternsInProgress.remove(identity)) }
    }

    /** 绑定声明必须来自真实一基 d 槽；其名称只作显示，不参与目标选择。 */
    private fun deserializePatternBindingVariable(
        rawDeclRef: UInt,
        outerIsLocal: Boolean,
    ): CfirPatternBindingVariable {
        val bindingIndex = checkNotNull(decodeDeclRef(rawDeclRef))
        context.declarations[bindingIndex]?.let { existing ->
                return checkNotNull(existing as? CfirPatternBindingVariable) {
                    "A pattern binding declaration must retain its canonical binding kind"
                }
        }
        val bindingDecl = checkNotNull(context.pkg.allDecls(bindingIndex))
        check(bindingDecl.kind == DeclKind.VarDecl && bindingDecl.infoType == DeclInfo.VarInfo)
        val bindingInfo = bindingDecl.info(VarInfo()) as VarInfo
        val bindingName = Name.identifier(checkNotNull(bindingDecl.identifier))
        val status = cloneStatus(buildStatus(bindingDecl))
        val symbol = CfirPatternBindingSymbol(
            if (outerIsLocal) CallableId(bindingName) else CallableId(packageFqName, bindingName),
        )
        val typeParameters = withContainingDeclarationSymbol(symbol) { deserializeTypeParameters(bindingDecl) }
        val returnTypeRef = buildTypeRef(bindingDecl.type)

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        return CfirPatternBindingVariableImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = outerIsLocal,
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status),
            status = status,
            deprecationsProvider = UnresolvedDeprecationProvider,
            contentSource = context.publishedContentSource,
            isVar = bindingInfo.isVar,
            isTypeImplicit = false,
            symbol = symbol,
            typeParameters = typeParameters,
            returnTypeRef = returnTypeRef,
            name = bindingName,
        ).also {
            it.hasSerializedInitializer = bindingInfo.initializer.isSavedInitializer()
            check(context.declarations.putIfAbsent(bindingIndex, it) == null) {
                "A pattern binding declaration was materialized twice"
            }
            publishDeserializedDeclaration(bindingIndex, bindingDecl, it)
        }
    }

    /** 二进制模式绑定继承同一完成状态值；副本保持 resolved 类型及独立身份，不创建 raw 状态。 */
    private fun cloneStatus(status: CfirDeclarationStatus): CfirDeclarationStatus = status.copy()

    /** 从 `allExprs` 中的 reference expression 读取原始引用文本。 */
    private fun extractReferenceName(exprIndex: Int): String? {
        val expr = context.pkg.allExprs(exprIndex) ?: return null
        if (expr.infoType != ExprInfo.ReferenceInfo) return null
        val referenceInfo = expr.info(ReferenceInfo()) as? ReferenceInfo ?: return null
        return referenceInfo.reference
    }

    /**
     * 将 ExtendDecl 反序列化为 [CfirExtend]。
     *
     * extend 的目标类型来自 `Decl.type`，继承类型和成员声明来自 [ExtendInfo]。
     */
    private fun convertExtend(decl: Decl): CfirExtend {
        val symbol = CfirExtendSymbol()
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }

        val extendInfo = decl.info(ExtendInfo()) as? ExtendInfo
        // 被扩展的类型来自 decl.type
        val extendedTypeRef = buildTypeRef(decl.type)
        // 成员接收者 = 被扩展类型；`extend<T> Foo<T>` 的接收者此时已带上该 extend 的类型参数 symbol。
        val extendReceiverType = extendedTypeRef.coneType as? ConeSimpleCangJieType
        val superTypeRefs = if (extendInfo != null) {
            deserializeInheritedTypes(extendInfo::inheritedTypes, extendInfo.inheritedTypesLength)
        } else mutableListOf()
        val members = if (extendInfo != null) {
            withContainingDeclarationSymbol(symbol) {
                withExtendReceiverType(extendReceiverType) {
                    deserializeBody(extendInfo::body, extendInfo.bodyLength)
                }
            }
        } else emptyList()

        val cfirExtend = CfirExtendImpl(
            contentSource = context.publishedContentSource,
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty<CfirAnnotation>(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            status = status,
            typeParameters = typeParams,
            extendedTypeRef = extendedTypeRef,
            superTypeRefs = superTypeRefs,
            declarations = members.toMutableList(),
        )
        symbol.bind(cfirExtend)
        cfirExtend.markResolved()
        return cfirExtend
    }

    /** TypeAliasDecl → CfirTypeAlias */
    private fun convertTypeAlias(index: Int, decl: Decl): CfirTypeAlias {
        val name = decl.classLikeName()
        val symbol = context.reserveClassLikeSymbol(index) as CfirTypeAliasSymbol
        check(symbol.classId == resolveClassId(decl, name))
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }

        val aliasInfo = decl.info(AliasInfo()) as? AliasInfo
        val expandedTypeRef = if (aliasInfo != null) {
            buildTypeRef(aliasInfo.aliasedTy)
        } else {
            buildTypeRef(decl.type)
        }

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val cfirAlias = CfirTypeAliasImpl(
            contentSource = context.publishedContentSource,
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = UnresolvedDeprecationProvider,
            declarations = mutableListOf(),
            superTypeRefs = mutableListOf(),
            status = status,
            typeParameters = typeParams,
            name = name,
            expandedTypeRef = expandedTypeRef,
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
        )
        symbol.bind(cfirAlias)
        cfirAlias.markResolved()
        return cfirAlias
    }

    /** GenericParamDecl → CfirTypeParameter */
    private fun convertTypeParameter(decl: Decl): CfirTypeParameter {
        val name = Name.identifier(checkNotNull(decl.identifier).also { check(it.isNotBlank()) })
        val symbol = CfirTypeParameterSymbol()
        val containingDeclarationSymbol = currentContainingDeclarationSymbol
            ?: error("Type parameter '${name.asString()}' must be deserialized inside a containing declaration")

        val bounds = mutableListOf<CfirTypeRef>()

        val cfirParam = CfirTypeParameterImpl(
            contentSource = context.publishedContentSource,
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty<CfirAnnotation>(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            containingDeclarationSymbol = containingDeclarationSymbol,
            symbol = symbol,
            name = name,
            bounds = bounds,
        )
        symbol.bind(cfirParam)
        cfirParam.markResolved()
        return cfirParam
    }

    /** 将 VarDecl 形式的 enum constructor 反序列化为 [CfirEnumConstructor]。 */
    private fun convertEnumConstructorFromVariableDecl(decl: Decl): CfirEnumConstructor {
        val name = Name.identifier(decl.identifier ?: "???")
        val owner = checkNotNull(currentEnumOwner) { "Enum constructor requires its exact wire parent" }
        val symbol = CfirEnumConstructorSymbol(CallableId(owner.classId, name))
        val status = buildStatus(decl)
        val typeParams: MutableList<CfirTypeParameterRef> = if (owner != null) {
            constructedClassTypeParameterRefs(owner.typeParameters).toMutableList()
        } else deserializeTypeParameters(decl).toMutableList()

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val enumCtor = CfirEnumConstructorImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            status = status,
            deprecationsProvider = UnresolvedDeprecationProvider,
            typeParameters = typeParams,
            returnTypeRef = buildResolvedTypeRef { coneType = checkNotNull(currentClassLikeOwner).selfType },
            valueParameters = mutableListOf(),
            name = name,
            contentSource = context.publishedContentSource,
            body = null,
        )
        enumCtor.markResolved()
        return enumCtor
    }

    /** 将 FuncDecl 形式的 enum constructor 反序列化为 [CfirEnumConstructor]。 */
    private fun convertEnumConstructorFromFunctionDecl(decl: Decl): CfirEnumConstructor {
        val name = Name.identifier(decl.identifier ?: "???")
        val owner = checkNotNull(currentEnumOwner) { "Enum constructor requires its exact wire parent" }
        val symbol = CfirEnumConstructorSymbol(CallableId(owner.classId, name))
        val status = buildStatus(decl)
        val typeParams: MutableList<CfirTypeParameterRef> = if (owner != null) {
            constructedClassTypeParameterRefs(owner.typeParameters).toMutableList()
        } else withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }.toMutableList()
        val valueParameters = deserializeFunctionParameters(decl, symbol)

        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val enumCtor = CfirEnumConstructorImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            status = status,
            deprecationsProvider = UnresolvedDeprecationProvider,
            typeParameters = typeParams,
            returnTypeRef = buildResolvedTypeRef { coneType = checkNotNull(currentClassLikeOwner).selfType },
            valueParameters = valueParameters,
            name = name,
            contentSource = context.publishedContentSource,
            body = null,
        )
        enumCtor.markResolved()
        return enumCtor
    }

    /** 反序列化函数、构造器或 enum constructor 的值参数列表。 */
    private fun deserializeFunctionParameters(decl: Decl, functionSymbol: CfirBasedSymbol<*>): MutableList<CfirValueParameter> {
        val funcInfo = checkNotNull(decl.info(FuncInfo()) as? FuncInfo) { "Function parameters require FuncInfo" }
        val funcBody = checkNotNull(funcInfo.funcBody) { "Function parameters require the actual FuncBody, not a fabricated empty list" }
        return deserializeFunctionParameters(funcBody, functionSymbol)
    }

    /** named/lambda/accessor 同一参数协议：对齐 desugar 完成后才构造参数的 default metadata。 */
    private fun deserializeFunctionParameters(funcBody: FuncBody, functionSymbol: CfirBasedSymbol<*>): MutableList<CfirValueParameter> {
        val valueParameters = mutableListOf<CfirValueParameter>()

        for (i in 0 until funcBody.paramListsLength) {
            val paramList = funcBody.paramLists(i) ?: continue
            require(paramList.desugarsLength == 0 || paramList.desugarsLength == paramList.paramsLength) {
                "FuncParamList.desugars must be absent or parallel to params"
            }
            for (j in 0 until paramList.paramsLength) {
                val paramIndex = requireNotNull(decodeDeclRef(paramList.params(j))) {
                    "FuncParamList.params contains an invalid declaration reference"
                }
                val parameterDecl = requireNotNull(context.pkg.allDecls(paramIndex))
                require(parameterDecl.kind == DeclKind.FuncParam) { "FuncParamList.params must reference FuncParam" }
                val defaultFunctionReference = if (paramList.desugarsLength == 0) 0u else paramList.desugars(j)
                val defaultFunctionIndex = if (defaultFunctionReference == 0u || defaultFunctionReference == UInt.MAX_VALUE) {
                    null
                } else {
                    requireNotNull(decodeDeclRef(defaultFunctionReference)) { "FuncParamList.desugars contains an invalid declaration reference" }
                }
                // 这里尚未压入 functionSymbol：辅助函数与原函数共享 class/extend/file 外层。
                val defaultFunction = defaultFunctionIndex?.let { index ->
                    val helperDecl = requireNotNull(context.pkg.allDecls(index))
                    require(helperDecl.kind == DeclKind.FuncDecl) { "FuncParamList.desugars must reference FuncDecl" }
                    requireNotNull(deserializeDecl(index, parameterDefaults = null) as? CfirNamedFunction) {
                        "A default-value helper must materialize as CfirNamedFunction"
                    }
                }
                val defaults = CfirParameterDefaultValueMetadata(testAttr(parameterDecl, AttrBit.HAS_INITIAL), defaultFunction)
                val param = withContainingDeclarationSymbol(functionSymbol) {
                    requireNotNull(deserializeDecl(paramIndex, defaults) as? CfirValueParameter)
                }
                valueParameters += param
            }
        }

        return valueParameters
    }


    /** 根据当前包名和声明名解析 class-like 声明的 [ClassId]。 */
    private fun resolveClassId(decl: Decl, fallbackName: Name): ClassId {
        val name = decl.classLikeName(fallbackName)
        return ClassId(packageFqName, name)
    }

    /** 提取 class-like 声明名，缺失时使用稳定 fallback。 */
    private fun Decl.classLikeName(fallback: Name = Name.identifier("___missing_class_name___")): Name {
        val rawName = identifier?.takeIf { it.isNotBlank() }
        return rawName?.let(Name::identifier) ?: fallback
    }

    /** 将 FuncParam 声明反序列化为 [CfirValueParameter]。 */
    private fun convertValueParameter(decl: Decl, defaults: CfirParameterDefaultValueMetadata): CfirValueParameter {
        val info = decl.info(ParamInfo()) as? ParamInfo

        val name = Name.identifier(decl.identifier ?: "_")
        val symbol = CfirValueParameterSymbol(CallableId(name))
        val containingDeclarationSymbol = currentContainingDeclarationSymbol
            ?: error("Value parameter '${name.asString()}' must be deserialized inside a containing declaration")
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val returnTypeRef = buildTypeRef(decl.type)
        val isNamed = info?.isNamedParam ?: false
        val deserializedAnnotations = MutableOrEmptyList.empty<CfirAnnotation>()
        val cfirParam = CfirValueParameterImpl(
            contentSource = context.publishedContentSource,
            source = null,
            isNamed = isNamed,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            symbol = symbol,
            containingDeclarationSymbol = containingDeclarationSymbol,
            status = status,
            deprecationsProvider = UnresolvedDeprecationProvider,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            defaultValue = null,
            isTypeImplicit = false,

        )
        cfirParam.isMemberParameter = info?.isMemberParam == true
        cfirParam.defaultValueMetadata = defaults
        symbol.bind(cfirParam)
        cfirParam.markResolved()
        return cfirParam
    }

}
