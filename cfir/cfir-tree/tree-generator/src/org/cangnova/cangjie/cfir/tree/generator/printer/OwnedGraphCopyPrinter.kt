package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.CfirTree
import org.cangnova.cangjie.cfir.tree.generator.model.*
import org.cangnova.cangjie.generators.tree.ElementOrRef
import org.cangnova.cangjie.generators.tree.ClassRef
import org.cangnova.cangjie.generators.tree.ClassOrElementRef
import org.cangnova.cangjie.generators.tree.ImplementationKind
import org.cangnova.cangjie.generators.tree.printer.GeneratedFile
import org.cangnova.cangjie.generators.tree.printer.printGeneratedType
import org.cangnova.cangjie.generators.tree.nullable
import java.io.File

/** 与 TreeGenerator 同一模型生成复制 visitor；字段变更自动进入壳分配和第二遍回填。 */
internal object OwnedGraphCopyPrinter {
    private val packageRoot = "org.cangnova.cangjie.cfir"
    private val skipped = setOf("CfirLazyBlockImpl", "CfirLazyExpressionImpl")
    private val handWritten = setOf("CfirArgumentList", "CfirErrorTypeRef", "CfirAnnotationArgumentMapping")

    fun generate(path: File, readme: String, implementations: List<Implementation>, intermediateBuilderNames: Set<String>): GeneratedFile =
        printGeneratedType(path, readme, packageRoot + ".copy", "CfirDeepCopyOwnedGraphVisitor") {
            listOf("", ".builder", ".declarations", ".declarations.builder", ".declarations.impl",
                ".expressions", ".expressions.builder", ".expressions.impl", ".impl",
                ".patterns", ".patterns.builder", ".patterns.impl", ".references", ".references.builder",
                ".references.impl", ".types", ".types.builder", ".types.impl").forEach {
                println("import " + packageRoot + it + ".*")
            }
            println()
            println("/** 全部生成式 concrete 节点的壳分配与拥有字段回填。 */")
            println("@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)")
            println("internal class CfirDeepCopyOwnedGraphVisitor(context: CfirOwnedGraphCopyContext) : CfirOwnedGraphCopyVisitor(context) {")
            println("    /** 与回填使用相同的已选择内容；收集阶段不能重新读取活动 Library getter。 */")
            println("    fun collectChildren(original: CfirElement, collector: org.cangnova.cangjie.cfir.visitors.CfirVisitor<Unit, Unit>) {")
            println("        when (original) {")
            implementations.filter {
                it.doPrint && it.kind in setOf(ImplementationKind.FinalClass, ImplementationKind.OpenClass) &&
                    it.allFields.any { field -> field.name == "contentSource" } &&
                    it.allFields.any { field -> field.publishedContentKind != null }
            }.map { it.element }.distinct().sortedByDescending { it.inheritanceDepth() }.forEach { element ->
                println("            is " + element.typeName + " -> {")
                // CFG 是后续分析重建产品；共同 F owner 的收集只读取已选择正文及真实拥有字段。
                element.allFields.filterNot { it.classType?.typeName == "CfirControlFlowGraphReference" }.forEach { field ->
                    val traversal = field.sourceTraversalKind ?: if (field.isChild && field.containsElement)
                        Field.SourceTraversalKind.ELEMENT else null
                    val value = selectedValue(field)
                    val access = if (field.typeRef.nullable) "?." else "."
                    val visit = when (traversal) {
                        Field.SourceTraversalKind.ELEMENT -> if (field is ListField)
                            value + access + "forEach { it.accept(collector, Unit) }"
                        else value + access + "accept(collector, Unit)"
                        Field.SourceTraversalKind.MAP_VALUES -> value + access + "values.forEach { it.accept(collector, Unit) }"
                        Field.SourceTraversalKind.ARGUMENT_VIEW -> value + access + "acceptSourceChildren(collector, Unit)"
                        null -> null
                    }
                    if (visit != null) println("                " + visit)
                }
                println("            }")
            }
            println("            else -> original.acceptSourceChildren(collector, Unit)")
            println("        }")
            println("    }")
            println()
            implementations.filter {
                it.doPrint && it.typeName !in skipped && it.element.typeName !in handWritten &&
                    it.kind in setOf(ImplementationKind.FinalClass, ImplementationKind.OpenClass)
            }.groupBy { it.element }.forEach { (element, variants) ->
                println("    override fun " + element.visitFunctionName + "(original: " + element.typeName + ", data: Boolean): CfirElement {")
                when (element.typeName) {
                    "CfirExpression" -> println("        if (original !is CfirExpressionStub) return copyHandWrittenExpression(original, data)")
                    "CfirImplicitTypeRef" -> println("        if (original is ResolvedImplicitTypeRef) return if (data) context.copyOwned(original) else ResolvedImplicitTypeRef(context.allocate(original.typeRef))")
                    "CfirResolvedNamedReference" -> println("        if (original is CfirResolvedAppliedCallableReference) return if (data) context.copyOwned(original) else copyAppliedReference(original)")
                }
                if (variants.size == 1) {
                    val implementation = variants.single()
                    println("        if (!data) return " + shell(implementation, intermediateBuilderNames))
                    println("        val copied = context.copyOwned(original) as " + implementation.typeName)
                    println(fill(implementation))
                } else {
                    check(element.typeName == "CfirConstructor") { "Add the actual discriminator for a new multi-implementation element" }
                    val primary = variants.single { it.typeName == "CfirPrimaryConstructor" }
                    val secondary = variants.single { it.typeName == "CfirConstructorImpl" }
                    println("        if (!data) return if (original.isPrimary) " + shell(primary, intermediateBuilderNames) + " else " + shell(secondary, intermediateBuilderNames))
                    println("        when (val copied = context.copyOwned(original)) {")
                    variants.forEach { implementation ->
                        println("            is " + implementation.typeName + " -> {")
                        println(fill(implementation))
                        println("            }")
                    }
                    println("        }")
                    println("        val copied = context.copyOwned(original)")
                }
                println("        return copied")
                println("    }")
                println()
            }
            println("}")
        }

