package org.cangnova.cangjie.wasm.ir

import java.io.ByteArrayOutputStream

/**
 * WebAssembly Core 二进制发射器。
 *
 * 先验证完整 module，再在此处分配函数、局部变量和全局变量索引，保持 CHIR lowering 与
 * section 布局解耦；相同的具名 module 输入产生相同的二进制字节。
 */
class WasmBinaryWriter {
    /** 验证 [module] 并编码为 WebAssembly Core 二进制 module。 */
    fun write(module: WasmModule, emitNameSection: Boolean = true): ByteArray {
        WasmModuleValidator.validate(module)
        val indices = Indices(module)
        val output = ByteArrayOutputStream()
        output.write(MAGIC)
        output.write(VERSION)

        if (indices.types.isNotEmpty()) {
            writeSection(output, TYPE_SECTION) {
                writeVectorSize(indices.types.size)
                indices.types.forEach { writeFunctionType(it) }
            }
        }

        if (module.imports.isNotEmpty() || module.globalImports.isNotEmpty()) {
            writeSection(output, IMPORT_SECTION) {
                writeVectorSize(module.imports.size + module.globalImports.size)
                module.imports.forEach { import ->
                    writeName(import.moduleName)
                    writeName(import.importName)
                    writeByte(EXTERNAL_KIND_FUNCTION)
                    writeVarUInt(indices.typeIndex(import.type))
                }
                module.globalImports.forEach { import ->
                    writeName(import.moduleName)
                    writeName(import.importName)
                    writeByte(EXTERNAL_KIND_GLOBAL)
                    writeByte(import.type.binaryCode.toInt())
                    writeByte(if (import.mutable) 0x01 else 0x00)
                }
            }
        }

        if (module.functions.isNotEmpty()) {
            writeSection(output, FUNCTION_SECTION) {
                writeVectorSize(module.functions.size)
                module.functions.forEach { function -> writeVarUInt(indices.typeIndex(function.type)) }
            }
        }

        module.table?.let { table ->
            writeSection(output, TABLE_SECTION) {
                writeVectorSize(1)
                writeByte(FUNCREF_TYPE)
                writeTableLimits(table.limits)
            }
        }

        module.memory?.let { memory ->
            writeSection(output, MEMORY_SECTION) {
                writeVectorSize(1)
                writeLimits(memory.limits)
            }
        }

        if (module.globals.isNotEmpty()) {
            writeSection(output, GLOBAL_SECTION) {
                writeVectorSize(module.globals.size)
                module.globals.forEach { writeGlobal(it) }
            }
        }

        if (module.exports.isNotEmpty()) {
            writeSection(output, EXPORT_SECTION) {
                val exports = module.exports.sortedBy(WasmExport::externalName)
                writeVectorSize(exports.size)
                exports.forEach { export -> writeExport(export, indices) }
            }
        }

        module.startFunctionName?.let { name ->
            writeSection(output, START_SECTION) {
                writeVarUInt(indices.functionIndex(name))
            }
        }

        if (module.elementSegments.isNotEmpty()) {
            writeSection(output, ELEMENT_SECTION) {
                writeVectorSize(module.elementSegments.size)
                module.elementSegments.forEach { segment ->
                    indices.tableIndex(segment.tableName)
                    writeByte(ELEMENT_MODE_ACTIVE_TABLE_ZERO)
                    writeByte(I32_CONST)
                    writeVarInt(segment.offset.toInt())
                    writeByte(END)
                    writeVectorSize(segment.functionNames.size)
                    segment.functionNames.forEach { functionName -> writeVarUInt(indices.functionIndex(functionName)) }
                }
            }
        }

        if (module.functions.isNotEmpty()) {
            writeSection(output, CODE_SECTION) {
                writeVectorSize(module.functions.size)
                module.functions.forEach { function -> writeFunctionBody(function, indices) }
            }
        }

        if (module.dataSegments.isNotEmpty()) {
            writeSection(output, DATA_SECTION) {
                writeVectorSize(module.dataSegments.size)
                module.dataSegments.forEach { segment ->
                    writeByte(DATA_MODE_ACTIVE_MEMORY_ZERO)
                    writeByte(I32_CONST)
                    writeVarInt(segment.offset.toInt())
                    writeByte(END)
                    writeVarUInt(segment.bytes.size.toUInt())
                    writeBytes(segment.bytes)
                }
            }
        }

        if (emitNameSection) writeNameSection(output, module, indices)
        return output.toByteArray()
    }

