package org.cangnova.cangjie.cfir.pipeline

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.entrypoint.session.CfirSessionConfigurator
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.allowAnyScriptsInSourceRoots
import org.cangnova.cangjie.config.dontSortSourceFiles
import org.cangnova.cangjie.name.Name

data class SessionWithSources<F>(
    val session: CfirSession,
    val files: List<F>,
)

/**
 * Platform-independent session construction utility.
 *
 * This mirrors Kotlin's SessionConstructionUtils structure for the single-module
 * flow used in this repository:
 * 1) create shared library session
 * 2) create regular library session (depends on shared session)
 * 3) create source session (depends on library session)
 */
object CfirSessionConstructionUtils {
    fun <F> prepareSessions(
        files: List<F>,
        configuration: CompilerConfiguration,
        rootModuleName: Name,
        createSharedLibrarySession: () -> CfirSession,
        createLibrarySession: (sharedLibrarySession: CfirSession) -> CfirSession,
        createSourceSession: CfirSessionProducer<F>,
        isScript: (F) -> Boolean = { false },
    ): List<SessionWithSources<F>> {
        val orderedFiles = if (configuration.dontSortSourceFiles) files else files.sortedBy { it.toString() }
        val (scriptFiles, nonScriptFiles) = if (configuration.allowAnyScriptsInSourceRoots) {
            orderedFiles.partition(isScript)
        } else {
            emptyList<F>() to orderedFiles
        }
        val sessionConfigurator: CfirSessionConfigurator.() -> Unit = {}

        val sharedSession = createSharedLibrarySession()
        val regularLibrarySession = createLibrarySession(sharedSession)

        val nonScriptSession = createSingleSession(
            files = nonScriptFiles,
            rootModuleName = rootModuleName,
            dependencies = listOf(regularLibrarySession.moduleData),
            sessionConfigurator = sessionConfigurator,
            sourceSessionProducer = createSourceSession,
        )

        if (scriptFiles.isEmpty()) return listOf(nonScriptSession)

        val scriptsSession = createSingleSession(
            files = scriptFiles,
            rootModuleName = Name.identifier("${rootModuleName.asString()}-scripts"),
            dependencies = listOf(regularLibrarySession.moduleData, nonScriptSession.session.moduleData),
            sessionConfigurator = sessionConfigurator,
            sourceSessionProducer = createSourceSession,
        )

        return listOf(
            nonScriptSession,
            scriptsSession,
        )
    }

    private fun <F> createSingleSession(
        files: List<F>,
        rootModuleName: Name,
        dependencies: List<CfirModuleData>,
        sessionConfigurator: CfirSessionConfigurator.() -> Unit,
        sourceSessionProducer: CfirSessionProducer<F>,
    ): SessionWithSources<F> {
        val sourceModuleData = CfirSourceModuleData(
            name = rootModuleName,
            dependencies = dependencies,
            refinementDependencies = emptyList(),
            platform = CfirPlatform.DEFAULT,
        )

        val sourceSession = sourceSessionProducer.createSession(
            files = files,
            moduleData = sourceModuleData,
            isForLeafHmppModule = false,
            sessionConfigurator = sessionConfigurator,
        )

        return SessionWithSources(sourceSession, files)
    }
}

fun interface CfirSessionProducer<F> {
    /**
     * @param isForLeafHmppModule reserved for HMPP parity; always false in this repository.
     */
    fun createSession(
        files: List<F>,
        moduleData: CfirModuleData,
        isForLeafHmppModule: Boolean,
        sessionConfigurator: CfirSessionConfigurator.() -> Unit,
    ): CfirSession
}
