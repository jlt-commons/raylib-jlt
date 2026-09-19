# Textures via rlgl: the struct-returning half of raylib, reached from underneath

> **Superseded as a constraint, still current as practice.** This page argues
> that `LoadTexture` cannot be bound because it returns a struct by value. That
> was true when it was written and is false now: jolt **0.7.23** added
> `[:by-value [:struct ...]]`, which reaches `LoadTexture` and the rest of the
> `Load*` family directly. See [`structs-by-value.md`](structs-by-value.md).
>
> The rlgl technique below is still what this suite's texture and framebuffer
> code actually does, and still the right tool when you want GPU objects
> without raylib's file loaders - so the page stays as written rather than
> being rewritten around the newer API.

The three earlier FFI pages are all about arguments going *in*: a `Color` packed
into a [`:uint`](color-by-value.md), a `Camera2D`
[passed by pointer](struct-by-value-pointer-trick.md), `Vector2`/`Vector3`
geometry [rerouted through rlgl](rlgl-immediate-mode.md). Textures fail on the
way *out*, which none of those tricks touch, and the fix is different in kind:
stop calling raylib's texture API and call the layer it is built on.

## Why LoadTexture has no binding

```c
RLAPI Texture2D LoadTexture(const char *fileName);
```

`Texture2D` is 20 bytes: `unsigned int id` plus four `int`s. On AArch64 a
composite larger than 16 bytes is **returned indirectly**: the caller allocates
space for the result and passes its address in **`x8`**, a register reserved for
exactly this and separate from the ordinary argument registers `x0`-`x7`. The
callee writes the struct through it and returns nothing in `x0`.

Chez's `foreign-procedure` has no way to say that. Its return types are scalars
and pointers, which cover values coming back in `x0` or `v0`; there is no
spelling for "and by the way, here is the hidden result buffer in `x8`". Adding a
leading `[:pointer]` argument does not work either: that lands in `x0`, so raylib
would read it as `fileName` and write the struct somewhere it was never told
about.

This is not specific to `LoadTexture`. `LoadImage`, `LoadRenderTexture`,
`LoadFont`, `LoadShader`, `LoadModel` and `LoadSound` all return their struct by
value, which is why this suite has no image, font, shader, model or audio
examples.

## rlgl is scalar all the way down

raylib's texture functions are a convenience layer over `rlgl`, and `rlgl` deals
in the raw OpenGL object: an `unsigned int` name. Every call in the path is
scalar.

```clojure
;; lib/src/net/b12n/raylib/textures.clj
(ffi/defcfn rl-load-texture       "rlLoadTexture"       [:pointer :int :int :int :int] :uint)
(ffi/defcfn rl-unload-texture     "rlUnloadTexture"     [:uint] :void)
(ffi/defcfn rl-update-texture     "rlUpdateTexture"     [:uint :int :int :int :int :int :pointer] :void)
(ffi/defcfn rl-texture-parameters "rlTextureParameters" [:uint :int :int] :void)
(ffi/defcfn rl-set-texture        "rlSetTexture"        [:uint] :void)
(ffi/defcfn rl-tex-coord-2f       "rlTexCoord2f"        [:float :float] :void)
```

So a texture in this project is an `int`. There is no struct to marshal, nothing
to allocate, and no ABI question to answer. What is lost is the part of
`LoadTexture` that is not about the GPU at all: decoding a PNG. `rlLoadTexture`
wants pixels, so pixels are what the examples build.

## Building the pixels

A packed `Color` is `r | g<<8 | b<<16 | a<<24` (see
[`color-by-value.md`](color-by-value.md)), which on a little-endian machine is
byte-for-byte the RGBA8 layout OpenGL wants. That makes each texel a single
`:uint` write rather than four `:uint8` ones:

