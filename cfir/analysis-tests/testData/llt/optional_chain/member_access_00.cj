// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct A {
    let b: Int64 = 0
}

main() {
    let a = Some(A())
    let x = a?.b
    match (x) {
        case Some(t) => t
        case None => 1
    }
}
