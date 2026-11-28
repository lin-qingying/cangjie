package org.cangnova.cangjie.cfir.source

import org.cangnova.cangjie.annotations.CangjieOverflowStrategy
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirContentSource
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportPlan
import org.cangnova.cangjie.cfir.declarations.CfirAnnotationExportKind
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.exportFacts
import org.cangnova.cangjie.cfir.declarations.requireOwnedGraphAnalysisReady
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.references.CfirReferenceParentType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceElementKind
import org.cangnova.cangjie.source.CjSourceLocation
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 完成语义规范化的 SOURCE 图身份，不是 CFIR 分析节点，也不是 CJO 表下标。
 *
 * 仓颉保存类型检查后的 AST 正文；Kotlin 的对应边界是完成 lowering 后的序列化输入。
 * 两者格式不同，图中的声明、表达式和类型仍各有独立身份，不能用文本或结构相等合并。
 */
class CfirSourceExpressionIdentity private constructor() {
    companion object {
        /** SOURCE 生产者或正式包 context 分配身份；不从文本、位置或结构相等派生。 */
        fun create(): CfirSourceExpressionIdentity = CfirSourceExpressionIdentity()
    }
}

/** 内联 P 的语义身份；嵌套模式、生成模式和相同位置的模式仍各自独立。 */
class CfirSourcePatternIdentity private constructor() {
    companion object {
        fun create(): CfirSourcePatternIdentity = CfirSourcePatternIdentity()
    }
}

/** Block 与 Pattern 的槽明确区分定义和表达式；声明引用不因此取得正文的所有权。 */
sealed interface CfirSourceSlot {
    data class Expression(val identity: CfirSourceExpressionIdentity) : CfirSourceSlot
    data class Declaration(val declaration: CfirDeclaration) : CfirSourceSlot
}

/** 源位置只保存本代范围；新的规范节点可有真实缺失位置，编码器不得制造锚点。 */
data class CfirSourceSpan(val beginOffset: Int, val endOffset: Int, val kind: CjSourceElementKind) {
    init {
        require(beginOffset >= 0 && endOffset >= beginOffset)
    }

    companion object {
        fun capture(source: CjSourceElement?): CfirSourceSpan? = source?.let {
            CfirSourceSpan(it.startOffset, it.endOffset, it.kind)
        }
    }
}

/** 保存槽的不透明身份；有效诊断锚点与原槽是否存在是两个独立事实。 */
class CfirSourcePositionIdentity

/** loaded SOURCE 的诊断位置与原保存槽分别发布，不从显示路径反推 file/pkg/ignore。 */
class CfirSourceImportedPosition(
    val begin: CjSourceLocation?,
    val end: CjSourceLocation?,
    val beginIdentity: CfirSourcePositionIdentity? = null,
    val endIdentity: CfirSourcePositionIdentity? = null,
) {
    /** 诊断坐标按原值语义比较；保存槽 token 只在格式身份域使用，不改变位置 API 的值契约。 */
    override fun equals(other: Any?): Boolean = other is CfirSourceImportedPosition && begin == other.begin && end == other.end
    override fun hashCode(): Int = 31 * (begin?.hashCode() ?: 0) + (end?.hashCode() ?: 0)
    override fun toString(): String = "CfirSourceImportedPosition(begin=$begin, end=$end)"
}

/** 完整语义签名与 Native partial 的有意省略分开；省略不产生可调用的结果类型。 */
sealed interface CfirSourceCallableSignature {
    data class Resolved(val returnType: ConeCangJieType) : CfirSourceCallableSignature
    data object Unexported : CfirSourceCallableSignature
}

/** 保存 F 的完成值产品只在创建时冻结列表；声明、图与 Lambda 持有同一个真实 header。 */
class CfirSourceImportedFunctionHeader private constructor(
    parameterLists: List<List<CfirValueParameter>>,
    desugaredParameterLists: List<List<CfirNamedFunction?>>,
    val returnType: ConeCangJieType?,
    val captureKind: Int,
    val always: Boolean,
    val bodyAvailable: Boolean,
    val signature: CfirSourceCallableSignature,
) {
    val parameterLists: List<List<CfirValueParameter>> = frozenList(parameterLists.map(::frozenList))
    val desugaredParameterLists: List<List<CfirNamedFunction?>> = frozenList(desugaredParameterLists.map(::frozenList))

    init {
        check(captureKind in 0..255 && this.desugaredParameterLists.size == this.parameterLists.size)
        this.parameterLists.indices.forEach { index ->
            check(this.desugaredParameterLists[index].isEmpty() || this.desugaredParameterLists[index].size == this.parameterLists[index].size)
        }
        if (signature == CfirSourceCallableSignature.Unexported) check(this.parameterLists.isEmpty() &&
            this.desugaredParameterLists.isEmpty() && returnType == null && !bodyAvailable && !always && captureKind == 0) {
            "An unexported signature retains only the native partial empty FuncBody"
        }
    }

    companion object {
        /** 两层列表防御复制，元素仍借用原参数/helper；没有接受可变列表的公开 copy 或构造器。 */
        operator fun invoke(
            parameterLists: List<List<CfirValueParameter>>,
            desugaredParameterLists: List<List<CfirNamedFunction?>>,
            returnType: ConeCangJieType?,
            captureKind: Int,
            always: Boolean,
            bodyAvailable: Boolean,
            signature: CfirSourceCallableSignature,
        ): CfirSourceImportedFunctionHeader = CfirSourceImportedFunctionHeader(
            parameterLists,
            desugaredParameterLists,
            returnType, captureKind, always, bodyAvailable, signature,
        )
    }

    override fun equals(other: Any?): Boolean = other is CfirSourceImportedFunctionHeader &&
        parameterLists == other.parameterLists && desugaredParameterLists == other.desugaredParameterLists &&
        returnType == other.returnType && captureKind == other.captureKind && always == other.always &&
        bodyAvailable == other.bodyAvailable && signature == other.signature

    override fun hashCode(): Int {
        var result = parameterLists.hashCode()
        result = 31 * result + desugaredParameterLists.hashCode()
        result = 31 * result + (returnType?.hashCode() ?: 0)
        result = 31 * result + captureKind
        result = 31 * result + always.hashCode()
        result = 31 * result + bodyAvailable.hashCode()
        return 31 * result + signature.hashCode()
    }

    override fun toString(): String = "CfirSourceImportedFunctionHeader(parameterLists=$parameterLists, " +
        "desugaredParameterLists=$desugaredParameterLists, returnType=$returnType, captureKind=$captureKind, " +
        "always=$always, bodyAvailable=$bodyAvailable, signature=$signature)"
}

