package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemMarker
import org.cangnova.cangjie.resolve.calls.inference.components.InferenceLogger
import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintError
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.InitialConstraint
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker

open class CfirInferenceLogger : InferenceLogger(), CfirSessionComponent {
    sealed class LoggingElement

    data class Call(
        val element: CfirElement,
        val render: String = element::class.simpleName ?: "CfirElement",
    )

    sealed class BlockOwner {
        data class CandidateOwner(val candidate: Candidate) : BlockOwner() {
            val owningCall: Call = Call(candidate.callInfo.callSite)
        }

        data object Unknown : BlockOwner()
    }

    data class BlockElement(
        val name: String,
        val items: MutableList<BlockItemElement> = mutableListOf(),
        val owner: BlockOwner,
    ) : LoggingElement()

    sealed class BlockItemElement : LoggingElement()

    data class LoggedTypeVariable(
        val lookupTag: LoggedLookupTag,
        val original: TypeVariableMarker,
    )

    data class LoggedLookupTag(val name: Name)

    data class ConstraintIssue(
        val message: String,
        val position: String,
    )

    data class NewVariableElement(val variable: LoggedTypeVariable) : BlockItemElement()

    data class ConstraintElement(
        val formatted: String,
        val origins: List<ConstraintElement> = emptyList(),
    ) : BlockItemElement()

    data class ErrorElement(val issue: ConstraintIssue) : BlockItemElement()

    data class FixVariableElement(
        val variable: LoggedTypeVariable,
        val resultType: CangJieTypeMarker,
    ) : BlockItemElement()

    val topLevelElements: MutableList<BlockElement> = mutableListOf()

    private var currentSystem: ConstraintSystemMarker? = null
    private var currentBlock: BlockElement? = null
    private var currentCandidate: BlockOwner.CandidateOwner? = null

    private val systemToKnownBlock: MutableMap<ConstraintSystemMarker, BlockElement> = mutableMapOf()
    private val systemToCandidate: MutableMap<ConstraintSystemMarker, BlockOwner.CandidateOwner> = mutableMapOf()
    private val initialConstraintToKnownElement = mutableMapOf<InitialConstraint, ConstraintElement>()
    private val variableConstraintToKnownElement = mutableMapOf<Pair<TypeVariableMarker, Constraint>, ConstraintElement>()

    private var origins: List<ConstraintElement> = emptyList()

    fun logCandidate(candidate: Candidate) {
        if (currentCandidate?.candidate === candidate) return
        val owner = BlockOwner.CandidateOwner(candidate)
        systemToCandidate[candidate.system] = owner
        currentCandidate = owner
        currentSystem = candidate.system
    }

    fun logStage(name: String, system: ConstraintSystemMarker) {
        updateCurrentSystem(system)
        currentBlock = registerBlock(BlockElement(name, owner = currentCandidate ?: BlockOwner.Unknown), system)
    }

    override fun logInitial(initialConstraint: InitialConstraint, context: Any?) {
        val system = context as? ConstraintSystemMarker ?: return
        val block = prepareProperBlock(system)
        val element = ConstraintElement(
            formatted = formatInitialConstraint(initialConstraint),
        )
        initialConstraintToKnownElement.putIfAbsent(initialConstraint, element)
        block.items += element
    }

    override fun log(variable: TypeVariableMarker, constraint: Constraint, context: Any?) {
        val system = context as? ConstraintSystemMarker ?: return
        val block = prepareProperBlock(system)
        val element = ConstraintElement(
            formatted = formatVariableConstraint(variable, constraint),
            origins = origins,
        )
        variableConstraintToKnownElement.putIfAbsent(variable to constraint, element)
        block.items += element
    }

    override fun logError(error: ConstraintSystemError, context: Any?) {
        val system = context as? ConstraintSystemMarker ?: return
        prepareProperBlock(system).items += ErrorElement(
            ConstraintIssue(
                message = error.toString(),
                position = extractErrorPosition(error),
            )
        )
    }

    override fun logNewVariable(variable: TypeVariableMarker, context: Any?) {
        val system = context as? ConstraintSystemMarker ?: return
        prepareProperBlock(system).items += NewVariableElement(variable.toLoggedTypeVariable())
    }

