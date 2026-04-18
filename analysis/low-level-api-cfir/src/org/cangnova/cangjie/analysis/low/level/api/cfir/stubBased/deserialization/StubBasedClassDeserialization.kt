/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.stubs.Stub
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.CjRealPsiSourceElement
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.CfirRegularClassBuilder
import org.cangnova.cangjie.cfir.declarations.builder.buildOuterClassTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.builder.buildRegularClass
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.comparators.CfirMemberDeclarationComparator
import org.cangnova.cangjie.cfir.declarations.impl.CfirResolvedDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirResolvedDeclarationStatusWithLazyEffectiveVisibility
import org.cangnova.cangjie.cfir.declarations.utils.*
import org.cangnova.cangjie.cfir.deserialization.addCloneForArrayIfNeeded
import org.cangnova.cangjie.cfir.deserialization.deserializationExtension
import org.cangnova.cangjie.cfir.deserialization.toLazyEffectiveVisibility
import org.cangnova.cangjie.cfir.resolve.transformers.setLazyPublishedVisibility
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.symbols.impl.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirRegularClassSymbol
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeRigidType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.impl.ConeClassLikeTypeImpl
import org.cangnova.cangjie.cfir.types.toLookupTag
import org.cangnova.cangjie.cfir.utils.exceptions.withConeTypeEntry
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.StandardClassIds
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.impl.KotlinClassStubImpl
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

internal val CjModifierListOwner.visibility: Visibility
    get() = with(modifierList) {
        when {
            this == null -> Visibilities.Public
            hasModifier(CjTokens.PRIVATE_KEYWORD) -> Visibilities.Private
            hasModifier(CjTokens.PUBLIC_KEYWORD) -> Visibilities.Public
            hasModifier(CjTokens.PROTECTED_KEYWORD) -> Visibilities.Protected
            else -> if (hasModifier(CjTokens.INTERNAL_KEYWORD)) Visibilities.Internal else Visibilities.Public
        }
    }

internal val CjDeclaration.modality: Modality
    get() = when {
        hasModifier(CjTokens.SEALED_KEYWORD) -> Modality.SEALED
        hasModifier(CjTokens.ABSTRACT_KEYWORD) || this is CjClass && isInterface() -> Modality.ABSTRACT
        hasModifier(CjTokens.OPEN_KEYWORD) -> Modality.OPEN
        else -> Modality.FINAL
    }

/**
 * Gets or calculates stub for [this] element and casts it to [S].
 *
 * [S] has to be a real stub implementation class. For instance, for [CjNamedFunction] it has to be [org.cangnova.cangjie.psi.stubs.impl.KotlinFunctionStubImpl].
 *
 * @return compiled stub
 */
internal inline val <T, reified S> T.compiledStub: S where T : StubBasedPsiElementBase<in S>, T : CjElement, S : StubElement<*>
    get() = (this.greenStub ?: calculateStub()) as S

private fun <S, T> T.calculateStub(): Stub where T : StubBasedPsiElementBase<in S>, T : CjElement, S : StubElement<*> {
    val ktFile = containingCjFile
    requireWithAttachment(ktFile.isCompiled, { "Expected compiled file" }) {
        withPsiEntry("ktFile", ktFile)
    }

    // `let` is used to hold the stub tree reference on the stack
    return ktFile.calcStubTree().let {
        val stub = greenStub
        requireWithAttachment(stub != null, { "Stub should be not null" }) {
            withPsiEntry("file", containingFile)
            withPsiEntry("element", this@calculateStub)
        }

        stub
    }
}

