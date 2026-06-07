// FILE: A.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package A

public class Foo {}

// FILE: B.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package B

internal import A.Foo

public interface I1 {
    func f(): Unit {}
}

extend Foo <: I1 {}

// FILE: C.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package C

internal import A.Foo
internal import B.I1

public interface I2 {
    func f(): Unit {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend Foo <: I2 {}<!>
