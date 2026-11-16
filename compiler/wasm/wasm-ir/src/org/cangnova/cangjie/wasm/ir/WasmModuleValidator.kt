package org.cangnova.cangjie.wasm.ir

/** 符号化 Wasm module 违反 Core 约束时，在二进制发射前抛出。 */
class WasmIrValidationException(message: String) : IllegalArgumentException(message)

/** 验证 module 符号、函数签名、资源限制、section 引用与指令操作数栈类型。 */
object WasmModuleValidator {
    /** 校验 Core binary writer 所需的 module 与函数体不变量。 */
    fun validate(module: WasmModule) {
        val functionByName = (module.imports.map { it.functionName to it.type } +
            module.functions.map { it.name to it.type }).toMapUnique("function")
        val globalByName = buildMap {
            module.globalImports.forEach { import ->
                if (put(import.globalName, GlobalInfo(import.type, import.mutable)) != null) {
                    fail("duplicate global symbol '${import.globalName}'")
                }
            }
            module.globals.forEach { global ->
                if (put(global.name, GlobalInfo(global.type, global.mutable)) != null) {
                    fail("duplicate global symbol '${global.name}'")
                }
            }
        }
        val memoryByName = module.memory?.let { mapOf(it.name to it) }.orEmpty()
        val tableByName = module.table?.let { mapOf(it.name to it) }.orEmpty()
        val externalImports = mutableSetOf<Pair<String, String>>()

        if (module.memory == null && module.dataSegments.isNotEmpty()) {
            fail("data segments require a defined linear memory")
        }
        if (module.table == null && module.elementSegments.isNotEmpty()) {
            fail("element segments require a defined function table")
        }
        if (module.table == null && module.functions.any { function -> function.body.any { it.containsIndirectCall() } }) {
            fail("call_indirect requires a defined function table")
        }
        module.memory?.let { memory ->
            val max = memory.limits.maximumPages
            if (max != null && max < memory.limits.minimumPages) {
                fail("memory '${memory.name}' maximum is below its minimum")
            }
        }

        module.imports.forEach { import ->
            if (import.moduleName.isEmpty()) fail("function import '${import.functionName}' has an empty module name")
            if (import.importName.isEmpty()) fail("function import '${import.functionName}' has an empty field name")
            if (!externalImports.add(import.moduleName to import.importName)) {
                fail("duplicate external import '${import.moduleName}.${import.importName}'")
            }
            validateFunctionType(import.type, "import '${import.moduleName}.${import.importName}'")
        }
        module.globalImports.forEach { import ->
            if (import.moduleName.isEmpty()) fail("global import '${import.globalName}' has an empty module name")
            if (import.importName.isEmpty()) fail("global import '${import.globalName}' has an empty field name")
            if (!externalImports.add(import.moduleName to import.importName)) {
                fail("duplicate external import '${import.moduleName}.${import.importName}'")
            }
        }
        module.functions.forEach { function ->
            validateFunctionType(function.type, "function '${function.name}'")
            if (function.parameters.map { it.type } != function.type.parameters) {
                fail("function '${function.name}' parameter locals do not match its function type")
            }
            val locals = (function.parameters + function.locals).associateByUnique { it.name to "local in '${function.name}'" }
            if (function.locals.any { it.name in function.parameters.map(WasmLocal::name) }) {
                fail("function '${function.name}' declares a local with the same name as a parameter")
            }
            verifyInstructions(
                function = function,
                instructions = function.body,
                locals = locals,
                functionByName = functionByName,
                globalByName = globalByName,
                memoryByName = memoryByName,
                tableByName = tableByName,
            )
        }
        module.globals.forEach { global ->
            if (global.initializer.type != global.type) {
                fail("global '${global.name}' initializer type ${global.initializer.type.displayName} does not match ${global.type.displayName}")
            }
        }

        val exportNames = mutableSetOf<String>()
        module.exports.forEach { export ->
            if (!exportNames.add(export.externalName)) fail("duplicate export name '${export.externalName}'")
            when (export) {
                is WasmExport.Function -> if (export.functionName !in functionByName) {
                    fail("export '${export.externalName}' references missing function '${export.functionName}'")
                }
                is WasmExport.Memory -> if (export.memoryName !in memoryByName) {
                    fail("export '${export.externalName}' references missing memory '${export.memoryName}'")
                }
                is WasmExport.Global -> if (export.globalName !in globalByName) {
                    fail("export '${export.externalName}' references missing global '${export.globalName}'")
                }
            }
        }

        module.startFunctionName?.let { name ->
            val type = functionByName[name] ?: fail("start section references missing function '$name'")
            if (type.parameters.isNotEmpty() || type.results.isNotEmpty()) {
                fail("start function '$name' must have the [] -> [] signature")
            }
        }

        module.dataSegments.forEachIndexed { index, segment ->
            val memory = module.memory ?: fail("data segment $index requires memory")
            val maximumAddress = memory.limits.minimumPages.toULong() * WASM_PAGE_SIZE.toULong()
            val endAddress = segment.offset.toULong() + segment.bytes.size.toULong()
            if (endAddress > maximumAddress) {
                fail("data segment $index ends at $endAddress, beyond initial memory size $maximumAddress")
            }
        }
        module.elementSegments.forEachIndexed { index, segment ->
            val table = tableByName[segment.tableName] ?: fail("element segment $index references missing table '${segment.tableName}'")
            val endElement = segment.offset.toULong() + segment.functionNames.size.toULong()
            if (endElement > table.limits.minimumElements.toULong()) {
                fail("element segment $index exceeds initial table size ${table.limits.minimumElements}")
            }
            segment.functionNames.forEach { name ->
                if (name !in functionByName) fail("element segment $index references missing function '$name'")
            }
        }
    }

