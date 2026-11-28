package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.*
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Implementation
import org.cangnova.cangjie.cfir.tree.generator.model.ListField
import org.cangnova.cangjie.cfir.tree.generator.util.generatedType
import org.cangnova.cangjie.cfir.tree.generator.util.getMutableType
import org.cangnova.cangjie.cfir.tree.generator.util.type
import org.cangnova.cangjie.generators.tree.*
import org.cangnova.cangjie.generators.tree.imports.ImportCollecting
import org.cangnova.cangjie.generators.tree.printer.*
import org.cangnova.cangjie.utils.withIndent

/**
 * 按全部 concrete implementation 生成 RAW copier。
 *
 * 与 Kotlin IR printer 一样使用 AbstractVisitorPrinter/fieldsInConstructor；
 * CFIR 的多实现通过 exact concrete class 分派，非 child Element 也经过同一缓存。
 */
internal class DeepCopyCfirTreeWithSymbolsPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(printer) {
    override val visitorTypeParameters: List<TypeVariable> = emptyList()
    override val visitorDataType: TypeRef = StandardTypes.nothing.copy(nullable = true)
    override fun visitMethodReturnType(element: Element): TypeRef = CfirTree.rootElement
    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>> =
        listOf(cfirVisitorType.withArgs(cfirElementType, StandardTypes.nothing.copy(nullable = true)))
    override val implementationKind: ImplementationKind = ImplementationKind.FinalClass
    override val optIns: List<ClassRef<*>> = listOf(cfirImplementationDetailType)
    override val constructorParameters: List<PrimaryConstructorParameter> = listOf(
        PrimaryConstructorParameter(FunctionParameter("context", cfirRawCopyContextType), VariableKind.VAL, Visibility.PRIVATE),
    )
    override val ImportCollecting.classKDoc: String
        get() = "RAW initializer 深复制；未知运行时实现和缺失字段策略明确失败。metadata hooks 与树节点共享 source2clone。"

    private lateinit var implementations: List<Implementation>

    private data class Hook(val method: String, val kind: String)

    /** 类型策略是 metadata schema，不是节点覆盖白名单。未知类型在生成阶段拒绝。 */
    private val hooks: Map<String, Hook> = buildMap {
        fun register(packageName: String, name: String, method: String, kind: String) {
            check(put("$packageName.$name", Hook(method, kind)) == null)
        }
        val base = BASE_PACKAGE
        register("org.cangnova.cangjie.source", "CjSourceElement", "source", "SOURCE")
        register("org.cangnova.cangjie", "CjSourceFile", "sourceFile", "SOURCE_FILE")
        register("org.cangnova.cangjie.source", "CjSourceFileLinesMapping", "lineMapping", "LINE_MAPPING")
        register("$base.common", "CfirModuleData", "moduleData", "MODULE_DATA")
        register("org.cangnova.cangjie.name", "Name", "name", "NAME")
        register("org.cangnova.cangjie.name", "FqName", "fqName", "FQ_NAME")
        register("org.cangnova.cangjie.name", "ClassId", "classId", "CLASS_ID")
        register("org.cangnova.cangjie.descriptors", "Visibility", "visibility", "VISIBILITY")
        register("org.cangnova.cangjie.descriptors", "Modality", "modality", "MODALITY")
        register("$base.declarations", "CfirDeclarationOrigin", "origin", "ORIGIN")
        register("$base.declarations", "CfirDeclarationStatus", "status", "STATUS")
        register("$base.declarations", "CfirResolvedDeclarationStatus", "status", "STATUS")
        register("$base.declarations", "CfirDeclarationAttributes", "attributes", "ATTRIBUTES")
        register("$base.declarations", "DeprecationsProvider", "deprecationsProvider", "DEPRECATIONS")
        register("$base.scopes", "CfirScopeProvider", "scopeProvider", "SCOPE_PROVIDER")
        register("$base.types", "ConeCangJieType", "type", "CONE_TYPE")
        register("$base.types", "ConeSimpleCangJieType", "type", "CONE_TYPE")
        register("$base.types", "ConeDiagnostic", "diagnostic", "DIAGNOSTIC")
        register(base, "CfirTarget", "target", "TARGET")
        register(base, "CfirContentSource", "contentSource", "CONTENT_SOURCE")
        register("$base.expressions", "CfirConstantValue", "patternConstant", "PATTERN_CONSTANT")
        register("$base.patterns", "CfirPatternTreeOwner", "patternOwner", "PATTERN_OWNER")
        register("$base.expressions", "CfirAnnotationArgumentMapping", "annotationMapping", "ANNOTATION_MAPPING")
        register("$base.expressions", "CfirAnnotationArgumentView", "annotationView", "ANNOTATION_VIEW")
        register("$base.session", "CfirConditionalCompilationFailure", "conditionalCompilationFailure", "CONDITIONAL_COMPILATION_FAILURE")
        register("kotlin", "Any", "literalValue", "LITERAL_VALUE")
        register("$base.expressions", "CfirMatchExhaustivenessStatus", "matchExhaustiveness", "MATCH_EXHAUSTIVENESS")
        register("$base.declarations", "CfirPropertyBodyResolveState", "propertyBodyState", "PROPERTY_BODY_STATE")
        register("$base.patterns", "CfirTypePatternMatchingKind", "typePatternKind", "TYPE_PATTERN_KIND")
        register("$base.expressions", "CfirAssignmentTypeMismatchOutcome", "assignmentMismatch", "ASSIGNMENT_MISMATCH")
        register("org.cangnova.cangjie.annotations", "CangjieAnnotationIdentity", "annotationIdentity", "ANNOTATION_IDENTITY")
    }

