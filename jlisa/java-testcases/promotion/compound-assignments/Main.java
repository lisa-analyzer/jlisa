import org.sosy_lab.sv_benchmarks.Verifier;

class Main {
  public static void main(String[] args) {
    int k = Verifier.nondetInt();
    if (k == 0) return;
    else if (k == 1) m1();
    else if (k == 2) m2();
    else if (k == 3) m3();
    else if (k == 4) m4();
    else if (k == 5) m5();
    else if (k == 6) m6();
    else if (k == 7) m7();
    else if (k == 8) m8();
    else if (k == 9) m9();
    else if (k == 10) m10();
    else if (k == 11) m11();
    else if (k == 12) m12();
    else if (k == 13) m13();
    else if (k == 14) m14();
    else if (k == 15) m15();
    else if (k == 16) m16();
    else if (k == 17) m17();
    else if (k == 18) m18();
    else if (k == 19) m19();
    else if (k == 20) m20();
    else if (k == 21) m21();
    else if (k == 22) m22();
    else if (k == 23) m23();
    else if (k == 24) m24();
    else if (k == 25) m25();
    else if (k == 26) m26();
    else if (k == 27) m27();
    else if (k == 28) m28();
    else if (k == 29) m29();
    else if (k == 30) m30();
    else if (k == 31) m31();
    else if (k == 32) m32();
    else if (k == 33) m33();
  }
  static void m1() { char x = 65535; char y = 2; x += y; assert x == 1; } // char += T
  static void m2() { char x = 65535; char y = 2; x -= y; assert x == 65533; } // char -= T
  static void m3() { char x = 65535; char y = 2; x *= y; assert x == 65534; } // char *= T
  static void m4() { char x = 65535; char y = 2; x /= y; assert x == 32767; } // char /= T
  static void m5() { char x = 65535; char y = 2; x %= y; assert x == 1; } // char %= T
  static void m6() { char x = 65535; char y = 2; x &= y; assert x == 2; } // char &= T
  static void m7() { char x = 65535; char y = 2; x |= y; assert x == 65535; } // char |= T
  static void m8() { char x = 65535; char y = 2; x ^= y; assert x == 65533; } // char ^= T
  static void m9() { char x = 65535; char y = 2; x <<= y; assert x == 65532; } // char <<= T
  static void m10() { char x = 65535; char y = 2; x >>= y; assert x == 16383; } // char >>= T
  static void m11() { char x = 65535; char y = 2; x >>>= y; assert x == 16383; } // char >>>= T
  static void m12() { byte x = (byte) 127; byte y = (byte) 2; x += y; assert x == -127; } // byte += T
  static void m13() { byte x = (byte) 127; byte y = (byte) 2; x -= y; assert x == 125; } // byte -= T
  static void m14() { byte x = (byte) 127; byte y = (byte) 2; x *= y; assert x == -2; } // byte *= T
  static void m15() { byte x = (byte) 127; byte y = (byte) 2; x /= y; assert x == 63; } // byte /= T
  static void m16() { byte x = (byte) 127; byte y = (byte) 2; x %= y; assert x == 1; } // byte %= T
  static void m17() { byte x = (byte) 127; byte y = (byte) 2; x &= y; assert x == 2; } // byte &= T
  static void m18() { byte x = (byte) 127; byte y = (byte) 2; x |= y; assert x == 127; } // byte |= T
  static void m19() { byte x = (byte) 127; byte y = (byte) 2; x ^= y; assert x == 125; } // byte ^= T
  static void m20() { byte x = (byte) 127; byte y = (byte) 2; x <<= y; assert x == -4; } // byte <<= T
  static void m21() { byte x = (byte) 127; byte y = (byte) 2; x >>= y; assert x == 31; } // byte >>= T
  static void m22() { byte x = (byte) 127; byte y = (byte) 2; x >>>= y; assert x == 31; } // byte >>>= T
  static void m23() { short x = (short) 32767; short y = (short) 2; x += y; assert x == -32767; } // short += T
  static void m24() { short x = (short) 32767; short y = (short) 2; x -= y; assert x == 32765; } // short -= T
  static void m25() { short x = (short) 32767; short y = (short) 2; x *= y; assert x == -2; } // short *= T
  static void m26() { short x = (short) 32767; short y = (short) 2; x /= y; assert x == 16383; } // short /= T
  static void m27() { short x = (short) 32767; short y = (short) 2; x %= y; assert x == 1; } // short %= T
  static void m28() { short x = (short) 32767; short y = (short) 2; x &= y; assert x == 2; } // short &= T
  static void m29() { short x = (short) 32767; short y = (short) 2; x |= y; assert x == 32767; } // short |= T
  static void m30() { short x = (short) 32767; short y = (short) 2; x ^= y; assert x == 32765; } // short ^= T
  static void m31() { short x = (short) 32767; short y = (short) 2; x <<= y; assert x == -4; } // short <<= T
  static void m32() { short x = (short) 32767; short y = (short) 2; x >>= y; assert x == 8191; } // short >>= T
  static void m33() { short x = (short) 32767; short y = (short) 2; x >>>= y; assert x == 8191; } // short >>>= T
}