/** CFIR 的真实声明语境与派生词法外层 F；不是未保存的 native FuncBody.outerFunc 字段。 */
data class CfirSourceFunctionContext(val declarationContext: CfirDeclaration?, val outerFunction: CfirCallableDeclaration?)

/** 注解实参与普通正文共用 e 身份；显式名称及源序不依赖另一个 annotation reader。 */
data class CfirSourceAnnotationArgument(val name: Name?, val expression: CfirSourceExpressionIdentity)

data class CfirSourceAnnotation(
    val kind: CfirAnnotationExportKind,
    val identifier: String,
    val arguments: List<CfirSourceAnnotationArgument>,
    val target: CfirDeclaration?,
    val targetKind: CfirSourceReferenceKind = CfirSourceReferenceKind.SOURCE,
)


/**
 * 已解析模式的原生语义产品。Type 模式的 inner、类型和两个 runtime 判据由语义层确定；
 * Var 的槽指拥有的声明，Const/Enum 的槽指表达式，不能共享一个无角色的编号列表。
 */
data class CfirSourcePattern(
    val kind: CfirSourcePatternKind,
    val types: List<ConeCangJieType>,
    val slots: List<CfirSourceSlot>,
    val patterns: List<CfirSourcePattern>,
    val needRuntimeTypeCheck: Boolean = false,
    val matchBeforeRuntime: Boolean = false,
    /** Native ExceptTypePattern在语义type前缀后保存真实Type节点，Alias选择属于这些尾槽。 */
    val typeReferences: List<CfirTypeRef> = emptyList(),
    val importedPosition: CfirSourceImportedPosition? = null,
    val constantValues: List<CfirConstantValue> = emptyList(),
    /** MatchCase 根模式借用同一 selector；不是新的拥有表达式。 */
    val contextExpression: CfirSourceExpressionIdentity? = null,
    /** 模式自身的 SOURCE 端点；生成 inner P 没有真实 source 时保持缺失。 */
    val span: CfirSourceSpan? = null,
    val identity: CfirSourcePatternIdentity = CfirSourcePatternIdentity.create(),
)

/** F 的完整保存产品；body=null 是原保存槽缺失，参数/header 与实际 F 身份仍然存在。 */
data class CfirSourceFunctionBody(
    val owner: CfirCallableDeclaration,
    val parameters: List<CfirValueParameter>,
    val signature: CfirSourceCallableSignature,
    val body: CfirSourceExpressionIdentity?,
    val captureKind: Int,
    val always: Boolean,
    val importedHeader: CfirSourceImportedFunctionHeader? = null,
) {
    init {
        check(importedHeader == null || importedHeader.signature == signature) {
            "A function body and its imported header must share the same semantic signature"
        }
        if (signature == CfirSourceCallableSignature.Unexported) check(parameters.isEmpty() && body == null &&
            captureKind == 0 && !always && importedHeader?.signature == signature) {
            "An unexported function body requires its actual omitted-signature header"
        }
    }
}

/**
 * SOURCE30 与三个辅助角色的完成载荷。它不含 PackageFormat、wire index 或活动 resolver。
 * Range/is/as/资源等表面语法只能发布完成的 canonical 载荷，不能交给 encoder 推测结构。
 */
/** 原生引用角色与目标来源正交；builtin extend 的 this 可以没有声明 target。 */
enum class CfirSourceReferenceRole { ORDINARY, THIS, SUPER }

enum class CfirSourceReferenceAbsence { FIELD_MISSING, INVALID_SENTINEL }

/** 保存格式 owner 提供的不可变原引用槽；不表示已绑定语义目标，也不授予正文读取能力。 */
interface CfirSourceSavedReference

/** 完成的引用目标种类，不按名字补声明，也不把解析失败转换成合法 absence。 */
sealed interface CfirSourceReferenceTarget {
    data class Declaration(val declaration: CfirDeclaration) : CfirSourceReferenceTarget
    data class Package(val packageName: org.cangnova.cangjie.name.FqName) : CfirSourceReferenceTarget
    data class Absent(val reason: CfirSourceReferenceAbsence) : CfirSourceReferenceTarget
}

