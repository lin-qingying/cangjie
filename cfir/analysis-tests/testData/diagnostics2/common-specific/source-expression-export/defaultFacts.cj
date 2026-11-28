package source_default_facts

public const globalValue: Int64 = 11

public const func fixedValue(): Int64 {
    7
}

// 在 const owner 中验证语义 const 检查与原 assignment.isConst 的区别。
public const func constDefaults(
    literal!: Int64 = 7,
    parenthesized!: Int64 = (7),
    negativeLiteral!: Int64 = -7,
    unaryExpression!: Int64 = -(7),
    compound!: Int64 = 1 + 2,
    compoundParen!: Int64 = (1 + 2),
    invocation!: Int64 = fixedValue(),
    constantReference!: Int64 = globalValue
): Int64 {
    literal + parenthesized + negativeLiteral + unaryExpression + compound + compoundParen + invocation + constantReference
}

// nested 不自行添加 Frozen 注解；由原生 SaveFuncBody 传播 outer 的 frozen 状态。
@Frozen
public func frozenOuter(seed: Int64, offset!: Int64 = 1 + 2): Int64 {
    func frozenNested(value: Int64): Int64 {
        value + offset
    }
    return frozenNested(seed)
}
