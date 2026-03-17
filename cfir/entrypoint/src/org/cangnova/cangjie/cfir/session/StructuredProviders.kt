package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider

/**
 * @property sourceProviders contains providers of declarations from exactly this session, which includes the following:
 * - provider of real source declarations
 * - provider of plugin-generated declarations for this specific module
 * - provider of precompiled classes from this specific module came from incremental compilation
 *
 * @property dependencyProviders contains providers of declarations from dependencies of this session, which includes the following:
 * - binary dependencies, such as .class files, .jar files, .klib files
 * - source providers of dependency modules
 *
 * @property sharedProvider contains a provider, which is shared between sessions. This provider includes fallback builtins,
 * synthetic functional interfaces provider and similar stuff.
 * The important note here is that [sharedProvider] differs for source session in case of single-pass and split compilation schemes:
 * - in the regular scheme there is a single shared provider, which is shared between all source and library sessions
 * - in the split scheme shared provider is shared only between library sessions. Source sessions don't have one, and each such
 *   session uses its own list of providers, which usually belong to the shared provider.
 */
class StructuredProviders(
    val sourceProviders: List<CfirSymbolProvider>,
    val dependencyProviders: List<CfirSymbolProvider>,
    val sharedProvider: CfirSymbolProvider
) : CfirSessionComponent

val CfirSession.structuredProviders: StructuredProviders by CfirSession.sessionComponentAccessor()
