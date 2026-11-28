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
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.evaluatedInitializer
import org.cangnova.cangjie.cfir.declarations.utils.hasSerializedInitializer
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructedClassTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.impl.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentStatus
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentView
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationArgumentViewEntry
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationResolveState
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirConstantValueExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.resolve.constants.CfirFloatConstantEvalUtils
import org.cangnova.cangjie.cfir.expressions.buildResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.toAnnotationArgumentView
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.declarations.builder.buildAnonymousFunction
import org.cangnova.cangjie.cfir.expressions.builder.buildAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
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
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirInteropTarget
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.arrayElementType
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

/**
 * 声明反序列化器：Decl → CfirDeclaration。
 *
 * 将 .cjo 中的 FlatBuffers Decl 表转换为 CFIR 声明树。
 * 所有反序列化声明的 origin 为 Library，签名 header 绑定后通过 postCompute 完成 BODY_RESOLVE。
 */
@OptIn(CfirImplementationDetail::class)
class CfirDeclDeserializer private constructor(
    /** 当前 `.cjo` 包的反序列化上下文、缓存与跨包解析入口。 */
    context: CfirDeserializationContext,
    /** 与当前声明反序列化器配套的类型反序列化器。 */
    typeDeserializer: CfirTypeDeserializer,
    /** compiled-file stub 只需要声明签名，不物化字段初始化表达式。 */
    private val loadFieldVariableInitializers: Boolean,
) {
    /** 为 CFIR 符号解析创建完整的声明反序列化器。 */
    constructor(context: CfirDeserializationContext, typeDeserializer: CfirTypeDeserializer) :
        this(context, typeDeserializer, loadFieldVariableInitializers = context.mode == CjoDeserializationMode.FULL)

    private val context = context.withMode(if (loadFieldVariableInitializers) CjoDeserializationMode.FULL else CjoDeserializationMode.SIGNATURE_ONLY)
    private val typeDeserializer = this.context.createTypeDeserializer()
    private val postComputeStack = mutableListOf<MutableList<() -> Unit>>()
    private val signatureComputeStack = mutableListOf<MutableList<() -> Unit>>()
    private val indexStack = mutableListOf<Int>()

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
        val typeParameters: List<CfirTypeParameter>,
    )

    /** 当前包完整包名。 */
    private val packageFqName: FqName = FqName(context.header.fullPkgName)
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

    /**
     * 反序列化指定索引的声明。
     * @param declIndex allDecls 中的索引（0-based）
     */
    /**
     * 记录反序列化声明的原始 CJO 源码位置，供 common-side 报告和候选 related-information 共用。
     * `begin.file` 为 `allFiles` 的 1 基下标。
     */
    private fun recordCjoDeclarationPosition(decl: PackageFormat.Decl, result: CfirDeclaration) {
        val begin = decl.begin ?: return
        val fileIndex = begin.file.toInt() - 1
        if (fileIndex !in 0 until context.pkg.allFilesLength) return
        val filePath = context.pkg.allFiles(fileIndex) ?: return
        result.cjoDeclarationPosition = CfirCjoDeclarationPosition(
            filePath = filePath,
            line = begin.line,
            column = begin.column,
            identifierLine = decl.identifierPos?.line,
            identifierColumn = decl.identifierPos?.column,
        )
    }

    /** 完整声明查询在模块事务完成后返回，内部重入不得观察半完成树。 */
    fun deserializeDecl(declIndex: Int): CfirDeclaration? {
        context.materialization.read { materializeHeader(declIndex) }
        check(context.declarationEntries[declIndex] !is CjoDeclarationCacheEntry.Header) {
            "Ready declaration requested during CJO postCompute: $declIndex"
        }
        return context.declCache[declIndex]?.declarationRoot
    }

    fun deserializeSymbol(declIndex: Int): CfirBasedSymbol<*>? {
        context.materialization.read { materializeHeader(declIndex) }
        check(context.declarationEntries[declIndex] !is CjoDeclarationCacheEntry.Header) {
            "Ready symbol requested during CJO postCompute: $declIndex"
        }
        return context.declCache[declIndex]?.wireSymbol
    }

    /** annotation/initializer 重入只取已绑定的正式 header；不强迫递归目标完成正文。 */
    internal fun deserializeSymbolForReference(declIndex: Int): CfirBasedSymbol<*>? =
        context.materialization.read { materializeHeader(declIndex)?.wireSymbol }

    /** 签名子节点已经属于当前真实 owner，不经过外部 ready-declaration 入口。 */
    private fun deserializeChildDecl(declIndex: Int): CfirDeclaration? = materializeHeader(declIndex, insideOwner = true)?.declarationRoot

    private fun materializeHeader(declIndex: Int, insideOwner: Boolean = false): CfirDeserializedDeclaration? {
        when (val entry = context.declarationEntries[declIndex]) {
            is CjoDeclarationCacheEntry.Header -> return entry.record
            is CjoDeclarationCacheEntry.Ready -> return entry.record
            is CjoDeclarationCacheEntry.Failed -> throw entry.failure
            CjoDeclarationCacheEntry.Building -> error("CJO signature cycle before binding: $declIndex")
            null -> Unit
        }
        if (declIndex !in 0 until context.pkg.allDeclsLength) return null
        if (!insideOwner && checkNotNull(context.pkg.allDecls(declIndex)).kind == DeclKind.FuncParam) {
            context.packageIndex.lambdaParameterOwnerIndex(declIndex)?.let { expressionIndex ->
                materializeAnonymousFunctionHeader(expressionIndex)
                return checkNotNull(context.headerRecordOrNull(declIndex))
            }
        }
        val parentIndex = context.packageIndex.parentIndex(declIndex)
        if (!insideOwner && parentIndex != null) {
            return withWireParent(parentIndex) { materializeHeader(declIndex, insideOwner = true) }
        }
        val decl = checkNotNull(context.pkg.allDecls(declIndex))
        context.declarationEntries[declIndex] = CjoDeclarationCacheEntry.Building
        context.materialization.onCompletion(
            publish = {
                val entry = context.declarationEntries[declIndex]
                check(entry is CjoDeclarationCacheEntry.Header)
                context.declarationEntries[declIndex] = CjoDeclarationCacheEntry.Ready(entry.record)
                context.declCache[declIndex] = entry.record
            },
            fail = { failure -> context.declarationEntries[declIndex] = CjoDeclarationCacheEntry.Failed(
                CjoDeclarationDeserializationException(context.header.fullPkgName, declIndex, context.mode, failure)) },
        )
        val tasks = mutableListOf<() -> Unit>()
        val signatures = mutableListOf<() -> Unit>()
        postComputeStack += tasks
        signatureComputeStack += signatures
        indexStack += declIndex
        try {
            val root = checkNotNull(convertDecl(decl)) { "Unsupported CJO declaration kind ${decl.kind}" }
            val wire = if (root is CfirPatternVariable && decl.kind == DeclKind.VarDecl) root.pattern.bindingVariables().single() else root
            check(root.symbol.isBound && wire.symbol.isBound)
            val record = CfirDeserializedDeclaration(root, wire.symbol)
            context.declarationEntries[declIndex] = CjoDeclarationCacheEntry.Header(record)
            // 先绑定 owner，再同步建立完整成员签名；常量对象的字段映射也依赖这些正式 records。
            signatures.forEach { it() }
            postCompute {
                publishDeclarationMetadata(declIndex, decl, wire)
                recordCjoDeclarationPosition(decl, wire)
                if (wire !== root) {
                    root.replaceAnnotations(wire.annotations)
                    root.serializedDeclarationAttributes = wire.serializedDeclarationAttributes
                    root.serializedInteropFacts = wire.serializedInteropFacts
                    root.annotationInfo = wire.annotationInfo
                    root.cjoDeclarationPosition = wire.cjoDeclarationPosition
                    root.cjoDeclarationIndex = wire.cjoDeclarationIndex
                    if (root is CfirCallableDeclaration && wire is CfirCallableDeclaration)
                        root.replaceDeprecationsProvider(wire.deprecationsProvider)
                }
            }
            tasks.forEach(context.materialization::postCompute)
            context.materialization.postCompute {
                root.initDefaultResolveState()
                root.replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
                if (wire !== root) {
                    wire.initDefaultResolveState()
                    wire.replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
                }
            }
            return record
        } finally {
            indexStack.removeAt(indexStack.lastIndex)
            postComputeStack.removeAt(postComputeStack.lastIndex)
            signatureComputeStack.removeAt(signatureComputeStack.lastIndex)
        }
    }

    /** 保留创建时的真实 owner context；完成任务不得继承另一个递归目标的栈。 */
    private fun postCompute(action: () -> Unit) {
        enqueueWithOwner(postComputeStack.last(), action)
    }

    /** 成员和 accessor 的完整签名必须在返回 header 前就绪，annotation/initializer 仍留在 postCompute。 */
    private fun postComputeSignature(action: () -> Unit) {
        enqueueWithOwner(signatureComputeStack.last(), action)
    }

    private fun enqueueWithOwner(tasks: MutableList<() -> Unit>, action: () -> Unit) {
        val enums = enumOwnerStack.toList()
        val classes = classLikeOwnerStack.toList()
        val containers = containingDeclarationSymbolStack.toList()
        val extensions = extendReceiverTypeStack.toList()
        tasks.add {
            val oldEnums = enumOwnerStack.toList()
            val oldClasses = classLikeOwnerStack.toList()
            val oldContainers = containingDeclarationSymbolStack.toList()
            val oldExtensions = extendReceiverTypeStack.toList()
            enumOwnerStack.clear(); enumOwnerStack.addAll(enums)
            classLikeOwnerStack.clear(); classLikeOwnerStack.addAll(classes)
            containingDeclarationSymbolStack.clear(); containingDeclarationSymbolStack.addAll(containers)
            extendReceiverTypeStack.clear(); extendReceiverTypeStack.addAll(extensions)
            try { action() } finally {
                enumOwnerStack.clear(); enumOwnerStack.addAll(oldEnums)
                classLikeOwnerStack.clear(); classLikeOwnerStack.addAll(oldClasses)
                containingDeclarationSymbolStack.clear(); containingDeclarationSymbolStack.addAll(oldContainers)
                extendReceiverTypeStack.clear(); extendReceiverTypeStack.addAll(oldExtensions)
            }
        }
    }

    /** 属性 accessor / 参数也保留所有真实祖先的 receiver 与 containing symbol context。 */
    private fun <R> withWireParent(parentIndex: Int, action: () -> R): R {
        val parent = checkNotNull(materializeHeader(parentIndex))
        val ancestor = context.packageIndex.parentIndex(parentIndex)
        return if (ancestor == null) withWireOwner(parent.declarationRoot, action)
        else withWireParent(ancestor) { withWireOwner(parent.declarationRoot, action) }
    }

    /** 冷读成员必须进入 wire parent 的实际绑定声明，而不是由名称构造一个 owner。 */
    private fun <R> withWireOwner(owner: CfirDeclaration, action: () -> R): R = withContainingDeclarationSymbol(owner.symbol) {
        when (owner) {
            is CfirEnum -> withClassLikeOwner(ClassLikeOwnerContext(owner.symbol.classId, owner.typeParameters)) {
                withEnumOwner(EnumOwnerContext(owner.symbol.classId, owner.typeParameters, owner.isRefEnum), action)
            }
            is CfirClassLikeDeclaration -> withClassLikeOwner(ClassLikeOwnerContext(owner.symbol.classId,
                owner.typeParameters.map { checkNotNull(it as? CfirTypeParameter) }), action)
            is CfirExtend -> withExtendReceiverType(checkNotNull(owner.extendedTypeRef.coneTypeOrNull as? ConeSimpleCangJieType), action)
            else -> action()
        }
    }

    /** 普通声明与直接构造的 pattern binding 共用唯一的 pre-publication 接缝。 */
    private fun publishDeclarationMetadata(index: Int, decl: Decl, result: CfirDeclaration) {
        if (result is CfirCallableDeclaration &&
            (result.status.isStatic || result is CfirConstructor || result is CfirEnumConstructor)
        ) {
            result.initContainingClassAttr()
        }
        result.cjoDeclarationIndex = index
        result.serializedDeclarationAttributes = CfirSerializedDeclarationAttributes(
            words = (0 until decl.attributesLength).map(decl::attributes),
        )
        result.serializedInteropFacts = serializedInteropFacts(decl)
        result.annotationInfo = serializedAnnotationInfo(decl)
        val additional = context.sidecar?.annotations(index, result).orEmpty()
        if (additional.isNotEmpty()) result.replaceAnnotations(result.annotations + additional)
        appendSerializedAnnotationMarker(result)
        when (result) {
            is CfirCallableDeclaration -> result.replaceDeprecationsProvider(deprecationsProviderFor(result.annotations))
            is CfirClassLikeDeclaration -> result.replaceDeprecationsProvider(deprecationsProviderFor(result.annotations))
            else -> Unit
        }
        if (result is CfirMemberDeclaration) {
            val target = result.annotationTargetFor()
            result.annotations.forEach { it.replaceAnnotationTarget(target) }
            if (result.annotations.isNotEmpty() || result.annotationInfo != null) {
                result.publishAnnotationInfo(context.moduleData.session)
            }
            result.publishInteropInfo(context.moduleData.session)
        }
    }

    /** 按 FlatBuffers [Decl.kind] 分派到具体 CFIR 声明转换器。 */
    private fun convertDecl(decl: Decl): CfirDeclaration? {
        return when (decl.kind) {
            DeclKind.ClassDecl -> convertClass(decl)
            DeclKind.InterfaceDecl -> convertInterface(decl)
            DeclKind.StructDecl -> convertStruct(decl)
            DeclKind.EnumDecl -> convertEnum(decl)
            DeclKind.FuncDecl -> if (currentContainingDeclarationSymbol is CfirPropertySymbol)
                convertPropertyAccessor(decl, currentContainingDeclarationSymbol as CfirPropertySymbol)
            else convertFunctionOrEnumConstructor(decl)
            DeclKind.PropDecl -> convertProperty(decl)
            DeclKind.VarDecl -> convertVariableOrEnumConstructor(decl)
            DeclKind.VarWithPatternDecl -> convertVariableWithPattern(decl)
            DeclKind.ExtendDecl -> convertExtend(decl)
            DeclKind.TypeAliasDecl -> convertTypeAlias(decl)
            DeclKind.GenericParamDecl -> convertTypeParameter(decl)
            DeclKind.FuncParam -> convertValueParameter(decl)
            DeclKind.BuiltInDecl -> convertBuiltIn(decl)
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
    private fun deserializeAnnotations(
        decl: Decl,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
    ): MutableOrEmptyList<CfirAnnotation> {
        postCompute { containingDeclarationSymbol.cfir.replaceAnnotations(deserializeAnnotationsNow(decl, containingDeclarationSymbol)) }
        return MutableOrEmptyList.empty()
    }

    private fun deserializeAnnotationsNow(decl: Decl, containingDeclarationSymbol: CfirBasedSymbol<*>): MutableOrEmptyList<CfirAnnotation> {
        if (decl.annotationsLength == 0) return MutableOrEmptyList.empty()

        val result = mutableListOf<CfirAnnotation>()
        for (index in 0 until decl.annotationsLength) {
            val serialized = checkNotNull(decl.annotations(index)) {
                "CJO declaration annotation #$index is absent"
            }
            result += checkNotNull(deserializeAnnotation(serialized, containingDeclarationSymbol)) {
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
        containingDeclarationSymbol: CfirBasedSymbol<*>,
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
        // For a custom annotation the official CJO writer stores the annotation
        // constructor FullId in Anno.target.  That FullId points to a child
        // FuncDecl, so resolving it as a class-like declaration loses the
        // annotation class entirely.  Resolve a direct class target first and
        // then walk the declaration parent chain for the constructor owner.
        val targetClassId = serialized.target?.let { target ->
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
                )
                val argumentName = serializedArgument.name?.takeIf(String::isNotBlank)
                add(
                    if (argumentName == null) {
                        expression
                    } else {
                        buildNamedArgumentExpression {
                            this.expression = expression
                            this.argumentName = Name.identifier(argumentName)
                        }
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

        val constructor = if (serialized.kind == AnnoKind.Custom) {
            val target = checkNotNull(serialized.target) { "Custom annotation is missing target FullId" }
            val symbol = context.fullIdResolver.resolveSymbol(target)
            checkNotNull(symbol.cfir as? CfirConstructor) { "Custom annotation target is not a constructor" }
        } else null
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
            annotation.replaceArgumentView(resolvedArguments.toAnnotationArgumentView(constructor?.valueParameters.orEmpty()))
            if (constructor != null) {
                annotation.replaceCalleeReference(buildResolvedNamedReference {
                    name = constructor.symbol.callableId.callableName
                    resolvedSymbol = constructor.symbol
                })
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
        ): CfirDeclDeserializer = CfirDeclDeserializer(
            context,
            typeDeserializer,
            loadFieldVariableInitializers = false,
        )

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

    /** 读取 initializer / annotation 的真实表达式闭包；callee 只取正式 symbol/signature，不展开函数体。 */
    private val expressionsUnderDeserialization = mutableSetOf<UInt>()
    private val expressionFunctionTargets = mutableListOf<CfirFunctionTarget>()

    private fun deserializeExpression(rawExprIndex: UInt): CfirExpression? {
        val exprIndex = decodeExprRef(rawExprIndex) ?: return null
        val expr = checkNotNull(context.pkg.allExprs(exprIndex))
        check(expressionsUnderDeserialization.add(rawExprIndex)) {
            "CJO expression cycle in " + context.header.fullPkgName + " at " + rawExprIndex
        }
        try {
            return when (expr.kind) {
                ExprKind.LitConstExpr -> deserializeLiteral(rawExprIndex)
                ExprKind.PrimitiveTypeExpr -> deserializePrimitiveTypeExpression(expr)
                ExprKind.TypeConvExpr -> {
                    requireOperands(expr, 1)
                    check(expr.infoType == ExprInfo.NONE)
                    val operand = deserializeRequiredExpression(expr.operands(0), "type conversion operand")
                    buildDeserializedTypeConversion(expr, operand)
                }
                ExprKind.RefExpr -> deserializeReference(expr)
                ExprKind.MemberAccess -> {
                    requireOperands(expr, 1)
                    deserializeReference(expr, deserializeRequiredExpression(expr.operands(0), "member receiver"))
                }
                ExprKind.CallExpr -> deserializeCall(expr)
                ExprKind.LambdaExpr -> deserializeLambda(exprIndex)
                ExprKind.ReturnExpr -> {
                    requireOperands(expr, 1)
                    val functionTarget = checkNotNull(expressionFunctionTargets.lastOrNull()) { "CJO return has no function owner" }
                    val value = deserializeRequiredExpression(expr.operands(0), "return value")
                    buildReturnExpression {
                        coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
                        target = functionTarget
                        result = value
                    }
                }
                ExprKind.BinaryExpr -> deserializeBinary(expr)
                ExprKind.UnaryExpr -> deserializeUnary(expr)
                ExprKind.Block -> deserializeBlock(expr)
                ExprKind.IfExpr -> {
                    requireOperands(expr, 3)
                    val condition = deserializeRequiredExpression(expr.operands(0), "if condition")
                    val thenBranch = deserializeRequiredExpression(expr.operands(1), "if then branch")
                    val thenBlock = if (thenBranch is CfirBlock) thenBranch else buildBlock {
                        coneTypeOrNull = thenBranch.coneTypeOrNull
                        statements += thenBranch
                    }
                    val elseBranch = deserializeExpression(expr.operands(2))
                    buildIfExpression {
                        coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
                        this.condition = condition
                        this.thenBranch = thenBlock
                        this.elseBranch = elseBranch
                    }
                }
                ExprKind.TupleLit -> buildTupleLiteral {
                    coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
                    for (i in 0 until expr.operandsLength) {
                        elements += deserializeRequiredExpression(expr.operands(i), "tuple operand #$i")
                    }
                }
                ExprKind.ArrayLit -> buildArrayLiteral {
                    coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
                    for (i in 0 until expr.operandsLength) {
                        elements += deserializeRequiredExpression(expr.operands(i), "array operand #$i")
                    }
                }
                ExprKind.ArrayExpr -> deserializeArrayConstruction(expr)
                else -> error("Unsupported CJO initializer node in " + context.header.fullPkgName +
                    ": raw=" + rawExprIndex + ", kind=" + expr.kind + ", union=" + expr.infoType)
            }
        } finally {
            expressionsUnderDeserialization.remove(rawExprIndex)
        }
    }

    /** 形状来自官方 ExprWriter / ExprLoader operands 合同，不能以相邻池条目推断。 */
    private fun requireOperands(expr: Expr, expected: Int) {
        check(expr.operandsLength == expected) { "CJO expression kind " + expr.kind + " requires $expected operands" }
    }

    /** 官方 PrimitiveTypeExpr 只保存 type；CFIR 使用正式 class-like reference 表示静态限定符。 */
    private fun deserializePrimitiveTypeExpression(expr: Expr): CfirExpression {
        requireOperands(expr, 0)
        check(expr.infoType == ExprInfo.NONE)
        val type = readPrimitiveExpressionType(expr)
        val symbol = context.lookupPrimitiveTypeSymbol(type.kind)
        return buildNamedAccessExpression {
            coneTypeOrNull = type
            calleeReference = buildResolvedNamedReference { name = symbol.name; resolvedSymbol = symbol }
        }
    }

    private fun readPrimitiveExpressionType(expr: Expr): ConePrimitiveType {
        check(expr.type != 0u && expr.type != UInt.MAX_VALUE) { "Primitive CJO expression is missing its type" }
        return typeDeserializer.deserializeTypeFromField(expr.type) as? ConePrimitiveType
            ?: error("CJO primitive expression has a non-primitive type")
    }

    /** 数值/Rune 转换具有独立 AST 节点；不存在可恢复或可制造的 callee FullId。 */
    private fun buildDeserializedTypeConversion(expr: Expr, operand: CfirExpression): CfirExpression {
        val destination = readPrimitiveExpressionType(expr)
        check(destination.kind.isNumeric || destination.kind == PrimitiveTypeKind.RUNE) {
            "CJO TypeConvExpr has a non-conversion target"
        }
        return buildTypeConversion {
            coneTypeOrNull = destination
            argument = operand
            targetTypeRef = buildResolvedTypeRef { coneType = destination }
        }
    }

    /** CallExpr 的 operand[0] 是 callee，其余 operand 是 FuncArg wrapper。 */
    private fun deserializeCall(expr: Expr): CfirExpression {
        check(expr.infoType == ExprInfo.CallInfo && expr.operandsLength > 0)
        val info = checkNotNull(expr.info(CallInfo()) as? CallInfo)
        val callee = deserializeRequiredExpression(expr.operands(0), "call callee")
        val access = callee as? CfirNamedAccessExpression
        val resolved = access?.calleeReference as? CfirResolvedNamedReference
        val declaration = resolved?.resolvedSymbol?.cfir
        if (resolved?.resolvedSymbol is CfirPrimitiveTypeSymbol) {
            // 未脱糖的 primitive callee 仍走两套 raw CFIR 的正式转换协议，不制造 constructor symbol。
            check(info.callKind != CallKind.CallFunctionPtr)
            requireOperands(expr, 2)
            val primitiveAccess = checkNotNull(access)
            check(primitiveAccess.typeArguments.isEmpty() && primitiveAccess.explicitReceiver == null)
            val argument = checkNotNull(deserializeFunctionArgument(expr.operands(1))) {
                "Primitive conversion cannot use an omitted argument"
            }
            check(argument !is org.cangnova.cangjie.cfir.expressions.CfirInoutArgumentExpression)
            val targetType = checkNotNull(primitiveAccess.coneTypeOrNull) as? ConePrimitiveType
                ?: error("Primitive callee is missing its type")
            check(readPrimitiveExpressionType(expr) == targetType) { "Primitive call result disagrees with its callee type" }
            return buildDeserializedTypeConversion(expr, argument)
        }
        val parameters = when (declaration) {
            is CfirFunction -> declaration.valueParameters
            is CfirEnumConstructor -> declaration.valueParameters
            else -> null
        }
        val arguments = mutableListOf<CfirExpression>()
        val mapping = LinkedHashMap<CfirExpression, CfirValueParameter>()
        for (i in 1 until expr.operandsLength) {
            val argument = deserializeFunctionArgument(expr.operands(i))
            val parameter = parameters?.getOrNull(i - 1)
            if (parameters != null) check(parameter != null) { "CJO call argument has no signature parameter" }
            if (argument == null) {
                // 官方 HAS_INITIAL wrapper 没有表达式子树；由实际 signature 参数表示默认值。
                check(parameter != null && parameter.defaultValue != null) { "CJO omitted argument has no default parameter" }
                continue
            }
            arguments += argument
            if (parameter != null) mapping[argument] = parameter
        }
        val rawArguments = buildArgumentList { this.arguments += arguments }
        return buildFunctionCall {
            coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
            if (access != null && info.callKind != CallKind.CallFunctionPtr) {
                calleeReference = access.calleeReference
                explicitReceiver = access.explicitReceiver
                dispatchReceiver = access.dispatchReceiver
                typeArguments += access.typeArguments
            } else {
                // 函数值调用沿既有 CFIR invoke 表示保留完整 callee 子树。
                calleeReference = buildNamedReference { name = OperatorNameConventions.INVOKE }
                explicitReceiver = callee
            }
            argumentList = if (parameters != null) buildResolvedArgumentList(rawArguments, mapping) else rawArguments
            origin = when (info.callKind) {
                CallKind.CallBuiltinFunction, CallKind.CallIntrinsicFunction -> CfirFunctionCallOrigin.CompilerCoreIntrinsic
                else -> CfirFunctionCallOrigin.Regular
            }
        }
    }

    /** initializer 与冷读参数共享正式 Lambda header；重复读取不得创建第二个参数 owner。 */
    private fun deserializeLambda(expressionIndex: Int): CfirExpression {
        val function = materializeAnonymousFunctionHeader(expressionIndex)
        completeAnonymousFunctionBody(expressionIndex, function)
        return buildAnonymousFunctionExpression {
            anonymousFunction = function
            isTrailingLambda = false
        }
    }

    /** 对齐 Kotlin childContext 的 containingDeclarationSymbol，并保留 CJO 表达式池身份。 */
    private fun materializeAnonymousFunctionHeader(expressionIndex: Int): CfirAnonymousFunction {
        when (val entry = context.anonymousFunctionEntries[expressionIndex]) {
            CjoAnonymousFunctionCacheEntry.Building -> error("CJO lambda signature cycle before binding: $expressionIndex")
            is CjoAnonymousFunctionCacheEntry.Header -> return entry.function
            is CjoAnonymousFunctionCacheEntry.BodyBuilding -> return entry.function
            is CjoAnonymousFunctionCacheEntry.BodyBuilt -> return entry.function
            is CjoAnonymousFunctionCacheEntry.Ready -> return entry.function
            is CjoAnonymousFunctionCacheEntry.Failed -> throw entry.failure
            null -> Unit
        }
        val expression = checkNotNull(context.pkg.allExprs(expressionIndex))
        check(expression.kind == ExprKind.LambdaExpr && expression.infoType == ExprInfo.LambdaInfo)
        val bodyInfo = checkNotNull(expression.info(LambdaInfo()) as? LambdaInfo).funcBody
        context.anonymousFunctionEntries[expressionIndex] = CjoAnonymousFunctionCacheEntry.Building
        context.materialization.onCompletion(
            publish = {
                val entry = context.anonymousFunctionEntries[expressionIndex]
                val function = if (loadFieldVariableInitializers) {
                    check(entry is CjoAnonymousFunctionCacheEntry.BodyBuilt)
                    entry.function
                } else {
                    check(entry is CjoAnonymousFunctionCacheEntry.Header)
                    entry.function
                }
                context.anonymousFunctionEntries[expressionIndex] = CjoAnonymousFunctionCacheEntry.Ready(function)
            },
            fail = { failure ->
                context.anonymousFunctionEntries[expressionIndex] = CjoAnonymousFunctionCacheEntry.Failed(failure)
            },
        )
        val symbol = CfirAnonymousFunctionSymbol()
        val function = withContainingDeclarationSymbol(symbol) {
            buildAnonymousFunction {
                moduleData = context.moduleData
                resolvePhase = CfirResolvePhase.TYPES
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = true
                status = CfirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL).resolvedWithEffectiveModality()
                this.symbol = symbol
                returnTypeRef = buildTypeRef(bodyInfo.retType)
                typeRef = buildTypeRef(expression.type)
                isLambda = true
                hasExplicitParameterList = bodyInfo.paramListsLength != 0
            }
        }
        check(symbol.isBound)
        context.anonymousFunctionEntries[expressionIndex] = CjoAnonymousFunctionCacheEntry.Header(function)
        withContainingDeclarationSymbol(symbol) {
            val parameters = mutableListOf<CfirValueParameter>()
            for (i in 0 until bodyInfo.paramListsLength) {
                val list = checkNotNull(bodyInfo.paramLists(i))
                for (j in 0 until list.paramsLength) {
                    val parameterIndex = checkNotNull(decodeDeclRef(list.params(j)))
                    check(context.packageIndex.lambdaParameterOwnerIndex(parameterIndex) == expressionIndex)
                    val parameter = checkNotNull(materializeHeader(parameterIndex, insideOwner = true))
                        .declarationRoot as? CfirValueParameter ?: error("Lambda parameter has the wrong wire family")
                    check(parameter.containingDeclarationSymbol === symbol)
                    parameters += parameter
                }
            }
            function.replaceValueParameters(parameters)
        }
        // 冷读参数也完成 FULL 的真实正文；SIGNATURE_ONLY 完全不进入该任务。
        context.materialization.postCompute {
            if (loadFieldVariableInitializers) completeAnonymousFunctionBody(expressionIndex, function)
            function.initDefaultResolveState()
            function.replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
        }
        return function
    }

    /** 正文同步递归保留表达式 cycle 检查；FullId 参数重入仍只取已经绑定的 header。 */
    private fun completeAnonymousFunctionBody(expressionIndex: Int, function: CfirAnonymousFunction) {
        when (val entry = context.anonymousFunctionEntries[expressionIndex]) {
            is CjoAnonymousFunctionCacheEntry.BodyBuilt, is CjoAnonymousFunctionCacheEntry.Ready -> return
            is CjoAnonymousFunctionCacheEntry.BodyBuilding -> error("CJO lambda expression cycle at $expressionIndex")
            is CjoAnonymousFunctionCacheEntry.Failed -> throw entry.failure
            is CjoAnonymousFunctionCacheEntry.Header -> check(entry.function === function)
            else -> error("CJO lambda body requested before binding: $expressionIndex")
        }
        check(loadFieldVariableInitializers)
        context.anonymousFunctionEntries[expressionIndex] = CjoAnonymousFunctionCacheEntry.BodyBuilding(function)
        val expression = checkNotNull(context.pkg.allExprs(expressionIndex))
        val bodyInfo = checkNotNull(expression.info(LambdaInfo()) as? LambdaInfo).funcBody
        withContainingDeclarationSymbol(function.symbol) {
            val target = CfirFunctionTarget(labelName = null, isLambda = true).also { it.bind(function) }
            expressionFunctionTargets += target
            try {
                val body = deserializeRequiredExpression(bodyInfo.body, "lambda body")
                function.replaceBody(if (body is CfirBlock) body else buildBlock {
                    coneTypeOrNull = body.coneTypeOrNull
                    statements += body
                })
            } finally {
                expressionFunctionTargets.removeAt(expressionFunctionTargets.lastIndex)
            }
        }
        context.anonymousFunctionEntries[expressionIndex] = CjoAnonymousFunctionCacheEntry.BodyBuilt(function)
    }

    private fun deserializeFunctionArgument(raw: UInt): CfirExpression? {
        val index = checkNotNull(decodeExprRef(raw)) { "Invalid CJO FuncArg reference $raw" }
        val expr = checkNotNull(context.pkg.allExprs(index))
        check(expr.infoType == ExprInfo.FuncArgInfo)
        val info = checkNotNull(expr.info(FuncArgInfo()) as? FuncArgInfo)
        if (info.isDefaultVal) {
            requireOperands(expr, 0)
            return null
        }
        requireOperands(expr, 1)
        val argument = deserializeRequiredExpression(expr.operands(0), "function argument")
        return if (info.withInout) buildInoutArgumentExpression {
            coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
            expression = argument
        } else argument
    }

    /** BinaryInfo 只保存 operator kind；沿正式 raw CFIR 表示恢复运算，不造 target symbol 或求值结果。 */
    private fun deserializeBinary(expr: Expr): CfirExpression {
        requireOperands(expr, 2)
        check(expr.infoType == ExprInfo.BinaryInfo)
        val info = checkNotNull(expr.info(BinaryInfo()) as? BinaryInfo)
        val left = deserializeRequiredExpression(expr.operands(0), "binary left operand")
        val right = deserializeRequiredExpression(expr.operands(1), "binary right operand")
        val logical = when (info.op) {
            OperatorKind.LogicAnd -> CfirBinaryOpKind.AND
            OperatorKind.LogicOr -> CfirBinaryOpKind.OR
            OperatorKind.Coalescing -> CfirBinaryOpKind.COALESCING
            OperatorKind.Pipeline -> CfirBinaryOpKind.PIPELINE
            OperatorKind.Composition -> CfirBinaryOpKind.COMPOSITION
            else -> null
        }
        if (logical != null) return buildBinaryOp {
            coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
            kind = logical
            this.left = left
            this.right = right
        }
        val comparison = when (info.op) {
            OperatorKind.LT -> CfirComparisonOp.LT
            OperatorKind.LE -> CfirComparisonOp.LE
            OperatorKind.GT -> CfirComparisonOp.GT
            OperatorKind.GE -> CfirComparisonOp.GE
            OperatorKind.Equal -> CfirComparisonOp.EQ
            OperatorKind.NotEqual -> CfirComparisonOp.NE
            else -> null
        }
        if (comparison != null) return buildComparisonExpression {
            coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
            operation = comparison
            this.left = left
            this.right = right
        }
        val operation = when (info.op) {
            OperatorKind.Power -> OperatorNameConventions.EXPONENTIATION
            OperatorKind.Multiply -> OperatorNameConventions.TIMES
            OperatorKind.Divide -> OperatorNameConventions.DIV
            OperatorKind.Remainder -> OperatorNameConventions.REM
            OperatorKind.Add -> OperatorNameConventions.PLUS
            OperatorKind.Subtract -> OperatorNameConventions.MINUS
            OperatorKind.BitLeftShift -> OperatorNameConventions.LEFT_SHIFT
            OperatorKind.BitRightShift -> OperatorNameConventions.RIGHT_SHIFT
            OperatorKind.BitAnd -> OperatorNameConventions.AND
            OperatorKind.BitOr -> OperatorNameConventions.OR
            OperatorKind.BitXor -> OperatorNameConventions.XOR
            else -> error("Invalid CJO binary operator " + info.op)
        }
        return buildFunctionCall {
            coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
            calleeReference = buildNamedReference { name = operation }
            explicitReceiver = left
            argumentList = buildArgumentList { arguments += right }
            origin = CfirFunctionCallOrigin.Operator
        }
    }

    private fun deserializeUnary(expr: Expr): CfirExpression {
        requireOperands(expr, 1)
        check(expr.infoType == ExprInfo.UnaryInfo)
        val info = checkNotNull(expr.info(UnaryInfo()) as? UnaryInfo)
        val operation = when (info.op) {
            OperatorKind.Add -> OperatorNameConventions.UNARY_PLUS
            OperatorKind.Subtract -> OperatorNameConventions.UNARY_MINUS
            OperatorKind.Not -> OperatorNameConventions.NOT
            else -> error("Invalid CJO unary operator " + info.op)
        }
        val operand = deserializeRequiredExpression(expr.operands(0), "unary operand")
        return buildFunctionCall {
            coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
            calleeReference = buildNamedReference { name = operation }
            explicitReceiver = operand
            origin = CfirFunctionCallOrigin.Operator
        }
    }

    /** BlockInfo 的各位区分 expression / declaration，最后一位是 unsafe，不能混用两个池。 */
    private fun deserializeBlock(expr: Expr): CfirExpression {
        check(expr.infoType == ExprInfo.BlockInfo)
        val info = checkNotNull(expr.info(BlockInfo()) as? BlockInfo)
        check(info.isExprLength == expr.operandsLength + 1)
        val block = buildBlock {
            coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
            for (i in 0 until expr.operandsLength) {
                val raw = expr.operands(i)
                if (info.isExpr(i)) {
                    statements += deserializeRequiredExpression(raw, "block expression #$i")
                } else {
                    val declaration = checkNotNull(materializeHeader(checkNotNull(decodeDeclRef(raw)))).declarationRoot
                    check(declaration is CfirStatement) { "CJO block declaration #$i is not a statement" }
                    statements += declaration
                }
            }
        }
        return if (info.isExpr(expr.operandsLength)) buildUnsafeExpression {
            coneTypeOrNull = block.coneTypeOrNull
            body = block
        } else block
    }

    /** ArrayExpr 必须恢复构造参数和可选 initFunc；空 raw Array 也不能伪造成 ArrayLit。 */
    private fun deserializeArrayConstruction(expr: Expr): CfirExpression {
        check(expr.infoType == ExprInfo.ArrayInfo)
        val info = checkNotNull(expr.info(ArrayInfo()) as? ArrayInfo)
        val type = typeDeserializer.deserializeTypeFromField(expr.type)
        val target = info.initFunc?.takeIf { it.pkgId != -1 }
            ?.let { context.fullIdResolver.resolveSymbol(it) }
        if (target != null) check(target is CfirFunctionSymbol<*>) { "Array initFunc is not a function symbol" }
        return buildFunctionCall {
            coneTypeOrNull = type
            calleeReference = if (target is CfirFunctionSymbol<*>) buildResolvedNamedReference {
                name = target.callableId.callableName
                resolvedSymbol = target
            } else buildNamedReference { name = Name.identifier(if (info.isValueArray) "VArray" else "Array") }
            origin = CfirFunctionCallOrigin.CompilerCoreIntrinsic
            if (type is ConeVArrayType) varraySizeLiteral = type.size.toString()
            val elementType = if (type is ConeVArrayType) type.elementType else type.arrayElementType
            if (elementType != null) typeArguments += buildResolvedTypeRef { coneType = elementType }
            argumentList = buildArgumentList {
                for (i in 0 until expr.operandsLength) {
                    arguments += checkNotNull(deserializeFunctionArgument(expr.operands(i))) {
                        "Array construction cannot contain a default FuncArg"
                    }
                }
            }
        }
    }

    private fun deserializeRequiredExpression(rawExprIndex: UInt, owner: String): CfirExpression =
        checkNotNull(deserializeExpression(rawExprIndex)) {
            "Cannot materialize CJO $owner (expression reference $rawExprIndex)"
        }

    /** wire target 决定身份；展示文本及 wire type 独立保留。 */
    private fun deserializeReference(expr: Expr, receiver: CfirExpression? = null): CfirExpression {
        check(expr.infoType == ExprInfo.ReferenceInfo) { "RefExpr is missing ReferenceInfo" }
        val info = checkNotNull(expr.info(ReferenceInfo()) as? ReferenceInfo)
        val name = Name.identifier(checkNotNull(info.reference).also { check(it.isNotBlank()) })
        val target = checkNotNull(info.target) { "RefExpr is missing target FullId" }
        val symbol = context.fullIdResolver.resolveSymbol(target)
        return buildNamedAccessExpression {
            coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
            explicitReceiver = receiver
            for (i in 0 until info.instTysLength) typeArguments += buildTypeRef(info.instTys(i))
            calleeReference = buildResolvedNamedReference { this.name = name; resolvedSymbol = symbol }
        }
    }

    /** Restore a CJO literal expression. */
    private fun deserializeLiteral(rawExprIndex: UInt): CfirExpression? {
        val exprIndex = decodeExprRef(rawExprIndex) ?: return null
        val expr = context.pkg.allExprs(exprIndex) ?: return null
        if (expr.kind != ExprKind.LitConstExpr || expr.infoType != ExprInfo.LitConstInfo) return null
        val literal = expr.info(LitConstInfo()) as? LitConstInfo ?: return null
        val rawValue = literal.strValue.orEmpty()

        val (kind, value) = when (literal.constKind) {
            LitConstKind.Integer -> CfirLiteralKind.INT to rawValue
            // 浮点载荷是数值（`Double`），与两条 raw 构建路径和 `CfirConstantValue.Primitive` 的约定一致。
            LitConstKind.Float -> CfirLiteralKind.FLOAT to CfirFloatConstantEvalUtils.parseFloatLiteral(rawValue)
            // 官方 RUNE_BYTE 定型 UInt8，与 Rune 分开建模。
            LitConstKind.RuneByte -> CfirLiteralKind.BYTE to rawValue
            LitConstKind.Rune,
            -> CfirLiteralKind.RUNE to rawValue
            LitConstKind.String,
            LitConstKind.JString,
            -> CfirLiteralKind.STRING to rawValue
            LitConstKind.Bool -> CfirLiteralKind.BOOLEAN to rawValue.toBooleanStrictOrNull()
            LitConstKind.Unit -> CfirLiteralKind.UNIT to null
            else -> return null
        }

        return buildLiteralExpression {
            coneTypeOrNull = typeDeserializer.deserializeTypeFromField(expr.type)
            this.kind = kind
            this.value = value
        }
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

    /** 构建 CfirDeclarationStatus */
    private fun buildStatus(decl: Decl): CfirDeclarationStatus {
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
    private fun appendSerializedAnnotationMarker(declaration: CfirDeclaration) {
        if (declaration.annotationInfo?.isAnnotation != true) return
        if (declaration.annotations
                .filterIsInstance<CfirAnnotationCall>()
                .any { it.annotationKind == org.cangnova.cangjie.annotations.BuiltInAnnotationKind.ANNOTATION }
        ) return

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
        declaration.replaceAnnotations(declaration.annotations + marker)
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
        val encodedReturnType = funcInfo.funcBody?.retType ?: 0u
        if (encodedReturnType != 0u) {
            return buildTypeRef(encodedReturnType)
        }

        val functionType = typeDeserializer.deserializeTypeFromField(decl.type) as? ConeFunctionType
            ?: error("FuncDecl '${decl.identifier ?: "<anonymous>"}' must carry FuncTy in Decl.type")
        return buildResolvedTypeRef {
            customRenderer = false
            coneType = functionType.returnType
        }
    }

    /** 反序列化泛型参数列表 */
    private fun deserializeTypeParameters(decl: Decl): MutableList<CfirTypeParameter> {
        val generic = decl.generic ?: return mutableListOf()
        val len = generic.typeParametersLength
        if (len == 0) return mutableListOf()
        val typeParameters = (0 until len).mapNotNullTo(mutableListOf()) { i ->
            val paramIndex = decodeDeclRef(generic.typeParameters(i)) ?: return@mapNotNullTo null
            deserializeChildDecl(paramIndex) as? CfirTypeParameter
        }
        applyOwnerGenericConstraints(generic, typeParameters)
        return typeParameters
    }

    /**
     * `.cjo` 的泛型约束挂在 owner `Decl.generic.constraints` 上，而不是
     * `GenericParamDecl.generic` 上。constraint.type 是被约束的类型参数，
     * constraint.uppers 才是其上界；反序列化后必须写回对应 CfirTypeParameter。
     */
    @Suppress("UNCHECKED_CAST")
    private fun applyOwnerGenericConstraints(
        generic: Generic,
        typeParameters: List<CfirTypeParameter>,
    ) {
        if (generic.constraintsLength == 0 || typeParameters.isEmpty()) return

        val typeParametersBySymbol = typeParameters.associateBy { it.symbol }
        for (i in 0 until generic.constraintsLength) {
            val constraint = checkNotNull(generic.constraints(i)) { "Missing owner generic constraint" }
            val constrainedType = typeDeserializer.deserializeTypeFromField(constraint.type)
            val constrainedSymbol = checkNotNull((constrainedType as? ConeTypeParameterType)?.lookupTag?.typeParameterSymbol) {
                "Owner constraint target is not a canonical type parameter"
            }
            val typeParameter = checkNotNull(typeParametersBySymbol[constrainedSymbol]) {
                "Owner constraint points outside its declared type parameters"
            }
            val mutableBounds = checkNotNull(typeParameter.bounds as? MutableList<CfirTypeRef>) {
                "Deserialized type parameter bounds must be mutable during signature construction"
            }
            mutableBounds.addAll((0 until constraint.uppersLength).map {
                buildTypeRef(constraint.uppers(it))
            })
        }
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
            deserializeChildDecl(declIndex)
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

    /** header 只完成签名状态；BODY_RESOLVE 由整个 postCompute 事务统一发布。 */
    private fun CfirDeclaration.markResolved() {
        initDefaultResolveState()
        replaceResolvePhase(CfirResolvePhase.TYPES)
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
    private fun convertBuiltIn(decl: Decl): CfirBuiltInDeclaration {
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

        val symbol = CfirBuiltInTypeSymbol(classId, kind)
        val typeParameters = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        check(typeParameters.size == kind.typeParameterCount) {
            "BuiltInType ${kind.name} materialized ${typeParameters.size} type parameters, " +
                "expected ${kind.typeParameterCount}"
        }
        val typeParameterRefs: MutableList<CfirTypeParameterRef> = typeParameters
            .mapTo(mutableListOf()) { it }

        val declaration = CfirBuiltInDeclaration(
            moduleData = context.moduleData,
            symbol = symbol,
            name = name,
            kind = kind,
            scopeProvider = context.moduleData.session.cangjieScopeProvider,
            annotations = deserializeAnnotations(decl, symbol),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            typeParameters = typeParameterRefs,
            status = buildStatus(decl),
            deprecationsProvider = EmptyDeprecationsProvider,
            declarations = mutableListOf(),
            superTypeRefs = mutableListOf(),
        )
        declaration.markResolved()
        return declaration
    }

    /** ClassDecl → CfirClass */
    private fun convertClass(decl: Decl): CfirClass {
        val name = decl.classLikeName()
        val classId = resolveClassId(decl, name)
        val symbol = CfirClassSymbol(classId)
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(ClassInfo()) as? ClassInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = mutableListOf<CfirDeclaration>()
        postComputeSignature { members += info?.let {
            withClassLikeOwner(ClassLikeOwnerContext(classId, typeParams)) {
                withContainingDeclarationSymbol(symbol) {
                    deserializeBody(it::body, it.bodyLength)
                }
            }
        } ?: emptyList() }

        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        val cfirClass = CfirClassImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
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
    private fun convertInterface(decl: Decl): CfirInterface {
        val name = decl.classLikeName()
        val classId = resolveClassId(decl, name)
        check(classId.relativeClassName.asString().isNotBlank()) {
            "Blank interface ClassId for decl.identifier='${decl.identifier}' pkg='${packageFqName.asString()}'"
        }
        val symbol = CfirInterfaceSymbol(classId)
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(InterfaceInfo()) as? InterfaceInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = mutableListOf<CfirDeclaration>()
        postComputeSignature { members += info?.let {
            withClassLikeOwner(ClassLikeOwnerContext(classId, typeParams)) {
                withContainingDeclarationSymbol(symbol) {
                    deserializeBody(it::body, it.bodyLength)
                }
            }
        } ?: emptyList() }

        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        val cfirInterface = CfirInterfaceImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
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
    private fun convertStruct(decl: Decl): CfirStruct {
        val name = decl.classLikeName()
        val classId = resolveClassId(decl, name)
        val symbol = CfirStructSymbol(classId)
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(StructInfo()) as? StructInfo
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = mutableListOf<CfirDeclaration>()
        postComputeSignature { members += info?.let {
            withClassLikeOwner(ClassLikeOwnerContext(classId, typeParams)) {
                withContainingDeclarationSymbol(symbol) {
                    deserializeBody(it::body, it.bodyLength)
                }
            }
        } ?: emptyList() }

        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        val cfirStruct = CfirStructImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
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
    private fun convertEnum(decl: Decl): CfirEnum {
        val name = decl.classLikeName()
        val isRefEnum = false
        val classId = resolveClassId(decl, name)
        val symbol = CfirEnumSymbol(classId, isRefEnum)
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val info = decl.info(EnumInfo()) as? EnumInfo
        val isNonExhaustive = info?.nonExhaustive == true
        val superTypeRefs =
            info?.let { deserializeInheritedTypes(it::inheritedTypes, it.inheritedTypesLength) } ?: mutableListOf()
        val members = mutableListOf<CfirDeclaration>()
        postComputeSignature { members += info?.let {
            withClassLikeOwner(ClassLikeOwnerContext(classId, typeParams)) {
                withEnumOwner(EnumOwnerContext(classId, typeParams, isRefEnum)) {
                    withContainingDeclarationSymbol(symbol) {
                        deserializeBody(it::body, it.bodyLength)
                    }
                }
            }
        } ?: emptyList() }

        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        val cfirEnum = CfirEnumImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
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

        // 从 FuncInfo.funcBody.paramLists 获取参数
        val valueParams = mutableListOf<CfirValueParameter>()
        val funcInfo = decl.info(FuncInfo()) as? FuncInfo
        val funcBody = funcInfo?.funcBody
        if (funcBody != null) {
            withContainingDeclarationSymbol(symbol) {
                for (i in 0 until funcBody.paramListsLength) {
                    val paramList = funcBody.paramLists(i) ?: continue
                    for (j in 0 until paramList.paramsLength) {
                        val paramIndex = decodeDeclRef(paramList.params(j)) ?: continue
                        val param = deserializeChildDecl(paramIndex) as? CfirValueParameter
                        if (param != null) {
                            val desugaredIndex = paramList.desugars(j)
                                .takeIf { it != 0u && it != UInt.MAX_VALUE }
                                ?.let(::decodeDeclRef)
                            param.desugaredParameter = desugaredIndex
                                ?.let(::deserializeChildDecl) as? CfirValueParameter
                            valueParams.add(param)
                        }
                    }
                }
            }
        }

        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        val cfirFunc = CfirNamedFunctionImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status),
            status = status,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            valueParameters = valueParams,
            hasVariableLenArg = false,
            body = null, // 库声明不加载函数体
            isMut = testAttr(decl, AttrBit.MUT),
        )
        symbol.bind(cfirFunc)
        cfirFunc.markResolved()
        return cfirFunc
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
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val valueParams = withContainingDeclarationSymbol(symbol) {
            deserializeFunctionParameters(decl)
        }
        val returnTypeRef = buildClassConstructorReturnTypeRef(checkNotNull(currentClassLikeOwner))

        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        val constructor = if (isPrimary) {
            buildPrimaryConstructor {
                source = null
                moduleData = context.moduleData
                resolvePhase = CfirResolvePhase.TYPES
                annotations.addAll(deserializedAnnotations)
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                deprecationsProvider = deprecationsProviderFor(deserializedAnnotations)
                dispatchReceiverType = null
                this.status = status
                this.typeParameters.addAll(typeParams)
                this.returnTypeRef = returnTypeRef
                this.valueParameters.addAll(valueParams)
                body = null
                this.symbol = symbol
            }
        } else {
            buildConstructor {
                source = null
                moduleData = context.moduleData
                resolvePhase = CfirResolvePhase.TYPES
                annotations.addAll(deserializedAnnotations)
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                deprecationsProvider = deprecationsProviderFor(deserializedAnnotations)
                dispatchReceiverType = null
                this.status = status
                this.typeParameters.addAll(typeParams)
                this.returnTypeRef = returnTypeRef
                this.valueParameters.addAll(valueParams)
                body = null
                this.symbol = symbol
            }
        }
        symbol.bind(constructor)
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
        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        val cfirProp = CfirPropertyImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status),
            status = status,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            getter = null,
            setter = null,
            bodyResolveState = CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED,
        )
        symbol.bind(cfirProp)
        postComputeSignature {
            withContainingDeclarationSymbol(symbol) {
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

                cfirProp.replaceGetter(getter)
                cfirProp.replaceSetter(setter)
            }
        }
        cfirProp.markResolved()
        return cfirProp
    }

    /** Restore an accessor declaration referenced by PropInfo. */
    private fun deserializePropertyAccessor(
        declIndex: Int,
        propertySymbol: CfirPropertySymbol,
        isGetter: Boolean,
    ): CfirPropertyAccessor {
        val accessor = checkNotNull(deserializeChildDecl(declIndex) as? CfirPropertyAccessor) { "PropInfo points to a non-accessor wire declaration: $declIndex" }
        check(accessor.propertySymbol === propertySymbol && accessor.isGetter == isGetter)
        return accessor
    }

    private fun convertPropertyAccessor(decl: Decl, propertySymbol: CfirPropertySymbol): CfirPropertyAccessor {
        val parentIndex = checkNotNull(context.packageIndex.parentIndex(indexStack.last()))
        val parentInfo = checkNotNull(context.pkg.allDecls(parentIndex)?.info(PropInfo()) as? PropInfo)
        val isGetter = (0 until parentInfo.gettersLength).any { decodeDeclRef(parentInfo.getters(it)) == indexStack.last() }
        check(isGetter || (0 until parentInfo.settersLength).any { decodeDeclRef(parentInfo.setters(it)) == indexStack.last() })
        val symbol = CfirPropertyAccessorSymbol()
        val status = buildStatus(decl)
        val returnTypeRef = buildFunctionReturnTypeRef(decl)
        val typeParameters = withContainingDeclarationSymbol(symbol) { deserializeTypeParameters(decl) }
        val valueParameters = mutableListOf<CfirValueParameter>()
        val funcBody = (decl.info(FuncInfo()) as? FuncInfo)?.funcBody
        if (funcBody != null) {
            withContainingDeclarationSymbol(symbol) {
                for (i in 0 until funcBody.paramListsLength) {
                    val list = funcBody.paramLists(i) ?: continue
                    for (j in 0 until list.paramsLength) {
                        decodeDeclRef(list.params(j))
                            ?.let { deserializeChildDecl(it) as? CfirValueParameter }
                            ?.let { parameter ->
                                val desugaredIndex = list.desugars(j)
                                    .takeIf { it != 0u && it != UInt.MAX_VALUE }
                                    ?.let(::decodeDeclRef)
                                parameter.desugaredParameter = desugaredIndex
                                    ?.let(::deserializeChildDecl) as? CfirValueParameter
                                valueParameters.add(parameter)
                            }
                    }
                }
            }
        }
        val annotations = deserializeAnnotations(decl, symbol)
        return buildPropertyAccessor {
            source = null
            moduleData = context.moduleData
            resolvePhase = CfirResolvePhase.TYPES
            this.annotations += annotations
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            deprecationsProvider = deprecationsProviderFor(annotations)
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status)
            this.status = status
            this.typeParameters += typeParameters
            this.returnTypeRef = returnTypeRef
            this.valueParameters += valueParameters
            hasVariableLenArg = false
            body = null
            this.symbol = symbol
            this.propertySymbol = propertySymbol
            this.isGetter = isGetter
        }.also { it.initContainingClassAttr() }
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
        val symbol: CfirVariableSymbol<*> = if (isField) {
            CfirFieldVariableSymbol(callableIdForCurrentOwner(name))
        } else {
            CfirPatternBindingSymbol(if (decl.isTopLevel) CallableId(packageFqName, name) else CallableId(name))
        }
        val status = buildStatus(decl)
        val typeParams = withContainingDeclarationSymbol(symbol) {
            deserializeTypeParameters(decl)
        }
        val returnTypeRef = buildTypeRef(decl.type)

        val varInfo = if (decl.infoType == DeclInfo.VarInfo) decl.info(VarInfo()) as? VarInfo else null
        val isVar = varInfo?.isVar ?: false
        // VarInfo.initializer is a 1-based expression-pool reference.  Restore
        // the expression when it is representable by the current CFIR
        // expression reader; do not synthesize a value from the declaration
        // name or from raw source text.
        val initializerReference = varInfo?.initializer?.let { rawIndex ->
            if (rawIndex == 0u || rawIndex == UInt.MAX_VALUE) {
                null
            } else {
                val expressionIndex = checkNotNull(decodeExprRef(rawIndex)) {
                    "CJO initializer reference $rawIndex for '${decl.identifier}' is outside the expression table"
                }
                checkNotNull(context.pkg.allExprs(expressionIndex)) {
                    "CJO initializer reference $rawIndex for '${decl.identifier}' points to an absent expression"
                }
                rawIndex
            }
        }
        val initializerInfo = varInfo?.takeIf { initializerReference != null }
        fun loadInitializer(): CfirExpression? = initializerInfo
            ?.takeIf { loadFieldVariableInitializers }
            ?.let { info ->
                withContainingDeclarationSymbol(symbol) {
                    deserializeRequiredExpression(info.initializer,
                        "initializer " + context.header.fullPkgName + "." + decl.identifier)
                }
            }

        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        if (!isField) {
            val bindingSymbol = symbol as CfirPatternBindingSymbol
            val binding = CfirPatternBindingVariableImpl(
                source = null,
                moduleData = context.moduleData,
                resolvePhase = CfirResolvePhase.TYPES,
                annotations = deserializedAnnotations,
                origin = CfirDeclarationOrigin.Library,
                attributes = CfirDeclarationAttributes.EMPTY,
                isLocal = !decl.isTopLevel,
                deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
                dispatchReceiverType = null,
                status = status,
                isVar = isVar,
                isTypeImplicit = false,
                symbol = bindingSymbol,
                typeParameters = typeParams,
                returnTypeRef = returnTypeRef,
                name = name,
            )
            bindingSymbol.bind(binding)
            binding.hasSerializedInitializer = initializerInfo != null
            val ownerSymbol = CfirPatternVariableSymbol(
                if (decl.isTopLevel) CallableId(packageFqName, Name.special("<pattern:${name.asString()}>"))
                else CallableId(Name.special("<pattern:${name.asString()}>")),
            )
            val root = CfirPatternVariableImpl(
                source = null,
                moduleData = context.moduleData,
                resolvePhase = CfirResolvePhase.TYPES,
                annotations = MutableOrEmptyList.empty(),
                origin = CfirDeclarationOrigin.Library,
                attributes = CfirDeclarationAttributes.EMPTY,
                isLocal = !decl.isTopLevel,
                dispatchReceiverType = null,
                status = status,
                deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
                initializer = null,
                isVar = isVar,
                isTypeImplicit = false,
                symbol = ownerSymbol,
                typeParameters = typeParams,
                returnTypeRef = returnTypeRef,
                pattern = buildBindingPattern { this.name = name; bindingVariable = binding },
            )
            ownerSymbol.bind(root)
            postCompute {
                root.replaceInitializer(loadInitializer())
                restoreSerializedConstantValue(binding, varInfo)
            }
            return root
        }
        val cfirVar = CfirFieldVariableImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            symbol = symbol as CfirFieldVariableSymbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status),
            status = status,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            initializer = null,
            isVar = isVar,
            isTypeImplicit = false,
        )
        cfirVar.hasSerializedInitializer = initializerInfo != null
        (symbol as CfirFieldVariableSymbol).bind(cfirVar)
        postCompute {
            cfirVar.replaceInitializer(loadInitializer())
            restoreSerializedConstantValue(cfirVar, varInfo)
        }
        cfirVar.markResolved()
        return cfirVar
    }

    /**
     * Publish the typed official VarInfo.ConstValue into the declaration's
     * existing constant-evaluation slot. Composite fields are bound against
     * the already materialized class-like declaration graph; field names are
     * never converted into synthetic symbols.
     */
    private fun restoreSerializedConstantValue(variable: CfirVariable, info: VarInfo?) {
        val serialized = info ?: return
        val wireValue = CjoConstValueReader.read(serialized, context.pkg) ?: return
        val constant = checkNotNull(
            materializeSerializedConstant(wireValue, variable.returnTypeRef.coneTypeOrNull),
        ) {
            "CJO constant value for '${variable.symbol.name}' cannot be represented by CFIR"
        }
        variable.evaluatedInitializer = org.cangnova.cangjie.cfir.CfirEvaluatorResult.Evaluated(
            CfirConstantValueExpression(source = null, constantValue = constant),
        )
    }

    private fun materializeSerializedConstant(
        value: CjoConstValueReader.Value,
        expectedType: ConeCangJieType?,
    ): CfirConstantValue? {
        return when (value) {
        is CjoConstValueReader.Value.Scalar -> {
            val primitive = when (value.tag) {
                ConstValue.Float32Value,
                ConstValue.Float64Value,
                -> CfirConstantValue.Primitive(
                    type = serializedScalarType(value.tag),
                    kind = CfirLiteralKind.FLOAT,
                    value = (value.value as? Number)?.toDouble() ?: return null,
                )

                ConstValue.Int8Value,
                ConstValue.Int16Value,
                ConstValue.Int32Value,
                ConstValue.Int64Value,
                ConstValue.UInt8Value,
                ConstValue.UInt16Value,
                ConstValue.UInt32Value,
                ConstValue.UInt64Value,
                -> CfirConstantValue.Primitive(
                    type = serializedScalarType(value.tag),
                    kind = CfirLiteralKind.INT,
                    value = when (val scalar = value.value) {
                        is UByte -> BigInteger.valueOf(scalar.toLong())
                        is UShort -> BigInteger.valueOf(scalar.toLong())
                        is UInt -> BigInteger(scalar.toString())
                        is ULong -> BigInteger(scalar.toString())
                        is Number -> BigInteger.valueOf(scalar.toLong())
                        else -> return null
                    },
                )

                else -> return null
            }
            primitive
        }
        is CjoConstValueReader.Value.StringValue ->
            CfirConstantValue.Primitive(stringConstantType(), CfirLiteralKind.STRING, value.value)
        is CjoConstValueReader.Value.ArrayValue -> {
            val elements = value.elements.mapIndexed { index, element ->
                checkNotNull(materializeSerializedConstant(element, expectedType?.arrayElementType)) {
                    "CJO array constant element #$index cannot be represented by CFIR"
                }
            }
            CfirConstantValue.ArrayValue(expectedType ?: ConePrimitiveType.UNIT, elements)
        }
        is CjoConstValueReader.Value.CompositeValue -> {
            val objectType = expectedType
                ?: value.type.takeIf { it != 0u }
                    ?.let(typeDeserializer::deserializeTypeFromField)
                ?: return null
            val fields = value.fields
            val classLikeSymbol = (objectType as? ConeClassLikeType)
                ?.classId
                ?.let(context::lookupClassLikeSymbol)
                ?: return null
            val membersByName = classLikeSymbol.cfir.declarations
                .asSequence()
                .filterIsInstance<CfirCallableDeclaration>()
                .associateBy { it.symbol.name }
            val fieldValues = linkedMapOf<CfirCallableSymbol<*>, CfirConstantValue>()
            for (field in fields) {
                val member = membersByName[Name.identifier(field.name)] ?: return null
                if (fieldValues.containsKey(member.symbol)) return null
                val memberType = member.returnTypeRef.coneTypeOrNull ?: return null
                val constant = materializeSerializedConstant(field.value, memberType) ?: return null
                fieldValues[member.symbol] = constant
            }
            CfirConstantValue.ObjectValue(
                type = objectType,
                fields = fieldValues,
                identity = value.index.toLong(),
            )
        }
        is CjoConstValueReader.Value.CompositeReference ->
            error("Raw composite reference ${value.index} reached semantic constant materialization")
        }
    }

    /** Preserve the primitive type carried by the official ConstValue union tag. */
    private fun serializedScalarType(tag: UByte): ConeCangJieType = when (tag) {
        ConstValue.Int8Value -> ConePrimitiveType.INT8
        ConstValue.Int16Value -> ConePrimitiveType.INT16
        ConstValue.Int32Value -> ConePrimitiveType.INT32
        ConstValue.Int64Value -> ConePrimitiveType.INT64
        ConstValue.UInt8Value -> ConePrimitiveType.UINT8
        ConstValue.UInt16Value -> ConePrimitiveType.UINT16
        ConstValue.UInt32Value -> ConePrimitiveType.UINT32
        ConstValue.UInt64Value -> ConePrimitiveType.UINT64
        ConstValue.Float32Value -> ConePrimitiveType.FLOAT32
        ConstValue.Float64Value -> ConePrimitiveType.FLOAT64
        else -> error("Not a scalar ConstValue tag: $tag")
    }

    /** Build the canonical std.core.String type when no declaration expected type exists. */
    private fun stringConstantType(): ConeCangJieType = ConeClassLikeType(
        lookupTag = ConeClassLikeLookupTagImpl(StdlibClassIds.String),
        typeArguments = emptyList(),
    )

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
            fbPattern = info?.irrefutablePattern,
            fallbackName = fallbackName,
            defaultTypeRef = returnTypeRef,
            outerStatus = status,
            outerIsLocal = false,
            outerIsVar = info?.isVar ?: false,
        )

        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        val cfirVar = CfirPatternVariableImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status),
            status = status,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
            initializer = null,
            isVar = info?.isVar ?: false,
            isTypeImplicit = false,
            symbol = symbol,
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            pattern = pattern,
        )
        symbol.bind(cfirVar)
        cfirVar.markResolved()
        return cfirVar
    }

    /**
     * 反序列化不可反驳模式。
     *
     * 支持变量、tuple、类型、enum 与 wildcard 模式；缺失或未知模式使用绑定模式恢复声明骨架。
     */
    private fun deserializeIrrefutablePattern(
        fbPattern: Pattern?,
        fallbackName: Name,
        defaultTypeRef: CfirResolvedTypeRef,
        outerStatus: CfirDeclarationStatus,
        outerIsLocal: Boolean,
        outerIsVar: Boolean,
    ): CfirPattern {
        if (fbPattern == null) {
            val bindingVariable = deserializePatternBindingVariable(
                rawDeclRef = null,
                fallbackName = fallbackName,
                defaultTypeRef = defaultTypeRef,
                outerStatus = outerStatus,
                outerIsLocal = outerIsLocal,
                outerIsVar = outerIsVar,
            )
            return buildBindingPattern {
                name = bindingVariable.name
                typeRef = bindingVariable.returnTypeRef
                this.bindingVariable = bindingVariable
            }
        }

        return when (fbPattern.kind) {
            PatternKind.VarPattern -> {
                require(fbPattern.typesLength == 1 && fbPattern.exprsLength == 1) {
                    "Malformed CJO VarPattern: expected exactly one type and one declaration reference"
                }
                val bindingVariable = deserializePatternBindingVariable(
                    rawDeclRef = fbPattern.exprs(0),
                    fallbackName = fallbackName,
                    defaultTypeRef = defaultTypeRef,
                    outerStatus = outerStatus,
                    outerIsLocal = outerIsLocal,
                    outerIsVar = outerIsVar,
                )
                buildBindingPattern {
                    name = bindingVariable.name
                    typeRef = bindingVariable.returnTypeRef
                    this.bindingVariable = bindingVariable
                }
            }

            PatternKind.TuplePattern -> buildTuplePattern {
                for (i in 0 until fbPattern.patternsLength) {
                    elements += deserializeIrrefutablePattern(
                        fbPattern = fbPattern.patterns(i),
                        fallbackName = fallbackName,
                        defaultTypeRef = defaultTypeRef,
                        outerStatus = outerStatus,
                        outerIsLocal = outerIsLocal,
                        outerIsVar = outerIsVar,
                    )
                }
            }

            PatternKind.TypePattern -> {
                require(fbPattern.typesLength == 1 && fbPattern.patternsLength == 1) {
                    "Malformed CJO TypePattern: expected exactly one type and one child pattern"
                }
                val resolvedTypeRef = buildTypeRef(fbPattern.types(0))
                val nestedPattern = fbPattern.patterns(0)
                val nestedBindingVariable = nestedPattern
                    ?.takeIf { it.kind == PatternKind.VarPattern }
                    ?.let {
                        deserializePatternBindingVariable(
                            rawDeclRef = it.exprs(0),
                            fallbackName = fallbackName,
                            defaultTypeRef = resolvedTypeRef,
                            outerStatus = outerStatus,
                            outerIsLocal = outerIsLocal,
                            outerIsVar = outerIsVar,
                        )
                    }
                buildTypePattern {
                    typeRef = resolvedTypeRef
                    bindingName = nestedBindingVariable?.name
                    bindingVariable = nestedBindingVariable
                }
            }

            PatternKind.EnumPattern -> buildEnumPattern {
                require(fbPattern.typesLength == 1 && fbPattern.exprsLength == 1) {
                    "Malformed CJO EnumPattern: expected exactly one type and one constructor expression"
                }
                val referenceName = decodeExprRef(fbPattern.exprs(0))
                    ?.let(::extractReferenceName)
                    ?.takeIf { it.isNotBlank() }
                    ?.substringAfterLast('.')
                    ?.let(Name::identifier)
                    ?: Name.special("<enum-pattern>")
                constructorReference = buildNamedReference {
                    name = referenceName
                    source = null
                }
                for (i in 0 until fbPattern.patternsLength) {
                    arguments += deserializeIrrefutablePattern(
                        fbPattern = fbPattern.patterns(i),
                        fallbackName = fallbackName,
                        defaultTypeRef = defaultTypeRef,
                        outerStatus = outerStatus,
                        outerIsLocal = outerIsLocal,
                        outerIsVar = outerIsVar,
                    )
                }
            }

            PatternKind.WildcardPattern -> buildWildcardPattern()
            else -> buildBindingPattern {
                val bindingVariable = deserializePatternBindingVariable(
                    rawDeclRef = null,
                    fallbackName = fallbackName,
                    defaultTypeRef = defaultTypeRef,
                    outerStatus = outerStatus,
                    outerIsLocal = outerIsLocal,
                    outerIsVar = outerIsVar,
                )
                name = bindingVariable.name
                typeRef = bindingVariable.returnTypeRef
                this.bindingVariable = bindingVariable
            }
        }
    }

    /**
     * 反序列化模式绑定变量。
     *
     * 如果模式引用了独立 binding Decl，则优先使用该 Decl 的名称、状态和类型；否则继承外层变量声明信息。
     */
    private fun deserializePatternBindingVariable(
        rawDeclRef: UInt?,
        fallbackName: Name,
        defaultTypeRef: CfirTypeRef,
        outerStatus: CfirDeclarationStatus,
        outerIsLocal: Boolean,
        outerIsVar: Boolean,
    ): CfirPatternBindingVariable {
        val bindingIndex = rawDeclRef?.let(::decodeDeclRef)
        val bindingDecl = bindingIndex?.let(context.pkg::allDecls)
        val bindingName = bindingDecl?.identifier?.let(Name::identifier) ?: fallbackName
        val status = bindingDecl?.let(::buildStatus)?.let(::cloneStatus) ?: cloneStatus(outerStatus)
        val returnTypeRef = bindingDecl
            ?.takeIf { it.type != 0u }
            ?.let { buildTypeRef(it.type) }
            ?: defaultTypeRef
        val symbol = CfirPatternBindingSymbol(
            if (outerIsLocal) CallableId(bindingName) else CallableId(packageFqName, bindingName),
        )

        val deserializedAnnotations = bindingDecl
            ?.let { deserializeAnnotations(it, symbol) }
            ?: MutableOrEmptyList.empty()
        return CfirPatternBindingVariableImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = outerIsLocal,
            dispatchReceiverType = dispatchReceiverTypeForCurrentOwner(status),
            status = status,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
            isVar = outerIsVar,
            isTypeImplicit = false,
            symbol = symbol,
            typeParameters = mutableListOf(),
            returnTypeRef = returnTypeRef,
            name = bindingName,
        ).also {
            symbol.bind(it)
            if (bindingIndex != null && bindingDecl != null) publishDeclarationMetadata(bindingIndex, bindingDecl, it)
        }
    }

    /**
     * 深复制声明状态。
     *
     * 模式绑定变量需要继承外层变量状态，但每个绑定变量必须拥有独立的 resolved status 实例。
     */
    private fun cloneStatus(status: CfirDeclarationStatus): CfirDeclarationStatus {
        return CfirDeclarationStatusImpl(
            visibility = status.visibility,
            modality = status.modality,
        ).also { copied ->
            copied.isVisibilityExplicit = status.isVisibilityExplicit
            copied.isModalityExplicit = status.isModalityExplicit
            copied.visibilityExplicitness = status.visibilityExplicitness
            copied.modalityExplicitness = status.modalityExplicitness
            copied.bodyPresence = status.bodyPresence
            copied.isDeclarationFile = status.isDeclarationFile
            copied.isAbstractExplicit = status.isAbstractExplicit
            copied.isOverride = status.isOverride
            copied.isOperator = status.isOperator
            copied.isStatic = status.isStatic
            copied.isConst = status.isConst
            copied.isMut = status.isMut
            copied.isUnsafe = status.isUnsafe
            copied.isForeign = status.isForeign
            copied.isC = status.isC
            copied.isCommon = status.isCommon
            copied.isFromCommonPart = status.isFromCommonPart
            copied.isSpecific = status.isSpecific
            copied.isCommonWithDefault = status.isCommonWithDefault
            copied.isRedef = status.isRedef
            copied.isDefault = status.isDefault
            copied.isAbstract = status.isAbstract
            copied.isOpen = status.isOpen
            copied.isSealed = status.isSealed
        }.resolvedWithEffectiveModality()
    }

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
        val members = mutableListOf<CfirDeclaration>()
        postComputeSignature { members += if (extendInfo != null) {
            withContainingDeclarationSymbol(symbol) {
                withExtendReceiverType(extendReceiverType) {
                    deserializeBody(extendInfo::body, extendInfo.bodyLength)
                }
            }
        } else emptyList() }

        val cfirExtend = CfirExtendImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializeAnnotations(decl, symbol),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            status = status,
            typeParameters = typeParams,
            extendedTypeRef = extendedTypeRef,
            superTypeRefs = superTypeRefs,
            declarations = members,
        )
        symbol.bind(cfirExtend)
        cfirExtend.markResolved()
        return cfirExtend
    }

    /** TypeAliasDecl → CfirTypeAlias */
    private fun convertTypeAlias(decl: Decl): CfirTypeAlias {
        val name = decl.classLikeName()
        val symbol = CfirTypeAliasSymbol(resolveClassId(decl, name))
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

        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        val cfirAlias = CfirTypeAliasImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
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
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializeAnnotations(decl, symbol),
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
        val typeParams = owner.typeParameters.mapTo(mutableListOf<CfirTypeParameterRef>()) { parameter ->
            buildConstructedClassTypeParameterRef { this.symbol = parameter.symbol }
        }

        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        val enumCtor = CfirEnumConstructorImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            status = status,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
            typeParameters = typeParams,
            returnTypeRef = buildEnumConstructorReturnTypeRef(owner),
            valueParameters = mutableListOf(),
            name = name,
        )
        symbol.bind(enumCtor)
        enumCtor.markResolved()
        return enumCtor
    }

    /** 将 FuncDecl 形式的 enum constructor 反序列化为 [CfirEnumConstructor]。 */
    private fun convertEnumConstructorFromFunctionDecl(decl: Decl): CfirEnumConstructor {
        val name = Name.identifier(decl.identifier ?: "???")
        val owner = checkNotNull(currentEnumOwner) { "Enum constructor requires its exact wire parent" }
        val symbol = CfirEnumConstructorSymbol(CallableId(owner.classId, name))
        val status = buildStatus(decl)
        val typeParams = owner.typeParameters.mapTo(mutableListOf<CfirTypeParameterRef>()) { parameter ->
            buildConstructedClassTypeParameterRef { this.symbol = parameter.symbol }
        }
        val valueParameters = withContainingDeclarationSymbol(symbol) {
            deserializeFunctionParameters(decl)
        }

        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        val enumCtor = CfirEnumConstructorImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            status = status,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
            typeParameters = typeParams,
            returnTypeRef = buildEnumConstructorReturnTypeRef(owner),
            valueParameters = valueParameters,
            name = name,
        )
        symbol.bind(enumCtor)
        enumCtor.markResolved()
        return enumCtor
    }

    /** 反序列化函数、构造器或 enum constructor 的值参数列表。 */
    private fun deserializeFunctionParameters(decl: Decl): MutableList<CfirValueParameter> {
        val funcInfo = decl.info(FuncInfo()) as? FuncInfo ?: return mutableListOf()
        val funcBody = funcInfo.funcBody ?: return mutableListOf()
        val valueParameters = mutableListOf<CfirValueParameter>()

        for (i in 0 until funcBody.paramListsLength) {
            val paramList = funcBody.paramLists(i) ?: continue
            for (j in 0 until paramList.paramsLength) {
                val paramIndex = decodeDeclRef(paramList.params(j)) ?: continue
                val param = deserializeChildDecl(paramIndex) as? CfirValueParameter ?: continue
                valueParameters += param
            }
        }

        return valueParameters
    }

    /** 构造 enum constructor 的返回类型引用，位于 enum owner 内时返回对应 enum 类型。 */
    private fun buildEnumConstructorReturnTypeRef(owner: EnumOwnerContext): CfirTypeRef {

        val typeArguments = owner.typeParameters.map { parameter ->
            ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag())
        }
        return buildResolvedTypeRef {
            customRenderer = false
            coneType = ConeEnumType(
                lookupTag = owner.classId.toLookupTag(),
                typeArguments = typeArguments,
                isRefEnum = owner.isRefEnum,
            )
        }
    }

    /** 构造 class-like 构造器返回类型引用，位于 owner 内时返回当前 class-like 类型。 */
    private fun buildClassConstructorReturnTypeRef(owner: ClassLikeOwnerContext): CfirTypeRef {

        val typeArguments = owner.typeParameters.map { parameter ->
            ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag())
        }
        return buildResolvedTypeRef {
            customRenderer = false
            coneType = ConeClassLikeType(
                lookupTag = owner.classId.toLookupTag(),
                typeArguments = typeArguments,
            )
        }
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
    private fun convertValueParameter(decl: Decl): CfirValueParameter {
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
        val serializedDefaultValue = info?.defaultVal?.let(::decodeExprRef)
        val defaultValue = if (testAttr(decl, AttrBit.HAS_INITIAL) || serializedDefaultValue != null) {
            buildLibraryDefaultValueMarker(returnTypeRef)
        } else {
            null
        }
        val deserializedAnnotations = deserializeAnnotations(decl, symbol)
        val cfirParam = CfirValueParameterImpl(
            source = null,
            isNamed = isNamed,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.TYPES,
            annotations = deserializedAnnotations,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            dispatchReceiverType = null,
            symbol = symbol,
            containingDeclarationSymbol = containingDeclarationSymbol,
            status = status,
            deprecationsProvider = deprecationsProviderFor(deserializedAnnotations),
            typeParameters = typeParams,
            returnTypeRef = returnTypeRef,
            name = name,
            defaultValue = defaultValue,
            isTypeImplicit = false,

        )
        symbol.bind(cfirParam)
        cfirParam.markResolved()
        return cfirParam
    }

    /** 对齐 Kotlin loadDefaultValue：只记录官方默认参数事实，签名标记不包含伪造的默认值。 */
    private fun buildLibraryDefaultValueMarker(typeRef: CfirResolvedTypeRef): CfirExpression = buildExpressionStub {
        coneTypeOrNull = typeRef.coneType
    }
}
