// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface p {
    func j(): Unit
}

class f<ab> <: p {
    public func j() {}
}

main() {
    var ttt = f<Int8>()
    0
}
