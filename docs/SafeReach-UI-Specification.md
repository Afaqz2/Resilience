# SafeReach — Complete UI Specification
## Metro 2033 Survival Theme

> Use this document as a comprehensive reference to build or extend any screen in the SafeReach app while maintaining perfect visual consistency.

---

## 1. DESIGN PHILOSOPHY

**Theme:** Post-apocalyptic tactical survival tool inspired by Metro 2033 game art.
**Feeling:** Gritty, weathered, military-industrial. Like a bunker terminal interface.
**No:** Rounded bubbly UI, bright saturated colors, playful aesthetics, gradients, or glossy effects.

---

## 2. COLOR SYSTEM (HSL Tokens)

All colors are defined as CSS custom properties in HSL format (without `hsl()` wrapper). Use Tailwind semantic classes — NEVER hardcode hex/rgb in components.

### Backgrounds
| Token | HSL | Hex Approx | Usage |
|-------|-----|------------|-------|
| `--background` | `0 0% 10%` | #1A1A1A | Page/app background |
| `--card` | `0 0% 16.5%` | #2A2A2A | Cards, panels, containers |
| `--muted` | `0 0% 22%` | #383838 | Disabled/subtle backgrounds |

### Text
| Token | HSL | Hex Approx | Usage |
|-------|-----|------------|-------|
| `--foreground` | `30 8% 82%` | #D4D0C8 | Primary text (weathered off-white) |
| `--muted-foreground` | `50 5% 50%` | #8A8A7A | Secondary/label text |
| `--primary-foreground` | `30 8% 90%` | #E8E4DC | Text on primary-colored backgrounds |
| `--accent-foreground` | `0 0% 10%` | #1A1A1A | Text on accent (amber) backgrounds |

### Accent Colors
| Token | HSL | Hex Approx | Usage |
|-------|-----|------------|-------|
| `--primary` | `18 50% 47%` | #B85C38 | Rust orange — primary actions, active states, key icons |
| `--secondary` | `100 21% 29%` | #4A5A3A | Military olive — secondary elements, "stocked"/"online" status |
| `--accent` | `45 70% 46%` | #D4A017 | Hazard amber — emergency mode, warnings, "unknown" status |
| `--destructive` | `0 70% 45%` | #C42B2B | Red — critical alerts, errors, "CRITICAL" status |

### Borders & Inputs
| Token | HSL | Hex Approx | Usage |
|-------|-----|------------|-------|
| `--border` | `0 0% 23%` | #3A3A3A | Card/panel borders |
| `--input` | `0 0% 23%` | #3A3A3A | Form input borders |
| `--ring` | `18 50% 47%` | #B85C38 | Focus ring (matches primary) |
| `--radius` | `0.25rem` | 4px | Global border-radius — intentionally small for industrial feel |

### Tailwind Usage
```tsx
// ✅ CORRECT — use semantic tokens
className="bg-card text-foreground border-border"
className="text-primary bg-primary/20"
className="text-muted-foreground"

// ❌ WRONG — never hardcode colors
className="bg-[#2A2A2A] text-white border-gray-700"
```

---

## 3. TYPOGRAPHY

### Font Stack
| Font | CSS Class | Usage |
|------|-----------|-------|
| **IBM Plex Mono** | Default body font (`font-family: 'IBM Plex Mono', monospace`) | All body text, descriptions, data |
| **Share Tech Mono** | `.stencil-text` class | Labels, headings, categories, navigation, status tags |

### Google Fonts Import
```css
@import url('https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@300;400;500;600;700&family=Share+Tech+Mono&display=swap');
```

