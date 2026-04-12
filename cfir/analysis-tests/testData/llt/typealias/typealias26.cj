// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

type str<T> = String

main(): Int64 {
    var str1: str<?Int32> = "hello"
    var str2: String = "Rune"
    var res: str<Int32> = str1 + " " + str2
    if (res == "hello Rune") {
        return 0
    }
    return 1
}
