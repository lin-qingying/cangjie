package org.cangnova.cangjie.analysis.decompiled.psi.text

import PackageFormat.Package as CjoPackage
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.analysis.decompiler.stub.CjoDeclarationLoader
import org.cangnova.cangjie.analysis.decompiler.stub.LoadedCjoPackage
import org.cangnova.cangjie.cfir.analysis.tests.AbstractCfirAnalysisResolveTest
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.serialization.cjo.CfirCjoPackageMetadataProducer
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/**
 * `ConeClassLikeType.isThisType` 必须经 `CfirCjoPackageMetadataProducer` 写入 `.cjo` 并能读回。
 *
 * 官方 `CompositeTyInfo.isThisTy` 用来区分类体内的 `This` 与普通 class-like 类型；
 * 写出侧曾从不写 `true`，`.cjo` 往返会把它静默退化成普通类型。
 *
 * 测试直接构造合成的 `This` cone 类型（源码侧 `This` 是 light-tree 节点，PSI 源码没有该 token），
 * 但**必须经过 producer**：只测 `CjoPackageWriter` + metadata 无法覆盖 producer 的 pass-through。
 */
class ThisTypeCjoRoundTripTest : AbstractCfirAnalysisResolveTest() {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun thisTypeIsWrittenByProducerAndReadBack() {
        val source = """
            package thistype
            public class Widget {
                public func thisView(): Widget {
                    return this
                }

                public func plainView(): Widget {
                    return this
                }
            }
        """.trimIndent()
        val file = createCjFile("thisRoundTrip", source)
        val cfirFile = resolveSource(file)

        val widget = cfirFile.declarations.filterIsInstance<CfirClass>().single()
        val lookupTag = widget.symbol.toLookupTag()
        val functions = widget.declarations.filterIsInstance<CfirNamedFunction>()
        val thisView = functions.single { function -> function.name.asString() == "thisView" }
        val plainView = functions.single { function -> function.name.asString() == "plainView" }

        // 合成的 This / 普通 class-like 返回类型：只替换返回类型，声明本身仍是源码声明。
        thisView.replaceReturnTypeRef(
            buildResolvedTypeRef { coneType = ConeClassLikeType(lookupTag, isThisType = true) },
        )
        plainView.replaceReturnTypeRef(
            buildResolvedTypeRef { coneType = ConeClassLikeType(lookupTag) },
        )

        val loaded = loadMetadata(CfirCjoPackageMetadataProducer.produce(listOf(cfirFile)))
        val decoded = CjoDeclarationLoader.loadDeclarations(loaded, cfirFile.moduleData)
        val decodedWidget = decoded.filterIsInstance<CfirClass>().single()
        val decodedFunctions = decodedWidget.declarations.filterIsInstance<CfirNamedFunction>()

        val decodedThisView = decodedFunctions.single { function -> function.name.asString() == "thisView" }
        val decodedPlainView = decodedFunctions.single { function -> function.name.asString() == "plainView" }

        val thisType = decodedThisView.returnTypeConeType()
        val plainType = decodedPlainView.returnTypeConeType()

        assertInstanceOf(ConeClassLikeType::class.java, thisType, "This 返回类型必须恢复为 class-like 类型")
        assertTrue(
            (thisType as ConeClassLikeType).isThisType,
            "producer 必须把 isThisType 写进 CompositeTyInfo.isThisTy 并读回",
        )
        assertInstanceOf(ConeClassLikeType::class.java, plainType)
        assertFalse((plainType as ConeClassLikeType).isThisType, "普通 class-like 类型不得被写成 This")
    }

    private fun CfirNamedFunction.returnTypeConeType(): ConeCangJieType =
        (returnTypeRef as CfirResolvedTypeRef).coneType

    private fun resolveSource(file: CjFile): CfirFile {
        val session = createTestSession()
        val cfirFile = file.toCfirFile(session)
        resolveToPhase(cfirFile, session, targetPhase)
        return cfirFile
    }

    private fun loadMetadata(metadata: CjoPackageMetadata): LoadedCjoPackage {
        val bytes = CjoPackageWriter.toByteArray(metadata)
        val pkg = CjoPackage.getRootAsPackage(ByteBuffer.wrap(bytes))
        return LoadedCjoPackage(
            binaryFile = LightVirtualFile("thistype.cjo"),
            packageFqName = FqName("thistype"),
            pkg = pkg,
            header = CjoPackageHeader.fromPackage(pkg),
            cjoManager = CjoManager(CjoSearchPath { null }),
            isVersionSupported = true,
        )
    }
}
