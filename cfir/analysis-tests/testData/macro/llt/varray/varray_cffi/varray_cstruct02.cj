// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


foreign func printS(s: S): Unit

@C
struct S {
    var a = VArray<VArray<VArray<VArray<Int32, $2>, $2>, $2>, $2>(repeat: [[[1, 2], [1, 2]], [[1, 2], [1, 2]]])
    var b = VArray<Int32, $3>(repeat: 3)
}

main() {
    var s: S = S()
    unsafe { printS(s) }
}
