package org.cangnova.cangjie.cfir.serialization.cjo

/**
 * `FullId.pkgId` 中保留的特殊包索引。
 *
 * 该约定与官方仓颉编译器 `ASTSerializeUtils.h` 保持一致：
 * - `-1` 表示无效引用
 * - `-2` 表示当前包内声明，需使用 `FullId.index`
 * - `-3` 表示包声明引用，需使用 `FullId.decl` 中的完整包名
 * - `>= 0` 表示导入包数组中的下标，需使用 `FullId.decl` 作为跨包引用键
 */
enum class PackageIndex(val value: Int) {
    INVALID(-1),
    CURRENT(-2),
    PACKAGE_REFERENCE(-3),
    ;

    companion object {
        fun fromValue(value: Int): PackageIndex? = entries.firstOrNull { it.value == value }

        fun isImportedPackage(value: Int): Boolean = value >= 0
    }
}
