// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: %n.c
// EXEC: %clang %clang_shared_opt %n.c -o %cffi_output
// EXEC: %compiler %verbose_opt %f %cffi-link -o %output
// EXEC: %pwd_to_ld_path %run %output
// ASSERT: regex-auto [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
// ASSERT: regex-auto [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
// ASSERT: regex-auto hello
// ASSERT: regex-auto hello world

class RPtr {
    public var a = CPointer<Int64>()
    public var b: Array<Int64>

    public init(ptr: CPointer<Int64>) {
        a = CPointer<Int64>(ptr)

        b = Array<Int64>(10, {i: Int64 => 0})
        for (i in 0..10) {
            b[i] = unsafe { a.read(i) }
        }
    }

    public func update() {
        for (i in 0..10) {
            b[i] = unsafe { a.read(i) }
        }
    }
}

foreign func ArrayGen(): CPointer<Int64>

foreign func ArrayAdd(a: CPointer<Int64>): CPointer<Int64>

foreign func free(ptr: CPointer<Int64>): Unit

main(): Int64 {
    unsafe {
        var t = RPtr(ArrayGen())
        println(t.b)

        t.a = ArrayAdd(t.a)
        t.update()
        println(t.b)
        free(t.a)
    }
    return 0
}
