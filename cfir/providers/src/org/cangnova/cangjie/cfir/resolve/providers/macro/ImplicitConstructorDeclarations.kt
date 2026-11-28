package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.interopInfo
import org.cangnova.cangjie.cfir.containingClassForStaticMemberAttr
import org.cangnova.cangjie.cfir.declarations.utils.constructedClassTypeParameterRefs
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement

/**
 * 在宏替换完成、源图发布之前确定 raw 隐式构造器是否仍属于最终 class/struct。
 *
 * raw 构建沿用 Kotlin 的早期隐式构造器模型；仓颉的声明宏可以继续改变构造器列表。
 * 官方1.1.3在展开后的 CheckDefaultParamFunc 中先检查 HasCtorForTypeDecl，只有没有
 * 实例构造器时才执行 AddDefaultCtor。因此最终图已有源码/宏实例构造器时，早期隐式
 * 构造器必须退出声明列表；static init不替代实例构造器。
 *
 * 此决策必须先于主构造成员字段派生、metadata封闭和provider注册，两种construction
 * service共用此入口。这里不修改显式构造器、名义参数声明或宏的词法owner。
 */
fun finalizeImplicitConstructorDeclarations(file: CfirFile, annotationMetadata: CfirAnnotationMetadataBuilder) {
    annotationMetadata.withOpenTransaction {
        for (declaration in file.declarations) {
            val owner = when (declaration) {
                is CfirClass -> declaration
                is CfirStruct -> declaration
                else -> continue
            }
            if (!owner.origin.fromSource || owner.cjoDeclarationOrigin != null) continue
            val hasDeclaredInstanceConstructor = owner.declarations.any {
                it is CfirConstructor && !it.status.isStatic && it.origin != CfirDeclarationOrigin.Synthetic.ImplicitConstructor
            }
            val declarations = owner.declarations as MutableList<CfirDeclaration>
            val implicitConstructors = declarations.filterIsInstance<CfirConstructor>().filter {
                !it.status.isStatic && it.origin == CfirDeclarationOrigin.Synthetic.ImplicitConstructor
            }
            val excludedByNativeRole = owner.status.isCommon || owner.status.isSpecific ||
                owner.interopInfo?.objc?.isMirror == true || owner.interopInfo?.objc?.isSyntheticWrapper == true ||
                owner is CfirClass && owner.interopInfo?.java?.isMirror == true
            if (hasDeclaredInstanceConstructor || excludedByNativeRole) declarations.removeAll(implicitConstructors.toSet())
            else if (implicitConstructors.isNotEmpty()) implicitConstructors.forEach { appendPreparedConstructor(owner, it) }
            else {
                // 宏可以删除最后一个显式init；最终声明图而非旧raw presence决定创建。
                val type = owner.symbol.constructType(owner.typeParameters.map { it.symbol.constructType() })
                check(type is ConeSimpleCangJieType)
                val constructor = org.cangnova.cangjie.cfir.builder.buildImplicitPrimaryConstructor(
                    CfirConstructorSymbol(CallableId(owner.symbol.classId, SpecialNames.INIT)),
                    owner.source?.fakeElement(CjFakeSourceElementKind.ImplicitConstructor), owner.moduleData,
                    false, type, constructedClassTypeParameterRefs(owner.typeParameters.map { it.symbol }))
                constructor.containingClassForStaticMemberAttr = (type as ConeClassifierType).lookupTag
                declarations.add(constructor)
            }
        }
    }
}

/**
 * Native AddDefaultCtor/DesugarPrimaryCtor追加的是已准备的构造声明，保持其它成员原顺序。
 * 两类construction owner共用此入口，必须在成员/字段快照封闭及provider注册之前调用。
 */
internal fun appendPreparedConstructor(owner: CfirClassLikeDeclaration, constructor: CfirConstructor) {
    val members = owner.declarations as MutableList<CfirDeclaration>
    val index = members.indexOfFirst { it === constructor }
    check(index >= 0 && members.count { it === constructor } == 1) { "Prepared constructor must have one nominal owner" }
    members.removeAt(index)
    members.add(constructor)
}
