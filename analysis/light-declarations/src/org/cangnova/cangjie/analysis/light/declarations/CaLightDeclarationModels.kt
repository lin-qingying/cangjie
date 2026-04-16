package org.cangnova.cangjie.analysis.light.declarations

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightCallableDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightClassLikeDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationKind
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationOrigin
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationOriginKind
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightExtendDeclaration
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * 仓颉声明视图的内部基座。
 *
 * 公共接口定义位于 `analysis-api`，这里仅承载实现与缓存基座，
 * 避免在具体语义后端模块中重复拼装同一批只读声明对象。
 */
sealed class CaLightDeclarationBase(
    final override val kind: CaLightDeclarationKind,
    final override val name: String?,
    final override val module: CaModule?,
    final override val annotations: List<CaAnnotation>,
    final override val origin: CaLightDeclarationOrigin,
    final override val token: CaLifetimeToken,
) : CaLightDeclaration

class CaLightPackageDeclarationImpl(
    packageName: String,
    module: CaModule?,
    origin: CaLightDeclarationOrigin,
    token: CaLifetimeToken,
) : CaLightDeclarationBase(
    kind = CaLightDeclarationKind.PACKAGE,
    name = packageName,
    module = module,
    annotations = emptyList(),
    origin = origin,
    token = token,
)

class CaLightClassLikeDeclarationImpl(
    name: String?,
    module: CaModule?,
    annotations: List<CaAnnotation>,
    origin: CaLightDeclarationOrigin,
    token: CaLifetimeToken,
    override val classId: ClassId?,
    override val typeParameters: List<Name>,
    override val superTypes: List<CaType>,
    override val members: List<CaLightDeclaration>,
) : CaLightDeclarationBase(
    kind = CaLightDeclarationKind.CLASS_LIKE,
    name = name,
    module = module,
    annotations = annotations,
    origin = origin,
    token = token,
), CaLightClassLikeDeclaration

class CaLightExtendDeclarationImpl(
    name: String?,
    module: CaModule?,
    annotations: List<CaAnnotation>,
    origin: CaLightDeclarationOrigin,
    token: CaLifetimeToken,
    override val extendId: String,
    override val targetClassId: ClassId?,
    override val extendedType: CaType,
    override val typeParameters: List<Name>,
    override val superTypes: List<CaType>,
    override val members: List<CaLightDeclaration>,
) : CaLightDeclarationBase(
    kind = CaLightDeclarationKind.EXTEND,
    name = name,
    module = module,
    annotations = annotations,
    origin = origin,
    token = token,
), CaLightExtendDeclaration

class CaLightCallableDeclarationImpl(
    name: String?,
    module: CaModule?,
    annotations: List<CaAnnotation>,
    origin: CaLightDeclarationOrigin,
    token: CaLifetimeToken,
    override val callableId: CallableId?,
    override val signature: CaSignature?,
) : CaLightDeclarationBase(
    kind = CaLightDeclarationKind.CALLABLE,
    name = name,
    module = module,
    annotations = annotations,
    origin = origin,
    token = token,
), CaLightCallableDeclaration

data class CaLightDeclarationCacheKey(
    val stableKey: String,
)

class CaLightDeclarationCache {
    private val declarations = linkedMapOf<CaLightDeclarationCacheKey, CaLightDeclaration>()

    fun <T : CaLightDeclaration> getOrPut(
        key: CaLightDeclarationCacheKey,
        create: () -> T,
    ): T {
        @Suppress("UNCHECKED_CAST")
        return declarations.getOrPut(key, create) as T
    }
}

fun sourceOrigin(
    description: String,
    containingFile: CjFile?,
    sourceElement: PsiElement?,
): CaLightDeclarationOrigin = CaLightDeclarationOrigin(
    kind = when {
        sourceElement == null && containingFile == null -> CaLightDeclarationOriginKind.SYNTHETIC
        containingFile?.isCompiled == true -> CaLightDeclarationOriginKind.DECOMPILED_PSI
        else -> CaLightDeclarationOriginKind.SOURCE_PSI
    },
    description = description,
    containingFile = containingFile,
    sourceElement = sourceElement,
)
