// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func test(b: Bool) {
    if (b) {
        0
    } else {
        1
    }
}

main() {
    let b1: Byte = '1'
    let b2: Byte = "2"
    let b3: Byte = '\r'
    let b4: Byte = "\n"
    let b5: Byte
    b5 = '5'
    let b6: Byte
    b6 = "6"

    test(b1 is Byte && b2 is Byte && b3 is Byte && b4 is Byte && b5 is Byte && b6 is Byte)
}
