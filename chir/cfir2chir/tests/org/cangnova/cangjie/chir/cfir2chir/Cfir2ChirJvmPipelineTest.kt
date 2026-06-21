package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirModuleCapabilities
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildAssignment
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildBlock
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.jvm.codegen.api.ChirJvmCodegenInput
import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
import org.cangnova.cangjie.jvm.codegen.pipeline.DefaultChirToJvmCodeGenerator
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.platform.isCommon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Cfir2ChirJvmPipelineTest {
    @Test
    fun `lowers resolved CFIR function to executable JVM method`() {
        val function = functionReturningInt("answer", 42)
        val file = fileWith(function)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
        val generatedClass = GeneratedClassLoader().define(output.classes.single())

        assertEquals(42, generatedClass.getMethod("answer").invoke(null))
    }

    @Test
    fun `registers all function headers before lowering forward calls`() {
        val callee = functionReturningInt("callee", 7)
        val caller = functionCalling("caller", callee)
        val file = fileWith(caller, callee)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
        val generatedClass = GeneratedClassLoader().define(output.classes.single())

        assertEquals(7, generatedClass.getMethod("caller").invoke(null))
    }

    @Test
    fun `lowers CFIR local variable assignment through CHIR memory to JVM locals`() {
        val function = functionWithLocalVariableAssignment()
        val file = fileWith(function)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
        val generatedClass = GeneratedClassLoader().define(output.classes.single())

        assertEquals(2, generatedClass.getMethod("localAssignment").invoke(null))
    }

    private fun functionReturningInt(name: String, value: Int): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier(name)))
        return buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = intTypeRef()
            this.symbol = symbol
            this.name = Name.identifier(name)
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += buildLiteralExpression {
                    kind = CfirLiteralKind.INT
                    this.value = value
                    coneTypeOrNull = ConePrimitiveType.INT32
                }
            }
        }
    }

    private fun functionWithLocalVariableAssignment(): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier("localAssignment")))
        val local = localIntVariable("x", 1)
        return buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = intTypeRef()
            this.symbol = symbol
            name = Name.identifier("localAssignment")
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += local
                statements += buildAssignment {
                    coneTypeOrNull = ConePrimitiveType.UNIT
                    lValue = namedAccess(local)
                    rValue = intLiteral(2)
                }
                statements += namedAccess(local)
            }
        }
    }

    private fun functionCalling(name: String, callee: CfirNamedFunction): CfirNamedFunction {
        val symbol = CfirNamedFunctionSymbol(CallableId(PackageName, Name.identifier(name)))
        return buildNamedFunction {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            returnTypeRef = intTypeRef()
            this.symbol = symbol
            this.name = Name.identifier(name)
            isMut = false
            body = buildBlock {
                coneTypeOrNull = ConePrimitiveType.INT32
                statements += buildFunctionCall {
                    coneTypeOrNull = ConePrimitiveType.INT32
                    calleeReference = buildResolvedNamedReference {
                        this.name = callee.name
                        resolvedSymbol = callee.symbol
                    }
                    argumentList = buildArgumentList()
                    origin = CfirFunctionCallOrigin.Regular
                }
            }
        }
    }

    private fun fileWith(vararg declarations: CfirNamedFunction): CfirFile {
        return buildFile {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            name = "sample.cj"
            packageDirective = buildPackageDirective {
                packageFqName = PackageName
                isMacroPackage = false
            }
            this.declarations += declarations
        }
    }

    private fun localIntVariable(name: String, initialValue: Int): CfirFieldVariable {
        val variableName = Name.identifier(name)
        return buildFieldVariable {
            moduleData = TestModuleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            dispatchReceiverType = null
            status = CfirDeclarationStatusImpl()
            initializer = intLiteral(initialValue)
            isVar = true
            symbol = CfirFieldVariableSymbol(CallableId(PackageName, variableName))
            returnTypeRef = intTypeRef()
            this.name = variableName
        }
    }

    private fun namedAccess(variable: CfirFieldVariable) = buildNamedAccessExpression {
        coneTypeOrNull = ConePrimitiveType.INT32
        calleeReference = buildResolvedNamedReference {
            name = variable.name
            resolvedSymbol = variable.symbol
        }
    }

    private fun intLiteral(value: Int) = buildLiteralExpression {
        kind = CfirLiteralKind.INT
        this.value = value
        coneTypeOrNull = ConePrimitiveType.INT32
    }

    private fun intTypeRef() = buildResolvedTypeRef {
        coneType = ConePrimitiveType.INT32
    }

    private object TestSession : CfirSession(Kind.Source)

    private object TestModuleData : CfirModuleData() {
        override val name: Name = Name.identifier("cfir2chir-test")
        override val dependencies: List<CfirModuleData> = emptyList()
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        override val isCommon: Boolean = targetPlatform.isCommon()
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        override val stableModuleName: String = "cfir2chir-test"
        override val session: CfirSession
            get() = TestSession

        init {
            bindSession(TestSession)
        }
    }

    private class GeneratedClassLoader : ClassLoader(Cfir2ChirJvmPipelineTest::class.java.classLoader) {
        fun define(artifact: JvmClassFileArtifact): Class<*> {
            return defineClass(artifact.internalName.replace('/', '.'), artifact.bytes, 0, artifact.bytes.size)
        }
    }

    private companion object {
        val PackageName = FqName("sample")
    }
}
