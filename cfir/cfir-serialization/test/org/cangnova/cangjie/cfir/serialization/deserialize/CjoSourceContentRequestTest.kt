@file:OptIn(ExperimentalUnsignedTypes::class)

package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.*
import PackageFormat.Package
import com.google.flatbuffers.FlatBufferBuilder
import org.cangnova.cangjie.cfir.serialization.cjo.PackageIndex
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Base64

/**
 * 原保存槽的内容授权、类型绑定需求与增量 occurrence 证明；不创建 CFIR 或语言诊断期待。
 * K=3c7a5041：IrDeclarationDeserializer 的 body/initializer guard、原 function parent scope；
 * N=14276740：ASTLoader 的参数/注解槽、ExprLoader 的 F/Block、ReferenceLoader 的类型与 refLoop。
 * 详细合同见 r26-content-request-gatekeeper、selected-source-owner-context-mapper、context-registry-revision-gatekeeper。
 * 合成字节只表达明确的 wire 拥有/绑定结构；官方两个增量案例消费已有原字节及其 SHA。
 */
class CjoSourceContentRequestTest {
    @Test fun nonzeroInitializerSelectsOnlyItsSavedExpression() {
        val fixture = slots()
        val root = CjoSourceContentRoot.Initializer(fixture.variable)
        val selection = request(fixture.headers, root)
        assertSelectedExpression(selection, root, fixture.initializer,
            CjoSourceOwningParent.Declaration(fixture.variable))
        assertSame(fixture.headers.declarations.getValue(fixture.variable),
            selection.borrowedContexts.declarations.getValue(fixture.variable))
        assertTrue(selection.borrowedContexts.functions.isEmpty())
    }

    @Test fun nonzeroDefaultBorrowsOriginalSignatureWithoutSelectingBody() {
        val fixture = slots()
        val root = CjoSourceContentRoot.ParameterDefault(fixture.parameter)
        val selection = request(fixture.headers, root)
        assertSelectedExpression(selection, root, fixture.default,
            CjoSourceOwningParent.Declaration(fixture.parameter))
        val identity = fixture.headers.functionByDeclaration.getValue(fixture.function)
        val scope = selection.ownedContent.expressions.getValue(fixture.default).scope
        assertEquals(identity, scope.function)
        assertEquals(CjoSourceContext.Function(identity), scope.declarationContext)
        assertSame(fixture.headers.functions.getValue(identity), selection.borrowedContexts.functions.getValue(identity))
        assertFalse(fixture.body in selection.ownedContent.expressions)
        assertFalse(fixture.annotation in selection.ownedContent.expressions)
    }

    @Test fun annotationArgumentRetainsOrdinalAndActualConstructorBinding() {
        val fixture = slots()
        val root = CjoSourceContentRoot.AnnotationArgument(fixture.function, 0, 0)
        val selection = request(fixture.headers, root)
        assertSelectedExpression(selection, root, fixture.annotation,
            CjoSourceOwningParent.AnnotationArgument(fixture.function, 0, 0))
        val binding = selection.declarationBindings.single {
            it.site == CjoSourceBindingSite.Declaration(fixture.function, CjoSourceBindingSlot.ANNOTATION_TARGET, 0)
        }
        assertEquals(PackageIndex.CURRENT.value, binding.target.pkgId)
        assertEquals(fixture.annotationConstructor, binding.target.index)
        assertTrue(fixture.annotationConstructor in selection.borrowedContexts.declarations)
        assertFalse(fixture.annotationConstructor in selection.ownedContent.declarations)
        assertFalse(fixture.default in selection.ownedContent.expressions)
    }

    @Test fun annotationOrdinalMustIdentifyAnExistingNonzeroSavedSlot() {
        val fixture = slots()
        assertThrows(IllegalArgumentException::class.java) {
            request(fixture.headers, CjoSourceContentRoot.AnnotationArgument(fixture.function, 1, 0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            request(fixture.headers, CjoSourceContentRoot.AnnotationArgument(fixture.function, 0, 1))
        }
        val wire = Wire()
        val host = wire.function(0u, annotations = intArrayOf(wire.annotation(0u)))
        assertThrows(IllegalArgumentException::class.java) {
            request(CjoSourceOwnershipPlan.headers(wire.finish()), CjoSourceContentRoot.AnnotationArgument(host, 0, 0))
        }
    }

    @Test fun absentInitializerAndDefaultDoNotInventContent() {
        val wire = Wire()
        val parameter = wire.parameter(0u)
        val function = wire.function(0u, uintArrayOf(parameter))
        val variable = wire.variable(0u)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        for (root in listOf(CjoSourceContentRoot.Initializer(variable), CjoSourceContentRoot.ParameterDefault(parameter))) {
            val selection = request(headers, root)
            assertEquals(listOf(root), selection.roots)
            assertTrue(selection.ownedContent.expressions.isEmpty())
            assertTrue(selection.selectedBodies.isEmpty())
        }
        assertTrue(headers.functionByDeclaration.containsKey(function))
    }

    @Test fun twoBodylessFunctionsKeepDistinctOriginalFIdentities() {
        val wire = Wire()
        val first = wire.function(0u)
        val second = wire.function(0u)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        val identities = listOf(first, second).map(headers.functionByDeclaration::getValue)
        assertNotEquals(identities[0], identities[1])
        for (identity in identities) {
            val selection = request(headers, CjoSourceContentRoot.FunctionBody(identity))
            assertEquals(setOf(identity), selection.ownedContent.functions.keys)
            assertSame(headers.functions.getValue(identity), selection.ownedContent.functions.getValue(identity))
            assertTrue(selection.selectedBodies.isEmpty())
            assertTrue(selection.ownedContent.expressions.isEmpty())
        }
    }

    @Test fun unselectedDamagedBodyFieldsDoNotEnterTheRequestRange() {
        for (damage in BodyDamage.entries) {
            val wire = Wire()
            val goodBody = wire.block()
            val damaged = when (damage) {
                BodyDamage.KIND -> wire.expression(UShort.MAX_VALUE)
                BodyDamage.INFO -> wire.expression(ExprKind.Block, infoType = ExprInfo.BlockInfo)
                BodyDamage.OPERAND -> wire.block(uintArrayOf(999u))
                BodyDamage.TYPE -> wire.block(type = 999u)
                BodyDamage.TARGET -> wire.block(uintArrayOf(wire.reference(target = 999u)))
                BodyDamage.PATTERN -> {
                    val pattern = wire.pattern(PatternKind.WildcardPattern, uintArrayOf(999u))
                    val info = TryInfo.createTryInfo(wire.builder, 0,
                        TryInfo.createPatternsVector(wire.builder, intArrayOf(pattern)))
                    val tryBody = wire.block()
                    val catchBody = wire.block()
                    val tryExpression = wire.expression(ExprKind.TryExpr, uintArrayOf(tryBody, catchBody, 0u),
                        infoType = ExprInfo.TryInfo, info = info)
                    wire.block(uintArrayOf(tryExpression))
                }
            }
            val good = wire.function(goodBody)
            val bad = wire.function(damaged)
            val pkg = wire.finish()
            val headers = CjoSourceOwnershipPlan.headers(pkg)
            val goodIdentity = headers.functionByDeclaration.getValue(good)
            val selection = request(headers, CjoSourceContentRoot.FunctionBody(goodIdentity))
            assertEquals(setOf(goodBody), selection.ownedContent.expressions.keys, damage.toString())
            assertEquals(setOf(goodIdentity), selection.selectedBodies)
            assertFalse(damaged in selection.registeredHeaders.expressionContexts)
            assertFalse(headers.functionByDeclaration.getValue(bad) in selection.borrowedContexts.functions)
            assertThrows(RuntimeException::class.java, {
                request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(bad)))
            }, "显式请求才进入损坏的 $damage 范围")
            when (damage) {
                BodyDamage.TYPE, BodyDamage.PATTERN -> {
                    // create 只建立拥有闭包；原 type 槽必须保留，不能在这里冒充 typed phase。
                    val ownership = CjoSourceOwnershipPlan.create(pkg)
                    assertEquals(headers.functionByDeclaration.getValue(bad), ownership.functionByDeclaration.getValue(bad))
                    assertTrue(damaged in ownership.expressions)
                    if (damage == BodyDamage.TYPE) {
                        assertEquals(999u, ownership.expressions.getValue(damaged).wire.type)
                        assertEquals(CjoSourceOwningParent.Function(headers.functionByDeclaration.getValue(bad)),
                            ownership.expressions.getValue(damaged).parent)
                    } else {
                        val pattern = ownership.patterns.values.single()
                        assertEquals(PatternKind.WildcardPattern, pattern.wire.kind)
                        assertEquals(1, pattern.wire.typesLength)
                        assertEquals(999u, pattern.wire.types(0))
                        val parent = pattern.parent as CjoSourceOwningParent.Expression
                        assertEquals(ExprKind.TryExpr, ownership.expressions.getValue(parent.reference).wire.kind)
                    }
                }
                else -> assertThrows(RuntimeException::class.java) { CjoSourceOwnershipPlan.create(pkg) }
            }
            assertTrue(headers.expressions.isEmpty())
            assertTrue(headers.expressionContexts.isEmpty(), "失败不能污染原 header proof")
        }
    }

