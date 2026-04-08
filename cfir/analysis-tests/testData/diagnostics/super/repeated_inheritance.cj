


interface I{
    func name():Unit{
      <!INTERFACE_SUPER_NOT_ALLOWED!>super<!>
    }
}

