// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

/*
  @Description:     Defines a package b for example.

  @Structure:       complex-aux
*/

package b

public import a.MyClass

public interface I1 {
  func test1() {}
}

public interface I2 {
  func test2() {}
}

extend MyClass <: I1 & I2 {
    public func test3() {}
    public func myExtendFunc() {
        "MyClass myExtendFunc()"
    }
}