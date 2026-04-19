/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.CjRealPsiSourceElement
import org.cangnova.cangjie.constant.*
import org.cangnova.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.impl.KotlinAnnotationEntryStubImpl
import org.cangnova.cangjie.psi.stubs.impl.KotlinPropertyStubImpl
import org.cangnova.cangjie.types.ConstantValueKind
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

internal class StubBasedAnnotationDeserializer(private val session: CfirSession) {
    companion object {
        fun getAnnotationClassId(ktAnnotation: CjAnnotationEntry): ClassId {
            val userType = ktAnnotation.calleeExpression?.typeReference?.typeElement
            requireWithAttachment(
                userType is CjUserType,
                { "${CjTypeElement::class.simpleName} should be ${CjUserType::class.simpleName}" },
            ) {
                withPsiEntry("annotationEntry", ktAnnotation)
            }

            return userType.classId()
        }

        val TYPE_ANNOTATIONS_FILTER: (AnnotationUseSiteTarget?) -> Boolean = {
            it == null
        }
    }

    fun loadAnnotations(
        ktAnnotated: CjAnnotated,
        useSiteTargetFilter: ((AnnotationUseSiteTarget?) -> Boolean)? = null,
    ): List<CfirAnnotation> {
        val annotations = ktAnnotated.annotationEntries
        if (annotations.isEmpty()) {
            return emptyList()
        }

        return annotations.mapNotNull { deserializeAnnotation(it, useSiteTargetFilter = useSiteTargetFilter) }
    }

    fun loadConstant(property: CjProperty, isUnsigned: Boolean): CfirExpression? {
        if (!property.hasModifier(CjTokens.CONST_KEYWORD)) return null
        val propertyStub: KotlinPropertyStubImpl = property.compiledStub
        val constantValue = propertyStub.constantInitializer ?: return null
        val resultValue = when {
            !isUnsigned -> constantValue
            constantValue is ByteValue -> UByteValue(constantValue.value)
            constantValue is ShortValue -> UShortValue(constantValue.value)
            constantValue is IntValue -> UIntValue(constantValue.value)
            constantValue is LongValue -> ULongValue(constantValue.value)
            else -> constantValue
        }

        return resolveValue(property, resultValue)
    }

    private fun deserializeAnnotation(
        ktAnnotation: CjAnnotationEntry,
        useSiteTargetFilter: ((AnnotationUseSiteTarget?) -> Boolean)? = null,
    ): CfirAnnotation? {
        val useSiteTarget = ktAnnotation.useSiteTarget?.getAnnotationUseSiteTarget()
        if (useSiteTargetFilter?.invoke(useSiteTarget) == false) {
            return null
        }

        val annotationStub: KotlinAnnotationEntryStubImpl = ktAnnotation.compiledStub
        val valueArguments = annotationStub.valueArguments

        return deserializeAnnotation(
            ktAnnotation,
            getAnnotationClassId(ktAnnotation),
            valueArguments,
            useSiteTarget,
        )
    }

    private fun deserializeAnnotation(
        ktAnnotation: PsiElement,
        classId: ClassId,
        valueArguments: Map<Name, ConstantValue<*>>?,
        useSiteTarget: AnnotationUseSiteTarget? = null
    ): CfirAnnotation {
        return buildAnnotation {
            source = CjRealPsiSourceElement(ktAnnotation)
            annotationTypeRef = buildResolvedTypeRef {
                coneType = classId.toLookupTag().constructClassType()
            }
            this.argumentMapping = buildAnnotationArgumentMapping {
                valueArguments?.forEach { (name, constantValue) ->
                    mapping[name] = resolveValue(ktAnnotation, constantValue)
                }
            }
            useSiteTarget?.let {
                this.useSiteTarget = it
            }
        }
    }

