// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output | compare %f


main() {
    var a: Array<Option<Int64>> = [1, 2, 3]

    match (a[1]) {
        case Some(2) => println("OK")
        case _ => println("Error")
    }
}

/* SCAN
OK
*/
