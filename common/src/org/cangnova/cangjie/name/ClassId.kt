package org.cangnova.cangjie.name


data class ClassId(
    val packageFqName: FqName,
    val relativeClassName: FqName,
) {
    constructor(packageFqName: FqName, topLevelName: Name) : this(
        packageFqName,
        FqName.topLevel(topLevelName),

    )

    init {
        assert(!relativeClassName.isRoot) {
            "Class name must not be root: $packageFqName "
        }
    }

    val parentClassId: ClassId?
        get() = if (isNestedClass) ClassId(packageFqName, relativeClassName.parent()) else null

    val shortClassName: Name
        get() = relativeClassName.shortName()

    val outerClassId: ClassId?
        get() {
            val parent = relativeClassName.parent()
            return if (!parent.isRoot) ClassId(packageFqName, parent) else null
        }

    val outermostClassId: ClassId
        get() {
            var name = relativeClassName
            while (!name.parent().isRoot) {
                name = name.parent()
            }
            return ClassId(packageFqName, name,)
        }

    val isNestedClass: Boolean
        get() = !relativeClassName.parent().isRoot

    fun createNestedClassId(name: Name): ClassId =
        ClassId(packageFqName, relativeClassName.child(name))

    fun asSingleFqName(): FqName =
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


    override fun toString(): String =
        if (packageFqName.isRoot) "/" + asString() else asString()



    companion object {
        @JvmStatic
        fun topLevel(topLevelFqName: FqName): ClassId =
            ClassId(topLevelFqName.parent(), topLevelFqName.shortName())

        @JvmOverloads
        @JvmStatic
        fun fromString(string: String): ClassId {
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
            return ClassId(FqName(packageName), FqName(className),)
        }
    }
}

fun FqName.toClassId(): ClassId = ClassId.topLevel(this)
