// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output | compare %f
// ASSERT: scan 1
// ASSERT: scan a

main() {
    var a: Array<Any> = [1, "a"]

    for (x in a) {
        match (x) {
            case y: Int64 => println(y)
            case y: String => println(y)
            case _ => println("wildcard")
        }
    }
}
