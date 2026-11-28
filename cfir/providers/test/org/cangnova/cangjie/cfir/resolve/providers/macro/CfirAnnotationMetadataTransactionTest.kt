package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ensureAnnotationMetadataRegistry
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** 文件构建事务只发布最终声明图；session 弱索引不拥有未完成产品或历史声明图。 */
class CfirAnnotationMetadataTransactionTest {
    @Test
    fun sealClosesEveryWriteOperationAndDoesNotPublishByItself() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val owner = fixture.function("first")
        val original = fixture.annotation(owner, owner.symbol)
        val carrier = fixture.record(builder, owner, original)
        val finalProduct = builder.seal(listOf(owner))
        assertNull(fixture.registry.snapshot(original))

        val other = fixture.function("other")
        val otherAnnotation = fixture.annotation(other, other.symbol)
        val replacement = fixture.newAnnotation(owner.symbol)
        assertThrows(IllegalStateException::class.java) { fixture.record(builder, other, otherAnnotation) }
        assertThrows(IllegalStateException::class.java) { builder.migrate(carrier, replacement) }
        assertThrows(IllegalStateException::class.java) {
            builder.recordDerivedSlot(original, other, 0, otherAnnotation)
        }
        assertThrows(IllegalStateException::class.java) { builder.seal(listOf(owner)) }
        assertThrows(IllegalStateException::class.java) { builder.transferOwner(owner, other) }
        assertThrows(IllegalStateException::class.java) { builder.replaceAnnotationList(owner, emptyList()) }

