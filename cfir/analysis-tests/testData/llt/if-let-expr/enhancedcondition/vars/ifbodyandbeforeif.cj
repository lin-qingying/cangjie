// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f

main() {
    let a = Some(1)
    if (let Some(a) <- a) {
        Some(0)
    } else {
        let b = a
        b
    }
    0
}
