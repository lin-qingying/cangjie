// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func includes(str: String, subString: String): Bool {
    return (str.indexOf(subString) ?? -1) >= 0
}

main() {
    let strArr = ["a"]
    let str = "abc"
    for (s in strArr where !includes(str, s)) {
        return 1
    }
    return 0
}