    @Test fun unselectedDamagedDefaultAndAnnotationRemainOutsideBodyRequest() {
        for (damagedSlot in listOf(Slot.DEFAULT, Slot.ANNOTATION)) {
            val fixture = slots(damagedSlot)
            val identity = fixture.headers.functionByDeclaration.getValue(fixture.function)
            val selection = request(fixture.headers, CjoSourceContentRoot.FunctionBody(identity))
            assertEquals(setOf(fixture.body), selection.ownedContent.expressions.keys)
            assertFalse(fixture.default in selection.registeredHeaders.expressionContexts)
            assertFalse(fixture.annotation in selection.registeredHeaders.expressionContexts)
            val root = if (damagedSlot == Slot.DEFAULT) CjoSourceContentRoot.ParameterDefault(fixture.parameter)
                else CjoSourceContentRoot.AnnotationArgument(fixture.function, 0, 0)
            assertThrows(IllegalStateException::class.java) { request(fixture.headers, root) }
        }
    }

    @Test fun varPatternRequiresExactlyOneSavedVariableSlot() {
        for (count in listOf(0, 2)) {
            val wire = Wire()
            val variables = UIntArray(count) { wire.variable(0u, topLevel = false) }
            val pattern = wire.pattern(PatternKind.VarPattern, uintArrayOf(wire.unitType), expressions = variables)
            val function = wire.function(wire.block(uintArrayOf(wire.letPattern(pattern))))
            val headers = CjoSourceOwnershipPlan.headers(wire.finish())
            assertThrows(IllegalStateException::class.java, {
                request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function)))
            }, "N LoadVarPattern 要求恰一个原 d 槽，不能接受 $count 个")
        }
    }

    @Test fun varPatternBindingSlotRejectsParameterAndFunctionDeclarations() {
        for (kind in listOf(DeclKind.FuncParam, DeclKind.FuncDecl)) {
            val wire = Wire()
            val wrong = if (kind == DeclKind.FuncParam) wire.parameter(0u) else wire.function(0u, topLevel = false)
            val pattern = wire.pattern(PatternKind.VarPattern, uintArrayOf(wire.unitType), expressions = uintArrayOf(wrong))
            val function = wire.function(wire.block(uintArrayOf(wire.letPattern(pattern))))
            val headers = CjoSourceOwnershipPlan.headers(wire.finish())
            assertThrows(IllegalStateException::class.java) {
                request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function)))
            }
        }
    }

    @Test fun singleVarPatternPreservesItsActualVariableAndPatternOwner() {
        val wire = Wire()
        val variable = wire.variable(0u, topLevel = false)
        val pattern = wire.pattern(PatternKind.VarPattern, uintArrayOf(wire.unitType), expressions = uintArrayOf(variable))
        val destructor = wire.letPattern(pattern)
        val function = wire.function(wire.block(uintArrayOf(destructor)))
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        val selection = request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function)))
        val selectedPattern = selection.ownedContent.patterns.values.single()
        val binding = selection.ownedContent.declarations.getValue(variable)
        assertEquals(DeclKind.VarDecl, binding.wire.kind)
        assertTrue(binding.isPatternBinding)
        assertEquals(CjoSourceOwningParent.Pattern(selectedPattern.position), binding.parent)
        assertEquals(CjoSourceOwningParent.Expression(destructor), selectedPattern.parent)
        assertEquals(variable, selectedPattern.wire.exprs(0))
    }

    @Test fun propertyGetterAndSetterSlotsRejectNonFunctionDeclarations() {
        for (getter in listOf(true, false)) {
            val wire = Wire()
            val variable = wire.variable(0u, topLevel = false)
            val info = PropInfo.createPropInfo(wire.builder, false, true,
                PropInfo.createSettersVector(wire.builder, if (getter) uintArrayOf() else uintArrayOf(variable)),
                PropInfo.createGettersVector(wire.builder, if (getter) uintArrayOf(variable) else uintArrayOf()))
            wire.declaration(DeclKind.PropDecl, DeclInfo.PropInfo, info)
            assertThrows(IllegalStateException::class.java, {
                CjoSourceOwnershipPlan.headers(wire.finish())
            }, "N 的 getter/setter 槽必须 LoadDecl<FuncDecl>")
        }
    }

    @Test fun actualGetterAndSetterHeadersKeepTheirOriginalPropertyOwner() {
        val wire = Wire()
        val getter = wire.function(0u, topLevel = false)
        val setter = wire.function(0u, topLevel = false)
        val info = PropInfo.createPropInfo(wire.builder, false, true,
            PropInfo.createSettersVector(wire.builder, uintArrayOf(setter)),
            PropInfo.createGettersVector(wire.builder, uintArrayOf(getter)))
        val property = wire.declaration(DeclKind.PropDecl, DeclInfo.PropInfo, info)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        for (reference in listOf(getter, setter)) {
            val entry = headers.declarations.getValue(reference)
            assertEquals(DeclKind.FuncDecl, entry.wire.kind)
            assertEquals(property, entry.accessorOwner)
            assertEquals(CjoSourceOwningParent.Declaration(property), entry.parent)
            assertEquals(reference, headers.functions.getValue(headers.functionByDeclaration.getValue(reference)).declaration)
        }
        assertTrue(headers.expressions.isEmpty())
    }

    @Test fun builtinAnnotationArgumentRequiresLiteralWithoutReadingUnselectedArgument() {
        for (kind in listOf(AnnoKind.Frozen, AnnoKind.Deprecated)) {
            val wire = Wire()
            val nonliteral = wire.block()
            val body = wire.block()
            val host = wire.function(body, annotations = intArrayOf(wire.annotation(nonliteral, kind = kind)))
            val headers = CjoSourceOwnershipPlan.headers(wire.finish())
            val bodySelection = request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(host)))
            assertEquals(setOf(body), bodySelection.ownedContent.expressions.keys)
            assertFalse(nonliteral in bodySelection.registeredHeaders.expressionContexts)
            assertThrows(IllegalStateException::class.java) {
                request(headers, CjoSourceContentRoot.AnnotationArgument(host, 0, 0))
            }
        }
    }

    @Test fun builtinAnnotationLiteralDoesNotConsumeItsUnusedTarget() {
        for (kind in listOf(AnnoKind.Frozen, AnnoKind.Deprecated)) {
            val wire = Wire()
            val literal = wire.literal()
            val body = wire.block()
            val host = wire.function(body, annotations = intArrayOf(wire.annotation(literal, target = 999u, kind = kind)))
            val headers = CjoSourceOwnershipPlan.headers(wire.finish())
            val bodySelection = request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(host)))
            assertEquals(setOf(body), bodySelection.ownedContent.expressions.keys)
            assertTrue(bodySelection.declarationBindings.isEmpty())
            val root = CjoSourceContentRoot.AnnotationArgument(host, 0, 0)
            val selected = request(headers, root)
            assertSelectedExpression(selected, root, literal, CjoSourceOwningParent.AnnotationArgument(host, 0, 0))
            assertEquals(ExprKind.LitConstExpr, selected.ownedContent.expressions.getValue(literal).wire.kind)
            assertTrue(selected.declarationBindings.isEmpty(), "非 CUSTOM 的 annotation 不消费 target FullId")
            assertEquals(setOf(host), selected.borrowedContexts.declarations.keys)
        }
    }

    @Test fun generalAnnotationMaySelectNonliteralWithoutBindingIgnoredTarget() {
        val wire = Wire()
        val nonliteral = wire.block()
        val host = wire.function(0u, annotations = intArrayOf(wire.annotation(nonliteral, target = 2u, kind = AnnoKind.Annotation)))
        val constructor = wire.function(0u, topLevel = false, attributes = ulongArrayOf(1uL shl 31))
        val classType = wire.classType(3u)
        val annotationClass = wire.classDeclaration(classType, members = uintArrayOf(constructor), isAnnotation = true)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        assertTrue(constructor in headers.declarations)
        assertTrue(annotationClass in headers.declarations)
        val annotation = checkNotNull(headers.declarations.getValue(host).wire.annotations(0))
        assertEquals(AnnoKind.Annotation, annotation.kind)
        assertEquals(constructor, checkNotNull(annotation.target).index)
        val root = CjoSourceContentRoot.AnnotationArgument(host, 0, 0)
        val selection = request(headers, root)
        assertSelectedExpression(selection, root, nonliteral, CjoSourceOwningParent.AnnotationArgument(host, 0, 0))
        assertTrue(selection.declarationBindings.none {
            (it.site as? CjoSourceBindingSite.Declaration)?.slot == CjoSourceBindingSlot.ANNOTATION_TARGET
        })
        assertEquals(setOf(host), selection.borrowedContexts.declarations.keys)
        assertFalse(constructor in selection.borrowedContexts.declarations)
        assertFalse(annotationClass in selection.borrowedContexts.declarations)
        assertFalse(headers.functionByDeclaration.getValue(constructor) in selection.borrowedContexts.functions)
        assertFalse(constructor in selection.ownedContent.declarations)
        assertFalse(classType in selection.types)
    }

    @Test fun expressionTypeAloneRequiresOriginalClassifierHeader() {
        val wire = Wire()
        val classType = wire.classType(2u)
        val expression = wire.expression(ExprKind.WildcardExpr, type = classType)
        val variable = wire.variable(expression)
        val classifier = wire.classDeclaration(type = classType)
        assertEquals(2u, classifier)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        val selection = request(headers, CjoSourceContentRoot.Initializer(variable))
        assertEquals(setOf(wire.unitType, classType), selection.types.keys)
        assertEquals(setOf(variable, classifier), selection.borrowedContexts.declarations.keys)
        assertTrue(selection.ownedContent.declarations.isEmpty())
        val binding = selection.declarationBindings.single { it.expectedKind == DeclKind.ClassDecl }
        assertEquals(CjoSourceBindingSite.Type(classType, CjoSourceBindingSlot.CLASSIFIER), binding.site)
        assertEquals(classifier, binding.target.index)
    }

    @Test fun genericTypeRequiresItsOriginalTPAndUpperBoundClassifier() {
        val fixture = genericTypes(withConstraints = false)
        val selection = request(fixture.headers, CjoSourceContentRoot.FunctionBody(fixture.identity))
        assertTrue(fixture.genericType in selection.types)
        assertTrue(fixture.boundType in selection.types)
        val tp = selection.declarationBindings.single { it.expectedKind == DeclKind.GenericParamDecl }
        assertEquals(fixture.parameter, tp.target.index)
        assertEquals(CjoSourceBindingSite.Type(fixture.genericType, CjoSourceBindingSlot.CLASSIFIER), tp.site)
        assertEquals(CjoSourceOwningParent.Declaration(fixture.function),
            selection.borrowedContexts.declarations.getValue(fixture.parameter).parent)
        assertTrue(selection.typeBindings.any {
            it.reference == fixture.boundType && it.site ==
                CjoSourceBindingSite.Type(fixture.genericType, CjoSourceBindingSlot.UPPER_BOUND, 0)
        })
        assertFalse(fixture.classifier in selection.ownedContent.declarations)
    }

    @Test fun constraintsRequireSubjectAndEveryUpperWithoutReadingBodies() {
        val fixture = genericTypes(withConstraints = true)
        val selection = request(fixture.headers, CjoSourceContentRoot.FunctionBody(fixture.identity))
        assertTrue(selection.typeBindings.any {
            it.reference == fixture.genericType && it.site ==
                CjoSourceBindingSite.Declaration(fixture.function, CjoSourceBindingSlot.CONSTRAINT_SUBJECT, 0)
        })
        assertTrue(selection.typeBindings.any {
            it.reference == fixture.boundType && it.site ==
                CjoSourceBindingSite.Declaration(fixture.function, CjoSourceBindingSlot.CONSTRAINT_UPPER, 0, 0)
        })
        assertEquals(setOf(fixture.identity), selection.selectedBodies)
        assertEquals(fixture.parameter,
            selection.declarationBindings.single { it.expectedKind == DeclKind.GenericParamDecl }.target.index)
    }

    @Test fun instantiationAndMatchedParentHaveSeparateTypedSites() {
        val wire = Wire()
        val instantiated = wire.classType(2u)
        val matched = wire.classType(3u)
        val reference = wire.reference(instantiation = uintArrayOf(instantiated), matchedParent = matched)
        val variable = wire.variable(reference)
        val first = wire.classDeclaration(type = instantiated)
        val second = wire.classDeclaration(type = matched)
        val selection = request(CjoSourceOwnershipPlan.headers(wire.finish()), CjoSourceContentRoot.Initializer(variable))
        val sites = selection.typeBindings.mapNotNull { binding ->
            (binding.site as? CjoSourceBindingSite.Expression)?.takeIf { it.occurrence.reference == reference }
                ?.let { it.slot to binding.reference }
        }
        assertTrue(CjoSourceBindingSlot.INSTANTIATION to instantiated in sites)
        assertTrue(CjoSourceBindingSlot.MATCHED_PARENT to matched in sites)
        assertEquals(setOf(variable, first, second), selection.borrowedContexts.declarations.keys)
    }

    @Test fun exceptionAndCommandPatternTailTypesRemainIndependentDemands() {
        for (kind in listOf(PatternKind.ExceptTypePattern, PatternKind.CommandTypePattern)) {
            val wire = Wire()
            val common = wire.classType(2u)
            val tail = wire.classType(3u)
            val nested = wire.pattern(PatternKind.WildcardPattern, uintArrayOf(common))
            val pattern = wire.pattern(kind, uintArrayOf(common, tail), intArrayOf(nested))
            val info = TryInfo.createTryInfo(wire.builder, 0,
                TryInfo.createPatternsVector(wire.builder, intArrayOf(pattern)))
            val tryBody = wire.block()
            val catchBody = wire.block()
            val tryExpression = wire.expression(ExprKind.TryExpr, uintArrayOf(tryBody, catchBody, 0u),
                infoType = ExprInfo.TryInfo, info = info)
            val body = wire.block(uintArrayOf(tryExpression))
            val function = wire.function(body)
            val commonClass = wire.classDeclaration(type = common)
            val tailClass = wire.classDeclaration(type = tail, inherited = uintArrayOf(common))
            val headers = CjoSourceOwnershipPlan.headers(wire.finish())
            val selection = request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function)))
            val selectedPattern = selection.ownedContent.patterns.values.single { it.wire.kind == kind }
            assertEquals(2, selectedPattern.wire.typesLength)
            assertTrue(selection.typeBindings.any {
                it.reference == tail && it.site == CjoSourceBindingSite.Pattern(selectedPattern.position, 1)
            })
            assertTrue(commonClass in selection.borrowedContexts.declarations)
            assertTrue(tailClass in selection.borrowedContexts.declarations)
            assertTrue(tail in selection.types)
        }
    }

    @Test fun unknownTypeAndWrongClassifierKindAreRejected() {
        val badIndex = Wire()
        val expression = badIndex.expression(ExprKind.WildcardExpr, type = 999u)
        val variable = badIndex.variable(expression)
        assertThrows(IllegalArgumentException::class.java) {
            request(CjoSourceOwnershipPlan.headers(badIndex.finish()), CjoSourceContentRoot.Initializer(variable))
        }
        val wrongKind = Wire()
        val type = wrongKind.classType(2u)
        val value = wrongKind.expression(ExprKind.WildcardExpr, type = type)
        val host = wrongKind.variable(value)
        wrongKind.function(0u)
        assertThrows(IllegalStateException::class.java) {
            request(CjoSourceOwnershipPlan.headers(wrongKind.finish()), CjoSourceContentRoot.Initializer(host))
        }
    }

    @Test fun zeroTypeSlotsRemainDemandsWithoutBecomingUnit() {
        val wire = Wire()
        val expression = wire.expression(ExprKind.WildcardExpr, type = 0u)
        val variable = wire.variable(expression, type = 0u)
        val selection = request(CjoSourceOwnershipPlan.headers(wire.finish()), CjoSourceContentRoot.Initializer(variable))
        assertTrue(selection.types.isEmpty(), "0 没有 allTypes 记录，不能补成已保存 Unit")
        assertTrue(selection.typeBindings.any {
            it.reference == 0u && it.site == CjoSourceBindingSite.Declaration(variable, CjoSourceBindingSlot.TYPE)
        })
        assertTrue(selection.typeBindings.any {
            it.reference == 0u && (it.site as? CjoSourceBindingSite.Expression)?.let { site ->
                site.occurrence.reference == expression && site.slot == CjoSourceBindingSlot.TYPE
            } == true
        })
        assertEquals(0u, selection.ownedContent.expressions.getValue(expression).wire.type)
    }

    @Test fun primitiveDoesNotConsumeExtraneousTypeArgumentSlots() {
        val wire = Wire()
        val primitive = wire.type(TypeKind.Int64, typeArguments = uintArrayOf(0u, 999u))
        val expression = wire.expression(ExprKind.WildcardExpr, type = primitive)
        val variable = wire.variable(expression, type = 0u)
        val selection = request(CjoSourceOwnershipPlan.headers(wire.finish()), CjoSourceContentRoot.Initializer(variable))
        assertEquals(setOf(primitive), selection.types.keys)
        assertFalse(selection.typeBindings.any {
            (it.site as? CjoSourceBindingSite.Type)?.let { site ->
                site.reference == primitive && site.slot == CjoSourceBindingSlot.TYPE_ARGUMENT
            } == true
        })
    }

    @Test fun consumedTupleZeroArgumentRemainsAnInvalidTypeDemand() {
        val wire = Wire()
        val tuple = wire.type(TypeKind.Tuple, typeArguments = uintArrayOf(0u))
        val expression = wire.expression(ExprKind.WildcardExpr, type = tuple)
        val variable = wire.variable(expression, type = 0u)
        val selection = request(CjoSourceOwnershipPlan.headers(wire.finish()), CjoSourceContentRoot.Initializer(variable))
        assertEquals(setOf(tuple), selection.types.keys)
        assertTrue(selection.typeBindings.any {
            it.reference == 0u && it.site == CjoSourceBindingSite.Type(tuple, CjoSourceBindingSlot.TYPE_ARGUMENT, 0)
        })
    }

    @Test fun valueArrayDoesNotConsumeUnusedInitializerFunctionTarget() {
        val wire = Wire()
        val type = wire.type(TypeKind.VArray, SemaTyInfo.ArrayTyInfo,
            ArrayTyInfo.createArrayTyInfo(wire.builder, 1L), uintArrayOf(wire.unitType))
        val unusedTarget = FullId.createFullId(wire.builder, PackageIndex.CURRENT.value, 0, 999u)
        val info = ArrayInfo.createArrayInfo(wire.builder, unusedTarget, true)
        val element = wire.literal()
        val expression = wire.expression(ExprKind.ArrayLit, uintArrayOf(element), type, ExprInfo.ArrayInfo, info)
        val variable = wire.variable(expression, type = type)
        val selection = request(CjoSourceOwnershipPlan.headers(wire.finish()), CjoSourceContentRoot.Initializer(variable))
        assertEquals(setOf(type, wire.unitType), selection.types.keys)
        assertTrue(selection.declarationBindings.isEmpty(), "N 的 VArray 分支不消费 initFunc")
        assertEquals(setOf(expression, element), selection.ownedContent.expressions.keys)
    }

    @Test fun returnInGlobalInitializerDoesNotAcquireUnrelatedFunctionDomain() = assertNonBodyReturn(Slot.INITIALIZER)
    @Test fun returnInParameterDefaultIsNotAFunctionBodyReturn() = assertNonBodyReturn(Slot.DEFAULT)
    @Test fun returnInAnnotationArgumentIsNotAFunctionBodyReturn() = assertNonBodyReturn(Slot.ANNOTATION)

    @Test fun lambdaBodyInsideInitializerHasItsOwnReturnFunction() {
        val wire = Wire()
        val value = wire.literal()
        val returned = wire.expression(ExprKind.ReturnExpr, uintArrayOf(value))
        val body = wire.block(uintArrayOf(returned))
        val lambda = wire.lambda(body)
        val variable = wire.variable(lambda)
        val selection = request(CjoSourceOwnershipPlan.headers(wire.finish()), CjoSourceContentRoot.Initializer(variable))
        val identity = selection.ownedContent.functionByLambda.getValue(lambda)
        assertNull(selection.ownedContent.expressions.getValue(lambda).scope.returnFunction)
        assertEquals(identity, selection.ownedContent.expressions.getValue(returned).scope.returnFunction)
        assertEquals(setOf(identity), selection.selectedBodies)
        assertEquals(CjoSourceOwningParent.Function(identity), selection.ownedContent.expressions.getValue(body).parent)
    }

    @Test fun bodyIfInitializerReturnAndIncrementalRequestRetainOriginalFunctionDomain() {
        val wire = Wire()
        val nothing = wire.type(TypeKind.Nothing)
        val returnedValue = wire.literal()
        val returned = wire.expression(ExprKind.ReturnExpr, uintArrayOf(returnedValue), type = nothing)
        val thenBranch = wire.block(uintArrayOf(returned), type = nothing)
        val elseValue = wire.literal()
        val elseBranch = wire.block(uintArrayOf(elseValue))
        val condition = wire.literal(boolean = true)
        val initializer = wire.expression(ExprKind.IfExpr, uintArrayOf(condition, thenBranch, elseBranch))
        val variable = wire.variable(initializer, topLevel = false)
        val body = wire.block(uintArrayOf(variable), booleanArrayOf(false))
        val function = wire.function(body)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        val identity = headers.functionByDeclaration.getValue(function)
        val bodyRoot = CjoSourceContentRoot.FunctionBody(identity)
        val first = request(headers, bodyRoot)
        assertEquals(identity, first.ownedContent.declarations.getValue(variable).scope.returnFunction)
        for (reference in listOf(initializer, thenBranch, returned)) {
            val entry = first.ownedContent.expressions.getValue(reference)
            assertEquals(identity, entry.scope.returnFunction)
            assertEquals(identity, entry.scope.function)
            assertEquals(CjoSourceContext.Declaration(variable), entry.scope.expressionContext)
        }
        assertEquals(CjoSourceOwningParent.Declaration(variable), first.ownedContent.expressions.getValue(initializer).parent)
        val root = CjoSourceContentRoot.Initializer(variable)
        val selected = request(first.registeredHeaders, root)
        assertEquals(setOf(initializer, condition, thenBranch, returned, returnedValue, elseBranch, elseValue),
            selected.ownedContent.expressions.keys)
        assertEquals(first.ownedContent.expressions.getValue(returned).scope,
            selected.ownedContent.expressions.getValue(returned).scope)
        assertEquals(identity, selected.ownedContent.expressions.getValue(returned).scope.returnFunction)
        assertTrue(selected.selectedBodies.isEmpty(), "保留原 Return 域不等于重新选择外 F.body")
        assertFalse(body in selected.ownedContent.expressions)
        assertStableProofs(selected, root)
        val again = request(selected.registeredHeaders, bodyRoot)
        assertEquals(identity, again.ownedContent.expressions.getValue(returned).scope.returnFunction)
        assertEquals(first.registeredHeaders.expressionContexts.getValue(returned),
            again.registeredHeaders.expressionContexts.getValue(returned))
    }

    @Test fun bodyInitializerLambdaRetainsOuterLexicalDomainAndOwnReturnDomain() {
        val wire = Wire()
        val value = wire.literal()
        val returned = wire.expression(ExprKind.ReturnExpr, uintArrayOf(value), type = wire.type(TypeKind.Nothing))
        val lambdaBody = wire.block(uintArrayOf(returned))
        val lambda = wire.lambda(lambdaBody)
        val variable = wire.variable(lambda, topLevel = false)
        val outerBody = wire.block(uintArrayOf(variable), booleanArrayOf(false))
        val outer = wire.function(outerBody)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        val outerIdentity = headers.functionByDeclaration.getValue(outer)
        val first = request(headers, CjoSourceContentRoot.FunctionBody(outerIdentity))
        val innerIdentity = first.ownedContent.functionByLambda.getValue(lambda)
        assertNotEquals(outerIdentity, innerIdentity)
        val lexical = first.ownedContent.functions.getValue(innerIdentity).lexicalScope
        assertEquals(outerIdentity, lexical.function)
        assertEquals(outerIdentity, lexical.returnFunction)
        assertEquals(CjoSourceContext.Function(outerIdentity), lexical.declarationContext)
        assertEquals(CjoSourceContext.Declaration(variable), lexical.expressionContext)
        assertEquals(outerIdentity, first.ownedContent.expressions.getValue(lambda).scope.returnFunction)
        assertEquals(innerIdentity, first.ownedContent.expressions.getValue(returned).scope.returnFunction)
        assertEquals(innerIdentity, first.ownedContent.expressions.getValue(lambdaBody).scope.function)
        assertEquals(setOf(outerIdentity, innerIdentity), first.selectedBodies)
        val root = CjoSourceContentRoot.Initializer(variable)
        val selected = request(first.registeredHeaders, root)
        assertEquals(innerIdentity, selected.ownedContent.functionByLambda.getValue(lambda))
        assertEquals(lexical, selected.ownedContent.functions.getValue(innerIdentity).lexicalScope)
        assertEquals(outerIdentity, selected.ownedContent.expressions.getValue(lambda).scope.returnFunction)
        assertEquals(innerIdentity, selected.ownedContent.expressions.getValue(returned).scope.returnFunction)
        assertEquals(setOf(innerIdentity), selected.selectedBodies)
        assertFalse(outerBody in selected.ownedContent.expressions)
        assertStableProofs(selected, root)
    }

    @Test fun realWhileAndDoWhileLoopsBindJumpInTheSameF() {
        for (kind in listOf(ExprKind.WhileExpr, ExprKind.DoWhileExpr)) {
            val wire = Wire()
            val condition = wire.literal(boolean = true)
            val jump = wire.jump(4u)
            val loopBody = wire.block(uintArrayOf(jump))
            val operands = if (kind == ExprKind.WhileExpr) uintArrayOf(condition, loopBody) else uintArrayOf(loopBody, condition)
            val loop = wire.expression(kind, operands)
            assertEquals(4u, loop)
            val body = wire.block(uintArrayOf(loop))
            val function = wire.function(body)
            val headers = CjoSourceOwnershipPlan.headers(wire.finish())
            val identity = headers.functionByDeclaration.getValue(function)
            val selection = request(headers, CjoSourceContentRoot.FunctionBody(identity))
            val binding = selection.expressionBindings.single { it.source.reference == jump }
            assertEquals(CjoSourceExpressionBindingRole.REF_LOOP, binding.role)
            assertEquals(loop, binding.target)
            assertTrue(binding.owned)
            assertEquals(identity, binding.source.scope.returnFunction)
            assertTrue(binding.targetContexts.all { it.scope.returnFunction == identity })
            assertTrue(selection.borrowedBindings.isEmpty())
        }
    }

    @Test fun jumpCannotTargetANonLoopEvenWhenTargetIsOwned() {
        val wire = Wire()
        val notLoop = wire.literal()
        val jump = wire.jump(notLoop)
        val body = wire.block(uintArrayOf(notLoop, jump))
        val function = wire.function(body)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        assertThrows(IllegalStateException::class.java) {
            request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function)))
        }
    }

    @Test fun jumpInsideNestedFDoesNotAcquireOuterLoopPermission() {
        val wire = Wire()
        val condition = wire.literal(boolean = true)
        val jump = wire.jump(6u)
        val nestedBody = wire.block(uintArrayOf(jump))
        val lambda = wire.lambda(nestedBody)
        val loopBody = wire.block(uintArrayOf(lambda))
        val loop = wire.expression(ExprKind.WhileExpr, uintArrayOf(condition, loopBody))
        assertEquals(6u, loop)
        val body = wire.block(uintArrayOf(loop))
        val function = wire.function(body)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        assertThrows(IllegalStateException::class.java) {
            request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function)))
        }
    }

    @Test fun nullReturnDomainsDoNotPermitJumpBetweenDifferentInitializerFunctions() {
        val wire = Wire()
        val first = loopInitializer(wire)
        val foreignJump = wire.jump(first.loop)
        val foreignInitializer = wire.block(uintArrayOf(foreignJump))
        val firstVariable = wire.variable(first.initializer, topLevel = false)
        val secondVariable = wire.variable(foreignInitializer, topLevel = false)
        val firstDefault = wire.block(uintArrayOf(firstVariable), booleanArrayOf(false))
        val secondDefault = wire.block(uintArrayOf(secondVariable), booleanArrayOf(false))
        val firstParameter = wire.parameter(firstDefault)
        val secondParameter = wire.parameter(secondDefault)
        val firstFunction = wire.function(0u, uintArrayOf(firstParameter))
        val secondFunction = wire.function(0u, uintArrayOf(secondParameter))
        val pkg = wire.finish()
        val headers = CjoSourceOwnershipPlan.headers(pkg)
        val firstIdentity = headers.functionByDeclaration.getValue(firstFunction)
        val secondIdentity = headers.functionByDeclaration.getValue(secondFunction)
        assertNotEquals(firstIdentity, secondIdentity)
        assertFalse(firstVariable in headers.declarations)
        assertFalse(secondVariable in headers.declarations)
        // 两个 initializer 由各自 ParameterDefault 下行发现；原 F 有签名但没有 body Return 域。
        val ownership = CjoSourceOwnershipPlan.create(pkg, validateReferences = false)
        val loopScope = ownership.expressions.getValue(first.loop).scope
        val jumpScope = ownership.expressions.getValue(foreignJump).scope
        assertNull(loopScope.returnFunction)
        assertNull(jumpScope.returnFunction)
        assertEquals(firstIdentity, loopScope.function)
        assertEquals(secondIdentity, jumpScope.function)
        assertEquals(CjoSourceContext.Declaration(firstVariable), loopScope.expressionContext)
        assertEquals(CjoSourceContext.Declaration(secondVariable), jumpScope.expressionContext)
        assertEquals(CjoSourceOwningParent.Expression(firstDefault), ownership.declarations.getValue(firstVariable).parent)
        assertEquals(CjoSourceOwningParent.Expression(secondDefault), ownership.declarations.getValue(secondVariable).parent)
        assertThrows(IllegalStateException::class.java) {
            CjoSourceOwnershipPlan.request(headers, listOf(CjoSourceContentRoot.ParameterDefault(firstParameter),
                CjoSourceContentRoot.ParameterDefault(secondParameter)))
        }
    }

    @Test fun nullReturnAndNullFunctionDoNotPermitJumpBetweenDifferentInitializerHosts() {
        val wire = Wire()
        val first = loopInitializer(wire)
        val foreignJump = wire.jump(first.loop)
        val firstVariable = wire.variable(first.initializer, topLevel = false)
        val secondVariable = wire.variable(wire.block(uintArrayOf(foreignJump)), topLevel = false)
        val firstArgument = wire.block(uintArrayOf(firstVariable), booleanArrayOf(false))
        val secondArgument = wire.block(uintArrayOf(secondVariable), booleanArrayOf(false))
        val firstHost = wire.function(0u, annotations = intArrayOf(wire.annotation(firstArgument, kind = AnnoKind.Annotation)))
        val secondHost = wire.function(0u, annotations = intArrayOf(wire.annotation(secondArgument, kind = AnnoKind.Annotation)))
        val pkg = wire.finish()
        val headers = CjoSourceOwnershipPlan.headers(pkg)
        assertFalse(firstVariable in headers.declarations)
        assertFalse(secondVariable in headers.declarations)
        val ownership = CjoSourceOwnershipPlan.create(pkg, validateReferences = false)
        val loopScope = ownership.expressions.getValue(first.loop).scope
        val jumpScope = ownership.expressions.getValue(foreignJump).scope
        assertNull(loopScope.returnFunction)
        assertNull(jumpScope.returnFunction)
        assertNull(loopScope.function)
        assertNull(jumpScope.function)
        assertEquals(CjoSourceContext.Declaration(firstVariable), loopScope.expressionContext)
        assertEquals(CjoSourceContext.Declaration(secondVariable), jumpScope.expressionContext)
        assertEquals(CjoSourceOwningParent.Expression(firstArgument), ownership.declarations.getValue(firstVariable).parent)
        assertEquals(CjoSourceOwningParent.Expression(secondArgument), ownership.declarations.getValue(secondVariable).parent)
        assertThrows(IllegalStateException::class.java) {
            CjoSourceOwnershipPlan.request(headers, listOf(CjoSourceContentRoot.AnnotationArgument(firstHost, 0, 0),
                CjoSourceContentRoot.AnnotationArgument(secondHost, 0, 0)))
        }
    }

    @Test fun initializerLoopAndItsOwnJumpRemainRequestableWithNullReturnDomain() {
        val wire = Wire()
        val loop = loopInitializer(wire)
        val variable = wire.variable(loop.initializer, topLevel = false)
        val default = wire.block(uintArrayOf(variable), booleanArrayOf(false))
        val parameter = wire.parameter(default)
        val function = wire.function(0u, uintArrayOf(parameter))
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        assertFalse(variable in headers.declarations)
        val identity = headers.functionByDeclaration.getValue(function)
        val discovered = request(headers, CjoSourceContentRoot.ParameterDefault(parameter))
        assertNull(discovered.ownedContent.declarations.getValue(variable).scope.returnFunction)
        val root = CjoSourceContentRoot.Initializer(variable)
        val selection = request(discovered.registeredHeaders, root)
        val binding = selection.expressionBindings.single()
        assertEquals(CjoSourceExpressionBindingRole.REF_LOOP, binding.role)
        assertEquals(loop.jump, binding.source.reference)
        assertEquals(loop.loop, binding.target)
        assertTrue(binding.owned)
        assertNull(binding.source.scope.returnFunction)
        assertEquals(identity, binding.source.scope.function)
        assertTrue(binding.targetContexts.all { it.scope.returnFunction == null && it.scope.function == identity &&
            it.scope.expressionContext == CjoSourceContext.Declaration(variable) })
        assertEquals(CjoSourceOwningParent.Declaration(variable), selection.ownedContent.expressions.getValue(loop.initializer).parent)
        assertTrue(selection.selectedBodies.isEmpty())
        assertTrue(selection.borrowedBindings.isEmpty())
        assertStableProofs(selection, root)
    }

    @Test fun twoBlockDeclarationSlotsCannotDefineTheSameD() {
        val wire = Wire()
        val variable = wire.variable(0u, topLevel = false)
        val body = wire.block(uintArrayOf(variable, variable), booleanArrayOf(false, false))
        val function = wire.function(body)
        val pkg = wire.finish()
        val headers = CjoSourceOwnershipPlan.headers(pkg)
        assertThrows(IllegalStateException::class.java) {
            request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function)))
        }
        assertThrows(IllegalStateException::class.java) { CjoSourceOwnershipPlan.create(pkg, validateReferences = false) }
    }

    @Test fun matchTryAndNestedPatternListsCannotDefineTheSamePTwice() {
        for (list in listOf("Match", "Try", "Pattern")) {
            val wire = Wire()
            val pattern = wire.pattern(PatternKind.WildcardPattern, uintArrayOf(wire.unitType))
            val duplicated = intArrayOf(pattern, pattern)
            val expression = when (list) {
                "Match" -> {
                    val branch = wire.block()
                    val info = MatchCaseInfo.createMatchCaseInfo(wire.builder,
                        MatchCaseInfo.createPatternsVector(wire.builder, duplicated))
                    val case = wire.expression(ExprKind.MatchCase, uintArrayOf(branch), infoType = ExprInfo.MatchCaseInfo, info = info)
                    val selector = wire.literal()
                    wire.expression(ExprKind.MatchExpr, uintArrayOf(selector, case), infoType = ExprInfo.MatchInfo,
                        info = MatchInfo.createMatchInfo(wire.builder, true))
                }
                "Try" -> {
                    val body = wire.block()
                    val firstCatch = wire.block()
                    val secondCatch = wire.block()
                    val info = TryInfo.createTryInfo(wire.builder, 0, TryInfo.createPatternsVector(wire.builder, duplicated))
                    wire.expression(ExprKind.TryExpr, uintArrayOf(body, firstCatch, secondCatch, 0u), infoType = ExprInfo.TryInfo, info = info)
                }
                else -> {
                    val tupleType = wire.type(TypeKind.Tuple, typeArguments = uintArrayOf(wire.unitType, wire.unitType))
                    val tuplePattern = wire.pattern(PatternKind.TuplePattern, uintArrayOf(tupleType), duplicated)
                    val initializer = wire.expression(ExprKind.TupleLit, uintArrayOf(wire.literal(), wire.literal()), type = tupleType)
                    val info = LetPatternDestructorInfo.createLetPatternDestructorInfo(wire.builder,
                        LetPatternDestructorInfo.createPatternsVector(wire.builder, intArrayOf(tuplePattern)))
                    wire.expression(ExprKind.LetPatternDestructor, uintArrayOf(initializer), infoType = ExprInfo.LetPatternDestructorInfo, info = info)
                }
            }
            val function = wire.function(wire.block(uintArrayOf(expression)))
            val pkg = wire.finish()
            val headers = CjoSourceOwnershipPlan.headers(pkg)
            assertThrows(IllegalStateException::class.java, {
                request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function)))
            }, "$list 两个原 ordinal 不能认领同一个 P table identity")
            assertThrows(IllegalStateException::class.java) { CjoSourceOwnershipPlan.create(pkg, validateReferences = false) }
        }
    }

    @Test fun expressionAndTypeInfoDiscriminantsMustMatchTheirActualKinds() {
        val expressionWire = Wire()
        val blockInfo = BlockInfo.createBlockInfo(expressionWire.builder,
            BlockInfo.createIsExprVector(expressionWire.builder, booleanArrayOf(false)))
        val body = expressionWire.expression(ExprKind.Block, infoType = ExprInfo.LitConstInfo, info = blockInfo)
        val function = expressionWire.function(body)
        val expressionHeaders = CjoSourceOwnershipPlan.headers(expressionWire.finish())
        assertThrows(IllegalStateException::class.java) {
            request(expressionHeaders, CjoSourceContentRoot.FunctionBody(expressionHeaders.functionByDeclaration.getValue(function)))
        }
        val typeWire = Wire()
        val target = FullId.createFullId(typeWire.builder, PackageIndex.CURRENT.value, 0, 2u)
        val genericInfo = GenericTyInfo.createGenericTyInfo(typeWire.builder, target,
            GenericTyInfo.createUpperBoundsVector(typeWire.builder, uintArrayOf()))
        val classType = typeWire.type(TypeKind.Class, SemaTyInfo.GenericTyInfo, genericInfo)
        val initializer = typeWire.expression(ExprKind.WildcardExpr, type = classType)
        val variable = typeWire.variable(initializer)
        val classifier = typeWire.classDeclaration(classType)
        assertEquals(2u, classifier)
        val typeHeaders = CjoSourceOwnershipPlan.headers(typeWire.finish())
        assertThrows(IllegalStateException::class.java) { request(typeHeaders, CjoSourceContentRoot.Initializer(variable)) }
    }

    @Test fun coldMapWithoutAnActualTargetOccurrenceIsRejected() {
        val wire = Wire()
        val source = wire.literal(map = 2u)
        val target = wire.literal()
        val first = wire.function(wire.block(uintArrayOf(source)))
        wire.function(wire.block(uintArrayOf(target)))
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        assertThrows(RuntimeException::class.java) {
            request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(first)))
        }
        assertTrue(headers.expressionContexts.isEmpty())
    }

    @Test fun legalMapCycleIsBindingRatherThanAnOwningCycle() {
        val wire = Wire()
        val first = wire.literal(map = 2u)
        val second = wire.literal(map = first)
        val body = wire.block(uintArrayOf(first, second))
        val function = wire.function(body)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        val selection = request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function)))
        assertEquals(setOf(first, second, body), selection.ownedContent.expressions.keys)
        assertEquals(setOf(first to second, second to first), selection.expressionBindings.map {
            assertEquals(CjoSourceExpressionBindingRole.MAP, it.role)
            assertTrue(it.owned)
            it.source.reference to it.target
        }.toSet())
        assertTrue(selection.borrowedBindings.isEmpty())
    }

    @Test fun registeredMapTargetIsBorrowedWithoutSelectingItsSiblingExpression() {
        val wire = Wire()
        val source = wire.literal(map = 2u)
        val target = wire.literal()
        val variable = wire.variable(source, topLevel = false)
        val body = wire.block(uintArrayOf(variable, target), booleanArrayOf(false, true))
        val function = wire.function(body)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        val discovered = request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function)))
        val proof = discovered.registeredHeaders.expressionContexts.getValue(target)
        val selected = request(discovered.registeredHeaders, CjoSourceContentRoot.Initializer(variable))
        assertEquals(setOf(source), selected.ownedContent.expressions.keys)
        val binding = selected.borrowedBindings.single()
        assertEquals(CjoSourceExpressionBindingRole.MAP, binding.role)
        assertEquals(source, binding.source.reference)
        assertEquals(target, binding.target)
        assertFalse(binding.owned)
        assertEquals(proof, binding.targetContexts)
        assertTrue(selected.selectedBodies.isEmpty())
        assertFalse(body in selected.ownedContent.expressions)
    }

    @Test fun matchCaseSelectorBindingComesFromItsActualOwningMatch() {
        val wire = Wire()
        val selector = wire.literal(boolean = true)
        val branch = wire.block()
        val pattern = wire.pattern(PatternKind.WildcardPattern, uintArrayOf(wire.unitType))
        val caseInfo = MatchCaseInfo.createMatchCaseInfo(wire.builder,
            MatchCaseInfo.createPatternsVector(wire.builder, intArrayOf(pattern)))
        val case = wire.expression(ExprKind.MatchCase, uintArrayOf(branch), infoType = ExprInfo.MatchCaseInfo, info = caseInfo)
        val match = wire.expression(ExprKind.MatchExpr, uintArrayOf(selector, case),
            infoType = ExprInfo.MatchInfo, info = MatchInfo.createMatchInfo(wire.builder, true))
        val function = wire.function(wire.block(uintArrayOf(match)))
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        val selected = request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function)))
        val binding = selected.expressionBindings.single { it.role == CjoSourceExpressionBindingRole.SELECTOR }
        assertEquals(case, binding.source.reference)
        assertEquals(CjoSourceOwningParent.Expression(match), binding.source.parent)
        assertEquals(selector, binding.target)
        assertTrue(binding.owned)
        assertTrue(binding.targetContexts.all { it.parent == CjoSourceOwningParent.Expression(match) })
    }

    @Test fun nestedFunctionHelperHeaderDoesNotAuthorizeItsDamagedBody() {
        val wire = Wire()
        val damagedBody = wire.expression(UShort.MAX_VALUE)
        val nestedBody = wire.block()
        val helper = wire.function(damagedBody, topLevel = false)
        val parameter = wire.parameter(0u)
        val nested = wire.function(nestedBody, uintArrayOf(parameter), topLevel = false, helpers = uintArrayOf(helper))
        val outer = wire.function(wire.block(uintArrayOf(nested), booleanArrayOf(false)))
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        val outerIdentity = headers.functionByDeclaration.getValue(outer)
        val selected = request(headers, CjoSourceContentRoot.FunctionBody(outerIdentity))
        val nestedIdentity = selected.ownedContent.functionByDeclaration.getValue(nested)
        val helperIdentity = selected.registeredHeaders.functionByDeclaration.getValue(helper)
        assertEquals(setOf(outerIdentity, nestedIdentity), selected.selectedBodies)
        assertFalse(damagedBody in selected.ownedContent.expressions)
        val helperHeader = selected.borrowedContexts.declarations.getValue(helper)
        assertEquals(CjoSourceOwningParent.Declaration(parameter), helperHeader.parent)
        assertEquals(nestedIdentity, helperHeader.helperOwner)
        assertThrows(IllegalStateException::class.java) {
            request(selected.registeredHeaders, CjoSourceContentRoot.FunctionBody(helperIdentity))
        }
    }

    @Test fun signatureSlotsRejectWrongKindAndDuplicateParameterIdentity() {
        val wrongKind = Wire()
        val variable = wrongKind.variable(0u, topLevel = false)
        wrongKind.function(0u, uintArrayOf(variable))
        assertThrows(IllegalStateException::class.java) { CjoSourceOwnershipPlan.headers(wrongKind.finish()) }
        val duplicate = Wire()
        val parameter = duplicate.parameter(0u)
        duplicate.function(0u, uintArrayOf(parameter, parameter))
        assertThrows(IllegalStateException::class.java) { CjoSourceOwnershipPlan.headers(duplicate.finish()) }
    }

    @Test fun actualOwnedOperandCycleIsRejected() {
        val wire = Wire()
        val first = wire.block(uintArrayOf(2u))
        val second = wire.block(uintArrayOf(first))
        assertEquals(2u, second)
        val function = wire.function(first)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        assertThrows(IllegalStateException::class.java) {
            request(headers, CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function)))
        }
    }

    @Test fun sameParentTwoOperandSlotsKeepTwoOccurrencesOfOneE() {
        val wire = Wire()
        val child = wire.literal()
        val tupleType = wire.type(TypeKind.Tuple, typeArguments = uintArrayOf(wire.unitType, wire.unitType))
        val parent = wire.expression(ExprKind.TupleLit, uintArrayOf(child, child), type = tupleType)
        val body = wire.block(uintArrayOf(parent))
        val function = wire.function(body)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        val root = CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function))
        val selection = request(headers, root)
        val occurrences = selection.ownedContent.expressionOccurrences.filter { it.reference == child }
        assertEquals(2, occurrences.size, "两个原 operand ordinal 不能被 record identity 查重吞掉")
        assertTrue(occurrences.all { it.parent == CjoSourceOwningParent.Expression(parent) })
        assertEquals(setOf(CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.OPERAND, 0),
            CjoSourceExpressionSlot(CjoSourceExpressionSlotKind.OPERAND, 1)), occurrences.map { it.slot }.toSet())
        assertEquals(2, selection.ownedContent.expressionContexts.getValue(child).size)
        assertEquals(2, selection.registeredHeaders.expressionContexts.getValue(child).size)
        assertEquals(setOf(child, parent, body), selection.ownedContent.expressions.keys)
        assertStableProofs(selection, root)
    }

    @Test fun repeatedIdenticalRootsDoNotAccumulateMetadataMaskProofs() {
        val wire = Wire()
        val child = wire.literal()
        val body = wire.block(uintArrayOf(child))
        val function = wire.function(body)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        val root = CjoSourceContentRoot.FunctionBody(headers.functionByDeclaration.getValue(function))
        val selection = CjoSourceOwnershipPlan.request(headers, listOf(root, root))
        assertEquals(2, selection.ownedContent.expressionOccurrences.size)
        assertStableProofs(selection, root)
    }

    @Test fun officialReadBodyThenLocalYInitializerReusesOriginalOccurrenceProof() =
        assertOfficialIncrementalInitializer("read", "8418D9881CB8710A465156AB2B5EAF74E3035656D3D5DC22311F7043EB274CCC", 4u, 2u, "y")

    @Test fun officialObservedInitializerReusesFinalizerDiscoveredProof() =
        assertOfficialIncrementalInitializer("finalizer_nested_field", "07606466A8C218711D8F79B9CB3CB07FC73EACCBCDB67B2EB5C5E76A13F18A37", 5u, 6u, "observed")

    /** 同一原 d 的 initializer 首次下行和后续独立请求必须使用同一四字段 context 身份。 */
    private fun assertOfficialIncrementalInitializer(id: String, sha: String, variable: UInt, initializer: UInt, name: String) {
        val encoded = checkNotNull(javaClass.getResourceAsStream("/sourceHeaderRegistry/$id.cjo.base64")).use {
            it.readAllBytes().toString(Charsets.UTF_8).trim()
        }
        val bytes = Base64.getDecoder().decode(encoded)
        assertEquals(sha, MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02X".format(it) })
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(bytes))
        val variableWire = checkNotNull(pkg.allDecls(variable.toInt() - 1))
        assertEquals(DeclKind.VarDecl, variableWire.kind)
        assertEquals(name, variableWire.identifier)
        assertEquals(initializer, (variableWire.info(VarInfo()) as VarInfo).initializer)
        val headers = CjoSourceOwnershipPlan.headers(pkg)
        val identity = headers.functionByDeclaration.getValue(1u)
        assertFalse(variable in headers.declarations)
        val bodyRoot = CjoSourceContentRoot.FunctionBody(identity)
        val body = request(headers, bodyRoot)
        assertTrue(variable in body.ownedContent.declarations)
        assertEquals(CjoSourceOwningParent.Declaration(variable), body.ownedContent.expressions.getValue(initializer).parent)
        assertEquals(identity, body.ownedContent.expressions.getValue(initializer).scope.returnFunction)
        assertEquals(identity, body.ownedContent.declarations.getValue(variable).scope.returnFunction)
        assertEquals(CjoSourceExpressionSlotKind.INITIALIZER, body.ownedContent.expressions.getValue(initializer).slot.kind)
        val proof = body.registeredHeaders.expressionContexts.getValue(initializer)
        assertEquals(1, proof.size)
        assertEquals(identity, proof.single().scope.returnFunction)
        val initializerRoot = CjoSourceContentRoot.Initializer(variable)
        val selected = request(body.registeredHeaders, initializerRoot)
        assertEquals(listOf(initializerRoot), selected.roots)
        assertEquals(CjoSourceOwningParent.Declaration(variable), selected.ownedContent.expressions.getValue(initializer).parent)
        assertEquals(identity, selected.ownedContent.expressions.getValue(initializer).scope.returnFunction)
        assertEquals(proof, selected.registeredHeaders.expressionContexts.getValue(initializer))
        assertEquals(body.ownedContent.expressions.getValue(initializer).scope,
            selected.ownedContent.expressions.getValue(initializer).scope)
        assertTrue(selected.selectedBodies.isEmpty())
        assertFalse(headers.functions.getValue(identity).body.body in selected.ownedContent.expressions)
        assertStableProofs(selected, initializerRoot)
        // 独立 initializer 与再次 body 请求都保留原 F 的 Return 域，不借此重新授权外 F.body。
        val again = request(selected.registeredHeaders, bodyRoot)
        assertEquals(identity, again.ownedContent.expressions.getValue(initializer).scope.returnFunction)
        assertEquals(proof, again.registeredHeaders.expressionContexts.getValue(initializer))
    }

    private fun assertStableProofs(first: CjoSourceContentSelection, root: CjoSourceContentRoot) {
        var registry = first.registeredHeaders
        val expected = registry.expressionContexts
        val counts = expected.mapValues { it.value.size }
        repeat(4) {
            val selected = CjoSourceOwnershipPlan.request(registry, listOf(root, root))
            assertEquals(counts, selected.registeredHeaders.expressionContexts.mapValues { it.value.size })
            assertEquals(expected, selected.registeredHeaders.expressionContexts)
            selected.registeredHeaders.expressionContexts.values.forEach { occurrences ->
                assertEquals(occurrences.size, occurrences.distinct().size)
                assertTrue(occurrences.all { !it.scope.includesInitializers })
            }
            assertTrue(selected.registeredHeaders.expressions.isEmpty())
            registry = selected.registeredHeaders
        }
    }

    private fun assertNonBodyReturn(slot: Slot) {
        val wire = Wire()
        val value = wire.literal()
        val returned = wire.expression(ExprKind.ReturnExpr, uintArrayOf(value), type = wire.type(TypeKind.Nothing))
        if (slot == Slot.INITIALIZER) {
            val variable = wire.variable(returned)
            val unrelated = wire.function(wire.block())
            val headers = CjoSourceOwnershipPlan.headers(wire.finish())
            assertTrue(headers.functionByDeclaration.containsKey(unrelated))
            val header = headers.declarations.getValue(variable)
            assertEquals(CjoSourceOwningParent.Package, header.parent)
            assertNull(header.scope.returnFunction)
            assertNull(header.scope.function)
            assertThrows(IllegalStateException::class.java) { request(headers, CjoSourceContentRoot.Initializer(variable)) }
            return
        }
        val parameter = wire.parameter(if (slot == Slot.DEFAULT) returned else 0u)
        val annotation = if (slot == Slot.ANNOTATION) intArrayOf(wire.annotation(returned, kind = AnnoKind.Annotation)) else intArrayOf()
        val body = wire.block()
        val function = wire.function(body, uintArrayOf(parameter), annotations = annotation)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        val root = when (slot) {
            Slot.INITIALIZER -> error("Global initializer is handled in its actual package context")
            Slot.DEFAULT -> CjoSourceContentRoot.ParameterDefault(parameter)
            Slot.ANNOTATION -> CjoSourceContentRoot.AnnotationArgument(function, 0, 0)
        }
        assertNull(headers.declarations.getValue(parameter).scope.returnFunction)
        assertThrows(IllegalStateException::class.java) { request(headers, root) }
    }

    private fun assertSelectedExpression(selection: CjoSourceContentSelection, root: CjoSourceContentRoot,
        expression: UInt, parent: CjoSourceOwningParent) {
        assertEquals(listOf(root), selection.roots)
        assertEquals(setOf(expression), selection.ownedContent.expressions.keys)
        assertEquals(parent, selection.ownedContent.expressions.getValue(expression).parent)
        assertNull(selection.ownedContent.expressions.getValue(expression).scope.returnFunction)
        assertTrue(selection.selectedBodies.isEmpty())
        assertTrue(selection.registeredHeaders.expressions.isEmpty())
    }

    private fun request(headers: CjoSourceOwnershipPlan, root: CjoSourceContentRoot): CjoSourceContentSelection =
        CjoSourceOwnershipPlan.request(headers, listOf(root))

    private data class LoopInitializer(val initializer: UInt, val loop: UInt, val jump: UInt)

    /** 原 refLoop 指向实际 enclosing While；Return 域由 initializer 的真实来源 scope 继承。 */
    private fun loopInitializer(wire: Wire): LoopInitializer {
        val condition = wire.literal(boolean = true)
        val jump = wire.jump(4u)
        val body = wire.block(uintArrayOf(jump))
        val loop = wire.expression(ExprKind.WhileExpr, uintArrayOf(condition, body))
        assertEquals(4u, loop)
        return LoopInitializer(wire.block(uintArrayOf(loop)), loop, jump)
    }

    private data class Slots(val headers: CjoSourceOwnershipPlan, val body: UInt, val default: UInt,
        val annotation: UInt, val initializer: UInt, val parameter: UInt, val function: UInt,
        val variable: UInt, val annotationConstructor: UInt)

    private fun slots(damaged: Slot? = null): Slots {
        val wire = Wire()
        val body = wire.block()
        val default = if (damaged == Slot.DEFAULT) wire.expression(UShort.MAX_VALUE) else wire.literal()
        val annotation = if (damaged == Slot.ANNOTATION) wire.expression(UShort.MAX_VALUE) else wire.literal()
        val initializer = wire.literal()
        val parameter = wire.parameter(default)
        val function = wire.function(body, uintArrayOf(parameter),
            annotations = intArrayOf(wire.annotation(annotation, target = 4u, kind = AnnoKind.Custom)))
        val variable = wire.variable(initializer)
        // 固定 N 的 CONSTRUCTOR 位是 bit31；目标还必须由真实 annotation class 的成员槽拥有。
        val annotationClassType = wire.classType(5u)
        val constructor = wire.function(0u, topLevel = false, attributes = ulongArrayOf(1uL shl 31))
        wire.classDeclaration(type = annotationClassType, members = uintArrayOf(constructor), isAnnotation = true)
        assertEquals(4u, constructor)
        return Slots(CjoSourceOwnershipPlan.headers(wire.finish()), body, default, annotation, initializer,
            parameter, function, variable, constructor)
    }

    private data class GenericTypes(val headers: CjoSourceOwnershipPlan, val identity: CjoSourceFunctionIdentity,
        val function: UInt, val parameter: UInt, val classifier: UInt, val genericType: UInt, val boundType: UInt)

    private fun genericTypes(withConstraints: Boolean): GenericTypes {
        val wire = Wire()
        val bound = wire.classType(3u)
        val generic = wire.genericType(1u, uintArrayOf(bound))
        val parameter = wire.declaration(DeclKind.GenericParamDecl, topLevel = false, type = generic)
        val metadata = wire.generic(uintArrayOf(parameter), if (withConstraints) generic else 0u, uintArrayOf(bound))
        val expression = wire.expression(ExprKind.WildcardExpr, type = if (withConstraints) wire.unitType else generic)
        val function = wire.function(wire.block(uintArrayOf(expression)), generic = metadata)
        val classifier = wire.classDeclaration(type = bound)
        val headers = CjoSourceOwnershipPlan.headers(wire.finish())
        return GenericTypes(headers, headers.functionByDeclaration.getValue(function), function, parameter, classifier, generic, bound)
    }

    private enum class Slot { INITIALIZER, DEFAULT, ANNOTATION }
    private enum class BodyDamage { KIND, INFO, OPERAND, TYPE, TARGET, PATTERN }

    /** 只分配明确的 d/e/T 保存槽；numeric reference 允许前向引用和 map 环，不推断任何 owner。 */
    private class Wire {
        val builder = FlatBufferBuilder(1024)
        private val declarations = ArrayList<Int>()
        private val expressions = ArrayList<Int>()
        private val types = ArrayList<Int>()
        val unitType: UInt = type(TypeKind.Unit)

        fun type(kind: UShort, infoType: UByte = SemaTyInfo.NONE, info: Int = 0,
            typeArguments: UIntArray = uintArrayOf()): UInt {
            val arguments = SemaTy.createTypeArgsVector(builder, typeArguments)
            types += SemaTy.createSemaTy(builder, kind, arguments, infoType, info)
            return types.size.toUInt()
        }

        fun classType(declaration: UInt): UInt = type(TypeKind.Class, SemaTyInfo.CompositeTyInfo,
            CompositeTyInfo.createCompositeTyInfo(builder, fullId(declaration), false))

        fun genericType(declaration: UInt, uppers: UIntArray): UInt = type(TypeKind.Generic, SemaTyInfo.GenericTyInfo,
            GenericTyInfo.createGenericTyInfo(builder, fullId(declaration), GenericTyInfo.createUpperBoundsVector(builder, uppers)))

        fun generic(parameters: UIntArray, subject: UInt, uppers: UIntArray): Int {
            val constraints = if (subject == 0u) 0 else {
                val bounds = Constraint.createUppersVector(builder, uppers)
                Constraint.startConstraint(builder)
                Constraint.addType(builder, subject)
                Constraint.addUppers(builder, bounds)
                Generic.createConstraintsVector(builder, intArrayOf(Constraint.endConstraint(builder)))
            }
            return Generic.createGeneric(builder, Generic.createTypeParametersVector(builder, parameters), constraints)
        }

        fun expression(kind: UShort, operands: UIntArray = uintArrayOf(), type: UInt = unitType,
            infoType: UByte = ExprInfo.NONE, info: Int = 0, map: UInt = 0u): UInt {
            val children = Expr.createOperandsVector(builder, operands)
            Expr.startExpr(builder)
            Expr.addKind(builder, kind)
            Expr.addOperands(builder, children)
            Expr.addType(builder, type)
            Expr.addInfoType(builder, infoType)
            Expr.addInfo(builder, info)
            Expr.addMapExpr(builder, map)
            expressions += Expr.endExpr(builder)
            return expressions.size.toUInt()
        }

        fun literal(boolean: Boolean = false, map: UInt = 0u): UInt {
            val literalType = if (boolean) type(TypeKind.Bool) else unitType
            val value = builder.createString(if (boolean) "true" else "()")
            val info = LitConstInfo.createLitConstInfo(builder, value, if (boolean) LitConstKind.Bool else LitConstKind.Unit, 0u)
            return expression(ExprKind.LitConstExpr, type = literalType, infoType = ExprInfo.LitConstInfo, info = info, map = map)
        }

        fun block(children: UIntArray = uintArrayOf(), isExpression: BooleanArray = BooleanArray(children.size) { true },
            type: UInt = unitType): UInt {
            val info = BlockInfo.createBlockInfo(builder, BlockInfo.createIsExprVector(builder, isExpression + false))
            return expression(ExprKind.Block, children, type, ExprInfo.BlockInfo, info)
        }

        fun reference(target: UInt = 0u, instantiation: UIntArray = uintArrayOf(), matchedParent: UInt = 0u): UInt {
            val name = builder.createString("saved-reference")
            val info = ReferenceInfo.createReferenceInfo(builder, name, if (target == 0u) 0 else fullId(target),
                ReferenceInfo.createInstTysVector(builder, instantiation), matchedParent)
            return expression(ExprKind.RefExpr, infoType = ExprInfo.ReferenceInfo, info = info)
        }

        fun jump(loop: UInt): UInt = expression(ExprKind.JumpExpr, infoType = ExprInfo.JumpInfo,
            info = JumpInfo.createJumpInfo(builder, true), map = loop)

        fun lambda(body: UInt): UInt = expression(ExprKind.LambdaExpr, infoType = ExprInfo.LambdaInfo,
            info = LambdaInfo.createLambdaInfo(builder, functionBody(body, uintArrayOf()), false))

        fun parameter(default: UInt): UInt = declaration(DeclKind.FuncParam, DeclInfo.ParamInfo,
            ParamInfo.createParamInfo(builder, false, false, default), topLevel = false)

        fun variable(initializer: UInt, topLevel: Boolean = true, type: UInt = unitType): UInt = declaration(DeclKind.VarDecl, DeclInfo.VarInfo,
            VarInfo.createVarInfo(builder, false, false, false, initializer, 0u, 0), topLevel, type)

        fun function(body: UInt, parameters: UIntArray = uintArrayOf(), generic: Int = 0,
            annotations: IntArray = intArrayOf(), topLevel: Boolean = true, helpers: UIntArray = uintArrayOf(),
            attributes: ULongArray = ulongArrayOf()): UInt = declaration(DeclKind.FuncDecl, DeclInfo.FuncInfo,
            FuncInfo.createFuncInfo(builder, functionBody(body, parameters, helpers), 0u, 0u, 0, false, false, false),
            topLevel = topLevel, generic = generic, annotations = annotations, attributes = attributes)

        private fun functionBody(body: UInt, parameters: UIntArray, helpers: UIntArray = uintArrayOf()): Int {
            val list = FuncParamList.createFuncParamList(builder, FuncParamList.createParamsVector(builder, parameters),
                FuncParamList.createDesugarsVector(builder, helpers))
            return FuncBody.createFuncBody(builder, FuncBody.createParamListsVector(builder, intArrayOf(list)), unitType, body, false, 0u)
        }

        fun classDeclaration(type: UInt, inherited: UIntArray = uintArrayOf(), members: UIntArray = uintArrayOf(),
            isAnnotation: Boolean = false): UInt = declaration(DeclKind.ClassDecl,
            DeclInfo.ClassInfo, ClassInfo.createClassInfo(builder, ClassInfo.createInheritedTypesVector(builder, inherited),
                ClassInfo.createBodyVector(builder, members), 0, isAnnotation, 0u, false, 0u), type = type)

        fun declaration(kind: UShort, infoType: UByte = DeclInfo.NONE, info: Int = 0, topLevel: Boolean = true,
            type: UInt = unitType, generic: Int = 0, annotations: IntArray = intArrayOf(),
            attributes: ULongArray = ulongArrayOf()): UInt {
            val name = builder.createString("d${declarations.size + 1}")
            val annotationVector = Decl.createAnnotationsVector(builder, annotations)
            val attributeVector = Decl.createAttributesVector(builder, attributes)
            Decl.startDecl(builder)
            Decl.addKind(builder, kind)
            Decl.addIsTopLevel(builder, topLevel)
            Decl.addIdentifier(builder, name)
            Decl.addInfoType(builder, infoType)
            Decl.addInfo(builder, info)
            Decl.addType(builder, type)
            Decl.addGeneric(builder, generic)
            Decl.addAnnotations(builder, annotationVector)
            Decl.addAttributes(builder, attributeVector)
            declarations += Decl.endDecl(builder)
            return declarations.size.toUInt()
        }

        fun annotation(expression: UInt, target: UInt = 0u, kind: UShort = AnnoKind.Annotation): Int {
            val argument = AnnoArg.createAnnoArg(builder, builder.createString("value"), expression)
            val arguments = Anno.createArgsVector(builder, intArrayOf(argument))
            return Anno.createAnno(builder, kind, builder.createString("SavedAnnotation"), arguments,
                if (target == 0u) 0 else fullId(target))
        }

        fun pattern(kind: Byte, types: UIntArray, nested: IntArray = intArrayOf(),
            expressions: UIntArray = uintArrayOf()): Int {
            val typeVector = Pattern.createTypesVector(builder, types)
            val patternVector = Pattern.createPatternsVector(builder, nested)
            val expressionVector = Pattern.createExprsVector(builder, expressions)
            Pattern.startPattern(builder)
            Pattern.addKind(builder, kind)
            Pattern.addTypes(builder, typeVector)
            Pattern.addPatterns(builder, patternVector)
            Pattern.addExprs(builder, expressionVector)
            return Pattern.endPattern(builder)
        }

        /** Let 的 initializer 是 e 槽，VarPattern.exprs 是 d 槽；两者不能按同一索引域解释。 */
        fun letPattern(pattern: Int): UInt {
            val initializer = literal()
            val info = LetPatternDestructorInfo.createLetPatternDestructorInfo(builder,
                LetPatternDestructorInfo.createPatternsVector(builder, intArrayOf(pattern)))
            return expression(ExprKind.LetPatternDestructor, uintArrayOf(initializer),
                infoType = ExprInfo.LetPatternDestructorInfo, info = info)
        }

        private fun fullId(reference: UInt): Int = FullId.createFullId(builder, PackageIndex.CURRENT.value, 0, reference)

        fun finish(): Package {
            val declarationVector = Package.createAllDeclsVector(builder, declarations.toIntArray())
            val expressionVector = Package.createAllExprsVector(builder, expressions.toIntArray())
            val typeVector = Package.createAllTypesVector(builder, types.toIntArray())
            val name = builder.createString("content_request_structure")
            Package.startPackage(builder)
            Package.addFullPkgName(builder, name)
            Package.addAllDecls(builder, declarationVector)
            Package.addAllExprs(builder, expressionVector)
            Package.addAllTypes(builder, typeVector)
            Package.finishPackageBuffer(builder, Package.endPackage(builder))
            return Package.getRootAsPackage(ByteBuffer.wrap(builder.sizedByteArray()))
        }
    }
}
