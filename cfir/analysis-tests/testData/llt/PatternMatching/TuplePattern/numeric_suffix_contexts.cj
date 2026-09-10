public func mixedFloat(): Int64 {
    match ((1.0f32, 2.0)) {
        case <!UNREACHABLE_PATTERN!>(_: Float32, _: Float32)<!> => 0
        case (_: Float32, _: Float64) => 1
        case <!UNREACHABLE_PATTERN!>_<!> => 2
    }
}
public func boxedInteger(): ?UInt8 { 1i32 }
public func boxedFloat(): ?Float32 { 1.0f64 }
public func directInteger(): Int64 { <!CANNOT_CONVERT_LITERAL!>1i32<!> }
public func directFloat(): Float32 { <!CANNOT_CONVERT_LITERAL!>1.0f64<!> }
public func boxedOverflow(): ?UInt8 { <!LITERAL_NUMERIC_OVERFLOW!>256i32<!> }
