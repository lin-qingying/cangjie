package org.cangnova.cangjie.name

/**
 * 可调用声明的稳定身份。
 *
 * 由包名、可选宿主类名和 callable 名称组成，可额外携带本地声明路径用于调试展示。
 */
class CallableId private constructor(
    /**
     * callable 所在包名。
     */
    val packageName: FqName,
    /**
     * callable 所属类名；顶层 callable 为 null。
     */
    val className: FqName?,
    /**
     * callable 自身名称。
     */
    val callableName: Name,
    /**
     * callable 所属类的 ClassId；顶层或本地 callable 为 null。
     */
    val classId:  ClassId?,
    /**
     * 本地 callable 的路径信息。
     */
    private val pathToLocal: FqName?,
) {
    companion object {
        /**
         * 本地 callable 使用的特殊包名片段。
         */
        private val LOCAL_NAME = SpecialNames.LOCAL
        /**
         * 本地 callable 使用的伪包 FqName。
         */
        val PACKAGE_FQ_NAME_FOR_LOCAL: FqName = FqName.topLevel(LOCAL_NAME)

        /**
         * 根据包名和类名计算宿主 ClassId。
         */
        private fun calculateClassId(packageName: FqName, className: FqName?): ClassId? =
            className?.let { ClassId(packageName, it, ) }
    }

    /**
     * 是否表示本地 callable。
     */
    val isLocal: Boolean
        get() = packageName == PACKAGE_FQ_NAME_FOR_LOCAL

    constructor(
        packageName: FqName,
        className: FqName?,
        callableName: Name,
    ) : this(packageName, className, callableName, calculateClassId(packageName, className), pathToLocal = null)

    constructor(
        packageName: FqName,
        className: FqName?,
        callableName: Name,
        pathToLocal: FqName?,
    ) : this(packageName, className, callableName, calculateClassId(packageName, className), pathToLocal)

    constructor(classId: ClassId, callableName: Name) :
        this(classId.packageFqName, classId.relativeClassName, callableName, classId, pathToLocal = null)

    constructor(packageName: FqName, callableName: Name) :
        this(packageName, className = null, callableName, classId = null, pathToLocal = null)

    constructor(
        callableName: Name,
        pathToLocal: FqName?,
    ) : this(PACKAGE_FQ_NAME_FOR_LOCAL, className = null, callableName, classId = null, pathToLocal)

    constructor(callableName: Name) :
        this(PACKAGE_FQ_NAME_FOR_LOCAL, className = null, callableName, classId = null, pathToLocal = null)

    /**
     * 返回用于调试信息的完整限定名，本地 callable 优先保留本地路径。
     */
    fun asFqNameForDebugInfo(): FqName {
        pathToLocal?.child(callableName)?.let { return it }
        return asSingleFqName()
    }

    /**
     * 将 callable 身份展平成单个 FqName。
     */
    fun asSingleFqName(): FqName =
        classId?.asSingleFqName()?.child(callableName) ?: packageName.child(callableName)

    /**
     * 复制当前身份并替换 callable 名称。
     */
    fun copy(callableName: Name): CallableId =
        CallableId(packageName, className, callableName, classId, pathToLocal)

    /**
     * 按包名、类名和 callable 名称判断相等。
     */
    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is CallableId -> false
        else -> packageName == other.packageName && className == other.className && callableName == other.callableName
    }

    /**
     * 返回包名、类名和 callable 名称组合后的哈希值。
     */
    override fun hashCode(): Int {
        var result = 17
        result = result * 31 + packageName.hashCode()
        result = result * 31 + className.hashCode()
        result = result * 31 + callableName.hashCode()
        return result
    }

    /**
     * 渲染为 `package/path/Class.callable` 风格的调试字符串。
     */
    override fun toString(): String = buildString {
        append(packageName.asString().replace('.', '/'))
        append("/")
        if (className != null) {
            append(className)
            append(".")
        }
        append(callableName)
    }
}
