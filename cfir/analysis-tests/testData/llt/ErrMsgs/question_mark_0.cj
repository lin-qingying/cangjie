// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class C {}

main() {
    let err = 1 + false
    let optLam: Option<() -> Unit> = {=>}
    let a = optLam?(1)
    a + 1
    (err)?(1)

    let optTup = Some((1, true))
    let b = optTup?[2]
    b - 2
    (err)?[1]

    let optC = Some(C())
    let c = optC?.x
    c * 3
    (err)?.x

    0
}

// ASSERT: scan-not 15 |
// ASSERT: scan-not 16 |
// ASSERT: scan-not 20 |
// ASSERT: scan-not 21 |
// ASSERT: scan-not 25 |
// ASSERT: scan-not 26 |
// ASSERT: scan 4 errors generated
