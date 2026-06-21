// FILE: aux_extend_test_a02_24.cj


/*
  @Description:     Defines a package a.c for example.

  @Structure:       complex-aux
*/

package a.b.c

internal import a.b.MyClass

internal interface I1 {
  func test1(){
    return 1
  }
}
public interface I2 {
  func test2(){
    return 2
  }
}
protected interface I3 {
  func test3(){
    return 3
  }
}
private interface I4 {
  func test4(){
    return 4
  }
}
internal interface I5 {
  func test5(){
    return 5
  }
}

public interface I11 {}
public interface I12 {}
protected interface I13 {}
extend Int64 <: I11 & I12 & I13 {}

extend <T> MyClass<T> <: I1 & I2 & I3 & I4 & I5 where T <: I11 & I12 & I13 {

}

// FILE: aux_test_a02_24.cj


/*
  @Description:     Defines a package a.b for example.

  @Structure:       complex-aux
*/

package a.b 

internal class MyClass<T> {}

// FILE: test_a02_24_02.cj


/*
  @Name:            08_06_02_a02_24

  @Assertion:       8.6.2(2) When an interface extension is in a different `package` from the type being extended, whether the interface extension is exported is determined by the smallest access level of the type used in the interface type and the generic constraints (if any). Other `packages` must import the extended type, the corresponding interface, and the type used by the constraint (if any) to access the extended members contained in the corresponding interface.

  @Description:     Direct extensions are defined in different `package` as the extended type
                    Generic,contains multiple generic constraints
                    Extended type modifier private
                    Extend in a subpackage defined by the interface
                    Access extension members in a subpackage of the extended type
                    All constraint types are modified by public/protected
                    Generic constraint type imported at all
                    Extension implements five interfaces, multiple interfaces have different modifiers
                    Interfaces are imported at all

  @Mode:            run

  @Negative:        no

  @Structure:       complex-main

  @Dependencies:    aux_extend_test_a02_24.cj aux_test_a02_24.cj

  @Author:          l60050068

*/

package a.b.c.d

import a.b.c.*


main() {
    var myClassImpl = MyClass<Int64>()
    if (1 != myClassImpl.test1()) {
        return 1
    }
    if (2 != myClassImpl.test2()) {
        return 2
    }
    if (3 != myClassImpl.test3()) {
        return 3
    }
    if (5 != myClassImpl.test5()) {
        return 4
    }
    return 0
}
