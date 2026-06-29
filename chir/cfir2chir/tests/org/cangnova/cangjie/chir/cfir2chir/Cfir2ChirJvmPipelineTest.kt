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

/**
 * 覆盖 CFIR -> CHIR -> JVM 的最小端到端管线。
 */
class Cfir2ChirJvmPipelineTest {
    /**
     * 验证解析后的 CFIR 函数可以降级并生成为可执行 JVM 静态方法。
     */
    @Test
    fun `lowers resolved CFIR function to executable JVM method`() {
        val function = functionReturningInt("answer", 42)
        val file = fileWith(function)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
        val generatedClass = GeneratedClassLoader().define(output.classes.single())

        assertEquals(42, generatedClass.getMethod("answer").invoke(null))
    }

    /**
     * 验证 cfir2chir 会在 body lowering 前预注册所有函数 header，从而支持前向调用。
     */
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

    /**
     * 验证 CFIR 局部变量赋值会通过 CHIR memory 表达式降级为 JVM local。
     */
    @Test
    fun `lowers CFIR local variable assignment through CHIR memory to JVM locals`() {
        val function = functionWithLocalVariableAssignment()
        val file = fileWith(function)

        val chirPackage = DefaultCfir2ChirConverter().convert(listOf(file))
        val output = DefaultChirToJvmCodeGenerator().generate(ChirJvmCodegenInput(chirPackage))
        val generatedClass = GeneratedClassLoader().define(output.classes.single())

        assertEquals(2, generatedClass.getMethod("localAssignment").invoke(null))
    }

    /**
     * 构造返回指定 Int32 常量的测试函数。
     */
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

    /**
     * 构造包含局部变量初始化、赋值和读取的测试函数。
     */
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

    /**
     * 构造调用另一个测试函数的命名函数。
     */
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

    /**
     * 构造包含指定函数声明的测试 CFIR 文件。
     */
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

    /**
     * 构造带 Int32 初始化值的可变局部变量。
     */
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

    /**
     * 构造指向指定局部变量 symbol 的命名访问表达式。
     */
    private fun namedAccess(variable: CfirFieldVariable) = buildNamedAccessExpression {
        coneTypeOrNull = ConePrimitiveType.INT32
        calleeReference = buildResolvedNamedReference {
            name = variable.name
            resolvedSymbol = variable.symbol
        }
    }

    /**
     * 构造 Int32 字面量表达式。
     */
    private fun intLiteral(value: Int) = buildLiteralExpression {
        kind = CfirLiteralKind.INT
        this.value = value
        coneTypeOrNull = ConePrimitiveType.INT32
    }

    /**
     * 构造 Int32 resolved type ref。
     */
    private fun intTypeRef() = buildResolvedTypeRef {
        coneType = ConePrimitiveType.INT32
    }

    /**
     * JVM 管线测试使用的最小 source session。
     */
    private object TestSession : CfirSession(Kind.Source)

    /**
     * JVM 管线测试使用的最小 module data。
     */
    private object TestModuleData : CfirModuleData() {
        /**
         * 测试模块名称。
         */
        override val name: Name = Name.identifier("cfir2chir-test")
        /**
         * 测试模块没有普通依赖。
         */
        override val dependencies: List<CfirModuleData> = emptyList()
        /**
         * 测试模块没有 refinement 依赖。
         */
        override val refinementDependencies: List<CfirModuleData> = emptyList()
        /**
         * 测试模块没有传递 refinement 依赖。
         */
        override val allRefinementDependencies: List<CfirModuleData> = emptyList()
        /**
         * 测试模块使用默认仓颉平台。
         */
        override val targetPlatform = CangJiePlatforms.defaultCangJiePlatform
        /**
         * 测试模块使用默认 CFIR 平台。
         */
        override val platform: CfirPlatform = CfirPlatform.DEFAULT
        /**
         * 测试模块是否为 common 模块。
         */
        override val isCommon: Boolean = targetPlatform.isCommon()
        /**
         * 测试模块不声明额外 capability。
         */
        override val capabilities: CfirModuleCapabilities = CfirModuleCapabilities.Empty
        /**
         * 测试模块稳定名称。
         */
        override val stableModuleName: String = "cfir2chir-test"
        /**
         * 测试模块绑定的 CFIR session。
         */
        override val session: CfirSession
            get() = TestSession

        init {
            bindSession(TestSession)
        }
    }

    /**
     * 将生成的 JVM class bytes 直接定义到当前测试 class loader。
     */
    private class GeneratedClassLoader : ClassLoader(Cfir2ChirJvmPipelineTest::class.java.classLoader) {
        /**
         * 定义单个 JVM class file artifact 并返回加载后的 Class。
         */
        fun define(artifact: JvmClassFileArtifact): Class<*> {
            return defineClass(artifact.internalName.replace('/', '.'), artifact.bytes, 0, artifact.bytes.size)
        }
    }

    private companion object {
        val PackageName = FqName("sample")
    }
}
