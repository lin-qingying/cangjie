package org.cangnova.cangjie.psi.stubs.impl

/**
 * USER_TYPE stub 里记录的声明种类（`.cjo` `SemaTy.kind` 的 stub 层投影）。
 *
 * **条目顺序即序列化格式**（`ordinal` 直接写入 stub 的单个字节），只允许在末尾追加；
 * 插入/重排会让已有持久化索引错位，必须同时递增 `CangJieStubVersions.SOURCE_STUB_VERSION`。
 *
 * 与 `cfir/cfir-tree` 的 `CfirClassKind` 四项同名同序，`CangJieStubClassifierKindConsistencyTest`
 * 守住这层对应关系。
 */
enum class CangJieStubClassifierKind {
    CLASS,
    INTERFACE,
    STRUCT,
    ENUM,
}
