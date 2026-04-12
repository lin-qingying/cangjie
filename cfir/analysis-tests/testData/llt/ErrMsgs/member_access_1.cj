// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let err = 1 + false

    err<Bool>
    err<Bool>.e2
    err<Bool>.e2(e3)
    err<Bool>.e2<Bool>
    err<Bool>.e2<Bool>(e3)

    err.e2
    err.e2(e3)
    err.e2<Bool>
    err.e2<Bool>(e3)

    0
}

// Errors:
// ASSERT: scan :10:
// ASSERT: scan :12:
// ASSERT: scan :13:
// ASSERT: scan :14:
// ASSERT: scan :15:
// ASSERT: scan :16:

// No errors:
// ASSERT: scan-not :18:
// ASSERT: scan-not :19:
// ASSERT: scan-not :20:
// ASSERT: scan-not :21:
