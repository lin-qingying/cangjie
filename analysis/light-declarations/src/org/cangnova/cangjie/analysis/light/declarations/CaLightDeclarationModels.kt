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
    /**
     * 按需恢复注解列表的工厂。
     */
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

/**
 * Class-like 声明视图实现，覆盖 class、interface、struct、enum 与 typealias 的公共只读投影。
 */
class CaLightClassLikeDeclarationImpl(
    name: String?,
    module: CaModule?,
    annotationsFactory: () -> List<CaAnnotation>,
    origin: CaLightDeclarationOrigin,
    token: CaLifetimeToken,
    /**
     * 按需恢复 classId 的工厂。
     */
    private val classIdFactory: () -> ClassId?,
    /**
     * 按需恢复类型参数名称列表的工厂。
     */
    private val typeParametersFactory: () -> List<Name>,
    /**
     * 按需恢复直接父类型列表的工厂。
     */
    private val superTypesFactory: () -> List<CaType>,
    /**
     * 按需恢复成员 light declaration 列表的工厂。
     */
    private val membersFactory: () -> List<CaLightDeclaration>,
) : CaLightDeclarationBase(
    kind = CaLightDeclarationKind.CLASS_LIKE,
    name = name,
    module = module,
    annotationsFactory = annotationsFactory,
    origin = origin,
    token = token,
), CaLightClassLikeDeclaration {
    /**
     * 声明的 classId；source、decompiled 和 synthetic 来源均允许为空。
     */
    override val classId: ClassId? by lazy(LazyThreadSafetyMode.NONE) {
        classIdFactory()
    }

    /**
     * 声明的类型参数名称列表。
     */
    override val typeParameters: List<Name> by lazy(LazyThreadSafetyMode.NONE) {
        typeParametersFactory()
    }

    /**
     * 声明的直接父类型列表。
     */
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

/**
 * extend 声明视图实现。
 */
class CaLightExtendDeclarationImpl(
    name: String?,
    module: CaModule?,
    annotationsFactory: () -> List<CaAnnotation>,
    origin: CaLightDeclarationOrigin,
    token: CaLifetimeToken,
    /**
     * extend 声明的稳定标识。
     */
    override val extendId: String,
    /**
     * 按需恢复目标 classId 的工厂。
     */
    private val targetClassIdFactory: () -> ClassId?,
    /**
     * 按需恢复被扩展类型的工厂。
     */
    private val extendedTypeFactory: () -> CaType,
    /**
     * 按需恢复类型参数名称列表的工厂。
     */
    private val typeParametersFactory: () -> List<Name>,
    /**
     * 按需恢复父类型列表的工厂。
     */
    private val superTypesFactory: () -> List<CaType>,
    /**
     * 按需恢复 extend 成员列表的工厂。
     */
    private val membersFactory: () -> List<CaLightDeclaration>,
) : CaLightDeclarationBase(
    kind = CaLightDeclarationKind.EXTEND,
    name = name,
    module = module,
    annotationsFactory = annotationsFactory,
    origin = origin,
    token = token,
), CaLightExtendDeclaration {
    /**
     * extend 目标类型对应的 classId。
     */
    override val targetClassId: ClassId? by lazy(LazyThreadSafetyMode.NONE) {
        targetClassIdFactory()
    }

    /**
     * extend 实际扩展的类型。
     */
    override val extendedType: CaType by lazy(LazyThreadSafetyMode.NONE) {
        extendedTypeFactory()
    }

    /**
     * extend 声明的类型参数名称列表。
     */
    override val typeParameters: List<Name> by lazy(LazyThreadSafetyMode.NONE) {
        typeParametersFactory()
    }

    /**
     * extend 声明的父类型列表。
     */
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

/**
 * 可调用声明视图实现，覆盖函数、属性、构造器等 callable 投影。
 */
class CaLightCallableDeclarationImpl(
    name: String?,
    module: CaModule?,
    annotationsFactory: () -> List<CaAnnotation>,
    origin: CaLightDeclarationOrigin,
    token: CaLifetimeToken,
    /**
     * 按需恢复 callableId 的工厂。
     */
    private val callableIdFactory: () -> CallableId?,
    /**
     * 按需恢复 callable 签名的工厂。
     */
    private val signatureFactory: () -> CaCallableSignature<*>?,
) : CaLightDeclarationBase(
    kind = CaLightDeclarationKind.CALLABLE,
    name = name,
    module = module,
    annotationsFactory = annotationsFactory,
    origin = origin,
    token = token,
), CaLightCallableDeclaration {
    /**
     * 可调用声明 ID；decompiled 或 synthetic 来源可为空。
     */
    override val callableId: CallableId? by lazy(LazyThreadSafetyMode.NONE) {
        callableIdFactory()
    }

    /**
     * 可调用签名；当来源无法恢复完整语义时可为空。
     */
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

/**
 * Light declaration 缓存键。
 *
 * @property stableKey provider 层生成的稳定字符串键。
 */
data class CaLightDeclarationCacheKey(
    /**
     * provider 层生成的稳定字符串键。
     */
    val stableKey: String,
)

/**
 * 单次 provider 构建过程中复用 light declaration 实例的缓存。
 */
class CaLightDeclarationCache {
    /**
     * 按稳定 key 记录的声明实例。
     */
    private val declarations = linkedMapOf<CaLightDeclarationCacheKey, CaLightDeclaration>()

    /**
     * 获取已有声明实例，或创建并缓存一个新实例。
     */
    fun <T : CaLightDeclaration> getOrPut(
        key: CaLightDeclarationCacheKey,
        create: () -> T,
    ): T {
        @Suppress("UNCHECKED_CAST")
        return declarations.getOrPut(key, create) as T
    }
}

/**
 * 根据 source element 与 containing file 构造 light declaration 来源信息。
 */
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