    private val immutableTypes: Set<String> = setOf(
        "kotlin.String", "kotlin.Boolean", "kotlin.Int", "kotlin.Long", "kotlin.Short", "kotlin.Byte",
        "kotlin.Char", "kotlin.Float", "kotlin.Double", "kotlin.UInt", "kotlin.ULong", "kotlin.UShort", "kotlin.UByte",
        "$BASE_PACKAGE.expressions.CfirLiteralKind", "$BASE_PACKAGE.expressions.CfirBinaryOpKind",
        "$BASE_PACKAGE.expressions.CfirComparisonOp", "$BASE_PACKAGE.expressions.CfirTypeOperationKind",
        "$BASE_PACKAGE.expressions.CfirFunctionCallOrigin", "$BASE_PACKAGE.expressions.CfirAnnotationResolveState",
        "$BASE_PACKAGE.declarations.CfirModifierExplicitness", "$BASE_PACKAGE.declarations.CfirBodyPresence",
        "$BASE_PACKAGE.source.CfirSourceOperator", "$BASE_PACKAGE.source.CfirSourceCallKind",
        "$BASE_PACKAGE.source.CfirSourceForInKind", "$BASE_PACKAGE.source.CfirSourceStringKind",
        "$BASE_PACKAGE.types.CfirTypeRefOrigin", "$BASE_PACKAGE.types.BuiltinPrimitiveOperatorSignature",
        "org.cangnova.cangjie.annotations.CangjieOverflowStrategy",
        "org.cangnova.cangjie.annotations.BuiltInAnnotationKind",
        "org.cangnova.cangjie.annotations.CangjieAnnotationOrigin", "org.cangnova.cangjie.annotations.CangjieAnnotationTarget",
    )

    /** 同一 CFIR Element 有时在模型中用普通 ClassRef 表达，仍必须进入唯一节点缓存。 */
    private val elementHookTypes = setOf(
        "$BASE_PACKAGE.declarations.CfirDeclarationStatus",
        "$BASE_PACKAGE.declarations.CfirResolvedDeclarationStatus",
        "$BASE_PACKAGE.expressions.CfirAnnotationArgumentMapping",
    )

    override fun printVisitor(elements: List<Element>) {
        implementations = elements.flatMap { it.implementations }.filter {
            when (it.kind ?: error("Implementation kind was not configured: $it")) {
                ImplementationKind.FinalClass, ImplementationKind.OpenClass, ImplementationKind.Object -> true
                ImplementationKind.Interface, ImplementationKind.SealedInterface,
                ImplementationKind.AbstractClass, ImplementationKind.SealedClass -> false
            }
        }
        check(implementations.map { it.packageName + "." + it.typeName }.toSet().size == implementations.size) {
            "Duplicate concrete implementation in RAW copier model"
        }
        implementations.forEach(::validateImplementation)
        super.printVisitor(elements)
    }

