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
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature

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
    private val annotationsFactory: () -> List<CaAnnotation>,
    final override val origin: CaLightDeclarationOrigin,
    final override val token: CaLifetimeToken,
) : CaLightDeclaration {
    /**
     * 注解列表按需恢复，避免在 light declaration 建立阶段触发完整符号解析。
     */
    final override val annotations: List<CaAnnotation> by lazy(LazyThreadSafetyMode.NONE) {
        annotationsFactory()
    }
}

class CaLightClassLikeDeclarationImpl(
    name: String?,
    module: CaModule?,
    annotationsFactory: () -> List<CaAnnotation>,
    origin: CaLightDeclarationOrigin,
    token: CaLifetimeToken,
    private val classIdFactory: () -> ClassId?,
    private val typeParametersFactory: () -> List<Name>,
    private val superTypesFactory: () -> List<CaType>,
    private val membersFactory: () -> List<CaLightDeclaration>,
) : CaLightDeclarationBase(
    kind = CaLightDeclarationKind.CLASS_LIKE,
    name = name,
    module = module,
    annotationsFactory = annotationsFactory,
    origin = origin,
    token = token,
), CaLightClassLikeDeclaration {
    override val classId: ClassId? by lazy(LazyThreadSafetyMode.NONE) {
        classIdFactory()
    }

    override val typeParameters: List<Name> by lazy(LazyThreadSafetyMode.NONE) {
        typeParametersFactory()
    }

    override val superTypes: List<CaType> by lazy(LazyThreadSafetyMode.NONE) {
        superTypesFactory()
    }

    /**
     * 对齐 Kotlin decompiled light class 的惰性成员展开时机。
     *
     * 顶层 light declaration 先稳定建立，成员树仅在真正访问时才展开，
     * 避免在 provider 构造阶段一次性递归物化整棵声明树。
     */
    override val members: List<CaLightDeclaration> by lazy(LazyThreadSafetyMode.NONE) {
        membersFactory()
    }

    constructor(
        name: String?,
        module: CaModule?,
        annotations: List<CaAnnotation>,
        origin: CaLightDeclarationOrigin,
        token: CaLifetimeToken,
        classId: ClassId?,
        typeParameters: List<Name>,
        superTypes: List<CaType>,
        members: List<CaLightDeclaration>,
    ) : this(
        name = name,
        module = module,
        annotationsFactory = { annotations },
        origin = origin,
        token = token,
        classIdFactory = { classId },
        typeParametersFactory = { typeParameters },
        superTypesFactory = { superTypes },
        membersFactory = { members },
    )

}

class CaLightExtendDeclarationImpl(
    name: String?,
    module: CaModule?,
    annotationsFactory: () -> List<CaAnnotation>,
    origin: CaLightDeclarationOrigin,
    token: CaLifetimeToken,
    override val extendId: String,
    private val targetClassIdFactory: () -> ClassId?,
    private val extendedTypeFactory: () -> CaType,
    private val typeParametersFactory: () -> List<Name>,
    private val superTypesFactory: () -> List<CaType>,
    private val membersFactory: () -> List<CaLightDeclaration>,
) : CaLightDeclarationBase(
    kind = CaLightDeclarationKind.EXTEND,
    name = name,
    module = module,
    annotationsFactory = annotationsFactory,
    origin = origin,
    token = token,
), CaLightExtendDeclaration {
    override val targetClassId: ClassId? by lazy(LazyThreadSafetyMode.NONE) {
        targetClassIdFactory()
    }

    override val extendedType: CaType by lazy(LazyThreadSafetyMode.NONE) {
        extendedTypeFactory()
    }

    override val typeParameters: List<Name> by lazy(LazyThreadSafetyMode.NONE) {
        typeParametersFactory()
    }

    override val superTypes: List<CaType> by lazy(LazyThreadSafetyMode.NONE) {
        superTypesFactory()
    }

    /**
     * extend 成员与 class-like 一样按需展开，避免 decompiled 路径预先递归构造。
     */
    override val members: List<CaLightDeclaration> by lazy(LazyThreadSafetyMode.NONE) {
        membersFactory()
    }

    constructor(
        name: String?,
        module: CaModule?,
        annotations: List<CaAnnotation>,
        origin: CaLightDeclarationOrigin,
        token: CaLifetimeToken,
        extendId: String,
        targetClassId: ClassId?,
        extendedType: CaType,
        typeParameters: List<Name>,
        superTypes: List<CaType>,
        members: List<CaLightDeclaration>,
    ) : this(
        name = name,
        module = module,
        annotationsFactory = { annotations },
        origin = origin,
        token = token,
        extendId = extendId,
        targetClassIdFactory = { targetClassId },
        extendedTypeFactory = { extendedType },
        typeParametersFactory = { typeParameters },
        superTypesFactory = { superTypes },
        membersFactory = { members },
    )

}

class CaLightCallableDeclarationImpl(
    name: String?,
    module: CaModule?,
    annotationsFactory: () -> List<CaAnnotation>,
    origin: CaLightDeclarationOrigin,
    token: CaLifetimeToken,
    private val callableIdFactory: () -> CallableId?,
    private val signatureFactory: () -> CaCallableSignature<*>?,
) : CaLightDeclarationBase(
    kind = CaLightDeclarationKind.CALLABLE,
    name = name,
    module = module,
    annotationsFactory = annotationsFactory,
    origin = origin,
    token = token,
), CaLightCallableDeclaration {
    override val callableId: CallableId? by lazy(LazyThreadSafetyMode.NONE) {
        callableIdFactory()
    }

    override val signature: CaCallableSignature<*>? by lazy(LazyThreadSafetyMode.NONE) {
        signatureFactory()
    }

    constructor(
        name: String?,
        module: CaModule?,
        annotations: List<CaAnnotation>,
        origin: CaLightDeclarationOrigin,
        token: CaLifetimeToken,
        callableId: CallableId?,
        signature: CaCallableSignature<*>?,
    ) : this(
        name = name,
        module = module,
        annotationsFactory = { annotations },
        origin = origin,
        token = token,
        callableIdFactory = { callableId },
        signatureFactory = { signature },
    )

}

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
