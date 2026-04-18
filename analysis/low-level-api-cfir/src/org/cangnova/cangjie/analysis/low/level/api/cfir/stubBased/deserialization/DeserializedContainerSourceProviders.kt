/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.impl.KotlinStubOrigin
import org.cangnova.cangjie.resolve.jvm.JvmClassName
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource

internal interface DeserializedContainerSourceProvider {
    fun getFacadeContainerSource(
        file: CjFile,
        stubOrigin: KotlinStubOrigin?,
        declarationOrigin: CfirDeclarationOrigin,
    ): DeserializedContainerSource?

    fun getClassContainerSource(classId: ClassId): DeserializedContainerSource?
}

// Currently, `null` is returned for KLIBs to avoid incorrect application of JVM file facade logic and overload filtering.
// We might want to provide non-`null` container source for all types of binaries in the future.
internal object NullDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getFacadeContainerSource(
        file: CjFile,
        stubOrigin: KotlinStubOrigin?,
        declarationOrigin: CfirDeclarationOrigin,
    ): DeserializedContainerSource? = null

    override fun getClassContainerSource(classId: ClassId): DeserializedContainerSource? = null
}

internal object JvmDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getFacadeContainerSource(
        file: CjFile,
        stubOrigin: KotlinStubOrigin?,
        declarationOrigin: CfirDeclarationOrigin,
    ): DeserializedContainerSource {
        return when (stubOrigin) {
            is KotlinStubOrigin.Facade -> {
                val className = JvmClassName.byInternalName(stubOrigin.className)
                val jvmClassName = if (stubOrigin.jvmClassName != null) JvmClassName.byInternalName(stubOrigin.jvmClassName!!) else null
                JvmStubDeserializedFacadeContainerSource(className = className, jvmClassName = jvmClassName, facadeClassName = null)
            }
            is KotlinStubOrigin.MultiFileFacade -> {
                val className = JvmClassName.byInternalName(stubOrigin.className)
                val facadeClassName = JvmClassName.byInternalName(stubOrigin.facadeClassName)
                JvmStubDeserializedFacadeContainerSource(className = className, jvmClassName = null, facadeClassName)
            }
            else -> {
                val virtualFile = file.virtualFile
                val classId = ClassId(file.packageFqName, Name.identifier(virtualFile.nameWithoutExtension))
                val className = JvmClassName.byClassId(classId)
                JvmStubDeserializedFacadeContainerSource(className = className, jvmClassName = null, facadeClassName = null)
            }
        }
    }

    override fun getClassContainerSource(classId: ClassId): DeserializedContainerSource? {
        return JvmStubDeserializedContainerSource(classId)
    }
}

internal object BuiltinsDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getFacadeContainerSource(
        file: CjFile,
        stubOrigin: KotlinStubOrigin?,
        declarationOrigin: CfirDeclarationOrigin,
    ): DeserializedContainerSource {
        require(stubOrigin is KotlinStubOrigin.Facade) {
            "Expected builtins file to have Facade origin, got origin=$stubOrigin instead"
        }

        return JvmStubDeserializedBuiltInsContainerSource(
            facadeClassName = JvmClassName.byInternalName(stubOrigin.className)
        )
    }

    override fun getClassContainerSource(classId: ClassId): DeserializedContainerSource? {
        return JvmStubDeserializedContainerSource(classId)
    }
}

internal object JvmAndBuiltinsDeserializedContainerSourceProvider : DeserializedContainerSourceProvider {
    override fun getFacadeContainerSource(
        file: CjFile,
        stubOrigin: KotlinStubOrigin?,
        declarationOrigin: CfirDeclarationOrigin
    ): DeserializedContainerSource? {
        if (declarationOrigin is CfirDeclarationOrigin.BuiltIns) {
            return BuiltinsDeserializedContainerSourceProvider.getFacadeContainerSource(file, stubOrigin, declarationOrigin)
        }

        return JvmDeserializedContainerSourceProvider.getFacadeContainerSource(file, stubOrigin, declarationOrigin)
    }

    override fun getClassContainerSource(classId: ClassId): DeserializedContainerSource? {
        return JvmStubDeserializedContainerSource(classId)
    }
}
