// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct A {
    public let a: Int32
    <!sema_class_uninitialized_field!>p<!>ublic A(x: Int32) {
    }
}

<!sema_value_type_recursive!>s<!>truct Data {
    public Data() {}
    public var b = <!RECURSIVE_CONSTRUCTOR_CALL!>Data<!>()
}

<!sema_value_type_recursive!>s<!>truct Data2 {
    public var b: Data2 = <!RECURSIVE_CONSTRUCTOR_CALL!>Data2<!>()
}

struct GenRec {
    public var a: Int64
    public var b: Int32
    public init(x: Int64, y: Int32, str: String) {
        this(x)
        this.b = y
    }
    <!sema_class_uninitialized_field!>p<!>rivate GenRec(x: Int64) {
        this.a = x
    }
}

main(): Int64 {
    var data2 = GenRec(3, 4, "1")
    if (data2.a == 3 && data2.b == 4) {
        return 0
    }
    return 1
}
