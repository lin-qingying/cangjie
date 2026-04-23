// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: m1
// DEPENDENCE: m2

import m1.p1

main() {
    "${p1.<!NOT_MEMBER_OF!>g<!>()}"
    0
}