    private fun validateFunctionType(type: WasmFunctionType, owner: String) {
        if (type.results.size > 1) fail("$owner uses multi-value results unsupported by the current Cangjie Wasm ABI")
    }

    private fun verifyInstructions(
        function: WasmFunction,
        instructions: List<WasmInstruction>,
        locals: Map<String, WasmLocal>,
        functionByName: Map<String, WasmFunctionType>,
        globalByName: Map<String, GlobalInfo>,
        memoryByName: Map<String, WasmMemory>,
        tableByName: Map<String, WasmTable>,
    ) {
        val flow = verifySequence(
            function = function,
            instructions = instructions,
            initialStack = emptyList(),
            labels = emptyMap(),
            locals = locals,
            functionByName = functionByName,
            globalByName = globalByName,
            memoryByName = memoryByName,
            tableByName = tableByName,
        )
        if (flow.reachable && flow.stack != function.type.results) {
            fail("function '${function.name}' leaves stack ${flow.stack.display()} instead of result ${function.type.results.display()}")
        }
        if (flow.branchTargets.isNotEmpty()) {
            fail("function '${function.name}' branches to out-of-scope label(s): ${flow.branchTargets.sorted().joinToString()}")
        }
    }

    private fun verifySequence(
        function: WasmFunction,
        instructions: List<WasmInstruction>,
        initialStack: List<WasmValueType>,
        labels: Map<String, Label>,
        locals: Map<String, WasmLocal>,
        functionByName: Map<String, WasmFunctionType>,
        globalByName: Map<String, GlobalInfo>,
        memoryByName: Map<String, WasmMemory>,
        tableByName: Map<String, WasmTable>,
    ): Flow {
        val stack = initialStack.toMutableList()
        var reachable = true
        val branchTargets = linkedSetOf<String>()

        instructions.forEachIndexed { instructionIndex, instruction ->
            if (!reachable) {
                fail("function '${function.name}' contains an instruction after an unconditional terminator at index $instructionIndex")
            }
            when (instruction) {
                WasmInstruction.Unreachable -> {
                    stack.clear()
                    reachable = false
                }
                WasmInstruction.Nop -> Unit
                WasmInstruction.Return -> {
                    function.type.results.asReversed().forEach { stack.popExpected(it, function.name) }
                    if (stack != initialStack) fail("return in '${function.name}' leaves extra operand values ${stack.display()}")
                    stack.clear()
                    reachable = false
                }
                WasmInstruction.Drop -> stack.popAny(function.name)
                WasmInstruction.Select -> {
                    stack.popExpected(WasmValueType.I32, function.name)
                    val right = stack.popAny(function.name)
                    val left = stack.popAny(function.name)
                    if (left != right) fail("select in '${function.name}' has mismatched operands ${left.displayName} and ${right.displayName}")
                    stack += left
                }
                WasmInstruction.MemorySize -> {
                    requireMemory(function, memoryByName)
                    stack += WasmValueType.I32
                }
                WasmInstruction.MemoryGrow -> {
                    requireMemory(function, memoryByName)
                    stack.popExpected(WasmValueType.I32, function.name)
                    stack += WasmValueType.I32
                }
                is WasmInstruction.Block -> {
                    ensureNewLabel(instruction.label, labels, function)
                    val bodyFlow = verifySequence(
                        function,
                        instruction.body,
                        stack,
                        labels + (instruction.label to Label(stack.size, loop = false)),
                        locals,
                        functionByName,
                        globalByName,
                        memoryByName,
                        tableByName,
                    )
                    if (bodyFlow.reachable && bodyFlow.stack != stack) {
                        fail("block '${instruction.label}' in '${function.name}' leaves an invalid operand stack ${bodyFlow.stack.display()}")
                    }
                    branchTargets += bodyFlow.branchTargets - instruction.label
                    reachable = bodyFlow.reachable || instruction.label in bodyFlow.branchTargets
                }
                is WasmInstruction.Loop -> {
                    ensureNewLabel(instruction.label, labels, function)
                    val bodyFlow = verifySequence(
                        function,
                        instruction.body,
                        stack,
                        labels + (instruction.label to Label(stack.size, loop = true)),
                        locals,
                        functionByName,
                        globalByName,
                        memoryByName,
                        tableByName,
                    )
                    if (bodyFlow.reachable && bodyFlow.stack != stack) {
                        fail("loop '${instruction.label}' in '${function.name}' leaves an invalid operand stack ${bodyFlow.stack.display()}")
                    }
                    branchTargets += bodyFlow.branchTargets - instruction.label
                    reachable = bodyFlow.reachable
                }
                is WasmInstruction.If -> {
                    stack.popExpected(WasmValueType.I32, function.name)
                    ensureNewLabel(instruction.label, labels, function)
                    val branchLabels = labels + (instruction.label to Label(stack.size, loop = false))
                    val thenFlow = verifySequence(
                        function,
                        instruction.thenBody,
                        stack,
                        branchLabels,
                        locals,
                        functionByName,
                        globalByName,
                        memoryByName,
                        tableByName,
                    )
                    val elseFlow = if (instruction.elseBody.isEmpty()) {
                        Flow(stack.toList(), reachable = true, branchTargets = emptySet())
                    } else {
                        verifySequence(
                            function,
                            instruction.elseBody,
                            stack,
                            branchLabels,
                            locals,
                            functionByName,
                            globalByName,
                            memoryByName,
                            tableByName,
                        )
                    }
                    if (thenFlow.reachable && thenFlow.stack != stack) {
                        fail("then branch of '${instruction.label}' in '${function.name}' leaves an invalid operand stack")
                    }
                    if (elseFlow.reachable && elseFlow.stack != stack) {
                        fail("else branch of '${instruction.label}' in '${function.name}' leaves an invalid operand stack")
                    }
                    val allTargets = thenFlow.branchTargets + elseFlow.branchTargets
                    branchTargets += allTargets - instruction.label
                    reachable = thenFlow.reachable || elseFlow.reachable || instruction.label in allTargets
                }
                is WasmInstruction.Br -> {
                    requireLabel(instruction.label, labels, function)
                    branchTargets += instruction.label
                    stack.clear()
                    reachable = false
                }
                is WasmInstruction.BrIf -> {
                    stack.popExpected(WasmValueType.I32, function.name)
                    requireLabel(instruction.label, labels, function)
                    branchTargets += instruction.label
                }
                is WasmInstruction.BrTable -> {
                    stack.popExpected(WasmValueType.I32, function.name)
                    (instruction.labels + instruction.defaultLabel).forEach { requireLabel(it, labels, function) }
                    branchTargets += instruction.labels
                    branchTargets += instruction.defaultLabel
                    stack.clear()
                    reachable = false
                }
                is WasmInstruction.Call -> {
                    val type = functionByName[instruction.functionName]
                        ?: fail("function '${function.name}' calls unknown function '${instruction.functionName}'")
                    type.parameters.asReversed().forEach { stack.popExpected(it, function.name) }
                    stack += type.results
                }
                is WasmInstruction.CallIndirect -> {
                    if (instruction.tableName !in tableByName) {
                        fail("function '${function.name}' calls through missing table '${instruction.tableName}'")
                    }
                    validateFunctionType(instruction.type, "call_indirect in '${function.name}'")
                    stack.popExpected(WasmValueType.I32, function.name)
                    instruction.type.parameters.asReversed().forEach { stack.popExpected(it, function.name) }
                    stack += instruction.type.results
                }
                is WasmInstruction.LocalGet -> {
                    val local = locals[instruction.localName]
                        ?: fail("function '${function.name}' reads unknown local '${instruction.localName}'")
                    stack += local.type
                }
                is WasmInstruction.LocalSet -> {
                    val local = locals[instruction.localName]
                        ?: fail("function '${function.name}' writes unknown local '${instruction.localName}'")
                    stack.popExpected(local.type, function.name)
                }
                is WasmInstruction.LocalTee -> {
                    val local = locals[instruction.localName]
                        ?: fail("function '${function.name}' tees unknown local '${instruction.localName}'")
                    stack.popExpected(local.type, function.name)
                    stack += local.type
                }
                is WasmInstruction.GlobalGet -> {
                    val global = globalByName[instruction.globalName]
                        ?: fail("function '${function.name}' reads unknown global '${instruction.globalName}'")
                    stack += global.type
                }
                is WasmInstruction.GlobalSet -> {
                    val global = globalByName[instruction.globalName]
                        ?: fail("function '${function.name}' writes unknown global '${instruction.globalName}'")
                    if (!global.mutable) fail("function '${function.name}' writes immutable global '${instruction.globalName}'")
                    stack.popExpected(global.type, function.name)
                }
                is WasmInstruction.I32Const -> stack += WasmValueType.I32
                is WasmInstruction.I64Const -> stack += WasmValueType.I64
                is WasmInstruction.F32Const -> stack += WasmValueType.F32
                is WasmInstruction.F64Const -> stack += WasmValueType.F64
                is WasmInstruction.Numeric -> {
                    instruction.operator.inputs.asReversed().forEach { stack.popExpected(it, function.name) }
                    stack += instruction.operator.result
                }
                is WasmInstruction.Load -> {
                    requireMemory(function, memoryByName)
                    if (instruction.alignmentExponent > instruction.kind.naturalAlignmentExponent) {
                        fail("function '${function.name}' has an over-aligned ${instruction.kind.name} load")
                    }
                    stack.popExpected(WasmValueType.I32, function.name)
                    stack += instruction.kind.resultType
                }
                is WasmInstruction.Store -> {
                    requireMemory(function, memoryByName)
                    if (instruction.alignmentExponent > instruction.kind.naturalAlignmentExponent) {
                        fail("function '${function.name}' has an over-aligned ${instruction.kind.name} store")
                    }
                    stack.popExpected(instruction.kind.valueType, function.name)
                    stack.popExpected(WasmValueType.I32, function.name)
                }
            }
        }
        return Flow(stack.toList(), reachable, branchTargets)
    }

