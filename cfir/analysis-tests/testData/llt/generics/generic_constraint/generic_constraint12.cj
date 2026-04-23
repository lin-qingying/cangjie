// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

var ff: (Int64) -> Any = {v => ()}

func test<T>(f: (Int64) -> T) where T <: ToString {
    ff = <!TYPE_MISMATCH!>f<!>
}

main() {
    println(ff(1) is Unit)
    test<Bool>({v => false})
    let v = ff(1)
    println(v is Unit)
    println(v is Bool)
}
