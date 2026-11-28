package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildFile
import org.cangnova.cangjie.cfir.declarations.builder.buildPackageDirective
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataRegistry
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CfirAnnotationMetadataRegistrySessionComponentTest {
    @Test
    fun parallelBuildersShareOneSessionRegistry() {
        val session = TestSession()
        val barrier = CyclicBarrier(8)
        val executor = Executors.newFixedThreadPool(8)
        try {
            val builders = List(8) {
                executor.submit<CfirAnnotationMetadataRegistry> {
                    barrier.await(10, TimeUnit.SECONDS)
                    session.ensureAnnotationMetadataRegistry()
                }
            }
            val registered = builders.first().get(10, TimeUnit.SECONDS)
            builders.forEach { assertSame(registered, it.get(10, TimeUnit.SECONDS)) }
            assertSame(registered, session.annotationMetadataRegistry)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun updatedSourceUsesAnIndependentWriterWithTheSameRegistry() {
        val session = TestSession()
        val registry = session.ensureAnnotationMetadataRegistry()
        val firstFile = session.buildSourceFile()
        val firstWriter = CfirAnnotationMetadataBuilder()
        val firstProduct = firstWriter.seal(listOf(firstFile))
        assertFalse(firstProduct.published)
        registry.publish(firstProduct)

        val updatedFile = session.buildSourceFile()
        val updatedWriter = CfirAnnotationMetadataBuilder()
        assertNotSame(firstFile, updatedFile)
        assertNotSame(firstWriter, updatedWriter)
        assertThrows(IllegalStateException::class.java) { firstWriter.seal(listOf(firstFile)) }
        val updatedProduct = updatedWriter.seal(listOf(updatedFile))
        assertNotSame(firstProduct, updatedProduct)
        assertFalse(updatedProduct.published)
        registry.publish(updatedProduct)

        assertTrue(firstProduct.published)
        assertTrue(updatedProduct.published)
        assertSame(registry, session.annotationMetadataRegistry)
        assertSame(registry, session.ensureAnnotationMetadataRegistry())
    }

    private class TestSession : CfirSession(CfirSession.Kind.Source) {
        private val module = CfirSourceModuleData(
            Name.identifier("annotation-metadata-session"), emptyList(), emptyList(),
            CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT,
        ).also { it.bindSession(this) }

        fun buildSourceFile(): CfirFile = buildFile {
            name = "annotation.cj"
            source = object : CjBinarySourceElement(name, null, name) {}
            moduleData = module
            resolvePhase = CfirResolvePhase.RAW_CFIR
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes.EMPTY
            symbol = CfirFileSymbol()
            packageDirective = buildPackageDirective {
                packageFqName = FqName.ROOT
                isMacroPackage = false
            }
        }
    }
}
