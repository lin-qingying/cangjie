package org.cangnova.cangjie.chir.core.serializer

/** 官方 v1.1.3 CHIR 中 uint32 ID 字段的索引域语义。 */
internal object OfficialChirV113ReferenceSemantics {
    private val fieldsByTable: Map<String, Map<String, OfficialChirV113TableDomain>> = buildMap {
        fun add(domain: OfficialChirV113TableDomain, table: String, vararg fields: String) {
            put(table, get(table).orEmpty() + fields.associateWith { domain })
        }

        add(OfficialChirV113TableDomain.TYPE, "Type", "typeID", "argTys")
        add(OfficialChirV113TableDomain.DEFINITION, "RuneType", "extends")
        add(OfficialChirV113TableDomain.DEFINITION, "BooleanType", "extends")
        add(OfficialChirV113TableDomain.DEFINITION, "UnitType", "extends")
        add(OfficialChirV113TableDomain.DEFINITION, "NothingType", "extends")
        add(OfficialChirV113TableDomain.DEFINITION, "NumericType", "extends")
        add(OfficialChirV113TableDomain.DEFINITION, "CustomType", "customTypeDef")
        add(OfficialChirV113TableDomain.DEFINITION, "CStringType", "extends")
        add(OfficialChirV113TableDomain.DEFINITION, "CPointerType", "extends")
        add(OfficialChirV113TableDomain.TYPE, "GenericType", "upperBounds")
        add(OfficialChirV113TableDomain.VALUE, "WrappedRawMethod", "rawMethod")
        add(OfficialChirV113TableDomain.TYPE, "OverrideSrcFuncType", "type")
        add(OfficialChirV113TableDomain.TYPE, "Value", "type")
        add(OfficialChirV113TableDomain.VALUE, "Value", "valueID")
        add(OfficialChirV113TableDomain.VALUE, "Parameter", "ownedFunc")
        add(OfficialChirV113TableDomain.EXPRESSION, "Parameter", "ownedLambda")
        add(OfficialChirV113TableDomain.EXPRESSION, "LocalVar", "associatedExpr")
        add(OfficialChirV113TableDomain.VALUE, "GlobalVar", "defaultInitVal", "associatedInitFunc")
        add(OfficialChirV113TableDomain.DEFINITION, "GlobalVar", "declaredParent")
        add(OfficialChirV113TableDomain.DEFINITION, "ImportedFunc", "declaredParent")
        add(OfficialChirV113TableDomain.VALUE, "ImportedFunc", "genericDecl", "paramDftValHostFunc")
        add(
            OfficialChirV113TableDomain.TYPE,
            "ImportedFunc",
            "originalLambdaFuncType",
            "originalLambdaGenericTypeParams",
            "genericTypeParams",
        )
        add(OfficialChirV113TableDomain.DEFINITION, "ImportedVar", "declaredParent")
        add(OfficialChirV113TableDomain.VALUE, "Block", "parentGroup", "predecessors")
        add(OfficialChirV113TableDomain.EXPRESSION, "Block", "exprs")
        add(OfficialChirV113TableDomain.TYPE, "Block", "exceptionCatchList")
        add(OfficialChirV113TableDomain.VALUE, "BlockGroup", "entryBlock", "blocks", "ownedFunc")
        add(OfficialChirV113TableDomain.EXPRESSION, "BlockGroup", "ownedExpression")
        add(OfficialChirV113TableDomain.DEFINITION, "Func", "declaredParent")
        add(OfficialChirV113TableDomain.VALUE, "Func", "genericDecl", "paramDftValHostFunc", "body", "params", "retVal")
        add(
            OfficialChirV113TableDomain.TYPE,
            "Func",
            "originalLambdaFuncType",
            "originalLambdaGenericTypeParams",
            "genericTypeParams",
        )
        add(OfficialChirV113TableDomain.TYPE, "MemberVarInfo", "type")
        add(OfficialChirV113TableDomain.VALUE, "MemberVarInfo", "initializerFunc")
        add(OfficialChirV113TableDomain.DEFINITION, "MemberVarInfo", "outerDef")
        add(
            OfficialChirV113TableDomain.TYPE,
            "VirtualMethodInfo",
            "sigType",
            "originalType",
            "parentType",
            "returnType",
        )
        add(OfficialChirV113TableDomain.TYPE, "VirtualMethodInfo", "methodGenericTypeParams")
        add(OfficialChirV113TableDomain.VALUE, "VirtualMethodInfo", "instance")
        add(OfficialChirV113TableDomain.TYPE, "VTableInType", "srcParentType")
        add(OfficialChirV113TableDomain.DEFINITION, "CustomTypeDef", "genericDecl", "extends")
        add(
            OfficialChirV113TableDomain.TYPE,
            "CustomTypeDef",
            "type",
            "implementedInterfaces",
        )
        add(OfficialChirV113TableDomain.DEFINITION, "CustomTypeDef", "customTypeDefID")
        add(
            OfficialChirV113TableDomain.VALUE,
            "CustomTypeDef",
            "methods",
            "staticMemberVars",
            "varInitializationFunc",
        )
        add(OfficialChirV113TableDomain.TYPE, "EnumCtorInfo", "funcType")
        add(OfficialChirV113TableDomain.TYPE, "ExtendDef", "extendedType", "genericParams")
        add(OfficialChirV113TableDomain.TYPE, "AbstractMethodParam", "paramType")
        add(OfficialChirV113TableDomain.TYPE, "AbstractMethodInfo", "methodType", "methodGenericTypeParams")
        add(OfficialChirV113TableDomain.DEFINITION, "AbstractMethodInfo", "parent")
        add(OfficialChirV113TableDomain.TYPE, "ClassDef", "superClass")
        add(OfficialChirV113TableDomain.EXPRESSION, "Expression", "expressionID")
        add(
            OfficialChirV113TableDomain.VALUE,
            "Expression",
            "operands",
            "blockGroups",
            "parentBlock",
            "resultLocalVar",
        )
        add(OfficialChirV113TableDomain.TYPE, "Expression", "resultTy")
        add(OfficialChirV113TableDomain.TYPE, "Allocate", "targetType")
        add(OfficialChirV113TableDomain.TYPE, "VirMethodContext", "originalFuncType", "genericTypeParams")
        add(OfficialChirV113TableDomain.TYPE, "Apply", "instantiatedTypeArgs", "thisType")
        add(OfficialChirV113TableDomain.TYPE, "Invoke", "instantiatedTypeArgs", "thisType")
        add(OfficialChirV113TableDomain.TYPE, "InstanceOf", "targetType")
        add(OfficialChirV113TableDomain.VALUE, "Terminator", "successors")
        add(OfficialChirV113TableDomain.TYPE, "ApplyWithException", "instantiatedTypeArgs", "thisType")
        add(OfficialChirV113TableDomain.TYPE, "InvokeWithException", "instantiatedTypeArgs", "thisType")
        add(OfficialChirV113TableDomain.TYPE, "IntrinsicWithException", "instantiatedTypeArgs")
        add(OfficialChirV113TableDomain.TYPE, "AllocateWithException", "targetType")
        add(OfficialChirV113TableDomain.TYPE, "RawArrayAllocateWithException", "elementType")
        add(OfficialChirV113TableDomain.TYPE, "RawArrayLiteralAllocateWithException", "elementType")
        add(OfficialChirV113TableDomain.VALUE, "SpawnWithException", "executeClosure")
        add(OfficialChirV113TableDomain.TYPE, "RawArrayAllocate", "elementType")
        add(OfficialChirV113TableDomain.TYPE, "RawArrayLiteralAllocate", "elementType")
        add(OfficialChirV113TableDomain.TYPE, "Intrinsic", "instantiatedTypeArgs")
        add(OfficialChirV113TableDomain.TYPE, "Debug", "accurateEnvObjTy")
        add(OfficialChirV113TableDomain.VALUE, "Spawn", "executeClosure")
        add(OfficialChirV113TableDomain.TYPE, "Lambda", "funcTy", "genericTypeParams")
        add(OfficialChirV113TableDomain.VALUE, "Lambda", "params", "body", "retVal")
        add(OfficialChirV113TableDomain.TYPE, "InvokeStatic", "instantiatedTypeArgs", "thisType")
        add(OfficialChirV113TableDomain.TYPE, "GetInstantiateValue", "instantiateTys")
        add(OfficialChirV113TableDomain.TYPE, "InvokeStaticWithException", "instantiatedTypeArgs", "thisType")
        add(OfficialChirV113TableDomain.TYPE, "GetRTTIStatic", "rttiType")
        add(OfficialChirV113TableDomain.VALUE, "CHIRPackage", "packageInitFunc", "packageLiteralInitFunc")
    }

    private val identityFields = mapOf(
        OfficialChirV113TableDomain.TYPE to ("Type" to "typeID"),
        OfficialChirV113TableDomain.VALUE to ("Value" to "valueID"),
        OfficialChirV113TableDomain.EXPRESSION to ("Expression" to "expressionID"),
        OfficialChirV113TableDomain.DEFINITION to ("CustomTypeDef" to "customTypeDefID"),
    )

    fun referenceDomain(schemaType: String, fieldName: String): OfficialChirV113TableDomain? =
        fieldsByTable[schemaType]?.get(fieldName)

    fun identityField(domain: OfficialChirV113TableDomain): Pair<String, String> =
        checkNotNull(identityFields[domain]) { "no identity field for $domain" }

    fun documentedFields(): Map<Pair<String, String>, OfficialChirV113TableDomain> =
        fieldsByTable.flatMap { (table, fields) -> fields.map { (field, domain) -> (table to field) to domain } }.toMap()
}
