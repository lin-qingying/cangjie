// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let err = <!sema_no_match_operator_function_call!>1<!>(false)
    spawn {err} && false
    spawn {err}.x

    spawn {err}
    0
}
