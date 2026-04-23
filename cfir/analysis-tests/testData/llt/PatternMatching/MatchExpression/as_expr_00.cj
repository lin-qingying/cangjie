// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -UNUSED_EXPRESSION

main() {
    let any: Any = 0
    let x = any as Any
    match (x) {
        case Some(_) => 0
        <!UNREACHABLE_PATTERN!>case None => 1<!>
    }
}
