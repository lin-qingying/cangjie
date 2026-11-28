package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.bindPrimaryConstructorParameterField
import org.cangnova.cangjie.cfir.correspondingField
import org.cangnova.cangjie.cfir.correspondingValueParameter
import org.cangnova.cangjie.cfir.memberParameterFieldInfo
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCallCopy
import org.cangnova.cangjie.cfir.copyWithNewSource
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement

/**
 * 从当前文件图的真实 primary 参数生成字段。调用者必须在本代次所有结构替换之后、metadata seal 之前调用。
 * 这只完成成员参数字段，不授予 macro construction 已完成的身份；不能在已发布图上补展开。
 */
fun materializePrimaryConstructorParameterFields(file: CfirFile, annotationMetadata: CfirAnnotationMetadataBuilder) {
    annotationMetadata.withOpenTransaction {
        // 官方 CheckPrimaryCtorBeforeMerge 只访问 FILE 的直接 class/struct，不进入非法嵌套 nominal 或宏输入。
        for (declaration in file.declarations) {
            val owner = when (declaration) {
                is CfirClass -> declaration
                is CfirStruct -> declaration
                else -> continue
            }
            materializeFields(owner, annotationMetadata)
        }
    }
}

private fun materializeFields(owner: CfirClassLikeDeclaration, metadata: CfirAnnotationMetadataBuilder) {
    val primary = owner.declarations.filterIsInstance<CfirConstructor>().firstOrNull { it.isDeclaredPrimary }
    owner.primaryConstructorFields?.let { previous ->
        checkMaterializedFields(owner, primary, previous)
        return
    }
    if (primary == null) return
    val parameters = primary.valueParameters
    val typeParameters = owner.typeParameters.map { it.symbol }
    // 反序列化参数没有 raw 成员字段事实，其字段已由 CJO 恢复，不在这里重新生成。
    val members = parameters.filter { it.memberParameterFieldInfo != null }
    if (members.isEmpty()) {
        appendPreparedConstructor(owner, primary)
        owner.primaryConstructorFields = PrimaryConstructorFields(primary, parameters.toList(), typeParameters,
            emptyList(), owner.declarations.toList())
        return
    }
    val declarations = owner.declarations as? MutableList<CfirDeclaration>
        ?: error("Source nominal declarations must be mutable before publication.")
    for (parameter in members) {
        check(parameter.containingDeclarationSymbol === primary.symbol && parameter.moduleData === owner.moduleData) {
            "Member parameter does not belong to the final primary constructor."
        }
        check(parameter.correspondingField == null) { "Primary parameter fields must be created by the shared materializer." }
        checkNotNull(parameter.source) { "A raw member parameter must have source provenance." }
        for (annotation in parameter.annotations) {
            check(annotation is CfirAnnotationCall && metadata.snapshot(annotation) != null) {
                "Member parameter annotations must have construction metadata before field derivation."
            }
        }
    }
    val receiverType = owner.symbol.constructType(typeParameters.map { it.constructType() })
    check(receiverType is ConeSimpleCangJieType) { "A class/struct dispatch receiver must be a simple nominal type." }
    val generated = members.mapNotNull { parameter ->
        val info = checkNotNull(parameter.memberParameterFieldInfo)
        val fieldStatus = info.createFieldStatus()
        // 官方反糖排除参数自身的 STATIC 属性，不按构造器修饰符跳过整个 primary。
        if (fieldStatus.isStatic) return@mapNotNull null
        val fieldSource = checkNotNull(parameter.source).fakeElement(CjFakeSourceElementKind.FieldFromParameter)
        val fieldSymbol = CfirFieldVariableSymbol(CallableId(owner.symbol.classId, parameter.name))
        val annotations = parameter.annotations.map { annotation ->
            buildAnnotationCallCopy(annotation as CfirAnnotationCall) { containingDeclarationSymbol = fieldSymbol }
        }
        val field = buildFieldVariable {
            source = fieldSource
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            moduleData = owner.moduleData
            symbol = fieldSymbol
            name = parameter.name
            status = fieldStatus
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = receiverType
            returnTypeRef = parameter.returnTypeRef.copyWithNewSource(fieldSource)
            isTypeImplicit = parameter.isTypeImplicit
            isVar = info.isVar
            this.annotations.addAll(annotations)
        }
        parameter to field
    }
    for ((parameter, field) in generated) {
        bindPrimaryConstructorParameterField(parameter, field)
        field.annotations.forEachIndexed { index, annotation ->
            metadata.recordDerivedSlot(parameter.annotations[index] as CfirAnnotationCall, field, index, annotation as CfirAnnotationCall)
        }
    }
    // 官方按参数顺序 append 到最终 nominal 原成员列表末尾；已有同名字段仍交给重复声明诊断。
    declarations.addAll(generated.map { it.second })
    appendPreparedConstructor(owner, primary)
    owner.primaryConstructorFields = PrimaryConstructorFields(
        primary, parameters.toList(), typeParameters, generated.map { it.second }, owner.declarations.toList(),
    )
}

