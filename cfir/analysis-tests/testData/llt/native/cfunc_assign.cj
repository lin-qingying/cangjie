// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %cmp_opt %typecheck %f

// Test for sema check of foreign function assignment.

foreign {
    func cb(a: Int64): Unit

    func ff(a: CFunc<(Int64) -> Unit>): Unit
}

main() {
    let f: CFunc<(Int64) -> Unit> = cb
    unsafe { ff(cb) }
    0
}
