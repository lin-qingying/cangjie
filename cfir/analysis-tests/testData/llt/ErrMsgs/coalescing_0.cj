// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    (Some(<!NO_MATCH_OPERATOR_FUNCTION_CALL!>1<!>(true)) ?? false) + true
    (Some(true) ?? <!CANNOT_CONVERT_LITERAL!>1<!>) * false

    let err = <!NO_MATCH_OPERATOR_FUNCTION_CALL!>1<!>(true)
    err ?? err
    0
}