### Text Sizes & Classes
| Element | Size | Classes |
|---------|------|---------|
| Screen title (H1) | `text-sm` (14px) | `stencil-text text-sm font-bold text-foreground tracking-widest` |
| Section label | `text-[10px]` | `stencil-text text-[10px] text-muted-foreground tracking-widest` |
| Card title | `text-sm` (14px) | `text-foreground font-semibold text-sm` |
| Body text | `text-xs` (12px) | `text-muted-foreground text-xs leading-relaxed` |
| Badge/tag | `text-[9px]` | `stencil-text text-[9px] px-2 py-0.5 rounded` |
| Status indicator | `text-[9px]` | `stencil-text text-[9px]` |
| Navigation label | `text-[10px]` | `stencil-text text-[10px] tracking-wider` |

### The `.stencil-text` Class
```css
.stencil-text {
  font-family: 'Share Tech Mono', monospace;
  letter-spacing: 0.15em;
  text-transform: uppercase;
}
```
**Rule:** ALL labels, headings, categories, navigation items, and status badges use `.stencil-text`. Body/description text does NOT.

---

## 4. ICONS

### Library
**Lucide React** (`lucide-react`) — all icons come from this library.

### Standard Sizes
| Context | Size | Example |
|---------|------|---------|
| Navigation grid tiles | `w-5 h-5` | `<Home className="w-5 h-5 text-primary" />` |
| Header utility buttons | `w-4 h-4` | `<Globe className="w-4 h-4 text-muted-foreground" />` |
| Emergency overlay actions | `w-6 h-6` | `<Phone className="w-6 h-6 text-destructive" />` |
| Back button | `w-4 h-4` | `<ArrowLeft className="w-4 h-4 text-muted-foreground" />` |
| Inline status | `w-3 h-3` | `<AlertTriangle className="w-3 h-3" />` |

### Icon Color Rules
- **Primary actions/nav icons:** `text-primary` (rust orange)
- **Inactive/utility icons:** `text-muted-foreground`
- **Critical/danger:** `text-destructive`
- **Warning/emergency:** `text-accent`
- **Success/online:** `text-secondary`
- **On hover:** `group-hover:text-primary/80` or `group-hover:text-foreground`

---

## 5. BUTTON PATTERNS

### Navigation Grid Tile (Home Screen)
```tsx
<button className="flex flex-col items-center gap-1.5 p-3 rounded bg-card border border-border hover:border-primary/40 transition-all group tactical-border">
  <Icon className="w-5 h-5 text-primary group-hover:text-primary/80 transition-colors" />
  <span className="text-[10px] text-muted-foreground stencil-text tracking-wider group-hover:text-foreground transition-colors">
    LABEL
  </span>
</button>
```

### Emergency Mode Button
```tsx
<button className="w-full py-4 rounded bg-accent text-accent-foreground font-bold stencil-text text-sm tracking-widest emergency-pulse flex items-center justify-center gap-3 border border-accent/50">
  <AlertTriangle className="w-5 h-5" />
  EMERGENCY MODE
  <AlertTriangle className="w-5 h-5" />
</button>
```

### Header Utility Button (Globe, QR)
```tsx
<button className="p-2 rounded bg-card border border-border hover:border-primary/50 transition-colors">
  <Icon className="w-4 h-4 text-muted-foreground" />
</button>
```

### Back Button (Sub-screens)
```tsx
<button onClick={() => onNavigate('home')} className="p-2 rounded bg-card border border-border hover:border-primary/50 transition-colors">
  <ArrowLeft className="w-4 h-4 text-muted-foreground" />
</button>
```

### Screen Selector Tab (Index page)
```tsx
// Active state
className="px-4 py-2 rounded text-xs stencil-text tracking-wider border bg-primary text-primary-foreground border-primary"

// Inactive state
className="px-4 py-2 rounded text-xs stencil-text tracking-wider border bg-card text-muted-foreground border-border hover:border-primary/40"
```

### Toggle Switch (Settings)
```tsx
<button className={`w-9 h-5 rounded-full transition-colors relative ${enabled ? 'bg-primary' : 'bg-muted'}`}>
  <div className={`absolute top-0.5 w-4 h-4 rounded-full bg-foreground transition-transform ${enabled ? 'left-[18px]' : 'left-0.5'}`} />
</button>
```

