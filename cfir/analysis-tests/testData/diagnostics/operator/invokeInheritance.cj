// invoke 操作符 —— 继承场景

open class Animal {
    public operator func ()(): Unit {
        return
    }
}

class Dog <: Animal {}

class Cat <: Animal {
    // 子类覆盖父类的 invoke
    public override operator func ()(): Unit {
        return
    }
}

open class EmptyAnimal {}
class Puppy <: EmptyAnimal {}

func testInheritedInvoke(): Unit {
    let dog = Dog()
    dog()  // OK：从 Animal 继承了 invoke

    let cat = Cat()
    cat()  // OK：覆盖了父类的 invoke
}

func testNoInheritedInvoke(): Unit {
    let p = Puppy()
    <!NO_MATCHING_OPERATOR_INVOKE!>p<!>()  // 错误：EmptyAnimal 和 Puppy 都没有 invoke
}
