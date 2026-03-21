package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.semantics.CfirConstraintSystemError
import org.cangnova.cangjie.cfir.semantics.CfirConstraintSystemMarker
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.types.ConeCangJieType

open class FirInferenceLogger : InferenceLogger(), CfirSessionComponent {
    sealed class LoggingElement

    class Call(
        val fir: CfirElement,
        val render: String = CfirRenderer.render(fir),
    )

    sealed class BlockOwner {
        class Candidate(
            val candidate: CfirCandidate,
        ) : BlockOwner() {
            val owningCall: Call = Call(candidate.callInfo.callSite)
        }

        /**
         * A fallback for cases where the constraints system is used
         * without a preceding candidate log call.
         */
        data object Unknown : BlockOwner()
    }

    class BlockElement(
        val name: String,
        val items: MutableList<BlockItemElement> = mutableListOf(),
        val owner: BlockOwner,
    ) : LoggingElement()

    sealed class BlockItemElement : LoggingElement()

    class NewVariableElement(val variable: CfirTypeVariable) : BlockItemElement()

    class ErrorElement(val error: CfirConstraintSystemError) : BlockItemElement()

    sealed class ConstraintElement(
        val origins: List<ConstraintElement>,
    ) : BlockItemElement()

    class InitialConstraintElement(val constraint: String, val position: String) : ConstraintElement(emptyList())

    class VariableConstraintElement(
        val constraint: String,
        origins: List<ConstraintElement>,
    ) : ConstraintElement(origins)

    class FixationLogRecordElement(val record: FixationLogRecord) : BlockItemElement()

    val topLevelElements: MutableList<BlockElement> = mutableListOf()

    private var currentSystem: CfirConstraintSystemMarker? = null

    private lateinit var currentBlock: BlockElement

    private val systemToKnownBlock: MutableMap<CfirConstraintSystemMarker, BlockElement> = mutableMapOf()
    private val systemToCandidate: MutableMap<CfirConstraintSystemMarker, BlockOwner.Candidate> = mutableMapOf()

    private fun prepareProperBlock(system: CfirConstraintSystemMarker) {
        if (system == currentSystem) return
        updateCurrentSystem(system)

        val knownPreviousBlock = systemToKnownBlock[system]
        val nextBlockTitle = when {
            knownPreviousBlock != null && currentCandidate != null -> "Continue " + knownPreviousBlock.name
            else -> error("UNKNOWN SYSTEM")
        }
        currentBlock = BlockElement(nextBlockTitle, owner = currentCandidate ?: BlockOwner.Unknown).apply { register(system) }
    }

    private fun updateCurrentSystem(system: CfirConstraintSystemMarker) {
        if (system == currentSystem) return
        currentSystem = system
        currentCandidate = systemToCandidate[system]
    }

    private fun BlockElement.register(system: CfirConstraintSystemMarker) {
        if (topLevelElements.lastOrNull()?.items?.isEmpty() == true) {
            topLevelElements.removeLast()
        }

        systemToKnownBlock[system] = this
        topLevelElements += this
    }

    private val initialConstraintToKnownElement = mutableMapOf<CfirInitialConstraint, ConstraintElement>()
    private val variableConstraintToKnownElement = mutableMapOf<Pair<CfirTypeVariable, CfirVariableConstraint>, ConstraintElement>()

    private fun cachedElementFor(constraint: CfirInitialConstraint) =
        initialConstraintToKnownElement[constraint]
            ?: error("This constraint has not yet been logged: $constraint")

    private fun cachedElementFor(variable: CfirTypeVariable, constraint: CfirVariableConstraint) =
        variableConstraintToKnownElement[variable to constraint]
            ?: error("This constraint has not yet been logged: $variable with $constraint")

    private var currentCandidate: BlockOwner.Candidate? = null

    fun logCandidate(candidate: CfirCandidate) {
        val system = candidate.constraintSystem ?: return

        if (currentCandidate?.candidate !== candidate) {
            val candidateOwner = BlockOwner.Candidate(candidate)
            systemToCandidate[system] = candidateOwner
            currentCandidate = candidateOwner
            currentSystem = system
        }
    }

    fun logStage(name: String, system: CfirConstraintSystemMarker) {
        updateCurrentSystem(system)
        currentBlock = BlockElement(name, owner = currentCandidate ?: BlockOwner.Unknown).apply { register(system) }
    }

    private val currentBlockItemElements: MutableList<BlockItemElement>
        get() = currentBlock.items

    override fun logInitial(constraint: CfirInitialConstraint, system: CfirConstraintSystemMarker) {
        prepareProperBlock(system)
        val element = InitialConstraintElement(formatConstraint(constraint), sanitizeFqNames(constraint.position.toString()))
        initialConstraintToKnownElement.putIfAbsent(constraint, element)
        currentBlockItemElements += element
    }