### Emergency Overlay Quick Action
```tsx
<button className="flex flex-col items-center gap-2 p-4 rounded bg-destructive/20 border border-destructive/40 hover:bg-destructive/30 transition-colors tactical-border">
  <Phone className="w-6 h-6 text-destructive" />
  <span className="text-[10px] stencil-text text-destructive tracking-wider">CALL 911</span>
</button>
```

---

## 6. CARD PATTERNS

### Standard Content Card
```tsx
<div className="rounded bg-card border border-border p-4 tactical-border">
  {/* Content */}
</div>
```

### Card with Status Badge
```tsx
<div className="p-3 rounded bg-card border border-border tactical-border">
  <div className="flex items-center justify-between mb-1">
    <span className="text-foreground text-xs font-medium">Item Name</span>
    <span className={`text-[9px] stencil-text px-1.5 py-0.5 rounded ${statusBg} ${statusColor}`}>
      STATUS
    </span>
  </div>
  <p className="text-[10px] text-muted-foreground">Description</p>
</div>
```

### Active Directive Card (Home)
```tsx
<div className="rounded bg-card border border-border p-4 tactical-border h-full">
  <div className="flex items-center gap-2 mb-3">
    <div className="w-2 h-2 rounded-full bg-primary animate-pulse" />
    <p className="text-[10px] text-primary stencil-text tracking-widest">ACTIVE DIRECTIVE</p>
  </div>
  <h2 className="text-foreground font-semibold text-sm mb-2">Title</h2>
  <p className="text-muted-foreground text-xs leading-relaxed">Description</p>
  <div className="mt-3 flex gap-2">
    <span className="text-[9px] px-2 py-0.5 rounded bg-primary/20 text-primary stencil-text">PRIORITY: MEDIUM</span>
    <span className="text-[9px] px-2 py-0.5 rounded bg-muted text-muted-foreground stencil-text">24H AGO</span>
  </div>
</div>
```

---

## 7. STATUS INDICATOR SYSTEM

### Color-Status Mapping
| Status | Text Color | Background | Examples |
|--------|-----------|------------|----------|
| Stocked / Online / Active | `text-secondary` | `bg-secondary/20` | Supply in stock, contact online |
| Low / Warning / Unknown | `text-accent` | `bg-accent/20` | Low supplies, unknown contact status |
| Critical / Danger | `text-destructive` | `bg-destructive/20` | Critical supply, critical alert |
| Neutral / Offline | `text-muted-foreground` | `bg-muted` | Offline contacts, old timestamps |
| Primary / Active | `text-primary` | `bg-primary/20` | Active directives, medium priority |

### Status Dot
```tsx
// Online (green/olive)
<div className="w-2 h-2 rounded-full bg-secondary" />

// Pulsing active
<div className="w-2 h-2 rounded-full bg-primary animate-pulse" />

// System nominal
<div className="w-1.5 h-1.5 rounded-full bg-secondary" />
```

### Severity Meter (Alerts Screen)
A 5-segment bar where filled segments use the severity color:
```tsx
// Segments: filled = severity color, empty = bg-muted
<div className="flex gap-0.5">
  {[...Array(5)].map((_, i) => (
    <div key={i} className={`h-2 w-3 rounded-sm ${i < level ? severityColor : 'bg-muted'}`} />
  ))}
</div>
```

---

## 8. SCREEN LAYOUT TEMPLATE

### Sub-Screen Standard Structure
Every sub-screen follows this pattern:

