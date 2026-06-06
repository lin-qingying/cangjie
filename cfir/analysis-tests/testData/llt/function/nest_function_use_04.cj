// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A {
    static func <!sema_unable_to_infer_return_type!>foo<!><T>(a: T) {
        bar<Int64>(1)
        a
    }
}

func bar<T>(a: T) {
    let b = {a: Any => ()}(<!sema_no_match_function_declaration_for_ref!>A<!>.foo<Int64>) // Use type func unknown as reference
    a
}

func baz<T>(a: T): T {
    a
}

main(): Unit {
    bar(1)
}
