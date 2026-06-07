// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


@C
struct CStruct {
    var c: Int8 = 1 // 1 byte
    var g: VArray<Int32, $3> = [111, 222, 333] // 12 byte
}

foreign func f(s: CStruct): Int32

foreign func size_of_CStruct(): UIntNative

main() {
    var s = CStruct()
    unsafe {
        f(s)
        if (size_of_CStruct() == sizeOf<CStruct>()) {
            return 0
        }
    }
    return 1;
}
