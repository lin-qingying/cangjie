package org.cangnova.cangjie.chir.core.expression

/**
 * CHIR 一元操作的结构化语义。
 *
 * `ChirUnaryExpression.operator` 保留字符串是为了兼容已有测试数据与序列化载荷；
 * 后端和校验器必须通过本枚举解析后再使用，禁止各自维护私有字符串分发表。
 */
enum class ChirUnaryOperator(
    val canonicalName: String,
    vararg aliases: String,
) {
    INT_NEG("neg", "ineg"),
    FLOAT_NEG("fneg"),
    BIT_NOT("bitnot", "not"),
    LOGICAL_NOT("logical_not", "lnot"),
    IDENTITY("identity", "copy", "mov");

    val acceptedNames: Set<String> = (aliases.toSet() + canonicalName).mapTo(linkedSetOf(), String::canonicalOperationName)

    companion object {
        fun parse(raw: String): ChirUnaryOperator? = byName[raw.canonicalOperationName()]
        fun require(raw: String): ChirUnaryOperator =
            parse(raw) ?: error("unsupported unary operator '$raw'")

        val acceptedNames: Set<String> = entries.flatMapTo(linkedSetOf()) { it.acceptedNames }

        private val byName: Map<String, ChirUnaryOperator> = entries
            .flatMap { operator -> operator.acceptedNames.map { it to operator } }
            .toMap()
    }
}

/**
 * CHIR 二元操作的结构化语义。
 */
enum class ChirBinaryOperator(
    val canonicalName: String,
    val family: ChirBinaryOperatorFamily,
    vararg aliases: String,
) {
    ADD("add", ChirBinaryOperatorFamily.ARITHMETIC, "+", "plus"),
    SUB("sub", ChirBinaryOperatorFamily.ARITHMETIC, "-", "minus"),
    MUL("mul", ChirBinaryOperatorFamily.ARITHMETIC, "*", "times"),
    SIGNED_DIV("div", ChirBinaryOperatorFamily.ARITHMETIC, "/"),
    UNSIGNED_DIV("udiv", ChirBinaryOperatorFamily.ARITHMETIC),
    SIGNED_REM("rem", ChirBinaryOperatorFamily.ARITHMETIC, "%"),
    UNSIGNED_REM("urem", ChirBinaryOperatorFamily.ARITHMETIC),
    BIT_AND("and", ChirBinaryOperatorFamily.BITWISE),
    BIT_OR("or", ChirBinaryOperatorFamily.BITWISE),
    BIT_XOR("xor", ChirBinaryOperatorFamily.BITWISE),
    SHIFT_LEFT("shl", ChirBinaryOperatorFamily.SHIFT),
    SIGNED_SHIFT_RIGHT("ashr", ChirBinaryOperatorFamily.SHIFT),
    UNSIGNED_SHIFT_RIGHT("lshr", ChirBinaryOperatorFamily.SHIFT),
    EQUAL("eq", ChirBinaryOperatorFamily.COMPARISON, "=="),
    NOT_EQUAL("ne", ChirBinaryOperatorFamily.COMPARISON, "!="),
    SIGNED_LESS("lt", ChirBinaryOperatorFamily.COMPARISON, "<", "slt"),
    SIGNED_LESS_OR_EQUAL("le", ChirBinaryOperatorFamily.COMPARISON, "<=", "sle"),
    SIGNED_GREATER("gt", ChirBinaryOperatorFamily.COMPARISON, ">", "sgt"),
    SIGNED_GREATER_OR_EQUAL("ge", ChirBinaryOperatorFamily.COMPARISON, ">=", "sge"),
    UNSIGNED_LESS("ult", ChirBinaryOperatorFamily.COMPARISON),
    UNSIGNED_LESS_OR_EQUAL("ule", ChirBinaryOperatorFamily.COMPARISON),
    UNSIGNED_GREATER("ugt", ChirBinaryOperatorFamily.COMPARISON),
    UNSIGNED_GREATER_OR_EQUAL("uge", ChirBinaryOperatorFamily.COMPARISON),
    FLOAT_EQUAL("feq", ChirBinaryOperatorFamily.COMPARISON),
    FLOAT_NOT_EQUAL("fne", ChirBinaryOperatorFamily.COMPARISON),
    FLOAT_LESS("flt", ChirBinaryOperatorFamily.COMPARISON),
    FLOAT_LESS_OR_EQUAL("fle", ChirBinaryOperatorFamily.COMPARISON),
    FLOAT_GREATER("fgt", ChirBinaryOperatorFamily.COMPARISON),
    FLOAT_GREATER_OR_EQUAL("fge", ChirBinaryOperatorFamily.COMPARISON);

    val acceptedNames: Set<String> = (aliases.toSet() + canonicalName).mapTo(linkedSetOf(), String::canonicalOperationName)

    companion object {
        fun parse(raw: String): ChirBinaryOperator? = byName[raw.canonicalOperationName()]
        fun require(raw: String): ChirBinaryOperator =
            parse(raw) ?: error("unsupported binary operator '$raw'")

        val acceptedNames: Set<String> = entries.flatMapTo(linkedSetOf()) { it.acceptedNames }

        private val byName: Map<String, ChirBinaryOperator> = entries
            .flatMap { operator -> operator.acceptedNames.map { it to operator } }
            .toMap()
    }
}

