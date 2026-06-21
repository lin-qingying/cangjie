package org.cangnova.cangjie.chir.core.pipeline

class ChirPipelineScheduler(
    private val passes: List<ChirPipelinePass>,
) {
    private val passByName = passes.associateBy { it.metadata.name }

    fun sortedPasses(): List<ChirPipelinePass> {
        val inDegree = LinkedHashMap<String, Int>()
        val outgoing = LinkedHashMap<String, MutableSet<String>>()

        passes.forEach { pass ->
            val name = pass.metadata.name
            inDegree.putIfAbsent(name, 0)
            outgoing.putIfAbsent(name, linkedSetOf())
        }

        passes.forEach { pass ->
            pass.metadata.dependsOn.forEach { dependency ->
                require(dependency in passByName) {
                    "pass ${pass.metadata.name} depends on missing pass $dependency"
                }
                outgoing.getValue(dependency).add(pass.metadata.name)
                inDegree[pass.metadata.name] = inDegree.getValue(pass.metadata.name) + 1
            }
        }

        val queue = ArrayDeque(inDegree.filterValues { it == 0 }.keys.sorted())
        val sortedNames = mutableListOf<String>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            sortedNames += current
            outgoing.getValue(current)
                .sorted()
                .forEach { next ->
                    val nextInDegree = inDegree.getValue(next) - 1
                    inDegree[next] = nextInDegree
                    if (nextInDegree == 0) {
                        queue.addLast(next)
                    }
                }
        }

        require(sortedNames.size == passes.size) {
            val unresolved = inDegree.filterValues { it > 0 }.keys.sorted()
            "cyclic pass dependencies detected: ${unresolved.joinToString()}"
        }

        return sortedNames.map { passByName.getValue(it) }
    }

    fun execute(
        context: ChirPassContext,
        cache: ChirAnalysisCache,
    ): List<ChirPassExecutionRecord> {
        sortedPasses().forEach { pass ->
            val output = context.runPass(
                passName = pass.metadata.name,
                action = { pass.execute(cache) },
                touchedNodesProvider = { it.touchedNodes },
                summaryProvider = { output ->
                    val invalidates = pass.metadata.effectiveInvalidates
                    val invalidateText = if (invalidates.isEmpty()) "none" else invalidates.joinToString()
                    val base = output.summary ?: "completed"
                    "$base | invalidates=$invalidateText"
                },
            )
            cache.invalidate(pass.metadata.effectiveInvalidates)
        }
        return context.records
    }
}
