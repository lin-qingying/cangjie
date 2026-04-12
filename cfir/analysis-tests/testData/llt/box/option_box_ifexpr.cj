// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f
// ASSERT: scan-4 Some(C)
// ASSERT: scan-5 C

main() {
    let a = calc()
    let key1 = if (a == 7) {
        Option<String>.Some("A")
    } else if (a == 9) {
        throw UnsupportedException()
    } else {
        "C"
    }
    key1 |> println

    let key2 = if (a == 7) {
        Option<String>.Some("A")
    } else {
        "C"
    }
    key2 |> println

    let key3 = if (a == 7) {
        throw UnsupportedException()
    } else if (a == 9) {
        Option<String>.Some("A")
    } else {
        "C"
    }
    key3 |> println

    let key4 = if (a == 7) {
        throw UnsupportedException()
    } else {
        "C"
    }
    key4 |> println

    let key5 = if (a == 7) {
        Option<String>.Some("A")
    } else if (a == 8) {
        "C"
    } else {
        throw UnsupportedException()
    }
    key5 |> println
}

func calc() {
    return 8
}