    private fun ByteArrayOutputStream.writeFunctionType(type: WasmFunctionType) {
        writeByte(FUNCTION_TYPE)
        writeVectorSize(type.parameters.size)
        type.parameters.forEach { writeByte(it.binaryCode.toInt()) }
        writeVectorSize(type.results.size)
        type.results.forEach { writeByte(it.binaryCode.toInt()) }
    }

    private fun ByteArrayOutputStream.writeLimits(limits: WasmMemoryLimits) {
        val hasMaximum = limits.maximumPages != null
        writeByte(if (hasMaximum) 0x01 else 0x00)
        writeVarUInt(limits.minimumPages)
        limits.maximumPages?.let { writeVarUInt(it) }
    }

    private fun ByteArrayOutputStream.writeTableLimits(limits: WasmTableLimits) {
        val hasMaximum = limits.maximumElements != null
        writeByte(if (hasMaximum) 0x01 else 0x00)
        writeVarUInt(limits.minimumElements)
        limits.maximumElements?.let { writeVarUInt(it) }
    }

    private fun ByteArrayOutputStream.writeGlobal(global: WasmGlobal) {
        writeByte(global.type.binaryCode.toInt())
        writeByte(if (global.mutable) 0x01 else 0x00)
        writeConstant(global.initializer)
        writeByte(END)
    }

    private fun ByteArrayOutputStream.writeConstant(constant: WasmConstant) {
        when (constant) {
            is WasmConstant.I32 -> {
                writeByte(I32_CONST)
                writeVarInt(constant.value)
            }
            is WasmConstant.I64 -> {
                writeByte(I64_CONST)
                writeVarInt(constant.value)
            }
            is WasmConstant.F32 -> {
                writeByte(F32_CONST)
                writeFloat(constant.value)
            }
            is WasmConstant.F64 -> {
                writeByte(F64_CONST)
                writeDouble(constant.value)
            }
        }
    }

    private fun ByteArrayOutputStream.writeExport(export: WasmExport, indices: Indices) {
        writeName(export.externalName)
        when (export) {
            is WasmExport.Function -> {
                writeByte(EXTERNAL_KIND_FUNCTION)
                writeVarUInt(indices.functionIndex(export.functionName))
            }
            is WasmExport.Memory -> {
                writeByte(EXTERNAL_KIND_MEMORY)
                indices.memoryIndex(export.memoryName)
                writeVarUInt(0u)
            }
            is WasmExport.Global -> {
                writeByte(EXTERNAL_KIND_GLOBAL)
                writeVarUInt(indices.globalIndex(export.globalName))
            }
        }
    }

    private fun ByteArrayOutputStream.writeFunctionBody(function: WasmFunction, indices: Indices) {
        val body = ByteArrayOutputStream()
        val localGroups = function.locals.groupConsecutiveBy(WasmLocal::type)
        body.writeVarUInt(localGroups.size.toUInt())
        localGroups.forEach { (type, locals) ->
            body.writeVarUInt(locals.size.toUInt())
            body.write(type.binaryCode.toInt())
        }
        val labels = ArrayDeque<String>()
        function.body.forEach { instruction ->
            body.writeInstruction(instruction, function, indices, labels)
        }
        body.write(END)
        writeVarUInt(body.size().toUInt())
        writeBytes(body.toByteArray())
    }

