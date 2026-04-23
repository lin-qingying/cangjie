// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class Input {}

main() {
    let n: Int64 = 1
    let cond = false
    var input = Input()
    <!CANNOT_ASSIGN_TO_SUBSCRIPT!>input[0]<!> = 1
}
