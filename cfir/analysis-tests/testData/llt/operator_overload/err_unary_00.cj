// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func run(): Int64 {
    var s1: String = ""
    var s2: String = "1_23"

    if (!s1.startsWith("") || s1.startsWith("8") || <!OPTIONAL_CHAIN_NON_OPTIONAL!>!s2.startsWith?("")<!>) {
        return 1
    }

    return 0
}

main(): Int64 {
    return run()
}
