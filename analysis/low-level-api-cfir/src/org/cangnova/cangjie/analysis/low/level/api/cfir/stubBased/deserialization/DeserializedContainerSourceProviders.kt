/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin

internal interface DeserializedContainerSourceProvider {
    fun getFacadeContainerSource(
        file: CjFile,
        stubOrigin: CangJieStubOrigin?,
        declarationOrigin: CfirDeclarationOrigin,
    ): DeserializedContainerSource?

    fun getClassContainerSource(classId: ClassId): DeserializedContainerSource?
}

// Currently, `null` is returned for KLIBs to avoid incorrect application of JVM file facade logic and overload filtering.
// We might want to provide non-`null` container source for all types of binaries in the future.
internal object NullDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getFacadeContainerSource(
        file: CjFile,
        stubOrigin: CangJieStubOrigin?,
        declarationOrigin: CfirDeclarationOrigin,
    ): DeserializedContainerSource? = null

    override fun getClassContainerSource(classId: ClassId): DeserializedContainerSource? = null
}

internal object StubDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getFacadeContainerSource(
        file: CjFile,
        stubOrigin: CangJieStubOrigin?,
        declarationOrigin: CfirDeclarationOrigin,
    ): DeserializedContainerSource {
        val stubKind = file.stub?.kind
        val binaryFilePath = file.virtualFile?.path

        return when (stubKind) {
            is CangJieFileStubKind.WithPackage.Facade.MultifileClass ->
                PackageFacadeDeserializedContainerSource(
                    packageFqName = stubKind.packageFqName,
                    facadeFqName = stubKind.facadeFqName,
                    partSimpleName = recoverPartSimpleName(file, stubOrigin),
                    partSimpleNames = stubKind.facadePartSimpleNames,
                    isMultifile = true,
                    binaryFilePath = binaryFilePath,
                )

            is CangJieFileStubKind.WithPackage.Facade.Simple ->
                PackageFacadeDeserializedContainerSource(
                    packageFqName = stubKind.packageFqName,
                    facadeFqName = stubKind.facadeFqName,
                    partSimpleName = stubKind.partSimpleName,
                    partSimpleNames = listOf(stubKind.partSimpleName),
                    isMultifile = false,
                    binaryFilePath = binaryFilePath,
                )

            else ->
                PackageFacadeDeserializedContainerSource(
                    packageFqName = file.packageFqName,
                    facadeFqName = file.packageFqName,
                    partSimpleName = recoverPartSimpleName(file, stubOrigin),
                    partSimpleNames = listOf(recoverPartSimpleName(file, stubOrigin)),
                    isMultifile = false,
                    binaryFilePath = binaryFilePath,
                )
        }
    }

    override fun getClassContainerSource(classId: ClassId): DeserializedContainerSource? =
        ClassDeserializedContainerSource(classId)
}

internal object BuiltinsDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getFacadeContainerSource(
        file: CjFile,
        stubOrigin: CangJieStubOrigin?,
        declarationOrigin: CfirDeclarationOrigin,
    ): DeserializedContainerSource {
        require(stubOrigin is CangJieStubOrigin.Facade) {
            "Expected builtins file to have Facade origin, got origin=$stubOrigin instead"
        }

        val stubKind = file.stub?.kind as? CangJieFileStubKind.WithPackage.Facade
        return BuiltinsDeserializedContainerSource(
            packageFqName = file.packageFqName,
            facadeFqName = stubKind?.facadeFqName ?: file.packageFqName,
            binaryFilePath = file.virtualFile?.path,
        )
    }

    override fun getClassContainerSource(classId: ClassId): DeserializedContainerSource? =
        ClassDeserializedContainerSource(classId)
}

internal object StubAndBuiltinsDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getFacadeContainerSource(
        file: CjFile,
        stubOrigin: CangJieStubOrigin?,
        declarationOrigin: CfirDeclarationOrigin
    ): DeserializedContainerSource? {
        if (declarationOrigin == CfirDeclarationOrigin.Library && file.virtualFile?.extension == STUB_BUILTINS_FILE_EXTENSION) {
            return BuiltinsDeserializedContainerSourceProvider.getFacadeContainerSource(file, stubOrigin, declarationOrigin)
        }

        return StubDeserializedContainerSourceProvider.getFacadeContainerSource(file, stubOrigin, declarationOrigin)
    }

    override fun getClassContainerSource(classId: ClassId): DeserializedContainerSource? =
        ClassDeserializedContainerSource(classId)
}

internal const val STUB_BUILTINS_FILE_EXTENSION: String = "kotlin_builtins"

private fun recoverPartSimpleName(file: CjFile, stubOrigin: CangJieStubOrigin?): String {
    val rawName = when (stubOrigin) {
        is CangJieStubOrigin.MultiFileFacade -> stubOrigin.className
        is CangJieStubOrigin.Facade -> stubOrigin.className
        null -> file.virtualFile?.nameWithoutExtension ?: file.name
    }

    return rawName
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .substringBeforeLast('.')
        .ifBlank { file.name.substringBeforeLast('.') }
        .sanitizeFacadePartSimpleName()
}

private fun String.sanitizeFacadePartSimpleName(): String {
    return replace(Regex("[^A-Za-z0-9_]"), "_")
        .trim('_')
        .ifBlank { "FacadePart" }
}