internal fun deserializeClassToSymbol(
    classId: ClassId,
    classOrObject: CjClassOrObject,
    symbol: CfirRegularClassSymbol,
    session: CfirSession,
    moduleData: CfirModuleData,
    defaultAnnotationDeserializer: StubBasedAnnotationDeserializer?,
    scopeProvider: CfirScopeProvider,
    parentContext: StubBasedCfirDeserializationContext? = null,
    containerSource: DeserializedContainerSource? = null,
    deserializeNestedClassLikeDeclaration: (ClassId, CjClassLikeDeclaration, StubBasedCfirDeserializationContext) -> CfirClassLikeSymbol<*>?,
    initialOrigin: CfirDeclarationOrigin,
) {
    val kind = when (classOrObject) {
        is CjObjectDeclaration -> ClassKind.CLASS
        is CjClass -> when {
            classOrObject.isInterface() -> ClassKind.INTERFACE
            classOrObject.isEnum() -> ClassKind.ENUM_CLASS
            else -> ClassKind.CLASS
        }
        else -> errorWithAttachment("Unexpected class or object: ${classOrObject::class}") {
            withPsiEntry("class", classOrObject)
        }
    }
    val modality = classOrObject.modality
    val visibility = classOrObject.visibility
    val status = CfirResolvedDeclarationStatusWithLazyEffectiveVisibility(
        visibility,
        modality,
        visibility.toLazyEffectiveVisibility(parentContext?.outerClassSymbol, session, forClass = true)
    )
    val annotationDeserializer = defaultAnnotationDeserializer ?: StubBasedAnnotationDeserializer(session)
    val context =
        parentContext?.childContext(
            classOrObject,
            classId.relativeClassName,
            containerSource,
            symbol,
            annotationDeserializer,
            false
        ) ?: StubBasedCfirDeserializationContext.createForClass(
            classId,
            classOrObject,
            moduleData,
            annotationDeserializer,
            containerSource,
            symbol,
            initialOrigin
        )
    buildRegularClass {
        source = CjRealPsiSourceElement(classOrObject)
        this.moduleData = moduleData
        this.origin = initialOrigin
        name = classId.shortClassName
        this.status = status
        classKind = kind
        this.scopeProvider = scopeProvider
        this.symbol = symbol

        resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES

        typeParameters += context.typeDeserializer.ownTypeParameters.map { it.fir }

        val typeDeserializer = context.typeDeserializer
        val memberDeserializer = context.memberDeserializer

        val superTypeList = classOrObject.getSuperTypeList()
        if (superTypeList != null) {
            superTypeRefs.addAll(superTypeList.entries.map { superTypeReference ->
                typeDeserializer.typeRef(
                    superTypeReference.typeReference
                        ?: errorWithAttachment("Super entry doesn't have type reference") {
                            withPsiEntry("superTypeReference", superTypeReference)
                        }
                )
            })
        } else if (StandardClassIds.Any != classId && StandardClassIds.Nothing != classId) {
            superTypeRefs.add(session.builtinTypes.anyType)
        }

        classOrObject.primaryConstructor?.let { constructor ->
            addDeclaration(memberDeserializer.loadConstructor(constructor, classOrObject, this))
        }

        classOrObject.body?.declarations?.forEach { declaration ->
            when (declaration) {
                is CjConstructor<*> -> addDeclaration(memberDeserializer.loadConstructor(declaration, classOrObject, this))
                is CjNamedFunction -> addDeclaration(memberDeserializer.loadFunction(declaration, symbol, session))
                is CjProperty -> addDeclaration(
                    memberDeserializer.loadProperty(
                        property = declaration,
                        classSymbol = symbol,
                    )
                )
                is CjEnumEntry -> addDeclaration(memberDeserializer.loadEnumEntry(declaration, symbol, classId))
                is CjClassOrObject,
                is CjTypeAlias
                    -> {
                    val name = declaration.name
                        ?: errorWithAttachment("${if (declaration is CjClassOrObject) "Class" else "Typealias"} doesn't have name") {
                            withPsiEntry(if (declaration is CjClassOrObject) "Class" else "Typealias", declaration)
                        }

                    val nestedClassId = classId.createNestedClassId(Name.identifier(name))
                    // Add declaration to the context to avoid redundant provider access to the class/typealias map
                    deserializeNestedClassLikeDeclaration(
                        nestedClassId,
                        declaration,
                        context.withClassLikeDeclaration(declaration),
                    )?.fir?.let(this::addDeclaration)
                }
            }
        }

        if (classKind == ClassKind.ENUM_CLASS) {
            generateValuesFunction(
                moduleData,
                classId.packageFqName,
                classId.relativeClassName,
                origin = initialOrigin
            )
            generateValueOfFunction(moduleData, classId.packageFqName, classId.relativeClassName, origin = initialOrigin)
            generateEntriesGetter(moduleData, classId.packageFqName, classId.relativeClassName, origin = initialOrigin)
        }

        addCloneForArrayIfNeeded(classId, context.dispatchReceiver, session)

        if (classId == StandardClassIds.Enum) {
            addCloneForEnumIfNeeded(classOrObject, context.dispatchReceiver)
        }

        session.deserializationExtension?.run {
            configureDeserializedClass(classId)
        }

        declarations.sortWith(object : Comparator<CfirDeclaration> {
            override fun compare(a: CfirDeclaration, b: CfirDeclaration): Int {
                // Reorder members based on their type and name only.
                // See FE 1.0's [DeserializedMemberScope#addMembers].
                if (a is CfirMemberDeclaration && b is CfirMemberDeclaration) {
                    return CfirMemberDeclarationComparator.TypeAndNameComparator.compare(a, b)
                }
                return 0
            }
        })
    }.apply {
        if (classOrObject is CjClass) {
            val classStub: KotlinClassStubImpl = classOrObject.compiledStub
            val clsStubCompiledToJvmDefaultImplementation = classStub.isClsStubCompiledToJvmDefaultImplementation
            if (clsStubCompiledToJvmDefaultImplementation) {
                symbol.cfir.isNewPlaceForBodyGeneration = true
            }
        }

        replaceAnnotations(context.annotationDeserializer.loadAnnotations(classOrObject))

        sourceElement = containerSource

        replaceDeprecationsProvider(getDeprecationsProvider(session))

        setLazyPublishedVisibility(
            hasPublishedApi = classOrObject.annotationEntries.any { StubBasedAnnotationDeserializer.getAnnotationClassId(it) == StandardClassIds.Annotations.PublishedApi },
            parentProperty = null,
            session
        )
    }
}

private fun CfirRegularClassBuilder.addCloneForEnumIfNeeded(classOrObject: CjClassOrObject, dispatchReceiver: ConeClassLikeType?) {
    val hasCloneFunction = classOrObject.declarations
        .any { it is CjNamedFunction && it.name == "clone" && it.valueParameters.isEmpty() }

    if (hasCloneFunction) {
        return
    }

    val anyLookupId = StandardClassIds.Any.toLookupTag()
    val cloneCallableId = StandardClassIds.Callables.clone

    declarations += buildNamedFunction {
        moduleData = this@addCloneForEnumIfNeeded.moduleData
        origin = this@addCloneForEnumIfNeeded.origin
        source = this@addCloneForEnumIfNeeded.source

        resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES

        returnTypeRef = buildResolvedTypeRef {
            coneType = ConeClassLikeTypeImpl(anyLookupId, typeArguments = emptyArray(), isMarkedNullable = false)
        }

        status = CfirResolvedDeclarationStatusImpl(
            Visibilities.Protected,
            Modality.FINAL,
            EffectiveVisibility.Protected(anyLookupId)
        )
        isLocal = false

        name = cloneCallableId.callableName
        symbol = CfirNamedFunctionSymbol(cloneCallableId)
        dispatchReceiverType = dispatchReceiver!!
    }
}
