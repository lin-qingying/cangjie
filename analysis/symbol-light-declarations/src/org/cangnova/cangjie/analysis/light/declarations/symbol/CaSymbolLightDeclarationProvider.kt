@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.light.declarations.symbol

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.components.asSignature
import org.cangnova.cangjie.analysis.api.components.declaredMemberScope
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.restoreSymbol
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.light.declarations.CaLightCallableDeclarationImpl
import org.cangnova.cangjie.analysis.light.declarations.CaLightClassLikeDeclarationImpl
import org.cangnova.cangjie.analysis.light.declarations.CaLightDeclarationCache
import org.cangnova.cangjie.analysis.light.declarations.CaLightDeclarationCacheKey
import org.cangnova.cangjie.analysis.light.declarations.CaLightExtendDeclarationImpl
import org.cangnova.cangjie.analysis.light.declarations.sourceOrigin
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * 基于 Analysis API symbol 的轻量声明视图提供器。
 *
 * 实现分为三层：
 * 1. 以 source/decompiled PSI 文件为入口；
 * 2. 在统一 analyze session 中把 PSI 恢复为公开 symbol；
 * 3. 用 `analysis:light-declarations` 的只读模型投影 class-like、extend 与 callable。
 */
class CaSymbolLightDeclarationProvider(
    private val project: Project,
) : CaLightDeclarationProvider {
    private val projectStructure: CangJieProjectStructureProvider
        get() = CangJieProjectStructureProvider.getInstance(project)

    override fun getLightDeclaration(symbol: CaSymbol): CaLightDeclaration? {
        val pointer = symbol.createPointer()
        return analyze(symbol.containingModule) {
            val restoredSymbol = restoreSymbol(pointer) ?: return@analyze null
            LightDeclarationBuilder(token, restoredSymbol.containingModule).build(this, restoredSymbol)
        }
    }

    override fun getLightDeclarations(file: CjFile, useSiteModule: CaModule?): List<CaLightDeclaration> {
        val module = useSiteModule ?: projectStructure.getModule(file, useSiteModule = null)
        return analyze(module) {
            LightDeclarationBuilder(token, module).buildFile(file)
        }
    }

    private inner class LightDeclarationBuilder(
        private val token: CaLifetimeToken,
        private val useSiteModule: CaModule,
    ) {
        private val cache = CaLightDeclarationCache()

        fun buildFile(file: CjFile): List<CaLightDeclaration> {
            /**
             * 对齐 Kotlin 的 declaration-first 入口：
             * 无论源码还是 decompiled file，都先沿 PSI 声明树建立 light declaration，
             * 需要语义信息时再按 use-site module 进入 analysis。
             */
            return file.declarations.mapNotNull(::buildDeclaration)
        }

        fun buildDeclaration(declaration: CjDeclaration): CaLightDeclaration? {
            return when (declaration) {
                is CjExtend -> buildExtend(declaration)
                is CjTypeStatement -> buildClassLike(declaration)
                is CjTypeAlias -> buildTypeAlias(declaration)
                is CjNamedDeclaration -> buildCallable(declaration)
                else -> null
            }
        }

        fun build(session: CaSession, symbol: CaSymbol): CaLightDeclaration? {
            return when (symbol) {
                is CaExtendSymbol -> buildExtend(session, symbol)
                is CaClassLikeSymbol -> buildClassLike(session, symbol)
                is CaCallableSymbol -> buildCallable(session, symbol)
                else -> null
            }
        }

        private fun buildClassLike(session: CaSession, symbol: CaClassLikeSymbol): CaLightDeclaration {
            val key = CaLightDeclarationCacheKey("classLike:${symbol.classId?.asString() ?: symbol.name?.asString() ?: symbol.hashCode()}")
            return cache.getOrPut(key) {
                val annotations = symbol.annotations
                val classId = symbol.classId
                val typeParameters = symbol.typeParameters.mapNotNull { typeParameter -> typeParameter.name }
                val superTypes = when (symbol) {
                    is CaClassSymbol -> symbol.superTypes
                    else -> emptyList()
                }
                val members = when (symbol) {
                    is CaClassSymbol -> with(session) { symbol.declaredMemberScope }
                        .declarations
                        .mapNotNull { member -> build(session, member) }
                        .toList()
                    else -> emptyList()
                }
                CaLightClassLikeDeclarationImpl(
                    name = symbol.name?.asString(),
                    module = symbol.containingModule,
                    annotationsFactory = { annotations },
                    origin = symbol.origin("class-like"),
                    token = token,
                    classIdFactory = { classId },
                    typeParametersFactory = { typeParameters },
                    superTypesFactory = { superTypes },
                    membersFactory = { members },
                )
            }
        }

        private fun buildExtend(session: CaSession, symbol: CaExtendSymbol): CaLightDeclaration {
            val key = CaLightDeclarationCacheKey("extend:${symbol.extendId}")
            return cache.getOrPut(key) {
                val annotations = symbol.annotations
                val targetClassId = symbol.targetClassId
                val extendedType = symbol.extendedType
                val typeParameters = symbol.typeParameters.mapNotNull { typeParameter -> typeParameter.name }
                val superTypes = symbol.superTypes
                val members = with(session) { symbol.declaredMemberScope }
                    .declarations
                    .mapNotNull { member -> build(session, member) }
                    .toList()
                CaLightExtendDeclarationImpl(
                    name = symbol.name?.asString(),
                    module = symbol.containingModule,
                    annotationsFactory = { annotations },
                    origin = symbol.origin("extend"),
                    token = token,
                    extendId = symbol.extendId,
                    targetClassIdFactory = { targetClassId },
                    extendedTypeFactory = { extendedType },
                    typeParametersFactory = { typeParameters },
                    superTypesFactory = { superTypes },
                    membersFactory = { members },
                )
            }
        }

        private fun buildCallable(session: CaSession, symbol: CaCallableSymbol): CaLightDeclaration {
            val callableId = symbol.callableId
            val signature = with(session) { symbol.asSignature() }
            val key = callableCacheKey(callableId, signature, symbol)
            return cache.getOrPut(key) {
                val annotations = symbol.annotations
                CaLightCallableDeclarationImpl(
                    name = symbol.name?.asString(),
                    module = symbol.containingModule,
                    annotationsFactory = { annotations },
                    origin = symbol.origin("callable"),
                    token = token,
                    callableIdFactory = { callableId },
                    signatureFactory = { signature },
                )
            }
        }

        private fun buildClassLike(declaration: CjTypeStatement): CaLightDeclaration {
            val key = CaLightDeclarationCacheKey("classLike:${declaration.getClassId()?.asString() ?: declaration.name ?: declaration.hashCode()}")
            return cache.getOrPut(key) {
                CaLightClassLikeDeclarationImpl(
                    name = declaration.name,
                    module = useSiteModule,
                    annotationsFactory = { analyze(useSiteModule) { declaration.classSymbol.annotations } },
                    origin = declaration.origin("class-like"),
                    token = token,
                    classIdFactory = { declaration.getClassId() },
                    typeParametersFactory = { declaration.typeParameters.map { typeParameter -> typeParameter.nameAsSafeName } },
                    superTypesFactory = { analyze(useSiteModule) { declaration.classSymbol.superTypes } },
                    membersFactory = { declaration.declarations.mapNotNull(::buildDeclaration) },
                )
            }
        }

        private fun buildTypeAlias(declaration: CjTypeAlias): CaLightDeclaration {
            val key = CaLightDeclarationCacheKey("classLike:${declaration.getClassId()?.asString() ?: declaration.name ?: declaration.hashCode()}")
            return cache.getOrPut(key) {
                CaLightClassLikeDeclarationImpl(
                    name = declaration.name,
                    module = useSiteModule,
                    annotationsFactory = { analyze(useSiteModule) { declaration.symbol.annotations } },
                    origin = declaration.origin("class-like"),
                    token = token,
                    classIdFactory = { declaration.getClassId() },
                    typeParametersFactory = { declaration.typeParameters.map { typeParameter -> typeParameter.nameAsSafeName } },
                    superTypesFactory = { emptyList() },
                    membersFactory = { emptyList() },
                )
            }
        }

        private fun buildExtend(declaration: CjExtend): CaLightDeclaration {
            val key = CaLightDeclarationCacheKey("extend:${declaration.getExtendId()}")
            return cache.getOrPut(key) {
                CaLightExtendDeclarationImpl(
                    name = declaration.nameAsSafeName.asString(),
                    module = useSiteModule,
                    annotationsFactory = { analyze(useSiteModule) { declaration.symbol.annotations } },
                    origin = declaration.origin("extend"),
                    token = token,
                    extendId = declaration.getExtendId(),
                    targetClassIdFactory = { analyze(useSiteModule) { declaration.symbol.targetClassId } },
                    extendedTypeFactory = { analyze(useSiteModule) { declaration.symbol.extendedType } },
                    typeParametersFactory = { declaration.typeParameters.map { typeParameter -> typeParameter.nameAsSafeName } },
                    superTypesFactory = { analyze(useSiteModule) { declaration.symbol.superTypes } },
                    membersFactory = { declaration.declarations.mapNotNull(::buildDeclaration) },
                )
            }
        }

        private fun buildCallable(declaration: CjNamedDeclaration): CaLightDeclaration {
            val callableData = analyze(useSiteModule) {
                val callableSymbol = declaration.symbol as? CaCallableSymbol ?: return@analyze null
                CallableLightDeclarationData(
                    callableId = callableSymbol.callableId,
                    signature = callableSymbol.asSignature(),
                    annotations = callableSymbol.annotations,
                )
            }
            val key = callableCacheKey(callableData?.callableId, callableData?.signature, declaration)
            return cache.getOrPut(key) {
                CaLightCallableDeclarationImpl(
                    name = declaration.name,
                    module = useSiteModule,
                    annotationsFactory = { callableData?.annotations ?: emptyList() },
                    origin = declaration.origin("callable"),
                    token = token,
                    callableIdFactory = { callableData?.callableId },
                    signatureFactory = { callableData?.signature },
                )
            }
        }

        private fun callableCacheKey(
            callableId: CallableId?,
            signature: CaCallableSignature<*>?,
            fallbackIdentity: Any,
        ): CaLightDeclarationCacheKey {
            return CaLightDeclarationCacheKey(
                "callable:${callableId}:${signature?.hashCode() ?: fallbackIdentity.hashCode()}",
            )
        }

        private fun CaSymbol.origin(kind: String) = sourceOrigin(
            description = when (this) {
                is CaClassLikeSymbol -> classId?.asString() ?: name?.asString() ?: kind
                is CaExtendSymbol -> extendId
                is CaCallableSymbol -> callableId?.toString() ?: name?.asString() ?: kind
                else -> name?.asString() ?: kind
            },
            containingFile = psi?.containingFile as? CjFile,
            sourceElement = psi,
        )

        private fun CjDeclaration.origin(kind: String) = sourceOrigin(
            description = when (this) {
                is CjExtend -> getExtendId()
                is CjTypeStatement -> getClassId()?.asString() ?: name ?: kind
                is CjTypeAlias -> getClassId()?.asString() ?: name ?: kind
                is CjNamedDeclaration -> fqName?.asString() ?: name ?: kind
                else -> kind
            },
            containingFile = containingFile as? CjFile,
            sourceElement = this,
        )

    }
}

private data class CallableLightDeclarationData(
    val callableId: CallableId?,
    val signature: CaCallableSignature<*>?,
    val annotations: CaAnnotationList,
)