sealed interface CfirSourceExpressionPayload {
    data object PrimitiveType : CfirSourceExpressionPayload
    data object Wildcard : CfirSourceExpressionPayload
    data class Return(val value: CfirSourceExpressionIdentity, val function: CfirCallableDeclaration) : CfirSourceExpressionPayload
    data class Jump(val isBreak: Boolean, val loop: CfirSourceExpressionIdentity) : CfirSourceExpressionPayload
    data class Reference(
        val name: Name,
        val target: CfirSourceReferenceTarget,
        val instantiatedTypes: List<ConeCangJieType>,
        val matchedParentType: CfirReferenceParentType,
        val targetKind: CfirSourceReferenceKind = CfirSourceReferenceKind.SOURCE,
        val role: CfirSourceReferenceRole = CfirSourceReferenceRole.ORDINARY,
    ) : CfirSourceExpressionPayload
    data class Member(val receiver: CfirSourceExpressionIdentity, val reference: Reference) : CfirSourceExpressionPayload
    data class Call(
        val base: CfirSourceExpressionIdentity,
        val arguments: List<CfirSourceExpressionIdentity>,
        val kind: CfirSourceCallKind,
        val hasSideEffect: Boolean,
    ) : CfirSourceExpressionPayload
    data class Unary(val operand: CfirSourceExpressionIdentity, val operator: CfirSourceOperator) : CfirSourceExpressionPayload
    data class Increment(val operand: CfirSourceExpressionIdentity, val operator: CfirSourceOperator) : CfirSourceExpressionPayload
    data class Literal(val kind: CfirLiteralKind, val text: String, val stringKind: CfirSourceStringKind) : CfirSourceExpressionPayload {
        constructor(kind: CfirLiteralKind, text: String, isJavaString: Boolean) :
            this(kind, text, if (isJavaString) CfirSourceStringKind.JSTRING else CfirSourceStringKind.NORMAL)
        val isJavaString: Boolean get() = stringKind == CfirSourceStringKind.JSTRING
    }
    data class Binary(
        val left: CfirSourceExpressionIdentity,
        val right: CfirSourceExpressionIdentity,
        val operator: CfirSourceOperator,
    ) : CfirSourceExpressionPayload
    data class Subscript(
        val receiver: CfirSourceExpressionIdentity,
        val indices: List<CfirSourceExpressionIdentity>,
        val isTupleAccess: Boolean,
    ) : CfirSourceExpressionPayload
    data class Assignment(
        val left: CfirSourceExpressionIdentity,
        val right: CfirSourceExpressionIdentity,
        val operator: CfirSourceOperator,
        val isCompound: Boolean,
    ) : CfirSourceExpressionPayload
    data class Array(
        val arguments: List<CfirSourceExpressionIdentity>,
        val initializer: CfirDeclaration?,
        val isValueArray: Boolean,
        val initializerKind: CfirSourceReferenceKind = CfirSourceReferenceKind.SOURCE,
        val savedInitializer: CfirSourceSavedReference? = null,
    ) : CfirSourceExpressionPayload
    data class Pointer(val argument: CfirSourceExpressionIdentity?) : CfirSourceExpressionPayload
    data class TypeConversion(val value: CfirSourceExpressionIdentity) : CfirSourceExpressionPayload
    data class Throw(val exception: CfirSourceExpressionIdentity) : CfirSourceExpressionPayload
    data class Perform(val value: CfirSourceExpressionIdentity) : CfirSourceExpressionPayload
    data class Spawn(val argument: CfirSourceExpressionIdentity?, val future: CfirDeclaration) : CfirSourceExpressionPayload
    data class ArrayLiteral(
        val elements: List<CfirSourceExpressionIdentity>,
        val initializer: CfirDeclaration?,
        val initializerKind: CfirSourceReferenceKind = CfirSourceReferenceKind.SOURCE,
        val savedInitializer: CfirSourceSavedReference? = null,
        val isValueArray: Boolean = false,
    ) : CfirSourceExpressionPayload
    data class Tuple(val elements: List<CfirSourceExpressionIdentity>) : CfirSourceExpressionPayload
    data class Conditional(
        val condition: CfirSourceExpressionIdentity,
        val thenBranch: CfirSourceExpressionIdentity,
        val elseBranch: CfirSourceExpressionIdentity?,
    ) : CfirSourceExpressionPayload
    data class Try(
        val body: CfirSourceExpressionIdentity,
        val catches: List<CfirSourceExpressionIdentity>,
        val patterns: List<CfirSourcePattern>,
        val finallyBlock: CfirSourceExpressionIdentity?,
        val resources: List<CfirDeclaration>,
    ) : CfirSourceExpressionPayload
    data class Loop(
        val condition: CfirSourceExpressionIdentity,
        val body: CfirSourceExpressionIdentity,
        val isDoWhile: Boolean,
    ) : CfirSourceExpressionPayload
    data class Lambda(val function: CfirSourceFunctionBody, val supportMock: Boolean) : CfirSourceExpressionPayload
    data class Block(val statements: List<CfirSourceSlot>, val isUnsafe: Boolean) : CfirSourceExpressionPayload
    data class Match(
        val selector: CfirSourceExpressionIdentity?,
        val cases: List<CfirSourceExpressionIdentity>,
    ) : CfirSourceExpressionPayload
    data class LetPatternDestructor(
        val initializer: CfirSourceExpressionIdentity,
        val patterns: List<CfirSourcePattern>,
    ) : CfirSourceExpressionPayload
    data class ForIn(
        val iterable: CfirSourceExpressionIdentity,
        val guard: CfirSourceExpressionIdentity?,
        val body: CfirSourceExpressionIdentity,
        val pattern: CfirSourcePattern,
        val kind: CfirSourceForInKind,
    ) : CfirSourceExpressionPayload
    data class MatchCase(
        val patterns: List<CfirSourcePattern>,
        val guard: CfirSourceExpressionIdentity?,
        val body: CfirSourceExpressionIdentity,
    ) : CfirSourceExpressionPayload
    data class MatchCaseOther(val condition: CfirSourceExpressionIdentity, val body: CfirSourceExpressionIdentity) : CfirSourceExpressionPayload
    /** 默认占位没有 value；它和普通显式参数都拥有独立辅助节点身份及应用后的参数类型。 */
    data class Argument(
        val value: CfirSourceExpressionIdentity?,
        val isDefaultValue: Boolean,
        val withInout: Boolean,
    ) : CfirSourceExpressionPayload
}

/** 本代节点的完整记录；map 是借用回边，不能作为拥有 children 再次生产节点。 */
data class CfirSourceExpressionRecord(
    val context: CfirDeclaration,
    val type: ConeCangJieType,
    val span: CfirSourceSpan?,
    val payload: CfirSourceExpressionPayload,
    val mapExpression: CfirSourceExpressionIdentity? = null,
    val overflowStrategy: CangjieOverflowStrategy? = null,
    val importedPosition: CfirSourceImportedPosition? = null,
)

data class CfirSourceDeclarationContext(val owner: CfirDeclaration, val file: CfirFile)

/** 正式发布 owner 的不透明身份；语义图不依赖 CJO reader、provider 或 context。 */
interface CfirSourceGraphPublicationOwner

/** 本地拥有图与原装载域发布图显式区分；owner 标记不代替成功提交的 membership 证明。 */
sealed interface CfirSourceGraphProvenance {
    data object Owned : CfirSourceGraphProvenance
    class Published(val owner: CfirSourceGraphPublicationOwner) : CfirSourceGraphProvenance
}

/**
 * 已完成 SOURCE 产品。创建时一次封闭全部节点和非拥有回边，之后只允许身份查询。
 * 原 CFIR body/type/CFG 保持正常分析生命周期；这个图仅表达原生 SOURCE 规范化结果。
 * 新图不能据此宣称为 BODY 分析就绪，encoder 也不能在这里补解析或生成缺失节点。
 */
