#!/usr/bin/env bash
# Corre DENTRO del emulador (android-emulator-runner). Genera las capturas de la ficha de Play.
set -u
PKG=com.tranquera.app
OUT=${OUT:-capturas}
mkdir -p "$OUT"
UI="python3 scripts/ui.py"
snap() { sleep 2; adb shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; adb pull /sdcard/u.xml "$OUT/$1.xml" >/dev/null 2>&1; adb exec-out screencap -p > "$OUT/$1.png"; echo "captura $1"; }


# Ingresa el PIN de padres en el diálogo; prueba varias formas y deja constancia de cuál anduvo
ingresar_pin() {
  $UI esperar "PIN de padres" 6 || return 0
  local x y
  read -r x y < <(python3 - <<'PY'
import sys; sys.path.insert(0, "scripts")
import ui
n = [n for n in ui.dump().iter("node") if n.get("class") == "android.widget.EditText"][0]
print(*ui.centro(n.get("bounds")))
PY
)
  for intento in texto teclas ime; do
    adb shell input tap "$x" "$y"; sleep 1
    case $intento in
      texto) adb shell input text 482915 ;;
      teclas) adb shell input keyevent 15 11 16 8 9 12 ;;
      ime) for i in $(adb shell ime list -s -a | tr -d "\r"); do adb shell ime enable "$i" >/dev/null; adb shell ime set "$i" >/dev/null; break; done
           sleep 2; adb shell input tap "$x" "$y"; sleep 1; adb shell input text 482915 ;;
    esac
    sleep 1
    $UI tocar "Abrir"; sleep 3
    if ! $UI esperar "PIN de padres" 2; then echo "PIN ok con: $intento" >> "$OUT/pin.txt"; return 0; fi
    echo "PIN fallo con: $intento" >> "$OUT/pin.txt"
    adb exec-out screencap -p > "$OUT/diag-pin-$intento.png"
  done
}

# Chrome reproduce un tono (el temporizador solo cuenta si hay audio sonando, como con YouTube)
preparar_chrome() {
  adb root >/dev/null 2>&1; sleep 3
  adb shell "echo '_ --disable-fre --no-first-run --autoplay-policy=no-user-gesture-required' > /data/local/tmp/chrome-command-line; chmod 755 /data/local/tmp/chrome-command-line"
  adb shell am set-debug-app --persistent com.android.chrome
  adb shell am force-stop com.android.chrome
  URL_TONO=$(python3 - <<'PY'
import wave, struct, math, io, base64, urllib.parse
b = io.BytesIO()
w = wave.open(b, "wb"); w.setnchannels(1); w.setsampwidth(1); w.setframerate(8000)
w.writeframes(bytes(int(128 + 40 * math.sin(2 * math.pi * 330 * i / 8000)) for i in range(8000))); w.close()
wav = base64.b64encode(b.getvalue()).decode()
html = '<body style="margin:0;background:#fff"><h1 style="font:40px sans-serif;margin:40vh 8vw">Video de ejemplo</h1><audio autoplay loop src="data:audio/wav;base64,' + wav + '"></audio>'
print("data:text/html," + urllib.parse.quote(html, safe=""))
PY
)
}
abrir_chrome_con_audio() { adb shell am start -a android.intent.action.VIEW -d "'$URL_TONO'" -p com.android.chrome >/dev/null; }

preparar_chrome
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell settings put secure show_ime_with_hard_keyboard 0
# Sin teclado en pantalla (input text no lo necesita)
for ime in $(adb shell ime list -s | tr -d "\r"); do adb shell ime disable "$ime" >/dev/null; done
# Barra de estado limpia
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1000 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null

# ---- Asistente (primer arranque) ----
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS 2>/dev/null
adb logcat -c
adb shell am start -W -n $PKG/.MainActivity | tee "$OUT/am-start.txt"
sleep 4
adb logcat -d -t 400 > "$OUT/logcat-inicio.txt"
adb shell dumpsys activity activities | grep -E "mResumedActivity|topResumedActivity" > "$OUT/resumed.txt"
$UI escribir "Mateo" 0
$UI escribir "7" 1
snap 01-asistente-nombre
$UI tocar - btnSiguiente        # intereses
sleep 1
$UI tocar - btnSiguiente        # apps
sleep 1
snap 02-asistente-apps

