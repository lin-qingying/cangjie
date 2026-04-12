// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let err = 1 + false
    let range11 = err..10 : 1
    let range12 = 0..err : 1
    let range13 = 0..10 : err
    let range14 = err..err : 1
    let range15 = 0..err : err
    let range16 = err..err : err

    let range21 = err..=10 : 2
    let range22 = 0..=err : 2
    let range23 = 0..=10 : err
    let range24 = err..=err : 2
    let range25 = 0..=err : err
    let range26 = err..=err : err

    range11 + false
    range11(true)
    0
}

// ASSERT: scan-not :11:
// ASSERT: scan-not :12:
// ASSERT: scan-not :13:
// ASSERT: scan-not :14:
// ASSERT: scan-not :15:
// ASSERT: scan-not :16:

// ASSERT: scan-not :18:
// ASSERT: scan-not :19:
// ASSERT: scan-not :20:
// ASSERT: scan-not :21:
// ASSERT: scan-not :22:

// ASSERT: scan-not :24:
// ASSERT: scan-not :25:
// ASSERT: scan-not :29:

// ASSERT: scan 1 error generated
