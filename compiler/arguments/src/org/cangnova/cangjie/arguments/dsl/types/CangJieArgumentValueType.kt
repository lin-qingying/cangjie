package org.cangnova.cangjie.arguments.dsl.types

import org.cangnova.cangjie.arguments.dsl.base.ExperimentalArgumentApi
import org.cangnova.cangjie.arguments.dsl.base.ReleaseDependent

@ExperimentalArgumentApi
sealed class CangJieArgumentValueType<T> {
    abstract val defaultValue: ReleaseDependent<T?>
    abstract val isNullable: ReleaseDependent<Boolean>
    abstract fun stringRepresentation(value: T?): String?
}

@ExperimentalArgumentApi
data class BooleanType(
    override val defaultValue: ReleaseDependent<Boolean?>,
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false)
) : CangJieArgumentValueType<Boolean>() {
    override fun stringRepresentation(value: Boolean?): String? = value?.toString()
}

@ExperimentalArgumentApi
data class StringType(
    override val defaultValue: ReleaseDependent<String?>,
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true)
) : CangJieArgumentValueType<String>() {
    override fun stringRepresentation(value: String?): String? = value?.let { "\"$it\"" }
}

@ExperimentalArgumentApi
data class StringArrayType(
    override val defaultValue: ReleaseDependent<Array<String>?> = ReleaseDependent(emptyArray()),
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false)
) : CangJieArgumentValueType<Array<String>>() {
    override fun stringRepresentation(value: Array<String>?): String = "emptyArray()"
}

@ExperimentalArgumentApi
data class StringListType(
    override val defaultValue: ReleaseDependent<List<String>?> = ReleaseDependent(emptyList()),
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false)
) : CangJieArgumentValueType<List<String>>() {
    override fun stringRepresentation(value: List<String>?): String? =
        value?.joinToString(", ") { "\"$it\"" }
}

@ExperimentalArgumentApi
data class SystemPathType(
    override val defaultValue: ReleaseDependent<String?> = ReleaseDependent(null),
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true)
) : CangJieArgumentValueType<String>() {
    override fun stringRepresentation(value: String?): String? = value?.let { "\"$it\"" }
}

@ExperimentalArgumentApi
data class LiteralPathType(
    override val defaultValue: ReleaseDependent<Array<String>?> = ReleaseDependent(emptyArray()),
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false)
) : CangJieArgumentValueType<Array<String>>() {
    override fun stringRepresentation(value: Array<String>?): String = "emptyArray()"
}
