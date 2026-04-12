// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    var err = 1 + false

    match (err) {
        case x => 1 + false
        case y => 1 + true
        case z => err
        case phi => phi
    }

    0
}

// Errors:
// ASSERT: scan :10:

// No errors:
// ASSERT: scan-not :12:
// ASSERT: scan-not :13:
// ASSERT: scan-not :14:
// ASSERT: scan-not :15:
// ASSERT: scan-not :16:
