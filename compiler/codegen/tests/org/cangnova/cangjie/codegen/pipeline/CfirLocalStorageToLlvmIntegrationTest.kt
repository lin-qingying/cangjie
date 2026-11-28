package org.cangnova.cangjie.codegen.pipeline

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildClass
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildPatternVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildAssignment
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildTupleLiteral
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.patterns.builder.buildTuplePattern
import org.cangnova.cangjie.cfir.patterns.builder.buildWildcardPattern
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.chir.cfir2chir.Cfir2ChirConversionException
import org.cangnova.cangjie.chir.cfir2chir.DefaultCfir2ChirConverter
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirFieldExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirTupleExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue
import org.cangnova.cangjie.codegen.api.ChirCodegenInput
import org.cangnova.cangjie.codegen.api.CodegenOptions
import org.cangnova.cangjie.codegen.pipeline.DefaultChirToLlvmCodeGenerator
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 通过真实 CFIR 局部变量转换验证 CHIR 地址身份贯穿 LLVM lowering。
 */
class CfirLocalStorageToLlvmIntegrationTest {
    /**
     * 验证普通隐式实例成员调用携带 owner TypeInfo 并完整通过 LLVM lowering。
     */
    @Test
    fun `lowers implicit instance member call with owner type info`() {
        val ownerId = ClassId(PACKAGE_NAME, Name.identifier("Receiver"))
        val ownerSymbol = CfirClassSymbol(ownerId)
        val ownerType = ConeClassLikeType(ConeClassLikeLookupTagImpl(ownerId))
        val targetName = Name.identifier("target")
        val targetSymbol = CfirNamedFunctionSymbol(CallableId(PACKAGE_NAME, targetName))
        val target = buildNamedFunction {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = ownerType
            status = CfirDeclarationStatusImpl()
            symbol = targetSymbol
            name = targetName
            returnTypeRef = resolvedIntType()
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += intLiteral(7)
            }
        }
        val call = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.INT32
            calleeReference = buildResolvedNamedReference {
                name = targetName
                resolvedSymbol = targetSymbol
            }
        }
        val callerName = Name.identifier("caller")
        val caller = buildNamedFunction {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = ownerType
            status = CfirDeclarationStatusImpl()
            symbol = CfirNamedFunctionSymbol(CallableId(PACKAGE_NAME, callerName))
            name = callerName
            returnTypeRef = resolvedIntType()
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += call
            }
        }
        val owner = buildClass {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            scopeProvider = UNUSED_SCOPE_PROVIDER
            status = CfirDeclarationStatusImpl()
            name = Name.identifier("Receiver")
            symbol = ownerSymbol
            declarations += target
            declarations += caller
        }
        val file = buildFile {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "instance-member-call.cj"
            packageDirective = buildPackageDirective {
                packageFqName = PACKAGE_NAME
                isMacroPackage = false
            }
            declarations += owner
        }

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val llvm = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = chirPackage,
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        ).modules.single().ir

        assertTrue(llvm.contains("call i32"), llvm)
    }

    /**
     * 验证可变局部变量初始化、赋值和读取最终使用同一 LLVM alloca 结果。
     */
    @Test
    fun `lowers CFIR mutable local storage through one alloca identity`() {
        val localName = Name.identifier("mutableLocal")
        val localSymbol = CfirFieldVariableSymbol(CallableId(PACKAGE_NAME, localName))
        val localVariable = buildFieldVariable {
            commonVariableDeclaration(localSymbol)
            name = localName
            returnTypeRef = resolvedIntType()
            isVar = true
            initializer = intLiteral(1)
        }
        val assignment = buildAssignment {
            coneTypeOrNull = ConePrimitiveType.UNIT
            lValue = localAccess(localSymbol, localName)
            rValue = intLiteral(2)
        }
        val function = buildNamedFunction {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            symbol = CfirNamedFunctionSymbol(CallableId(PACKAGE_NAME, Name.identifier("readMutableLocal")))
            name = Name.identifier("readMutableLocal")
            returnTypeRef = resolvedIntType()
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += localVariable
                statements += assignment
                statements += localAccess(localSymbol, localName)
            }
        }
        val file = buildFile {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "local-storage.cj"
            packageDirective = buildPackageDirective {
                packageFqName = PACKAGE_NAME
                isMacroPackage = false
            }
            declarations += function
        }

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val chirFunction = chirPackage.modules.single().declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "readMutableLocal" }
        val expressions = chirFunction.blocks.flatMap { it.expressions }
        val memoryExpressions = expressions.filterIsInstance<ChirMemoryExpression>()
        val alloca = memoryExpressions.single { it.operation == "alloca" }
        assertEquals(
            ChirResolvedTypeRef(ChirRefType(ChirResolvedTypeRef(ChirPrimitiveType.INT32), mutable = true)),
            alloca.resultType,
        )
        assertEquals("1", (alloca.address as org.cangnova.cangjie.chir.core.value.ChirConstantValue).literal)
        assertTrue(memoryExpressions.filter { it.operation == "store" }.all { it.address.semanticId == alloca.semanticId })
        assertTrue(memoryExpressions.single { it.operation == "load" }.address.semanticId == alloca.semanticId)

        val lowered = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = chirPackage,
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )
        val llvm = lowered.modules.single().ir
        val llvmLines = llvm.lineSequence().map(String::trim).toList()
        val allocation = llvmLines.single { " = alloca i32, i32 1" in it }
        val pointer = allocation.substringBefore(" = alloca")
        val loadResult = llvmLines.single { " = load i32, ptr $pointer" in it }.substringBefore(" = load")
        assertTrue(llvm.contains("store i32 1, ptr $pointer"), llvm)
        assertTrue(llvm.contains("store i32 2, ptr $pointer"), llvm)
        assertTrue(llvm.contains("load i32, ptr $pointer"), llvm)
        assertFalse(llvm.contains("%mutableLocal"), llvm)
        assertTrue(llvmLines.any { it == "ret i32 $loadResult" }, llvm)
    }

    /**
     * 验证 CFIR Unit 字面量和 Unit 调用结果均以 UnitLiteral 值进入 tuple，并完整降低到 LLVM。
     */
    @Test
    fun `lowers CFIR Unit tuple operands through CHIR and LLVM`() {
        val unitFunctionName = Name.identifier("unitEffect")
        val unitFunctionSymbol = CfirNamedFunctionSymbol(CallableId(PACKAGE_NAME, unitFunctionName))
        val unitFunction = buildNamedFunction {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            symbol = unitFunctionSymbol
            name = unitFunctionName
            returnTypeRef = resolvedUnitType()
            isMut = false
            body = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
        }
        val tupleConeType = ConeTupleType(
            listOf(ConePrimitiveType.UNIT, ConePrimitiveType.UNIT, ConePrimitiveType.INT32),
        )
        val unitCall = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.UNIT
            calleeReference = buildResolvedNamedReference {
                name = unitFunctionName
                resolvedSymbol = unitFunctionSymbol
            }
        }
        val tuple = buildTupleLiteral {
            coneTypeOrNull = tupleConeType
            elements += unitLiteral()
            elements += unitCall
            elements += intLiteral(42)
        }
        val tupleFunctionName = Name.identifier("makeUnitTuple")
        val tupleFunction = buildNamedFunction {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            symbol = CfirNamedFunctionSymbol(CallableId(PACKAGE_NAME, tupleFunctionName))
            name = tupleFunctionName
            returnTypeRef = resolvedTupleType(tupleConeType)
            isMut = false
            body = buildBlock {
                coneTypeOrNull = tupleConeType
                statements += tuple
            }
        }
        val file = buildFile {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "unit-tuple.cj"
            packageDirective = buildPackageDirective {
                packageFqName = PACKAGE_NAME
                isMacroPackage = false
            }
            declarations += unitFunction
            declarations += tupleFunction
        }

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val chirFunction = chirPackage.modules.single().declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "makeUnitTuple" }
        val blockExpressions = chirFunction.blocks.single().expressions
        val callIndex = blockExpressions.indexOfFirst { it is ChirCallExpression }
        val tupleIndex = blockExpressions.indexOfFirst { it is ChirTupleExpression }
        val chirTuple = blockExpressions.filterIsInstance<ChirTupleExpression>().single()
        assertTrue(callIndex >= 0 && callIndex < tupleIndex, "Unit call must execute before constructing its tuple value")
        assertEquals(3, chirTuple.elements.size)
        assertTrue(chirTuple.elements[0] is ChirConstantValue)
        assertTrue(chirTuple.elements[1] is ChirConstantValue)
        assertEquals("unit", (chirTuple.elements[0] as ChirConstantValue).literal)
        assertEquals("unit", (chirTuple.elements[1] as ChirConstantValue).literal)

        val lowered = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = chirPackage,
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )
        val llvm = lowered.modules.single().ir
        assertTrue(llvm.contains("call void @unitEffect()"), llvm)
        assertTrue(llvm.contains("%cangjie.unit zeroinitializer, 0"), llvm)
        assertTrue(llvm.contains("%cangjie.unit zeroinitializer, 1"), llvm)
        assertTrue(llvm.contains("ret { %cangjie.unit, %cangjie.unit, i32 }"), llvm)
    }

    /**
     * 验证 Unit 局部变量初始化、重赋值和读取都保留调用副作用及真实内存值。
     */
    @Test
    fun `lowers Unit local storage without dropping calls or loads`() {
        val unitFunctionName = Name.identifier("unitLocalEffect")
        val unitFunctionSymbol = CfirNamedFunctionSymbol(CallableId(PACKAGE_NAME, unitFunctionName))
        val unitFunction = buildNamedFunction {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            symbol = unitFunctionSymbol
            name = unitFunctionName
            returnTypeRef = resolvedUnitType()
            isMut = false
            body = buildBlock { coneTypeOrNull = ConePrimitiveType.UNIT }
        }
        fun unitCall() = buildFunctionCall {
            coneTypeOrNull = ConePrimitiveType.UNIT
            calleeReference = buildResolvedNamedReference {
                name = unitFunctionName
                resolvedSymbol = unitFunctionSymbol
            }
        }

        val localName = Name.identifier("unitLocal")
        val localSymbol = CfirFieldVariableSymbol(CallableId(PACKAGE_NAME, localName))
        val localVariable = buildFieldVariable {
            commonVariableDeclaration(localSymbol)
            name = localName
            returnTypeRef = resolvedUnitType()
            isVar = true
            initializer = unitCall()
        }
        val assignment = buildAssignment {
            coneTypeOrNull = ConePrimitiveType.UNIT
            lValue = unitLocalAccess(localSymbol, localName)
            rValue = unitCall()
        }
        val tupleType = ConeTupleType(listOf(ConePrimitiveType.UNIT, ConePrimitiveType.INT32))
        val tuple = buildTupleLiteral {
            coneTypeOrNull = tupleType
            elements += unitLocalAccess(localSymbol, localName)
            elements += intLiteral(42)
        }
        val functionName = Name.identifier("readUnitLocal")
        val function = buildNamedFunction {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            symbol = CfirNamedFunctionSymbol(CallableId(PACKAGE_NAME, functionName))
            name = functionName
            returnTypeRef = resolvedTupleType(tupleType)
            isMut = false
            body = buildBlock {
                coneTypeOrNull = tupleType
                statements += localVariable
                statements += assignment
                statements += tuple
            }
        }
        val file = buildFile {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "unit-local-storage.cj"
            packageDirective = buildPackageDirective {
                packageFqName = PACKAGE_NAME
                isMacroPackage = false
            }
            declarations += unitFunction
            declarations += function
        }

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val chirFunction = chirPackage.modules.single().declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "readUnitLocal" }
        val expressions = chirFunction.blocks.single().expressions
        val calls = expressions.filterIsInstance<ChirCallExpression>()
        val memoryExpressions = expressions.filterIsInstance<ChirMemoryExpression>()
        val alloca = memoryExpressions.single { it.operation == "alloca" }
        val stores = memoryExpressions.filter { it.operation == "store" }
        val load = memoryExpressions.single { it.operation == "load" }
        val chirTuple = expressions.filterIsInstance<ChirTupleExpression>().single()
        val callIndexes = calls.map(expressions::indexOf)
        val allocaIndex = expressions.indexOf(alloca)
        val storeIndexes = stores.map(expressions::indexOf)
        val loadIndex = expressions.indexOf(load)
        val tupleValue = chirTuple.elements.first()

        assertEquals(2, calls.size, "both Unit-returning calls must remain in CHIR")
        assertEquals(2, stores.size)
        assertTrue(stores.all { (it.value as? ChirConstantValue)?.literal == "unit" })
        assertTrue(callIndexes[0] < allocaIndex)
        assertTrue(allocaIndex < storeIndexes[0])
        assertTrue(storeIndexes[0] < callIndexes[1])
        assertTrue(callIndexes[1] < storeIndexes[1])
        assertTrue(storeIndexes[1] < loadIndex)
        assertEquals(2, chirTuple.elements.size)
        assertTrue(tupleValue is ChirLocalValue)
        assertEquals(load.semanticId, tupleValue.semanticId)

        val lowered = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = chirPackage,
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )
        val llvm = lowered.modules.single().ir
        assertEquals(2, llvm.lineSequence().count { "call void @unitLocalEffect()" in it }, llvm)
        assertTrue(llvm.contains("alloca %cangjie.unit, i32 1"), llvm)
        assertTrue(llvm.contains("store %cangjie.unit zeroinitializer"), llvm)
        assertTrue(llvm.contains("load %cangjie.unit"), llvm)
        assertTrue(llvm.contains("ret { %cangjie.unit, i32 }"), llvm)
    }

    /**
     * 验证嵌套元组及“单个绑定加通配符”都按字段布局绑定，并保留 Unit 投影的真实类型。
     */
    @Test
    fun `lowers nested and wildcard Unit tuple pattern bindings through typed projections`() {
        val nestedTupleType = ConeTupleType(listOf(ConePrimitiveType.INT32, ConePrimitiveType.UNIT))
        val tupleType = ConeTupleType(listOf(ConePrimitiveType.UNIT, nestedTupleType))
        val unitBinding = patternBinding("patternUnit", resolvedUnitType())
        val intBinding = patternBinding("patternInt", resolvedIntType())
        val pattern = buildTuplePattern {
            elements += bindingPattern(unitBinding)
            elements += buildTuplePattern {
                elements += bindingPattern(intBinding)
                elements += buildWildcardPattern {}
            }
        }
        val initializer = buildTupleLiteral {
            coneTypeOrNull = tupleType
            elements += unitLiteral()
            elements += buildTupleLiteral {
                coneTypeOrNull = nestedTupleType
                elements += intLiteral(7)
                elements += unitLiteral()
            }
        }
        val patternVariable = patternVariable("tuplePattern", pattern, initializer, resolvedTupleType(tupleType))

        val singleTupleType = ConeTupleType(listOf(ConePrimitiveType.UNIT, ConePrimitiveType.INT32))
        val singleBinding = patternBinding("singlePatternUnit", resolvedUnitType())
        val singleBindingPattern = buildTuplePattern {
            elements += bindingPattern(singleBinding)
            elements += buildWildcardPattern {}
        }
        val singlePatternVariable = patternVariable(
            "singleTuplePattern",
            singleBindingPattern,
            buildTupleLiteral {
                coneTypeOrNull = singleTupleType
                elements += unitLiteral()
                elements += intLiteral(9)
            },
            resolvedTupleType(singleTupleType),
        )
        val functionName = Name.identifier("readPatternUnit")
        val function = buildNamedFunction {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            symbol = CfirNamedFunctionSymbol(CallableId(PACKAGE_NAME, functionName))
            name = functionName
            returnTypeRef = resolvedUnitType()
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.UNIT
                statements += patternVariable
                statements += singlePatternVariable
                statements += unitLocalAccess(unitBinding.symbol, unitBinding.name)
                statements += unitLocalAccess(singleBinding.symbol, singleBinding.name)
            }
        }
        val file = buildFile {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "unit-pattern-projection.cj"
            packageDirective = buildPackageDirective {
                packageFqName = PACKAGE_NAME
                isMacroPackage = false
            }
            declarations += function
        }

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val chirFunction = chirPackage.modules.single().declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .single { it.name == "readPatternUnit" }
        val expressions = chirFunction.blocks.single().expressions
        val fields = expressions.filterIsInstance<ChirFieldExpression>()
        assertEquals(listOf(listOf(0uL), listOf(1uL, 0uL), listOf(0uL)), fields.map(ChirFieldExpression::path))
        assertEquals(
            listOf(
                ChirResolvedTypeRef(ChirPrimitiveType.UNIT),
                ChirResolvedTypeRef(ChirPrimitiveType.INT32),
                ChirResolvedTypeRef(ChirPrimitiveType.UNIT),
            ),
            fields.map(ChirFieldExpression::resultType),
        )
        assertFalse(expressions.any { it is ChirOtherExpression && it.operation == "cfir.pattern_extract" })

        val lowered = DefaultChirToLlvmCodeGenerator().generate(
            ChirCodegenInput(
                chirPackage = chirPackage,
                options = CodegenOptions(
                    enabled = true,
                    validateChirBeforeLowering = true,
                    verifyBeforeWrite = true,
                    emitBitcode = false,
                    emitComments = false,
                    emitModuleHeader = false,
                    emitRuntimeDeclarations = false,
                ),
            ),
        )
        val llvm = lowered.modules.single().ir
        assertTrue(llvm.contains("extractvalue { %cangjie.unit, { i32, %cangjie.unit } }"), llvm)
        assertTrue(llvm.contains("extractvalue { %cangjie.unit, i32 }"), llvm)
        assertTrue(llvm.contains("store %cangjie.unit %cfir_callable_readPatternUnit_pattern_field_"), llvm)
        assertTrue(llvm.contains("load %cangjie.unit"), llvm)
        assertTrue(llvm.contains("ret void"), llvm)
    }

    /**
     * 元组模式必须逐层匹配输入元组类型和元数，不能把不匹配的聚合类型视为元组布局。
     */
    @Test
    fun `rejects tuple pattern input type and arity mismatches`() {
        val twoElementTupleType = ConeTupleType(listOf(ConePrimitiveType.UNIT, ConePrimitiveType.INT32))
        val oneElementTuplePattern = buildTuplePattern {
            elements += buildWildcardPattern {}
        }
        assertTuplePatternRejected(
            pattern = oneElementTuplePattern,
            initializer = buildTupleLiteral {
                coneTypeOrNull = twoElementTupleType
                elements += unitLiteral()
                elements += intLiteral(1)
            },
            typeRef = resolvedTupleType(twoElementTupleType),
            sourceName = "wrongTupleArity",
        )
        assertTuplePatternRejected(
            pattern = oneElementTuplePattern,
            initializer = intLiteral(1),
            typeRef = resolvedIntType(),
            sourceName = "nonTupleInput",
        )
    }

    /**
     * 构造绑定到局部变量符号的直接读取。
     */
    private fun localAccess(symbol: CfirFieldVariableSymbol, name: Name) = buildNamedAccessExpression {
        coneTypeOrNull = ConePrimitiveType.INT32
        calleeReference = buildResolvedNamedReference {
            this.name = name
            resolvedSymbol = symbol
        }
    }

    /**
     * 构造绑定到 Unit 局部变量符号的读取。
     */
    private fun unitLocalAccess(symbol: CfirFieldVariableSymbol, name: Name) = buildNamedAccessExpression {
        coneTypeOrNull = ConePrimitiveType.UNIT
        calleeReference = buildResolvedNamedReference {
            this.name = name
            resolvedSymbol = symbol
        }
    }

    /**
     * 构造绑定到 Unit 模式绑定符号的读取。
     */
    private fun unitLocalAccess(symbol: CfirPatternBindingSymbol, name: Name) = buildNamedAccessExpression {
        coneTypeOrNull = ConePrimitiveType.UNIT
        calleeReference = buildResolvedNamedReference {
            this.name = name
            resolvedSymbol = symbol
        }
    }

    /**
     * 构造 Int32 常量表达式。
     */
    private fun intLiteral(value: Int) = buildLiteralExpression {
        kind = CfirLiteralKind.INT
        this.value = value
        coneTypeOrNull = ConePrimitiveType.INT32
    }

    /**
     * 构造 Unit 字面量表达式。
     */
    private fun unitLiteral() = buildLiteralExpression {
        kind = CfirLiteralKind.UNIT
        value = Unit
        coneTypeOrNull = ConePrimitiveType.UNIT
    }

    /**
     * 构造 Int32 resolved type reference。
     */
    private fun resolvedIntType() = buildResolvedTypeRef {
        coneType = ConePrimitiveType.INT32
    }

    /**
     * 构造 Unit resolved type reference。
     */
    private fun resolvedUnitType() = buildResolvedTypeRef {
        coneType = ConePrimitiveType.UNIT
    }

    /**
     * 构造 tuple resolved type reference。
     */
    private fun resolvedTupleType(type: ConeTupleType) = buildResolvedTypeRef {
        coneType = type
    }

    private fun patternBinding(name: String, typeRef: CfirTypeRef): CfirPatternBindingVariable =
        buildPatternBindingVariable {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            status = CfirDeclarationStatusImpl()
            symbol = CfirPatternBindingSymbol(CallableId(PACKAGE_NAME, Name.identifier(name)))
            this.name = Name.identifier(name)
            isVar = false
            returnTypeRef = typeRef
        }

    private fun bindingPattern(variable: CfirPatternBindingVariable) = buildBindingPattern {
        name = variable.name
        bindingVariable = variable
    }

    private fun patternVariable(
        name: String,
        pattern: org.cangnova.cangjie.cfir.patterns.CfirPattern,
        initializer: CfirExpression,
        typeRef: CfirTypeRef,
    ): CfirPatternVariable = buildPatternVariable {
        moduleData = MODULE_DATA
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true
        status = CfirDeclarationStatusImpl()
        symbol = CfirPatternVariableSymbol(CallableId(PACKAGE_NAME, Name.identifier(name)))
        isVar = false
        returnTypeRef = typeRef
        this.initializer = initializer
        this.pattern = pattern
    }

    private fun assertTuplePatternRejected(
        pattern: org.cangnova.cangjie.cfir.patterns.CfirPattern,
        initializer: CfirExpression,
        typeRef: CfirTypeRef,
        sourceName: String,
    ) {
        val patternVariable = patternVariable(sourceName, pattern, initializer, typeRef)
        val functionName = Name.identifier("reject_$sourceName")
        val function = buildNamedFunction {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            symbol = CfirNamedFunctionSymbol(CallableId(PACKAGE_NAME, functionName))
            name = functionName
            returnTypeRef = resolvedUnitType()
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.UNIT
                statements += patternVariable
            }
        }
        val file = buildFile {
            moduleData = MODULE_DATA
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "$sourceName.cj"
            packageDirective = buildPackageDirective {
                packageFqName = PACKAGE_NAME
                isMacroPackage = false
            }
            declarations += function
        }

        assertThrows(Cfir2ChirConversionException::class.java) {
            DefaultCfir2ChirConverter().convert(listOf(file))
        }
    }

    /**
     * 填充测试变量的必要 CFIR 声明元数据。
     */
    private fun org.cangnova.cangjie.cfir.declarations.builder.CfirFieldVariableBuilder.commonVariableDeclaration(
        symbol: CfirFieldVariableSymbol,
    ) {
        moduleData = MODULE_DATA
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Synthetic.Default
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = true
        dispatchReceiverType = null
        status = CfirDeclarationStatusImpl()
        this.symbol = symbol
    }

    /**
     * 为测试文件设置仓颉 session 与模块数据。
     */
    private object TEST_SESSION : CfirSession(CfirSession.Kind.Source)

    private object UNUSED_SCOPE_PROVIDER : CfirScopeProvider() {
        override fun getUseSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
            memberRequiredPhase: CfirResolvePhase?,
        ): CfirTypeScope = error("codegen integration fixture does not resolve scopes")

        override fun getTypealiasConstructorScope(
            typeAlias: CfirTypeAlias,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirScope = error("codegen integration fixture does not resolve scopes")

        override fun getDeclarationSiteMemberScope(
            klass: CfirClass,
            useSiteSession: CfirSession,
            scopeSession: ScopeSession,
        ): CfirTypeScope = error("codegen integration fixture does not resolve scopes")
    }

    private object MODULE_DATA : CfirModuleData() {
        override val name: Name = Name.identifier("codegen-local-storage")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = targetPlatform.isCommon()
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        override val stableModuleName: String = "codegen-local-storage"
        override val session: CfirSession
            get() = TEST_SESSION

        init {
            bindSession(TEST_SESSION)
        }
    }

    private companion object {
        val PACKAGE_NAME = FqName("codegen.localstorage")
    }
}
