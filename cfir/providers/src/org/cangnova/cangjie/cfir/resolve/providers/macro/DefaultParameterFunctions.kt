package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.copy.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.resolve.substitution.AbstractConeSubstitutor
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CjParsingBackend
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 对位官方 PrepareTypeCheck/AddDefaultFunction：最终 construction 图在 metadata seal 前
 * 创建独立 helper。lazy 正文只建立稳定声明，随后由原函数的正式重建事务物化。
 */
fun prepareDefaultParameterFunctions(
    file: CfirFile,
    metadata: CfirAnnotationMetadataBuilder,
    backend: CjParsingBackend,
): Unit = metadata.withOpenTransaction {
    prepareDefaultParameterFunctionsInGraph(file, metadata, backend)
}

/** 正常 lazy 重建中新出现的局部声明，也在本次开放事务内完成 helper 准备。 */
fun prepareDefaultParameterFunctionsInRebuiltBody(
    body: CfirElement,
    metadata: CfirAnnotationMetadataBuilder,
    backend: CjParsingBackend,
): Unit = metadata.withOpenTransaction { prepareDefaultParameterFunctionsInGraph(body, metadata, backend) }

@OptIn(CfirImplementationDetail::class)
private fun prepareDefaultParameterFunctionsInGraph(
    root: CfirElement,
    metadata: CfirAnnotationMetadataBuilder,
    backend: CjParsingBackend,
) {
    val seen = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
    val visitor = object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (!seen.add(element)) return
            if (element is CfirFunction) prepareFunction(element, metadata, backend)
            // Lazy 实现的 ordinary children 为空；这里不读取其尚未物化的 source/body getter。
            element.acceptChildren(this)
            if (element is CfirValueParameter) element.defaultValueMetadata?.function?.accept(this)
        }
    }
    root.accept(visitor)
}

private fun prepareFunction(owner: CfirFunction, metadata: CfirAnnotationMetadataBuilder, backend: CjParsingBackend) {
    if (owner is CfirAnonymousFunction || owner is CfirMacroDeclaration) return
    // CFIR 的 body 是 Block；参数表单独存在，不能用无 Block 判定原生 FuncBody 不存在。
    if (owner.exportFacts.isImported) return
    if (owner.status.isOperator || owner.status.isOpen || owner.status.isAbstract || owner.status.isDefault) return
    owner.valueParameters.forEachIndexed { index, parameter ->
        val assignment = parameter.defaultValue ?: return@forEachIndexed
        if (parameter.defaultValueMetadata?.hasInitial == true && parameter.defaultValueMetadata?.function == null) {
            return@forEachIndexed
        }
        parameter.defaultValueMetadata?.function?.let {
            check(it.defaultParameterHelperInfo == CfirDefaultParameterHelperInfo(owner, index))
            return@forEachIndexed
        }
        check(owner.resolvePhase == CfirResolvePhase.RAW_CFIR) { "Default helpers must be prepared before binding" }
        val name = Name.identifier(parameter.name.asString() + "." + index)
        val ownerId = owner.symbol.callableId
        val native = backend == CjParsingBackend.CJNATIVE
        val assignmentSource = checkNotNull(parameter.defaultAssignmentSyntaxFacts?.source) {
            "SOURCE helper preparation requires the original assignment source"
        }
        val previous = owner.valueParameters.take(index)
        val ownParameters = if (native && owner is CfirNamedFunction) owner.typeParameters else emptyList()
        val prototypeSymbol = CfirNamedFunctionSymbol(CallableId(ownerId.packageName, ownerId.className, name))
        val finalSymbol = CfirNamedFunctionSymbol(prototypeSymbol.callableId)
        val returnTarget = CfirFunctionTarget(isLambda = false)
        val prototype = buildNamedFunction {
            source = assignmentSource
            moduleData = owner.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Synthetic.DefaultParameterHelper
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = owner.isLocal
            // helper 与原函数具有同一个真实实例宿主；构造器/STATIC helper 不拥有 dispatch receiver。
            dispatchReceiverType = if (owner is CfirConstructor || owner.status.isStatic) null else owner.dispatchReceiverType
            status = CfirDeclarationStatusImpl(if (native) Visibilities.Private else Visibilities.Public, Modality.FINAL).apply {
                isStatic = owner is CfirConstructor || owner.status.isStatic
                isMut = owner.status.isMut
            }
            returnTypeRef = parameter.returnTypeRef
            symbol = prototypeSymbol
            this.name = name
            isMut = owner.status.isMut
            typeParameters.addAll(ownParameters)
            previous.forEach { original ->
                valueParameters += buildValueParameter {
                    source = original.source; moduleData = owner.moduleData; resolvePhase = CfirResolvePhase.RAW_CFIR
                    origin = CfirDeclarationOrigin.Synthetic.DefaultParameterHelper; attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = true; containingDeclarationSymbol = prototypeSymbol
                    symbol = CfirValueParameterSymbol(CallableId(Name.identifier(original.name.asString())))
                    this.name = original.name; isNamed = false; isTypeImplicit = original.isTypeImplicit
                    returnTypeRef = original.returnTypeRef
                }.also { it.exportFacts = CfirDeclarationExportFacts(isCompilerAdded = true, sourceFactsPublished = true) }
            }
            if (assignment !is CfirLazyExpression) body = buildBlock {
                source = assignmentSource
                statements += buildReturnExpression { source = assignmentSource; target = returnTarget; result = assignment }
            }
        }
        returnTarget.bind(prototype)
        prototype.defaultParameterHelperInfo = CfirDefaultParameterHelperInfo(owner, index)
        prototype.interopInfo = CfirInteropInfo.NONE
        prototype.exportFacts = CfirDeclarationExportFacts(isCompilerAdded = true, isDefaultParameterHelper = true,
            hasInitial = true, isImplicitlyAdded = true, hasNoReflectInfo = true, sourceFactsPublished = true)
        val symbols = IdentityHashMap<CfirBasedSymbol<*>, CfirBasedSymbol<*>>()
        if (owner.symbol is CfirNamedFunctionSymbol) symbols[owner.symbol] = finalSymbol
        symbols[prototype.symbol] = finalSymbol
        previous.zip(prototype.valueParameters).forEach { (old, parameterPrototype) ->
            val fresh = CfirValueParameterSymbol(parameterPrototype.symbol.callableId)
            symbols[old.symbol] = fresh
            symbols[parameterPrototype.symbol] = fresh
        }
        ownParameters.forEach { symbols[it.symbol] = CfirTypeParameterSymbol() }
        val names = genericNames(ownParameters)
        val copy = copyHelperGraphs(listOf(prototype), symbols, names, metadata, owner.moduleData.session, ownParameters)
        val helper = copy.getCopied(prototype)
        if (ownParameters.isNotEmpty()) helper.hasGenericNode = true
        if (assignment is CfirLazyExpression) helper.replaceBody(buildLazyBlock())
        parameter.defaultValueMetadata = CfirParameterDefaultValueMetadata(true, helper)
        if (owner.status.isStatic) propagateStatic(helper)
        if (assignment !is CfirLazyExpression) markOriginalAssignment(assignment, owner.status.isStatic)
    }
}

