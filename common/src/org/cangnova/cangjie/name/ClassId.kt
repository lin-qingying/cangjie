package org.cangnova.cangjie.name

interface IClassId {
    val packageFqName: FqName
    val relativeClassName: FqName
    val isLocal: Boolean
    val shortClassName: Name
    fun asSingleFqName(): FqName
    val outerClassId: ClassId?
}

data class ClassId(
    override val packageFqName: FqName,
    override val relativeClassName: FqName,
    override val isLocal: Boolean,
) : IClassId {
    constructor(packageFqName: FqName, topLevelName: Name) : this(
        packageFqName,
        FqName.topLevel(topLevelName),
        isLocal = false,
    )

    init {
        assert(!relativeClassName.isRoot) {
            "Class name must not be root: " + packageFqName + if (isLocal) " (local)" else ""
        }
    }

    val parentClassId: ClassId?
        get() = if (isNestedClass) ClassId(packageFqName, relativeClassName.parent(), isLocal) else null

    override val shortClassName: Name
        get() = relativeClassName.shortName()

    override val outerClassId: ClassId?
        get() {
            val parent = relativeClassName.parent()
            return if (!parent.isRoot) ClassId(packageFqName, parent, isLocal) else null
        }

    val outermostClassId: ClassId
        get() {
            var name = relativeClassName
            while (!name.parent().isRoot) {
                name = name.parent()
            }
            return ClassId(packageFqName, name, isLocal = false)
        }

    val isNestedClass: Boolean
        get() = !relativeClassName.parent().isRoot

    fun createNestedClassId(name: Name): ClassId =
        ClassId(packageFqName, relativeClassName.child(name), isLocal)

    override fun asSingleFqName(): FqName =
        if (packageFqName.isRoot) relativeClassName
        else FqName(packageFqName.asString() + "." + relativeClassName.asString())

    fun startsWith(segment: Name): Boolean = packageFqName.startsWith(segment)

    fun asString(): String =
        if (packageFqName.isRoot) {
            relativeClassName.asString()
        } else {
            buildString {
                append(packageFqName.asString().replace('.', '/'))
                append("/")
                append(relativeClassName.asString())
            }
        }

    fun asFqNameString(): String =
        if (packageFqName.isRoot) {
            relativeClassName.asString()
        } else {
            buildString {
                append(packageFqName.asString())
                append(".")
                append(relativeClassName.asString())
            }
        }

    override fun equals(other: Any?): Boolean {
        if (other !is ClassId) return false
        return other.packageFqName == packageFqName && other.relativeClassName == relativeClassName
    }

    override fun toString(): String =
        if (packageFqName.isRoot) "/" + asString() else asString()

    override fun hashCode(): Int {
        var result = packageFqName.hashCode()
        result = 31 * result + relativeClassName.hashCode()
        return result
    }

    companion object {
        @JvmStatic
        fun topLevel(topLevelFqName: FqName): ClassId =
            ClassId(topLevelFqName.parent(), topLevelFqName.shortName())

        @JvmOverloads
        @JvmStatic
        fun fromString(string: String, isLocal: Boolean = false): ClassId {
            val lastSlashIndex = string.lastIndexOf("/")
            val packageName: String
            val className: String
            if (lastSlashIndex == -1) {
                packageName = ""
                className = string
            } else {
                packageName = string.substring(0, lastSlashIndex).replace('/', '.')
                className = string.substring(lastSlashIndex + 1)
            }
            return ClassId(FqName(packageName), FqName(className), isLocal)
        }
    }
}

fun FqName.toClassId(): ClassId = ClassId.topLevel(this)
