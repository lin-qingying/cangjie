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
    private val testServices: TestServices,
    private val testModules: List<TestModule>
) : TestService {
    private val artifactsByModule: MutableMap<TestModule, MutableMap<TestArtifactKind<*>, ResultingArtifact<*>>> = mutableMapOf()

    fun <OutputArtifact : ResultingArtifact<OutputArtifact>> getArtifactSafe(
        module: TestModule,
        kind: TestArtifactKind<OutputArtifact>,
    ): OutputArtifact? {
        @Suppress("UNCHECKED_CAST")
        return artifactsByModule.getMap(module)[kind] as OutputArtifact?
    }

    fun <A : ResultingArtifact<A>> getArtifact(module: TestModule, kind: TestArtifactKind<A>): A {
        return getArtifactSafe(module, kind) ?: error("Artifact with kind $kind is not registered for module ${module.name}")
    }

    fun <OutputArtifact : ResultingArtifact<OutputArtifact>> registerArtifact(
        module: TestModule,
        artifact: ResultingArtifact<OutputArtifact>,
    ) {
        val artifacts = artifactsByModule.getMap(module)
        artifacts[artifact.kind] = artifact
    }

    fun unregisterAllArtifacts(module: TestModule) {
        artifactsByModule.remove(module)
    }

    fun copy(): ArtifactsProvider {
        return ArtifactsProvider(testServices, testModules).also {
            it.artifactsByModule.putAll(artifactsByModule.mapValues { (_, map) -> map.toMutableMap() })
        }
    }

    private fun <K, V, R> MutableMap<K, MutableMap<V, R>>.getMap(key: K): MutableMap<V, R> {
        return getOrPut(key) { mutableMapOf() }
    }
}

val TestServices.artifactsProvider: ArtifactsProvider by TestServices.testServiceAccessor()
