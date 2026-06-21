

public class Choir<T> {}

public interface JustExtension {}

extend<T> Choir<T> <: JustExtension {
    public static func just(arr: Array<T>): Choir<T> {
        return Choir<T>()
    }
}

public class Person {}

public func getChoirPerson(p: Person): Choir<Person> {
    return Choir.just(p)
}

main() {}
