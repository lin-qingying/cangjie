
main() {
  for (a in [1] where ([<!INVALID_LOOP_CONTROL!>break<!>, <!INVALID_LOOP_CONTROL!>continue<!>][0])) { }
  return 0
}



