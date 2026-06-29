package org.cangnova.cangjie.chir.core.expression

/**
 * CHIR 一元操作的结构化语义。
 *
 * `ChirUnaryExpression.operator` 保留字符串是为了兼容已有测试数据与序列化载荷；
 * 后端和校验器必须通过本枚举解析后再使用，禁止各自维护私有字符串分发表。
 */
enum class ChirUnaryOperator(
    /**
     * 操作的规范名称。
     */
    val canonicalName: String,
    vararg aliases: String,
) {
    INT_NEG("neg", "ineg"),
    FLOAT_NEG("fneg"),
    BIT_NOT("bitnot", "not"),
    LOGICAL_NOT("logical_not", "lnot"),
    IDENTITY("identity", "copy", "mov");

    /**
     * 当前操作可接受的所有规范化名称。
     */
    val acceptedNames: Set<String> = (aliases.toSet() + canonicalName).mapTo(linkedSetOf(), String::canonicalOperationName)

    /**
     * 一元操作解析工具。
     */
    companion object {
        /**
         * 解析原始操作名。
         */
        fun parse(raw: String): ChirUnaryOperator? = byName[raw.canonicalOperationName()]

        /**
         * 解析原始操作名，失败时抛出错误。
         */
        fun require(raw: String): ChirUnaryOperator =
            parse(raw) ?: error("unsupported unary operator '$raw'")

        /**
         * 所有一元操作可接受名称集合。
         */
        val acceptedNames: Set<String> = entries.flatMapTo(linkedSetOf()) { it.acceptedNames }

        /**
         * 规范化名称到一元操作的索引。
         */
        private val byName: Map<String, ChirUnaryOperator> = entries
            .flatMap { operator -> operator.acceptedNames.map { it to operator } }
            .toMap()
    }
}

/**
 * CHIR 二元操作的结构化语义。
 */
enum class ChirBinaryOperator(
    /**
     * 操作的规范名称。
     */
    val canonicalName: String,

    /**
     * 二元操作族。
     */
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

    /**
     * 当前操作可接受的所有规范化名称。
     */
    val acceptedNames: Set<String> = (aliases.toSet() + canonicalName).mapTo(linkedSetOf(), String::canonicalOperationName)

    /**
     * 二元操作解析工具。
     */
    companion object {
        /**
         * 解析原始操作名。
         */
        fun parse(raw: String): ChirBinaryOperator? = byName[raw.canonicalOperationName()]

        /**
         * 解析原始操作名，失败时抛出错误。
         */
        fun require(raw: String): ChirBinaryOperator =
            parse(raw) ?: error("unsupported binary operator '$raw'")

        /**
         * 所有二元操作可接受名称集合。
         */
        val acceptedNames: Set<String> = entries.flatMapTo(linkedSetOf()) { it.acceptedNames }

        /**
         * 规范化名称到二元操作的索引。
         */
        private val byName: Map<String, ChirBinaryOperator> = entries
            .flatMap { operator -> operator.acceptedNames.map { it to operator } }
            .toMap()
    }
}

/**
 * 二元操作族。
 */
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
    /**
     * 操作的规范名称。
     */
    val canonicalName: String,
    vararg aliases: String,
) {
    LOAD("load"),
    STORE("store"),
    ALLOCA("alloca"),
    GET_ELEMENT_PTR("gep", "getelementptr"),
    GET_ELEMENT_PTR_INBOUNDS("getelementptr.inbounds", "getelementptr inbounds");

    /**
     * 当前操作可接受的所有规范化名称。
     */
    val acceptedNames: Set<String> = (aliases.toSet() + canonicalName).mapTo(linkedSetOf(), String::canonicalOperationName)

    /**
     * 内存操作解析工具。
     */
    companion object {
        /**
         * 解析原始操作名。
         */
        fun parse(raw: String): ChirMemoryOperation? = byName[raw.canonicalOperationName()]

        /**
         * 解析原始操作名，失败时抛出错误。
         */
        fun require(raw: String): ChirMemoryOperation =
            parse(raw) ?: error("unsupported memory operation '$raw'")

        /**
         * 所有内存操作可接受名称集合。
         */
        val acceptedNames: Set<String> = entries.flatMapTo(linkedSetOf()) { it.acceptedNames }

        /**
         * 规范化名称到内存操作的索引。
         */
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
    /**
     * 操作的规范名称。
     */
    val canonicalName: String,

    /**
     * 其他操作族。
     */
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

    /**
     * 当前操作可接受的所有规范化名称。
     */
    val acceptedNames: Set<String> = (aliases.toSet() + canonicalName).mapTo(linkedSetOf(), String::canonicalOperationName)

    /**
     * 其他操作解析工具。
     */
    companion object {
        /**
         * 解析原始操作名。
         */
        fun parse(raw: String): ChirOtherOperation? = byName[raw.canonicalOperationName()]

        /**
         * 解析原始操作名，失败时抛出错误。
         */
        fun require(raw: String): ChirOtherOperation =
            parse(raw) ?: error("unsupported other operation '$raw'")

        /**
         * 所有其他操作可接受名称集合。
         */
        val acceptedNames: Set<String> = entries.flatMapTo(linkedSetOf()) { it.acceptedNames }

        /**
         * 规范化名称到其他操作的索引。
         */
        private val byName: Map<String, ChirOtherOperation> = entries
            .flatMap { operation -> operation.acceptedNames.map { it to operation } }
            .toMap()
    }
}

/**
 * 其他表达式操作族。
 */
enum class ChirOtherOperationFamily {
    LLVM_VALUE,
    LLVM_CAST,
    JVM,
}

/**
 * 将操作名规范化为比较用小写形式。
 */
internal fun String.canonicalOperationName(): String = trim().lowercase()