/** 在原函数正常 RAW 重建的开放事务中生成正文；保留先前已发布 helper 的 symbol/header。 */
fun materializeDefaultParameterHelperBodies(
    owner: CfirFunction,
    rebuilt: CfirFunction,
    metadata: CfirAnnotationMetadataBuilder,
    backend: CjParsingBackend,
): List<Pair<CfirNamedFunction, CfirBlock>> = metadata.withOpenTransaction {
    require(owner.valueParameters.size == rebuilt.valueParameters.size)
    owner.valueParameters.zip(rebuilt.valueParameters).mapIndexedNotNull { index, (parameter, recreated) ->
        val helper = parameter.defaultValueMetadata?.function ?: return@mapIndexedNotNull null
        if (helper.body !is CfirLazyBlock) return@mapIndexedNotNull null
        check(helper.defaultParameterHelperInfo == CfirDefaultParameterHelperInfo(owner, index))
        val assignment = checkNotNull(recreated.defaultValue)
        check(assignment !is CfirLazyExpression) { "Normal owner reconstruction must materialize its default assignment" }
        val symbols = IdentityHashMap<CfirBasedSymbol<*>, CfirBasedSymbol<*>>()
        if (owner.symbol is CfirNamedFunctionSymbol) symbols[owner.symbol] = helper.symbol
        if (rebuilt.symbol is CfirNamedFunctionSymbol) symbols[rebuilt.symbol] = helper.symbol
        owner.valueParameters.take(index).zip(helper.valueParameters).forEach { (old, fresh) -> symbols[old.symbol] = fresh.symbol }
        rebuilt.valueParameters.take(index).zip(helper.valueParameters).forEach { (old, fresh) -> symbols[old.symbol] = fresh.symbol }
        val originals = if (backend == CjParsingBackend.CJNATIVE && owner is CfirNamedFunction) owner.typeParameters else emptyList()
        originals.zip(helper.typeParameters).forEach { (old, fresh) -> symbols[old.symbol] = fresh.symbol }
        if (rebuilt is CfirNamedFunction) rebuilt.typeParameters.zip(helper.typeParameters).forEach { (old, fresh) -> symbols[old.symbol] = fresh.symbol }
        val copy = copyHelperGraphs(listOf(assignment), symbols, genericNames(originals), metadata, owner.moduleData.session, emptyList())
        val body = helperBody(helper, copy.getCopied(assignment))
        markOriginalAssignment(assignment, owner.status.isStatic)
        if (owner.status.isStatic) propagateStatic(body)
        prepareDefaultParameterFunctionsInGraph(body, metadata, backend)
        helper to body
    }
}

