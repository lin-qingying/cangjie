

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.stubs.Stub
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirModifierExplicitness
import org.cangnova.cangjie.cfir.declarations.CfirBodyPresence
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildEnum
import org.cangnova.cangjie.cfir.declarations.builder.buildInterface
import org.cangnova.cangjie.cfir.declarations.builder.buildStruct
import org.cangnova.cangjie.cfir.declarations.builder.buildResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationMetadata
import org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationStatus
import org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationStatus.Flag
import org.cangnova.cangjie.psi.stubs.CangJieModifierListStub
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import org.cangnova.cangjie.source.CjRealPsiSourceElement
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/** 取 compiled PSI 元素的 backing stub：优先绿色 stub，缺失时即时计算并缓存。 */
internal inline val <T, reified S> T.compiledStub: S
        where T : StubBasedPsiElementBase<in S>, T : CjElement, S : StubElement<*>
    get() = (this.greenStub ?: calculateStub()) as S

/**
 * 为 compiled PSI 元素计算并返回 backing stub。
 */
private fun <S, T> T.calculateStub(): Stub
        where T : StubBasedPsiElementBase<in S>, T : CjElement, S : StubElement<*> {
    val cjFile = containingCjFile
    requireWithAttachment(cjFile.isCompiled, { "Expected compiled file" }) {
        withPsiEntry("cjFile", cjFile)
    }

    return cjFile.calcStubTree().let {
        val stub = greenStub
        requireWithAttachment(stub != null, { "Stub should be not null" }) {
            withPsiEntry("file", containingFile)
            withPsiEntry("element", this@calculateStub)
        }
        stub
    }
}

/** 当前声明自己的 CJO 状态快照；缺失是存根协议错误，不能由源码 token 或 consumer session 补齐。 */
internal val CjDeclaration.compiledDeclarationMetadata: CangJieCompiledDeclarationMetadata
    get() {
        val header = checkNotNull(modifierList) { "Compiled declaration has no metadata header: ${this::class}" }
        val stub: CangJieModifierListStub = header.compiledStub
        return checkNotNull(stub.compiledDeclarationMetadata) { "Compiled declaration has no CJO status: ${this::class}" }
    }

/** 在 ANALYZED_DEPENDENCIES 发布之前，从所属声明的完整 metadata 构建 resolved status。 */
internal fun CjDeclaration.buildResolvedStatus(): CfirResolvedDeclarationStatus =
    compiledDeclarationMetadata.status.toResolvedStatus()

/** 只改变 CFIR 状态表示形态，逐项保留当前/imported owner 的已解码 CJO 事实。 */
internal fun CangJieCompiledDeclarationStatus.toResolvedStatus(): CfirResolvedDeclarationStatus =
    buildResolvedDeclarationStatus {
        visibility = this@toResolvedStatus.visibility
        modality = this@toResolvedStatus.modality
        visibilityExplicitness = CfirModifierExplicitness.valueOf(this@toResolvedStatus.visibilityExplicitness.name)
        modalityExplicitness = CfirModifierExplicitness.valueOf(this@toResolvedStatus.modalityExplicitness.name)
        bodyPresence = CfirBodyPresence.valueOf(this@toResolvedStatus.bodyPresence.name)
        val flags = this@toResolvedStatus.flags
        isDeclarationFile = Flag.DECLARATION_FILE in flags
        isVisibilityExplicit = Flag.VISIBILITY_EXPLICIT in flags
        isModalityExplicit = Flag.MODALITY_EXPLICIT in flags
        isAbstractExplicit = Flag.ABSTRACT_EXPLICIT in flags
        isOverride = Flag.OVERRIDE in flags
        isOperator = Flag.OPERATOR in flags
        isStatic = Flag.STATIC in flags
        isConst = Flag.CONST in flags
        isMut = Flag.MUT in flags
        isUnsafe = Flag.UNSAFE in flags
        isForeign = Flag.FOREIGN in flags
        isC = Flag.C in flags
        isCommon = Flag.COMMON in flags
        isFromCommonPart = Flag.FROM_COMMON_PART in flags
        isSpecific = Flag.SPECIFIC in flags
        isCommonWithDefault = Flag.COMMON_WITH_DEFAULT in flags
        isRedef = Flag.REDEF in flags
        isDefault = Flag.DEFAULT in flags
        isAbstract = Flag.ABSTRACT in flags
        isOpen = Flag.OPEN in flags
        isSealed = Flag.SEALED in flags
    }

/**
 * 对齐 Kotlin `deserializeClassToSymbol` 的职责边界：
 * 负责把 compiled PSI/stub 中的 class-like 声明装配成已分析依赖阶段的 declaration。
 *
 * 仓颉边界在这里明确收紧：
 * 1. 不承载 local/nested class-like。
 * 2. 不承载 published api、effective visibility、clone/synthetic members；enum entry 与字段按
 *    class body 中的 compiled PSI stub 完整还原。
 * 3. 不额外引入 deserialization extension 或 comparator 子系统。
 */
