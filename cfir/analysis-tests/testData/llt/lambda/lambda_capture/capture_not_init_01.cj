// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    let x: Int64
    let f = <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> <!CAPTURE_BEFORE_INITIALIZATION!>x<!>}<!>
    var y: Int32
    <!USE_FUNC_CAPTURE_VAR_ALONE!>{a: Int32, b: Int32 => a + b + <!CAPTURE_BEFORE_INITIALIZATION!>y<!>}<!>
    return 0
}
