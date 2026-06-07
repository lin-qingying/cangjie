// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func foo(): Int32 {
    match (<!parse_unexpected_expected_found!>_<!>) {
        case _ => 0
    }
    return 1
}
