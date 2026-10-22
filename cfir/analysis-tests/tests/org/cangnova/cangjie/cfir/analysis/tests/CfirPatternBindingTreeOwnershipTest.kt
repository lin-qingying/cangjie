package org.cangnova.cangjie.cfir.analysis.tests

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariableCopy
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternVariableCopy
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildCatch
import org.cangnova.cangjie.cfir.expressions.builder.buildHandleClause
import org.cangnova.cangjie.cfir.expressions.builder.buildLetPatternExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildMatchBranch
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPatternCopy
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.expandWithDefaultContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildCatchPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildCommandTypePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildTuplePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildTypePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildVarOrEnumPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.cfir.visitors.CfirDefaultTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

/**
 * Pattern binding 变量可引用外层 initializer，但不能再次拥有它的 CFIR 子树。
 */
class CfirPatternBindingTreeOwnershipTest : AbstractCfirAnalysisResolveTest() {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun psiPatternBindingInitializerHasSingleTreeOwner() {
        val session = createTestSession()
        val file = createCjFile("patternBindingInitializer", SOURCE).toCfirFile(session)
        assertRawConstructionAlias(file)
        resolveToPhase(file, session, CfirResolvePhase.BODY_RESOLVE)

        assertSingleTreeOwner(file)
    }

    @Test
    fun lightTreePatternBindingInitializerHasSingleTreeOwner() {
        val session = createTestSession()
        val file = buildLightTreeCfirFile("patternBindingInitializerLightTree", SOURCE, session)
        assertRawConstructionAlias(file)
        resolveToPhase(file, session, CfirResolvePhase.BODY_RESOLVE)

        assertSingleTreeOwner(file)
    }

    @Test
    fun wholeInitializerAliasFollowsOnlySingleValuePatternBindings() {
        val session = createTestSession()
        val file = createCjFile("patternBindingInitializerShapes", SOURCE).toCfirFile(session)
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val patternVariable = function.body!!.statements.filterIsInstance<CfirPatternVariable>().single()
        val sourceBinding = (patternVariable.pattern as CfirBindingPattern).bindingVariable!!
        val initializer = requireNotNull(patternVariable.initializer)

        fun newBinding() = buildPatternBindingVariableCopy(sourceBinding) {
            this.symbol = CfirPatternBindingSymbol(sourceBinding.symbol.callableId)
        }

        fun newOwner(pattern: CfirPattern) = buildPatternVariableCopy(patternVariable) {
            this.symbol = CfirPatternVariableSymbol(patternVariable.symbol.callableId)
            this.initializer = initializer
            this.pattern = pattern
        }

        val varOrEnumBinding = newBinding()
        val varOrEnumPattern = buildVarOrEnumPattern {
            name = varOrEnumBinding.name
            bindingVariable = varOrEnumBinding
        }
        val varOrEnumOwner = newOwner(varOrEnumPattern)
        assertSame(initializer, varOrEnumBinding.initializer)
        assertSame(varOrEnumOwner, varOrEnumBinding.patternTreeOwner)

        val typePatternBinding = newBinding()
        val typePattern = buildTypePattern {
            typeRef = patternVariable.returnTypeRef
            bindingName = typePatternBinding.name
            bindingVariable = typePatternBinding
        }
        val typePatternOwner = newOwner(typePattern)
        assertSame(initializer, typePatternBinding.initializer)
        assertSame(typePatternOwner, typePatternBinding.patternTreeOwner)

        val tupleFirstBinding = newBinding()
        val tupleSecondBinding = newBinding()
        val tuplePattern = buildTuplePattern {
            elements += buildBindingPattern {
                name = tupleFirstBinding.name
                bindingVariable = tupleFirstBinding
            }
            elements += buildBindingPattern {
                name = tupleSecondBinding.name
                bindingVariable = tupleSecondBinding
            }
        }
        val tupleOwner = newOwner(tuplePattern)
        assertNull(tupleFirstBinding.initializer, "A tuple pattern must not assign its whole initializer to every binding.")
        assertNull(tupleSecondBinding.initializer, "A tuple pattern must not assign its whole initializer to every binding.")
        assertSame(tupleOwner, tupleFirstBinding.patternTreeOwner)
        assertSame(tupleOwner, tupleSecondBinding.patternTreeOwner)

        val tuple = tuplePattern as CfirTuplePattern
        assertThrows(UnsupportedOperationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            (tuple.elements as MutableList<CfirPattern>).clear()
        }
        assertEquals(2, tuple.elements.size, "Pattern child lists must not be mutable through the implementation view.")

        val originalFirstPattern = tuple.elements.first() as CfirBindingPattern
        val replacedFirstBinding = newBinding()
        val replacedFirstPattern = buildBindingPatternCopy(originalFirstPattern) {
            this.bindingVariable = replacedFirstBinding
        }
        tuple.replaceElements(listOf(replacedFirstPattern, tuple.elements[1]))
        assertNull(tupleFirstBinding.patternTreeOwner, "Replacing a pattern child list must detach removed bindings.")
        assertSame(tupleOwner, replacedFirstBinding.patternTreeOwner)
        assertSame(tupleOwner, replacedFirstPattern.patternTreeOwner)

        val transformedFirstBinding = newBinding()
        val transformedFirstPattern = buildBindingPatternCopy(replacedFirstPattern) {
            this.bindingVariable = transformedFirstBinding
        }
        tuple.transformElements(
            object : CfirDefaultTransformer<Unit>() {
                override fun transformBindingPattern(bindingPattern: CfirBindingPattern, data: Unit): CfirPattern =
                    if (bindingPattern === replacedFirstPattern) transformedFirstPattern
                    else super.transformBindingPattern(bindingPattern, data)
            },
            Unit,
        )
        assertNull(replacedFirstBinding.patternTreeOwner, "Transforming a pattern child list must detach removed bindings.")
        assertSame(tupleOwner, transformedFirstBinding.patternTreeOwner)
        assertSame(tupleOwner, transformedFirstPattern.patternTreeOwner)

        val nestedBinding = newBinding()
        val nestedLeafBinding = newBinding()
        val nestedPattern = buildBindingPattern {
            name = nestedBinding.name
            bindingVariable = nestedBinding
            nestedPattern = buildBindingPattern {
                name = nestedLeafBinding.name
                bindingVariable = nestedLeafBinding
            }
        }
        val nestedOwner = newOwner(nestedPattern)
        assertNull(nestedBinding.initializer, "A nested binding must receive its projected value, not the whole initializer.")
        assertSame(nestedOwner, nestedBinding.patternTreeOwner)
        assertSame(nestedOwner, nestedLeafBinding.patternTreeOwner)
    }

