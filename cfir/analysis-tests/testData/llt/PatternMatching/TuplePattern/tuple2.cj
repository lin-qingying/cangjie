// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    match (1) {
        case <!TUPLE_PATTERN_NOT_MATCH!>(k, v)<!> => 1
        case _ => <!INVALID_LOOP_CONTROL!>continue<!>
    }
    0
}
