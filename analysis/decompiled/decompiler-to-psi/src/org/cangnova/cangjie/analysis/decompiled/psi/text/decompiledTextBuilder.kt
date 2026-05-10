package org.cangnova.cangjie.analysis.decompiled.psi.text

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjMainFunction
import org.cangnova.cangjie.psi.CjModifierList
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjParameterList
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeConstraint
import org.cangnova.cangjie.psi.CjTypeConstraintList
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeParameterList
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjVisitorUnit
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.cangnova.cangjie.psi.stubs.CangJieImportDirectiveStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.elements.CjTokenSets.FILE_DECLARATION_TYPES
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl

private const val DECOMPILED_CODE_COMMENT = "/* compiled code */"

/**
 * 对齐 Kotlin `decompiledTextBuilder.kt`：
 * 这里只接受 compiled file stub，并在函数内部声明 visitor 驱动 stub-backed PSI 渲染。
 */
internal fun buildDecompiledText(fileStub: CangJieFileStubImpl): String = PrettyPrinter(indentSize = 4).apply {
    (fileStub.kind as? CangJieFileStubKind.Invalid)?.errorMessage?.let {
        return it
    }

    appendLine("// IntelliJ API Decompiler stub source generated from a .cjo file")
    appendLine("// Implementation of declarations is not available")
    appendLine()

    val keywordNames = CjTokens.KEYWORDALL.types
        .filterIsInstance<CjKeywordToken>()
        .mapTo(hashSetOf()) { keyword -> keyword.value }

    fun renderIdentifier(identifier: String): String {
        return if (identifier in keywordNames) "`$identifier`" else identifier
    }

    fun renderFqName(fqName: org.cangnova.cangjie.name.FqName): String {
        return fqName.pathSegments().joinToString(".") { segment -> renderIdentifier(segment.asString()) }
    }

    val packageFqName = fileStub.getPackageFqName()
    if (!packageFqName.isRoot) {
        append("package ")
        appendLine(renderFqName(packageFqName))
        appendLine()
    }

    fun renderImportItem(importItem: CangJieImportDirectiveStub.ImportItemInfo): String? {
        val importedFqName = importItem.importedFqName ?: return null
        return buildString {
            append(renderFqName(importedFqName))
            if (importItem.isAllUnder) {
                append(".*")
            }
            importItem.aliasName?.takeIf(String::isNotBlank)?.let { aliasName: String ->
                append(" as ")
                append(renderIdentifier(aliasName))
            }
        }
    }

    fun printImportDirective(importDirective: CangJieImportDirectiveStub) {
        val renderedImportItems = importDirective.getImportItems().mapNotNull { importItem: CangJieImportDirectiveStub.ImportItemInfo ->
            renderImportItem(importItem)
        }
        renderedImportItems.forEachIndexed { index: Int, importItem: String ->
            if (index > 0) {
                appendLine()
            }
            append("import ")
            append(importItem)
        }
    }

    val importDirectives = fileStub.findChildStubByType(CjStubElementTypes.IMPORT_LIST)
        ?.childrenStubs
        ?.filterIsInstance<CangJieImportDirectiveStub>()
        .orEmpty()
    printCollectionIfNotEmpty(importDirectives, separator = "\n", postfix = "\n\n") { importDirective ->
        printImportDirective(importDirective)
    }

    // The visitor is declared as local to capture the pretty printer as a context
    val visitor = object : CjVisitorUnit() {
        private inline val explicitThis get() = this

        override fun visitClass(cclass: org.cangnova.cangjie.psi.CjClass) {
            renderTypeStatement(cclass)
        }

        override fun visitStruct(cstruct: org.cangnova.cangjie.psi.CjStruct) {
            renderTypeStatement(cstruct)
        }

        override fun visitInterface(cinterface: org.cangnova.cangjie.psi.CjInterface) {
            renderTypeStatement(cinterface)
        }

        override fun visitExtend(extend: CjExtend) {
            renderTypeStatement(extend)
        }

        override fun visitEnum(cenum: CjEnum) {
            renderTypeStatement(cenum)
        }

        private fun renderTypeStatement(typeStatement: CjTypeStatement) {
            withSuffix(" ") { typeStatement.modifierList?.accept(this) }
            append(typeStatement.typeName)
            withPrefix(" ") { typeStatement.name?.let(::renderIdentifier)?.let(::append) }
            typeStatement.typeParameterList?.accept(this)
            withPrefix("") { typeStatement.getPrimaryConstructorParameterList()?.accept(this) }

            val superTypes = buildList {
                addAll(typeStatement.superTypeListEntries.mapNotNull { entry -> entry.typeReference?.getTypeText() })
            }
            withPrefix(" <: ") {
                if (superTypes.isNotEmpty()) {
                    append(superTypes.joinToString(" & "))
                }
            }

            withPrefix(" ") { typeStatement.typeConstraintList?.accept(this) }
            appendLine(" {")
            withIndent {
                if (typeStatement is CjEnum) {
                    val enumEntries = typeStatement.constructor
                    val members = typeStatement.declarations
                    withSuffix("\n") {
                        "\n\n".separated(
                            {
                                if (enumEntries.isNotEmpty()) {
                                    printCollection(enumEntries, prefix = "| ", separator = "\n\n| ") {
                                        it.accept(explicitThis)
                                    }
                                }
                            },
                            {
                                printCollectionIfNotEmpty(members, separator = "\n\n") {
                                    it.accept(explicitThis)
                                }
                            },
                        )
                    }
                } else {
                    withSuffix("\n") {
                        printCollectionIfNotEmpty(typeStatement.declarations, separator = "\n\n") {
                            it.accept(explicitThis)
                        }
                    }
                }
            }
            append("}")
        }

        override fun visitEnumConstructor(enumConstructor: CjEnumConstructor) {
            withSuffix(" ") { enumConstructor.modifierList?.accept(this) }
            append(enumConstructor.name?.let(::renderIdentifier).orEmpty())
            if (enumConstructor.hasParameters()) {
                append(
                    enumConstructor.typeReferences.joinToString(prefix = "(", postfix = ")") { typeReference ->
                        typeReference.getTypeText()
                    },
                )
            }
        }

        override fun visitTypeAlias(typeAlias: CjTypeAlias) {
            withSuffix(" ") { typeAlias.modifierList?.accept(this) }
            append("type ")
            append(typeAlias.name?.let(::renderIdentifier).orEmpty())
            typeAlias.typeParameterList?.accept(this)
            withPrefix(" ") { typeAlias.typeConstraintList?.accept(this) }
            withPrefix(" = ") { typeAlias.getTypeReference()?.getTypeText()?.takeIf(String::isNotBlank)?.let(::append) }
        }

        override fun visitNamedFunction(function: CjNamedFunction) {
            withSuffix(" ") { function.modifierList?.accept(this) }
            if (function.name == "init") {
                append("init")
                function.valueParameterList?.accept(this) ?: append("()")
                printBody(hasBody = true)
                return
            }
            append("func ")
            withSuffix(" ") { function.typeParameterList?.accept(this) }
            append(function.name?.let(::renderIdentifier).orEmpty())
            function.valueParameterList?.accept(this)
            withPrefix(": ") { function.typeReference?.getTypeText()?.takeIf(String::isNotBlank)?.let(::append) }
            withPrefix(" ") { function.typeConstraintList?.accept(this) }
            printBody(function.hasBody())
        }

        override fun visitMainFunction(mainFunction: CjMainFunction) {
            withSuffix(" ") { mainFunction.modifierList?.accept(this) }
            append("main")
            mainFunction.valueParameterList?.accept(this)
            withPrefix(": ") { mainFunction.typeReference?.getTypeText()?.takeIf(String::isNotBlank)?.let(::append) }
            withPrefix(" ") { mainFunction.typeConstraintList?.accept(this) }
            printBody(mainFunction.hasBody())
        }

        override fun visitSecondaryConstructor(constructor: CjSecondaryConstructor) {
            withSuffix(" ") { constructor.modifierList?.accept(this) }
            append("init")
            constructor.valueParameterList?.accept(this) ?: append("()")
            printBody(hasBody = true)
        }

        override fun visitFinalizer(constructor: CjFinalizer) {
            withSuffix(" ") { constructor.modifierList?.accept(this) }
            append("~init")
            constructor.valueParameterList?.accept(this) ?: append("()")
            printBody(constructor.hasBody())
        }

        override fun visitProperty(property: CjProperty) {
            withSuffix(" ") { property.modifierList?.accept(this) }
            append(if (property.isVar) "mut prop " else "prop ")
            append(property.name?.let(::renderIdentifier).orEmpty())
            withPrefix(": ") { property.typeReference?.getTypeText()?.takeIf(String::isNotBlank)?.let(::append) }
            printCollectionIfNotEmpty(property.accessors, prefix = " {\n", separator = "\n", postfix = "\n}") {
                withIndent {
                    it.accept(explicitThis)
                }
            }
        }

        override fun visitFieldVariable(field: CjFieldVariable) {
            withSuffix(" ") { field.modifierList?.accept(this) }
            append(
                when {
                    field.isConst -> "const "
                    field.isVar -> "var "
                    else -> "let "
                },
            )
            append(field.name?.let(::renderIdentifier).orEmpty())
            withPrefix(": ") { field.typeReference?.getTypeText()?.takeIf(String::isNotBlank)?.let(::append) }
            if (field.hasInitializer()) {
                append(" = ")
                append(DECOMPILED_CODE_COMMENT)
            }
        }

        override fun visitPatternVariable(variable: CjPatternVariable) {
            withSuffix(" ") { variable.modifierList?.accept(this) }
            append(if (variable.isVar) "var " else "let ")
            append((variable.pattern as? CjBindingPattern)?.name?.let(::renderIdentifier) ?: "_")
            withPrefix(": ") { variable.typeReference?.getTypeText()?.takeIf(String::isNotBlank)?.let(::append) }
            if (variable.hasInitializer()) {
                append(" = ")
                append(DECOMPILED_CODE_COMMENT)
            }
        }

        override fun visitPropertyAccessor(accessor: CjPropertyAccessor) {
            withSuffix(" ") { accessor.modifierList?.accept(this) }
            append(if (accessor.isGetter) "get" else "set")
            accessor.parameterList?.accept(this)
            withPrefix(": ") { accessor.returnTypeReference?.getTypeText()?.takeIf(String::isNotBlank)?.let(::append) }
            printBody(accessor.hasBody())
        }

        override fun visitParameterList(list: CjParameterList) {
            printCollection(list.parameters, prefix = "(", postfix = ")") {
                it.accept(explicitThis)
            }
        }

        override fun visitParameter(parameter: CjParameter) {
            withSuffix(" ") { parameter.modifierList?.accept(this) }
            append(parameter.name?.let(::renderIdentifier).orEmpty())
            append(": ")
            parameter.typeReference?.getTypeText()?.takeIf(String::isNotBlank)?.let(::append)
        }

        override fun visitTypeParameterList(list: CjTypeParameterList) {
            printCollection(list.parameters, prefix = "<", postfix = ">") {
                it.accept(explicitThis)
            }
        }

        override fun visitTypeParameter(parameter: CjTypeParameter) {
            withSuffix(" ") { parameter.modifierList?.accept(this) }
            append(parameter.name?.let(::renderIdentifier).orEmpty())
            withPrefix(" : ") { parameter.extendsBound?.getTypeText()?.takeIf(String::isNotBlank)?.let(::append) }
        }

        override fun visitTypeConstraintList(list: CjTypeConstraintList) {
            append("where ")
            printCollection(list.constraints, separator = ", ") {
                it.accept(explicitThis)
            }
        }

        override fun visitTypeConstraint(constraint: CjTypeConstraint) {
            append(constraint.subjectTypeParameterName?.text.orEmpty())
            append(" : ")
            constraint.boundTypeReference?.getTypeText()?.takeIf(String::isNotBlank)?.let(::append)
        }

        override fun visitModifierList(list: CjModifierList) {
            val modifiers = CjTokens.MODIFIER_KEYWORDS_ARRAY
                .filter { modifier -> list.hasModifier(modifier) }
                .joinToString(" ") { modifier -> modifier.value }
            append(modifiers)
        }

        override fun visitElement(element: PsiElement) {
            append("/* !${element::class.simpleName}! */")
            super.visitElement(element)
        }

        private fun printBody(hasBody: Boolean) {
            if (!hasBody) {
                return
            }
            append(" { ")
            append(DECOMPILED_CODE_COMMENT)
            append(" }")
        }
    }

    val declarations = fileStub.getChildrenByType(FILE_DECLARATION_TYPES, CjDeclaration.ARRAY_FACTORY).asList()
    printCollectionIfNotEmpty(declarations, separator = "\n\n", postfix = "\n") {
        it.accept(visitor)
    }
}.toString()
