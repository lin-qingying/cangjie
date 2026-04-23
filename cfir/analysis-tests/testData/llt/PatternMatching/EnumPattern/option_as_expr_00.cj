// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum Option<T> {
    Some(T)
}

main() {
    let x: Int64 = 17
    let y = x as Int64
    match (y) {
        case Some(_) => 0
        case _ => 1
    }
}
