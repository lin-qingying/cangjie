// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum bbb {
    center | Month | Day | Hour
}

func aaa(a: Int32) {
    0
}

func aaa(ctx!: Int64, callback!: Option<(Int64, Int64) -> Unit> = Option<(Int64, Int64) -> Unit>.None) {}

func ccc(a: Int16): Int16 {
    1
}

func ccc(a: Int32): Int32 {
    1
}

func ccc(a!: Int32, b!: Int32): Int32 {
    1
}

func Test() {
    aaa(
        ctx: 10,
        callback: Option<(Int64, Int64) -> Unit>.Some(
            {
                start: Int64, end: Int64 =>
                let a = Some<bbb>(bbb.<!sema_not_member_of!>cente<!>)
                return
            }
        )
    )

    aaa(<!sema_mismatched_types!>ccc(Int16(1))<!>)
    aaa(ccc(<!sema_cannot_convert_literal!>r<!>'a'))
    aaa(ccc(a: r'a', <!sema_unordered_arguments!>1<!>))
    aaa(ccc(a: r'a', <!sema_multiple_named_argument!>a<!>: 1))
    aaa(ccc(a: <!sema_cannot_convert_literal!>r<!>'a', b: 1))
    return 0
}

main() {
    Test()
    return 10
}
