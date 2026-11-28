# `Combining path patterns`

Supported by P1: ✅ 77 of 77 (100%)

| # | `this` | `other` | Result | P1 result | Supported by P1 |
|---:|---|---|---|---|:---:|
| 1 | `""` | `""` | `""` | `""` | ✅ |
| 2 | `""` | `/a` | `/a` | `/a` | ✅ |
| 3 | `/a` | `""` | `/a` | `/a` | ✅ |
| 4 | `/a` | `null` | throws NullPointerException | throws NullPointerException | ✅ |
| 5 | `/hotels` | `/booking` | `/hotels/booking` | `/hotels/booking` | ✅ |
| 6 | `/projects` | `/spring-framework` | `/projects/spring-framework` | `/projects/spring-framework` | ✅ |
| 7 | `/usr` | `/user` | `/usr/user` | `/usr/user` | ✅ |
| 8 | `/hotels` | `/hotels` | `/hotels/hotels` | `/hotels/hotels` | ✅ |
| 9 | `/a.html` | `/a.html` | `/a.html/a.html` | `/a.html/a.html` | ✅ |
| 10 | `/hotels` | `booking` | `/hotels/booking` | `/hotels/booking` | ✅ |
| 11 | `/` | `/a` | `/a` | `/a` | ✅ |
| 12 | `/a` | `/` | `/a/` | `/a/` | ✅ |
| 13 | `/` | `/` | `/` | `/` | ✅ |
| 14 | `/projects` | `/{project}` | `/projects/{project}` | `/projects/{project}` | ✅ |
| 15 | `/{foo}` | `/bar` | `/{foo}/bar` | `/{foo}/bar` | ✅ |
| 16 | `/a/{x}` | `/b` | `/a/{x}/b` | `/a/{x}/b` | ✅ |
| 17 | `/hotels/?` | `/booking` | `/hotels/?/booking` | `/hotels/?/booking` | ✅ |
| 18 | `/projects/*/releases` | `/{id}` | `/projects/*/releases/{id}` | `/projects/*/releases/{id}` | ✅ |
| 19 | `/{x}/*.html` | `/y.pdf` | `/{x}/*.html/y.pdf` | `/{x}/*.html/y.pdf` | ✅ |
| 20 | `/{id}` | `/{id}` | throws PatternParseException | throws PathMergeException | ✅ |
| 21 | `/*` | `/hotel` | `/hotel` | `/hotel` | ✅ |
| 22 | `/*` | `/{project}` | `/{project}` | `/{project}` | ✅ |
| 23 | `/*` | `/**` | `/**` | `/**` | ✅ |
| 24 | `/a/*/c` | `/a/b/c` | `/a/b/c` | `/a/b/c` | ✅ |
| 25 | `/**` | `/booking` | `/booking` | `/booking` | ✅ |
| 26 | `/hotels/*` | `/booking` | `/hotels/booking` | `/hotels/booking` | ✅ |
| 27 | `/projects/*` | `/spring-framework` | `/projects/spring-framework` | `/projects/spring-framework` | ✅ |
| 28 | `/hotels/*` | `/booking/rooms` | `/hotels/booking/rooms` | `/hotels/booking/rooms` | ✅ |
| 29 | `/hotels/*` | `booking` | `/hotels/booking` | `/hotels/booking` | ✅ |
| 30 | `/projects/*` | `/{project}` | `/projects/{project}` | `/projects/{project}` | ✅ |
| 31 | `/{foo}/*` | `/bar` | `/{foo}/bar` | `/{foo}/bar` | ✅ |
| 32 | `/*` | `/a/b` | `/a/b` | `/a/b` | ✅ |
| 33 | `/*` | `/*` | `/*` | `/*` | ✅ |
| 34 | `/hotels/**` | `/booking` | `/hotels/booking` | `/hotels/booking` | ✅ |
| 35 | `/hotels/**` | `/booking/rooms` | `/hotels/booking/rooms` | `/hotels/booking/rooms` | ✅ |
| 36 | `/projects/**` | `/*.html` | `/projects/*.html` | `/projects/*.html` | ✅ |
| 37 | `/a/**` | `/**` | `/a/**` | `/a/**` | ✅ |
| 38 | `/hotels/**` | `/hotels/**` | `/hotels/hotels/**` | `/hotels/hotels/**` | ✅ |
| 39 | `/**` | `/**` | throws StringIndexOutOfBoundsException | throws PathMergeException | ✅ |
| 40 | `/{*path}` | `/booking` | throws PatternParseException | throws PathMergeException | ✅ |
| 41 | `/hotels/{*path}` | `/booking` | throws PatternParseException | throws PathMergeException | ✅ |
| 42 | `a.b` | `c` | `a.b/c` | `a.b/c` | ✅ |
| 43 | `a.*` | `b` | `a.*/b` | `a.*/b` | ✅ |
| 44 | `a.**` | `b.c` | `a.**/b.c` | `a.**/b.c` | ✅ |
| 45 | `*.html` | `hotel` | throws StringIndexOutOfBoundsException | throws PathMergeException | ✅ |
| 46 | `/*.html` | `/hotel.html` | `/hotel.html` | `/hotel.html` | ✅ |
| 47 | `/*.html` | `/{name}.html` | `/{name}.html` | `/{name}.html` | ✅ |
| 48 | `/*.*` | `/hotel.pdf` | `/hotel.pdf` | `/hotel.pdf` | ✅ |
| 49 | `/*.*` | `/*.html` | `/*.html` | `/*.html` | ✅ |
| 50 | `/*.*` | `/{name:.+}` | `/{name:.+}` | `/{name:.+}` | ✅ |
| 51 | `/*.html` | `/hotel` | `/hotel.html` | `/hotel.html` | ✅ |
| 52 | `/*.html` | `/{name}` | `/{name}.html` | `/{name}.html` | ✅ |
| 53 | `/*.html` | `/hotel.*` | `/hotel.html` | `/hotel.html` | ✅ |
| 54 | `/*.html` | `/x{y}.*` | `/x{y}.html` | `/x{y}.html` | ✅ |
| 55 | `/hotels/*.html` | `/hotels/x` | `/hotels/x.html` | `/hotels/x.html` | ✅ |
| 56 | `/projects/*.html` | `/projects/spring.*` | `/projects/spring.html` | `/projects/spring.html` | ✅ |
| 57 | `/*/*.html` | `/{x}/y.*` | `/{x}/y.html` | `/{x}/y.html` | ✅ |
| 58 | `/dir/*.*` | `/dir/hotel` | `/dir/hotel` | `/dir/hotel` | ✅ |
| 59 | `/*.html` | `/*.*` | `/*.html` | `/*.html` | ✅ |
| 60 | `/*.*` | `/*.*` | `/*.*` | `/*.*` | ✅ |
| 61 | `/*.html` | `/**` | `/**.html` | `/**.html` | ✅ |
| 62 | `/*.html` | `/hotel.pdf` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 63 | `/*.html` | `/*.pdf` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 64 | `/*.html` | `/*.html` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 65 | `/*.html` | `/hotel.html.gz` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 66 | `/*.html` | `/v1.2/hotel` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 67 | `/*.html` | `/{name:.*}` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 68 | `/dir/*.html` | `/hotel` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 69 | `/projects/*.html` | `/spring-framework.html` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 70 | `/projects/*.html` | `/spring-framework.*` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 71 | `/dir/*.*` | `/other/hotel.pdf` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 72 | `/a/*.html` | `/{x}/y.*` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 73 | `/*.html` | `/a/b` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 74 | `/*.html/x` | `/bar` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 75 | `/projects/*.html/releases` | `/{id}` | throws IllegalArgumentException | throws PathMergeException | ✅ |
| 76 | `/*.html` | `hotel` | throws StringIndexOutOfBoundsException | throws PathMergeException | ✅ |
| 77 | `*.html` | `/hotel` | throws StringIndexOutOfBoundsException | throws PathMergeException | ✅ |