    private fun ByteArrayOutputStream.writeInstruction(
        instruction: WasmInstruction,
        function: WasmFunction,
        indices: Indices,
        labels: ArrayDeque<String>,
    ) {
        when (instruction) {
            WasmInstruction.Unreachable -> writeByte(UNREACHABLE)
            WasmInstruction.Nop -> writeByte(NOP)
            WasmInstruction.Return -> writeByte(RETURN)
            WasmInstruction.Drop -> writeByte(DROP)
            WasmInstruction.Select -> writeByte(SELECT)
            WasmInstruction.MemorySize -> {
                writeByte(MEMORY_SIZE)
                writeByte(0)
            }
            WasmInstruction.MemoryGrow -> {
                writeByte(MEMORY_GROW)
                writeByte(0)
            }
            is WasmInstruction.Block -> {
                writeByte(BLOCK)
                writeByte(EMPTY_BLOCK_TYPE)
                labels.addLast(instruction.label)
                instruction.body.forEach { writeInstruction(it, function, indices, labels) }
                labels.removeLast()
                writeByte(END)
            }
            is WasmInstruction.Loop -> {
                writeByte(LOOP)
                writeByte(EMPTY_BLOCK_TYPE)
                labels.addLast(instruction.label)
                instruction.body.forEach { writeInstruction(it, function, indices, labels) }
                labels.removeLast()
                writeByte(END)
            }
            is WasmInstruction.If -> {
                writeByte(IF)
                writeByte(EMPTY_BLOCK_TYPE)
                labels.addLast(instruction.label)
                instruction.thenBody.forEach { writeInstruction(it, function, indices, labels) }
                if (instruction.elseBody.isNotEmpty()) {
                    writeByte(ELSE)
                    instruction.elseBody.forEach { writeInstruction(it, function, indices, labels) }
                }
                labels.removeLast()
                writeByte(END)
            }
            is WasmInstruction.Br -> {
                writeByte(BR)
                writeVarUInt(labels.depthOf(instruction.label))
            }
            is WasmInstruction.BrIf -> {
                writeByte(BR_IF)
                writeVarUInt(labels.depthOf(instruction.label))
            }
            is WasmInstruction.BrTable -> {
                writeByte(BR_TABLE)
                writeVectorSize(instruction.labels.size)
                instruction.labels.forEach { writeVarUInt(labels.depthOf(it)) }
                writeVarUInt(labels.depthOf(instruction.defaultLabel))
            }
            is WasmInstruction.Call -> {
                writeByte(CALL)
                writeVarUInt(indices.functionIndex(instruction.functionName))
            }
            is WasmInstruction.CallIndirect -> {
                writeByte(CALL_INDIRECT)
                writeVarUInt(indices.typeIndex(instruction.type))
                writeVarUInt(indices.tableIndex(instruction.tableName))
            }
            is WasmInstruction.LocalGet -> {
                writeByte(LOCAL_GET)
                writeVarUInt(indices.localIndex(function, instruction.localName))
            }
            is WasmInstruction.LocalSet -> {
                writeByte(LOCAL_SET)
                writeVarUInt(indices.localIndex(function, instruction.localName))
            }
            is WasmInstruction.LocalTee -> {
                writeByte(LOCAL_TEE)
                writeVarUInt(indices.localIndex(function, instruction.localName))
            }
            is WasmInstruction.GlobalGet -> {
                writeByte(GLOBAL_GET)
                writeVarUInt(indices.globalIndex(instruction.globalName))
            }
            is WasmInstruction.GlobalSet -> {
                writeByte(GLOBAL_SET)
                writeVarUInt(indices.globalIndex(instruction.globalName))
            }
            is WasmInstruction.I32Const -> {
                writeByte(I32_CONST)
                writeVarInt(instruction.value)
            }
            is WasmInstruction.I64Const -> {
                writeByte(I64_CONST)
                writeVarInt(instruction.value)
            }
            is WasmInstruction.F32Const -> {
                writeByte(F32_CONST)
                writeFloat(instruction.value)
            }
            is WasmInstruction.F64Const -> {
                writeByte(F64_CONST)
                writeDouble(instruction.value)
            }
            is WasmInstruction.Numeric -> writeByte(instruction.operator.opcode)
            is WasmInstruction.Load -> {
                writeByte(instruction.kind.opcode)
                writeVarUInt(instruction.alignmentExponent)
                writeVarUInt(instruction.offset)
            }
            is WasmInstruction.Store -> {
                writeByte(instruction.kind.opcode)
                writeVarUInt(instruction.alignmentExponent)
                writeVarUInt(instruction.offset)
            }
        }
    }

    private fun writeNameSection(output: ByteArrayOutputStream, module: WasmModule, indices: Indices) {
        val payload = ByteArrayOutputStream()
        payload.writeName("name")
        payload.writeNameSubsection(0) { writeName(module.name) }
        val namedFunctions = indices.functionNamesInIndexOrder
        if (namedFunctions.isNotEmpty()) {
            payload.writeNameSubsection(1) {
                writeVectorSize(namedFunctions.size)
                namedFunctions.forEachIndexed { index, functionName ->
                    writeVarUInt(index.toUInt())
                    writeName(functionName)
                }
            }
        }
        val functionsWithLocals = module.functions.map { function ->
            indices.functionIndex(function.name) to (function.parameters + function.locals)
        }.filter { (_, locals) -> locals.isNotEmpty() }
        if (functionsWithLocals.isNotEmpty()) {
            payload.writeNameSubsection(2) {
                writeVectorSize(functionsWithLocals.size)
                functionsWithLocals.forEach { (functionIndex, locals) ->
                    writeVarUInt(functionIndex)
                    writeVectorSize(locals.size)
                    locals.forEachIndexed { localIndex, local ->
                        writeVarUInt(localIndex.toUInt())
                        writeName(local.name)
                    }
                }
            }
        }
        output.writeSection(CUSTOM_SECTION, payload.toByteArray())
    }

