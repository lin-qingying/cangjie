package org.cangnova.cangjie.name

/**
 * 编译器内部使用的特殊名称集合。
 *
 * 特殊名称不一定是合法源码标识符，主要用于匿名声明、隐式接收者、构造器和脱糖生成节点。
 */
object SpecialNames {
    /**
     * 没有可用名称时使用的特殊占位名称。
     */
    @JvmField
    val NO_NAME_PROVIDED = Name.special("<no name provided>")

    /**
     * 根包使用的特殊名称。
     */
    @JvmField
    val ROOT_PACKAGE = Name.special("<root package>")

    /**
     * 匿名参数名称的稳定前缀。
     */
    private const val ANONYMOUS_PARAMETER_NAME_PREFIX = "anonymous parameter"

    /**
     * 伴生对象默认使用的普通标识符名称。
     */
    @JvmField
    val DEFAULT_NAME_FOR_COMPANION_OBJECT: Name = Name.identifier("Companion")

    /**
     * 根据位置生成匿名参数的特殊名称。
     */
    @JvmStatic
    fun anonymousParameterName(index: Int): Name =
        Name.special("<$ANONYMOUS_PARAMETER_NAME_PREFIX $index>")

    /**
     * 在 PSI 层需要普通标识符但原名称缺失时使用的安全占位名称。
     */
    @JvmField
    val SAFE_IDENTIFIER_FOR_NO_NAME: Name =
        Name.identifier("no_name_in_PSI_3d19d79d_1ba9_4cd0_b7f5_b46aa3cd5d40")

    /**
     * 匿名声明使用的字符串常量。
     */
    const val ANONYMOUS_STRING = "<anonymous>"

    /**
     * 匿名声明使用的特殊名称。
     */
    @JvmField
    val ANONYMOUS = Name.special(ANONYMOUS_STRING)

    /**
     * 匿名声明使用的顶层 FqName。
     */
    @JvmField
    val ANONYMOUS_FQ_NAME: FqName = FqName.topLevel(Name.special(ANONYMOUS_STRING))

    /**
     * 一元操作中临时值使用的特殊名称。
     */
    @JvmField
    val UNARY = Name.special("<unary>")

    /**
     * this 接收者使用的特殊名称。
     */
    @JvmField
    val THIS = Name.special("<this>")

    /**
     * 初始化结束标记使用的特殊名称。
     */
    @JvmField
    val END_INIT = Name.special("<~init>")

    /**
     * 构造器初始化入口使用的特殊名称。
     */
    @JvmField
    val INIT = Name.special("<init>")

    /**
     * 迭代器临时变量使用的特殊名称。
     */
    @JvmField
    val ITERATOR = Name.special("<iterator>")

    /**
     * 解构声明临时块使用的特殊名称。
     */
    @JvmField
    val DESTRUCT = Name.special("<destruct>")

    /**
     * 本地声明占位使用的特殊名称。
     */
    @JvmField
    val LOCAL = Name.special("<local>")

    /**
     * 未使用变量下划线占位使用的特殊名称。
     */
    @JvmField
    val UNDERSCORE_FOR_UNUSED_VAR = Name.special("<unused var>")

    /**
     * 隐式 setter 参数使用的特殊名称。
     */
    @JvmField
    val IMPLICIT_SET_PARAMETER = Name.special("<set-?>")

    /**
     * 数组字面量或脱糖数组节点使用的特殊名称。
     */
    @JvmField
    val ARRAY = Name.special("<array>")

    /**
     * 接收者参数使用的特殊名称。
     */
    @JvmField
    val RECEIVER = Name.special("<receiver>")

    /**
     * 枚举 entries getter 使用的特殊名称。
     */
    @JvmField
    val ENUM_GET_ENTRIES = Name.special("<get-entries>")

    /**
     * 根据索引生成订阅操作使用的特殊索引名称。
     */
    @JvmStatic
    fun subscribeOperatorIndex(idx: Int): Name {
        require(idx >= 0) { "Index should be non-negative, but was $idx" }
        return Name.special("<index_$idx>")
    }

    /**
     * 将可能为空或特殊的名称转换为可作为标识符使用的安全名称。
     */
    @JvmStatic
    fun safeIdentifier(name: Name?): Name =
        if (name != null && !name.isSpecial) name else SAFE_IDENTIFIER_FOR_NO_NAME

    /**
     * 将可能为空的字符串转换为安全标识符名称。
     */
    @JvmStatic
    fun safeIdentifier(name: String?): Name =
        safeIdentifier(if (name == null) null else Name.identifier(name))

    /**
     * 判断名称是否符合匿名参数特殊名称格式。
     */
    @JvmStatic
    fun isAnonymousParameterName(name: Name): Boolean =
        name.isSpecial && name.asStringStripSpecialMarkers().startsWith(ANONYMOUS_PARAMETER_NAME_PREFIX)

    /**
     * 判断名称是否可安全作为普通标识符使用。
     */
    fun isSafeIdentifier(name: Name): Boolean =
        name.asString().isNotEmpty() && !name.isSpecial
}
