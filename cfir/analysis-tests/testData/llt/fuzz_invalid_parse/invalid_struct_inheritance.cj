// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was stackoverflow during type substitution. should pass
func test() {
    var a: A<Int64> = A<Int64>()
    a.test(1)
}

interface I<T> {
    func test(a: T): Int64 {
        return 0
    }
}

<!sema_type_implement_non_interface!>s<!>truct A<T> <: A<A<T>> & I<T> {
}

main() {
    var a: A<Int64> = A<Int64>()
    a.test(<!sema_mismatched_types!>a<!>)
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