class CfirSourceGraph private constructor(
    private val origins: Map<CfirElement, CfirSourceExpressionIdentity>,
    val expressions: Map<CfirSourceExpressionIdentity, CfirSourceExpressionRecord>,
    /** 同次材料化会话已经完成的借用绑定；不属于本图的拥有表达式或正文根。 */
    val borrowedExpressions: Map<CfirSourceExpressionIdentity, CfirSourceExpressionRecord>,
    val functionBodies: Map<CfirCallableDeclaration, CfirSourceExpressionIdentity>,
    /** 原生具名 F 也由 EnumConstructor 持有；不创建替代 CfirFunction。 */
    val functionHeaders: Map<CfirCallableDeclaration, CfirSourceImportedFunctionHeader>,
    val initializers: Map<CfirVariable, CfirSourceExpressionIdentity>,
    val defaults: Map<CfirValueParameter, CfirSourceExpressionIdentity>,
    val declarationPatterns: Map<CfirPatternVariable, CfirSourcePattern>,
    val generatedDeclarations: Map<CfirDeclaration, CfirSourceDeclarationContext>,
    val annotationPlans: Map<CfirDeclaration, List<CfirAnnotationExportPlan>>,
    /** SOURCE保存的完成继承节点；原显式refs和原生生成父类按实际顺序共同发布。 */
    val inheritedTypeReferences: Map<CfirClassLikeDeclaration, List<CfirTypeRef>>,
    /** 声明的实际拥有祖先；helper 指参数、accessor 指 property，词法语境另见 functionContexts。 */
    val declarationOwners: Map<CfirDeclaration, CfirDeclaration?>,
    val functionContexts: Map<CfirCallableDeclaration, CfirSourceFunctionContext>,
    val annotations: Map<CfirDeclaration, List<CfirSourceAnnotation>>,
    /** VarInfo 的完成值与 Pattern.values 共享实际常量对象；没有保存值时不生成值。 */
    val constantValues: Map<CfirVariable, CfirConstantValue>,
    /** 原 F 确有 body，但这份 header-export 产品没有材料化它；不改写原 availability。 */
    val unloadedFunctionBodies: Set<CfirCallableDeclaration>,
    val provenance: CfirSourceGraphProvenance,
    /** 这些 P 是原完成产品的显式借用；其 e 槽不取得本图的 initializer/body 拥有权。 */
    val borrowedDeclarationPatterns: Set<CfirPatternVariable>,
) {
    /** 所有 P 均来自本图已完成的实际拥有载荷；同一个 P token 不能指向两份完成对象。 */
    private val patternsByIdentity: Map<CfirSourcePatternIdentity, CfirSourcePattern> =
        frozenIdentityMap(IdentityHashMap<CfirSourcePatternIdentity, CfirSourcePattern>().apply {
            fun register(pattern: CfirSourcePattern) {
                val previous = putIfAbsent(pattern.identity, pattern)
                check(previous == null || previous === pattern) { "A SOURCE pattern identity must retain one completed product" }
                if (previous == null) pattern.patterns.forEach(::register)
            }
            (expressions.values + borrowedExpressions.values).forEach { it.payload.patterns().forEach(::register) }
            declarationPatterns.values.forEach(::register)
        })

    fun expression(identity: CfirSourceExpressionIdentity): CfirSourceExpressionRecord =
        if (identity in expressions) expressions.getValue(identity) else borrowedExpressions.getValue(identity)
    fun pattern(identity: CfirSourcePatternIdentity): CfirSourcePattern = patternsByIdentity.getValue(identity)
    fun expressionOf(origin: CfirElement): CfirSourceExpressionIdentity = origins.getValue(origin)
    fun containsOrigin(origin: CfirElement): Boolean = origins.containsKey(origin)

    /** 普通正文在同一身份域完成后加入来源映射；此前发布的图及记录保持不可变。 */
    fun withOriginAliases(aliases: Map<CfirElement, CfirSourceExpressionIdentity>): CfirSourceGraph {
        if (aliases.all { (origin, identity) -> origins[origin] === identity }) return this
        val merged = IdentityHashMap(origins)
        aliases.forEach { (origin, identity) ->
            check(identity in expressions) { "A SOURCE origin must belong to this completed expression product" }
            check(merged.putIfAbsent(origin, identity).let { it == null || it === identity }) {
                "A SOURCE origin cannot acquire a different expression identity"
            }
        }
        return CfirSourceGraph(frozenIdentityMap(merged), expressions, borrowedExpressions, functionBodies, functionHeaders,
            initializers, defaults, declarationPatterns, generatedDeclarations, annotationPlans,
            inheritedTypeReferences, declarationOwners, functionContexts, annotations, constantValues,
            unloadedFunctionBodies, provenance, borrowedDeclarationPatterns)
    }

    /**
     * 语义生产事务。先 reserve，再递归 fill；允许 Return/loop/map 回边，发布前必须全 Filled。
     * builder 的可变容器不进入产品，且所有嵌套载荷列表都在 fill 时防御复制。
     */
    class Builder(private val provenance: CfirSourceGraphProvenance = CfirSourceGraphProvenance.Owned) {
        private val origins = IdentityHashMap<CfirElement, CfirSourceExpressionIdentity>()
        private val reserved = identitySet<CfirSourceExpressionIdentity>()
        private val records = IdentityHashMap<CfirSourceExpressionIdentity, CfirSourceExpressionRecord>()
        private val borrowedRecords = IdentityHashMap<CfirSourceExpressionIdentity, CfirSourceExpressionRecord>()
        private val reusedRecords = identitySet<CfirSourceExpressionIdentity>()
        private val caseSelectors = IdentityHashMap<CfirSourceExpressionIdentity, CfirSourceExpressionIdentity>()
        private val bodies = IdentityHashMap<CfirCallableDeclaration, CfirSourceExpressionIdentity>()
        private val headers = IdentityHashMap<CfirCallableDeclaration, CfirSourceImportedFunctionHeader>()
        private val initializers = IdentityHashMap<CfirVariable, CfirSourceExpressionIdentity>()
        private val defaults = IdentityHashMap<CfirValueParameter, CfirSourceExpressionIdentity>()
        private val declarationPatterns = IdentityHashMap<CfirPatternVariable, CfirSourcePattern>()
        private val borrowedDeclarationPatterns = identitySet<CfirPatternVariable>()
        private val generated = IdentityHashMap<CfirDeclaration, CfirSourceDeclarationContext>()
        private val annotationPlans = IdentityHashMap<CfirDeclaration, List<CfirAnnotationExportPlan>>()
        private val inheritedTypeReferences = IdentityHashMap<CfirClassLikeDeclaration, List<CfirTypeRef>>()
        private val declarationOwners = IdentityHashMap<CfirDeclaration, CfirDeclaration?>()
        private val functionContexts = IdentityHashMap<CfirCallableDeclaration, CfirSourceFunctionContext>()
        private val annotations = IdentityHashMap<CfirDeclaration, List<CfirSourceAnnotation>>()
        private val constants = IdentityHashMap<CfirVariable, CfirConstantValue>()
        private val unloadedBodies = identitySet<CfirCallableDeclaration>()
        private var finished = false

        /** header-only 选择是独立能力：保留原保存槽存在性，完整 SOURCE 消费仍需实际 body。 */
        fun unloadedBody(function: CfirCallableDeclaration) {
            check(!finished && !bodies.containsKey(function))
            unloadedBodies += function
        }

        fun existing(origin: CfirElement): CfirSourceExpressionIdentity? = origins[origin]

        /** 仅供同一次语义生产读取已 Filled 子产品；前向回边不能读取尚未完成的载荷。 */
        fun record(identity: CfirSourceExpressionIdentity): CfirSourceExpressionRecord = records.getValue(identity)

        fun reserve(origin: CfirElement): CfirSourceExpressionIdentity {
            check(!finished && !origins.containsKey(origin))
            return reserveGenerated().also { origins[origin] = it }
        }

        fun reserveGenerated(identity: CfirSourceExpressionIdentity = CfirSourceExpressionIdentity.create()): CfirSourceExpressionIdentity {
            check(!finished)
            return identity.also { check(reserved.add(it)) }
        }

        fun alias(origin: CfirElement, identity: CfirSourceExpressionIdentity) {
            check(!finished && identity in reserved && !origins.containsKey(origin))
            origins[origin] = identity
        }

        fun fill(identity: CfirSourceExpressionIdentity, record: CfirSourceExpressionRecord) {
            check(!finished && identity in reserved && !records.containsKey(identity))
            records[identity] = record.copy(payload = record.payload.frozen())
        }

        /** 完成图是冻结证明；只复用指定 record，不导入该图的其它拥有根、来源或正文。 */
        fun reuseExpression(identity: CfirSourceExpressionIdentity, source: CfirSourceGraph) {
            check(!finished && identity in reserved && !records.containsKey(identity))
            records[identity] = source.expressions.getValue(identity)
            check(reusedRecords.add(identity))
        }

        /** 借用只从实际完成图取得绑定闭包；不 reserve 拥有身份，也不认领该图的 d/F/body/origin。 */
        fun borrowExpression(identity: CfirSourceExpressionIdentity, source: CfirSourceGraph) {
            check(!finished && identity !in reserved)
            val pending = ArrayDeque<CfirSourceExpressionIdentity>()
            val visited = identitySet<CfirSourceExpressionIdentity>()
            pending.add(identity)
            fun addPattern(pattern: CfirSourcePattern) {
                pattern.contextExpression?.let(pending::add)
                pattern.slots.forEach { slot -> if (slot is CfirSourceSlot.Expression) pending.add(slot.identity) }
                pattern.patterns.forEach(::addPattern)
            }
            while (pending.isNotEmpty()) {
                val reference = pending.removeFirst()
                if (reference in reserved || !visited.add(reference)) continue
                val record = source.expression(reference)
                check(borrowedRecords.putIfAbsent(reference, record).let { it == null || it === record }) {
                    "A SOURCE borrowed binding must retain its completed expression product"
                }
                record.payload.expressionChildren().forEach(pending::add)
                record.mapExpression?.let(pending::add)
                (record.payload as? CfirSourceExpressionPayload.Jump)?.loop?.let(pending::add)
                record.payload.patterns().forEach(::addPattern)
            }
        }

        /** 子图没有拥有 Match 时，也须由真实 owning 索引登记同一个 selector 绑定。 */
        fun matchCaseSelector(caseIdentity: CfirSourceExpressionIdentity, selector: CfirSourceExpressionIdentity) {
            check(!finished && caseIdentity in reserved && (selector in reserved || selector in borrowedRecords))
            check(caseSelectors.putIfAbsent(caseIdentity, selector).let { it == null || it === selector }) {
                "A SOURCE case must retain its actual owning selector"
            }
        }

        fun body(function: CfirCallableDeclaration, identity: CfirSourceExpressionIdentity) {
            check(!finished && identity in reserved && !bodies.containsKey(function))
            bodies[function] = identity
        }

        fun functionHeader(function: CfirCallableDeclaration, header: CfirSourceImportedFunctionHeader) {
            check(!finished && function.symbol.isBound && !headers.containsKey(function))
            check(header.captureKind in 0..255 && header.parameterLists.size == header.desugaredParameterLists.size)
            header.parameterLists.zip(header.desugaredParameterLists).forEach { (parameters, helpers) ->
                check(helpers.isEmpty() || helpers.size == parameters.size)
                parameters.forEach { check(it.containingDeclarationSymbol === function.symbol) }
            }
            headers[function] = header
        }

        fun initializer(variable: CfirVariable, identity: CfirSourceExpressionIdentity) {
            check(!finished && identity in reserved && !initializers.containsKey(variable))
            initializers[variable] = identity
            // 仅新 SOURCE 声明取得 HAS_INITIAL；原 binding 的初始化投影不改写 normal CFIR 事实。
            if (generated.containsKey(variable)) variable.exportFacts = variable.exportFacts.copy(hasInitial = true)
        }

        fun constantValue(variable: CfirVariable, value: CfirConstantValue) {
            check(!finished && variable.symbol.isBound && !constants.containsKey(variable))
            constants[variable] = value
        }

        fun default(parameter: CfirValueParameter, identity: CfirSourceExpressionIdentity) {
            check(!finished && identity in reserved && !defaults.containsKey(parameter))
            defaults[parameter] = identity
        }

        /** 局部VarWithPatternDecl拥有完整模式；binding声明与模式容器各自保留身份。 */
        fun declarationPattern(variable: CfirPatternVariable, pattern: CfirSourcePattern) {
            check(!finished && !declarationPatterns.containsKey(variable))
            declarationPatterns[variable] = pattern.frozen()
        }

        /** 完成根 P 及其子 P 直接来自原完成图；不接收未经证明的裸 P 或按结构寻找替代。 */
        fun reuseDeclarationPattern(variable: CfirPatternVariable, source: CfirSourceGraph) {
            check(!finished && variable.symbol.isBound && !declarationPatterns.containsKey(variable))
            declarationPatterns[variable] = source.declarationPatterns.getValue(variable)
        }

        /** 只借用已完成 P 及其实际 e 绑定闭包，不选择原函数正文或变量 initializer。 */
        fun borrowDeclarationPattern(variable: CfirPatternVariable, source: CfirSourceGraph) {
            reuseDeclarationPattern(variable, source)
            borrowedDeclarationPatterns += variable
            fun borrow(pattern: CfirSourcePattern) {
                (pattern.expressionChildren() + listOfNotNull(pattern.contextExpression)).forEach { identity ->
                    if (identity !in reserved) borrowExpression(identity, source)
                }
                pattern.patterns.forEach(::borrow)
            }
            borrow(declarationPatterns.getValue(variable))
        }

        fun generatedDeclaration(declaration: CfirDeclaration, context: CfirSourceDeclarationContext) {
            check(!finished && declaration.symbol.isBound && declaration.symbol.cfir === declaration)
            check(!generated.containsKey(declaration))
            generated[declaration] = context
            declarationOwner(declaration, context.owner)
        }

        /** 计划及其嵌套有序参数防御复制；空计划仍是已完成的真实声明产品。 */
        fun annotationPlans(declaration: CfirDeclaration, plans: List<CfirAnnotationExportPlan>) {
            check(!finished && !annotationPlans.containsKey(declaration))
            annotationPlans[declaration] = Collections.unmodifiableList(plans.map { plan ->
                plan.copy(arguments = Collections.unmodifiableList(plan.arguments.toList()))
            })
        }

        fun inheritedTypes(declaration: CfirClassLikeDeclaration, refs: List<CfirTypeRef>) {
            check(!finished && !inheritedTypeReferences.containsKey(declaration))
            inheritedTypeReferences[declaration] = frozenList(refs)
        }

        fun declarationOwner(declaration: CfirDeclaration, owner: CfirDeclaration?) {
            check(!finished && declaration.symbol.isBound && !declarationOwners.containsKey(declaration))
            check(owner !== declaration && (owner == null || owner.symbol.isBound))
            declarationOwners[declaration] = owner
        }

        fun functionContext(function: CfirCallableDeclaration, context: CfirSourceFunctionContext) {
            check(!finished && function.symbol.isBound && !functionContexts.containsKey(function))
            check(context.outerFunction !== function)
            functionContexts[function] = context
        }

        fun annotations(declaration: CfirDeclaration, values: List<CfirSourceAnnotation>) {
            check(!finished && !annotations.containsKey(declaration))
            annotations[declaration] = frozenList(values.map { it.copy(arguments = frozenList(it.arguments)) })
        }

        fun finish(): CfirSourceGraph {
            check(!finished && records.keys.containsAll(reserved) && records.size == reserved.size) {
                "SOURCE production cannot publish reserved or missing records"
            }
            // native refs 阶段在全部 case/selector 已登记后补 ctxExpr，不从 consumer 当前上下文猜测。
            val caseOwners = IdentityHashMap<CfirSourceExpressionIdentity, CfirSourceExpressionIdentity>()
            fun completeSelector(caseIdentity: CfirSourceExpressionIdentity, selector: CfirSourceExpressionIdentity) {
                val caseRecord = records.getValue(caseIdentity)
                val case = caseRecord.payload as? CfirSourceExpressionPayload.MatchCase
                    ?: error("Selected SOURCE match requires MatchCase")
                if (case.patterns.any { it.contextExpression !== selector }) {
                    check(caseIdentity !in reusedRecords) { "A completed SOURCE pattern must retain its first completed selector" }
                    records[caseIdentity] = caseRecord.copy(payload = case.copy(patterns = frozenList(case.patterns.map { pattern ->
                        check(pattern.contextExpression == null || pattern.contextExpression === selector)
                        pattern.copy(contextExpression = selector)
                    })))
                }
            }
            caseSelectors.forEach { (caseIdentity, selector) -> completeSelector(caseIdentity, selector) }
            records.entries.toList().forEach { (matchIdentity, record) ->
                val match = record.payload as? CfirSourceExpressionPayload.Match ?: return@forEach
                val selector = match.selector ?: return@forEach
                match.cases.forEach { caseIdentity ->
                    check(caseOwners.putIfAbsent(caseIdentity, matchIdentity).let { it == null || it === matchIdentity }) {
                        "SOURCE case has multiple owning matches"
                    }
                    check(caseSelectors[caseIdentity].let { it == null || it === selector }) { "SOURCE case selector disagrees with its owning Match" }
                    completeSelector(caseIdentity, selector)
                }
            }
            fun requireExpression(identity: CfirSourceExpressionIdentity) {
                check(identity in records || identity in borrowedRecords) { "SOURCE edge is outside its completed binding product" }
            }
            fun requireOwnedExpression(identity: CfirSourceExpressionIdentity) {
                check(identity in records) { "SOURCE owning edge is outside its selected owning product" }
            }
            fun requireBlock(identity: CfirSourceExpressionIdentity) {
                requireOwnedExpression(identity)
                check(records.getValue(identity).payload is CfirSourceExpressionPayload.Block)
            }
            records.values.forEach { record ->
                record.payload.expressionChildren().forEach(::requireOwnedExpression)
                record.mapExpression?.let(::requireExpression)
                record.payload.patterns().forEach { it.validate(::requireOwnedExpression, ::requireExpression) }
                when (val payload = record.payload) {
                    is CfirSourceExpressionPayload.Return -> check(payload.function.symbol.isBound)
                    is CfirSourceExpressionPayload.Jump -> {
                        requireExpression(payload.loop)
                        val loopRecord = if (payload.loop in records) records.getValue(payload.loop) else borrowedRecords.getValue(payload.loop)
                        check(loopRecord.payload.let {
                            it is CfirSourceExpressionPayload.Loop || it is CfirSourceExpressionPayload.ForIn
                        }) { "SOURCE jump must reference its actual loop" }
                    }
                    is CfirSourceExpressionPayload.Reference -> if (payload.target is CfirSourceReferenceTarget.Declaration) check(payload.target.declaration.symbol.isBound)
                    is CfirSourceExpressionPayload.Member -> if (payload.reference.target is CfirSourceReferenceTarget.Declaration) check(payload.reference.target.declaration.symbol.isBound)
                    is CfirSourceExpressionPayload.Call -> check(payload.arguments.all {
                        records.getValue(it).payload is CfirSourceExpressionPayload.Argument
                    })
                    is CfirSourceExpressionPayload.Subscript -> check(payload.indices.isNotEmpty())
                    is CfirSourceExpressionPayload.Conditional -> requireBlock(payload.thenBranch)
                    is CfirSourceExpressionPayload.Try -> {
                        check(payload.catches.size == payload.patterns.size)
                        requireBlock(payload.body)
                        payload.catches.forEach(::requireBlock)
                        payload.finallyBlock?.let(::requireBlock)
                    }
                    is CfirSourceExpressionPayload.Loop -> requireBlock(payload.body)
                    is CfirSourceExpressionPayload.Lambda -> {
                        check(payload.function.signature is CfirSourceCallableSignature.Resolved) {
                            "A lambda cannot use an unexported partial signature"
                        }
                        payload.function.body?.let(::requireBlock)
                        val header = payload.function.importedHeader
                        check(payload.function.body != null || header != null && !header.bodyAvailable)
                        if (header != null) check(header.bodyAvailable == (payload.function.body != null))
                        check(payload.function.owner.symbol.isBound && payload.function.captureKind in 0..255)
                    }
                    is CfirSourceExpressionPayload.Match -> {
                        check(payload.cases.isNotEmpty())
                        check(payload.cases.all { records.getValue(it).payload.let { case ->
                            if (payload.selector == null) case is CfirSourceExpressionPayload.MatchCaseOther
                            else case is CfirSourceExpressionPayload.MatchCase
                        } })
                    }
                    is CfirSourceExpressionPayload.ForIn -> {
                        requireBlock(payload.body)
                        if (payload.kind != CfirSourceForInKind.DIRECT) requireBlock(payload.iterable)
                    }
                    is CfirSourceExpressionPayload.MatchCase -> {
                        check(payload.patterns.isNotEmpty())
                        requireBlock(payload.body)
                    }
                    is CfirSourceExpressionPayload.MatchCaseOther -> requireBlock(payload.body)
                    is CfirSourceExpressionPayload.Argument -> check(payload.isDefaultValue == (payload.value == null))
                    else -> Unit
                }
            }
            bodies.values.forEach(::requireBlock)
            headers.forEach { (function, header) ->
                check(header.bodyAvailable == (bodies.containsKey(function) || function in unloadedBodies) &&
                    !(bodies.containsKey(function) && function in unloadedBodies)) { "SOURCE header/body availability disagree" }
            }
            declarationPatterns.forEach { (variable, pattern) ->
                pattern.validate(if (variable in borrowedDeclarationPatterns) ::requireExpression else ::requireOwnedExpression, ::requireExpression)
            }
            initializers.values.forEach(::requireOwnedExpression)
            defaults.values.forEach(::requireOwnedExpression)
            annotationPlans.values.flatten().forEach { plan ->
                plan.arguments.forEach { argument -> requireExpression(checkNotNull(origins[argument.expression])) }
                plan.target?.let { check(it.symbol.isBound && it.symbol.cfir === it) }
            }
            annotations.values.flatten().forEach { annotation ->
                annotation.arguments.forEach { requireExpression(it.expression) }
                annotation.target?.let { check(it.symbol.isBound) }
            }
            finished = true
            return CfirSourceGraph(frozenIdentityMap(origins), frozenIdentityMap(records), frozenIdentityMap(borrowedRecords), frozenIdentityMap(bodies), frozenIdentityMap(headers),
                frozenIdentityMap(initializers), frozenIdentityMap(defaults), frozenIdentityMap(declarationPatterns), frozenIdentityMap(generated),
                frozenIdentityMap(annotationPlans), frozenIdentityMap(inheritedTypeReferences),
                frozenIdentityMap(declarationOwners), frozenIdentityMap(functionContexts), frozenIdentityMap(annotations),
                frozenIdentityMap(constants), Collections.unmodifiableSet(identitySet<CfirCallableDeclaration>().apply { addAll(unloadedBodies) }), provenance,
                Collections.unmodifiableSet(identitySet<CfirPatternVariable>().apply { addAll(borrowedDeclarationPatterns) }))
        }
    }
}