    override fun logReadiness(record: FixationLogRecord, context: Any?) {
        // Readiness records are intentionally not rendered yet by the current local handler.
        // Keep the callback as a no-op until Task 5/7 converges richer block item rendering.
    }

    override fun logFixVariable(variable: TypeVariableMarker, resultType: CangJieTypeMarker, context: Any?) {
        val system = context as? ConstraintSystemMarker ?: return
        prepareProperBlock(system).items += FixVariableElement(variable.toLoggedTypeVariable(), resultType)
    }

    override fun <T> withOrigin(origin: Any?, action: () -> T): T {
        val initialConstraint = origin as? InitialConstraint ?: return action()
        val element = initialConstraintToKnownElement[initialConstraint] ?: return action()
        return withOriginatingElements(listOf(element), action)
    }

    override fun <T> withOrigins(
        firstOwner: Any?,
        firstOrigin: Any?,
        secondOwner: Any?,
        secondOrigin: Any?,
        action: () -> T,
    ): T {
        val firstVariable = firstOwner as? TypeVariableMarker
        val firstConstraint = firstOrigin as? Constraint
        val secondVariable = secondOwner as? TypeVariableMarker
        val secondConstraint = secondOrigin as? Constraint

        val firstElement = firstVariable?.let { variableConstraintToKnownElement[it to firstConstraint] }
        val secondElement = secondVariable?.let { variableConstraintToKnownElement[it to secondConstraint] }
        val elements = listOfNotNull(firstElement, secondElement)
        if (elements.isEmpty()) return action()
        return withOriginatingElements(elements, action)
    }

    private fun prepareProperBlock(system: ConstraintSystemMarker): BlockElement {
        if (system == currentSystem) {
            return currentBlock ?: registerBlock(BlockElement("Unknown system", owner = currentCandidate ?: BlockOwner.Unknown), system)
        }

        updateCurrentSystem(system)
        val knownPreviousBlock = systemToKnownBlock[system]
        val nextBlockTitle = when {
            knownPreviousBlock != null && currentCandidate != null -> "Continue ${knownPreviousBlock.name}"
            knownPreviousBlock != null -> "Continue ${knownPreviousBlock.name}"
            else -> "Unknown system"
        }
        return registerBlock(BlockElement(nextBlockTitle, owner = currentCandidate ?: BlockOwner.Unknown), system)
    }

    private fun updateCurrentSystem(system: ConstraintSystemMarker) {
        currentSystem = system
        currentCandidate = systemToCandidate[system]
    }

    private fun registerBlock(block: BlockElement, system: ConstraintSystemMarker): BlockElement {
        if (topLevelElements.lastOrNull()?.items?.isEmpty() == true) {
            topLevelElements.removeLast()
        }
        systemToKnownBlock[system] = block
        topLevelElements += block
        currentBlock = block
        currentSystem = system
        return block
    }

    private inline fun <T> withOriginatingElements(elements: List<ConstraintElement>, action: () -> T): T {
        val previous = origins
        return try {
            origins = elements
            action()
        } finally {
            origins = previous
        }
    }

    private fun TypeVariableMarker.toLoggedTypeVariable(): LoggedTypeVariable {
        val debugName = toString().removePrefix("ConeTypeVariableType(").removeSuffix(")")
        return LoggedTypeVariable(LoggedLookupTag(Name.identifier(debugName)), this)
    }

    private fun formatInitialConstraint(constraint: InitialConstraint): String = constraint.asStringWithoutPosition()

    private fun formatVariableConstraint(variable: TypeVariableMarker, constraint: Constraint): String {
        val variableName = variable.toLoggedTypeVariable().lookupTag.name
        return when (constraint.kind) {
            ConstraintKind.LOWER -> "${constraint.type} <: $variableName"
            ConstraintKind.UPPER -> "$variableName <: ${constraint.type}"
            ConstraintKind.EQUALITY -> "$variableName == ${constraint.type}"
        }
    }

    private fun extractErrorPosition(error: ConstraintSystemError): String = when (error) {
        is ConstraintError -> error.position.toString()
        else -> error::class.simpleName ?: "ConstraintSystemError"
    }
}
