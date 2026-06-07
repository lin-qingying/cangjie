// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


enum TokenID {
    ADD
    | MINUS
    | MUL
    | DIVIDE
}
main():Int64 {
    var isbinary : Bool = match (id) {
        case <!parse_expected_pattern!>.<!>ADD => true
        case _ => false
    }
    return 0
}