/** 完成图的实际拥有槽；引用 target、借用表达式及词法祖先不因被读取而成为本图拥有声明。 */
fun CfirSourceGraph.forEachOwningDeclaration(action: (CfirDeclaration) -> Unit) {
    declarationOwners.keys.forEach(action)
    functionContexts.keys.forEach(action)
    functionHeaders.forEach { (function, header) ->
        action(function)
        header.parameterLists.flatten().forEach(action)
        header.desugaredParameterLists.flatten().filterNotNull().forEach(action)
    }
    functionBodies.keys.forEach(action)
    initializers.keys.forEach(action)
    defaults.keys.forEach(action)
    declarationPatterns.keys.forEach(action)
    annotations.keys.forEach(action)
    constantValues.keys.forEach(action)
    generatedDeclarations.keys.forEach(action)
}

/** 身份与完成就绪门不解释 borrowed target；Published 标记仍须由原 owner 证明成功发布。 */
fun CfirSourceGraph.requireCanonicalOwningDeclarations() = forEachOwningDeclaration { declaration ->
    check(declaration.symbol.isBound && declaration.symbol.cfir === declaration) { "SOURCE owning slots require canonical declaration bindings" }
    when (val provenance = provenance) {
        CfirSourceGraphProvenance.Owned -> {
            check(declaration.contentSource === CfirContentSource.Owned) { "Owned SOURCE cannot claim a published declaration's content" }
            declaration.requireOwnedGraphAnalysisReady()
        }
        is CfirSourceGraphProvenance.Published -> check(declaration.contentSource === provenance.owner) {
            "Published SOURCE owning slots must retain their actual content owner"
        }
    }
}

