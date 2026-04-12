// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

internal import std.core.Object

public interface Comparables<Y> {}

public class Integer <: Comparables<Integer> {
    public init(val: Int64) {}
}

public interface InteropTest<Z> where Z <: Comparables<Z> {
    func test(value1: Z, value2: Z): Unit {}
}

class Child<T> <: InteropTest<T> where T <: Comparables<T> {}

main() {
    var interoptest: Child<Integer> = Child<Integer>()
    interoptest.test(Integer(-12345), Integer(12345))
    return 0
}
