open class Animal {
    public open operator func ()(): Unit {
        return
    }
}

class Dog <: Animal {}

class Cat <: Animal {
    public override operator func ()(): Unit {
        return
    }
}

open class EmptyAnimal {}
class Puppy <: EmptyAnimal {}

func testInheritedInvoke(): Unit {
    let dog = Dog()
    dog()

    let cat = Cat()
    cat()
}

func testNoInheritedInvoke(): Unit {
    let p = Puppy()
    <!NO_MATCHING_OPERATOR_INVOKE!>p<!>()
}