/** 表达式拥有边，不含 Jump.loop、Return.function、map 或普通 reference 的声明目标。 */
fun CfirSourceExpressionPayload.expressionChildren(): List<CfirSourceExpressionIdentity> = when (this) {
    CfirSourceExpressionPayload.PrimitiveType, CfirSourceExpressionPayload.Wildcard,
    is CfirSourceExpressionPayload.Jump, is CfirSourceExpressionPayload.Reference,
    is CfirSourceExpressionPayload.Literal -> emptyList()
    is CfirSourceExpressionPayload.Return -> listOf(value)
    is CfirSourceExpressionPayload.Member -> listOf(receiver)
    is CfirSourceExpressionPayload.Call -> listOf(base) + arguments
    is CfirSourceExpressionPayload.Unary -> listOf(operand)
    is CfirSourceExpressionPayload.Increment -> listOf(operand)
    is CfirSourceExpressionPayload.Binary -> listOf(left, right)
    is CfirSourceExpressionPayload.Subscript -> listOf(receiver) + indices
    is CfirSourceExpressionPayload.Assignment -> listOf(left, right)
    is CfirSourceExpressionPayload.Array -> arguments
    is CfirSourceExpressionPayload.Pointer -> listOfNotNull(argument)
    is CfirSourceExpressionPayload.TypeConversion -> listOf(value)
    is CfirSourceExpressionPayload.Throw -> listOf(exception)
    is CfirSourceExpressionPayload.Perform -> listOf(value)
    is CfirSourceExpressionPayload.Spawn -> listOfNotNull(argument)
    is CfirSourceExpressionPayload.ArrayLiteral -> elements
    is CfirSourceExpressionPayload.Tuple -> elements
    is CfirSourceExpressionPayload.Conditional -> listOfNotNull(condition, thenBranch, elseBranch)
    is CfirSourceExpressionPayload.Try -> listOf(body) + catches + listOfNotNull(finallyBlock) + patterns.flatMap { it.expressionChildren() }
    is CfirSourceExpressionPayload.Loop -> listOf(condition, body)
    is CfirSourceExpressionPayload.Lambda -> listOfNotNull(function.body)
    is CfirSourceExpressionPayload.Block -> statements.filterIsInstance<CfirSourceSlot.Expression>().map { it.identity }
    is CfirSourceExpressionPayload.Match -> listOfNotNull(selector) + cases
    is CfirSourceExpressionPayload.LetPatternDestructor -> listOf(initializer) + patterns.flatMap { it.expressionChildren() }
    is CfirSourceExpressionPayload.ForIn -> listOfNotNull(iterable, guard, body) + pattern.expressionChildren()
    is CfirSourceExpressionPayload.MatchCase -> listOfNotNull(guard, body) + patterns.flatMap { it.expressionChildren() }
    is CfirSourceExpressionPayload.MatchCaseOther -> listOf(condition, body)
    is CfirSourceExpressionPayload.Argument -> listOfNotNull(value)
}