    private fun ByteArrayOutputStream.writeNameSubsection(id: Int, content: ByteArrayOutputStream.() -> Unit) {
        val subsection = ByteArrayOutputStream().apply(content).toByteArray()
        write(id)
        writeVarUInt(subsection.size.toUInt())
        write(subsection)
    }

    private fun writeSection(output: ByteArrayOutputStream, id: Int, content: ByteArrayOutputStream.() -> Unit) {
        val payload = ByteArrayOutputStream().apply(content).toByteArray()
        output.writeSection(id, payload)
    }

    private fun ByteArrayOutputStream.writeSection(id: Int, payload: ByteArray) {
        write(id)
        writeVarUInt(payload.size.toUInt())
        write(payload)
    }

    private fun ByteArrayOutputStream.writeName(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeVarUInt(bytes.size.toUInt())
        write(bytes)
    }

    private fun ByteArrayOutputStream.writeByte(value: Int) = write(value and 0xff)

    private fun ByteArrayOutputStream.writeVectorSize(size: Int) = writeVarUInt(size.toUInt())

    private fun ByteArrayOutputStream.writeVarUInt(value: UInt) {
        var remaining = value
        do {
            var byte = (remaining and 0x7fu).toInt()
            remaining = remaining shr 7
            if (remaining != 0u) byte = byte or 0x80
            write(byte)
        } while (remaining != 0u)
    }

    private fun ByteArrayOutputStream.writeVarInt(value: Int) {
        var remaining = value
        while (true) {
            var byte = remaining and 0x7f
            remaining = remaining shr 7
            val done = (remaining == 0 && byte and 0x40 == 0) || (remaining == -1 && byte and 0x40 != 0)
            if (!done) byte = byte or 0x80
            write(byte)
            if (done) return
        }
    }

    private fun ByteArrayOutputStream.writeVarInt(value: Long) {
        var remaining = value
        while (true) {
            var byte = (remaining and 0x7f).toInt()
            remaining = remaining shr 7
            val done = (remaining == 0L && byte and 0x40 == 0) || (remaining == -1L && byte and 0x40 != 0)
            if (!done) byte = byte or 0x80
            write(byte)
            if (done) return
        }
    }

    private fun ByteArrayOutputStream.writeFloat(value: Float) {
        val bits = value.toRawBits()
        repeat(Float.SIZE_BYTES) { index -> write(bits ushr (index * 8) and 0xff) }
    }

    private fun ByteArrayOutputStream.writeDouble(value: Double) {
        val bits = value.toRawBits()
        repeat(Double.SIZE_BYTES) { index -> write((bits ushr (index * 8)).toInt() and 0xff) }
    }

    private fun <T, K> List<T>.groupConsecutiveBy(key: (T) -> K): List<Pair<K, List<T>>> {
        if (isEmpty()) return emptyList()
        val groups = mutableListOf<Pair<K, MutableList<T>>>()
        for (item in this) {
            val itemKey = key(item)
            if (groups.lastOrNull()?.first == itemKey) {
                groups.last().second += item
            } else {
                groups += itemKey to mutableListOf(item)
            }
        }
        return groups.map { (itemKey, items) -> itemKey to items.toList() }
    }

    private fun ArrayDeque<String>.depthOf(label: String): UInt {
        val depth = indexOfLast { it == label }
        if (depth < 0) throw WasmIrValidationException("cannot encode branch to out-of-scope label '$label'")
        return (lastIndex - depth).toUInt()
    }