```clojure
(defn texture-from-fn
  [w h f]
  (let [buf (ffi/alloc (* w h 4))]
    (try
      (dotimes [y h]
        (dotimes [x w]
          (ffi/write buf :uint (f x y) (* 4 (+ x (* y w))))))
      (let [id (rl-load-texture buf w h PIXELFORMAT-R8G8B8A8 1)]
        (texture-filter! id RL-TEXTURE-FILTER-NEAREST)
        (texture-wrap! id RL-TEXTURE-WRAP-REPEAT)
        id)
      (finally (ffi/free buf)))))
```

`rlLoadTexture` copies to the GPU before it returns, so the staging buffer is
freed immediately. `texture-procedural` and `texture-tiling` are both nothing
more than different `f`s.

## Drawing with one

`rl/texture!` emits a quad in the same order raylib's own `DrawTexturePro` does,
so it batches identically:

```clojure
(rl-set-texture id)
(rl-begin RL-QUADS)
(rl-color! tint)
(rl-normal-3f 0.0 0.0 1.0)
(rl-tex-coord-2f u0 v0) (rl-vertex-2f x0 y0)   ; top left
(rl-tex-coord-2f u0 v1) (rl-vertex-2f x0 y1)   ; bottom left
(rl-tex-coord-2f u1 v1) (rl-vertex-2f x1 y1)   ; bottom right
(rl-tex-coord-2f u1 v0) (rl-vertex-2f x1 y0)   ; top right
(rl-end)
(rl-set-texture 0)
```

Texture coordinates past `1.0` tile when the wrap mode is `REPEAT`, which is the
whole of `texture-tiling`: one 64x64 texture covers the window in a single quad,
and raising the density costs nothing. `bunnymark` leans on the batching instead:
thousands of quads that all share one texture become one draw call.

## Render textures, and the restore that is easy to get wrong

Framebuffers are scalar too, so an off-screen target is `rlLoadFramebuffer` plus
two `rlFramebufferAttach` calls. What `BeginTextureMode` adds around them is
viewport and projection bookkeeping, which `rl/with-render-texture` spells out.

The half worth reading closely is the restore. raylib does it across two
functions: `EndTextureMode` calls its private `SetupViewport`, and the next
`BeginDrawing` multiplies a **screen-scale matrix** into the modelview. On a
HiDPI display those are different numbers:

| | value on a Retina Mac |
|---|---|
| `GetScreenWidth` | 800, the logical window |
| `GetRenderWidth` | 1600, what is actually rasterised |
| `rlGetFramebufferWidth` | 800 |
| modelview scale raylib applies | 2.0 |

raylib projects in **render** pixels and bridges the gap with that scale, so
restoring the viewport and the projection while leaving the modelview at identity
draws every later frame at half size in the lower-left corner. The first version
of `with-render-texture` in this repo did exactly that, and it looked correct in
the code: it restored from `rlGetFramebufferWidth`, whose name suggests it is the
framebuffer size and which reports the logical size instead.

The working restore reads `GetRenderWidth`/`GetRenderHeight`, both scalar, and
rebuilds the scale from `render/screen`:

```clojure
(defn- restore-screen-projection!
  []
  (let [rw (get-render-width)
        rh (get-render-height)
        sx (/ (double rw) (max 1 (get-screen-width)))
        sy (/ (double rh) (max 1 (get-screen-height)))
        m (ffi/alloc 64)]                       ; 16 floats, column-major
    ...
    (rl-mult-matrix-f m)))
```

`rlMultMatrixf` takes `const float *`, so unlike the by-value struct that started
this page, the matrix crosses the boundary as an ordinary pointer.

One GL convention survives into the API: a framebuffer texture is stored
bottom-up, so drawing one back needs `:v0 1.0 :v1 0.0` to flip it.
`render-texture` and `window-letterbox` both do.

## See also

- [`color-by-value.md`](color-by-value.md): the packed `Color` that makes a texel
  one write.
- [`rlgl-immediate-mode.md`](rlgl-immediate-mode.md): the same "go under raylib"
  move, applied to by-value vector arguments instead of struct returns.
- [`struct-by-value-pointer-trick.md`](struct-by-value-pointer-trick.md): what to
  do when the struct is going in rather than coming out.