fun CfirSourcePattern.expressionChildren(): List<CfirSourceExpressionIdentity> =
    slots.filterIsInstance<CfirSourceSlot.Expression>().map { it.identity } + patterns.flatMap { it.expressionChildren() }

fun CfirSourceExpressionPayload.patterns(): List<CfirSourcePattern> = when (this) {
    is CfirSourceExpressionPayload.Try -> patterns
    is CfirSourceExpressionPayload.LetPatternDestructor -> patterns
    is CfirSourceExpressionPayload.ForIn -> listOf(pattern)
    is CfirSourceExpressionPayload.MatchCase -> patterns
    else -> emptyList()
}

private fun CfirSourcePattern.validate(
    requireOwnedExpression: (CfirSourceExpressionIdentity) -> Unit,
    requireExpression: (CfirSourceExpressionIdentity) -> Unit,
) {
    contextExpression?.let(requireExpression)
    check(types.isNotEmpty()) { "SOURCE pattern requires its completed own type" }
    slots.forEach { slot -> when (slot) {
        is CfirSourceSlot.Expression -> requireOwnedExpression(slot.identity)
        is CfirSourceSlot.Declaration -> check(slot.declaration.symbol.isBound)
    } }
    patterns.forEach { it.validate(requireOwnedExpression, requireExpression) }
    when (kind) {
        CfirSourcePatternKind.VARIABLE -> check(slots.size == 1 && slots.single() is CfirSourceSlot.Declaration)
        CfirSourcePatternKind.TYPE -> check(patterns.size == 1 && slots.isEmpty())
        CfirSourcePatternKind.CONSTANT -> check(slots.size in 1..2 && slots.all { it is CfirSourceSlot.Expression })
        CfirSourcePatternKind.WILDCARD -> check(slots.isEmpty() && patterns.isEmpty())
        CfirSourcePatternKind.TUPLE -> check(slots.isEmpty())
        CfirSourcePatternKind.ENUM -> check(slots.size == 1 && slots.single() is CfirSourceSlot.Expression)
        CfirSourcePatternKind.EXCEPTION_TYPE, CfirSourcePatternKind.COMMAND_TYPE -> check(slots.isEmpty() && patterns.size == 1)
    }
}

