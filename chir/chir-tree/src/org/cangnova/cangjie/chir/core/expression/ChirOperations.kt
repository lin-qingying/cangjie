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
    POWER("pow", ChirBinaryOperatorFamily.ARITHMETIC, "**"),
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
    GET_ELEMENT_PTR_INBOUNDS("getelementptr.inbounds", "getelementptr inbounds"),
    GET_ELEMENT_BY_NAME("get.element.by.name"),
    STORE_ELEMENT_BY_NAME("store.element.by.name");

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
    CANGJIE_TYPE_CAST_NA("cangjie.typecast.na", ChirOtherOperationFamily.CANGJIE_SEMANTIC),
    CANGJIE_TYPE_CAST_CHECKED("cangjie.typecast.checked", ChirOtherOperationFamily.CANGJIE_SEMANTIC),
    CANGJIE_TYPE_CAST_WRAPPING("cangjie.typecast.wrapping", ChirOtherOperationFamily.CANGJIE_SEMANTIC),
    CANGJIE_TYPE_CAST_THROWING("cangjie.typecast.throwing", ChirOtherOperationFamily.CANGJIE_SEMANTIC),
    CANGJIE_TYPE_CAST_SATURATING("cangjie.typecast.saturating", ChirOtherOperationFamily.CANGJIE_SEMANTIC),
    PHI("phi", ChirOtherOperationFamily.LLVM_VALUE),
    RAW_ARRAY_ALLOCATE("rawarray.allocate", ChirOtherOperationFamily.LLVM_VALUE),
    RAW_ARRAY_LITERAL_INIT("rawarray.literal.init", ChirOtherOperationFamily.LLVM_VALUE),
    RAW_ARRAY_INIT_BY_VALUE("rawarray.init.by.value", ChirOtherOperationFamily.LLVM_VALUE),
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

    /** 仓颉有溢出策略的类型转换对应的语义策略。 */
    val typeCastOverflowStrategy: ChirOverflowStrategy?
        get() = when (this) {
            CANGJIE_TYPE_CAST_NA -> ChirOverflowStrategy.NOT_APPLICABLE
            CANGJIE_TYPE_CAST_CHECKED -> ChirOverflowStrategy.CHECKED
            CANGJIE_TYPE_CAST_WRAPPING -> ChirOverflowStrategy.WRAPPING
            CANGJIE_TYPE_CAST_THROWING -> ChirOverflowStrategy.THROWING
            CANGJIE_TYPE_CAST_SATURATING -> ChirOverflowStrategy.SATURATING
            else -> null
        }

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
    CANGJIE_SEMANTIC,
    JVM,
}

/** 仓颉 CHIR 类型转换的溢出策略。 */
enum class ChirOverflowStrategy {
    NOT_APPLICABLE,
    CHECKED,
    WRAPPING,
    THROWING,
    SATURATING,
}

/** 官方 CHIR schema 定义的 intrinsic 操作码，保留完整枚举域以支持语义包往返。 */
data class ChirIntrinsicKind(val schemaCode: UShort) {
    /** 供现有操作展示与诊断使用的稳定名称；权威身份始终是 schemaCode。 */
    val canonicalName: String
        get() = "official.intrinsic.$schemaCode"

    companion object {
        /** v1.1.3 与当前 CHIR schema 中 ARRAY_BUILT_IN_COPY_TO 的官方枚举值。 */
        val ARRAY_BUILT_IN_COPY_TO = ChirIntrinsicKind(7u)

        /** v1.1.3 与当前 CHIR schema 中 ARRAY_GET_UNCHECKED 的官方枚举值。 */
        val ARRAY_GET_UNCHECKED = ChirIntrinsicKind(10u)

        /** 官方 CHIR schema 中 RawArray 引用身份比较的 intrinsic 值。 */
        val RAW_ARRAY_REFEQ = ChirIntrinsicKind(41u)

        /** 官方 CHIR schema 中以 32 字节向量比较两个 RawArray 区间的 intrinsic。 */
        val VECTOR_COMPARE_32 = ChirIntrinsicKind(104u)

        /** 官方 CHIR schema 中在 32 字节 RawArray 区间中查找首个匹配字节的 intrinsic。 */
        val VECTOR_INDEX_BYTE_32 = ChirIntrinsicKind(105u)
    }
}

/**
 * 将操作名规范化为比较用小写形式。
 */
internal fun String.canonicalOperationName(): String = trim().lowercase()
