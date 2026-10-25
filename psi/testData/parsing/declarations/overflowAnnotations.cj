// 溢出策略注解：方括号形式（策略与注解名解耦）与裸写形式。
// 官方依据：Cangjie 文档《内置编译标记》；cjc 1.0.5 / 1.1.3 零诊断。
@OverflowWrapping[wrapping]
func overflowWrapping(x: UInt8): UInt8 {
    return x + 1u8
}

@OverflowThrowing[saturating]
func overflowSaturating(x: UInt8): UInt8 {
    return x + 1u8
}

@OverflowSaturating
func saturatingBare(x: UInt8): UInt8 {
    return x + 1u8
}

@OverflowThrowing
func throwingBare(x: UInt8): UInt8 {
    return x + 1u8
}
