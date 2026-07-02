

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.stubs.Stub
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildEnum
import org.cangnova.cangjie.cfir.declarations.builder.buildInterface
import org.cangnova.cangjie.cfir.declarations.builder.buildStruct
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
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
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import org.cangnova.cangjie.source.CjRealPsiSourceElement
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * 从 PSI 修饰符列表恢复 CFIR visibility。
 */
internal val CjModifierListOwner.visibility: Visibility
    get() = with(modifierList) {
        when {
            this == null -> Visibilities.Public
            hasModifier(CjTokens.PRIVATE_KEYWORD) -> Visibilities.Private
            hasModifier(CjTokens.PUBLIC_KEYWORD) -> Visibilities.Public
            hasModifier(CjTokens.PROTECTED_KEYWORD) -> Visibilities.Protected
            else -> if (hasModifier(CjTokens.INTERNAL_KEYWORD)) Visibilities.Internal else Visibilities.Public
        }
    }

/**
 * 从 PSI 修饰符恢复 CFIR modality。
 */
internal val CjDeclaration.modality: Modality
    get() = when {
        hasModifier(CjTokens.SEALED_KEYWORD) -> Modality.SEALED
        hasModifier(CjTokens.ABSTRACT_KEYWORD) || this is CjClass && isInterface() -> Modality.ABSTRACT
        hasModifier(CjTokens.OPEN_KEYWORD) -> Modality.OPEN
        else -> Modality.FINAL
    }

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

/**
 * 对齐 Kotlin `deserializeClassToSymbol` 的职责边界：
 * 负责把 compiled PSI/stub 中的 class-like 声明装配成已分析依赖阶段的 declaration。
 *
 * 仓颉边界在这里明确收紧：
 * 1. 不承载 local/nested class-like。
 * 2. 不承载 enum entry、published api、effective visibility、clone/synthetic members。
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

    val status = buildResolvedStatus(classOrObject.visibility, classOrObject.modality)
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
            is CjConstructor<*> -> declarations += context.memberDeserializer.loadConstructor(declaration, classOrObject, symbol, typeParameters)
            is CjNamedFunction -> declarations += context.memberDeserializer.loadFunction(declaration, symbol, session)
            is CjProperty -> declarations += context.memberDeserializer.loadProperty(declaration, symbol)
        }
    }

    val sortedDeclarations = declarations.sortedWith(
        compareBy<CfirDeclaration>(
            { declarationOrderKey(it) },
            { (it as? CfirMemberDeclaration)?.symbol?.debugName ?: "" },
        )
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
 * 根据 visibility 和 modality 构建已解析声明状态。
 */
private fun buildResolvedStatus(visibility: Visibility, modality: Modality): CfirDeclarationStatusImpl {
    return CfirDeclarationStatusImpl(visibility, modality).apply {
        isVisibilityExplicit = visibility != Visibilities.Public
        isModalityExplicit = modality != Modality.FINAL
        isAbstract = modality == Modality.ABSTRACT
        isOpen = modality == Modality.OPEN
        isSealed = modality == Modality.SEALED
    }
}

/**
 * 返回类成员声明在反序列化结果中的稳定排序 key。
 */
private fun declarationOrderKey(declaration: CfirDeclaration): Int {
    return when (declaration) {
        is org.cangnova.cangjie.cfir.declarations.CfirConstructor -> 0
        is org.cangnova.cangjie.cfir.declarations.CfirProperty -> 1
        is org.cangnova.cangjie.cfir.declarations.CfirNamedFunction -> 2
        else -> 3
    }
}