/** 可注册产品只能在字段生成完成后封闭；工厂不在 cache key 之后补造字段。 */
internal fun CfirFile.checkPrimaryConstructorParameterFieldsMaterialized() {
    for (declaration in declarations) {
        val owner = when (declaration) {
            is CfirClass -> declaration
            is CfirStruct -> declaration
            else -> continue
        }
        val primary = owner.declarations.filterIsInstance<CfirConstructor>().firstOrNull { it.isDeclaredPrimary }
        val state = owner.primaryConstructorFields
        if (state != null) {
            checkMaterializedFields(owner, primary, state)
        } else {
            check(primary == null || primary.valueParameters.none { it.memberParameterFieldInfo != null }) {
                "Raw primary parameter fields were not materialized before publication."
            }
        }
    }
}

private fun checkMaterializedFields(owner: CfirClassLikeDeclaration, primary: CfirConstructor?, previous: PrimaryConstructorFields) {
    check(primary != null) { "Declared primary constructor was removed after field materialization." }
    check(previous.primary === primary && previous.parameters.size == primary.valueParameters.size &&
        previous.parameters.zip(primary.valueParameters).all { (old, current) -> old === current } &&
        previous.typeParameters == owner.typeParameters.map { it.symbol }) {
        "Primary constructor changed after its fields were materialized."
    }
    check(previous.declarations.size == owner.declarations.size &&
        previous.declarations.zip(owner.declarations).all { (old, current) -> old === current }) {
        "Nominal members changed after primary field materialization."
    }
    check(previous.fields.all { field -> field.correspondingValueParameter?.correspondingField === field }) {
        "Materialized primary fields no longer belong to their parameters."
    }
}

/** CFIR 沿 K2 将隐式默认构造也标为 primary；字段反糖只对应官方的真实 PrimaryCtorDecl。 */
private val CfirConstructor.isDeclaredPrimary: Boolean
    get() = isPrimary && origin.fromSource && cjoDeclarationOrigin == null && source?.kind != CjFakeSourceElementKind.ImplicitConstructor

/** 仅记录本代字段生成，重复调用只验证同一成员图，不重新按名称匹配或清理旧字段。 */
private class PrimaryConstructorFields(
    val primary: CfirConstructor,
    val parameters: List<CfirValueParameter>,
    val typeParameters: List<CfirTypeParameterSymbol>,
    val fields: List<CfirFieldVariable>,
    val declarations: List<CfirDeclaration>,
)

private object PrimaryConstructorFieldsKey : CfirDeclarationDataKey()
private var CfirClassLikeDeclaration.primaryConstructorFields: PrimaryConstructorFields? by
    CfirDeclarationDataRegistry.data(PrimaryConstructorFieldsKey)