    /** 普通 visitor 的父分派最终到 root；ExpressionStub 因而也能进入完整 concrete 分派。 */
    override fun printMethodsForElement(element: Element) {
        if (element != CfirTree.rootElement) return
        printer.run {
            println()
            printVisitMethodDeclaration(element, override = true)
            println(" = copyElement(element)")
        }
    }

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        println("init { context.state.attachCopier(::copyElement) }")
        println()
        println("/** 只对一个 RAW root 执行 first pass、复制和 deferred-reference 完成。 */")
        println("fun <E : ${cfirElementType.render()}> copyRoot(source: E): E {")
        withIndent {
            println("return try {")
            withIndent {
                println("context.state.beginScan()")
                println("scanOwned(source)")
                println("context.state.beginCopy()")
                println("val target = copyElement(source)")
                println("context.state.finishReferences()")
                println("@Suppress(\"UNCHECKED_CAST\")")
                println("target as E")
            }
            println("} catch (failure: Throwable) {")
            withIndent { println("context.state.fail()"); println("throw failure") }
            println("}")
        }
        println("}")
        println()
        printCopyDispatcher()
        println()
        printScanDispatcher()
        for (implementation in implementations) {
            println()
            printCopyImplementation(implementation)
            println()
            printScanImplementation(implementation)
        }
    }

    private fun ImportCollectingPrinter.printCopyDispatcher() {
        println("private fun copyElement(source: ${cfirElementType.render()}): ${cfirElementType.render()} {")
        withIndent {
            println("context.state.lookup(source)?.let { return it }")
            println("context.validateRawNode(source)")
            println("context.state.enterConstruction(source)")
            println("return try {")
            withIndent {
                println("when (source.javaClass) {")
                withIndent {
                    for (impl in implementations) println("${impl.render()}::class.java -> copy${impl.typeName}(source as ${impl.render()})")
                    printExternalCopyBranches()
                    println("else -> error(\"Unknown RAW CFIR implementation: \${source.javaClass.name}\")")
                }
                println("}")
            }
            println("} finally { context.state.leaveConstruction(source) }")
        }
        println("}")
    }

    /** 外置实现的策略边界显式列出；没有 generic hand-written-node passthrough。 */
    private fun ImportCollectingPrinter.printExternalCopyBranches() {
        fun external(packageName: String, className: String, method: String) {
            val ref = type(packageName, className, exactPackage = true)
            println("${ref.render()}::class.java -> recordExternal(source, context.$method(source as ${ref.render()}))")
        }
        external("$BASE_PACKAGE.types.impl", "ResolvedImplicitTypeRef", "resolvedImplicitTypeRef")
        external("$BASE_PACKAGE.declarations.impl", "CfirDeclarationStatusImpl", "status")
        external("$BASE_PACKAGE.expressions", "CfirEmptyArgumentList", "emptyArgumentList")
        external("$BASE_PACKAGE.expressions", "CfirResolvedArgumentListImpl", "resolvedArgumentList")
        external("$BASE_PACKAGE.expressions", "CfirResolvedArgumentListForErrorCall", "resolvedArgumentList")
        external("$BASE_PACKAGE.expressions.impl", "CfirAnnotationArgumentMappingImpl", "annotationMapping")
        external("$BASE_PACKAGE.expressions.impl", "CfirEmptyAnnotationArgumentMapping", "annotationMapping")
        val stubReference = type("$BASE_PACKAGE.references", "CfirStubReference", exactPackage = true)
        println("${stubReference.render()}::class.java -> {")
        withIndent {
            println("context.state.allowSharedImmutable(source)")
            println("recordExternal(source, source)")
        }
        println("}")
    }

    private fun ImportCollectingPrinter.printScanDispatcher() {
        println("private fun recordExternal(source: ${cfirElementType.render()}, target: ${cfirElementType.render()}): ${cfirElementType.render()} {")
        withIndent {
            println("context.state.remember(source, target)")
            println("context.validateCopiedNode(source, target)")
            println("return target")
        }
        println("}")
        println()
        println("private fun scanOwned(source: ${cfirElementType.render()}) {")
        withIndent {
            println("if (!context.state.registerOwnedElement(source)) return")
            println("context.validateRawNode(source)")
            println("when (source.javaClass) {")
            withIndent {
                for (impl in implementations) println("${impl.render()}::class.java -> scan${impl.typeName}(source as ${impl.render()})")
                val wrapped = type("$BASE_PACKAGE.types.impl", "ResolvedImplicitTypeRef", exactPackage = true)
                println("${wrapped.render()}::class.java -> scanOwned((source as ${wrapped.render()}).typeRef)")
                val status = type("$BASE_PACKAGE.declarations.impl", "CfirDeclarationStatusImpl", exactPackage = true)
                val kind = generatedType("CfirRawCopyMetadataKind")
                println("${status.render()}::class.java -> context.collectOwnedMetadata(${kind.render()}.STATUS, source, ::scanOwned)")
                val empty = type("$BASE_PACKAGE.expressions", "CfirEmptyArgumentList", exactPackage = true)
                println("${empty.render()}::class.java -> Unit")
                val argList = type("$BASE_PACKAGE.expressions", "CfirResolvedArgumentList", exactPackage = true)
                for (name in listOf("CfirResolvedArgumentListImpl", "CfirResolvedArgumentListForErrorCall")) {
                    val ref = type("$BASE_PACKAGE.expressions", name, exactPackage = true)
                    println("${ref.render()}::class.java -> {")
                    withIndent {
                        println("val list = source as ${argList.render()}")
                        println("list.originalArgumentList?.let(::scanOwned)")
                        println("list.mappingIncludingContextArguments.keys.forEach(::scanOwned)")
                    }
                    println("}")
                }
                for (name in listOf("CfirAnnotationArgumentMappingImpl", "CfirEmptyAnnotationArgumentMapping")) {
                    val ref = type("$BASE_PACKAGE.expressions.impl", name, exactPackage = true)
                    println("${ref.render()}::class.java -> (source as ${ref.render()}).mapping.values.forEach(::scanOwned)")
                }
                val stubReference = type("$BASE_PACKAGE.references", "CfirStubReference", exactPackage = true)
                println("${stubReference.render()}::class.java -> Unit")
                println("else -> error(\"Unknown RAW CFIR implementation during first pass: \${source.javaClass.name}\")")
            }
            println("}")
        }
        println("}")
    }

    private fun validateImplementation(impl: Implementation) {
        check(impl.kind != ImplementationKind.Object) { "Model object needs an explicit immutable/construction policy: $impl" }
        check(impl.element.params.isEmpty()) { "Concrete generic implementation needs an explicit parameter strategy: $impl" }
        for (field in impl.allFields) {
            if (field.invisibleField) {
                check(field.name == "resolveState" && field.customInitializationCall == "resolvePhase.asResolveState()") {
                    "Unknown custom initialization in $impl.${field.name}: ${field.customInitializationCall}"
                }
                continue
            }
            if (field.implementationDefaultStrategy?.withGetter == true) continue
            if (field.isParameter) {
                check(field.name == "resolvePhase") { "Unknown non-property constructor parameter: $impl.${field.name}" }
                continue
            }
            validateType(impl, field, if (field is ListField) field.baseType else field.typeRef)
            if (field !in impl.fieldsInConstructor) writeKind(field, impl)
        }
        if (impl.element == CfirTree.errorTypeRef) {
            val expected = setOf(
                "source", "annotations", "customRenderer", "coneType", "delegatedTypeRef",
                "diagnostic", "partiallyResolvedTypeRef", "typeUseOrigin",
            )
            check(impl.allFields.map { it.name }.toSet() == expected) { "Handwritten error type-ref constructor schema changed: $impl" }
        }
    }

    private fun validateType(impl: Implementation, field: Field, ref: TypeRef) {
        when (ref) {
            is ElementOrRef<*> -> Unit
            is ClassRef<*> -> when {
                ref.packageName == "$BASE_PACKAGE.symbols" && ref.simpleName.endsWith("Symbol") ->
                    check(field.symbolFieldRole != null) { "Symbol role missing in $impl.${field.name}; Field.copy must preserve it" }
                ref.canonicalName in hooks || ref.canonicalName in immutableTypes -> Unit
                ref.canonicalName == "kotlin.collections.Map" -> {
                    val args = ref.args.values.toList()
                    check(args.size == 2 && (args[0] as? ClassRef<*>)?.canonicalName == "org.cangnova.cangjie.name.Name" &&
                        (args[1] as? ElementOrRef<*>)?.element == CfirTree.expression) {
                        "Map field needs a typed policy: $impl.${field.name}"
                    }
                }
                else -> error("No required RAW metadata strategy for $impl.${field.name}: ${ref.canonicalName}")
            }
            else -> error("No RAW strategy for type parameter/lambda field $impl.${field.name}: $ref")
        }
    }

    private fun ImportCollectingPrinter.printCopyImplementation(impl: Implementation) {
        println("private fun copy${impl.typeName}(source: ${impl.render()}): ${impl.render()} {")
        withIndent {
            if (impl.element == CfirTree.resolvedDeclarationStatus) {
                println("val target = context.status(source)")
                println("context.state.remember(source, target)")
                println("context.validateCopiedNode(source, target)")
                println("return target as ${impl.render()}")
                return@withIndent
            }
            val fields = impl.fieldsInConstructor.filterNot { it.invisibleField }.filterNot {
                impl.element == CfirTree.errorTypeRef && it.name == "customRenderer"
            }
            if (impl.element == CfirTree.errorTypeRef) println("check(!source.customRenderer) { \"Handwritten error type ref has no custom-renderer constructor parameter\" }")
            println("val target = ${impl.render()}(")
            withIndent {
                for (field in fields) {
                    val parameterName = if (impl.element == CfirTree.errorTypeRef && field.name == "coneType") "typeOrNull" else field.name
                    print("$parameterName = ")
                    if (field.isParameter) print("context.resolvePhase(source)") else printCopyField(impl, field)
                    println(",")
                }
            }
            println(")")
            println("context.state.remember(source, target)")
            for (field in impl.fieldsInBody.distinctBy { it.name }) {
                if (field in impl.fieldsInConstructor || field.invisibleField || field.implementationDefaultStrategy?.withGetter == true) continue
                val deferred = (field.typeRef as? ClassRef<*>)?.canonicalName == "$BASE_PACKAGE.patterns.CfirPatternTreeOwner"
                if (deferred) {
                    println("context.state.deferReference {")
                    withIndent { printSetField(impl, field) }
                    println("}")
                } else printSetField(impl, field)
            }
            println("context.validateCopiedNode(source, target)")
            println("return target")
        }
        println("}")
    }

    private enum class WriteKind { REPLACE, ASSIGN, MUTATE_LIST }

    /** 与 ImplementationFieldPrinter.forceMutable/getMutableType 使用同一字段语义。 */
    private fun writeKind(field: Field, impl: Implementation): WriteKind {
        if (field.withReplace) return WriteKind.REPLACE
        val list = field as? ListField
        if (list != null && !field.nullable &&
            (field.getMutableType() as? ClassRef<*>)?.canonicalName == "kotlin.collections.MutableList" &&
            !field.readOnlyImplementationList) return WriteKind.MUTATE_LIST
        val propertyIsMutable = field.isMutable &&
            (list == null || list.isMutableOrEmptyList || field.readOnlyImplementationList)
        check(propertyIsMutable && field.customSetterVisibility == Visibility.PUBLIC) {
            "Stored body field has no writable construction strategy: $impl.${field.name}"
        }
        return WriteKind.ASSIGN
    }

    private fun ImportCollectingPrinter.printSetField(impl: Implementation, field: Field) {
        when (writeKind(field, impl)) {
            WriteKind.REPLACE -> {
                print("target.replace${field.name.replaceFirstChar(Char::uppercaseChar)}(")
                printCopyField(impl, field, forReplace = true)
                println(")")
            }
            WriteKind.MUTATE_LIST -> {
                println("target.${field.name}.clear()")
                print("target.${field.name}.addAll(")
                printCopyField(impl, field, forReplace = true)
                println(")")
            }
            WriteKind.ASSIGN -> {
                print("target.${field.name} = ")
                printCopyField(impl, field)
                println()
            }
        }
    }

    private fun ImportCollectingPrinter.printCopyField(impl: Implementation, field: Field, forReplace: Boolean = false) {
        if (field is ListField) {
            print("source.${field.name}${if (field.nullable) "?." else "."}mapTo(mutableListOf()) { value -> ")
            printCopyValue(field, field.baseType, "value")
            print(" }")
            if (field.isMutableOrEmptyList && !forReplace) {
                addImport(toMutableOrEmptyImport)
                print(if (field.nullable) "?.toMutableOrEmpty()" else ".toMutableOrEmpty()")
            }
        } else printCopyValue(field, field.typeRef, "source.${field.name}")
    }

    private fun ImportCollectingPrinter.printCopyValue(field: Field, ref: TypeRef, expression: String) {
        val nullable = (ref as? TypeRefWithNullability)?.nullable == true
        fun call(body: (String) -> Unit) {
            if (nullable) { print("$expression?.let { value -> "); body("value"); print(" }") } else body(expression)
        }
        when (ref) {
            is ElementOrRef<*> -> {
                call { print("context.state.copyOwned($it)") }
            }
            is ClassRef<*> -> when {
                ref.canonicalName in elementHookTypes -> call { print("context.state.copyOwned($it)") }
                ref.packageName == "$BASE_PACKAGE.symbols" && ref.simpleName.endsWith("Symbol") -> call {
                    val method = when (field.symbolFieldRole) {
                        AbstractField.SymbolFieldRole.DECLARED -> "declaredSymbol"
                        AbstractField.SymbolFieldRole.REFERENCED -> "referencedSymbol"
                        null -> error("Missing symbol role: ${field.name}")
                    }
                    print("context.state.$method($it)")
                }
                ref.canonicalName in hooks -> call { print("context.${hooks.getValue(ref.canonicalName).method}($it)") }
                ref.canonicalName in immutableTypes -> print(expression)
                ref.canonicalName == "kotlin.collections.Map" -> call { print("context.annotationArgumentMap($it)") }
                else -> error("Field strategy was not validated: ${field.name} $ref")
            }
            else -> error("Unknown field type: ${field.name} $ref")
        }
    }

    private fun ImportCollectingPrinter.printScanImplementation(impl: Implementation) {
        println("private fun scan${impl.typeName}(source: ${impl.render()}) {")
        withIndent {
            for (field in impl.allFields.distinctBy { it.name }) {
                if (field.invisibleField || field.isParameter || field.implementationDefaultStrategy?.withGetter == true) continue
                if (impl.element == CfirTree.errorTypeRef && field.name == "customRenderer") continue
                val ref = if (field is ListField) field.baseType else field.typeRef
                if (field is ListField) {
                    println("source.${field.name}${if (field.nullable) "?." else "."}forEach { value ->")
                    withIndent { printScanValue(field, ref, "value") }
                    println("}")
                } else printScanValue(field, ref, "source.${field.name}")
            }
        }
        println("}")
    }

    private fun ImportCollectingPrinter.printScanValue(field: Field, ref: TypeRef, expression: String) {
        val nullable = (ref as? TypeRefWithNullability)?.nullable == true
        fun metadata(hook: Hook) {
            val kind = generatedType("CfirRawCopyMetadataKind")
            if (nullable) println("$expression?.let { context.collectOwnedMetadata(${kind.render()}.${hook.kind}, it, ::scanOwned) }")
            else println("context.collectOwnedMetadata(${kind.render()}.${hook.kind}, $expression, ::scanOwned)")
        }
        when (ref) {
            is ElementOrRef<*> -> {
                if (nullable) println("$expression?.let(::scanOwned)") else println("scanOwned($expression)")
            }
            is ClassRef<*> -> when {
                ref.canonicalName in elementHookTypes -> {
                    if (nullable) println("$expression?.let(::scanOwned)") else println("scanOwned($expression)")
                }
                ref.packageName == "$BASE_PACKAGE.symbols" && ref.simpleName.endsWith("Symbol") -> {
                    if (field.symbolFieldRole == AbstractField.SymbolFieldRole.DECLARED) {
                        if (nullable) println("$expression?.let { context.registerDeclaredSymbol(source, it) }")
                        else println("context.registerDeclaredSymbol(source, $expression)")
                    }
                }
                ref.canonicalName in hooks -> metadata(hooks.getValue(ref.canonicalName))
                ref.canonicalName in immutableTypes -> Unit
                ref.canonicalName == "kotlin.collections.Map" -> metadata(Hook("annotationArgumentMap", "ANNOTATION_ARGUMENT_MAP"))
                else -> error("Unknown scan strategy: ${field.name} $ref")
            }
            else -> error("Unknown scan type: ${field.name} $ref")
        }
    }
}
