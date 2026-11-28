// IMPORT_PATH: ../../libraries/digest_overload
// 负例守卫：`write(data: Array<Byte>)` 传 `[true]` 必须报类型不匹配，
// 证明“元素期望类型展开别名”没有把类型检查整体放空。
import digestoverload.*

func useBadLiteral(): Unit {
    var sha256 = SHA256()
    sha256.write(<!ARGUMENT_TYPE_MISMATCH!>[true]<!>)
}
