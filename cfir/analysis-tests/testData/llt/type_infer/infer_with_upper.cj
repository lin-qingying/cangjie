// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %diag_json %f 2>&1 | compare %f

interface I<T> {}

class A<T> where T <: I<T> {
    init() {}
}

struct B<T> where T <: I<T> {
    init() {}
}

func test<T>() where T <: I<T> {
    // test for error report for this 'A()'
    return A()
}

func test2<T>() where T <: I<T> {
    // test for error report for join of return type
    if (true) {
        return B<Int64>()
    } else {
        return ()
    }
}

extend Int64 <: I<Int64> {}

main() {
    test<Int64>()
    return 0
}

// ASSERT: scan-1 sema_unable_to_infer_generic_func
// ASSERT: scan "Errors": 1
