// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A<T> {
    public func test(a: <!sema_generic_infinite_instantiation!>A<!><(A<T>, A<T>)>): Bool {
        true
    }
}

main(): Int64 {
    var a: A<Int32> = A<Int32>()
    return 0
}
