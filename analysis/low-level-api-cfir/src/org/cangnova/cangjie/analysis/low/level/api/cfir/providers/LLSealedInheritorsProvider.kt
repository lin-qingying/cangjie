/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.declarations.KotlinDirectInheritorsProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.baseContextModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSessionCache
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirRegularClass
import org.cangnova.cangjie.cfir.declarations.SealedClassInheritorsProvider
import org.cangnova.cangjie.cfir.declarations.SealedClassInheritorsProviderInternals
import org.cangnova.cangjie.cfir.declarations.sealedInheritorsAttr
import org.cangnova.cangjie.cfir.declarations.utils.classId
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjClass
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.filter
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.plus
import kotlin.collections.sortedBy
import kotlin.let

/**
 * [LLSealedInheritorsProvider] is the LL CFIR implementation of [SealedClassInheritorsProvider] for both the IDE and Standalone mode.
 */
@OptIn(SealedClassInheritorsProviderInternals::class)
internal class LLSealedInheritorsProvider(private val project: Project) : SealedClassInheritorsProvider() {
    val cache = ConcurrentHashMap<ClassId, List<ClassId>>()

    override fun getSealedClassInheritors(firClass: CfirRegularClass): List<ClassId> {
        // Classes from binary libraries which are deserialized from class files (but not stubs) will have their `sealedInheritorsAttr` set
        // from metadata.
        firClass.sealedInheritorsAttr?.let { return it.value }

        val classId = firClass.classId

        // Local classes cannot be sealed.
        if (firClass.isLocal) {
            return emptyList()
        }

        return cache.computeIfAbsent(classId) { searchInheritors(firClass) }
    }

    /**
     * Some notes about the search:
     *
     *  - A Java class cannot legally extend a sealed Kotlin class (even in the same package), so we don't need to search for Java class
     *    inheritors.
     *  - Technically, we could use a package scope to narrow the search, but the search is already sufficiently narrow because it uses
     *    supertype indices and is confined to the current `CaModule`. Finding a `PsiPackage`
     *    for a `PackageScope` is not cheap, hence the decision to avoid it. If a `PackageScope` is needed in the future, it'd be best to
     *    extract a `PackageNameScope` which operates just with the qualified package name, to avoid `PsiPackage`. (At the time of writing,
     *    this is possible with the implementation of `PackageScope`.)
     *  - We ignore local classes to avoid lazy resolve contract violations.
     *    See KT-63795.
     */
    private fun searchInheritors(firClass: CfirClass): List<ClassId> {
        val (targetModule, targetCfirClass) = when (val classModule = firClass.llCfirModuleData.ktModule) {
            is CaDanglingFileModule -> {
                // Since we are searching for inheritors in the context module's scope, we need to search for inheritors of the *original*
                // CFIR class, not the dangling CFIR class.
                val contextModule = classModule.baseContextModule
                val contextSession = LLCfirSessionCache.getInstance(project).getSession(contextModule, preferBinary = true)
                val originalCfirSymbol = contextSession.symbolProvider.getClassLikeSymbolByClassId(firClass.classId)
                val originalCfirClass = originalCfirSymbol?.fir as? CfirClass ?: return emptyList()
                contextModule to originalCfirClass
            }
            else -> classModule to firClass
        }
        val targetCjClass = targetCfirClass.psi as? CjClass ?: return emptyList()

        val scope = targetModule.contentScope

        return searchInScope(targetCjClass, targetCfirClass.classId, scope)
    }

    private fun searchInScope(ktClass: CjClass, classId: ClassId, scope: GlobalSearchScope): List<ClassId> =
        KotlinDirectInheritorsProvider.getInstance(project)
            .getDirectKotlinInheritors(ktClass, scope, includeLocalInheritors = false)
            .mapNotNull { it.getClassId() }
            .filter { it.packageFqName == classId.packageFqName }
            // Enforce a deterministic order on the result, e.g. for stable test output.
            .sortedBy { it.toString() }
            .ifEmpty { emptyList() }
}