@Suppress("UNUSED_PARAMETER")
internal fun deserializeClassToSymbol(
    classId: ClassId,
    classOrObject: CjTypeStatement,
    symbol: CfirClassLikeSymbol<*>,
    session: CfirSession,
    moduleData: CfirModuleData,
    defaultAnnotationDeserializer: StubBasedAnnotationDeserializer?,
    scopeProvider: CfirScopeProvider,
    parentContext: StubBasedCfirDeserializationContext? = null,
    containerSource: DeserializedContainerSource? = null,
    initialOrigin: CfirDeclarationOrigin,
) {
    val annotationDeserializer = defaultAnnotationDeserializer ?: StubBasedAnnotationDeserializer(session)
    val context = parentContext?.childContext(
        classOrObject,
        classId.relativeClassName,
        containerSource,
        symbol,
        annotationDeserializer,
        capturesTypeParameters = false,
    ) ?: StubBasedCfirDeserializationContext.createForClass(
        classId,
        classOrObject,
        moduleData,
        annotationDeserializer,
        containerSource,
        symbol,
        initialOrigin,
    )

    val status = classOrObject.buildResolvedStatus()
    val typeParameters = context.typeDeserializer.ownTypeParameters.map { it.cfir }
    val superTypeRefs = mutableListOf<CfirTypeRef>()
    val declarations = mutableListOf<CfirDeclaration>()

    val superTypeList = classOrObject.getSuperTypeList()
    if (superTypeList != null) {
        superTypeRefs += superTypeList.entries.map { superTypeReference ->
            context.typeDeserializer.typeRef(
                superTypeReference.typeReference
                    ?: errorWithAttachment("Super entry doesn't have type reference") {
                        withPsiEntry("superTypeReference", superTypeReference)
                    }
            )
        }
    } else if (classId != StdlibClassIds.Any) {
        superTypeRefs += ConeClassLikeType(StdlibClassIds.Any.toLookupTag(), isInterface = true).toCfirResolvedTypeRef()
    }

    classOrObject.primaryConstructor?.let { constructor ->
        declarations += context.memberDeserializer.loadConstructor(constructor, classOrObject, symbol, typeParameters)
    }

    classOrObject.body?.declarations?.forEach { declaration ->
        when (declaration) {
            is CjEnumConstructor -> {
                val enumSymbol = symbol as? CfirEnumSymbol
                    ?: errorWithAttachment("Enum constructor is nested in a non-enum class-like declaration") {
                        withPsiEntry("classOrObject", classOrObject)
                        withPsiEntry("enumConstructor", declaration)
                    }
                declarations += context.memberDeserializer.loadEnumConstructor(declaration, enumSymbol, typeParameters)
            }
            is CjConstructor<*> -> declarations += context.memberDeserializer.loadConstructor(declaration, classOrObject, symbol, typeParameters)
            is CjFieldVariable -> declarations += context.memberDeserializer.loadFieldVariable(declaration)
            is CjNamedFunction -> declarations += context.memberDeserializer.loadFunction(declaration, symbol, session)
            is CjProperty -> declarations += context.memberDeserializer.loadProperty(declaration, symbol)
        }
    }

    val sortedDeclarations = declarations.sortedWith(
        compareBy<CfirDeclaration> { declarationOrderKey(it) }
    )

    when (symbol) {
        is CfirClassSymbol -> buildClass {
            source = CjRealPsiSourceElement(classOrObject)
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            deprecationsProvider = EmptyDeprecationsProvider
            this.scopeProvider = scopeProvider
            name = classId.shortClassName
            this.status = status
            this.symbol = symbol
            this.typeParameters += typeParameters
            this.superTypeRefs += superTypeRefs
            this.declarations += sortedDeclarations
            annotations += context.annotationDeserializer.loadAnnotations(classOrObject, symbol)
        }

        is CfirInterfaceSymbol -> buildInterface {
            source = CjRealPsiSourceElement(classOrObject)
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            deprecationsProvider = EmptyDeprecationsProvider
            this.scopeProvider = scopeProvider
            name = classId.shortClassName
            this.status = status
            this.symbol = symbol
            this.typeParameters += typeParameters
            this.superTypeRefs += superTypeRefs
            this.declarations += sortedDeclarations
            annotations += context.annotationDeserializer.loadAnnotations(classOrObject, symbol)
        }

        is CfirStructSymbol -> buildStruct {
            source = CjRealPsiSourceElement(classOrObject)
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            deprecationsProvider = EmptyDeprecationsProvider
            this.scopeProvider = scopeProvider
            name = classId.shortClassName
            this.status = status
            this.symbol = symbol
            this.typeParameters += typeParameters
            this.superTypeRefs += superTypeRefs
            this.declarations += sortedDeclarations
            annotations += context.annotationDeserializer.loadAnnotations(classOrObject, symbol)
        }

        is CfirEnumSymbol -> buildEnum {
            source = CjRealPsiSourceElement(classOrObject)
            this.moduleData = moduleData
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            deprecationsProvider = EmptyDeprecationsProvider
            this.scopeProvider = scopeProvider
            name = classId.shortClassName
            this.status = status
            this.symbol = symbol
            this.typeParameters += typeParameters
            this.superTypeRefs += superTypeRefs
            this.declarations += sortedDeclarations
            this.isRefEnum = symbol.isRefEnum
            this.isNonExhaustive = (classOrObject as? CjEnum)?.isNonExhaustive == true
            annotations += context.annotationDeserializer.loadAnnotations(classOrObject, symbol)
        }

        else -> errorWithAttachment("Unexpected class-like symbol: ${symbol::class}") {
            withPsiEntry("classOrObject", classOrObject)
        }
    }
}

/**
 * 返回类成员声明在反序列化结果中的稳定排序 key。
 */
private fun declarationOrderKey(declaration: CfirDeclaration): Int {
    return when (declaration) {
        is CfirEnumConstructor -> 0
        is org.cangnova.cangjie.cfir.declarations.CfirConstructor -> 1
        is org.cangnova.cangjie.cfir.declarations.CfirProperty,
        is CfirFieldVariable -> 2
        is org.cangnova.cangjie.cfir.declarations.CfirNamedFunction -> 3
        else -> 4
    }
}