    private fun shell(implementation: Implementation, intermediateBuilderNames: Set<String>): String {
        val constructorFields = copyConstructorFields(implementation)
        // 同名 intermediate builder 最终出版的是接口，不存在叶子 DSL copy 函数。
        // 内容节点直接按已捕获选择值构造；build*Copy 的预填会额外读取活动原 body/initializer。
        val builder = (implementation.builder as? LeafBuilder)?.takeIf {
            it.wantsCopy && it.typeName !in intermediateBuilderNames &&
                implementation.allFields.none { field -> field.publishedContentKind != null } &&
                constructorFields.none { field -> isPatternOwnershipEdge(implementation, field) }
        }
        return if (builder != null) {
            val name = builder.typeName.removePrefix("Cfir").removeSuffix("Builder").removeSuffix("Impl")
            "build" + name + "Copy(original) {\n" + builder.allFields.filter {
                it.hasSymbolType || it.name == "attributes" || (it.name == "name" && it.classType?.typeName == "Name") || it.hasConeType ||
                    it.classType?.typeName in setOf("CfirTarget", "BuiltinPrimitiveOperatorSignature")
            }.joinToString("\n") { "            " + it.name + " = " + shellValue(implementation, it) } + "\n        }"
        } else {
            // 与 ImplementationPrinter 同一参数规则：final 的 resolveState 由 init 根据 phase 建立。
            implementation.typeName + "(\n" + constructorFields.joinToString(",\n") {
                "            " + it.name + " = " + shellValue(implementation, it)
            } + ",\n        )"
        }
    }

    /** 与 ImplementationPrinter 的实际参数规则一致；字段身份不用于区分构造和类体存储。 */
    private fun copyConstructorFields(implementation: Implementation): List<Field> {
        val fields = implementation.fieldsInConstructor.filter { it.isParameter || !it.isFinal }
        check(fields.map { it.name }.distinct().size == fields.size) {
            "Copy constructor parameters must have unique field names: " + implementation.typeName
        }
        return fields
    }

    /**
     * 根节点的构造器会绑定整棵模式树。只对真实模式拥有边递归分配，
     * 使这些构造器永远不会读到仍由原宿主持有的孩子或 binding。
     */
    private fun isPatternOwnershipEdge(implementation: Implementation, field: Field): Boolean {
        val element = implementation.element
        if (element.patternTreeRootFieldName == field.name) return true
        if (!element.isPattern()) return false
        val fieldElement = ((if (field is ListField) field.baseType else field.typeRef) as? ElementOrRef<*>)?.element
        return field.isChild && fieldElement is Element &&
            (fieldElement.isPattern() || fieldElement == CfirTree.patternBindingVariable)
    }

    private fun Element.isPattern(): Boolean =
        this == CfirTree.pattern || allParents.any { it.isPattern() }

    private fun shellValue(implementation: Implementation, field: Field): String {
        val value = selectedValue(field)
        return when {
            isPatternOwnershipEdge(implementation, field) && field is ListField ->
                value + ".mapTo(mutableListOf()) { context.allocate(it) }" +
                    if (field.isMutableOrEmptyList) ".toMutableOrEmpty()" else ""
            isPatternOwnershipEdge(implementation, field) -> nullableCall(field, "context.allocate")
            field.name == "contentSource" -> "CfirContentSource.Owned"
            field.name == "name" && field.classType?.typeName == "Name" -> "context.name(original, " + value + ")"
            field.classType?.typeName == "BuiltinPrimitiveOperatorSignature" -> "copyOperatorSignature(" + value + ")"
            field.hasSymbolType -> nullableCall(field, "context.remapSymbol")
            field.name == "attributes" -> "context.copyAttributes(" + value + ")"
            field.classType?.typeName == "CfirTarget" -> "context.target(" + value + ")"
            field.hasConeType -> copyValue(field)
            field is ListField -> value + ".toMutableList()" + if (field.isMutableOrEmptyList) ".toMutableOrEmpty()" else ""
            else -> value
        }
    }

