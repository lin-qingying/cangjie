// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

internal import std.core.Object

public open class A {}

public class Integer <: A {
    public init(val: Int64) {}
}

public interface InteropTest<Z> {
    func get(value: Z): A
}

class Child<T> <: InteropTest<T> where T <: A {
    public func get(value: T): T {
        return value
    }
}

main() {
    var interoptest: Child<Integer> = Child<Integer>()
    interoptest.get(Integer(-12345))
    return 0
}
