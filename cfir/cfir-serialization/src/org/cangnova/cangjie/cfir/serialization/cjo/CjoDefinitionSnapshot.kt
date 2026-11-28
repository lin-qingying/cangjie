package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoSourceBindingSite
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoSourceContentRoot
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.source.CfirSourcePatternIdentity
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import java.util.Collections
import java.util.IdentityHashMap

/** 定义 header、注解、参数 default 与函数/变量内容分槽请求，不授予整包 SOURCE。 */
enum class CjoDefinitionContentSlot { DECLARATION_ANNOTATIONS, PARAMETER_DEFAULT, FUNCTION_BODY, VARIABLE_INITIALIZER }

data class CjoDefinitionContentRequirement(val declaration: CfirDeclaration, val slot: CjoDefinitionContentSlot)

/** 原来源和 binder 在正式准备中选定；外部不能以包名或裸 d/e 下标自行构造授权。 */
class CjoDefinitionSnapshotRequest internal constructor(
    val source: CjoLoadedPackage,
    internal val binderIdentity: Any,
    internal val domain: CfirSession,
    seeds: Collection<CfirDeclaration>,
    contents: Collection<CjoDefinitionContentRequirement>,
) {
    internal val identity: Any = Any()
    val seeds: List<CfirDeclaration> = Collections.unmodifiableList(seeds.toList())
    val contents: List<CjoDefinitionContentRequirement> = Collections.unmodifiableList(contents.toList())
    init { require(this.seeds.isNotEmpty()) { "A scoped definition request requires actual canonical seeds" } }
}

internal data class CjoDefinitionDeclarationBinding(val site: CjoSourceBindingSite, val target: CjoExpressionTargetMetadata)
internal data class CjoDefinitionTypeBinding(val site: CjoSourceBindingSite, val originalReference: UInt, val type: ConeCangJieType?)

/**
 * 同一次完成代中的限定选择。definitions 与 borrowedHeaders 分离，F/P 保持自己的原表身份；
 * 保存绑定仅为原来源事实，尚不代表当前输出的 CURRENT 资格。
 */
class CjoDefinitionSnapshotSelection internal constructor(
    val request: CjoDefinitionSnapshotRequest,
    internal val identity: Any,
    definitions: Map<Int, CfirDeclaration>,
    borrowedHeaders: Map<Int, CfirDeclaration>,
    owners: Map<CfirDeclaration, CfirDeclaration?>,
    functions: Map<Int, CfirCallableDeclaration>,
    patterns: Map<Int, CfirSourcePatternIdentity>,
    borrowedPatterns: Set<Int>,
    roots: List<CjoSourceContentRoot>,
    declarationBindings: List<CjoDefinitionDeclarationBinding>,
    typeBindings: List<CjoDefinitionTypeBinding>,
    positionFiles: Map<CjoSavedFileReference, CjoPositionFileBinding>,
) {
    internal val roots: List<CjoSourceContentRoot> = Collections.unmodifiableList(roots.toList())
    internal val declarationBindings: List<CjoDefinitionDeclarationBinding> = Collections.unmodifiableList(declarationBindings.toList())
    internal val typeBindings: List<CjoDefinitionTypeBinding> = Collections.unmodifiableList(typeBindings.toList())
    internal val positionFiles: Map<CjoSavedFileReference, CjoPositionFileBinding> = Collections.unmodifiableMap(LinkedHashMap(positionFiles))
    val definitions: Map<Int, CfirDeclaration> = Collections.unmodifiableMap(LinkedHashMap(definitions))
    val borrowedHeaders: Map<Int, CfirDeclaration> = Collections.unmodifiableMap(LinkedHashMap(borrowedHeaders))
    val owners: Map<CfirDeclaration, CfirDeclaration?> = Collections.unmodifiableMap(IdentityHashMap(owners))
    /** key 是原 F tablePosition，不是 d/e index。 */
    val functions: Map<Int, CfirCallableDeclaration> = Collections.unmodifiableMap(LinkedHashMap(functions))
    /** key 是原 P tablePosition，不是 d/e index。 */
    val patterns: Map<Int, CfirSourcePatternIdentity> = Collections.unmodifiableMap(LinkedHashMap(patterns))
    /** 原 P 完成产品被本请求显式借用，不等同于本请求选择其 e 或 initializer。 */
    val borrowedPatterns: Set<Int> = Collections.unmodifiableSet(LinkedHashSet(borrowedPatterns))
}

/** 未归属可以交给其它 provider；已匹配请求的失败必须由调用方传播，不可继续择其它来源。 */
sealed interface CjoDefinitionSnapshotResult {
    data object NotOwner : CjoDefinitionSnapshotResult
    class Failure internal constructor(val reason: String) : CjoDefinitionSnapshotResult
    class Completed internal constructor(val snapshot: CjoSourceExportSnapshot, val selection: CjoDefinitionSnapshotSelection) : CjoDefinitionSnapshotResult
}
