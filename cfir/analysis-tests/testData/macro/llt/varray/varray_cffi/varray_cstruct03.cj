// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


@C
struct S {
    var a: VArray<Int32, $3> = VArray<Int32, $3>(repeat: 1)
    var c: VArray<Int32, $2> = VArray<Int32, $2>(repeat: 2)
    var e: VArray<Int32, $0> = VArray<Int32, $0>(repeat: 4)
    var d: VArray<Int32, $4> = VArray<Int32, $4>(repeat: 3)
}

foreign func f(s: S): Int32

main() {
    var s = S()
    return unsafe { f(s) }
}
