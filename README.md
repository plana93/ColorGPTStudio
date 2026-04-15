<div align="center">
  <img src="assets/logo.png" width="110" alt="ColorGPT Studio" /><br/><br/>
  <strong style="font-size:1.6em">ColorGPT Studio</strong><br/>
  <em>Tap a pixel. Get the color. Move on.</em>
</div>

---

Point your camera at a wall, a piece of wood, a fabric sample — and in one tap you get the HEX, RAL, CMYK, NCS and RGB values you need to order the right paint, write the spec, or send the quote.  
No cloud. No account. No noise.



---

## Screens

| Home | Quick Analysis | Project |
|:----:|:--------------:|:-------:|
| ![Home](assets/starting.jpg) | ![Quick](assets/rapid.jpg) | ![Project](assets/pro.jpg) |

---

## Features

- **Tap-to-pick** — one touch extracts the exact color from any pixel
- **Full color vocabulary** — HEX · RGB · CMYK · HSL · RAL Classic · NCS
- **Projects** — group images under a job; global palette built automatically
- **Annotate** — label, note, material code and craft tags per color point
- **Quick Analysis** — instant session for on-site checks, no project needed
- **100% offline** — zero internet permission, everything on-device

---

## Stack

| | |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DB | Room + KSP |
| DI | Koin |
| Images | Coil |
| Color engine | Custom K-means clustering (pure Kotlin) |
| Min SDK | Android 8.0 (API 26) |

---

## Run it

```bash
git clone https://github.com/plana93/ColorGPTStudio.git
cd ColorGPTStudio
./gradlew installDebug
```

---

## License

MIT
