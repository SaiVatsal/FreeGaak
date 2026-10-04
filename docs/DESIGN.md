# SoundOrbit Design & Brand

## Design System

### 1. Brand Identity
- **Name:** SoundOrbit
- **Concept:** Music rotating in personal orbit around the listener.
- **Palette Concept:** Cosmic Teal & Nebula Coral on Deep Space Onyx.
- **Originality Rule:** Strictly avoid Spotify green (`#1DB954`), Spotify circular badges, or Spotify iconography.

### 2. Color Palette
- **Deep Space Black (OLED):** `#050505`
- **Surface Dark:** `#121417`
- **Surface Variant:** `#1E2228`
- **Primary Teal:** `#00BFA5` (Orbit Glow)
- **Primary Container:** `#004D40`
- **Secondary Coral:** `#FF6B6B` (Accent Star)
- **Secondary Container:** `#4A1515`
- **Tertiary Cyan:** `#00E5FF`
- **Text High Emphasis:** `#FFFFFF` (90% opacity)
- **Text Medium Emphasis:** `#B0BEC5` (70% opacity)
- **Text Disabled:** `#546E7A` (38% opacity)

### 3. Typography
Clean, modern sans-serif scale (Material 3 standard scale):
- **Display Large:** 57sp / 64sp line height
- **Headline Large:** 32sp / 40sp line height
- **Headline Medium:** 28sp / 36sp line height
- **Title Large:** 22sp / 28sp line height
- **Title Medium:** 16sp / 24sp line height
- **Body Large:** 16sp / 24sp line height
- **Body Medium:** 14sp / 20sp line height
- **Label Large:** 14sp / 20sp line height (buttons)
- **Label Small:** 11sp / 16sp line height (badges, timestamps)

### 4. Components & Layout
- **Cards:** Rounded 16dp corners, subtle surface elevation.
- **Touch Targets:** Minimum 48dp on all interactive elements.
- **Adaptive Layout:**
  - Phone (<600dp width): Bottom navigation bar with mini player docked above.
  - Tablet/Foldable (≥600dp width): Navigation rail on leading side, multi-pane content.
- **Motion:** Spring-based physics for player sheet expansion, subtle fade-through on navigation.
- **Accessibility:** Content descriptions on all icons, WCAG AAA contrast ratio on all text pairs.