    private fun assertSingleTreeOwner(file: CfirFile) {
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val patternVariable = function.body!!.statements.filterIsInstance<CfirPatternVariable>().single()
        val bindingVariable = (patternVariable.pattern as CfirBindingPattern).bindingVariable!!
        val initializer = requireNotNull(patternVariable.initializer)
        assertSame(initializer, bindingVariable.initializer)

        var visitorVisits = 0
        file.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element === initializer) visitorVisits++
                element.acceptChildren(this)
            }
        })

        var transformerVisits = 0
        val transformer = object : CfirDefaultTransformer<Unit>() {
            override fun <E : CfirElement> transformElement(element: E, data: Unit): E {
                if (element === initializer) transformerVisits++
                return super.transformElement(element, data)
            }
        }
        file.transform<CfirElement, Unit>(transformer, Unit)

        assertEquals(1, visitorVisits, "A CFIR visitor must traverse the initializer through one owning child edge.")
        assertEquals(1, transformerVisits, "A CFIR transformer must transform the initializer through one owning child edge.")

        val replacement = buildLiteralExpression {
            kind = CfirLiteralKind.INT
            value = 100
        }
        var replacementCount = 0
        val replacingTransformer = object : CfirDefaultTransformer<Unit>() {
            override fun <E : CfirElement> transformElement(element: E, data: Unit): E {
                if (element === initializer) {
                    replacementCount++
                    @Suppress("UNCHECKED_CAST")
                    return replacement as E
                }
                return super.transformElement(element, data)
            }
        }
        file.transform<CfirElement, Unit>(replacingTransformer, Unit)

        assertEquals(1, replacementCount, "The replacing transformer must see the owned initializer once.")
        assertSame(replacement, patternVariable.initializer)
        assertSame(replacement, bindingVariable.initializer)

        patternVariable.replaceInitializer(null)
        assertNull(bindingVariable.initializer, "Clearing the owner initializer must clear its semantic alias.")
        patternVariable.replaceInitializer(replacement)

        var replacementVisitorVisits = 0
        file.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element === replacement) replacementVisitorVisits++
                element.acceptChildren(this)
            }
        })
        var replacementTransformerVisits = 0
        val replacementCountingTransformer = object : CfirDefaultTransformer<Unit>() {
            override fun <E : CfirElement> transformElement(element: E, data: Unit): E {
                if (element === replacement) replacementTransformerVisits++
                return super.transformElement(element, data)
            }
        }
        file.transform<CfirElement, Unit>(replacementCountingTransformer, Unit)

        assertEquals(1, replacementVisitorVisits, "The replacement must remain owned by exactly one CFIR child edge.")
        assertEquals(1, replacementTransformerVisits, "The replacement must be transformed through exactly one child edge.")

        val sourcePattern = patternVariable.pattern as CfirBindingPattern
        val transformedBinding = buildPatternBindingVariableCopy(bindingVariable) {
            this.symbol = CfirPatternBindingSymbol(bindingVariable.symbol.callableId)
        }
        val transformedPattern = buildBindingPatternCopy(sourcePattern) {
            this.bindingVariable = transformedBinding
        }
        patternVariable.transformPattern(
            object : CfirDefaultTransformer<Unit>() {
                override fun transformBindingPattern(bindingPattern: CfirBindingPattern, data: Unit): CfirPattern =
                    if (bindingPattern === sourcePattern) transformedPattern else super.transformBindingPattern(bindingPattern, data)
            },
            Unit,
        )
        assertSame(transformedPattern, patternVariable.pattern)
        assertNull(bindingVariable.patternTreeOwner, "Replacing a pattern must detach its former binding owner.")
        assertNull(bindingVariable.initializer, "A detached binding cannot retain the former owner's initializer.")
        assertSame(replacement, transformedBinding.initializer, "Replacing the owner pattern must bind its new variable to the owner's initializer.")

        val copiedBinding = buildPatternBindingVariableCopy(transformedBinding) {
            this.symbol = CfirPatternBindingSymbol(transformedBinding.symbol.callableId)
        }
        val copiedPattern = buildBindingPatternCopy(transformedPattern) {
            this.bindingVariable = copiedBinding
        }
        val copiedPatternVariable = buildPatternVariableCopy(patternVariable) {
            this.symbol = CfirPatternVariableSymbol(patternVariable.symbol.callableId)
            this.initializer = replacement
            this.pattern = copiedPattern
        }
        assertNotSame(transformedBinding, copiedBinding, "A pattern owner copy must receive a distinct binding declaration.")
        assertSame(replacement, copiedPatternVariable.initializer)
        assertSame(replacement, copiedBinding.initializer)
        assertSame(replacement, transformedBinding.initializer, "Updating a copied owner must not change the original owner's binding alias.")

        val initializerSetter = patternVariable.javaClass.declaredMethods.single { it.name == "setInitializer" }
        val patternSetter = patternVariable.javaClass.declaredMethods.single { it.name == "setPattern" }
        assertFalse(Modifier.isPublic(initializerSetter.modifiers), "The generated initializer property must not expose an unsynchronized setter.")
        assertFalse(Modifier.isPublic(patternSetter.modifiers), "The generated pattern property must not expose an unsynchronized setter.")
    }

    @Test
    fun aPatternTreeCannotBeClaimedByTwoOwners() {
        val session = createTestSession()
        val file = createCjFile("patternBindingSharedOwner", SOURCE).toCfirFile(session)
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val firstOwner = function.body!!.statements.filterIsInstance<CfirPatternVariable>().single()

        assertThrows(IllegalArgumentException::class.java) {
            buildPatternVariableCopy(firstOwner) {
                this.symbol = CfirPatternVariableSymbol(firstOwner.symbol.callableId)
                this.pattern = firstOwner.pattern
            }
        }

        val replacementBinding = buildPatternBindingVariableCopy(
            (firstOwner.pattern as CfirBindingPattern).bindingVariable!!,
        ) {
            this.symbol = CfirPatternBindingSymbol(firstOwner.symbol.callableId)
        }
        val replacementPattern = buildBindingPatternCopy(firstOwner.pattern as CfirBindingPattern) {
            this.bindingVariable = replacementBinding
        }
        val secondOwner = buildPatternVariableCopy(firstOwner) {
            this.symbol = CfirPatternVariableSymbol(firstOwner.symbol.callableId)
            this.pattern = replacementPattern
        }
        assertThrows(IllegalArgumentException::class.java) {
            secondOwner.replacePattern(firstOwner.pattern)
        }
        assertSame(replacementPattern, secondOwner.pattern, "A rejected replacement must not partially mutate its owner.")
    }

    @Test
    fun failedOwnerConstructionLeavesThePatternTreeAvailable() {
        val session = createTestSession()
        val file = createCjFile("failedPatternOwnerConstruction", SOURCE).toCfirFile(session)
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val template = function.body!!.statements.filterIsInstance<CfirPatternVariable>().single()
        val detachedPattern = buildWildcardPattern()

        assertThrows(IllegalArgumentException::class.java) {
            buildPatternVariableCopy(template) {
                source = null
                pattern = detachedPattern
                symbol = CfirPatternVariableSymbol(template.symbol.callableId)
            }
        }
        assertNull(detachedPattern.patternTreeOwner, "A constructor rejected before initialization must not leave a half-built owner link.")

        val validOwner = buildPatternVariableCopy(template) {
            pattern = detachedPattern
            symbol = CfirPatternVariableSymbol(template.symbol.callableId)
        }
        assertSame(validOwner, detachedPattern.patternTreeOwner)
    }

    @Test
    fun cyclicPatternTreesAreRejectedAsStructuredInvariants() {
        val recursivePattern = buildBindingPattern {
            name = Name.identifier("value")
        }

        assertThrows(IllegalArgumentException::class.java) {
            recursivePattern.replaceNestedPattern(recursivePattern)
        }

        assertNull(recursivePattern.nestedPattern)
    }

    @Test
    fun sharedPatternSubtreesAreRejectedBeforeAnOwnerClaimsThem() {
        val sharedPattern = buildWildcardPattern()
        val sharedTuple = buildTuplePattern {
            elements += sharedPattern
            elements += sharedPattern
        }

        assertThrows(IllegalArgumentException::class.java) {
            buildLetPatternExpression {
                initializer = newIntegerInitializer()
                pattern = sharedTuple
            }
        }

        assertNull(sharedPattern.patternTreeOwner)
    }

    @Test
    fun everyPatternTreeHostOwnsAndReconcilesItsRootTree() {
        val session = createTestSession()
        val file = createCjFile("patternTreeHostOwnership", SOURCE).toCfirFile(session)
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val patternVariable = function.body!!.statements.filterIsInstance<CfirPatternVariable>().single()
        val existingPattern = patternVariable.pattern

        val letPattern = buildWildcardPattern()
        val letExpression = buildLetPatternExpression {
            initializer = newIntegerInitializer()
            pattern = letPattern
        }
        assertSame(letExpression, letPattern.patternTreeOwner)

        val branchPattern = buildWildcardPattern()
        val branch = buildMatchBranch {
            pattern = branchPattern
            body = buildBlock()
        }
        assertSame(branch, branchPattern.patternTreeOwner)
        assertThrows(IllegalArgumentException::class.java) {
            buildMatchBranch {
                pattern = letPattern
                body = buildBlock()
            }
        }

        val replacementPattern = buildWildcardPattern()
        branch.replacePattern(replacementPattern)
        assertNull(branchPattern.patternTreeOwner, "Replacing a branch root must detach the former tree.")
        assertSame(branch, replacementPattern.patternTreeOwner, "Replacing a branch root must attach its new tree.")

        val sourceBinding = (existingPattern as CfirBindingPattern).bindingVariable!!
        val catchBinding = buildPatternBindingVariableCopy(sourceBinding) {
            symbol = CfirPatternBindingSymbol(sourceBinding.symbol.callableId)
        }
        val catchPattern = buildCatchPattern {
            isWildcard = false
            bindingName = catchBinding.name
            bindingVariable = catchBinding
        }
        val catchClause = buildCatch {
            pattern = catchPattern
            body = buildBlock()
        }
        assertSame(catchClause, catchPattern.patternTreeOwner)
        assertSame(catchClause, catchBinding.patternTreeOwner)

        val replacementCatchBinding = buildPatternBindingVariableCopy(catchBinding) {
            symbol = CfirPatternBindingSymbol(catchBinding.symbol.callableId)
        }
        catchPattern.replaceBindingVariable(replacementCatchBinding)
        assertNull(catchBinding.patternTreeOwner, "Replacing a catch binding must detach its old tree node.")
        assertSame(catchClause, replacementCatchBinding.patternTreeOwner)

        val commandPattern = buildCommandTypePattern {
            isWildcard = true
        }
        val handleClause = buildHandleClause {
            this.commandPattern = commandPattern
            body = buildBlock()
        }
        assertSame(handleClause, commandPattern.patternTreeOwner)
    }

    private fun newIntegerInitializer() = buildLiteralExpression {
        kind = CfirLiteralKind.INT
        value = 1
    }

    @Test
    fun inPlacePatternTransformationReconcilesBindingOwnersAndRejectsInitializerMutation() {
        val session = createTestSession()
        val file = createCjFile("patternBindingInPlaceTransform", SOURCE).toCfirFile(session)
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val owner = function.body!!.statements.filterIsInstance<CfirPatternVariable>().single()
        val oldBinding = (owner.pattern as CfirBindingPattern).bindingVariable!!
        val initializer = requireNotNull(owner.initializer)
        val newBinding = buildPatternBindingVariableCopy(oldBinding) {
            this.symbol = CfirPatternBindingSymbol(oldBinding.symbol.callableId)
        }

        owner.transformPattern(
            object : CfirDefaultTransformer<Unit>() {
                override fun transformPatternBindingVariable(
                    patternBindingVariable: CfirPatternBindingVariable,
                    data: Unit,
                ): CfirPatternBindingVariable =
                    if (patternBindingVariable === oldBinding) newBinding
                    else super.transformPatternBindingVariable(patternBindingVariable, data)
            },
            Unit,
        )

        assertNull(oldBinding.patternTreeOwner, "An in-place replacement must detach the removed binding.")
        assertNull(oldBinding.initializer, "A detached binding must not retain its former initializer.")
        assertSame(owner, newBinding.patternTreeOwner, "The replacement binding must be attached to its owner.")
        assertSame(initializer, newBinding.initializer)

        val directReplacement = buildPatternBindingVariableCopy(newBinding) {
            this.symbol = CfirPatternBindingSymbol(newBinding.symbol.callableId)
        }
        (owner.pattern as CfirBindingPattern).transformBindingVariable(
            object : CfirDefaultTransformer<Unit>() {
                override fun transformPatternBindingVariable(
                    patternBindingVariable: CfirPatternBindingVariable,
                    data: Unit,
                ): CfirPatternBindingVariable =
                    if (patternBindingVariable === newBinding) directReplacement
                    else super.transformPatternBindingVariable(patternBindingVariable, data)
            },
            Unit,
        )
        assertNull(newBinding.patternTreeOwner, "A direct pattern child transform must detach its former binding.")
        assertSame(owner, directReplacement.patternTreeOwner, "A direct pattern child transform must attach its replacement.")
        assertSame(initializer, directReplacement.initializer)

        assertThrows(IllegalStateException::class.java) {
            directReplacement.replaceInitializer(null)
        }
        var initializerTransformVisits = 0
        val sideEffectingTransformer = object : CfirDefaultTransformer<Unit>() {
            override fun <E : CfirElement> transformElement(element: E, data: Unit): E {
                if (element === initializer) initializerTransformVisits++
                return super.transformElement(element, data)
            }
        }
        assertThrows(IllegalStateException::class.java) {
            directReplacement.transformInitializer(sideEffectingTransformer, Unit)
        }
        assertEquals(0, initializerTransformVisits, "A rejected initializer transform must not invoke its transformer.")
        assertSame(initializer, directReplacement.initializer, "Rejected mutation must leave the derived initializer unchanged.")
    }

    private fun assertRawConstructionAlias(file: CfirFile) {
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        val patternVariable = function.body!!.statements.filterIsInstance<CfirPatternVariable>().single()
        val bindingVariable = (patternVariable.pattern as CfirBindingPattern).bindingVariable!!
        assertSame(
            patternVariable.initializer,
            bindingVariable.initializer,
            "A built CFIR owner must establish its binding initializer alias before body resolution.",
        )
    }

    private fun buildLightTreeCfirFile(name: String, source: String, session: CfirSession): CfirFile {
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val lightTree = CangJieLightParser.parse(builder, languageModuleName = "")
        val built = LightTree2Cfir(session, session.cangjieScopeProvider).buildCfirFileWithSurfaces(
            lightTree,
            CjInMemoryTextSourceFile("$name.cj", null, source),
            source.toSourceLinesMapping(),
        )
        val file = built.first
        val pre = buildPreMacroRawFiles(session, listOf(file), listOf(built.second))
        val result = MacroConstructionService.Identity.expandWithDefaultContext(
            pre = pre,
            mode = MacroConstructionService.Mode.STRICT,
        ) as MacroConstructionResult.Success
        recordExpandedRawFilesOnce(session.cfirProvider as CfirProviderImpl, result.recordableFiles, result.registry)
        return file
    }

    companion object {
        private val SOURCE = """
            func bindingInitializer(): Int64 {
                let value: Int64 = 42
                return value
            }
        """.trimIndent()
    }
}
