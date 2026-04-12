// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args
// ASSERT: scan try
// ASSERT: scan finally

internal import std.fs.*
internal import std.io.*

enum Option<T> {
    Some(T)
}

main() {
    try (input = File("option_try_00.cj", Read)) {
        let data = readToEnd(input)
        println("try")
    } catch (e: FSException) {
        println("catch")
    } finally {
        println("finally")
    }
}
