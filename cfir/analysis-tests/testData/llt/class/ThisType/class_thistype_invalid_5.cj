


class R{
  public static func a2() : <!INVALID_POSITION_OF_THIS_TYPE!>This<!>{

  }
}

extend R {
	func foo(): This {
		return this
	}
}

main(): Int64 {
    return 0
}