enum class ChirBinaryOperatorFamily {
    ARITHMETIC,
    BITWISE,
    SHIFT,
    COMPARISON,
}

/**
 * CHIR 内存操作的结构化语义。
 */
enum class ChirMemoryOperation(
    val canonicalName: String,
    vararg aliases: String,
) {
    LOAD("load"),
    STORE("store"),
    ALLOCA("alloca"),
    GET_ELEMENT_PTR("gep", "getelementptr"),
    GET_ELEMENT_PTR_INBOUNDS("getelementptr.inbounds", "getelementptr inbounds");

    val acceptedNames: Set<String> = (aliases.toSet() + canonicalName).mapTo(linkedSetOf(), String::canonicalOperationName)

    companion object {
        fun parse(raw: String): ChirMemoryOperation? = byName[raw.canonicalOperationName()]
        fun require(raw: String): ChirMemoryOperation =
            parse(raw) ?: error("unsupported memory operation '$raw'")

        val acceptedNames: Set<String> = entries.flatMapTo(linkedSetOf()) { it.acceptedNames }

        private val byName: Map<String, ChirMemoryOperation> = entries
            .flatMap { operation -> operation.acceptedNames.map { it to operation } }
            .toMap()
    }
}

/**
 * CHIR 其它表达式操作的结构化语义。
 *
 * 这里包含 LLVM lowering 直接消费的操作，也保留 JVM 后端专用操作名用于 CHIR 校验。
 */
enum class ChirOtherOperation(
    val canonicalName: String,
    val family: ChirOtherOperationFamily,
    vararg aliases: String,
) {
    SELECT("select", ChirOtherOperationFamily.LLVM_VALUE),
    BITCAST("bitcast", ChirOtherOperationFamily.LLVM_CAST),
    PTRTOINT("ptrtoint", ChirOtherOperationFamily.LLVM_CAST),
    INTTOPTR("inttoptr", ChirOtherOperationFamily.LLVM_CAST),
    TRUNC("trunc", ChirOtherOperationFamily.LLVM_CAST),
    ZEXT("zext", ChirOtherOperationFamily.LLVM_CAST),
    SEXT("sext", ChirOtherOperationFamily.LLVM_CAST),
    FPTRUNC("fptrunc", ChirOtherOperationFamily.LLVM_CAST),
    FPEXT("fpext", ChirOtherOperationFamily.LLVM_CAST),
    SITOFP("sitofp", ChirOtherOperationFamily.LLVM_CAST),
    UITOFP("uitofp", ChirOtherOperationFamily.LLVM_CAST),
    FPTOSI("fptosi", ChirOtherOperationFamily.LLVM_CAST),
    FPTOUI("fptoui", ChirOtherOperationFamily.LLVM_CAST),
    PHI("phi", ChirOtherOperationFamily.LLVM_VALUE),
    JVM_NEW("jvm.new", ChirOtherOperationFamily.JVM),
    JVM_GET_FIELD("jvm.getfield", ChirOtherOperationFamily.JVM),
    JVM_PUT_FIELD("jvm.putfield", ChirOtherOperationFamily.JVM),
    JVM_GET_STATIC("jvm.getstatic", ChirOtherOperationFamily.JVM),
    JVM_PUT_STATIC("jvm.putstatic", ChirOtherOperationFamily.JVM),
    JVM_NEW_ARRAY("jvm.newarray", ChirOtherOperationFamily.JVM),
    JVM_ARRAY_LOAD("jvm.arrayload", ChirOtherOperationFamily.JVM),
    JVM_ARRAY_STORE("jvm.arraystore", ChirOtherOperationFamily.JVM),
    JVM_ARRAY_LENGTH("jvm.arraylength", ChirOtherOperationFamily.JVM),
    JVM_CHECKCAST("jvm.checkcast", ChirOtherOperationFamily.JVM),
    JVM_INSTANCEOF("jvm.instanceof", ChirOtherOperationFamily.JVM);

    val acceptedNames: Set<String> = (aliases.toSet() + canonicalName).mapTo(linkedSetOf(), String::canonicalOperationName)

    companion object {
        fun parse(raw: String): ChirOtherOperation? = byName[raw.canonicalOperationName()]
        fun require(raw: String): ChirOtherOperation =
            parse(raw) ?: error("unsupported other operation '$raw'")

        val acceptedNames: Set<String> = entries.flatMapTo(linkedSetOf()) { it.acceptedNames }

        private val byName: Map<String, ChirOtherOperation> = entries
            .flatMap { operation -> operation.acceptedNames.map { it to operation } }
            .toMap()
    }
}

enum class ChirOtherOperationFamily {
    LLVM_VALUE,
    LLVM_CAST,
    JVM,
}

internal fun String.canonicalOperationName(): String = trim().lowercase()
