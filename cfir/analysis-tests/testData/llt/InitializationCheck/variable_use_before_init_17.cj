// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func f() {
    var x: Int64
    while (true) {
        <!USED_BEFORE_INITIALIZATION!>x<!>++
    }
}

main() {}