```tsx
const Screen = ({ onNavigate }) => {
  return (
    <div className="flex flex-col h-full noise-overlay scanner-line">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 pt-4 pb-2 relative z-10">
        <button onClick={() => onNavigate('home')} className="p-2 rounded bg-card border border-border hover:border-primary/50 transition-colors">
          <ArrowLeft className="w-4 h-4 text-muted-foreground" />
        </button>
        <div className="flex items-center gap-2">
          <ScreenIcon className="w-4 h-4 text-primary" />
          <h1 className="stencil-text text-sm font-bold text-foreground tracking-widest">SCREEN TITLE</h1>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto px-4 pb-4 relative z-10">
        <p className="text-[10px] text-muted-foreground stencil-text mb-2 tracking-widest">// SECTION LABEL</p>
        {/* Cards/content here */}
      </div>

      {/* Bottom Status Bar */}
      <div className="px-4 py-2 border-t border-border flex items-center justify-between relative z-10">
        <div className="flex items-center gap-2">
          <div className="w-1.5 h-1.5 rounded-full bg-secondary" />
          <span className="text-[9px] text-muted-foreground stencil-text">STATUS TEXT</span>
        </div>
        <span className="text-[9px] text-muted-foreground stencil-text">COUNT / INFO</span>
      </div>
    </div>
  );
};
```

### Key Layout Rules
1. **Always** add `noise-overlay scanner-line` to root container
2. **Always** add `relative z-10` to content sections (to sit above noise overlay)
3. **Always** include a bottom status bar with a status dot + label
4. Section labels use format: `// SECTION NAME` with stencil-text styling
5. Content area uses `flex-1 overflow-y-auto`

---

## 9. CSS EFFECTS & ANIMATIONS

### Noise Overlay (Post-Apocalyptic Texture)
```css
.noise-overlay { position: relative; }
.noise-overlay::before {
  content: '';
  position: absolute;
  inset: 0;
  opacity: 0.04;
  background-image: url("data:image/svg+xml,...fractalNoise...");
  pointer-events: none;
  z-index: 1;
}
```

### Scanner Line
```css
.scanner-line::after {
  content: '';
  position: absolute;
  left: 0; right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, hsl(18 50% 47% / 0.3), transparent);
  animation: scanner 4s linear infinite;
  pointer-events: none;
  z-index: 2;
}
```

### Emergency Pulse
```css
.emergency-pulse {
  animation: emergency-pulse 2s ease-in-out infinite;
  /* Glows with accent (amber) color */
}
```

### Tactical Border
```css
.tactical-border {
  border: 1px solid hsl(0 0% 23%);
  position: relative;
}
.tactical-border::before {
  /* Top-edge orange gradient highlight */
  background: linear-gradient(90deg, transparent, hsl(18 50% 47% / 0.4), transparent);
}
```

### Hazard Stripes (Emergency Overlay)
```css
.bg-repeating-hazard {
  background: repeating-linear-gradient(
    -45deg,
    hsl(45 70% 46%),
    hsl(45 70% 46%) 10px,
    hsl(0 0% 10%) 10px,
    hsl(0 0% 10%) 20px
  );
}
```

---

## 10. COMPONENT INVENTORY BY SCREEN

### Home Screen
- Logo + app title header
- Utility buttons (Globe, QR Code) — `w-4 h-4`, card-style
- Emergency Mode button — full-width, amber, pulsing
- 3×3 navigation grid (7 items)
- "Active Directive" card
- Bottom status bar

### Inventory Screen
- Back button + Package icon header
- Supply cards with name, quantity, category badge, status badge
- Status colors: stocked(olive), low(amber), critical(red)
- Bottom bar: item count + "ADD SUPPLY" label

### Alerts Screen
- Back button + Bell icon header
- Alert cards with: icon, title, description, severity meter, timestamp, location, classification badge
- Severity levels: critical(red), high(orange/primary), medium(amber), low(olive)
- Bottom bar: active alert count

### Family Screen
- Back button + Users icon header
- Contact cards with: name, role, status dot, last seen, distance
- Rally Point info card with MapPin + coordinates
- Status: online(olive), offline(muted), unknown(amber)
- Bottom bar: contact count + "COMMS ACTIVE"

### Playbooks Screen
- Back button + BookOpen icon header
- Expandable playbook cards (click to toggle)
- Each playbook: icon, title, severity badge, category, description
- Expanded view: step-by-step checklist with progress bar
- Steps toggle between Circle and CheckCircle2 icons
- Bottom bar: protocol count