    private fun requireMemory(function: WasmFunction, memories: Map<String, WasmMemory>) {
        if (memories.isEmpty()) fail("function '${function.name}' uses memory without a memory definition")
    }

    private fun ensureNewLabel(label: String, labels: Map<String, Label>, function: WasmFunction) {
        if (label.isEmpty()) fail("function '${function.name}' has an empty control label")
        if (label in labels) fail("function '${function.name}' reuses control label '$label' while it is in scope")
    }

    private fun requireLabel(label: String, labels: Map<String, Label>, function: WasmFunction) {
        if (label !in labels) fail("function '${function.name}' branches to out-of-scope label '$label'")
    }

    private fun <K, V> List<Pair<K, V>>.toMapUnique(kind: String): Map<K, V> = buildMap {
        for ((key, value) in this@toMapUnique) {
            if (put(key, value) != null) fail("duplicate $kind symbol '$key'")
        }
    }

    private fun <T, K> Iterable<T>.associateByUnique(key: (T) -> Pair<K, String>): Map<K, T> = buildMap {
        for (value in this@associateByUnique) {
            val (entryKey, kind) = key(value)
            if (put(entryKey, value) != null) fail("duplicate $kind symbol '$entryKey'")
        }
    }

    private fun MutableList<WasmValueType>.popExpected(expected: WasmValueType, functionName: String) {
        val actual = popAny(functionName)
        if (actual != expected) {
            fail("function '$functionName' expects ${expected.displayName} on the operand stack, found ${actual.displayName}")
        }
    }

    private fun MutableList<WasmValueType>.popAny(functionName: String): WasmValueType =
        removeLastOrNull() ?: fail("function '$functionName' operand stack underflow")

    private fun List<WasmValueType>.display(): String = joinToString(prefix = "[", postfix = "]") { it.displayName }

    private fun WasmValueType?.displayName(): String = this?.displayName ?: "<empty>"

    private data class GlobalInfo(val type: WasmValueType, val mutable: Boolean)

    private data class Label(val stackHeight: Int, val loop: Boolean)

    private data class Flow(
        val stack: List<WasmValueType>,
        val reachable: Boolean,
        val branchTargets: Set<String>,
    )

    private fun fail(message: String): Nothing = throw WasmIrValidationException(message)

    private fun WasmInstruction.containsIndirectCall(): Boolean = when (this) {
        is WasmInstruction.CallIndirect -> true
        is WasmInstruction.Block -> body.any { it.containsIndirectCall() }
        is WasmInstruction.Loop -> body.any { it.containsIndirectCall() }
        is WasmInstruction.If -> thenBody.any { it.containsIndirectCall() } || elseBody.any { it.containsIndirectCall() }
        else -> false
    }

    private const val WASM_PAGE_SIZE = 65_536
}
