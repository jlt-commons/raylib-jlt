# Structs by value: `[:by-value [:struct ...]]`

Three of this guide's other pages exist because jolt could not pass or return a
C struct by value, and each works around that in a different way. jolt
**0.7.23** added `[:by-value ...]` and the workarounds became optional. This
page is what to write now.

0.7.23 is the first version where `LoadShader` is callable at all, and the ten
`shaders` examples all depend on it, so nothing in this page works below it.

The suite's declared floor is higher now. `deps.edn` says
`:jolt/min-version "0.8.0"`, for an unrelated reason: 0.8.0 moved `ffi/write`'s
value argument in front of the offset, and the old and new spellings are both
integers, so an older runtime writes to the wrong place without complaining. The
declaration is a forward guard rather than a cure, since a jolt old enough to have
the old order is also too old to read the key. See the
[README's jolt requirements](../../README.md#jolt) for the full story and for
`JOLT_SKIP_MIN_VERSION=1`.

## Both directions, one descriptor

From jolt's own `stdlib/jolt/ffi.clj`:

> A struct passed or returned by value uses the same literal descriptor as
> layout, wrapped in `[:by-value descriptor]`. An argument value is a non-null
> caller-owned pointer to the struct bytes. An aggregate-returning callable
> takes a non-null caller-owned destination pointer as its first Jolt argument,
> writes the C return there, and returns that pointer.

So the same `[:struct [[field type] ...]]` shape serves three jobs: `ffi/layout`
for reading and writing fields, `[:by-value ...]` in an argument slot, and
`[:by-value ...]` in the return slot.

raylib's `Shader` is `{unsigned int id; int *locs;}`. Here it is in all three
positions:

```clojure
(def shader-layout (ffi/layout [:struct [[:id :uint] [:locs :pointer]]]))

;; returned by value
(ffi/defcfn ^:private load-shader-from-memory "LoadShaderFromMemory"
  [:pointer :string]
  [:by-value [:struct [[:id :uint] [:locs :pointer]]]])

;; passed by value
(ffi/defcfn ^:private begin-shader-mode "BeginShaderMode"
  [[:by-value [:struct [[:id :uint] [:locs :pointer]]]]] :void)
```

## The return convention: a leading destination pointer

This is the part that surprises. `load-shader-from-memory` is *declared* with
two argument types, and *called* with three:

```clojure
(let [p (ffi/alloc (ffi/layout-size shader-layout))]
  (load-shader-from-memory p ffi/null fs-source)   ; p is the destination
  (if (pos? (ffi/read-field p shader-layout :id))
    p
    (do (ffi/free p) nil)))
```

The caller allocates the struct's bytes and passes that pointer first; jolt
writes C's return value there and hands the same pointer back. The declared
argument list does not mention it.

That asymmetry is worth knowing before you write a lint rule: this repo's
clj-kondo hook for `defcfn` has to add one to the expected arity whenever the
return type is an aggregate, or every aggregate-returning call site reports as
a wrong-arity error.

Two limits from the same paragraph, neither of which this suite hits:
aggregate **callbacks and exports** are not supported, and while a fixed
aggregate may precede `:varargs`, aggregate variadic arguments and
aggregate-return-plus-varargs are rejected.

## The descriptor is a literal, not a value

`ffi/layout` is a macro and `defcfn` expands at analysis time, so the
descriptor has to be written out at each use. Naming it does not work:

```clojure
(def shader-struct [:struct [[:id :uint] [:locs :pointer]]])

(ffi/defcfn ^:private begin-sm "BeginShaderMode"
  [[:by-value shader-struct]] :void)
```

```
Unhandled exception: jolt.ffi layout descriptor must be
  [:struct [[field type] ...]], got shader-struct
  ex-data: {:jolt/error {:type :analysis-error, :line 3, :column 1, ...}}
```

An `:analysis-error`, so it fails at compile time rather than at the call - the
good outcome. The cost is that the struct shape is repeated at every binding
site, which is why `net.b12n.raylib.shaders` keeps a `shader-layout` next to them: the
`ffi/layout` value cannot be shared with the `defcfn` forms, but it is what
every field access goes through.

## Reading and writing fields

`ffi/layout` compiles the descriptor into ABI layout data - Chez supplies size,
alignment and offsets, so no offset is ever written by hand:

```clojure
(def texture2d-layout
  (ffi/layout [:struct [[:id :uint] [:width :int] [:height :int]
                        [:mipmaps :int] [:format :int]]]))

(ffi/with-layout [t texture2d-layout]              ; allocates, frees on exit
  (ffi/write-field t texture2d-layout :id tex-id)
  (ffi/write-field t texture2d-layout :width (int w))
  (set-shader-value-texture-raw sh loc t))
```

- `with-layout` allocates one instance and frees it exactly once; the pointer
  must not escape the body.
- `read-field` / `write-field` take `[pointer layout path]`. A path is a
  keyword, or a vector for nesting. Integer components index arrays; jolt's own
  docstring gives `[:params 3]`, `[:events 1 :frame]` and `[:matrix 1 2]`.
  Nothing in this suite needs a nested path yet - every struct it binds is flat.
- `layout-size` and `layout-alignment` read the compiled layout, which is how
  the allocation above is sized.
- A path naming a struct or array rather than a scalar is rejected, so a
  half-written field access fails loudly.

Nesting matters for correctness, not just tidiness. Flattening a nested struct
into one field list drops the inner struct's alignment padding, and the fields
after it read as garbage.

## Two structs, two ABI paths, one call

`SetShaderValueTexture(Shader, int, Texture2D)` takes both by value, and on
arm64 they do not travel the same way. Measured with clang rather than assumed:

```
Shader    16  (id 0, locs 8)
Texture2D 20  (id 0 w 4 h 8 mips 12 fmt 16)
```

AAPCS64 puts an all-integer struct of 16 bytes or less in general-purpose
registers and passes anything larger indirectly, by a pointer the caller
supplies. So `Shader` rides in registers and `Texture2D` does not, in the same
signature. jolt 0.7.23 handles the pair; `multi-sampler` is the example.

## Read the enum from the header you are actually linking

`[:by-value ...]` gets the struct across. Constants are still yours to get
right, and `SHADER_UNIFORM_SAMPLER2D` is the cautionary one:

| header | 5.5 | 6.0 |
|---|---|---|
| `raylib.h` `SHADER_UNIFORM_SAMPLER2D` | **8** | **12** |
| `rlgl.h` `RL_SHADER_UNIFORM_SAMPLER2D` | **12** | **12** |

raylib 6.0 inserted `UINT`, `UIVEC2`, `UIVEC3`, `UIVEC4` at 8-11, which pushed
`SAMPLER2D` from 8 to 12. rlgl's parallel enum already had those four in 5.5,
so on 5.5 **the two headers disagreed**: 8 in one, 12 in the other, for the
same-named constant in the same library.

A wrong value here does not fail. It binds a different uniform slot and renders
a plausible image. So read it from the header being linked:

```sh
$ cat > /tmp/enumprobe.c <<'C'
#include <raylib.h>
#include <rlgl.h>
#include <stdio.h>
int main(void) {
  printf("raylib.h SHADER_UNIFORM_SAMPLER2D    = %d\n", SHADER_UNIFORM_SAMPLER2D);
  printf("rlgl.h   RL_SHADER_UNIFORM_SAMPLER2D = %d\n", RL_SHADER_UNIFORM_SAMPLER2D);
  return 0;
}
C
$ cc /tmp/enumprobe.c -I/opt/homebrew/include -L/opt/homebrew/lib -lraylib -o /tmp/enumprobe && /tmp/enumprobe
raylib.h SHADER_UNIFORM_SAMPLER2D    = 12
rlgl.h   RL_SHADER_UNIFORM_SAMPLER2D = 12
```

Never copy the value from the other header, from a tutorial, or from memory.
`net.b12n.raylib.shaders` carries `UNIFORM-SAMPLER2D` as 12 with a comment naming the
version it came from, so the next version bump has something to check against.

## Why this generalizes

Any C API whose types are small structs is now reachable directly rather than
through a workaround: `Texture2D`, `Rectangle`, `Vector2`, `Camera2D`,
`RenderTexture2D`, and the file loaders and audio API that return them. The
suite has not migrated - the rlgl and pointer-trick code still works and is
still what most of these examples use - but new bindings should start here.

## See also

- [`color-by-value.md`](color-by-value.md) - `Color` as a packed `:uint`, which
  is still the cheapest way to move a 4-byte struct and needs no layout at all.
- [`struct-by-value-pointer-trick.md`](struct-by-value-pointer-trick.md) -
  superseded by this page, and x86-64-unsafe.
- [`textures-via-rlgl.md`](textures-via-rlgl.md) - superseded for the reason it
  gives for existing, though the suite still draws through rlgl.
- [`rlgl-immediate-mode.md`](rlgl-immediate-mode.md) - still the right tool for
  geometry, independent of how structs cross.
