package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.declarations.parsingSourceContext
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.type.AbstractTypeChecker
import java.util.Collections

/** 原 bound 的一个叶用途与其派生比较类型；原声明、引用、根类型和解析域始终保留。 */
class CfirGenericConstraintBound internal constructor(
    val owner: CfirTypeParameterSymbol,
    val declaration: CfirTypeParameter,
    val containingDeclarationSymbol: CfirBasedSymbol<*>,
    val boundOrdinal: Int,
    componentPath: List<Int>,
    val reference: CfirResolvedTypeRef,
    val originalRootType: ConeCangJieType,
    val originalType: ConeCangJieType,
    val type: ConeCangJieType,
    val domain: CfirSession,
) {
    val componentPath: List<Int> = Collections.unmodifiableList(ArrayList(componentPath))
    internal fun withType(value: ConeCangJieType): CfirGenericConstraintBound =
        CfirGenericConstraintBound(owner, declaration, containingDeclarationSymbol, boundOrdinal, componentPath,
            reference, originalRootType, originalType, value, domain)
}

/**
 * 在原 TP 拥有域收集的闭合输入；不是另一套声明或类型推断事实。
 * 原 intersection 先拆叶、原 TP 边先闭合，再允许派生类型替换和求交。
 */
class CfirGenericConstraintBounds private constructor(
    roots: List<CfirTypeParameterSymbol>,
    inputs: Map<CfirTypeParameterSymbol, ParameterInput>,
    bounds: Map<CfirTypeParameterSymbol, List<CfirGenericConstraintBound>>,
) {
    private val roots: List<CfirTypeParameterSymbol> = immutable(roots)
    private val inputs: Map<CfirTypeParameterSymbol, ParameterInput> = Collections.unmodifiableMap(LinkedHashMap(inputs))
    private val bounds: Map<CfirTypeParameterSymbol, List<CfirGenericConstraintBound>> =
        Collections.unmodifiableMap(bounds.mapValues { immutable(it.value) })

    private class ParameterInput(
        val declaration: CfirTypeParameter,
        val domain: CfirSession,
        val containingSymbol: CfirBasedSymbol<*>,
        val containingDeclaration: CfirDeclaration,
        references: List<CfirResolvedTypeRef>,
        rootTypes: List<ConeCangJieType>,
        rootShapes: List<BoundShape>,
    ) {
        val references: List<CfirResolvedTypeRef> = immutable(references)
        val rootTypes: List<ConeCangJieType> = immutable(rootTypes)
        val rootShapes: List<BoundShape> = immutable(rootShapes)
        private val declarationSource = declaration.source
        private val containingSource = containingDeclaration.source
        private val referenceSources = immutable(references.map { it.source })
        private val sourceInput: SourceInput? = if (declaration.origin.fromSource || containingDeclaration.origin.fromSource) {
            check(!declaration.origin.fromSource || declaration.resolvePhase >= CfirResolvePhase.TYPES) {
                "Source bound capture requires the completed original TYPES input"
            }
            check(!containingDeclaration.origin.fromSource || containingDeclaration.resolvePhase >= CfirResolvePhase.TYPES) {
                "Source bound capture requires its completed containing header"
            }
            val file = checkNotNull(containingDeclaration.moduleData.session.cfirProvider.getContainingFile(containingSymbol)) {
                "Source bounds require their original containing file product"
            }
            check(file.moduleData === containingDeclaration.moduleData) { "Source bound file must belong to its original declaration module" }
            SourceInput(file)
        } else null

        fun validateSource() {
            check(declaration.source === declarationSource && containingDeclaration.source === containingSource &&
                references.indices.all { references[it].source === referenceSources[it] }) {
                "Original bound sources changed after input capture"
            }
            sourceInput?.validate()
        }
    }

    /** 复用原 parser/source 产品的生命周期，不创造新 revision，也不在失效后重收 raw bounds。 */
    private class SourceInput(val file: CfirFile) {
        private val module = file.moduleData
        private val source = file.source
        private val sourceFile = checkNotNull(file.sourceFile) { "Source bounds require the original source file" }
        private val lines = file.sourceFileLinesMapping
        private val parserSource = checkNotNull(file.parsingSourceContext) { "Source bounds require the original parser input" }

        fun validate() {
            // 原 LL 文件产品在同一 session monitor 下提交；只读这些槽，不触发 resolver/provider。
            synchronized(module.session) {
                check(file.symbol.isBound && file.symbol.cfir === file && file.moduleData === module &&
                    file.source === source && file.sourceFile === sourceFile && file.sourceFileLinesMapping === lines &&
                    file.parsingSourceContext === parserSource) {
                    "Original source product changed after bound capture"
                }
            }
        }
    }

    /** 原交叉成分的 encounter 路径；保存实际对象/槽位，而不是把原 collection 当成不可变。 */
    private class BoundShape(val type: ConeCangJieType, children: List<BoundShape>) {
        val children: List<BoundShape> = immutable(children)
        fun validate(current: ConeCangJieType) {
            check(current === type) { "Original bound occurrence changed after input capture" }
            if (type is ConeIntersectionType) {
                val members = type.intersectedTypes.toList()
                check(members.size == children.size) { "Original intersection shape changed after input capture" }
                members.indices.forEach { children[it].validate(members[it]) }
            }
        }
    }

    /** 只校验原有数据，不在消费时推进 lazy phase 或重读另一个 owner 的 bounds。 */
    fun validateOriginalInput() {
        for ((symbol, input) in inputs) {
            check(symbol.isBound && symbol.cfir === input.declaration &&
                input.declaration.moduleData.session === input.domain) { "Bound owner changed after input capture" }
            check(input.declaration.containingDeclarationSymbol === input.containingSymbol &&
                input.containingSymbol.isBound && input.containingSymbol.cfir === input.containingDeclaration) {
                "Original containing declaration changed after bound capture"
            }
            val current = input.declaration.bounds
            check(current.size == input.references.size && current.indices.all {
                current[it] === input.references[it] && input.references[it].coneType === input.rootTypes[it]
            }) { "Original bound references changed after input capture" }
            input.rootShapes.indices.forEach { input.rootShapes[it].validate(input.references[it].coneType) }
            input.validateSource()
        }
    }

    fun requireParameters(parameters: List<CfirTypeParameterRef>) {
        check(parameters.size == roots.size && parameters.indices.all { parameters[it].symbol === roots[it] }) {
            "Constraint inputs must belong to the original corresponding parameters"
        }
        validateOriginalInput()
    }

    /** 不按裸 Cone 去重，不让相同 Cone 的两个原 owner 丢失任一来源。 */
    fun closedBounds(parameter: CfirTypeParameterSymbol): List<CfirGenericConstraintBound> {
        check(parameter in inputs) { "Parameter was not captured in the closed bound input" }
        validateOriginalInput()
        val result = ArrayList<CfirGenericConstraintBound>()
        val pending = ArrayDeque<CfirTypeParameterSymbol>()
        val visited = LinkedHashSet<CfirTypeParameterSymbol>()
        pending += parameter
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (!visited.add(current)) continue
            for (bound in bounds.getValue(current)) {
                result += bound
                // 传递依赖来自原叶，不能由 mapTypes 后的类型形状决定是否继续访问原 owner。
                (bound.originalType as? ConeTypeParameterType)?.let { pending += it.lookupTag.typeParameterSymbol }
            }
        }
        return immutable(result)
    }

    /** 仅产生保留原 origin 的比较视图；转换器不会存进输入，也不会写回原 CFIR。 */
    fun mapTypes(transform: (CfirGenericConstraintBound) -> ConeCangJieType): CfirGenericConstraintBounds {
        validateOriginalInput()
        val mapped = bounds.mapValues { (_, entries) -> entries.map { it.withType(transform(it)) } }
        validateOriginalInput()
        return CfirGenericConstraintBounds(roots, inputs, mapped)
    }

    companion object {
        /** 与原 resolvedBounds 拥有路径一致，先完成原 TP 的 TYPES 读取，再记录真实绑定对象。 */
        fun capture(parameters: List<CfirTypeParameterRef>): CfirGenericConstraintBounds {
            val roots = parameters.map { it.symbol }
            val pending = ArrayDeque<CfirTypeParameterSymbol>()
            pending.addAll(roots)
            val inputs = LinkedHashMap<CfirTypeParameterSymbol, ParameterInput>()
            val bounds = LinkedHashMap<CfirTypeParameterSymbol, List<CfirGenericConstraintBound>>()
            while (pending.isNotEmpty()) {
                val symbol = pending.removeFirst()
                if (symbol in inputs) continue
                val references = symbol.resolvedBounds.toList()
                val declaration = symbol.cfir
                val domain = declaration.moduleData.session
                val containingSymbol = declaration.containingDeclarationSymbol
                check(containingSymbol.isBound) { "Bound collection requires its original containing declaration" }
                val containingDeclaration = containingSymbol.cfir
                val rootTypes = references.map { it.coneType }
                val visiting = Collections.newSetFromMap(java.util.IdentityHashMap<ConeCangJieType, Boolean>())
                fun shape(type: ConeCangJieType): BoundShape {
                    check(visiting.add(type)) { "Original bounds contain a structural intersection cycle" }
                    try {
                        val children = if (type is ConeIntersectionType) type.intersectedTypes.map(::shape) else emptyList()
                        return BoundShape(type, children)
                    } finally {
                        visiting.remove(type)
                    }
                }
                val rootShapes = rootTypes.map(::shape)
                inputs[symbol] = ParameterInput(declaration, domain, containingSymbol, containingDeclaration, references, rootTypes, rootShapes)
                val entries = ArrayList<CfirGenericConstraintBound>()
                references.forEachIndexed { index, reference ->
                    val rootType = rootTypes[index]
                    fun collect(shape: BoundShape, path: List<Int>) {
                        val type = shape.type
                        if (type is ConeIntersectionType) {
                            shape.children.forEachIndexed { childIndex, child -> collect(child, path + childIndex) }
                        } else {
                            entries += CfirGenericConstraintBound(symbol, declaration, containingSymbol, index, path,
                                reference, rootType, type, type, domain)
                            (type as? ConeTypeParameterType)?.let { pending += it.lookupTag.typeParameterSymbol }
                        }
                    }
                    collect(rootShapes[index], emptyList())
                }
                bounds[symbol] = immutable(entries)
            }
            return CfirGenericConstraintBounds(roots, inputs, bounds).also { it.validateOriginalInput() }
        }

        private fun <T> immutable(values: Collection<T>): List<T> = Collections.unmodifiableList(ArrayList(values))
    }
}

