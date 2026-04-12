// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args
interface I {
    static func foo() {
        return 0
    }
}

class D<T> where T <: I {
    public func foo() {
        return T.foo()
    }
}

class C1 <: I {
    public static func foo(a: Int32) {
        return 1
    }
}

main() {
    return D<C1>().foo()
}
