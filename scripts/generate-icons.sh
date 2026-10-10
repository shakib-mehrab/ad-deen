#!/usr/bin/env bash
set -euo pipefail

# Script to regenerate all Android app icons from master logo source
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MASTER_SVG="$ROOT_DIR/assets/logo/ad-deen-logo.svg"
MASTER_FG_SVG="$ROOT_DIR/assets/logo/ad-deen-foreground.svg"
NOTIF_SVG="$ROOT_DIR/assets/logo/ic-stat-notification.svg"
OUTPUT_1024="$ROOT_DIR/assets/logo/ad-deen-logo-1024.png"
OUTPUT_512="$ROOT_DIR/assets/logo/play-store-512.png"
RES_DIR="$ROOT_DIR/app/src/main/res"

echo "==> 1. Generating master 1024x1024 PNG and 512x512 Play Store icon..."
rsvg-convert -w 1024 -h 1024 "$MASTER_SVG" -o "$OUTPUT_1024"
rsvg-convert -w 512 -h 512 "$MASTER_SVG" -o "$OUTPUT_512"

echo "==> 2. Removing template webp files from mipmap directories..."
rm -f "$RES_DIR"/mipmap-*/ic_launcher.webp "$RES_DIR"/mipmap-*/ic_launcher_round.webp

echo "==> 3. Generating mipmap raster launcher icons and round icons..."
# mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192
for spec in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  density="${spec%:*}"
  size="${spec#*:}"
  dir="$RES_DIR/mipmap-$density"
  mkdir -p "$dir"

  # Standard launcher icon
  convert "$OUTPUT_1024" -resize "${size}x${size}!" "PNG32:$dir/ic_launcher.png"

  # Circular launcher icon
  radius=$((size / 2))
  convert "$OUTPUT_1024" -resize "${size}x${size}!" \
    \( -size "${size}x${size}" xc:none -fill white \
       -draw "circle $radius,$radius $radius,0" \) \
    -alpha set -compose DstIn -composite "PNG32:$dir/ic_launcher_round.png"
done

echo "==> 4. Generating notification icon ic_stat_notification.png in drawable directories..."
# Default drawable
rsvg-convert -w 48 -h 48 "$NOTIF_SVG" -o "$RES_DIR/drawable/ic_stat_notification.png"
# Density drawables
for spec in mdpi:24 hdpi:36 xhdpi:48 xxhdpi:72 xxxhdpi:96; do
  density="${spec%:*}"
  size="${spec#*:}"
  dir="$RES_DIR/drawable-$density"
  mkdir -p "$dir"
  rsvg-convert -w "$size" -h "$size" "$NOTIF_SVG" -o "$dir/ic_stat_notification.png"
done

# Also generate adaptive foreground raster for fallback
mkdir -p "$RES_DIR/drawable"
rsvg-convert -w 432 -h 432 "$MASTER_FG_SVG" -o "$RES_DIR/drawable/ic_launcher_foreground_img.png"

echo "==> 5. Writing adaptive icon XML wrappers..."
cat << 'EOF' > "$RES_DIR/drawable/ic_launcher_background.xml"
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#874D14"
        android:pathData="M0,0h108v108h-108z" />
</vector>
EOF

cat << 'EOF' > "$RES_DIR/drawable/ic_launcher_foreground.xml"
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:width="66dp"
        android:height="66dp"
        android:drawable="@drawable/ic_launcher_foreground_img"
        android:gravity="center" />
</layer-list>
EOF

mkdir -p "$RES_DIR/mipmap-anydpi-v26"
cat << 'EOF' > "$RES_DIR/mipmap-anydpi-v26/ic_launcher.xml"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
EOF

cat << 'EOF' > "$RES_DIR/mipmap-anydpi-v26/ic_launcher_round.xml"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
EOF

cat << 'EOF' > "$RES_DIR/drawable/ic_launcher_monochrome.xml"
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <foreground android:drawable="@drawable/ic_stat_notification" />
</adaptive-icon>
EOF

echo "==> Done! All Android icons successfully generated."