/** 每个父声明的约束及其到子声明空间的替换器；CJMP 可显式组合外层 owner 与函数映射。 */
data class CfirGenericConstraintParent(
    val typeParameters: List<CfirTypeParameterRef>,
    val substitutor: ConeSubstitutor,
    val bounds: CfirGenericConstraintBounds,
)

/**
 * 仓颉的泛型约束域宽严比较。返回不兼容的参数位置，不报告诊断，也不读取函数体。
 *
 * 单个父声明的 bounds 构成有效交集；多个父声明保持为候选约束域的并集。
 * 子域必须覆盖至少一个父域，并且不能严格窄于任何父域。定位与诊断通道由调用方负责。
 */
object CfirGenericConstraintCompatibility {
    /** 声明检查的已准备单 parent 域；传递闭包和 owner 投影已在准备时完成，不重新读取符号 bounds。 */
    fun findIncompatiblePreparedBounds(
        childBounds: List<List<ConeCangJieType>>,
        parentBounds: List<List<ConeCangJieType>>,
        context: ConeTypeContext,
    ): List<Int> {
        require(childBounds.size == parentBounds.size)
        fun domain(values: List<ConeCangJieType>): ConeCangJieType = ConeTypeIntersector.intersectTypes(context,
            values.filterNot { it is ConeErrorType || it == ConeAnyType || (it as? ConeClassLikeType)?.classId == StdlibClassIds.Any })
        return childBounds.indices.filter { index ->
            val child = domain(childBounds[index])
            val parent = domain(parentBounds[index])
            !AbstractTypeChecker.isSubtypeOf(context, parent, child) ||
                !AbstractTypeChecker.equalTypes(context, child, parent) && AbstractTypeChecker.isSubtypeOf(context, child, parent)
        }
    }
    /** 两侧各自收集完整传递上界后再替换；调用者必须明确子侧的比较类型空间。 */
    fun findIncompatibleTypeParameterIndices(
        childParameters: List<CfirTypeParameterRef>,
        parents: List<CfirGenericConstraintParent>,
        context: ConeTypeContext,
        childSubstitutor: ConeSubstitutor,
        childBounds: CfirGenericConstraintBounds,
    ): List<Int> {
        require(parents.all { it.typeParameters.size == childParameters.size }) {
            "Generic constraint comparison requires corresponding type parameters"
        }
        childBounds.requireParameters(childParameters)
        parents.forEach { it.bounds.requireParameters(it.typeParameters) }
        if (childParameters.isEmpty() || parents.isEmpty()) return emptyList()
        val parentDomains = List(childParameters.size) { linkedSetOf<ConeCangJieType>() }
        for (parent in parents) {
            for (index in childParameters.indices) {
                parentDomains[index] += effectiveDomain(parent.bounds, parent.typeParameters[index].symbol, parent.substitutor, context)
            }
        }
        val incompatible = childParameters.indices.filter { index ->
            val childDomain = effectiveDomain(childBounds, childParameters[index].symbol, childSubstitutor, context)
            val domains = parentDomains[index]
            val coversAtLeastOne = domains.any { AbstractTypeChecker.isSubtypeOf(context, it, childDomain) }
            val strictlyNarrows = domains.any {
                !AbstractTypeChecker.equalTypes(context, childDomain, it) && AbstractTypeChecker.isSubtypeOf(context, childDomain, it)
            }
            !coversAtLeastOne || strictlyNarrows
        }
        childBounds.validateOriginalInput()
        parents.forEach { it.bounds.validateOriginalInput() }
        return incompatible
    }

    /** 先收集传递上界再替换和规范化，避免在父参数空间提前简化而丢失约束。 */
    private fun effectiveDomain(
        input: CfirGenericConstraintBounds,
        parameter: CfirTypeParameterSymbol,
        substitutor: ConeSubstitutor,
        context: ConeTypeContext,
    ): ConeCangJieType {
        val bounds = input.closedBounds(parameter).filterNot {
            it.originalType is ConeErrorType || it.originalType == ConeAnyType ||
                (it.originalType as? ConeClassLikeType)?.classId == StdlibClassIds.Any
        }
        return ConeTypeIntersector.intersectTypes(context, bounds.map { substitutor.substituteOrSelf(it.type) })
    }
}
