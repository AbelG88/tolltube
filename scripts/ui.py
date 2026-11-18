#!/usr/bin/env python3
"""Ayudante de UI para el emulador: toca por texto o id y escribe en campos."""
import subprocess, sys, re, time
import xml.etree.ElementTree as ET

def sh(*a):
    return subprocess.run(["adb", *a], capture_output=True, text=True).stdout

def dump():
    sh("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    return ET.fromstring(sh("shell", "cat", "/sdcard/ui.xml"))

def centro(b):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", b))
    return (x1 + x2) // 2, (y1 + y2) // 2

def buscar(root, texto=None, rid=None, clase=None):
    for n in root.iter("node"):
        if texto and texto.lower() in (n.get("text") or "").lower(): return n
        if texto and texto.lower() in (n.get("content-desc") or "").lower(): return n
        if rid and (n.get("resource-id") or "").endswith(rid): return n
        if clase and n.get("class") == clase: return n
    return None

def esperar(texto=None, rid=None, clase=None, seg=25):
    t = time.time()
    while time.time() - t < seg:
        n = buscar(dump(), texto, rid, clase)
        if n is not None: return n
        time.sleep(1)
    print(f"NO ENCONTRADO: texto={texto} rid={rid} clase={clase}", file=sys.stderr)
    return None

def tocar(texto=None, rid=None, clase=None, seg=25):
    n = esperar(texto, rid, clase, seg)
    if n is None: return False
    x, y = centro(n.get("bounds"))
    sh("shell", "input", "tap", str(x), str(y))
    return True

def escribir(texto, rid=None, clase="android.widget.EditText", indice=0):
    editables = [n for n in dump().iter("node") if n.get("class") == clase]
    if len(editables) <= indice:
        print("NO HAY CAMPO", file=sys.stderr); return False
    x, y = centro(editables[indice].get("bounds"))
    sh("shell", "input", "tap", str(x), str(y)); time.sleep(0.5)
    if texto.isdigit():
        sh("shell", "input", "keyevent", *[str(7 + int(d)) for d in texto])
    else:
        sh("shell", "input", "text", texto.replace(" ", "%s"))
    time.sleep(0.5)
    return True

if __name__ == "__main__":
    c = sys.argv[1]
    if c == "tocar": sys.exit(0 if tocar(texto=sys.argv[2] if sys.argv[2] != "-" else None, rid=sys.argv[3] if len(sys.argv) > 3 else None) else 1)
    if c == "escribir": sys.exit(0 if escribir(sys.argv[2], indice=int(sys.argv[3]) if len(sys.argv) > 3 else 0) else 1)
    if c == "esperar": sys.exit(0 if esperar(texto=sys.argv[2], seg=int(sys.argv[3]) if len(sys.argv) > 3 else 25) is not None else 1)
