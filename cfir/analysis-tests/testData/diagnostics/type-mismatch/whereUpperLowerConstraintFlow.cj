// RUN_PIPELINE_TILL: FRONTEND
interface Animal {
    func sound(): String
}

class Dog <: Animal {
    public func sound(): String {
        return "woof"
    }
}

func keepAnimal<T>(value: T): T where T <: Animal {
    return value
}

func useKeepAnimal(): String {
    let animal: Animal = keepAnimal(Dog())
    let dog = keepAnimal(Dog())

    animal.sound()
    let secondSound = dog.sound()
    return secondSound
}