private fun CfirSourceExpressionPayload.frozen(): CfirSourceExpressionPayload = when (this) {
    is CfirSourceExpressionPayload.Reference -> copy(instantiatedTypes = frozenList(instantiatedTypes))
    is CfirSourceExpressionPayload.Member -> copy(reference = reference.copy(instantiatedTypes = frozenList(reference.instantiatedTypes)))
    is CfirSourceExpressionPayload.Call -> copy(arguments = frozenList(arguments))
    is CfirSourceExpressionPayload.Subscript -> copy(indices = frozenList(indices))
    is CfirSourceExpressionPayload.Array -> copy(arguments = frozenList(arguments))
    is CfirSourceExpressionPayload.ArrayLiteral -> copy(elements = frozenList(elements))
    is CfirSourceExpressionPayload.Tuple -> copy(elements = frozenList(elements))
    is CfirSourceExpressionPayload.Try -> copy(catches = frozenList(catches), patterns = frozenList(patterns.map { it.frozen() }), resources = frozenList(resources))
    is CfirSourceExpressionPayload.Lambda -> copy(function = function.copy(
        parameters = frozenList(function.parameters),
    ))
    is CfirSourceExpressionPayload.Block -> copy(statements = frozenList(statements))
    is CfirSourceExpressionPayload.Match -> copy(cases = frozenList(cases))
    is CfirSourceExpressionPayload.LetPatternDestructor -> copy(patterns = frozenList(patterns.map { it.frozen() }))
    is CfirSourceExpressionPayload.ForIn -> copy(pattern = pattern.frozen())
    is CfirSourceExpressionPayload.MatchCase -> copy(patterns = frozenList(patterns.map { it.frozen() }))
    else -> this
}

private fun CfirSourcePattern.frozen(): CfirSourcePattern = copy(
    types = frozenList(types), slots = frozenList(slots), patterns = frozenList(patterns.map { it.frozen() }),
    typeReferences = frozenList(typeReferences),
    constantValues = frozenList(constantValues),
)

private fun <T> frozenList(values: Collection<T>): List<T> = Collections.unmodifiableList(values.toList())
private fun <K : Any, V> frozenIdentityMap(values: Map<K, V>): Map<K, V> =
    Collections.unmodifiableMap(IdentityHashMap<K, V>().apply { putAll(values) })
private fun <T : Any> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap<T, Boolean>())
