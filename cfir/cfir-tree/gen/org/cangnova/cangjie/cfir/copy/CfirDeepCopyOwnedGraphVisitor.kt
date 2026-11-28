

// 本文件由生成器自动生成。参见 cfir/cfir-tree/tree-generator/Readme.md.
// 请勿手动修改。

package org.cangnova.cangjie.cfir.copy

import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.builder.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.expressions.impl.*
import org.cangnova.cangjie.cfir.impl.*
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.patterns.builder.*
import org.cangnova.cangjie.cfir.patterns.impl.*
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.references.builder.*
import org.cangnova.cangjie.cfir.references.impl.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.*
import org.cangnova.cangjie.cfir.types.impl.*

/** 全部生成式 concrete 节点的壳分配与拥有字段回填。 */
@OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
internal class CfirDeepCopyOwnedGraphVisitor(context: CfirOwnedGraphCopyContext) : CfirOwnedGraphCopyVisitor(context) {
    /** 与回填使用相同的已选择内容；收集阶段不能重新读取活动 Library getter。 */
    fun collectChildren(original: CfirElement, collector: org.cangnova.cangjie.cfir.visitors.CfirVisitor<Unit, Unit>) {
        when (original) {
            is CfirErrorPrimaryConstructor -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
                context.body(original)?.accept(collector, Unit)
                original.status.accept(collector, Unit)
                original.valueParameters.forEach { it.accept(collector, Unit) }
            }
            is CfirNamedFunction -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
                context.body(original)?.accept(collector, Unit)
                original.status.accept(collector, Unit)
                original.valueParameters.forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
            }
            is CfirMainFunction -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
                context.body(original)?.accept(collector, Unit)
                original.status.accept(collector, Unit)
                original.valueParameters.forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
            }
            is CfirMacroDeclaration -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
                context.body(original)?.accept(collector, Unit)
                original.status.accept(collector, Unit)
                original.valueParameters.forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
            }
            is CfirFinalizer -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
                context.body(original)?.accept(collector, Unit)
                original.status.accept(collector, Unit)
                original.valueParameters.forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
            }
            is CfirConstructor -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
                context.body(original)?.accept(collector, Unit)
                original.status.accept(collector, Unit)
                original.valueParameters.forEach { it.accept(collector, Unit) }
            }
            is CfirPropertyAccessor -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
                context.body(original)?.accept(collector, Unit)
                original.status.accept(collector, Unit)
                original.valueParameters.forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
            }
            is CfirFieldVariable -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.status.accept(collector, Unit)
                context.initializer(original)?.accept(collector, Unit)
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
            }
            is CfirPatternVariable -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.status.accept(collector, Unit)
                context.initializer(original)?.accept(collector, Unit)
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
                original.pattern.accept(collector, Unit)
            }
            is CfirPatternBindingVariable -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.status.accept(collector, Unit)
                context.initializer(original)?.accept(collector, Unit)
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
            }
            is CfirValueParameter -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                context.initializer(original)?.accept(collector, Unit)
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.status.accept(collector, Unit)
                original.returnTypeRef.accept(collector, Unit)
                context.defaultValue(original)?.accept(collector, Unit)
            }
            is CfirErrorFunction -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
                context.body(original)?.accept(collector, Unit)
                original.status.accept(collector, Unit)
                original.valueParameters.forEach { it.accept(collector, Unit) }
            }
            is CfirAnonymousFunction -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
                context.body(original)?.accept(collector, Unit)
                original.status.accept(collector, Unit)
                original.valueParameters.forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.typeRef.accept(collector, Unit)
            }
            is CfirClass -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.status.accept(collector, Unit)
                original.superTypeRefs.forEach { it.accept(collector, Unit) }
                original.declarations.forEach { it.accept(collector, Unit) }
            }
            is CfirInterface -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.status.accept(collector, Unit)
                original.superTypeRefs.forEach { it.accept(collector, Unit) }
                original.declarations.forEach { it.accept(collector, Unit) }
            }
            is CfirStruct -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.status.accept(collector, Unit)
                original.superTypeRefs.forEach { it.accept(collector, Unit) }
                original.declarations.forEach { it.accept(collector, Unit) }
            }
            is CfirEnum -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.status.accept(collector, Unit)
                original.superTypeRefs.forEach { it.accept(collector, Unit) }
                original.declarations.forEach { it.accept(collector, Unit) }
            }
            is CfirEnumConstructor -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.returnTypeRef.accept(collector, Unit)
                original.valueParameters.forEach { it.accept(collector, Unit) }
                context.body(original)?.accept(collector, Unit)
                original.status.accept(collector, Unit)
            }
            is CfirTypeAlias -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.declarations.forEach { it.accept(collector, Unit) }
                original.superTypeRefs.forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.status.accept(collector, Unit)
                original.expandedTypeRef.accept(collector, Unit)
            }
            is CfirProperty -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.status.accept(collector, Unit)
                original.returnTypeRef.accept(collector, Unit)
                original.getter?.accept(collector, Unit)
                original.setter?.accept(collector, Unit)
            }
            is CfirErrorNamedValue -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.status.accept(collector, Unit)
                original.returnTypeRef.accept(collector, Unit)
            }
            is CfirExtend -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.typeParameters.forEach { it.accept(collector, Unit) }
                original.status.accept(collector, Unit)
                original.extendedTypeRef.accept(collector, Unit)
                original.superTypeRefs.forEach { it.accept(collector, Unit) }
                original.declarations.forEach { it.accept(collector, Unit) }
            }
            is CfirFile -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.featuresDirective?.accept(collector, Unit)
                original.packageDirective.accept(collector, Unit)
                original.imports.forEach { it.accept(collector, Unit) }
                original.declarations.forEach { it.accept(collector, Unit) }
            }
            is CfirCodeFragment -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.block.accept(collector, Unit)
            }
            is CfirInvalidDeclaration -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
            }
            is CfirMacroExpansionDeclaration -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
            }
            is CfirTypeParameter -> {
                context.annotations(original).forEach { it.accept(collector, Unit) }
                original.bounds.forEach { it.accept(collector, Unit) }
            }
            else -> original.acceptSourceChildren(collector, Unit)
        }
    }

    override fun visitOptionalExpression(original: CfirOptionalExpression, data: Boolean): CfirElement {
        if (!data) return CfirOptionalExpressionImpl(
            source = original.source,
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) },
            mappedExpression = original.mappedExpression,
            overflowStrategy = original.overflowStrategy,
            expression = original.expression,
        )
        val copied = context.copyOwned(original) as CfirOptionalExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.expression = context.copyOwned(original.expression)
        return copied
    }

    override fun visitOptionalChainExpression(original: CfirOptionalChainExpression, data: Boolean): CfirElement {
        if (!data) return CfirOptionalChainExpressionImpl(
            source = original.source,
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) },
            mappedExpression = original.mappedExpression,
            overflowStrategy = original.overflowStrategy,
            expression = original.expression,
        )
        val copied = context.copyOwned(original) as CfirOptionalChainExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.expression = context.copyOwned(original.expression)
        return copied
    }

    override fun visitIfAvailableExpression(original: CfirIfAvailableExpression, data: Boolean): CfirElement {
        if (!data) return buildIfAvailableExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirIfAvailableExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.condition = original.condition?.let { context.copyOwned(it) }
        copied.thenBranch = context.copyOwned(original.thenBranch)
        copied.elseBranch = context.copyOwned(original.elseBranch)
        return copied
    }

    override fun visitFeaturesDirective(original: CfirFeaturesDirective, data: Boolean): CfirElement {
        if (!data) return CfirFeaturesDirectiveImpl(
            source = original.source,
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            featureIds = original.featureIds.toMutableList(),
        )
        val copied = context.copyOwned(original) as CfirFeaturesDirectiveImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.featureIds.clear()
        copied.featureIds.addAll(original.featureIds.map { it }.toMutableList())
        return copied
    }

    override fun visitPackageDirective(original: CfirPackageDirective, data: Boolean): CfirElement {
        if (!data) return buildPackageDirectiveCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirPackageDirectiveImpl

        return copied
    }

    override fun visitImport(original: CfirImport, data: Boolean): CfirElement {
        if (!data) return CfirImportImpl(
            source = original.source,
            importedFqName = original.importedFqName,
            organizationName = original.organizationName,
            isAllUnder = original.isAllUnder,
            visibility = original.visibility,
            aliasName = original.aliasName,
            aliasSource = original.aliasSource,
            condition = original.condition,
        )
        val copied = context.copyOwned(original) as CfirImportImpl
        copied.condition = original.condition?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitResolvedImport(original: CfirResolvedImport, data: Boolean): CfirElement {
        if (!data) return buildResolvedImportCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirResolvedImportImpl
        copied.condition = original.condition?.let { context.copyOwned(it) }
        copied.delegate = context.copyOwned(original.delegate)
        return copied
    }

    override fun visitFile(original: CfirFile, data: Boolean): CfirElement {
        if (!data) return CfirFileImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            symbol = context.remapSymbol(original.symbol),
            name = original.name,
            sourceFile = original.sourceFile,
            featuresDirective = original.featuresDirective,
            packageDirective = original.packageDirective,
            imports = original.imports.toMutableList(),
            sourceFileLinesMapping = original.sourceFileLinesMapping,
            conditionalCompilationFailures = original.conditionalCompilationFailures.toMutableList(),
            declarations = original.declarations.toMutableList(),
        )
        val copied = context.copyOwned(original) as CfirFileImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.featuresDirective = original.featuresDirective?.let { context.copyOwned(it) }
        copied.packageDirective = context.copyOwned(original.packageDirective)
        copied.imports.clear()
        copied.imports.addAll(original.imports.map { context.copyOwned(it) }.toMutableList())
        copied.conditionalCompilationFailures.clear()
        copied.conditionalCompilationFailures.addAll(original.conditionalCompilationFailures.map { it }.toMutableList())
        copied.declarations.clear()
        copied.declarations.addAll(original.declarations.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitCodeFragment(original: CfirCodeFragment, data: Boolean): CfirElement {
        if (!data) return CfirCodeFragmentImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            symbol = context.remapSymbol(original.symbol),
            block = original.block,
        )
        val copied = context.copyOwned(original) as CfirCodeFragmentImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.block = context.copyOwned(original.block)
        return copied
    }

    override fun visitClass(original: CfirClass, data: Boolean): CfirElement {
        if (!data) return CfirClassImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            deprecationsProvider = UnresolvedDeprecationProvider,
            scopeProvider = original.scopeProvider,
            typeParameters = original.typeParameters.toMutableList(),
            status = original.status,
            symbol = context.remapSymbol(original.symbol),
            superTypeRefs = original.superTypeRefs.toMutableList(),
            declarations = original.declarations.toMutableList(),
            name = context.name(original, original.name),
        )
        val copied = context.copyOwned(original) as CfirClassImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.status = context.copyOwned(original.status)
        copied.superTypeRefs.clear()
        copied.superTypeRefs.addAll(original.superTypeRefs.map { context.copyOwned(it) }.toMutableList())
        copied.declarations.clear()
        copied.declarations.addAll(original.declarations.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitInterface(original: CfirInterface, data: Boolean): CfirElement {
        if (!data) return CfirInterfaceImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            deprecationsProvider = UnresolvedDeprecationProvider,
            scopeProvider = original.scopeProvider,
            typeParameters = original.typeParameters.toMutableList(),
            status = original.status,
            symbol = context.remapSymbol(original.symbol),
            superTypeRefs = original.superTypeRefs.toMutableList(),
            declarations = original.declarations.toMutableList(),
            name = context.name(original, original.name),
        )
        val copied = context.copyOwned(original) as CfirInterfaceImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.status = context.copyOwned(original.status)
        copied.superTypeRefs.clear()
        copied.superTypeRefs.addAll(original.superTypeRefs.map { context.copyOwned(it) }.toMutableList())
        copied.declarations.clear()
        copied.declarations.addAll(original.declarations.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitStruct(original: CfirStruct, data: Boolean): CfirElement {
        if (!data) return CfirStructImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            deprecationsProvider = UnresolvedDeprecationProvider,
            scopeProvider = original.scopeProvider,
            typeParameters = original.typeParameters.toMutableList(),
            status = original.status,
            symbol = context.remapSymbol(original.symbol),
            superTypeRefs = original.superTypeRefs.toMutableList(),
            declarations = original.declarations.toMutableList(),
            name = context.name(original, original.name),
        )
        val copied = context.copyOwned(original) as CfirStructImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.status = context.copyOwned(original.status)
        copied.superTypeRefs.clear()
        copied.superTypeRefs.addAll(original.superTypeRefs.map { context.copyOwned(it) }.toMutableList())
        copied.declarations.clear()
        copied.declarations.addAll(original.declarations.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitEnum(original: CfirEnum, data: Boolean): CfirElement {
        if (!data) return CfirEnumImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            deprecationsProvider = UnresolvedDeprecationProvider,
            scopeProvider = original.scopeProvider,
            typeParameters = original.typeParameters.toMutableList(),
            status = original.status,
            symbol = context.remapSymbol(original.symbol),
            superTypeRefs = original.superTypeRefs.toMutableList(),
            declarations = original.declarations.toMutableList(),
            name = context.name(original, original.name),
            isRefEnum = original.isRefEnum,
            isNonExhaustive = original.isNonExhaustive,
        )
        val copied = context.copyOwned(original) as CfirEnumImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.status = context.copyOwned(original.status)
        copied.superTypeRefs.clear()
        copied.superTypeRefs.addAll(original.superTypeRefs.map { context.copyOwned(it) }.toMutableList())
        copied.declarations.clear()
        copied.declarations.addAll(original.declarations.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitEnumConstructor(original: CfirEnumConstructor, data: Boolean): CfirElement {
        if (!data) return CfirEnumConstructorImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            typeParameters = original.typeParameters.toMutableList(),
            isLocal = original.isLocal,
            returnTypeRef = original.returnTypeRef,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            valueParameters = original.valueParameters.toMutableList(),
            body = context.body(original),
            symbol = context.remapSymbol(original.symbol),
            status = original.status,
            name = context.name(original, original.name),
        )
        val copied = context.copyOwned(original) as CfirEnumConstructorImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.valueParameters.clear()
        copied.valueParameters.addAll(original.valueParameters.map { context.copyOwned(it) }.toMutableList())
        copied.body = context.body(original)?.let { context.copyOwned(it) }
        copied.status = context.copyOwned(original.status)
        return copied
    }

    override fun visitExtend(original: CfirExtend, data: Boolean): CfirElement {
        if (!data) return CfirExtendImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
            status = original.status,
            extendedTypeRef = original.extendedTypeRef,
            superTypeRefs = original.superTypeRefs.toMutableList(),
            declarations = original.declarations.toMutableList(),
        )
        val copied = context.copyOwned(original) as CfirExtendImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.status = context.copyOwned(original.status)
        copied.extendedTypeRef = context.copyOwned(original.extendedTypeRef)
        copied.superTypeRefs.clear()
        copied.superTypeRefs.addAll(original.superTypeRefs.map { context.copyOwned(it) }.toMutableList())
        copied.declarations.clear()
        copied.declarations.addAll(original.declarations.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitTypeAlias(original: CfirTypeAlias, data: Boolean): CfirElement {
        if (!data) return CfirTypeAliasImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            deprecationsProvider = UnresolvedDeprecationProvider,
            declarations = original.declarations.toMutableList(),
            superTypeRefs = original.superTypeRefs.toMutableList(),
            scopeProvider = original.scopeProvider,
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
            status = original.status,
            name = context.name(original, original.name),
            expandedTypeRef = original.expandedTypeRef,
        )
        val copied = context.copyOwned(original) as CfirTypeAliasImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.declarations.clear()
        copied.declarations.addAll(original.declarations.map { context.copyOwned(it) }.toMutableList())
        copied.superTypeRefs.clear()
        copied.superTypeRefs.addAll(original.superTypeRefs.map { context.copyOwned(it) }.toMutableList())
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.status = context.copyOwned(original.status)
        copied.expandedTypeRef = context.copyOwned(original.expandedTypeRef)
        return copied
    }

    override fun visitNamedFunction(original: CfirNamedFunction, data: Boolean): CfirElement {
        if (!data) return CfirNamedFunctionImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            isLocal = original.isLocal,
            returnTypeRef = original.returnTypeRef,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            body = context.body(original),
            status = original.status,
            valueParameters = original.valueParameters.toMutableList(),
            hasVariableLenArg = original.hasVariableLenArg,
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
            name = context.name(original, original.name),
            isMut = original.isMut,
        )
        val copied = context.copyOwned(original) as CfirNamedFunctionImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.body = context.body(original)?.let { context.copyOwned(it) }
        copied.status = context.copyOwned(original.status)
        copied.valueParameters.clear()
        copied.valueParameters.addAll(original.valueParameters.map { context.copyOwned(it) }.toMutableList())
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitMainFunction(original: CfirMainFunction, data: Boolean): CfirElement {
        if (!data) return CfirMainFunctionImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            isLocal = original.isLocal,
            returnTypeRef = original.returnTypeRef,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            body = context.body(original),
            status = original.status,
            valueParameters = original.valueParameters.toMutableList(),
            hasVariableLenArg = original.hasVariableLenArg,
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
        )
        val copied = context.copyOwned(original) as CfirMainFunctionImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.body = context.body(original)?.let { context.copyOwned(it) }
        copied.status = context.copyOwned(original.status)
        copied.valueParameters.clear()
        copied.valueParameters.addAll(original.valueParameters.map { context.copyOwned(it) }.toMutableList())
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitMacroDeclaration(original: CfirMacroDeclaration, data: Boolean): CfirElement {
        if (!data) return CfirMacroDeclarationImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            isLocal = original.isLocal,
            returnTypeRef = original.returnTypeRef,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            body = context.body(original),
            status = original.status,
            valueParameters = original.valueParameters.toMutableList(),
            hasVariableLenArg = original.hasVariableLenArg,
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
            name = context.name(original, original.name),
        )
        val copied = context.copyOwned(original) as CfirMacroDeclarationImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.body = context.body(original)?.let { context.copyOwned(it) }
        copied.status = context.copyOwned(original.status)
        copied.valueParameters.clear()
        copied.valueParameters.addAll(original.valueParameters.map { context.copyOwned(it) }.toMutableList())
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitFinalizer(original: CfirFinalizer, data: Boolean): CfirElement {
        if (!data) return CfirFinalizerImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            isLocal = original.isLocal,
            returnTypeRef = original.returnTypeRef,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            body = context.body(original),
            status = original.status,
            valueParameters = original.valueParameters.toMutableList(),
            hasVariableLenArg = original.hasVariableLenArg,
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
        )
        val copied = context.copyOwned(original) as CfirFinalizerImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.body = context.body(original)?.let { context.copyOwned(it) }
        copied.status = context.copyOwned(original.status)
        copied.valueParameters.clear()
        copied.valueParameters.addAll(original.valueParameters.map { context.copyOwned(it) }.toMutableList())
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitConstructor(original: CfirConstructor, data: Boolean): CfirElement {
        if (!data) return if (original.isPrimary) CfirPrimaryConstructor(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            typeParameters = original.typeParameters.toMutableList(),
            isLocal = original.isLocal,
            returnTypeRef = original.returnTypeRef,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            body = context.body(original),
            status = original.status,
            valueParameters = original.valueParameters.toMutableList(),
            hasVariableLenArg = original.hasVariableLenArg,
            symbol = context.remapSymbol(original.symbol),
        ) else CfirConstructorImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            typeParameters = original.typeParameters.toMutableList(),
            isLocal = original.isLocal,
            returnTypeRef = original.returnTypeRef,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            body = context.body(original),
            status = original.status,
            valueParameters = original.valueParameters.toMutableList(),
            hasVariableLenArg = original.hasVariableLenArg,
            symbol = context.remapSymbol(original.symbol),
        )
        when (val copied = context.copyOwned(original)) {
            is CfirConstructorImpl -> {
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.body = context.body(original)?.let { context.copyOwned(it) }
        copied.status = context.copyOwned(original.status)
        copied.valueParameters.clear()
        copied.valueParameters.addAll(original.valueParameters.map { context.copyOwned(it) }.toMutableList())
            }
            is CfirPrimaryConstructor -> {
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.body = context.body(original)?.let { context.copyOwned(it) }
        copied.status = context.copyOwned(original.status)
        copied.valueParameters.clear()
        copied.valueParameters.addAll(original.valueParameters.map { context.copyOwned(it) }.toMutableList())
            }
        }
        val copied = context.copyOwned(original)
        return copied
    }

    override fun visitInvalidDeclaration(original: CfirInvalidDeclaration, data: Boolean): CfirElement {
        if (!data) return CfirInvalidDeclarationImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            symbol = context.remapSymbol(original.symbol),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            reason = original.reason,
        )
        val copied = context.copyOwned(original) as CfirInvalidDeclarationImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        return copied
    }

    override fun visitMacroExpansionDeclaration(original: CfirMacroExpansionDeclaration, data: Boolean): CfirElement {
        if (!data) return CfirMacroExpansionDeclarationImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            symbol = context.remapSymbol(original.symbol),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
        )
        val copied = context.copyOwned(original) as CfirMacroExpansionDeclarationImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        return copied
    }

    override fun visitProperty(original: CfirProperty, data: Boolean): CfirElement {
        if (!data) return CfirPropertyImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            isLocal = original.isLocal,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
            status = original.status,
            returnTypeRef = original.returnTypeRef,
            name = context.name(original, original.name),
            getter = original.getter,
            setter = original.setter,
            bodyResolveState = original.bodyResolveState,
        )
        val copied = context.copyOwned(original) as CfirPropertyImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.status = context.copyOwned(original.status)
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.getter = original.getter?.let { context.copyOwned(it) }
        copied.setter = original.setter?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitPropertyAccessor(original: CfirPropertyAccessor, data: Boolean): CfirElement {
        if (!data) return CfirPropertyAccessorImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            isLocal = original.isLocal,
            returnTypeRef = original.returnTypeRef,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            body = context.body(original),
            status = original.status,
            valueParameters = original.valueParameters.toMutableList(),
            hasVariableLenArg = original.hasVariableLenArg,
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
            propertySymbol = context.remapSymbol(original.propertySymbol),
            isGetter = original.isGetter,
        )
        val copied = context.copyOwned(original) as CfirPropertyAccessorImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.body = context.body(original)?.let { context.copyOwned(it) }
        copied.status = context.copyOwned(original.status)
        copied.valueParameters.clear()
        copied.valueParameters.addAll(original.valueParameters.map { context.copyOwned(it) }.toMutableList())
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitFieldVariable(original: CfirFieldVariable, data: Boolean): CfirElement {
        if (!data) return CfirFieldVariableImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            isLocal = original.isLocal,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            status = original.status,
            contentSource = CfirContentSource.Owned,
            initializer = context.initializer(original),
            isVar = original.isVar,
            isTypeImplicit = original.isTypeImplicit,
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
            returnTypeRef = original.returnTypeRef,
            name = context.name(original, original.name),
        )
        val copied = context.copyOwned(original) as CfirFieldVariableImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.status = context.copyOwned(original.status)
        copied.initializer = context.initializer(original)?.let { context.copyOwned(it) }
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        return copied
    }

    override fun visitPatternVariable(original: CfirPatternVariable, data: Boolean): CfirElement {
        if (!data) return CfirPatternVariableImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            isLocal = original.isLocal,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            status = original.status,
            contentSource = CfirContentSource.Owned,
            initializer = context.initializer(original),
            isVar = original.isVar,
            isTypeImplicit = original.isTypeImplicit,
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
            returnTypeRef = original.returnTypeRef,
            pattern = original.pattern,
        )
        val copied = context.copyOwned(original) as CfirPatternVariableImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.status = context.copyOwned(original.status)
        copied.initializer = context.initializer(original)?.let { context.copyOwned(it) }
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.pattern = context.copyOwned(original.pattern)
        return copied
    }

    override fun visitPatternBindingVariable(original: CfirPatternBindingVariable, data: Boolean): CfirElement {
        if (!data) return CfirPatternBindingVariableImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            isLocal = original.isLocal,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            status = original.status,
            contentSource = CfirContentSource.Owned,
            initializer = context.initializer(original),
            isVar = original.isVar,
            isTypeImplicit = original.isTypeImplicit,
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
            returnTypeRef = original.returnTypeRef,
            name = context.name(original, original.name),
        )
        val copied = context.copyOwned(original) as CfirPatternBindingVariableImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.status = context.copyOwned(original.status)
        copied.initializer = context.initializer(original)?.let { context.copyOwned(it) }
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        return copied
    }

    override fun visitValueParameter(original: CfirValueParameter, data: Boolean): CfirElement {
        if (!data) return CfirValueParameterImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            isLocal = original.isLocal,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            contentSource = CfirContentSource.Owned,
            isTypeImplicit = original.isTypeImplicit,
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
            containingDeclarationSymbol = context.remapSymbol(original.containingDeclarationSymbol),
            isNamed = original.isNamed,
            status = original.status,
            returnTypeRef = original.returnTypeRef,
            name = context.name(original, original.name),
            defaultValue = context.defaultValue(original),
        )
        val copied = context.copyOwned(original) as CfirValueParameterImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.status = context.copyOwned(original.status)
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.defaultValue = context.defaultValue(original)?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitTypeParameter(original: CfirTypeParameter, data: Boolean): CfirElement {
        if (!data) return CfirTypeParameterImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            containingDeclarationSymbol = context.remapSymbol(original.containingDeclarationSymbol),
            symbol = context.remapSymbol(original.symbol),
            name = context.name(original, original.name),
            bounds = original.bounds.toMutableList(),
        )
        val copied = context.copyOwned(original) as CfirTypeParameterImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.bounds.clear()
        copied.bounds.addAll(original.bounds.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitConstructedClassTypeParameterRef(original: CfirConstructedClassTypeParameterRef, data: Boolean): CfirElement {
        if (!data) return CfirConstructedClassTypeParameterRefImpl(
            source = original.source,
            symbol = context.remapSymbol(original.symbol),
        )
        val copied = context.copyOwned(original) as CfirConstructedClassTypeParameterRefImpl

        return copied
    }

    override fun visitExpression(original: CfirExpression, data: Boolean): CfirElement {
        if (original !is CfirExpressionStub) return copyHandWrittenExpression(original, data)
        if (!data) return CfirExpressionStub(
            source = original.source,
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) },
            mappedExpression = original.mappedExpression,
            overflowStrategy = original.overflowStrategy,
        )
        val copied = context.copyOwned(original) as CfirExpressionStub
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        return copied
    }

    override fun visitNativeMatchExpression(original: CfirNativeMatchExpression, data: Boolean): CfirElement {
        if (!data) return buildNativeMatchExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirNativeMatchExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.selector = context.copyOwned(original.selector)
        copied.cases.clear()
        copied.cases.addAll(original.cases.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitNativeMatchCase(original: CfirNativeMatchCase, data: Boolean): CfirElement {
        if (!data) return buildNativeMatchCaseCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirNativeMatchCaseImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.patterns.clear()
        copied.patterns.addAll(original.patterns.map { context.copyOwned(it) }.toMutableList())
        copied.guard = original.guard?.let { context.copyOwned(it) }
        copied.body = context.copyOwned(original.body)
        return copied
    }

    override fun visitNativeConditionMatchExpression(original: CfirNativeConditionMatchExpression, data: Boolean): CfirElement {
        if (!data) return buildNativeConditionMatchExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirNativeConditionMatchExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.cases.clear()
        copied.cases.addAll(original.cases.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitNativeConditionMatchCase(original: CfirNativeConditionMatchCase, data: Boolean): CfirElement {
        if (!data) return buildNativeConditionMatchCaseCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirNativeConditionMatchCaseImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.condition = context.copyOwned(original.condition)
        copied.body = context.copyOwned(original.body)
        return copied
    }

    override fun visitLetPatternDestructorExpression(original: CfirLetPatternDestructorExpression, data: Boolean): CfirElement {
        if (!data) return buildLetPatternDestructorExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirLetPatternDestructorExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.initializer = context.copyOwned(original.initializer)
        copied.patterns.clear()
        copied.patterns.addAll(original.patterns.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitNativeCatch(original: CfirNativeCatch, data: Boolean): CfirElement {
        if (!data) return buildNativeCatchCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirNativeCatchImpl
        copied.pattern = context.copyOwned(original.pattern)
        copied.body = context.copyOwned(original.body)
        return copied
    }

    override fun visitNativeTryExpression(original: CfirNativeTryExpression, data: Boolean): CfirElement {
        if (!data) return buildNativeTryExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirNativeTryExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.resources.clear()
        copied.resources.addAll(original.resources.map { context.copyOwned(it) }.toMutableList())
        copied.tryBlock = context.copyOwned(original.tryBlock)
        copied.catches.clear()
        copied.catches.addAll(original.catches.map { context.copyOwned(it) }.toMutableList())
        copied.finallyBlock = original.finallyBlock?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitPrimitiveTypeExpression(original: CfirPrimitiveTypeExpression, data: Boolean): CfirElement {
        if (!data) return buildPrimitiveTypeExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirPrimitiveTypeExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        return copied
    }

    override fun visitWildcardExpression(original: CfirWildcardExpression, data: Boolean): CfirElement {
        if (!data) return buildWildcardExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirWildcardExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        return copied
    }

    override fun visitFunctionArgument(original: CfirFunctionArgument, data: Boolean): CfirElement {
        if (!data) return buildFunctionArgumentCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirFunctionArgumentImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.value = original.value?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitCallExpression(original: CfirCallExpression, data: Boolean): CfirElement {
        if (!data) return buildCallExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirCallExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.base = context.copyOwned(original.base)
        copied.arguments.clear()
        copied.arguments.addAll(original.arguments.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitUnaryExpression(original: CfirUnaryExpression, data: Boolean): CfirElement {
        if (!data) return buildUnaryExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirUnaryExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.operand = context.copyOwned(original.operand)
        return copied
    }

    override fun visitBinaryExpression(original: CfirBinaryExpression, data: Boolean): CfirElement {
        if (!data) return buildBinaryExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirBinaryExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.left = context.copyOwned(original.left)
        copied.right = context.copyOwned(original.right)
        return copied
    }

    override fun visitArrayCreationExpression(original: CfirArrayCreationExpression, data: Boolean): CfirElement {
        if (!data) return buildArrayCreationExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
            initializerSymbol = original.initializerSymbol?.let { context.remapSymbol(it) }
        }
        val copied = context.copyOwned(original) as CfirArrayCreationExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.arguments.clear()
        copied.arguments.addAll(original.arguments.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitPointerExpression(original: CfirPointerExpression, data: Boolean): CfirElement {
        if (!data) return buildPointerExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirPointerExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.argument = original.argument?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitResolvedSpawnExpression(original: CfirResolvedSpawnExpression, data: Boolean): CfirElement {
        if (!data) return buildResolvedSpawnExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirResolvedSpawnExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.argument = original.argument?.let { context.copyOwned(it) }
        copied.future = context.copyOwned(original.future)
        return copied
    }

    override fun visitBlock(original: CfirBlock, data: Boolean): CfirElement {
        if (!data) return buildBlockCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirBlockImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.statements.clear()
        copied.statements.addAll(original.statements.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitLiteralExpression(original: CfirLiteralExpression, data: Boolean): CfirElement {
        if (!data) return buildLiteralExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirLiteralExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        return copied
    }

    override fun visitStringInterpolation(original: CfirStringInterpolation, data: Boolean): CfirElement {
        if (!data) return buildStringInterpolationCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirStringInterpolationImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.parts.clear()
        copied.parts.addAll(original.parts.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitNamedReferenceWithCandidateBase(original: CfirNamedReferenceWithCandidateBase, data: Boolean): CfirElement {
        if (!data) return CfirNamedReferenceWithCandidateBaseImpl(
            source = original.source,
            name = context.name(original, original.name),
            candidateSymbol = context.remapSymbol(original.candidateSymbol),
        )
        val copied = context.copyOwned(original) as CfirNamedReferenceWithCandidateBaseImpl

        return copied
    }

    override fun visitErrorPrimaryConstructor(original: CfirErrorPrimaryConstructor, data: Boolean): CfirElement {
        if (!data) return CfirErrorPrimaryConstructorImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            typeParameters = original.typeParameters.toMutableList(),
            isLocal = original.isLocal,
            returnTypeRef = original.returnTypeRef,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            body = context.body(original),
            status = original.status,
            valueParameters = original.valueParameters.toMutableList(),
            hasVariableLenArg = original.hasVariableLenArg,
            symbol = context.remapSymbol(original.symbol),
            isPrimary = original.isPrimary,
            diagnostic = original.diagnostic,
        )
        val copied = context.copyOwned(original) as CfirErrorPrimaryConstructorImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.body = context.body(original)?.let { context.copyOwned(it) }
        copied.status = context.copyOwned(original.status)
        copied.valueParameters.clear()
        copied.valueParameters.addAll(original.valueParameters.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitFunctionCall(original: CfirFunctionCall, data: Boolean): CfirElement {
        if (!data) return buildFunctionCallCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirFunctionCallImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.calleeReference = context.copyOwned(original.calleeReference)
        copied.dispatchReceiver = original.dispatchReceiver?.let { context.copyOwned(it) }
        copied.explicitReceiver = original.explicitReceiver?.let { context.copyOwned(it) }
        copied.typeArguments = original.typeArguments.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.argumentList = context.copyOwned(original.argumentList)
        return copied
    }

    override fun visitIncrementDecrementExpression(original: CfirIncrementDecrementExpression, data: Boolean): CfirElement {
        if (!data) return CfirIncrementDecrementExpressionImpl(
            source = original.source,
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) },
            mappedExpression = original.mappedExpression,
            overflowStrategy = original.overflowStrategy,
            isPrefix = original.isPrefix,
            operationName = original.operationName,
            expression = original.expression,
            operationSource = original.operationSource,
        )
        val copied = context.copyOwned(original) as CfirIncrementDecrementExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.expression = context.copyOwned(original.expression)
        return copied
    }

    override fun visitErrorNamedReference(original: CfirErrorNamedReference, data: Boolean): CfirElement {
        if (!data) return CfirErrorNamedReferenceImpl(
            source = original.source,
            name = context.name(original, original.name),
            diagnostic = original.diagnostic,
        )
        val copied = context.copyOwned(original) as CfirErrorNamedReferenceImpl

        return copied
    }

    override fun visitInoutArgumentExpression(original: CfirInoutArgumentExpression, data: Boolean): CfirElement {
        if (!data) return buildInoutArgumentExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirInoutArgumentExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.expression = context.copyOwned(original.expression)
        return copied
    }

    override fun visitNamedArgumentExpression(original: CfirNamedArgumentExpression, data: Boolean): CfirElement {
        if (!data) return CfirNamedArgumentExpressionImpl(
            source = original.source,
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) },
            mappedExpression = original.mappedExpression,
            overflowStrategy = original.overflowStrategy,
            expression = original.expression,
            argumentName = original.argumentName,
            nameSource = original.nameSource,
        )
        val copied = context.copyOwned(original) as CfirNamedArgumentExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.expression = context.copyOwned(original.expression)
        return copied
    }

    override fun visitAnnotationCall(original: CfirAnnotationCall, data: Boolean): CfirElement {
        if (!data) return buildAnnotationCallCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
            containingDeclarationSymbol = context.remapSymbol(original.containingDeclarationSymbol)
        }
        val copied = context.copyOwned(original) as CfirAnnotationCallImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.typeRef = context.copyOwned(original.typeRef)
        copied.argumentMapping = context.copyOwned(original.argumentMapping)
        copied.argumentList = context.copyOwned(original.argumentList)
        copied.calleeReference = context.copyOwned(original.calleeReference)
        copied.argumentView = original.argumentView?.let { copyArgumentView(it) }
        return copied
    }

    override fun visitSuperReference(original: CfirSuperReference, data: Boolean): CfirElement {
        if (!data) return CfirSuperReferenceImpl(
            source = original.source,
            superTypeRef = original.superTypeRef,
        )
        val copied = context.copyOwned(original) as CfirSuperReferenceImpl
        copied.superTypeRef = context.copyOwned(original.superTypeRef)
        return copied
    }

    override fun visitThisReference(original: CfirThisReference, data: Boolean): CfirElement {
        if (!data) return buildThisReferenceCopy(original) {
            boundSymbol = original.boundSymbol?.let { context.remapSymbol(it) }
        }
        val copied = context.copyOwned(original) as CfirThisReferenceImpl

        return copied
    }

    override fun visitResolvedQualifier(original: CfirResolvedQualifier, data: Boolean): CfirElement {
        if (!data) return buildResolvedQualifierCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
            qualifierSymbol = original.qualifierSymbol?.let { context.remapSymbol(it) }
        }
        val copied = context.copyOwned(original) as CfirResolvedQualifierImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.explicitParent = original.explicitParent?.let { context.copyOwned(it) }
        copied.nonFatalDiagnostics = original.nonFatalDiagnostics.map { it }.toMutableOrEmpty()
        copied.typeArguments = original.typeArguments.map { context.copyOwned(it) }.toMutableOrEmpty()
        return copied
    }

    override fun visitErrorResolvedQualifier(original: CfirErrorResolvedQualifier, data: Boolean): CfirElement {
        if (!data) return buildErrorResolvedQualifierCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
            qualifierSymbol = original.qualifierSymbol?.let { context.remapSymbol(it) }
        }
        val copied = context.copyOwned(original) as CfirErrorResolvedQualifierImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.explicitParent = original.explicitParent?.let { context.copyOwned(it) }
        copied.nonFatalDiagnostics = original.nonFatalDiagnostics.map { it }.toMutableOrEmpty()
        copied.typeArguments = original.typeArguments.map { context.copyOwned(it) }.toMutableOrEmpty()
        return copied
    }

    override fun visitNamedAccessExpression(original: CfirNamedAccessExpression, data: Boolean): CfirElement {
        if (!data) return buildNamedAccessExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirNamedAccessExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.calleeReference = context.copyOwned(original.calleeReference)
        copied.dispatchReceiver = original.dispatchReceiver?.let { context.copyOwned(it) }
        copied.typeArguments = original.typeArguments.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.explicitReceiver = original.explicitReceiver?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitQualifiedAccessExpression(original: CfirQualifiedAccessExpression, data: Boolean): CfirElement {
        if (!data) return CfirQualifiedAccessExpressionImpl(
            source = original.source,
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) },
            mappedExpression = original.mappedExpression,
            overflowStrategy = original.overflowStrategy,
            calleeReference = original.calleeReference,
            dispatchReceiver = original.dispatchReceiver,
            explicitReceiver = original.explicitReceiver,
            typeArguments = original.typeArguments.toMutableList().toMutableOrEmpty(),
        )
        val copied = context.copyOwned(original) as CfirQualifiedAccessExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.calleeReference = context.copyOwned(original.calleeReference)
        copied.dispatchReceiver = original.dispatchReceiver?.let { context.copyOwned(it) }
        copied.explicitReceiver = original.explicitReceiver?.let { context.copyOwned(it) }
        copied.typeArguments = original.typeArguments.map { context.copyOwned(it) }.toMutableOrEmpty()
        return copied
    }

    override fun visitSuperReceiverExpression(original: CfirSuperReceiverExpression, data: Boolean): CfirElement {
        if (!data) return CfirSuperReceiverExpressionImpl(
            source = original.source,
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) },
            mappedExpression = original.mappedExpression,
            overflowStrategy = original.overflowStrategy,
            dispatchReceiver = original.dispatchReceiver,
            explicitReceiver = original.explicitReceiver,
            typeArguments = original.typeArguments.toMutableList().toMutableOrEmpty(),
            calleeReference = original.calleeReference,
        )
        val copied = context.copyOwned(original) as CfirSuperReceiverExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.dispatchReceiver = original.dispatchReceiver?.let { context.copyOwned(it) }
        copied.explicitReceiver = original.explicitReceiver?.let { context.copyOwned(it) }
        copied.typeArguments = original.typeArguments.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.calleeReference = context.copyOwned(original.calleeReference)
        return copied
    }

    override fun visitErrorFunction(original: CfirErrorFunction, data: Boolean): CfirElement {
        if (!data) return CfirErrorFunctionImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            typeParameters = original.typeParameters.toMutableList(),
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            body = context.body(original),
            status = original.status,
            valueParameters = original.valueParameters.toMutableList(),
            hasVariableLenArg = original.hasVariableLenArg,
            diagnostic = original.diagnostic,
            symbol = context.remapSymbol(original.symbol),
        )
        val copied = context.copyOwned(original) as CfirErrorFunctionImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.body = context.body(original)?.let { context.copyOwned(it) }
        copied.status = context.copyOwned(original.status)
        copied.valueParameters.clear()
        copied.valueParameters.addAll(original.valueParameters.map { context.copyOwned(it) }.toMutableList())
        copied.replaceReturnTypeRef(context.copyOwned(original.returnTypeRef))
        return copied
    }

    override fun visitErrorNamedValue(original: CfirErrorNamedValue, data: Boolean): CfirElement {
        if (!data) return CfirErrorNamedValueImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            typeParameters = original.typeParameters.toMutableList(),
            status = original.status,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            diagnostic = original.diagnostic,
            name = context.name(original, original.name),
            symbol = context.remapSymbol(original.symbol),
        )
        val copied = context.copyOwned(original) as CfirErrorNamedValueImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.status = context.copyOwned(original.status)
        copied.replaceReturnTypeRef(context.copyOwned(original.returnTypeRef))
        return copied
    }

    override fun visitAssignment(original: CfirAssignment, data: Boolean): CfirElement {
        if (!data) return buildAssignmentCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirAssignmentImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.lValue = context.copyOwned(original.lValue)
        copied.rValue = context.copyOwned(original.rValue)
        copied.typeMismatchOutcome = original.typeMismatchOutcome?.let { it.copy(expectedType = context.type(it.expectedType), actualType = context.type(it.actualType)) }
        return copied
    }

    override fun visitAugmentedAssignment(original: CfirAugmentedAssignment, data: Boolean): CfirElement {
        if (!data) return CfirAugmentedAssignmentImpl(
            source = original.source,
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) },
            mappedExpression = original.mappedExpression,
            overflowStrategy = original.overflowStrategy,
            operation = original.operation,
            operationSource = original.operationSource,
            leftArgument = original.leftArgument,
            rightArgument = original.rightArgument,
        )
        val copied = context.copyOwned(original) as CfirAugmentedAssignmentImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.leftArgument = context.copyOwned(original.leftArgument)
        copied.rightArgument = context.copyOwned(original.rightArgument)
        return copied
    }

    override fun visitBinaryOp(original: CfirBinaryOp, data: Boolean): CfirElement {
        if (!data) return buildBinaryOpCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirBinaryOpImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.left = context.copyOwned(original.left)
        copied.right = context.copyOwned(original.right)
        return copied
    }

    override fun visitComparisonExpression(original: CfirComparisonExpression, data: Boolean): CfirElement {
        if (!data) return buildComparisonExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirComparisonExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.left = context.copyOwned(original.left)
        copied.right = context.copyOwned(original.right)
        return copied
    }

    override fun visitTypeOperator(original: CfirTypeOperator, data: Boolean): CfirElement {
        if (!data) return buildTypeOperatorCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirTypeOperatorImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.argument = context.copyOwned(original.argument)
        copied.typeRef = context.copyOwned(original.typeRef)
        return copied
    }

    override fun visitTypeConversion(original: CfirTypeConversion, data: Boolean): CfirElement {
        if (!data) return buildTypeConversionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirTypeConversionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.argument = context.copyOwned(original.argument)
        copied.targetTypeRef = context.copyOwned(original.targetTypeRef)
        return copied
    }

    override fun visitLetPatternExpression(original: CfirLetPatternExpression, data: Boolean): CfirElement {
        if (!data) return buildLetPatternExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirLetPatternExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.initializer = context.copyOwned(original.initializer)
        copied.pattern = context.copyOwned(original.pattern)
        return copied
    }

    override fun visitIfExpression(original: CfirIfExpression, data: Boolean): CfirElement {
        if (!data) return buildIfExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirIfExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.condition = context.copyOwned(original.condition)
        copied.thenBranch = context.copyOwned(original.thenBranch)
        copied.elseBranch = original.elseBranch?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitMatchExpression(original: CfirMatchExpression, data: Boolean): CfirElement {
        if (!data) return buildMatchExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirMatchExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.subject = original.subject?.let { context.copyOwned(it) }
        copied.branches.clear()
        copied.branches.addAll(original.branches.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitOrPattern(original: CfirOrPattern, data: Boolean): CfirElement {
        if (!data) return CfirOrPatternImpl(
            source = original.source,
            semanticTypeRef = original.semanticTypeRef,
            constantValues = original.constantValues.toMutableList(),
            contextExpression = original.contextExpression,
            alternatives = original.alternatives.toMutableList(),
        )
        val copied = context.copyOwned(original) as CfirOrPatternImpl
        copied.semanticTypeRef = original.semanticTypeRef?.let { context.copyOwned(it) }
        copied.constantValues.clear()
        copied.constantValues.addAll(original.constantValues.map { context.copyConstant(it) }.toMutableList())
        copied.contextExpression = original.contextExpression?.let { context.copyReferenceElement(it) }
        copied.alternatives.clear()
        copied.alternatives.addAll(original.alternatives.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitExpressionPattern(original: CfirExpressionPattern, data: Boolean): CfirElement {
        if (!data) return CfirExpressionPatternImpl(
            source = original.source,
            semanticTypeRef = original.semanticTypeRef,
            constantValues = original.constantValues.toMutableList(),
            contextExpression = original.contextExpression,
            expression = original.expression,
        )
        val copied = context.copyOwned(original) as CfirExpressionPatternImpl
        copied.semanticTypeRef = original.semanticTypeRef?.let { context.copyOwned(it) }
        copied.constantValues.clear()
        copied.constantValues.addAll(original.constantValues.map { context.copyConstant(it) }.toMutableList())
        copied.contextExpression = original.contextExpression?.let { context.copyReferenceElement(it) }
        copied.expression = context.copyOwned(original.expression)
        return copied
    }

    override fun visitMatchBranch(original: CfirMatchBranch, data: Boolean): CfirElement {
        if (!data) return buildMatchBranchCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirMatchBranchImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.pattern = context.copyOwned(original.pattern)
        copied.guard = original.guard?.let { context.copyOwned(it) }
        copied.body = context.copyOwned(original.body)
        return copied
    }

    override fun visitCatchPattern(original: CfirCatchPattern, data: Boolean): CfirElement {
        if (!data) return buildCatchPatternCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirCatchPatternImpl
        copied.semanticTypeRef = original.semanticTypeRef?.let { context.copyOwned(it) }
        copied.constantValues.clear()
        copied.constantValues.addAll(original.constantValues.map { context.copyConstant(it) }.toMutableList())
        copied.contextExpression = original.contextExpression?.let { context.copyReferenceElement(it) }
        copied.nestedPattern = context.copyOwned(original.nestedPattern)
        copied.typeRefs.clear()
        copied.typeRefs.addAll(original.typeRefs.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitCatch(original: CfirCatch, data: Boolean): CfirElement {
        if (!data) return buildCatchCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirCatchImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.pattern = context.copyOwned(original.pattern)
        copied.body = context.copyOwned(original.body)
        return copied
    }

    override fun visitCommandTypePattern(original: CfirCommandTypePattern, data: Boolean): CfirElement {
        if (!data) return buildCommandTypePatternCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirCommandTypePatternImpl
        copied.semanticTypeRef = original.semanticTypeRef?.let { context.copyOwned(it) }
        copied.constantValues.clear()
        copied.constantValues.addAll(original.constantValues.map { context.copyConstant(it) }.toMutableList())
        copied.contextExpression = original.contextExpression?.let { context.copyReferenceElement(it) }
        copied.nestedPattern = context.copyOwned(original.nestedPattern)
        copied.typeRefs.clear()
        copied.typeRefs.addAll(original.typeRefs.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitHandleClause(original: CfirHandleClause, data: Boolean): CfirElement {
        if (!data) return CfirHandleClauseImpl(
            source = original.source,
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) },
            mappedExpression = original.mappedExpression,
            overflowStrategy = original.overflowStrategy,
            commandPattern = original.commandPattern,
            body = original.body,
        )
        val copied = context.copyOwned(original) as CfirHandleClauseImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.commandPattern = context.copyOwned(original.commandPattern)
        copied.body = context.copyOwned(original.body)
        return copied
    }

    override fun visitLoopExpression(original: CfirLoopExpression, data: Boolean): CfirElement {
        if (!data) return buildLoopExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirLoopExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.condition = context.copyOwned(original.condition)
        copied.body = context.copyOwned(original.body)
        return copied
    }

    override fun visitForInExpression(original: CfirForInExpression, data: Boolean): CfirElement {
        if (!data) return buildForInExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirForInExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.pattern = context.copyOwned(original.pattern)
        copied.iterable = context.copyOwned(original.iterable)
        copied.patternGuard = original.patternGuard?.let { context.copyOwned(it) }
        copied.body = context.copyOwned(original.body)
        return copied
    }

    override fun visitTryExpression(original: CfirTryExpression, data: Boolean): CfirElement {
        if (!data) return buildTryExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirTryExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.resources.clear()
        copied.resources.addAll(original.resources.map { context.copyOwned(it) }.toMutableList())
        copied.tryBlock = context.copyOwned(original.tryBlock)
        copied.handlers.clear()
        copied.handlers.addAll(original.handlers.map { context.copyOwned(it) }.toMutableList())
        copied.catches.clear()
        copied.catches.addAll(original.catches.map { context.copyOwned(it) }.toMutableList())
        copied.finallyBlock = original.finallyBlock?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitThrowExpression(original: CfirThrowExpression, data: Boolean): CfirElement {
        if (!data) return buildThrowExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirThrowExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.exception = context.copyOwned(original.exception)
        return copied
    }

    override fun visitResolvedDeclarationStatus(original: CfirResolvedDeclarationStatus, data: Boolean): CfirElement {
        if (!data) return CfirResolvedDeclarationStatusImpl(
            source = original.source,
            visibility = original.visibility,
            isVisibilityExplicit = original.isVisibilityExplicit,
            isModalityExplicit = original.isModalityExplicit,
            isAbstractExplicit = original.isAbstractExplicit,
            isOverride = original.isOverride,
            isOperator = original.isOperator,
            isStatic = original.isStatic,
            isConst = original.isConst,
            isMut = original.isMut,
            isUnsafe = original.isUnsafe,
            isForeign = original.isForeign,
            isC = original.isC,
            isCommon = original.isCommon,
            isSpecific = original.isSpecific,
            isCommonWithDefault = original.isCommonWithDefault,
            isRedef = original.isRedef,
            isDefault = original.isDefault,
            isAbstract = original.isAbstract,
            isOpen = original.isOpen,
            isSealed = original.isSealed,
            modality = original.modality,
        )
        val copied = context.copyOwned(original) as CfirResolvedDeclarationStatusImpl

        return copied
    }

    override fun visitPerformExpression(original: CfirPerformExpression, data: Boolean): CfirElement {
        if (!data) return CfirPerformExpressionImpl(
            source = original.source,
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) },
            mappedExpression = original.mappedExpression,
            overflowStrategy = original.overflowStrategy,
            expression = original.expression,
        )
        val copied = context.copyOwned(original) as CfirPerformExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.expression = context.copyOwned(original.expression)
        return copied
    }

    override fun visitResumeExpression(original: CfirResumeExpression, data: Boolean): CfirElement {
        if (!data) return CfirResumeExpressionImpl(
            source = original.source,
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) },
            mappedExpression = original.mappedExpression,
            overflowStrategy = original.overflowStrategy,
            withExpression = original.withExpression,
            throwingExpression = original.throwingExpression,
        )
        val copied = context.copyOwned(original) as CfirResumeExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.withExpression = original.withExpression?.let { context.copyOwned(it) }
        copied.throwingExpression = original.throwingExpression?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitReturnExpression(original: CfirReturnExpression, data: Boolean): CfirElement {
        if (!data) return buildReturnExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
            target = context.target(original.target)
        }
        val copied = context.copyOwned(original) as CfirReturnExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.result = context.copyOwned(original.result)
        return copied
    }

    override fun visitBreakExpression(original: CfirBreakExpression, data: Boolean): CfirElement {
        if (!data) return buildBreakExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
            target = context.target(original.target)
        }
        val copied = context.copyOwned(original) as CfirBreakExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        return copied
    }

    override fun visitContinueExpression(original: CfirContinueExpression, data: Boolean): CfirElement {
        if (!data) return buildContinueExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
            target = context.target(original.target)
        }
        val copied = context.copyOwned(original) as CfirContinueExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        return copied
    }

    override fun visitAnonymousFunction(original: CfirAnonymousFunction, data: Boolean): CfirElement {
        if (!data) return CfirAnonymousFunctionImpl(
            source = original.source,
            moduleData = original.moduleData,
            resolvePhase = original.resolvePhase,
            contentSource = CfirContentSource.Owned,
            annotations = context.annotations(original).toMutableList().toMutableOrEmpty(),
            origin = original.origin,
            attributes = context.copyAttributes(original.attributes),
            isLocal = original.isLocal,
            returnTypeRef = original.returnTypeRef,
            deprecationsProvider = UnresolvedDeprecationProvider,
            dispatchReceiverType = original.dispatchReceiverType?.let { context.simpleType(it) },
            body = context.body(original),
            status = original.status,
            valueParameters = original.valueParameters.toMutableList(),
            hasVariableLenArg = original.hasVariableLenArg,
            typeParameters = original.typeParameters.toMutableList(),
            symbol = context.remapSymbol(original.symbol),
            hasExplicitParameterList = original.hasExplicitParameterList,
            isLambda = original.isLambda,
            typeRef = original.typeRef,
            matchingParameterFunctionType = original.matchingParameterFunctionType?.let { context.type(it) },
        )
        val copied = context.copyOwned(original) as CfirAnonymousFunctionImpl
        copied.annotations = context.annotations(original).map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        copied.body = context.body(original)?.let { context.copyOwned(it) }
        copied.status = context.copyOwned(original.status)
        copied.valueParameters.clear()
        copied.valueParameters.addAll(original.valueParameters.map { context.copyOwned(it) }.toMutableList())
        copied.typeParameters.clear()
        copied.typeParameters.addAll(original.typeParameters.map { context.copyOwned(it) }.toMutableList())
        copied.typeRef = context.copyOwned(original.typeRef)
        return copied
    }

    override fun visitResolvedErrorReference(original: CfirResolvedErrorReference, data: Boolean): CfirElement {
        if (!data) return CfirResolvedErrorReferenceImpl(
            source = original.source,
            name = context.name(original, original.name),
            resolvedSymbol = context.remapSymbol(original.resolvedSymbol),
            diagnostic = original.diagnostic,
        )
        val copied = context.copyOwned(original) as CfirResolvedErrorReferenceImpl

        return copied
    }

    override fun visitAnonymousFunctionExpression(original: CfirAnonymousFunctionExpression, data: Boolean): CfirElement {
        if (!data) return buildAnonymousFunctionExpressionCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirAnonymousFunctionExpressionImpl
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.anonymousFunction = context.copyOwned(original.anonymousFunction)
        return copied
    }

    override fun visitRangeExpression(original: CfirRangeExpression, data: Boolean): CfirElement {
        if (!data) return buildRangeExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirRangeExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.start = context.copyOwned(original.start)
        copied.end = context.copyOwned(original.end)
        copied.step = original.step?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitArrayLiteral(original: CfirArrayLiteral, data: Boolean): CfirElement {
        if (!data) return buildArrayLiteralCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
            initializerSymbol = original.initializerSymbol?.let { context.remapSymbol(it) }
        }
        val copied = context.copyOwned(original) as CfirArrayLiteralImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.elements.clear()
        copied.elements.addAll(original.elements.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitTupleLiteral(original: CfirTupleLiteral, data: Boolean): CfirElement {
        if (!data) return buildTupleLiteralCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirTupleLiteralImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.elements.clear()
        copied.elements.addAll(original.elements.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitSpawnExpression(original: CfirSpawnExpression, data: Boolean): CfirElement {
        if (!data) return buildSpawnExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirSpawnExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.body = context.copyOwned(original.body)
        copied.threadContextArgument = original.threadContextArgument?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitSynchronizedExpression(original: CfirSynchronizedExpression, data: Boolean): CfirElement {
        if (!data) return buildSynchronizedExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirSynchronizedExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.monitor = context.copyOwned(original.monitor)
        copied.body = context.copyOwned(original.body)
        return copied
    }

    override fun visitUnsafeExpression(original: CfirUnsafeExpression, data: Boolean): CfirElement {
        if (!data) return buildUnsafeExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirUnsafeExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.body = context.copyOwned(original.body)
        return copied
    }

    override fun visitQuoteExpression(original: CfirQuoteExpression, data: Boolean): CfirElement {
        if (!data) return buildQuoteExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirQuoteExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.interpolations.clear()
        copied.interpolations.addAll(original.interpolations.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitSubscriptExpression(original: CfirSubscriptExpression, data: Boolean): CfirElement {
        if (!data) return buildSubscriptExpressionCopy(original) {
            coneTypeOrNull = original.coneTypeOrNull?.let { context.type(it) }
        }
        val copied = context.copyOwned(original) as CfirSubscriptExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.receiver = context.copyOwned(original.receiver)
        copied.indices.clear()
        copied.indices.addAll(original.indices.map { context.copyOwned(it) }.toMutableList())
        copied.resolvedGetCall = original.resolvedGetCall?.let { context.copyOwned(it) }
        copied.resolvedSetCall = original.resolvedSetCall?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitErrorExpression(original: CfirErrorExpression, data: Boolean): CfirElement {
        if (!data) return buildErrorExpressionCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirErrorExpressionImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.mappedExpression = original.mappedExpression?.let { context.copyReferenceElement(it) }
        copied.expression = original.expression?.let { context.copyOwned(it) }
        copied.nonExpressionElement = original.nonExpressionElement?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitConstPattern(original: CfirConstPattern, data: Boolean): CfirElement {
        if (!data) return buildConstPatternCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirConstPatternImpl
        copied.semanticTypeRef = original.semanticTypeRef?.let { context.copyOwned(it) }
        copied.constantValues.clear()
        copied.constantValues.addAll(original.constantValues.map { context.copyConstant(it) }.toMutableList())
        copied.contextExpression = original.contextExpression?.let { context.copyReferenceElement(it) }
        copied.expression = context.copyOwned(original.expression)
        copied.operatorCall = original.operatorCall?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitWildcardPattern(original: CfirWildcardPattern, data: Boolean): CfirElement {
        if (!data) return buildWildcardPatternCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirWildcardPatternImpl
        copied.semanticTypeRef = original.semanticTypeRef?.let { context.copyOwned(it) }
        copied.constantValues.clear()
        copied.constantValues.addAll(original.constantValues.map { context.copyConstant(it) }.toMutableList())
        copied.contextExpression = original.contextExpression?.let { context.copyReferenceElement(it) }
        return copied
    }

    override fun visitBindingPattern(original: CfirBindingPattern, data: Boolean): CfirElement {
        if (!data) return buildBindingPatternCopy(original) {
            name = context.name(original, original.name)
        }
        val copied = context.copyOwned(original) as CfirBindingPatternImpl
        copied.semanticTypeRef = original.semanticTypeRef?.let { context.copyOwned(it) }
        copied.constantValues.clear()
        copied.constantValues.addAll(original.constantValues.map { context.copyConstant(it) }.toMutableList())
        copied.contextExpression = original.contextExpression?.let { context.copyReferenceElement(it) }
        copied.typeRef = original.typeRef?.let { context.copyOwned(it) }
        copied.bindingVariable = original.bindingVariable?.let { context.copyOwned(it) }
        copied.nestedPattern = original.nestedPattern?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitVarOrEnumPattern(original: CfirVarOrEnumPattern, data: Boolean): CfirElement {
        if (!data) return buildVarOrEnumPatternCopy(original) {
            name = context.name(original, original.name)
        }
        val copied = context.copyOwned(original) as CfirVarOrEnumPatternImpl
        copied.semanticTypeRef = original.semanticTypeRef?.let { context.copyOwned(it) }
        copied.constantValues.clear()
        copied.constantValues.addAll(original.constantValues.map { context.copyConstant(it) }.toMutableList())
        copied.contextExpression = original.contextExpression?.let { context.copyReferenceElement(it) }
        copied.bindingVariable = original.bindingVariable?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitTuplePattern(original: CfirTuplePattern, data: Boolean): CfirElement {
        if (!data) return buildTuplePatternCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirTuplePatternImpl
        copied.semanticTypeRef = original.semanticTypeRef?.let { context.copyOwned(it) }
        copied.constantValues.clear()
        copied.constantValues.addAll(original.constantValues.map { context.copyConstant(it) }.toMutableList())
        copied.contextExpression = original.contextExpression?.let { context.copyReferenceElement(it) }
        copied.elements.clear()
        copied.elements.addAll(original.elements.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitEnumPattern(original: CfirEnumPattern, data: Boolean): CfirElement {
        if (!data) return buildEnumPatternCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirEnumPatternImpl
        copied.semanticTypeRef = original.semanticTypeRef?.let { context.copyOwned(it) }
        copied.constantValues.clear()
        copied.constantValues.addAll(original.constantValues.map { context.copyConstant(it) }.toMutableList())
        copied.contextExpression = original.contextExpression?.let { context.copyReferenceElement(it) }
        copied.constructor = context.copyOwned(original.constructor)
        copied.arguments.clear()
        copied.arguments.addAll(original.arguments.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitTypePattern(original: CfirTypePattern, data: Boolean): CfirElement {
        if (!data) return buildTypePatternCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirTypePatternImpl
        copied.semanticTypeRef = original.semanticTypeRef?.let { context.copyOwned(it) }
        copied.constantValues.clear()
        copied.constantValues.addAll(original.constantValues.map { context.copyConstant(it) }.toMutableList())
        copied.contextExpression = original.contextExpression?.let { context.copyReferenceElement(it) }
        copied.typeRef = context.copyOwned(original.typeRef)
        copied.nestedPattern = context.copyOwned(original.nestedPattern)
        return copied
    }

    override fun visitResolvedTypeRef(original: CfirResolvedTypeRef, data: Boolean): CfirElement {
        if (!data) return buildResolvedTypeRefCopy(original) {
            coneType = context.type(original.coneType)
        }
        val copied = context.copyOwned(original) as CfirResolvedTypeRefImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.delegatedTypeRef = original.delegatedTypeRef?.let { context.copyOwned(it) }
        return copied
    }

    override fun visitQualifierPart(original: CfirQualifierPart, data: Boolean): CfirElement {
        if (!data) return buildQualifierPartCopy(original) {
            name = context.name(original, original.name)
        }
        val copied = context.copyOwned(original) as CfirQualifierPartImpl
        copied.typeArguments = original.typeArguments.map { context.copyOwned(it) }.toMutableOrEmpty()
        return copied
    }

    override fun visitParenthesizedTypeRef(original: CfirParenthesizedTypeRef, data: Boolean): CfirElement {
        if (!data) return buildParenthesizedTypeRefCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirParenthesizedTypeRefImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.innerTypeRef = context.copyOwned(original.innerTypeRef)
        return copied
    }

    override fun visitUserTypeRef(original: CfirUserTypeRef, data: Boolean): CfirElement {
        if (!data) return buildUserTypeRefCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirUserTypeRefImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.qualifier = original.qualifier.map { context.copyOwned(it) }.toMutableOrEmpty()
        return copied
    }

    override fun visitBasicTypeRef(original: CfirBasicTypeRef, data: Boolean): CfirElement {
        if (!data) return buildBasicTypeRefCopy(original) {
            name = context.name(original, original.name)
        }
        val copied = context.copyOwned(original) as CfirBasicTypeRefImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        return copied
    }

    override fun visitImplicitTypeRef(original: CfirImplicitTypeRef, data: Boolean): CfirElement {
        if (original is ResolvedImplicitTypeRef) return if (data) context.copyOwned(original) else ResolvedImplicitTypeRef(context.allocate(original.typeRef))
        if (!data) return buildImplicitTypeRefCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirImplicitTypeRefImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        return copied
    }

    override fun visitFunctionTypeRef(original: CfirFunctionTypeRef, data: Boolean): CfirElement {
        if (!data) return buildFunctionTypeRefCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirFunctionTypeRefImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.parameterTypeRefs.clear()
        copied.parameterTypeRefs.addAll(original.parameterTypeRefs.map { context.copyOwned(it) }.toMutableList())
        copied.returnTypeRef = context.copyOwned(original.returnTypeRef)
        return copied
    }

    override fun visitOptionTypeRef(original: CfirOptionTypeRef, data: Boolean): CfirElement {
        if (!data) return CfirOptionTypeRefImpl(
            annotations = original.annotations.toMutableList().toMutableOrEmpty(),
            customRenderer = original.customRenderer,
            typeUseOrigin = original.typeUseOrigin,
            source = original.source,
            componentTypeRef = original.componentTypeRef,
        )
        val copied = context.copyOwned(original) as CfirOptionTypeRefImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.componentTypeRef = context.copyOwned(original.componentTypeRef)
        return copied
    }

    override fun visitTupleTypeRef(original: CfirTupleTypeRef, data: Boolean): CfirElement {
        if (!data) return buildTupleTypeRefCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirTupleTypeRefImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.elementTypeRefs.clear()
        copied.elementTypeRefs.addAll(original.elementTypeRefs.map { context.copyOwned(it) }.toMutableList())
        return copied
    }

    override fun visitVArrayTypeRef(original: CfirVArrayTypeRef, data: Boolean): CfirElement {
        if (!data) return buildVArrayTypeRefCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirVArrayTypeRefImpl
        copied.annotations = original.annotations.map { context.copyOwned(it) }.toMutableOrEmpty()
        copied.elementTypeRef = context.copyOwned(original.elementTypeRef)
        return copied
    }

    override fun visitNamedReference(original: CfirNamedReference, data: Boolean): CfirElement {
        if (!data) return buildNamedReferenceCopy(original) {
            name = context.name(original, original.name)
        }
        val copied = context.copyOwned(original) as CfirNamedReferenceImpl

        return copied
    }

    override fun visitResolvedNamedReference(original: CfirResolvedNamedReference, data: Boolean): CfirElement {
        if (original is CfirResolvedAppliedCallableReference) return if (data) context.copyOwned(original) else copyAppliedReference(original)
        if (!data) return buildResolvedNamedReferenceCopy(original) {
            name = context.name(original, original.name)
            resolvedSymbol = context.remapSymbol(original.resolvedSymbol)
        }
        val copied = context.copyOwned(original) as CfirResolvedNamedReferenceImpl

        return copied
    }

    override fun visitResolvedBuiltinOperatorReference(original: CfirResolvedBuiltinOperatorReference, data: Boolean): CfirElement {
        if (!data) return buildResolvedBuiltinOperatorReferenceCopy(original) {
            name = context.name(original, original.name)
            signature = copyOperatorSignature(original.signature)
        }
        val copied = context.copyOwned(original) as CfirResolvedBuiltinOperatorReferenceImpl

        return copied
    }

    override fun visitErrorReference(original: CfirErrorReference, data: Boolean): CfirElement {
        if (!data) return buildErrorReferenceCopy(original) {

        }
        val copied = context.copyOwned(original) as CfirErrorReferenceImpl

        return copied
    }

}
