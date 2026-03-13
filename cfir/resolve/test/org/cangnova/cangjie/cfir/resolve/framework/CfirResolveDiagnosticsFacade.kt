package org.cangjie.cfir.resolve.framework

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.common.moduleData
import org.cangjie.cfir.declarations.CfirClass
import org.cangjie.cfir.declarations.CfirClassKind
import org.cangjie.cfir.declarations.CfirDeclaration
import org.cangjie.cfir.declarations.CfirExtend
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangjie.cfir.declarations.CfirPackageDirective
import org.cangjie.cfir.resolve.CfirTotalResolveProcessor
import org.cangjie.cfir.session.phaseResolverRegistry
import org.cangjie.cfir.types.CfirBasicTypeRef
import org.cangjie.cfir.types.CfirUserTypeRef
import org.cangjie.test.config.TestFacade
import org.cangjie.test.model.TestModule
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

class CfirResolveDiagnosticsFacade : TestFacade {
    override fun transform(module: TestModule, inputArtifact: Any?): Any {
        val source = module.files.firstOrNull() ?: error("Expected at least one source file")
        val context = createCfirResolveTestSessionContext(module.name)
        val session = context.session

        val declarations = buildSyntheticDeclarations(session.moduleData, source.content)
        val file = CfirFile(
            moduleData = session.moduleData,
            name = source.name,
            packageDirective = CfirPackageDirective(FqName.ROOT),
            declarations = declarations,
        )

        CfirTotalResolveProcessor(session, session.phaseResolverRegistry).processFile(file)
        val declarationPhase = file.declarations.firstOrNull()?.resolvePhase?.name ?: "NONE"
        return ResolveDiagnosticsArtifact(
            fileName = file.name,
            declarationPhase = declarationPhase,
            diagnostics = context.diagnostics.diagnostics,
        )
    }

    private fun buildSyntheticDeclarations(
        moduleData: CfirModuleData,
        sourceContent: String,
    ): MutableList<CfirDeclaration> {
        val declarations = mutableListOf<CfirDeclaration>()

        if (sourceContent.contains(INVALID_DECLARATION_MARKER)) {
            declarations += CfirInvalidDeclaration(
                moduleData = moduleData,
                reason = "invalid declaration marker found",
            )
        }

        if (sourceContent.contains(SUPER_DUPLICATE_MARKER)) {
            declarations += CfirClass(
                moduleData = moduleData,
                name = Name.identifier("TypeWithDuplicateSupers"),
                classKind = CfirClassKind.CLASS,
                superTypeRefs = mutableListOf(userTypeRef("IPrintable"), userTypeRef("IPrintable")),
            )
        }

        if (sourceContent.contains(SUPER_SELF_MARKER)) {
            declarations += CfirClass(
                moduleData = moduleData,
                name = Name.identifier("SelfType"),
                classKind = CfirClassKind.CLASS,
                superTypeRefs = mutableListOf(userTypeRef("SelfType")),
            )
        }

        if (sourceContent.contains(EXTEND_DUPLICATE_MARKER)) {
            declarations += CfirExtend(
                moduleData = moduleData,
                extendedTypeRef = userTypeRef("Vec"),
                superTypeRefs = mutableListOf(userTypeRef("IIterable"), userTypeRef("IIterable")),
            )
        }

        if (sourceContent.contains(EXTEND_NOT_INTERFACE_MARKER)) {
            declarations += CfirExtend(
                moduleData = moduleData,
                extendedTypeRef = userTypeRef("Vec"),
                superTypeRefs = mutableListOf(CfirBasicTypeRef(name = Name.identifier("Int64"))),
            )
        }

        if (declarations.isEmpty()) {
            declarations += CfirInvalidDeclaration(
                moduleData = moduleData,
                reason = "<synthetic> no-op declaration for resolve pipeline",
            )
        }

        return declarations
    }

    private fun userTypeRef(name: String): CfirUserTypeRef =
        CfirUserTypeRef(qualifier = listOf(Name.identifier(name)))

    private companion object {
        private const val INVALID_DECLARATION_MARKER = "// INVALID_DECLARATION"
        private const val SUPER_DUPLICATE_MARKER = "// SUPER_DUPLICATE"
        private const val SUPER_SELF_MARKER = "// SUPER_SELF"
        private const val EXTEND_DUPLICATE_MARKER = "// EXTEND_DUPLICATE"
        private const val EXTEND_NOT_INTERFACE_MARKER = "// EXTEND_NOT_INTERFACE"
    }
}
