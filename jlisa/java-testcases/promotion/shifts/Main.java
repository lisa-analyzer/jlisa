class Main {
  public static void main(String[] args) {
    int k = args.length;
    if (k == 1) { int x = 1; long s = 40; int r = x << s; assert r == 256; }
    else if (k == 2) { int x = 1; long s = 40; long r = x << s; assert r == 256; }
    else if (k == 3) { byte b = 1; long s = 9; int r = b << s; assert r == 512; }
    else if (k == 4) { int x = -16; long s = 34; int r = x >> s; assert r == -4; }
    else if (k == 5) { int x = 1; int s = 40; int r = x << s; assert r == 256; }
  }
}