    /** 与构造器共用同一参数写集；custom body getter/setter 不让同名字段再次回填。 */
    private fun fill(implementation: Implementation): String {
        val constructorFields = copyConstructorFields(implementation)
        val constructorFieldNames = constructorFields.mapTo(mutableSetOf()) { it.name }
        val owningFields = constructorFields.filter {
            it.typeRef is ElementOrRef<*> || it is ListField ||
                it.classType?.typeName in setOf("CfirAnnotationArgumentView", "CfirAssignmentTypeMismatchOutcome")
        }.filterNot {
            it.classType?.typeName == "CfirControlFlowGraphReference" || isPatternOwnershipEdge(implementation, it)
        }
        val lines = owningFields.map { field ->
            val value = copyValue(field)
            when {
                field.withReplace -> "        copied.replace" + field.name.replaceFirstChar(Char::uppercaseChar) + "(" + value + ")"
                field is ListField && !field.isMutableOrEmptyList -> {
                    check(!field.readOnlyImplementationList) { "Read-only list requires a replace entry point: ${implementation.typeName}.${field.name}" }
                    "        copied." + field.name + ".clear()\n        copied." + field.name + ".addAll(" + value + ")"
                }
                else -> {
                    check(field.isMutable && field.customSetterVisibility == org.cangnova.cangjie.generators.tree.Visibility.PUBLIC) {
                        "Immutable owned field requires a hand-written allocation contract: " + implementation.typeName + "." + field.name
                    }
                    "        copied." + field.name + " = " + value
                }
            }
        }.toMutableList()
        implementation.fieldsInBody.filter {
            it.typeRef is ElementOrRef<*> && it.withReplace && it.classType?.typeName != "CfirControlFlowGraphReference" &&
                it.implementationDefaultStrategy?.withGetter != true && it.name !in constructorFieldNames
        }.forEach { field ->
            val method = "replace" + field.name.replaceFirstChar { it.uppercaseChar() }
            lines += "        copied." + method + "(" + copyValue(field) + ")"
        }
        return lines.joinToString("\n")
    }

    private fun copyValue(field: Field): String {
        val value = selectedValue(field)
        val type = field.classType?.typeName
        return when {
            field is ListField -> {
                val mapper = when {
                    (field.baseType as? ClassOrElementRef)?.typeName == "CfirConstantValue" -> "context.copyConstant(it)"
                    field.baseType is ElementOrRef<*> -> "context.copyOwned(it)"
                    else -> "it"
                }
                value + ".map { " + mapper + " }" + if (field.isMutableOrEmptyList) ".toMutableOrEmpty()" else ".toMutableList()"
            }
            type == "CfirAnnotationArgumentView" -> nullableCall(field, "copyArgumentView")
            type == "CfirAssignmentTypeMismatchOutcome" -> value + "?.let { it.copy(expectedType = context.type(it.expectedType), actualType = context.type(it.actualType)) }"
            type == "BuiltinPrimitiveOperatorSignature" -> "copyOperatorSignature(" + value + ")"
            field.hasConeType -> nullableCall(field, if (type == "ConeSimpleCangJieType") "context.simpleType" else "context.type")
            field.hasSymbolType -> nullableCall(field, "context.remapSymbol")
            field.typeRef is ElementOrRef<*> && !field.isChild && field.sourceTraversalKind == null ->
                nullableCall(field, "context.copyReferenceElement")
            type == "CfirTarget" -> "context.target(" + value + ")"
            else -> nullableCall(field, "context.copyOwned")
        }
    }

    private fun selectedValue(field: Field): String = when (field.publishedContentKind) {
        Field.PublishedContentKind.BODY -> "context.body(original)"
        Field.PublishedContentKind.INITIALIZER -> "context.initializer(original)"
        Field.PublishedContentKind.DEFAULT_VALUE -> "context.defaultValue(original)"
        Field.PublishedContentKind.ANNOTATIONS -> "context.annotations(original)"
        Field.PublishedContentKind.DEPRECATIONS -> "UnresolvedDeprecationProvider"
        null -> "original." + field.name
    }

    private fun nullableCall(field: Field, function: String): String {
        val value = selectedValue(field)
        return if (field.typeRef.nullable) value + "?.let { " + function + "(it) }"
        else function + "(" + value + ")"
    }

    private fun Element.inheritanceDepth(): Int = 1 + (allParents.maxOfOrNull { it.inheritanceDepth() } ?: 0)
}

/** 与 ImplementationPrinter 使用同一符号字段判定。 */
private val Field.hasSymbolType: Boolean
    get() = (typeRef as? ClassRef<*>)?.simpleName?.contains("Symbol") ?: false

/** 名称只属于 ClassOrElementRef；泛型变量等 TypeRef 不具有类名合同。 */
private val Field.classType: ClassOrElementRef?
    get() = typeRef as? ClassOrElementRef

private val Field.hasConeType: Boolean
    get() = (typeRef as? ClassRef<*>)?.let { it.typeName.startsWith("Cone") && it.typeName.endsWith("Type") } == true
