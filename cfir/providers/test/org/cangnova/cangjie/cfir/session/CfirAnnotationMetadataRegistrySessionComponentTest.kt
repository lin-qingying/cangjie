package org.cangnova.cangjie.cfir.session

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CfirAnnotationMetadataRegistrySessionComponentTest {
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
