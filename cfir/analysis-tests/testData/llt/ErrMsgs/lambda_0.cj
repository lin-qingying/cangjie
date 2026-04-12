// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let err = 1 + false

    let lam = {
        =>
        err;
        err
    }
    let lam2: () -> Bool = {
        =>
        err;
        err
    }
    {=> err}()

    0
}

// Errors:
// ASSERT: scan :10:

// No errors:
// ASSERT: scan-not :14:
// ASSERT: scan-not :19:
// ASSERT: scan-not :22:
