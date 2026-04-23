// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

foreign func malloc(size: Int64): CPointer<UInt64>

func getgroups(size: Int32, gidArray: CPointer<UInt32>): Int32 {
    0
}

main() {
    var a: CPointer<UInt64> = unsafe { malloc(4 * 1) }
    var groupNumber = unsafe { getgroups(0, <!TYPE_MISMATCH!>CPointer<UInt64>(a)<!>) }
    var a2: CPointer<UInt64> = unsafe { malloc(4 * Int64(groupNumber)) }
    var cp2: CPointer<UInt64> = CPointer<UInt64>(a2)
    return 0
}
