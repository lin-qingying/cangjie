package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.model.TestArtifactKind
import org.cangnova.cangjie.test.model.TestModule

/**
 * 存储每个模块的测试产物
 *
 * 对应 Kotlin K2 的 ArtifactsProvider
 */
class ArtifactsProvider(
    /**
     * 保存 `testServices`，供测试服务在测试执行期间读取或传递。
     */
    private val testServices: TestServices,
    /**
     * 保存 `testModules`，供测试服务在测试执行期间读取或传递。
     */
    private val testModules: List<TestModule>
) : TestService {
    /**
     * 保存 `artifactsByModule`，供测试服务在测试执行期间读取或传递。
     */
    private val artifactsByModule: MutableMap<TestModule, MutableMap<TestArtifactKind<*>, ResultingArtifact<*>>> = mutableMapOf()

    /**
     * 执行 `>` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun <OutputArtifact : ResultingArtifact<OutputArtifact>> getArtifactSafe(
        module: TestModule,
        kind: TestArtifactKind<OutputArtifact>,
    ): OutputArtifact? {
        @Suppress("UNCHECKED_CAST")
        return artifactsByModule.getMap(module)[kind] as OutputArtifact?
    }

    /**
     * 执行 `>` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun <A : ResultingArtifact<A>> getArtifact(module: TestModule, kind: TestArtifactKind<A>): A {
        return getArtifactSafe(module, kind) ?: error("Artifact with kind $kind is not registered for module ${module.name}")
    }

    /**
     * 执行 `>` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun <OutputArtifact : ResultingArtifact<OutputArtifact>> registerArtifact(
        module: TestModule,
        artifact: ResultingArtifact<OutputArtifact>,
    ) {
        val artifacts = artifactsByModule.getMap(module)
        artifacts[artifact.kind] = artifact
    }

    /**
     * 执行 `unregisterAllArtifacts` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun unregisterAllArtifacts(module: TestModule) {
        artifactsByModule.remove(module)
    }

    /**
     * 执行 `copy` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun copy(): ArtifactsProvider {
        return ArtifactsProvider(testServices, testModules).also {
            it.artifactsByModule.putAll(artifactsByModule.mapValues { (_, map) -> map.toMutableMap() })
        }
    }

    /**
     * 提供 `MutableMap` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun <K, V, R> MutableMap<K, MutableMap<V, R>>.getMap(key: K): MutableMap<V, R> {
        return getOrPut(key) { mutableMapOf() }
    }
}

/**
 * 保存 `TestServices.artifactsProvider`，供测试服务在测试执行期间读取或传递。
 */
val TestServices.artifactsProvider: ArtifactsProvider by TestServices.testServiceAccessor()
