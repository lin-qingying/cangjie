// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f<T>(x: Option<T>) {
    match (x) {
        case Some(_) => 0
        case None => 1
        case <!UNREACHABLE_PATTERN!>_: ToString<!> => 2 // unreachable!
    }
}

main() {
    f(Some(0))
}
