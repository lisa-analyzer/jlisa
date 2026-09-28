# `Combining path patterns`

| # | `this` | `other` | Result | Supported by P1 |
|---:|---|---|---|:---:|
| 1 | `""` | `""` | `""` | ✅ |
| 2 | `""` | `/a` | `/a` | ✅ |
| 3 | `/a` | `""` | `/a` | ✅ |
| 4 | `/a` | `null` | throws NullPointerException | ✅ |
| 5 | `/hotels` | `/booking` | `/hotels/booking` | ✅ |
| 6 | `/projects` | `/spring-framework` | `/projects/spring-framework` | ✅ |
| 7 | `/usr` | `/user` | `/usr/user` | ✅ |
| 8 | `/hotels` | `/hotels` | `/hotels/hotels` | ✅ |
| 9 | `/a.html` | `/a.html` | `/a.html/a.html` | ✅ |
| 10 | `/hotels` | `booking` | `/hotels/booking` | ✅ |
| 11 | `/` | `/a` | `/a` | ❌ |
| 12 | `/a` | `/` | `/a/` | ✅ |
| 13 | `/` | `/` | `/` | ❌ |
| 14 | `/projects` | `/{project}` | `/projects/{project}` | ✅ |
| 15 | `/{foo}` | `/bar` | `/{foo}/bar` | ❌ |
| 16 | `/a/{x}` | `/b` | `/a/{x}/b` | ❌ |
| 17 | `/hotels/?` | `/booking` | `/hotels/?/booking` | ❌ |
| 18 | `/projects/*/releases` | `/{id}` | `/projects/*/releases/{id}` | ❌ |
| 19 | `/{x}/*.html` | `/y.pdf` | `/{x}/*.html/y.pdf` | ❌ |
| 20 | `/{id}` | `/{id}` | throws PatternParseException | ❌ |
| 21 | `/*` | `/hotel` | `/hotel` | ✅ |
| 22 | `/*` | `/{project}` | `/{project}` | ❌ |
| 23 | `/*` | `/**` | `/**` | ❌ |
| 24 | `/a/*/c` | `/a/b/c` | `/a/b/c` | ❌ |
| 25 | `/**` | `/booking` | `/booking` | ✅ |
| 26 | `/hotels/*` | `/booking` | `/hotels/booking` | ✅ |
| 27 | `/projects/*` | `/spring-framework` | `/projects/spring-framework` | ✅ |
| 28 | `/hotels/*` | `/booking/rooms` | `/hotels/booking/rooms` | ✅ |
| 29 | `/hotels/*` | `booking` | `/hotels/booking` | ✅ |
| 30 | `/projects/*` | `/{project}` | `/projects/{project}` | ❌ |
| 31 | `/{foo}/*` | `/bar` | `/{foo}/bar` | ✅ |
| 32 | `/*` | `/a/b` | `/a/b` | ✅ |
| 33 | `/*` | `/*` | `/*` | ❌ |
| 34 | `/hotels/**` | `/booking` | `/hotels/booking` | ✅ |
| 35 | `/hotels/**` | `/booking/rooms` | `/hotels/booking/rooms` | ✅ |
| 36 | `/projects/**` | `/*.html` | `/projects/*.html` | ❌ |
| 37 | `/a/**` | `/**` | `/a/**` | ❌ |
| 38 | `/hotels/**` | `/hotels/**` | `/hotels/hotels/**` | ❌ |
| 39 | `/**` | `/**` | throws StringIndexOutOfBoundsException | ❌ |
| 40 | `/{*path}` | `/booking` | throws PatternParseException | ❌ |
| 41 | `/hotels/{*path}` | `/booking` | throws PatternParseException | ❌ |
| 42 | `a.b` | `c` | `a.b.c` | ❌ |
| 43 | `a.*` | `b` | `a.b` | ❌ |
| 44 | `a.**` | `b.c` | `a.b.c` | ❌ |
| 45 | `*.html` | `hotel` | `*.html.hotel` | ❌ |
| 46 | `/*.html` | `/hotel.html` | `/hotel.html` | ❌ |
| 47 | `/*.html` | `/{name}.html` | `/{name}.html` | ❌ |
| 48 | `/*.*` | `/hotel.pdf` | `/hotel.pdf` | ❌ |
| 49 | `/*.*` | `/*.html` | `/*.html` | ❌ |
| 50 | `/*.*` | `/{name:.+}` | `/{name:.+}` | ❌ |
| 51 | `/*.html` | `/hotel` | `/hotel.html` | ❌ |
| 52 | `/*.html` | `/{name}` | `/{name}.html` | ❌ |
| 53 | `/*.html` | `/hotel.*` | `/hotel.html` | ❌ |
| 54 | `/*.html` | `/x{y}.*` | `/x{y}.html` | ❌ |
| 55 | `/hotels/*.html` | `/hotels/x` | `/hotels/x.html` | ❌ |
| 56 | `/projects/*.html` | `/projects/spring.*` | `/projects/spring.html` | ❌ |
| 57 | `/*/*.html` | `/{x}/y.*` | `/{x}/y.html` | ❌ |
| 58 | `/dir/*.*` | `/dir/hotel` | `/dir/hotel` | ❌ |
| 59 | `/*.html` | `/*.*` | `/*.html` | ❌ |
| 60 | `/*.*` | `/*.*` | `/*.*` | ❌ |
| 61 | `/*.html` | `/**` | `/**.html` | ❌ |
| 62 | `/*.html` | `/hotel.pdf` | throws IllegalArgumentException | ❌ |
| 63 | `/*.html` | `/*.pdf` | throws IllegalArgumentException | ❌ |
| 64 | `/*.html` | `/*.html` | throws IllegalArgumentException | ❌ |
| 65 | `/*.html` | `/hotel.html.gz` | throws IllegalArgumentException | ❌ |
| 66 | `/*.html` | `/v1.2/hotel` | throws IllegalArgumentException | ❌ |
| 67 | `/*.html` | `/{name:.*}` | throws IllegalArgumentException | ❌ |
| 68 | `/dir/*.html` | `/hotel` | throws IllegalArgumentException | ❌ |
| 69 | `/projects/*.html` | `/spring-framework.html` | throws IllegalArgumentException | ❌ |
| 70 | `/projects/*.html` | `/spring-framework.*` | throws IllegalArgumentException | ❌ |
| 71 | `/dir/*.*` | `/other/hotel.pdf` | throws IllegalArgumentException | ❌ |
| 72 | `/a/*.html` | `/{x}/y.*` | throws IllegalArgumentException | ❌ |
| 73 | `/*.html` | `/a/b` | throws IllegalArgumentException | ❌ |
| 74 | `/*.html/x` | `/bar` | throws IllegalArgumentException | ❌ |
| 75 | `/projects/*.html/releases` | `/{id}` | throws IllegalArgumentException | ❌ |
| 76 | `/*.html` | `hotel` | throws StringIndexOutOfBoundsException | ❌ |
| 77 | `*.html` | `/hotel` | throws StringIndexOutOfBoundsException | ❌ |
