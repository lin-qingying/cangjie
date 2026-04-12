// DEPENDENCE: %n.c
// EXEC-PIPE-0: %clang %clang_shared_opt %n.c -o %cffi_output
// EXEC-PIPE-0: %compiler %cmp_opt %cffi-link %f -o %output
// EXEC-PIPE-0: %run %run_opt %output %run_args


var cStr = unsafe {LibC.mallocCString("CANG")}
interface I {
    func foo(): CString {
        return cStr
    }
}

foreign {
    func testfunc(): CPointer<CString>
    func freeMalloced(a: CPointer<CString>) : Unit
}

extend<T> CPointer<T> <: I {
    public func goo(): CString {
        return cStr
    }
}

unsafe main():Int64 {
    var count = 0

    var p1: CPointer<CString> = testfunc()
    var cStr1 = LibC.mallocCString("cang")
    var cStr2 = LibC.mallocCString("jie")
    var cStr3 = LibC.mallocCString("CANG")

    if ( (p1.read(0).equals(cStr1)) && (p1.read(1).equals(cStr2)) ) {
        count += 1
    }

    if (p1.foo().equals(cStr3)) {
        count += 1
    }
    LibC.free(cStr)
    LibC.free(cStr1)
    LibC.free(cStr2)
    LibC.free(cStr3)
    freeMalloced(p1)
    if (count == 2) {
        return 0
    } else {
        return 1
    }
}


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
