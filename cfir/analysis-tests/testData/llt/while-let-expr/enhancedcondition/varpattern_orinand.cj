// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let a = true
    let b = false
    let d = Some(3)
    while (<!VAR_IN_OR_CONDITION!>a || b && let Some(c) <- d<!>) {
        0
    }
    while (<!VAR_IN_OR_CONDITION!>(let Some(c) <- d) && a || b<!>) {
        0
    }
}