    private fun resolveValue(
        sourceElement: PsiElement,
        value: ConstantValue<*>
    ): CfirExpression {
        return when (value) {
            is EnumValue -> sourceElement.toEnumEntryReferenceExpression(value.enumClassId, value.enumEntryName)
            is ArrayValue -> {
                buildCollectionLiteral {
                    source = CjRealPsiSourceElement(sourceElement)
                    // Not quite precise, yet doesn't require annotation resolution
                    coneTypeOrNull = (inferArrayValueType(value.value) ?: session.builtinTypes.anyType.coneType).createArrayType()

                    argumentList = buildArgumentList {
                        value.value.mapTo(arguments) { resolveValue(sourceElement, it) }
                    }
                }
            }
            is AnnotationValue -> {
                deserializeAnnotation(
                    sourceElement,
                    value.value.classId,
                    value.value.argumentsMapping
                )
            }
            is BooleanValue -> const(ConstantValueKind.Boolean, value.value, session.builtinTypes.booleanType, sourceElement)
            is ByteValue -> const(ConstantValueKind.Byte, value.value, session.builtinTypes.byteType, sourceElement)
            is CharValue -> const(ConstantValueKind.Char, value.value, session.builtinTypes.charType, sourceElement)
            is ShortValue -> const(ConstantValueKind.Short, value.value, session.builtinTypes.shortType, sourceElement)
            is LongValue -> const(ConstantValueKind.Long, value.value, session.builtinTypes.longType, sourceElement)
            is FloatValue -> const(ConstantValueKind.Float, value.value, session.builtinTypes.floatType, sourceElement)
            is DoubleValue -> const(ConstantValueKind.Double, value.value, session.builtinTypes.doubleType, sourceElement)
            is UByteValue -> const(ConstantValueKind.UnsignedByte, value.value, session.builtinTypes.uByteType, sourceElement)
            is UShortValue -> const(ConstantValueKind.UnsignedShort, value.value, session.builtinTypes.uShortType, sourceElement)
            is UIntValue -> const(ConstantValueKind.UnsignedInt, value.value, session.builtinTypes.uIntType, sourceElement)
            is ULongValue -> const(ConstantValueKind.UnsignedLong, value.value, session.builtinTypes.uLongType, sourceElement)
            is IntValue -> const(ConstantValueKind.Int, value.value, session.builtinTypes.intType, sourceElement)
            is StringValue -> const(ConstantValueKind.String, value.value, session.builtinTypes.stringType, sourceElement)
            else -> errorWithAttachment("Unexpected value ${value::class}") {
                withEntry("value", value.toString())
            }
        }
    }

    private fun inferArrayValueType(values: List<ConstantValue<*>>): ConeClassLikeType? {
        if (values.isNotEmpty()) {
            val firstValue = values.first()

            for ((index, value) in values.withIndex()) {
                if (index > 0 && value.javaClass != firstValue.javaClass) {
                    return null
                }
            }

            return when (firstValue) {
                is BooleanValue -> session.builtinTypes.booleanType.coneType
                is ByteValue -> session.builtinTypes.byteType.coneType
                is CharValue -> session.builtinTypes.charType.coneType
                is ShortValue -> session.builtinTypes.shortType.coneType
                is IntValue -> session.builtinTypes.intType.coneType
                is LongValue -> session.builtinTypes.longType.coneType
                is UByteValue -> session.builtinTypes.byteType.coneType
                is UShortValue -> session.builtinTypes.shortType.coneType
                is UIntValue -> session.builtinTypes.intType.coneType
                is ULongValue -> session.builtinTypes.longType.coneType
                is DoubleValue -> session.builtinTypes.doubleType.coneType
                is FloatValue -> session.builtinTypes.floatType.coneType
                is AnnotationValue -> session.builtinTypes.annotationType.coneType
                is StringValue -> session.builtinTypes.stringType.coneType
                is EnumValue -> firstValue.enumClassId.constructClassLikeType(ConeTypeProjection.EMPTY_ARRAY, isMarkedNullable = false)
                is ArrayValue -> values.firstNotNullOfOrNull { inferArrayValueType((it as ArrayValue).value) }?.createArrayType()
                else -> null
            }
        }

        return null
    }

    private fun const(
        kind: ConstantValueKind,
        value: Any?,
        typeRef: CfirResolvedTypeRef,
        sourceElement: PsiElement
    ): CfirLiteralExpression {
        return buildLiteralExpression(
            CjRealPsiSourceElement(sourceElement),
            kind,
            value,
            setType = true
        ).apply { this.replaceConeTypeOrNull(typeRef.coneType) }
    }

    private fun PsiElement.toEnumEntryReferenceExpression(classId: ClassId, entryName: Name): CfirExpression =
        buildEnumEntryDeserializedAccessExpression {
            enumClassId = classId
            enumEntryName = entryName
        }
}
