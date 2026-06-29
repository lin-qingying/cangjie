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

/**
 * 为 stub-based 反序列化声明提供 container source 的策略接口。
 */
internal interface DeserializedContainerSourceProvider {
    /**
     * 返回文件 facade 级声明的 container source。
     */
    fun getFacadeContainerSource(
        file: CjFile,
        stubOrigin: CangJieStubOrigin?,
        declarationOrigin: CfirDeclarationOrigin,
    ): DeserializedContainerSource?

    /**
     * 返回 class-like 声明的 container source。
     */
    fun getClassContainerSource(classId: ClassId): DeserializedContainerSource?
}

// Currently, `null` is returned for KLIBs to avoid incorrect application of JVM file facade logic and overload filtering.
// We might want to provide non-`null` container source for all types of binaries in the future.
/**
 * 不提供任何 container source 的策略。
 */
internal object NullDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    /**
     * facade 声明不绑定 container source。
     */
    override fun getFacadeContainerSource(
        file: CjFile,
        stubOrigin: CangJieStubOrigin?,
        declarationOrigin: CfirDeclarationOrigin,
    ): DeserializedContainerSource? = null

    /**
     * class 声明不绑定 container source。
     */
    override fun getClassContainerSource(classId: ClassId): DeserializedContainerSource? = null
}

/**
 * 基于 compiled stub 信息生成 container source 的策略。
 */
internal object StubDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    /**
     * 根据文件 stub kind 构造 package facade 或 multifile facade container source。
     */
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

    /**
     * class 声明使用 class id 作为 container source。
     */
    override fun getClassContainerSource(classId: ClassId): DeserializedContainerSource? =
        ClassDeserializedContainerSource(classId)
}

/**
 * builtins 文件专用 container source provider。
 */
internal object BuiltinsDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    /**
     * 构造 builtins package facade container source。
     */
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

    /**
     * builtins class 声明使用 class id 作为 container source。
     */
    override fun getClassContainerSource(classId: ClassId): DeserializedContainerSource? =
        ClassDeserializedContainerSource(classId)
}

/**
 * 同时支持普通 stub 文件和 builtins stub 文件的 container source provider。
 */
internal object StubAndBuiltinsDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    /**
     * 根据文件扩展名在 builtins 与普通 stub provider 间分派 facade container source。
     */
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

    /**
     * class 声明统一使用 class id container source。
     */
    override fun getClassContainerSource(classId: ClassId): DeserializedContainerSource? =
        ClassDeserializedContainerSource(classId)
}

/**
 * stub builtins 文件扩展名。
 */
internal const val STUB_BUILTINS_FILE_EXTENSION: String = "kotlin_builtins"

/**
 * 从 stub origin 或文件名恢复 facade part 简名。
 */
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

/**
 * 将 facade part 名称规整为只包含字母、数字和下划线的稳定名称。
 */
private fun String.sanitizeFacadePartSimpleName(): String {
    return replace(Regex("[^A-Za-z0-9_]"), "_")
        .trim('_')
        .ifBlank { "FacadePart" }
}
