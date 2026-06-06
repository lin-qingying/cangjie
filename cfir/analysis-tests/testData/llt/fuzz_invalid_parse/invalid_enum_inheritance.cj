// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was stackoverflow during type substitution. should pass
func test() {
    EE<Int64>.test(1)
}

interface I<T> {
    func test(a: T): Int64 {
        return 0
    }
}

enum E<T> <: <!parse_decl_cannot_inherit_their_self!>E<E<T>><!> & I<T> {
    EE
}

main() {
    EE.test(1)
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
