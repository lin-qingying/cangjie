
import stdx.effect.Command

class Get <: Command<Int64> {}

main() {
    try {
        1
    } <!parse_expected_catch_or_finally_in_try!>handle<!>(_: Get) {
        try {
            2
        } catch (_: Exception) {
            "Hello"
        }
    }
<!parse_expected_expr_or_decl_in!>}<!>

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
