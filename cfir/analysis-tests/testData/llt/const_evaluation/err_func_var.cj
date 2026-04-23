// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

const func f() {
    <!CANNOT_DEFINE_VAR_IN_CONST_FUNCTION!>var x = 0<!>
    x
}

main() {
    f()
}
