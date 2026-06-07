// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum E {
    A | B(Int64)
}

main() {
    let arr = [A, B(0)]
    <!sema_forin_pattern_must_be_irrefutable!>f<!>or (<!ENUM_PATTERN_PARAM_SIZE_ERROR!>E<!>.A(x) in arr) {}
}
