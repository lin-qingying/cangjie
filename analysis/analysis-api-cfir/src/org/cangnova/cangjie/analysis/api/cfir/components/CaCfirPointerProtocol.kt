package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaScriptSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjScript

internal sealed interface CaCfirSymbolRestoreKey {
    fun restore(session: CaSession): CaSymbol?
}

internal data class CaCfirFileSymbolRestoreKey(
    val file: CjFile,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? =
        (session as? CaCfirSession)?.createFileSymbol(file)
}

internal data class CaCfirPackageSymbolRestoreKey(
    val fqName: FqName,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? =
        (session as? CaCfirSession)?.getPackagePublicSymbol(fqName)
}

internal data class CaCfirClassLikeSymbolRestoreKey(
    val classId: ClassId,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? =
        (session as? CaCfirSession)?.getClassLikePublicSymbol(classId)
}

internal data class CaCfirExtendSymbolRestoreKey(
    val extendId: String,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? =
        (session as? CaCfirSession)?.restoreExtendPublicSymbol(extendId)
}

internal data class CaCfirCallableSymbolRestoreKey(
    val callableId: CallableId,
    val kind: CaCfirCallableSymbolKind,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? =
        (session as? CaCfirSession)?.restoreCallablePublicSymbol(callableId, kind)
}

internal data class CaCfirExtendMemberCallableSymbolRestoreKey(
    val extendId: String,
    val callableName: Name,
    val kind: CaCfirCallableSymbolKind,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? =
        (session as? CaCfirSession)?.restoreExtendMemberCallablePublicSymbol(extendId, callableName, kind)
}

internal data class CaCfirPropertyAccessorSymbolRestoreKey(
    val ownerRestoreKey: CaCfirSymbolRestoreKey,
    val kind: CaCfirPropertyAccessorKind,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? {
        val owner = ownerRestoreKey.restore(session) as? org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol ?: return null
        return when (kind) {
            CaCfirPropertyAccessorKind.GETTER -> owner.getter
            CaCfirPropertyAccessorKind.SETTER -> owner.setter
        }
    }
}

internal data class CaCfirTypeParameterSymbolRestoreKey(
    val ownerRestoreKey: CaCfirSymbolRestoreKey,
    val parameterName: Name,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? {
        val ownerSymbol = ownerRestoreKey.restore(session) as? CaTypeParameterOwnerSymbol ?: return null
        return ownerSymbol.typeParameters.singleOrNull { typeParameter -> typeParameter.name == parameterName }
    }
}

internal data class CaCfirValueParameterSymbolRestoreKey(
    val ownerRestoreKey: CaCfirSymbolRestoreKey,
    val parameterIndex: Int,
    val parameterName: Name,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? {
        val ownerSymbol = ownerRestoreKey.restore(session) as? CaValueParameterOwnerSymbol ?: return null
        return ownerSymbol.valueParameters.getOrNull(parameterIndex)?.takeIf { parameter ->
            parameter.name == parameterName
        }
    }
}

/**
 * 源码锚点恢复 key。
 *
 * 匿名/局部/source-only 声明不再走“按名字扫描文件”的宽松恢复，
 * 而是直接以真实声明 PSI 的 smart pointer 作为恢复锚点。
 * 这样既能区分同名局部声明，也能保留 `CjVarOrEnumPattern` 这类真实源码形态。
 */
internal data class CaCfirPsiSymbolRestoreKey(
    val pointer: SmartPsiElementPointer<com.intellij.psi.PsiElement>,
    val kind: CaCfirPsiSymbolKind,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? {
        val cfirSession = session as? CaCfirSession ?: return null
        val psi = pointer.element ?: return null
        return when (kind) {
            CaCfirPsiSymbolKind.SCRIPT -> (psi as? CjScript)?.let(cfirSession::createScriptSymbol)
            CaCfirPsiSymbolKind.ANONYMOUS_FUNCTION -> cfirSession.getPublicSymbolByPsi<CaAnonymousFunctionSymbol>(psi)
            CaCfirPsiSymbolKind.LOCAL_VARIABLE -> cfirSession.getPublicSymbolByPsi<CaSymbol>(psi)
            CaCfirPsiSymbolKind.PATTERN_VARIABLE ->
                cfirSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol>(psi)
            CaCfirPsiSymbolKind.PATTERN_BINDING ->
                cfirSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol>(psi)
            CaCfirPsiSymbolKind.TYPE_PARAMETER -> cfirSession.getPublicSymbolByPsi<CaTypeParameterSymbol>(psi)
        }
    }
}

