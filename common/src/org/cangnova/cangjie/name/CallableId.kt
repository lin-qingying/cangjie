package org.cangnova.cangjie.name

class CallableId private constructor(
    val packageName: FqName,
    val className: FqName?,
    val callableName: Name,
    val classId:  ClassId?,
    private val pathToLocal: FqName?,
) {
    companion object {
        private val LOCAL_NAME = SpecialNames.LOCAL
        val PACKAGE_FQ_NAME_FOR_LOCAL: FqName = FqName.topLevel(LOCAL_NAME)

        private fun calculateClassId(packageName: FqName, className: FqName?): ClassId? =
            className?.let { ClassId(packageName, it, ) }
    }

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

    fun asFqNameForDebugInfo(): FqName {
        pathToLocal?.child(callableName)?.let { return it }
        return asSingleFqName()
    }

    fun asSingleFqName(): FqName =
        classId?.asSingleFqName()?.child(callableName) ?: packageName.child(callableName)

    fun copy(callableName: Name): CallableId =
        CallableId(packageName, className, callableName, classId, pathToLocal)

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is CallableId -> false
        else -> packageName == other.packageName && className == other.className && callableName == other.callableName
    }

    override fun hashCode(): Int {
        var result = 17
        result = result * 31 + packageName.hashCode()
        result = result * 31 + className.hashCode()
        result = result * 31 + callableName.hashCode()
        return result
    }

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
