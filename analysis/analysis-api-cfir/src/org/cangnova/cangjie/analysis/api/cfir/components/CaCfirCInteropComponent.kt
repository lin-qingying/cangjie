package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.annotations.*
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.components.CaCInteropComponent
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseAnnotationImpl
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseAnnotationValues
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.interop.*
import org.cangnova.cangjie.analysis.api.lifetime.*
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.annotations.AnnotationSemanticHandler
import org.cangnova.cangjie.annotations.BuiltInAnnotationCategory
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.publishInteropInfo
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builtInDescriptor
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.name.Name

/** PSI 查询只用于定位声明，所有互操作语义均来自同一个 CFIR 快照。 */
internal class CaCfirCInteropComponent(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaCInteropComponent {
    override fun CjElement.getInteropInfo(): CaInteropInfo? = withValidityAssertion {
        analysisSession.getInteropInfo(this@getInteropInfo)
    }
    override fun CaSymbol.getInteropInfo(): CaInteropInfo? = withValidityAssertion {
        analysisSession.getInteropInfo(this@getInteropInfo)
    }
}

internal class CaCfirInteropInfoImpl(
    override val abi: CaInteropAbi,
    override val isCFunction: Boolean,
    override val backends: List<CaInteropBackend>,
    override val isForeignDeclaration: Boolean,
    override val isFastNative: Boolean,
    override val isFrozen: Boolean,
    override val externalName: String?,
    override val externalGetterName: String?,
    override val externalSetterName: String?,
    override val callingConvention: CaInteropCallingConvention?,
    override val hasJavaDefault: Boolean,
    override val isObjCInit: Boolean,
    override val isObjCOptional: Boolean,
    override val ffiAnnotationNames: List<String>,
    override val ffiAnnotations: List<CaAnnotation>,
    override val token: CaLifetimeToken,
) : CaInteropInfo

internal fun CaCfirSession.getInteropInfo(element: CjElement): CaInteropInfo? {
    val owner = element as? CjDeclaration ?: element.getStrictParentOfType<CjDeclaration>() ?: return null
    return buildInteropInfo(owner.resolveToCfirSymbol(resolutionFacade, CfirResolvePhase.BODY_RESOLVE))
}

internal fun CaCfirSession.getInteropInfo(symbol: CaSymbol): CaInteropInfo? {
    val cfirSymbol = (symbol as? CaCfirSymbol<*>)?.cfirSymbol ?: return null
    cfirSymbol.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
    return buildInteropInfo(cfirSymbol)
}

private fun CaCfirSession.buildInteropInfo(symbol: CfirBasedSymbol<*>): CaInteropInfo? {
    val declaration = symbol.cfir
    // Source declarations publish the snapshot during STATUS.  A binary
    // declaration is already at BODY_RESOLVE and therefore does not pass that
    // source phase; its deserializer attaches serialized facts, which this
    // same canonical producer materializes on demand.
    if (declaration.interopInfo == null) declaration.publishInteropInfo(cfirSession)
    val info = declaration.interopInfo ?: return null
    if (info.resolvedAbi.kind == CfirAbiKind.CANGJIE && !info.isFastNative && !info.isFrozen &&
        info.foreignName == null && info.foreignGetterName == null && info.foreignSetterName == null &&
        info.java == null && info.objc == null
    ) return null
    val backends = buildList {
        if (info.resolvedAbi.kind == CfirAbiKind.C) add(CaInteropBackend.C)
        if (info.resolvedAbi.kind == CfirAbiKind.JAVA) add(CaInteropBackend.JAVA)
        if (info.java?.isMirror == true) add(CaInteropBackend.JAVA_MIRROR)
        if (info.java?.isImpl == true) add(CaInteropBackend.JAVA_IMPL)
        if (info.objc?.isMirror == true) add(CaInteropBackend.OBJC_MIRROR)
        if (info.objc?.isImpl == true) add(CaInteropBackend.OBJC_IMPL)
    }
    val annotationCalls = declaration.annotations.filterIsInstance<CfirAnnotationCall>().filter {
        val descriptor = it.builtInDescriptor
        descriptor?.category == BuiltInAnnotationCategory.FFI || descriptor?.semanticHandler == AnnotationSemanticHandler.C_FFI
    }
    val annotations = buildList {
        addAll(annotationCalls.map { it.asPublicAnnotation(cfirSymbolBuilder, token) })
        val serializedFacts = declaration.serializedInteropFacts
        if (info.abiRequest.hasExplicitC && annotationCalls.none { it.annotationKind == org.cangnova.cangjie.annotations.BuiltInAnnotationKind.C }) {
            addSyntheticFfiAnnotation(
                kind = org.cangnova.cangjie.annotations.BuiltInAnnotationKind.C,
                name = "C",
                token = token,
            )
        }
        if (serializedFacts?.callingConvention != null &&
            annotationCalls.none { it.annotationKind == org.cangnova.cangjie.annotations.BuiltInAnnotationKind.CALLING_CONV }
        ) {
            val convention = checkNotNull(serializedFacts.callingConvention)
            addSyntheticFfiAnnotation(
                kind = org.cangnova.cangjie.annotations.BuiltInAnnotationKind.CALLING_CONV,
                name = "CallingConv",
                token = token,
                arguments = listOf(
                    CaBaseNamedAnnotationValue(
                        Name.identifier("convention"),
                        CaBaseAnnotationValues.constant(
                            CaBaseAnnotationValues.stringValue(convention.name, null),
                            null,
                            token,
                        ),
                    ),
                ),
            )
        }
        if (serializedFacts?.isFastNative == true &&
            annotationCalls.none { it.annotationKind == org.cangnova.cangjie.annotations.BuiltInAnnotationKind.FASTNATIVE }
        ) {
            addSyntheticFfiAnnotation(
                kind = org.cangnova.cangjie.annotations.BuiltInAnnotationKind.FASTNATIVE,
                name = "FastNative",
                token = token,
            )
        }
    }
    return CaCfirInteropInfoImpl(
        abi = CaInteropAbi.valueOf(info.resolvedAbi.kind.name),
        isCFunction = info.resolvedAbi.isCFunction,
        backends = backends,
        isForeignDeclaration = info.abiRequest.isForeign,
        isFastNative = info.isFastNative,
        isFrozen = info.isFrozen,
        externalName = info.externalSymbolName,
        externalGetterName = info.foreignGetterName,
        externalSetterName = info.foreignSetterName,
        callingConvention = info.abiRequest.callingConvention?.let { CaInteropCallingConvention.valueOf(it.name) },
        hasJavaDefault = info.java?.hasDefault == true,
        isObjCInit = info.objc?.isInit == true,
        isObjCOptional = info.objc?.isOptional == true,
        ffiAnnotationNames = info.ffiAnnotationNames,
        ffiAnnotations = annotations,
        token = token,
    )
}

/**
 * CJO 按官方格式把 C/STD_CALL/FastNative 放在 declaration attributes/FuncInfo，
 * 不会生成 `Anno`。这里建立同一 Analysis API 视图所需的无源码 synthetic annotation，
 * 只接受已由 serialized interop facts 发布的事实。
 */
private fun MutableList<CaAnnotation>.addSyntheticFfiAnnotation(
    kind: org.cangnova.cangjie.annotations.BuiltInAnnotationKind,
    name: String,
    token: CaLifetimeToken,
    arguments: List<CaNamedAnnotationValue> = emptyList(),
) {
    add(
        CaBaseAnnotationImpl(
            classId = null,
            shortName = Name.identifier(name),
            psi = null,
            lazyArguments = lazyOf(arguments),
            constructorSymbol = null,
            token = token,
            builtInKind = kind,
            isCompileTimeVisible = false,
            isForcedCustom = false,
            resolutionStatus = CaAnnotationResolutionStatus.RESOLVED,
        ),
    )
}
