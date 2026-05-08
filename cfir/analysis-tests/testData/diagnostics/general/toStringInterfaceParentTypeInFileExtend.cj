
interface A{

}

class B{

}

extend B <: A{

}

func acceptA(value: A): Unit {
    let _ = value
}

main(): Unit {
    acceptA(B())
}
