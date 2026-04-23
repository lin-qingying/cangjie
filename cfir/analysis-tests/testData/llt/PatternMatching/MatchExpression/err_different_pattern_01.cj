// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum E {
    e
}

func f(x: Bool) {
    match (x) {
        case <!DIFFERENT_OR_PATTERN!>true | e<!> => 0
    }
}

main() {}
