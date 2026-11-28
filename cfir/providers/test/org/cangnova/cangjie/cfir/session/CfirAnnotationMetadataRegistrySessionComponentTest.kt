package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataRegistry
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CfirAnnotationMetadataRegistrySessionComponentTest {
    @Test
    fun parallelBuildersShareOneRegistryGeneration() {
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
    fun newGenerationReplacesFrozenRegistryForUpdatedSourceInput() {
        val session = TestSession()
        val firstGeneration = session.beginAnnotationMetadataRegistryGeneration()
        firstGeneration.freeze()

        val secondGeneration = session.beginAnnotationMetadataRegistryGeneration()

        assertNotSame(firstGeneration, secondGeneration)
        assertTrue(firstGeneration.isFrozen)
        assertFalse(secondGeneration.isFrozen)
        assertSame(secondGeneration, session.annotationMetadataRegistry)
        assertSame(secondGeneration, session.ensureAnnotationMetadataRegistry())
    }

    private class TestSession : CfirSession(CfirSession.Kind.Source)
}
