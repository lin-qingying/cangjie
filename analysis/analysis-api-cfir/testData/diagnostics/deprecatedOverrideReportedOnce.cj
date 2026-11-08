package deprecated_override

// 实现弃用的接口成员：官方只报告一次 `DEPRECATION_OVERRIDE_WARNING`（锚在实现声明上）。
// CFIR 的 Common 与 Extra checker 集合若都收录 `CfirDeprecatedDeclarationChecker`，
// `EXTENDED_AND_COMMON_CHECKERS`（两个 filter 的并集）会把同一诊断报告两次。
interface DeprecatedBase {
    @Deprecated
    func legacy(): Int64
}

class DeprecatedImpl <: DeprecatedBase {
    public func legacy(): Int64 {
        return 1
    }
}
