package org.cangnova.cangjie.cfir.serialization.cjo

/** 输入表域互不相通；相同raw下标不表示同一声明、表达式、类型、值或文件。 */
internal enum class CjoSnapshotTableDomain { DECLARATION, EXPRESSION, TYPE, VALUE, FILE }

/**
 * 一次实际装载的表项身份。路径是来源事实，不是快照身份；相同路径的两代包不能共用memo。
 * snapshot采用对象身份比较，防止provider的结构化equals把不同读取事务合并。
 */
internal class CjoSnapshotTableEntry(
    val snapshot: CjoLoadedPackage,
    val domain: CjoSnapshotTableDomain,
    val rawIndex: Int,
) {
    init { require(rawIndex >= 0) }
    override fun equals(other: Any?): Boolean = other is CjoSnapshotTableEntry &&
        snapshot === other.snapshot && domain == other.domain && rawIndex == other.rawIndex
    override fun hashCode(): Int = 31 * (31 * System.identityHashCode(snapshot) + domain.hashCode()) + rawIndex
    override fun toString(): String = "${snapshot.sourcePath}[$domain:$rawIndex]"
}