/** 原生 walkOrigin/MarkParamWithInitialValue：原 assignment 留作诊断，不再为其内部参数创建 helper。 */
private fun markOriginalAssignment(root: CfirElement, isStatic: Boolean) {
    val seen = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
    root.accept(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (!seen.add(element)) return
            if (element is CfirFunction && element !is CfirAnonymousFunction && element !is CfirMacroDeclaration && isStatic) {
                element.replaceStatus(cloneDeclarationStatus(element.status).also { it.isStatic = true })
            }
            if (element is CfirValueParameter && element.defaultValue != null) {
                element.defaultValueMetadata = element.defaultValueMetadata?.copy(hasInitial = true)
                    ?: CfirParameterDefaultValueMetadata(true, null)
            }
            element.acceptChildren(this)
        }
    })
}

/** 原生 walkFunc 的 STATIC 传播发生在生成图中，保留独立 status 容器。 */
private fun propagateStatic(root: CfirElement) {
    val seen = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
    root.accept(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (!seen.add(element)) return
            if (element is CfirFunction && element !is CfirAnonymousFunction && element !is CfirMacroDeclaration) {
                element.replaceStatus(cloneDeclarationStatus(element.status).also { it.isStatic = true })
            }
            element.acceptChildren(this)
        }
    })
}

private fun genericNames(parameters: List<CfirTypeParameter>): Map<Name, Name> = parameters.mapIndexed { index, parameter ->
    parameter.name to Name.identifier(parameter.name.asString() + "$" + index)
}.toMap()

@OptIn(CfirImplementationDetail::class)
private fun copyHelperGraphs(
    roots: List<CfirElement>,
    symbols: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>>,
    genericNames: Map<Name, Name>,
    metadata: CfirAnnotationMetadataBuilder,
    session: org.cangnova.cangjie.cfir.session.CfirSession,
    ownParameters: List<CfirTypeParameter>,
): CfirOwnedGraphCopyResult {
    val names = IdentityHashMap<CfirElement, Name>()
    val targets = IdentityHashMap<CfirTarget<*>, CfirTarget<*>>()
    val seen = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
    ownParameters.forEachIndexed { index, parameter ->
        names[parameter] = Name.identifier(parameter.name.asString() + "$" + index)
    }
    val visitor = object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (!seen.add(element)) return
            if (element is CfirUserTypeRef) element.qualifier.firstOrNull()?.let { part ->
                genericNames[part.name]?.let { names[part] = it }
            }
            if (element is CfirQualifiedAccessExpression && element.explicitReceiver == null) {
                val reference = element.calleeReference as? CfirNamedReference
                reference?.let { genericNames[it.name]?.let { name -> names[it] = name } }
            }
            if (element is CfirReturnExpression) {
                val old = element.target
                if (old.isBound && symbols[old.labeledElement.symbol] is CfirNamedFunctionSymbol) {
                    check(old is CfirFunctionTarget) { "Return targets require the function target contract" }
                    targets.getOrPut(old) { CfirFunctionTarget(old.labelName, old.isLambda).also { target ->
                        val symbol = symbols.getValue(old.labeledElement.symbol) as CfirNamedFunctionSymbol
                        if (symbol.isBound) target.bind(symbol.cfir)
                    } }
                }
            }
            if (element is CfirDeclaration) element.attributes.typeConstraintDiagnosticData?.typeConstraints?.forEach {
                it.boundTypeRefs.forEach { ref -> ref.accept(this) }
            }
            element.acceptSourceChildren(this)
        }
    }
    roots.forEach { it.accept(visitor) }
    val remapper = CfirTypeRefRemapper { type, context ->
        object : AbstractConeSubstitutor(session.typeContext) {
            override fun substituteType(type: ConeCangJieType): ConeCangJieType? {
                if (type !is ConeTypeParameterType) return null
                val old = type.lookupTag.typeParameterSymbol
                val fresh = context.remapSymbol(old)
                return if (old === fresh) null else ConeTypeParameterTypeImpl(fresh.toLookupTag(), type.attributes)
            }
        }.substituteOrSelf(type)
    }
    val attributes = object : CfirOwnedAttributeRemapper {
        override fun collectOwnedChildren(key: String, value: Any, visit: (CfirElement) -> Unit): Unit =
            error("SOURCE attribute has no declared ownership protocol: $key")
        override fun copyAttribute(key: String, value: Any, context: CfirOwnedGraphCopyContext): Any =
            error("SOURCE attribute has no declared copy protocol: $key")
    }
    return deepCopyOwnedGraphs(roots, remapper, attributes, symbols, targets, names).also { copy ->
        copy.forEachCopied { old, fresh ->
            if (old is CfirDeclaration && fresh is CfirDeclaration) old.annotations.forEachIndexed { index, annotation ->
                if (annotation is CfirAnnotationCall) metadata.recordDerivedSlot(annotation, fresh, index, copy.getCopied(annotation))
            }
        }
    }
}

private fun helperBody(helper: CfirNamedFunction, assignment: CfirExpression): CfirBlock {
    val target = CfirFunctionTarget(isLambda = false).also { it.bind(helper) }
    return buildBlock {
        source = helper.source
        statements += buildReturnExpression { source = helper.source; this.target = target; result = assignment }
    }
}
