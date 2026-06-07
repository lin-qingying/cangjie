// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    (Some(1 <!INVALID_BINARY_OPERATOR!>+<!> true) ?? <!NO_MATCHING_OPERATOR_INVOKE!>2<!>(false)) + true
    0
}
