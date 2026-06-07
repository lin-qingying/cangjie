// FILE: pkg1/a.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg1

public interface MyInterface {
    static func foo1(): IntNative
    static func foo2<T>(a: IntNative): IntNative {
        foo1()
        return a + foo1()
    }
}

// FILE: pkg2/b.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg2

internal import pkg1.*

public class myClass1<T> <: MyInterface {
    public static var x: IntNative = 20
    public static func f() {
        x = 10
        return x
    }
    public static func foo1(): IntNative {
        f()
    }
}

public class myClass2<T> <: MyInterface {
    public static var x: IntNative = 20
    public static func foo1(): IntNative {
        x = 11
        return x
    }
    public static func foo3() {
        foo2<IntNative>(11)
    }
}

public class myClass3<T> <: MyInterface {
    public static var x: IntNative = 10
    public static func foo1(): IntNative {
        x = 30
        return x
    }
}

// FILE: use_interface_def_imple_005.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkg1.*
internal import pkg2.*

main() {
    print(myClass1<IntNative>.foo2<IntNative>(11).toString())
    print(myClass2<IntNative>.foo2<IntNative>(11).toString())
    print(myClass3<IntNative>.foo2<IntNative>(11).toString())
    print(myClass1<IntNative>.x.toString())
    print(myClass2<IntNative>.x.toString())
    print(myClass3<IntNative>.x.toString())
    if (myClass1<IntNative>.foo2<IntNative>(11) == 21 && myClass2<IntNative>.foo3() == 22 &&
        myClass3<IntNative>.foo2<IntNative>(11) == 41 && myClass1<IntNative>.x == 10 && myClass2<IntNative>.x == 11 &&
        myClass3<IntNative>.x == 30) {
        return 0
    } else {
        return 1
    }
    return 0
}
