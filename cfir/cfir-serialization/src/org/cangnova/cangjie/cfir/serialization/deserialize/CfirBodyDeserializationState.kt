package org.cangnova.cangjie.cfir.serialization.deserialize

import org.cangnova.cangjie.cfir.CfirFunctionBodyOwner
import org.cangnova.cangjie.cfir.CfirPublishedContentSnapshot
import org.cangnova.cangjie.cfir.CfirPublishedAnnotations
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.CfirLoopTarget
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.source.*
import java.util.Collections
import java.util.IdentityHashMap

/** 同一包的 canonical 与普通正文共同发布；读者只取得完成的身份、记录及目标。 */
internal class CfirBodyDeserializationSnapshot(
    val identities: Map<UInt, CfirSourceExpressionIdentity> = emptyMap(),
    val records: Map<UInt, CfirSourceExpressionRecord> = emptyMap(),
    val patternIdentities: Map<Int, CfirSourcePatternIdentity> = emptyMap(),
    val patternRecords: Map<Int, CfirSourcePattern> = emptyMap(),
    val expressions: Map<CfirSourceExpressionIdentity, CfirExpression> = emptyMap(),
    val patterns: Map<CfirSourcePatternIdentity, CfirPattern> = emptyMap(),
    val loops: Map<CfirSourceExpressionIdentity, CfirLoopTarget> = emptyMap(),
    val functions: Map<CfirFunctionBodyOwner, CfirFunctionTarget> = emptyMap(),
    val bodies: Map<CfirCallableDeclaration, CfirBlock> = emptyMap(),
    val initializers: Map<CfirVariable, CfirExpression> = emptyMap(),
    val expressionSources: Map<UInt, CfirSourceGraph> = emptyMap(),
    val patternSources: Map<Int, CfirSourceGraph> = emptyMap(),
    val defaults: Map<CfirValueParameter, CfirExpression> = emptyMap(),
    val annotationContents: Map<CfirDeclaration, CfirPublishedAnnotations.Complete> = emptyMap(),
    val selectedGraphs: Map<List<CjoSourceContentRoot>, CfirDeserializedSourceGraph> = emptyMap(),
    val additionalOrigins: Map<org.cangnova.cangjie.cfir.CfirElement, CfirSourceExpressionIdentity> = emptyMap(),
) : CfirPublishedContentSnapshot {
    override fun body(owner: CfirFunctionBodyOwner): CfirBlock? {
        require(owner is CfirCallableDeclaration) { "A body source requires its actual callable owner" }
        return bodies[owner]
    }

    override fun initializer(owner: CfirVariable): CfirExpression? = initializers[owner]
    override fun defaultValue(owner: CfirValueParameter): CfirExpression? = defaults[owner]
    override fun annotations(owner: CfirDeclaration): CfirPublishedAnnotations = when (val selected = annotationContents[owner]) {
        null -> CfirPublishedAnnotations.Unselected
        else -> selected
    }
}

/** 可重入根事务的工作集；失败只丢弃新工作，不更改此前完成的节点或 target。 */
internal class CfirBodyDeserializationState(completed: CfirBodyDeserializationSnapshot = CfirBodyDeserializationSnapshot()) {
    val identities = HashMap(completed.identities)
    val records = HashMap(completed.records)
    val patternIdentities = HashMap(completed.patternIdentities)
    val patternRecords = HashMap(completed.patternRecords)
    val expressions = IdentityHashMap(completed.expressions)
    val patterns = IdentityHashMap(completed.patterns)
    val loops = IdentityHashMap(completed.loops)
    val functions = IdentityHashMap(completed.functions)
    val bodies = IdentityHashMap(completed.bodies)
    val initializers = IdentityHashMap(completed.initializers)
    val defaults = IdentityHashMap(completed.defaults)
    val annotationContents = IdentityHashMap(completed.annotationContents)
    val selectedGraphs = HashMap(completed.selectedGraphs)
    val additionalOrigins = IdentityHashMap(completed.additionalOrigins)
    /** 新注解节点的绑定草稿不向snapshot暴露；metadata完成后从此表移出。 */
    val annotationDrafts = IdentityHashMap<CfirDeclaration, List<org.cangnova.cangjie.cfir.expressions.CfirAnnotation>>()
    /** 只有已经 finish 的同包图能证明池中 record/P 是首次完成产品。 */
    val expressionSources = HashMap(completed.expressionSources)
    val patternSources = HashMap(completed.patternSources)
    val expressionsInProgress = Collections.newSetFromMap(IdentityHashMap<CfirSourceExpressionIdentity, Boolean>())
    val patternsInProgress = Collections.newSetFromMap(IdentityHashMap<CfirSourcePatternIdentity, Boolean>())
    val patternRecordsInProgress = HashSet<Int>()

    /** 完成record身份不变，来源图容器随新alias换代；所有目录必须在同一根提交中重定向。 */
    fun remapSourceGraphs(transform: (CfirSourceGraph) -> CfirSourceGraph) {
        selectedGraphs.replaceAll { _, value -> value.withGraph(transform(value.graph)) }
        expressionSources.replaceAll { _, value -> transform(value) }
        patternSources.replaceAll { _, value -> transform(value) }
    }

    fun snapshot(): CfirBodyDeserializationSnapshot {
        check(expressionsInProgress.isEmpty() && patternsInProgress.isEmpty() && patternRecordsInProgress.isEmpty()) {
            "CJO cannot publish incomplete ordinary expressions or patterns"
        }
        check(annotationDrafts.isEmpty()) { "CJO cannot publish annotation drafts before metadata completion" }
        check(loops.values.all { it.isBound } && functions.values.all { it.isBound }) {
            "CJO cannot publish unbound ordinary control targets"
        }
        check(records.keys == expressionSources.keys && records.all { (reference, record) ->
            expressionSources.getValue(reference).expression(identities.getValue(reference)) === record
        }) { "CJO expression records require their actual first completed graph" }
        check(patternRecords.keys == patternSources.keys && patternRecords.all { (position, pattern) ->
            patternSources.getValue(position).pattern(patternIdentities.getValue(position)) === pattern
        }) { "CJO pattern records require their actual first completed graph" }
        return CfirBodyDeserializationSnapshot(frozen(identities), frozen(records), frozen(patternIdentities), frozen(patternRecords),
            frozenIdentity(expressions), frozenIdentity(patterns), frozenIdentity(loops), frozenIdentity(functions), frozenIdentity(bodies),
            frozenIdentity(initializers), frozen(expressionSources), frozen(patternSources), frozenIdentity(defaults), frozenIdentity(annotationContents), frozen(selectedGraphs), frozenIdentity(additionalOrigins))
    }

    private fun <K, V> frozen(values: Map<K, V>): Map<K, V> = Collections.unmodifiableMap(HashMap(values))
    private fun <K, V> frozenIdentity(values: Map<K, V>): Map<K, V> = Collections.unmodifiableMap(IdentityHashMap(values))
}