    override fun log(variable: CfirTypeVariable, constraint: CfirVariableConstraint, system: CfirConstraintSystemMarker) {
        prepareProperBlock(system)
        val element = VariableConstraintElement(formatConstraint(variable, constraint), origins)
        variableConstraintToKnownElement.putIfAbsent(variable to constraint, element)
        currentBlockItemElements += element
    }

    override fun logError(error: CfirConstraintSystemError, system: CfirConstraintSystemMarker) {
        prepareProperBlock(system)
        currentBlockItemElements.add(ErrorElement(error))
    }

    override fun logNewVariable(variable: CfirTypeVariable, system: CfirConstraintSystemMarker) {
        prepareProperBlock(system)
        currentBlockItemElements.add(NewVariableElement(variable))
    }

    override fun logReadiness(
        fixationLog: FixationLogRecord,
        system: CfirConstraintSystemMarker,
    ) {
        prepareProperBlock(system)
        val fixationLogs = currentBlockItemElements.mapNotNull { (it as? FixationLogRecordElement)?.record }

        if (fixationLogs.isEmpty() || !fixationLogs.last().isSimilarTo(fixationLog)) {
            currentBlockItemElements.add(FixationLogRecordElement(fixationLog))
        }
    }

    private fun FixationLogRecord.isSimilarTo(record: FixationLogRecord): Boolean {
        if (record.chosen !== chosen) return false
        if (record.map.size != map.size) return false
        for ((variable, info) in record.map) {
            if (!info.isSimilarTo(map[variable])) return false
        }
        return true
    }

    private fun FixationLogVariableInfo<*>.isSimilarTo(info: FixationLogVariableInfo<*>?): Boolean {
        if (info == null) return false
        if (readiness != info.readiness) return false
        if (constraints.size != info.constraints.size) return false
        for (i in 0 until constraints.size) {
            if (constraints[i] !== info.constraints[i]) return false
        }
        return true
    }

    private val FixationLogVariableInfo<*>.isForbiddenReadiness: Boolean
        get() = when (val readiness = readiness) {
            is CfirFixationReadiness -> !readiness.allowsFixation
            else -> error("Unexpected readiness type: ${readiness::class}")
        }

    var origins: List<ConstraintElement> = listOf()

    private inline fun <T> withOriginatingElements(elements: List<ConstraintElement>, block: () -> T): T {
        val oldOrigins = origins
        return try {
            origins = elements
            block()
        } finally {
            origins = oldOrigins
        }
    }

    override fun <T> withOrigin(constraint: CfirInitialConstraint, block: () -> T): T =
        withOriginatingElements(listOf(cachedElementFor(constraint)), block)

    override fun <T> withOrigins(
        variable1: CfirTypeVariable,
        constraint1: CfirVariableConstraint,
        variable2: CfirTypeVariable,
        constraint2: CfirVariableConstraint,
        block: () -> T,
    ): T {
        val elements = listOf(
            cachedElementFor(variable1, constraint1),
            cachedElementFor(variable2, constraint2),
        )
        return withOriginatingElements(elements, block)
    }

    override fun logFixVariable(
        variable: CfirTypeVariable,
        resultType: ConeCangJieType,
        system: CfirConstraintSystemMarker,
    ) {
        prepareProperBlock(system)

        val relevantBlocks = topLevelElements.filter { (it.owner as? BlockOwner.Candidate)?.candidate?.constraintSystem == system }

        for (block in relevantBlocks) {
            for (element in block.items) {
                if (element !is FixationLogRecordElement) continue
                val log = element.record
                if (log.chosen !== variable) continue
                if (log.map[variable]?.isForbiddenReadiness == true) continue
                log.fixedTo = resultType
            }
        }
    }

    companion object {
        @JvmStatic
        protected fun formatConstraint(constraint: CfirInitialConstraint): String {
            return when (constraint.constraintKind) {
                CfirConstraintKind.UPPER -> "${constraint.a} <: ${constraint.b}"
                CfirConstraintKind.LOWER -> "${constraint.b} <: ${constraint.a}"
                CfirConstraintKind.EQUALITY -> "${constraint.a} == ${constraint.b}"
            }
        }

        @JvmStatic
        protected fun formatConstraint(variable: CfirTypeVariable, constraint: CfirVariableConstraint): String {
            return when (constraint.kind) {
                CfirConstraintKind.LOWER -> "${constraint.type} <: $variable"
                CfirConstraintKind.UPPER -> "$variable <: ${constraint.type}"
                CfirConstraintKind.EQUALITY -> "$variable == ${constraint.type}"
            }
        }

        private val fqNameRegex = """(?:\w+\.)*(\w+)@\w+""".toRegex()

        @JvmStatic
        fun sanitizeFqNames(string: String): String = string.replace(fqNameRegex, "$1")
    }
}

val CfirSession.inferenceLogger: FirInferenceLogger? by CfirSession.nullableSessionComponentAccessor<FirInferenceLogger>()


