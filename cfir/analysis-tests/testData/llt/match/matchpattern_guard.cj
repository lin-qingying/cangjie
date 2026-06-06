// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    print(case01(<!sema_cannot_convert_literal!>1<!>).toString())
    case02(<!sema_cannot_convert_literal!>1<!>)
    return 0
}

func case01(x: Float32) {
    let a: Float32 = <!sema_cannot_convert_literal!>1<!>
    let b: Float32 = <!sema_cannot_convert_literal!>0<!>
    var result: Int64 = match (<!sema_nonexhuastive_patterns!>Year(x)<!>) {
        case _ where false => 1
    }
    return result
}

func case02(x: Float32) {
    let a: Float32 = <!sema_cannot_convert_literal!>1<!>
    let b: Float32 = <!sema_cannot_convert_literal!>0<!>
    var result: Int64 = match (Year(x)) {
        case _ where x <!sema_invalid_binary_expr!>!=<!> 1 => 1
    }
    return result
}

enum TimeUnit {
    Year(Float32)
}
