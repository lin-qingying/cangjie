// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

public func test(): Option<Int64> {
    None
}

main() {
    var a: Int64
    let b = test()
    match (b) {
        case Some(v) => return 1
        case _ => a = 0
    }
    return a
}
