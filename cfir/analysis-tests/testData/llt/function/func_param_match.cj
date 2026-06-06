// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

func foo(a: Float32) {
    0
}

main() {
    let a: Float32 = <!sema_cannot_convert_literal!>1<!>
    return foo(<!sema_cannot_convert_literal!>1<!>)
}
