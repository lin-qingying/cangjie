// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

main(): Int64 {
    var a = B<Int32>()
    return 0
}

class B<T> {
    <!parse_expected_decl!>operatr<!> func <!parse_expected_name!>+<!>(x:Int32){return 1}
    operator func +(x:T){return 2}
}
