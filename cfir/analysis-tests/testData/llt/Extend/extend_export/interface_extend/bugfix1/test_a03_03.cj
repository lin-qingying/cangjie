// FILE: aux_extend_test_a03_01.cj


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

// FILE: aux_test_a03_01.cj


/*
  @Description:     Defines a package a for example.

  @Structure:       complex-aux
*/

package a 

public class MyClass {}

// FILE: test_a03_03.cj

package c

import b.{MyClass,I1}

main() {
    var myClassImpl = MyClass()
    myClassImpl.test1()
    myClassImpl.<!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>test2<!>()
}

