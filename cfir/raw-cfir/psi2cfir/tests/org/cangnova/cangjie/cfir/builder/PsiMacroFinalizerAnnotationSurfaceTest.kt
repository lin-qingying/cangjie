package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@RunWith(JUnit3RunnerWithInners::class)
class PsiMacroFinalizerAnnotationSurfaceTest : AbstractRawCfirBuilderTestCase() {
    fun testFinalizerAnnotationSurfaceOwnsFinalizerAnnotationSlot() {
        val file = createCjFile(
            "finalizerAnnotation",
            """
            @Annotation[target: [MemberFunction]]
            public class APILevel { public const init() {} }
            public class C {
                @APILevel
                ~init() {}
            }
            main() {}
            """.trimIndent(),
        )
        val session = createTestSession()
        val metadata = CfirAnnotationMetadataBuilder()
        val preFile = PsiRawCfirBuilder(
            session,
            BodyBuildingMode.NORMAL,
            metadata,
        ).buildPreMacroCfirFile(file)

        val surface = preFile.surfaces.single { it.qualifiedName?.shortName() == Name.identifier("APILevel") }
        assertNotNull(surface.replaceHandle.annotationCarrier, surface.toString())
        val finalizer = preFile.cfirFile.declarations
            .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirClass>()
            .single { it.name == Name.identifier("C") }
            .declarations.filterIsInstance<CfirFinalizer>().single()
        assertSame(finalizer, surface.replaceHandle.annotationCarrier?.owner)
    }
}
