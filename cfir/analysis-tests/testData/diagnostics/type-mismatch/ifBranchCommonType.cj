// RUN_PIPELINE_TILL: FRONTEND
open class Animal {
}

class Dog <: Animal {
}

class Cat <: Animal {
}

func chooseAnimal(flag: Bool): Animal {
    let a: Animal = if (flag) {
        Dog()
    } else {
        Cat()
    }
    return a
}