# ---- Estado de una familia ya configurada ----
# PIN de ejemplo (482915): mismo hash PBKDF2 que usa la app
eval "$(python3 - <<'PY'
import hashlib, base64, os
sal = os.urandom(16)
h = hashlib.pbkdf2_hmac("sha256", b"482915", sal, 120000, 32)
print("PINHASH=%s" % base64.b64encode(h).decode())
print("PINSAL=%s" % base64.b64encode(sal).decode())
PY
)"
adb shell am force-stop $PKG
adb shell "run-as $PKG mkdir -p shared_prefs"
adb shell "run-as $PKG sh -c 'cat > shared_prefs/tranquera_prefs.xml'" < <(sed -e "s/HOY/$(date +%F)/" -e "s#PINHASH#$PINHASH#" -e "s#PINSAL#$PINSAL#" <<'XML'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
  <boolean name="onboarding_hecho" value="true" />
  <string name="nombre_nino">Mateo</string>
  <int name="edad" value="7" />
  <int name="interval_seconds" value="20" />
  <string name="modo_apps">elegir</string>
  <set name="apps_elegidas"><string>com.android.chrome</string></set>
  <int name="aciertos_total" value="37" />
  <int name="aciertos_n" value="6" />
  <string name="pin_hash">PINHASH</string>
  <string name="pin_salt">PINSAL</string>
  <int name="pin_iter" value="120000" />
  <string name="aciertos_dia">HOY</string>
  <int name="hist_suma_ok" value="14" />
  <int name="hist_suma_err" value="3" />
  <int name="hist_resta_ok" value="9" />
  <int name="hist_resta_err" value="2" />
  <int name="hist_contar_ok" value="21" />
  <int name="hist_contar_err" value="1" />
</map>
XML
)
adb shell appops set $PKG GET_USAGE_STATS allow
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow

adb shell am start -n $PKG/.MainActivity
sleep 4
# La app pide el PIN de padres al abrir
ingresar_pin
snap 03-inicio

# ---- Ejercicio sobre una app cuidada (Ajustes hace de YouTube) ----
adb shell am start -a android.intent.action.VIEW -d about:blank -p com.android.chrome
sleep 30
snap 04-ejercicio

# ---- Progreso y Ajustes ----
adb shell am force-stop com.android.chrome
adb shell am start -n $PKG/.MainActivity
sleep 3
ingresar_pin
$UI tocar "Progreso"
snap 05-progreso
$UI tocar "Ajustes"
sleep 2
ingresar_pin
snap 06-ajustes


# ---- Diagnóstico: modo "todo menos lo que permito" (libre: solo Chrome) ----
adb shell am force-stop $PKG
adb shell "run-as $PKG cat shared_prefs/tranquera_prefs.xml" | sed -z \
  -e 's#<string name="modo_apps">elegir</string>#<string name="modo_apps">todo</string>#' \
  -e 's#<set name="apps_elegidas">[^<]*<string>[^<]*</string>[^<]*</set>#<set name="apps_libres"><string>com.android.chrome</string></set>#' > "$OUT/prefs-todo.xml"
adb shell "run-as $PKG sh -c 'cat > shared_prefs/tranquera_prefs.xml'" < "$OUT/prefs-todo.xml"
adb logcat -c
adb shell am start -n $PKG/.MainActivity; sleep 4
ingresar_pin
adb shell input keyevent KEYCODE_HOME; sleep 2
abrir_chrome_con_audio; sleep 35
snap diag-todo-chrome-libre
adb shell monkey -p com.google.android.apps.messaging -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 30
snap diag-todo-mensajes-vigilada
adb logcat -d -s Tranquera:D > "$OUT/logcat-todo.txt"
adb shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null
adb shell uiautomator dump /sdcard/final.xml >/dev/null; adb pull /sdcard/final.xml "$OUT/ultimo-ui.xml" >/dev/null 2>&1 || true
