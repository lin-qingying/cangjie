// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-PIPE: %run %run_opt %output %run_args 2>&1 | compare %f





foreign func malloc(size: IntNative): CPointer<Int64>

foreign func free(ptr: CPointer<Unit>): Unit
// Test for declaration of array of CString, CPointer
unsafe main(): Int64 {
    var s1 = LibC.mallocCString("123")
    var s2 = LibC.mallocCString("456")
    let a1: Array<CString> = [s1, s2]
    println(a1[0].toString())
    let tmp = a1[1]
    println(tmp.toString())

    var p1 = malloc(8)
    p1.write(789)
    var p2 = CPointer<Int64>()
    let a2: Array<CPointer<Int64>> = [p1, p2]
    println(a2[0].read())
    println(a2[1].isNull())

    // copyTo
    var a3 = Array<CString>(2, {i => LibC.mallocCString("")})
    for (i in a3) {
        LibC.free(i)
    }
    a1.copyTo(a3, 0, 0, 2)
    println(a3[0].toString())
    println(a3[1].toString())

    var a4 = Array<CPointer<Int64>>(2, {i => CPointer<Int64>()})
    a2.copyTo(a4, 0, 0, 2)
    println(a2[0].read())
    println(a2[1].isNull())

    // construct by null CPointer and CString
    var arr1: Array<CPointer<Int64>> = [CPointer<Int64>(), p1]
    var arr2: Array<CString> = [CString(CPointer<UInt8>()), s1]

    for (i in a1) {
        LibC.free(i)
    }
    for (i in a2) {
        free(CPointer<Unit>(i))
    }
    return 0
}

/* SCAN
123
456
789
true
123
456
789
true
*/