internal fun CaSymbol.createRestoreKey(): CaCfirSymbolRestoreKey = when (this) {
    is CaCfirFileSymbolImpl -> CaCfirFileSymbolRestoreKey(file)
    is CaCfirPackageSymbolImpl -> CaCfirPackageSymbolRestoreKey(fqName)
    is CaCfirClassLikeSymbolBase<*> -> classId?.let(::CaCfirClassLikeSymbolRestoreKey)
        ?: error("Class-like symbol `${this::class.simpleName}` is missing ClassId")
    is CaCfirExtendSymbolImpl -> CaCfirExtendSymbolRestoreKey(extendId)
    is CaCfirPropertyAccessorSymbolBase -> {
        val ownerRestoreKey = owningProperty.createRestoreKey()
        CaCfirPropertyAccessorSymbolRestoreKey(
            ownerRestoreKey = ownerRestoreKey,
            kind = if (isGetter) CaCfirPropertyAccessorKind.GETTER else CaCfirPropertyAccessorKind.SETTER,
        )
    }
    is CaCfirValueParameterSymbolImpl -> {
        val ownerRestoreKey = (containingDeclaration ?: error("Value parameter is missing owner")).createRestoreKey()
        val parameterIndex = stableParameterIndex ?: error("Value parameter `${name}` is missing stable index")
        CaCfirValueParameterSymbolRestoreKey(ownerRestoreKey, parameterIndex, name)
    }
    is CaCfirTypeParameterSymbolImpl -> {
        val ownerRestoreKey = (containingDeclaration ?: error("Type parameter is missing owner")).createRestoreKey()
        CaCfirTypeParameterSymbolRestoreKey(ownerRestoreKey, name)
    }
    is org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol ->
        CaCfirPsiSymbolRestoreKey(
            pointer = (psi ?: error("Pattern binding symbol is missing declaration PSI")).createPointer(),
            kind = CaCfirPsiSymbolKind.PATTERN_BINDING,
        )
    is org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol ->
        CaCfirPsiSymbolRestoreKey(
            pointer = (psi ?: error("Pattern variable symbol is missing declaration PSI")).createPointer(),
            kind = CaCfirPsiSymbolKind.PATTERN_VARIABLE,
        )
    is CaCfirCallableSymbolBase<*> -> {
        val cacheKey = publicSymbolCacheKeyOrNull()
        when (cacheKey) {
            is CaCfirCallableSymbolCacheKey -> CaCfirCallableSymbolRestoreKey(cacheKey.callableId, cacheKey.kind)
            is CaCfirExtendMemberCallableSymbolCacheKey ->
                CaCfirExtendMemberCallableSymbolRestoreKey(cacheKey.extendId, cacheKey.callableName, cacheKey.kind)
            is CaCfirPsiSymbolCacheKey -> CaCfirPsiSymbolRestoreKey(cacheKey.psi.createPointer(), cacheKey.kind)
            else -> error("Unsupported callable restore protocol for `${this::class.simpleName}`")
        }
    }
    is CaCfirScriptSymbolImpl -> CaCfirPsiSymbolRestoreKey(scriptPsi.createPointer(), CaCfirPsiSymbolKind.SCRIPT)
    else -> error("Unsupported restore protocol for `${this::class.simpleName}`")
}

internal class CaCfirSymbolPointerDelegate<out S : CaSymbol>(
    private val restoreKey: CaCfirSymbolRestoreKey,
) : CaSymbolPointer<S> {
    @Suppress("UNCHECKED_CAST")
    override fun restoreSymbol(session: CaSession): S? =
        restoreKey.restore(session) as? S
}

private fun com.intellij.psi.PsiElement.createPointer(): SmartPsiElementPointer<com.intellij.psi.PsiElement> =
    SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this)
