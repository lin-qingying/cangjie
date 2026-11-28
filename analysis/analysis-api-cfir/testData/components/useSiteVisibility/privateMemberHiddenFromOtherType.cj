package completion.visibility

class Owner {
    private var secret: Int64 = 0

    func reveal(): Int64 {
        secret
    }
}

class Other {
    func peek(owner: Owner): Int64 {
        owner.secret
    }
}