### Maps Screen
- Back button + Map icon header
- SVG tactical grid map with topographic contour lines
- Animated crosshair at center
- Color-coded map markers (rally/shelter/danger/supply/family)
- Filter tabs to toggle marker types
- Marker detail cards below map
- GPS lock indicator with coordinates
- Bottom bar: marker count + "GPS LOCKED"

### Settings Screen
- Back button + Settings icon header
- "Operative Dossier" profile card with avatar placeholder
- Toggle settings: Notifications, Sound Alerts, Dark Mode, Auto-Sync
- Action rows with ChevronRight: Security, Export Data
- System info card with version, build, last sync
- Bottom bar: version number

### Emergency Overlay
- Full-screen takeover with `fixed inset-0 z-50`
- Hazard stripe bars top and bottom
- SOS shield icon in pulsing circle
- 2×2 quick action grid: Call 911, Send Location, Alert Family, SOS Signal
- GPS coordinates card
- Deactivate button at bottom

---

## 11. SPACING & LAYOUT CONVENTIONS

| Element | Spacing |
|---------|---------|
| Screen padding (horizontal) | `px-4` |
| Header top padding | `pt-4` |
| Header bottom padding | `pb-2` |
| Content bottom padding | `pb-4` |
| Card internal padding | `p-3` or `p-4` |
| Grid gap (navigation) | `gap-2` |
| Card list gap | `gap-2` (via `space-y-2`) |
| Badge padding | `px-1.5 py-0.5` or `px-2 py-0.5` |
| Bottom bar padding | `px-4 py-2` |

---

## 12. MOBILE FRAME (Preview Wrapper)

The app preview is displayed inside a phone mockup frame:
- Dimensions: `375px × 812px` (iPhone aspect ratio)
- Border: `3px solid border-color`, `rounded-[2.5rem]`
- Notch: centered, `w-32 h-6`, `bg-border`, `rounded-b-2xl`
- Home indicator: bottom, `w-32 h-1`, `bg-muted-foreground/30`
- Content offset: `pt-7` to clear notch

---

## 13. NAMING CONVENTIONS

- Section labels: `// UPPERCASE LABEL` format
- Status labels: Always UPPERCASE (`STOCKED`, `CRITICAL`, `ACTIVE`)
- Categories: UPPERCASE stencil text (`RADIOLOGICAL`, `HYDRATION`, `COMMS`)
- Navigation: UPPERCASE in grid, Title Case in screen selectors
- Version strings: `v2.4.1` format
- Coordinates: Degree format `48.8566°N 2.3522°E`
- Time: Relative format `14 min ago`, `2h ago`, `1d ago`

---

## 14. DO's AND DON'Ts

### DO
- ✅ Use semantic Tailwind tokens (`bg-card`, `text-primary`, `border-border`)
- ✅ Apply `.stencil-text` to ALL labels, headers, badges, categories
- ✅ Add `noise-overlay scanner-line` to every screen's root div
- ✅ Include `tactical-border` on cards for the orange top-edge glow
- ✅ Keep border-radius small (`rounded`, not `rounded-xl`)
- ✅ Use monospace fonts exclusively
- ✅ Include bottom status bar on every screen
- ✅ Add `relative z-10` to content sections
- ✅ Use opacity variants for backgrounds (`bg-primary/20`, `bg-accent/20`)

### DON'T
- ❌ Use bright/saturated colors or gradients (except hazard stripes)
- ❌ Use rounded-lg or rounded-xl (too soft for tactical theme)
- ❌ Use sans-serif fonts (Inter, Poppins, etc.)
- ❌ Hardcode hex/rgb colors in components
- ❌ Use large text sizes (max is `text-sm` for titles)
- ❌ Skip the noise overlay or scanner line effects
- ❌ Make cards or buttons feel "modern" or "clean" — they should feel rugged
- ❌ Use emojis or playful iconography

