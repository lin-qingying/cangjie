// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct S0 {
    var t0 = 0
    var t1 = {=> <!CANNOT_MODIFY_VAR!>t0<!> += 1}
}

main() {
    let s0 = S0()
    s0.t0
}
