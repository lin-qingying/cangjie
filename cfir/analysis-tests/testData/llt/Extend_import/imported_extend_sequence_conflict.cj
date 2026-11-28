// IMPORT_PATH: ../../libraries/extend_sequence
// cjc 1.1.3 reports the order conflict on both imported extensions without instantiating Target.
import <!UNUSED_IMPORT!>d7.base.Target<!>
import <!EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE, UNUSED_IMPORT!>d7.one.Middle<!>
import <!UNUSED_IMPORT!>d7.one.Root<!>
import <!EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE, UNUSED_IMPORT!>d7.two.Deep<!>