        fixture.registry.publish(finalProduct)
        assertSame(owner, fixture.registry.snapshot(original)?.owner)
        assertNull(fixture.registry.snapshot(otherAnnotation))
        assertThrows(IllegalStateException::class.java) { fixture.registry.publish(finalProduct) }
        assertSame(owner, fixture.registry.snapshot(original)?.owner)
    }

    @Test
    fun recordsRequireTheActualSlotAndRejectDuplicateSlotOrAnnotationIdentities() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val owner = fixture.function("owner")
        val annotation = fixture.annotation(owner, owner.symbol)
        val snapshot = fixture.slot(owner, annotation)
        val carrier = builder.record(snapshot)
        assertThrows(IllegalArgumentException::class.java) { builder.record(snapshot) }
        assertThrows(IllegalArgumentException::class.java) { builder.record(snapshot.copy(annotationIndex = 1)) }
        val unknown = fixture.newAnnotation(owner.symbol)
        assertThrows(IllegalArgumentException::class.java) { builder.record(snapshot.copy(originalAnnotation = unknown)) }
        val other = fixture.function("other")
        other.replaceAnnotations(listOf(annotation))
        assertThrows(IllegalArgumentException::class.java) { builder.record(snapshot.copy(owner = other)) }
        assertSame(owner, builder.snapshot(carrier)?.owner)
        assertSame(annotation, builder.snapshot(carrier)?.originalAnnotation)
    }

    @Test
    fun migrationKeepsOriginalCarrierProvenanceButPublishesOnlyTheCurrentAnnotation() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val owner = fixture.function("owner")
        val original = fixture.annotation(owner, owner.symbol)
        val originalSlot = fixture.slot(owner, original)
        val carrier = builder.record(originalSlot)
        val replacement = fixture.newAnnotation(owner.symbol)

        // 先完成真实 annotation 槽位替换，再迁移 metadata；不能仅改 side table。
        owner.replaceAnnotations(listOf(replacement))
        builder.migrate(carrier, replacement)
        val migrated = requireNotNull(builder.snapshot(carrier))
        assertSame(replacement, migrated.originalAnnotation)
        assertSame(migrated, builder.snapshot(replacement))
        assertNull(builder.snapshot(original))
        assertProvenance(originalSlot, migrated)

        val unknownCarrier = carrier.copy(originalAnnotation = fixture.newAnnotation(owner.symbol))
        assertNull(builder.snapshot(unknownCarrier))
        assertThrows(IllegalArgumentException::class.java) { builder.migrate(unknownCarrier, replacement) }
        fixture.registry.publish(builder.seal(listOf(owner)))
        assertNull(fixture.registry.snapshot(original))
        assertSame(replacement, fixture.registry.snapshot(replacement)?.originalAnnotation)
        assertProvenance(originalSlot, requireNotNull(fixture.registry.snapshot(replacement)))
    }

    @Test
    fun migrationRejectsAReplacementThatWasNotInstalledInTheActualSlot() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val owner = fixture.function("owner")
        val original = fixture.annotation(owner, owner.symbol)
        val carrier = fixture.record(builder, owner, original)
        assertThrows(IllegalArgumentException::class.java) { builder.migrate(carrier, fixture.newAnnotation(owner.symbol)) }
        assertSame(original, owner.annotations.single())
        assertSame(original, builder.snapshot(carrier)?.originalAnnotation)
    }

    @Test
    fun parameterSlotsUseTheParameterOwnerAndDerivedFieldsHaveIndependentSlots() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val function = fixture.function("constructor")
        val parameter = fixture.parameter(function, "value")
        val annotation = fixture.annotation(parameter, function.symbol)
        val sourceSlot = fixture.slot(parameter, annotation)
        builder.record(sourceSlot)
        val field = fixture.field("value")
        val fieldAnnotation = fixture.annotation(field, field.symbol)
        val carrier = builder.recordDerivedSlot(annotation, field, 0, fieldAnnotation)
        val derived = requireNotNull(builder.snapshot(carrier))
        val otherSession = Fixture()
        val foreignField = otherSession.field("foreign")
        val foreignAnnotation = otherSession.annotation(foreignField, foreignField.symbol)
        assertThrows(IllegalArgumentException::class.java) {
            builder.recordDerivedSlot(annotation, foreignField, 0, foreignAnnotation)
        }
        assertNull(builder.snapshot(foreignAnnotation))
        assertNull(otherSession.registry.snapshot(foreignAnnotation))
        assertSame(function.symbol, annotation.containingDeclarationSymbol)
        assertNotSame(parameter.symbol, annotation.containingDeclarationSymbol)
        assertSame(parameter, builder.snapshot(annotation)?.owner)
        assertSame(field, derived.owner)
        assertNotSame(annotation, derived.originalAnnotation)
        assertProvenance(sourceSlot, derived)

        fixture.registry.publish(builder.seal(listOf(function, field)))
        assertSame(parameter, fixture.registry.snapshot(annotation)?.owner)
        assertSame(field, fixture.registry.snapshot(fieldAnnotation)?.owner)
    }

    @Test
    fun sealUsesOnlyFinalRootsAndRetainsReachableParameterAnnotations() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val finalOwner = fixture.function("replacement")
        val finalAnnotation = fixture.annotation(finalOwner, finalOwner.symbol)
        fixture.record(builder, finalOwner, finalAnnotation)
        val parameter = fixture.parameter(finalOwner, "parameter")
        val parameterAnnotation = fixture.annotation(parameter, finalOwner.symbol)
        fixture.record(builder, parameter, parameterAnnotation)
        val replacedOwner = fixture.function("original")
        val replacedAnnotation = fixture.annotation(replacedOwner, replacedOwner.symbol)
        fixture.record(builder, replacedOwner, replacedAnnotation)
        val unusedFragment = fixture.function("unusedFragment")
        val unusedAnnotation = fixture.annotation(unusedFragment, unusedFragment.symbol)
        fixture.record(builder, unusedFragment, unusedAnnotation)

        val finalFile = fixture.file("file.cj", listOf(finalOwner))
        fixture.registry.publish(builder.seal(listOf(finalFile)))
        assertSame(finalOwner, fixture.registry.snapshot(finalAnnotation)?.owner)
        assertSame(parameter, fixture.registry.snapshot(parameterAnnotation)?.owner)
        assertNull(fixture.registry.snapshot(replacedAnnotation))
        assertNull(fixture.registry.snapshot(unusedAnnotation))
    }

    @Test
    fun sealRejectsStaleSlotsInsteadOfPublishingMetadataForDifferentAnnotations() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val owner = fixture.function("owner")
        val original = fixture.annotation(owner, owner.symbol)
        fixture.record(builder, owner, original)
        val unrecordedReplacement = fixture.newAnnotation(owner.symbol)
        owner.replaceAnnotations(listOf(unrecordedReplacement))
        assertThrows(IllegalStateException::class.java) { builder.seal(listOf(owner)) }
        assertNull(fixture.registry.snapshot(original))
        assertNull(fixture.registry.snapshot(unrecordedReplacement))
    }

    @Test
    fun sealingCopiesMutableTokenInput() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val owner = fixture.function("owner")
        val annotation = fixture.annotation(owner, owner.symbol)
        val tokens = mutableListOf(MacroSurfaceToken("@!Tag", 0, 5), MacroSurfaceToken("[42]", 5, 9))
        val expectedTokens = tokens.toList()
        builder.record(fixture.slot(owner, annotation).copy(tokens = tokens))
        val product = builder.seal(listOf(owner))
        tokens.clear()
        tokens += MacroSurfaceToken("corrupted", 0, 9)
        fixture.registry.publish(product)
        assertEquals(expectedTokens, fixture.registry.snapshot(annotation)?.tokens)
    }

    @Test
    fun successiveFilesAndSameSourceGenerationsRemainQueryableTogether() {
        val fixture = Fixture()
        val first = fixture.annotatedFunction("same")
        val second = fixture.annotatedFunction("same")
        assertEquals(first.owner.source, second.owner.source)
        assertNotSame(first.owner, second.owner)
        assertNotSame(first.annotation, second.annotation)
        val firstProduct = first.builder.seal(listOf(first.owner))
        fixture.registry.publish(firstProduct)
        val firstMetadata = requireNotNull(fixture.registry.snapshot(first.annotation))
        assertNull(fixture.registry.snapshot(second.annotation))
        val secondProduct = second.builder.seal(listOf(second.owner))
        assertNotSame(firstProduct, secondProduct)
        fixture.registry.publish(secondProduct)
        assertSame(firstMetadata, fixture.registry.snapshot(first.annotation))
        assertSame(first.owner, fixture.registry.snapshot(first.annotation)?.owner)
        assertSame(second.owner, fixture.registry.snapshot(second.annotation)?.owner)
    }

    @Test
    fun conflictingPublicationCannotOverwriteOldMetadataOrLeakOtherEntries() {
        val fixture = Fixture()
        val published = fixture.annotatedFunction("published")
        fixture.registry.publish(published.builder.seal(listOf(published.owner)))
        val originalMetadata = requireNotNull(fixture.registry.snapshot(published.annotation))
        val conflictingBuilder = CfirAnnotationMetadataBuilder()
        val fresh = fixture.function("fresh")
        val freshAnnotation = fixture.annotation(fresh, fresh.symbol)
        fixture.record(conflictingBuilder, fresh, freshAnnotation)
        val conflictingOwner = fixture.function("conflicting")
        conflictingOwner.replaceAnnotations(listOf(published.annotation))
        fixture.record(conflictingBuilder, conflictingOwner, published.annotation)
        val conflictingProduct = conflictingBuilder.seal(listOf(fresh, conflictingOwner))

        assertThrows(IllegalStateException::class.java) { fixture.registry.publish(conflictingProduct) }
        assertSame(originalMetadata, fixture.registry.snapshot(published.annotation))
        assertSame(published.owner, originalMetadata.owner)
        assertNull(fixture.registry.snapshot(freshAnnotation), "整批冲突检查必须早于任何新索引项可见")
    }

    @Test
    fun changedSlotsAfterSealRejectTheWholePublication() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val first = fixture.function("first")
        val firstAnnotation = fixture.annotation(first, first.symbol)
        fixture.record(builder, first, firstAnnotation)
        val second = fixture.function("second")
        val secondAnnotation = fixture.annotation(second, second.symbol)
        fixture.record(builder, second, secondAnnotation)
        val product = builder.seal(listOf(first, second))
        val unrecorded = fixture.newAnnotation(second.symbol)
        second.replaceAnnotations(listOf(unrecorded))

        assertThrows(IllegalStateException::class.java) { fixture.registry.publish(product) }
        assertNull(fixture.registry.snapshot(firstAnnotation))
        assertNull(fixture.registry.snapshot(secondAnnotation))
        assertNull(fixture.registry.snapshot(unrecorded))
    }

    @Test
    fun abortedBuildCannotBeSealedOrChangePreviouslyPublishedMetadata() {
        val fixture = Fixture()
        val published = fixture.annotatedFunction("published")
        fixture.registry.publish(published.builder.seal(listOf(published.owner)))
        val publishedMetadata = fixture.registry.snapshot(published.annotation)
        val aborted = fixture.annotatedFunction("aborted")
        val carrier = requireNotNull(aborted.builder.snapshot(aborted.annotation)).let {
            CfirAnnotationReplaceCarrier(it.owner, it.annotationIndex, it.originalAnnotation)
        }
        aborted.builder.abort()
        assertThrows(IllegalStateException::class.java) { aborted.builder.seal(listOf(aborted.owner)) }
        assertThrows(IllegalStateException::class.java) { fixture.record(aborted.builder, aborted.owner, aborted.annotation) }
        assertThrows(IllegalStateException::class.java) { aborted.builder.migrate(carrier, aborted.annotation) }
        assertThrows(IllegalStateException::class.java) {
            aborted.builder.recordDerivedSlot(aborted.annotation, aborted.owner, 0, aborted.annotation)
        }
        assertThrows(IllegalStateException::class.java) { aborted.builder.transferOwner(aborted.owner, published.owner) }
        assertThrows(IllegalStateException::class.java) { aborted.builder.replaceAnnotationList(aborted.owner, emptyList()) }
        assertNull(fixture.registry.snapshot(aborted.annotation))
        assertSame(publishedMetadata, fixture.registry.snapshot(published.annotation))
    }

    @Test
    fun deletingTheFirstAnnotationRetiresItsSlotAndReindexesRetainedCarrierAliases() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val owner = fixture.function("owner")
        val annotations = List(3) { fixture.annotation(owner, owner.symbol) }
        val sources = annotations.map { fixture.slot(owner, it) }
        val carriers = sources.map(builder::record)

        builder.replaceAnnotationList(owner, annotations.drop(1))
        assertEquals(annotations.drop(1), owner.annotations)
        assertNull(builder.currentSlot(carriers[0]))
        assertNull(builder.snapshot(annotations[0]))
        assertProvenance(sources[0], requireNotNull(builder.snapshot(carriers[0])))
        for (index in 1..2) {
            assertEquals(CfirAnnotationReplaceCarrier(owner, index - 1, annotations[index]), builder.currentSlot(carriers[index]))
            assertEquals(index - 1, builder.snapshot(carriers[index])?.annotationIndex)
            assertProvenance(sources[index], requireNotNull(builder.snapshot(carriers[index])))
        }

        val replacement = fixture.newAnnotation(owner.symbol)
        builder.replaceAnnotationList(owner, listOf(replacement, annotations[2]), mapOf(replacement to carriers[1]))
        assertNull(builder.currentSlot(carriers[0]))
        assertEquals(CfirAnnotationReplaceCarrier(owner, 0, replacement), builder.currentSlot(carriers[1]))
        assertSame(replacement, builder.snapshot(carriers[1])?.originalAnnotation)
        assertNull(builder.snapshot(annotations[1]))
        // 尾部 index 2 曾属于第三个注解，现在已空出；新对象可占用，旧完整 carrier 仍定位 index 1。
        val newTail = fixture.annotation(owner, owner.symbol)
        val newTailCarrier = fixture.record(builder, owner, newTail)
        assertEquals(CfirAnnotationReplaceCarrier(owner, 2, newTail), builder.currentSlot(newTailCarrier))
        assertEquals(CfirAnnotationReplaceCarrier(owner, 1, annotations[2]), builder.currentSlot(carriers[2]))
        assertSame(annotations[2], builder.snapshot(carriers[2])?.originalAnnotation)
        assertNull(builder.currentSlot(carriers[0]))
        fixture.registry.publish(builder.seal(listOf(owner)))
        assertNull(fixture.registry.snapshot(annotations[0]))
        assertNull(fixture.registry.snapshot(annotations[1]))
        assertEquals(0, fixture.registry.snapshot(replacement)?.annotationIndex)
        assertEquals(1, fixture.registry.snapshot(annotations[2])?.annotationIndex)
        assertEquals(2, fixture.registry.snapshot(newTail)?.annotationIndex)
    }

    @Test
    fun crossOwnerMergePrependsBoundReplacementsAndRetiresDeduplicatedFragmentSlots() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val original = fixture.function("original")
        val incoming = List(2) { fixture.annotation(original, original.symbol) }
        val incomingSlots = incoming.map { fixture.slot(original, it) }
        val incomingCarriers = incomingSlots.map(builder::record)
        val replacementOwner = fixture.function("replacement")
        val duplicateFragment = fixture.annotation(replacementOwner, replacementOwner.symbol)
        val retained = fixture.annotation(replacementOwner, replacementOwner.symbol)
        val duplicateSlot = fixture.slot(replacementOwner, duplicateFragment)
        val duplicateCarrier = builder.record(duplicateSlot)
        val retainedSlot = fixture.slot(replacementOwner, retained)
        val retainedCarrier = builder.record(retainedSlot)
        val replacements = List(2) { fixture.newAnnotation(replacementOwner.symbol) }

        builder.replaceAnnotationList(
            replacementOwner, replacements + retained,
            replacements.indices.associate { replacements[it] to incomingCarriers[it] },
        )
        assertEquals(replacements + retained, replacementOwner.annotations)
        for (index in replacements.indices) {
            assertEquals(CfirAnnotationReplaceCarrier(replacementOwner, index, replacements[index]), builder.currentSlot(incomingCarriers[index]))
            assertProvenance(incomingSlots[index], requireNotNull(builder.snapshot(incomingCarriers[index])))
            assertNull(builder.snapshot(incoming[index]))
        }
        assertNull(builder.currentSlot(duplicateCarrier))
        assertNull(builder.snapshot(duplicateFragment))
        assertProvenance(duplicateSlot, requireNotNull(builder.snapshot(duplicateCarrier)))
        assertEquals(CfirAnnotationReplaceCarrier(replacementOwner, 2, retained), builder.currentSlot(retainedCarrier))
        assertProvenance(retainedSlot, requireNotNull(builder.snapshot(retainedCarrier)))

        fixture.registry.publish(builder.seal(listOf(replacementOwner)))
        incoming.forEach { assertNull(fixture.registry.snapshot(it)) }
        assertNull(fixture.registry.snapshot(duplicateFragment))
        (replacements + retained).forEachIndexed { index, annotation ->
            assertSame(replacementOwner, fixture.registry.snapshot(annotation)?.owner)
            assertEquals(index, fixture.registry.snapshot(annotation)?.annotationIndex)
        }
    }

    @Test
    fun invalidListBindingsLeaveBothRealAnnotationListsAndMetadataUnchanged() {
        val fixture = Fixture()
        val foreign = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val owner = fixture.function("owner")
        val original = fixture.annotation(owner, owner.symbol)
        val carrier = fixture.record(builder, owner, original)
        val originalMetadata = requireNotNull(builder.snapshot(carrier))
        val foreignOwner = foreign.function("foreign")
        val foreignAnnotation = foreign.annotation(foreignOwner, foreignOwner.symbol)
        val foreignCarrier = foreign.record(builder, foreignOwner, foreignAnnotation)
        val foreignMetadata = requireNotNull(builder.snapshot(foreignCarrier))
        val replacement = fixture.newAnnotation(owner.symbol)
        fun assertUnchanged() {
            assertSame(original, owner.annotations.single())
            assertSame(foreignAnnotation, foreignOwner.annotations.single())
            assertSame(originalMetadata, builder.snapshot(carrier))
            assertSame(foreignMetadata, builder.snapshot(foreignCarrier))
            assertEquals(carrier, builder.currentSlot(carrier))
            assertEquals(foreignCarrier, builder.currentSlot(foreignCarrier))
            assertNull(builder.snapshot(replacement))
        }
        val unknown = carrier.copy(originalAnnotation = fixture.newAnnotation(owner.symbol))
        assertThrows(IllegalArgumentException::class.java) {
            builder.replaceAnnotationList(owner, listOf(replacement), mapOf(replacement to unknown))
        }
        assertUnchanged()
        assertThrows(IllegalArgumentException::class.java) {
            builder.replaceAnnotationList(owner, listOf(replacement), mapOf(replacement to foreignCarrier))
        }
        assertUnchanged()
        assertThrows(IllegalArgumentException::class.java) {
            builder.replaceAnnotationList(owner, listOf(original), mapOf(replacement to carrier))
        }
        assertUnchanged()
        assertNull(fixture.registry.snapshot(original))
        assertNull(foreign.registry.snapshot(foreignAnnotation))
        builder.abort()
    }

    /** degraded 声明替换保留注解对象；槽位 owner 改为新图，原 surface carrier 仍指向同一 provenance。 */
    @Test
    fun ownerTransferPreservesCarrierAliasesAndMigratesAgainstTheActualReplacementOwner() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val original = fixture.function("original")
        val annotations = List(2) { fixture.annotation(original, original.symbol) }
        val originalSlots = annotations.map { fixture.slot(original, it) }
        val carriers = originalSlots.map(builder::record)
        val replacement = fixture.function("replacement")
        replacement.replaceAnnotations(original.annotations.toList())
        val file = fixture.file("replaced.cj", listOf(replacement))

        builder.transferOwner(original, replacement)
        // 原 owner 的 slot 已成为 carrier alias，不能用新 annotation 覆盖这个查询入口。
        val attemptedAliasAnnotation = fixture.newAnnotation(original.symbol)
        original.replaceAnnotations(listOf(attemptedAliasAnnotation, annotations[1]))
        assertThrows(IllegalArgumentException::class.java) { fixture.record(builder, original, attemptedAliasAnnotation) }
        assertNull(builder.snapshot(attemptedAliasAnnotation))
        original.replaceAnnotations(annotations)
        for (index in annotations.indices) {
            val transferred = requireNotNull(builder.snapshot(carriers[index]))
            assertSame(replacement, transferred.owner)
            assertEquals(index, transferred.annotationIndex)
            assertSame(annotations[index], transferred.originalAnnotation)
            assertSame(transferred, builder.snapshot(annotations[index]))
            assertSame(original.symbol, annotations[index].containingDeclarationSymbol)
            assertProvenance(originalSlots[index], transferred)
        }
        val migratedAnnotation = fixture.newAnnotation(replacement.symbol)
        replacement.replaceAnnotations(listOf(migratedAnnotation, annotations[1]))
        builder.migrate(carriers[0], migratedAnnotation)
        assertSame(replacement, builder.snapshot(carriers[0])?.owner)
        assertSame(migratedAnnotation, builder.snapshot(carriers[0])?.originalAnnotation)
        assertNull(builder.snapshot(annotations[0]))

        fixture.registry.publish(builder.seal(listOf(file)))
        assertNull(fixture.registry.snapshot(annotations[0]))
        assertSame(replacement, fixture.registry.snapshot(migratedAnnotation)?.owner)
        assertSame(replacement, fixture.registry.snapshot(annotations[1])?.owner)
        assertProvenance(originalSlots[0], requireNotNull(fixture.registry.snapshot(migratedAnnotation)))
    }

    @Test
    fun ownerTransferRejectsChangedIdentityOrOrderWithoutChangingOriginalSlots() {
        val fixture = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val original = fixture.function("original")
        val annotations = List(2) { fixture.annotation(original, original.symbol) }
        val carriers = annotations.map { fixture.record(builder, original, it) }
        val replacement = fixture.function("replacement")
        replacement.replaceAnnotations(annotations.reversed())
        assertThrows(IllegalArgumentException::class.java) { builder.transferOwner(original, replacement) }
        replacement.replaceAnnotations(listOf(annotations[0], fixture.newAnnotation(replacement.symbol)))
        assertThrows(IllegalArgumentException::class.java) { builder.transferOwner(original, replacement) }
        assertThrows(IllegalArgumentException::class.java) { builder.transferOwner(original, original) }
        val otherSession = Fixture()
        val foreignReplacement = otherSession.function("foreignReplacement")
        foreignReplacement.replaceAnnotations(annotations.toList())
        assertThrows(IllegalArgumentException::class.java) { builder.transferOwner(original, foreignReplacement) }
        annotations.forEach { assertNull(otherSession.registry.snapshot(it)) }
        for (index in annotations.indices) {
            assertSame(original, builder.snapshot(carriers[index])?.owner)
            assertSame(annotations[index], builder.snapshot(carriers[index])?.originalAnnotation)
        }
        replacement.replaceAnnotations(annotations.toList())
        builder.transferOwner(original, replacement)
        fixture.registry.publish(builder.seal(listOf(replacement)))
        annotations.forEach { assertSame(replacement, fixture.registry.snapshot(it)?.owner) }
    }

    @Test
    fun sealRequiresActualRootsFromOneSessionIncludingNestedRecordedOwners() {
        assertThrows(IllegalArgumentException::class.java) { CfirAnnotationMetadataBuilder().seal(emptyList()) }
        val first = Fixture()
        val second = Fixture()
        val builder = CfirAnnotationMetadataBuilder()
        val firstOwner = first.function("first")
        val firstAnnotation = first.annotation(firstOwner, firstOwner.symbol)
        first.record(builder, firstOwner, firstAnnotation)
        val secondOwner = second.function("second")
        val secondAnnotation = second.annotation(secondOwner, secondOwner.symbol)
        second.record(builder, secondOwner, secondAnnotation)
        assertThrows(IllegalArgumentException::class.java) { builder.seal(listOf(firstOwner, secondOwner)) }
        val firstFile = first.file("first.cj", listOf(firstOwner, secondOwner))
        assertThrows(IllegalArgumentException::class.java) { builder.seal(listOf(firstFile)) }
        assertNull(first.registry.snapshot(firstAnnotation))
        assertNull(second.registry.snapshot(secondAnnotation))
        builder.abort()
    }

    @Test
    @Timeout(60)
    fun crossSessionPublicationIsRejectedEvenWhenRacingTheOwningSession() {
        val ownerFixture = Fixture()
        val otherFixture = Fixture()
        val graph = ownerFixture.annotatedFunction("owner")
        val product = graph.builder.seal(listOf(graph.owner))
        val outcomes = race(
            { ownerFixture.registry.publish(product) },
            { otherFixture.registry.publish(product) },
        )
        assertNull(outcomes[0])
        assertTrue(outcomes[1] is IllegalStateException)
        assertSame(graph.owner, ownerFixture.registry.snapshot(graph.annotation)?.owner)
        assertNull(otherFixture.registry.snapshot(graph.annotation))
    }

    @Test
    @Timeout(60)
    fun nonCanonicalRegistryCannotPublishOrConsumeTheCanonicalProductDuringARace() {
        val fixture = Fixture()
        assertSame(fixture.registry, fixture.session.ensureAnnotationMetadataRegistry())
        val nonCanonical = CfirAnnotationMetadataRegistry(fixture.session)
        val graph = fixture.annotatedFunction("owner")
        val product = graph.builder.seal(listOf(graph.owner))
        val outcomes = race(
            { fixture.session.ensureAnnotationMetadataRegistry().publish(product) },
            { nonCanonical.publish(product) },
        )
        assertNull(outcomes[0])
        assertTrue(outcomes[1] is IllegalStateException)
        assertSame(graph.owner, fixture.registry.snapshot(graph.annotation)?.owner)
        assertNull(nonCanonical.snapshot(graph.annotation))
        assertSame(fixture.registry, fixture.session.ensureAnnotationMetadataRegistry())
    }

    @Test
    @Timeout(60)
    fun concurrentCanonicalPublicationOfOneProductSucceedsExactlyOnce() {
        val fixture = Fixture()
        val graph = fixture.annotatedFunction("owner")
        val product = graph.builder.seal(listOf(graph.owner))
        val outcomes = race(
            { fixture.session.ensureAnnotationMetadataRegistry().publish(product) },
            { fixture.session.ensureAnnotationMetadataRegistry().publish(product) },
        )
        assertEquals(1, outcomes.count { it == null })
        assertEquals(1, outcomes.count { it is IllegalStateException })
        assertSame(graph.owner, fixture.registry.snapshot(graph.annotation)?.owner)
    }

    @Test
    fun batchesRejectForeignOrDuplicateProductsBeforePublishingAnyEntry() {
        val first = Fixture()
        val second = Fixture()
        first.registry.publishAll(emptyList())
        val localGraph = first.annotatedFunction("local")
        val foreignGraph = second.annotatedFunction("foreign")
        val localProduct = localGraph.builder.seal(listOf(localGraph.owner))
        val foreignProduct = foreignGraph.builder.seal(listOf(foreignGraph.owner))
        assertThrows(IllegalStateException::class.java) { first.registry.publishAll(listOf(localProduct, foreignProduct)) }
        assertThrows(IllegalStateException::class.java) { first.registry.publishAll(listOf(localProduct, localProduct)) }
        assertNull(first.registry.snapshot(localGraph.annotation))
        assertNull(first.registry.snapshot(foreignGraph.annotation))
        assertNull(second.registry.snapshot(foreignGraph.annotation))
        first.registry.publish(localProduct)
        second.registry.publish(foreignProduct)
        assertSame(localGraph.owner, first.registry.snapshot(localGraph.annotation)?.owner)
        assertSame(foreignGraph.owner, second.registry.snapshot(foreignGraph.annotation)?.owner)
    }

    @Test
    @Timeout(60)
    fun independentBuildersCanPublishWhileReadersKeepSeeingTheEarlierFile() {
        val fixture = Fixture()
        val prior = fixture.annotatedFunction("prior")
        fixture.registry.publish(prior.builder.seal(listOf(prior.owner)))
        val priorMetadata = requireNotNull(fixture.registry.snapshot(prior.annotation))
        val ready = CountDownLatch(2)
        val publish = CountDownLatch(1)
        val complete = CountDownLatch(2)
        val pool = Executors.newFixedThreadPool(3)
        try {
            val generations = (0..1).map { generation ->
                val builder = CfirAnnotationMetadataBuilder()
                val owners = (0 until 24).map { index -> fixture.function("generation${generation}_$index") }
                val annotations = owners.map { fixture.annotation(it, it.symbol) }
                Triple(builder, owners, annotations)
            }
            val writers = generations.map { (builder, owners, annotations) ->
                pool.submit {
                    try {
                        owners.zip(annotations).forEach { (owner, annotation) -> fixture.record(builder, owner, annotation) }
                        val product = builder.seal(owners)
                        ready.countDown()
                        check(publish.await(20, TimeUnit.SECONDS))
                        fixture.registry.publish(product)
                    } finally {
                        complete.countDown()
                    }
                }
            }
            assertTrue(ready.await(20, TimeUnit.SECONDS))
            generations.forEach { (_, _, annotations) -> annotations.forEach { assertNull(fixture.registry.snapshot(it)) } }
            val reader = pool.submit {
                publish.countDown()
                do {
                    assertSame(priorMetadata, fixture.registry.snapshot(prior.annotation))
                    for ((_, owners, annotations) in generations) {
                        if (fixture.registry.snapshot(annotations.first()) != null) {
                            owners.zip(annotations).forEach { (owner, annotation) ->
                                assertSame(owner, fixture.registry.snapshot(annotation)?.owner)
                            }
                        }
                    }
                } while (complete.count > 0)
            }
            writers.forEach { it.get(30, TimeUnit.SECONDS) }
            reader.get(30, TimeUnit.SECONDS)
            for ((_, owners, annotations) in generations) {
                owners.zip(annotations).forEach { (owner, annotation) -> assertSame(owner, fixture.registry.snapshot(annotation)?.owner) }
            }
            assertSame(priorMetadata, fixture.registry.snapshot(prior.annotation))
        } finally {
            publish.countDown()
            pool.shutdownNow()
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))
        }
    }

    @Test
    @Timeout(60)
    fun sessionIndexDoesNotRetainOldGenerationButLiveOwnerRetainsItsMetadata() {
        val fixture = Fixture()
        val oldReferences = publishCollectableGeneration(fixture)
        val currentOwner = publishRetainedGeneration(fixture)
        awaitCollected(oldReferences)
        val currentAnnotation = currentOwner.annotations.single() as CfirAnnotationCall
        assertSame(currentOwner, fixture.registry.snapshot(currentAnnotation)?.owner)
        Reference.reachabilityFence(currentOwner)
    }

    @Test
    @Timeout(60)
    fun retainingOneOwnerDoesNotRetainOtherOwnersFromTheSamePublication() {
        val fixture = Fixture()
        val (retainedOwner, collectableReferences) = publishPartiallyRetainedGeneration(fixture)
        awaitCollected(collectableReferences)
        val annotation = retainedOwner.annotations.single() as CfirAnnotationCall
        assertSame(retainedOwner, fixture.registry.snapshot(annotation)?.owner)
        Reference.reachabilityFence(retainedOwner)
    }

    /** 将强局部变量留在独立栈帧，GC 断言仅保留 WeakReference，不依赖 JIT 的局部变量活跃期。 */
    private fun publishCollectableGeneration(fixture: Fixture): List<WeakReference<*>> {
        val graph = fixture.annotatedFunction("same")
        fixture.registry.publish(graph.builder.seal(listOf(graph.owner)))
        val metadata = requireNotNull(fixture.registry.snapshot(graph.annotation))
        return listOf(WeakReference(graph.owner), WeakReference(graph.annotation), WeakReference(metadata))
    }

    private fun publishRetainedGeneration(fixture: Fixture): CfirNamedFunction {
        val graph = fixture.annotatedFunction("same")
        fixture.registry.publish(graph.builder.seal(listOf(graph.owner)))
        return graph.owner
    }

    private fun publishPartiallyRetainedGeneration(fixture: Fixture): Pair<CfirNamedFunction, List<WeakReference<*>>> {
        val builder = CfirAnnotationMetadataBuilder()
        val retained = fixture.function("retained")
        val retainedAnnotation = fixture.annotation(retained, retained.symbol)
        fixture.record(builder, retained, retainedAnnotation)
        val discarded = fixture.function("discarded")
        val discardedAnnotation = fixture.annotation(discarded, discarded.symbol)
        fixture.record(builder, discarded, discardedAnnotation)
        fixture.registry.publish(builder.seal(listOf(fixture.file("both.cj", listOf(retained, discarded)))))
        val discardedMetadata = requireNotNull(fixture.registry.snapshot(discardedAnnotation))
        return retained to listOf(WeakReference(discarded), WeakReference(discardedAnnotation), WeakReference(discardedMetadata))
    }

    /** 沿用项目 CleanableWeakValueReferenceCacheTest 的有界真实 GC 重试，不引入 LL 依赖。 */
    private fun awaitCollected(references: List<WeakReference<*>>) {
        repeat(1000) {
            System.gc()
            System.runFinalization()
            if (references.all { it.get() == null }) return
            Thread.sleep(20)
        }
        references.forEach { assertNull(it.get(), "session annotation 索引不得强持有已丢弃的声明图或 metadata") }
    }

    /** 同一屏障同时释放发布者；返回每个操作结果，不假定哪个合法竞争者先取得 session 锁。 */
    private fun race(vararg actions: () -> Unit): List<Throwable?> {
        val ready = CountDownLatch(actions.size)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(actions.size)
        try {
            val futures = actions.map { action ->
                pool.submit<Throwable?> {
                    ready.countDown()
                    check(start.await(20, TimeUnit.SECONDS))
                    runCatching { action() }.exceptionOrNull()
                }
            }
            assertTrue(ready.await(20, TimeUnit.SECONDS))
            start.countDown()
            return futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            start.countDown()
            pool.shutdownNow()
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS))
        }
    }

    private fun assertProvenance(expected: CfirAnnotationSlotSnapshot, actual: CfirAnnotationSlotSnapshot) {
        assertEquals(expected.rawSyntax, actual.rawSyntax)
        assertEquals(expected.forcedCustom, actual.forcedCustom)
        assertEquals(expected.isCompileTimeVisible, actual.isCompileTimeVisible)
        assertSame(expected.annotationSource, actual.annotationSource)
        assertEquals(expected.qualifiedName, actual.qualifiedName)
        assertEquals(expected.argumentText, actual.argumentText)
        assertEquals(expected.tokens, actual.tokens)
        assertEquals(expected.callSite, actual.callSite)
    }

    private data class AnnotatedFunction(
        val builder: CfirAnnotationMetadataBuilder,
        val owner: CfirNamedFunction,
        val annotation: CfirAnnotationCall,
    )

    private class Fixture {
        val session: CfirSession = object : CfirSession(Kind.Source) {}
        val registry = session.ensureAnnotationMetadataRegistry()
        val moduleData: CfirModuleData = CfirSourceModuleData(
            Name.identifier("metadata-test"), emptyList(), emptyList(), CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT,
        ).also { it.bindSession(session) }

        fun function(name: String): CfirNamedFunction = buildNamedFunction {
            source = TestSource("function:$name")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = CfirNamedFunctionSymbol(CallableId(FqName("metadata"), Name.identifier(name)))
            this.name = Name.identifier(name)
            isMut = false
        }

        fun parameter(owner: CfirNamedFunction, name: String): CfirValueParameter = buildValueParameter {
            source = TestSource("parameter:$name")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            isNamed = false
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = CfirValueParameterSymbol(CallableId(Name.identifier(name)))
            containingDeclarationSymbol = owner.symbol
            this.name = Name.identifier(name)
        }.also { owner.replaceValueParameters(owner.valueParameters + it) }

        fun field(name: String): CfirFieldVariable = buildFieldVariable {
            source = TestSource("field:$name")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildImplicitTypeRef()
            symbol = CfirFieldVariableSymbol(CallableId(FqName("metadata"), Name.identifier(name)))
            this.name = Name.identifier(name)
            isVar = false
        }

        fun file(name: String, declarations: List<CfirDeclaration>): CfirFile = buildFile {
            source = TestSource("file:$name")
            moduleData = this@Fixture.moduleData
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            this.name = name
            packageDirective = buildPackageDirective { packageFqName = FqName("metadata"); isMacroPackage = false }
            this.declarations += declarations
        }

        fun newAnnotation(context: CfirBasedSymbol<*>): CfirAnnotationCall = buildAnnotationCall {
            source = TestSource("annotation")
            typeRef = buildImplicitTypeRef()
            annotationSourceName = "Tag"
            sourceModuleName = "metadata"
            forcedCustom = true
            isCompileTimeVisible = true
            argumentList = buildArgumentList()
            calleeReference = buildNamedReference { name = Name.identifier("Tag") }
            containingDeclarationSymbol = context
        }

        fun annotation(owner: CfirDeclaration, context: CfirBasedSymbol<*>): CfirAnnotationCall =
            newAnnotation(context).also { owner.replaceAnnotations(owner.annotations + it) }

        fun slot(owner: CfirDeclaration, annotation: CfirAnnotationCall): CfirAnnotationSlotSnapshot = CfirAnnotationSlotSnapshot(
            owner = owner,
            annotationIndex = owner.annotations.indexOfFirst { it === annotation },
            originalAnnotation = annotation,
            rawSyntax = "@!Tag[42]",
            forcedCustom = true,
            isCompileTimeVisible = true,
            annotationSource = requireNotNull(annotation.source),
            qualifiedName = FqName("metadata.Tag"),
            argumentText = "[42]",
            tokens = listOf(MacroSurfaceToken("@!Tag", 0, 5), MacroSurfaceToken("[42]", 5, 9)),
            callSite = if (owner is CfirValueParameter) MacroCallSite.PARAMETER else MacroCallSite.DECLARATION,
        )

        fun record(builder: CfirAnnotationMetadataBuilder, owner: CfirDeclaration, annotation: CfirAnnotationCall): CfirAnnotationReplaceCarrier =
            builder.record(slot(owner, annotation))

        fun annotatedFunction(name: String): AnnotatedFunction {
            val builder = CfirAnnotationMetadataBuilder()
            val owner = function(name)
            val annotation = annotation(owner, owner.symbol)
            record(builder, owner, annotation)
            return AnnotatedFunction(builder, owner, annotation)
        }
    }

    /** 同名不同代故意拥有相同 source identity，索引仍必须按真实 annotation 对象区分。 */
    private class TestSource(identity: String) : CjBinarySourceElement(identity, null, identity)
}
