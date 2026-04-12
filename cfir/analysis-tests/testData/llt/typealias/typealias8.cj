// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
// EXEC: %run %run_opt %n.%suffix %run_args

type A<T> = Cl<T>

open class Base {}

class Cl<U> where U <: Base {}

class Sub<X> where X <: Base {
    public var a: A<X>
    public init(a: A<X>) {
        this.a = a
    }
}

main() {
    return 0;
}
