// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


foreign func c_f1(a: CPointer<Int32>): Unit

foreign func c_f2(a: VArray<Int32, $3>): Unit

foreign func c_f3(a: CPointer<VArray<Int32, $2>>): Unit

foreign func c_f4(a: VArray<VArray<Int32, $2>, $2>): Unit

class A {}

main() {
    unsafe {
        var a = VArray<Int32, $3>({i => Int32(i)})
        c_f1(inout a)
        c_f2(inout a)

        var b = VArray<VArray<Int32, $2>, $2>(repeat: [1, 2])
        c_f3(inout b)
        c_f4(inout b)
    }
    0
}
