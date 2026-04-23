// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

let (c0, c1) = (f0(), f1())

func f0(): Int64 {
    <!USED_BEFORE_INITIALIZATION!>c1<!>
}

func f1(): Int64 {
    c0
}

main() {
    println("${c0} ${c1}")
}
