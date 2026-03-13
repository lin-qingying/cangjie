package org.cangjie.cfir.tree.generator

import org.cangjie.cfir.tree.generator.util.generatedType
import org.cangjie.cfir.tree.generator.util.type
import org.cangjie.generators.tree.TypeKind

val cfirVisitorType = generatedType("visitors", "CfirVisitor")
val cfirVisitorVoidType = generatedType("visitors", "CfirVisitorVoid")
val cfirDefaultVisitorType = generatedType("visitors", "CfirDefaultVisitor")
val cfirDefaultVisitorVoidType = generatedType("visitors", "CfirDefaultVisitorVoid")
val cfirTransformerType = generatedType("visitors", "CfirTransformer")

val cfirElementType = generatedType("CfirElement")
val pureAbstractElementType = generatedType("CfirPureAbstractElement")
val cfirImplementationDetailType = generatedType("CfirImplementationDetail", kind = TypeKind.Class)
val cfirBuilderDslAnnotation = generatedType("builder", "CfirBuilderDsl", kind = TypeKind.Class)