    private inner class Indices(module: WasmModule) {
        val types: List<WasmFunctionType> = (
            module.imports.map(WasmImport::type) +
                module.functions.map(WasmFunction::type) +
                module.functions.flatMap { function -> function.body.indirectCallTypes() }
            ).distinct()
        private val typeIndices = types.withIndex().associate { (index, type) -> type to index.toUInt() }
        private val functionIndices = buildMap {
            var index = 0u
            module.imports.forEach { put(it.functionName, index++) }
            module.functions.forEach { put(it.name, index++) }
        }
        private val globalIndices = buildMap {
            var index = 0u
            module.globalImports.forEach { put(it.globalName, index++) }
            module.globals.forEach { put(it.name, index++) }
        }
        private val memoryNames = module.memory?.let { listOf(it.name) }.orEmpty()
        private val tableNames = module.table?.let { listOf(it.name) }.orEmpty()
        val functionNamesInIndexOrder: List<String> = module.imports.map(WasmImport::functionName) + module.functions.map(WasmFunction::name)

        fun typeIndex(type: WasmFunctionType): UInt = typeIndices[type]
            ?: throw WasmIrValidationException("function type was not registered")

        fun functionIndex(name: String): UInt = functionIndices[name]
            ?: throw WasmIrValidationException("function '$name' was not registered")

        fun globalIndex(name: String): UInt = globalIndices[name]
            ?: throw WasmIrValidationException("global '$name' was not registered")

        fun memoryIndex(name: String) {
            if (name !in memoryNames) throw WasmIrValidationException("memory '$name' was not registered")
        }

        fun tableIndex(name: String): UInt {
            val index = tableNames.indexOf(name)
            if (index < 0) throw WasmIrValidationException("table '$name' was not registered")
            return index.toUInt()
        }

        fun localIndex(function: WasmFunction, name: String): UInt {
            val index = (function.parameters + function.locals).indexOfFirst { it.name == name }
            if (index < 0) throw WasmIrValidationException("local '$name' in function '${function.name}' was not registered")
            return index.toUInt()
        }
    }

    private fun List<WasmInstruction>.indirectCallTypes(): List<WasmFunctionType> = flatMap { instruction ->
        when (instruction) {
            is WasmInstruction.CallIndirect -> listOf(instruction.type)
            is WasmInstruction.Block -> instruction.body.indirectCallTypes()
            is WasmInstruction.Loop -> instruction.body.indirectCallTypes()
            is WasmInstruction.If -> instruction.thenBody.indirectCallTypes() + instruction.elseBody.indirectCallTypes()
            else -> emptyList()
        }
    }

    private companion object {
        val MAGIC = byteArrayOf(0x00, 0x61, 0x73, 0x6d)
        val VERSION = byteArrayOf(0x01, 0x00, 0x00, 0x00)

        const val CUSTOM_SECTION = 0
        const val TYPE_SECTION = 1
        const val IMPORT_SECTION = 2
        const val FUNCTION_SECTION = 3
        const val TABLE_SECTION = 4
        const val MEMORY_SECTION = 5
        const val GLOBAL_SECTION = 6
        const val EXPORT_SECTION = 7
        const val START_SECTION = 8
        const val ELEMENT_SECTION = 9
        const val CODE_SECTION = 10
        const val DATA_SECTION = 11
        const val FUNCTION_TYPE = 0x60
        const val FUNCREF_TYPE = 0x70
        const val ELEMENT_MODE_ACTIVE_TABLE_ZERO = 0
        const val EMPTY_BLOCK_TYPE = 0x40
        const val EXTERNAL_KIND_FUNCTION = 0
        const val EXTERNAL_KIND_MEMORY = 2
        const val EXTERNAL_KIND_GLOBAL = 3
        const val DATA_MODE_ACTIVE_MEMORY_ZERO = 0
        const val UNREACHABLE = 0x00
        const val NOP = 0x01
        const val BLOCK = 0x02
        const val LOOP = 0x03
        const val IF = 0x04
        const val ELSE = 0x05
        const val END = 0x0b
        const val BR = 0x0c
        const val BR_IF = 0x0d
        const val BR_TABLE = 0x0e
        const val RETURN = 0x0f
        const val CALL = 0x10
        const val CALL_INDIRECT = 0x11
        const val DROP = 0x1a
        const val SELECT = 0x1b
        const val LOCAL_GET = 0x20
        const val LOCAL_SET = 0x21
        const val LOCAL_TEE = 0x22
        const val GLOBAL_GET = 0x23
        const val GLOBAL_SET = 0x24
        const val MEMORY_SIZE = 0x3f
        const val MEMORY_GROW = 0x40
        const val I32_CONST = 0x41
        const val I64_CONST = 0x42
        const val F32_CONST = 0x43
        const val F64_CONST = 0x44
    }
}
