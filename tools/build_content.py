"""يبني ملف المحتوى app/src/main/assets/content.json من ملفات tools/content/*.py ويتحقق منه.
التشغيل: python3 tools/build_content.py
"""
import json, os, sys, importlib.util

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ORDER = ["a1", "a2", "b1", "b2", "c1"]

def load(name):
    spec = importlib.util.spec_from_file_location(name, os.path.join(ROOT, "tools", "content", name + ".py"))
    m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
    return m.LEVEL

def t3(t): return {"ar": t[0], "fr": t[1], "en": t[2]}

errors = []
def check(cond, msg):
    if not cond: errors.append(msg)

levels = []
for name in ORDER:
    L = load(name)
    units = []
    for u in L["units"]:
        uid = u["id"]
        emojis = [w[1] for w in u["vocab"]]
        check(len(set(emojis)) == len(emojis), f"{uid}: duplicate picture {emojis}")
        words = [w[0] for w in u["vocab"]]
        check(len(set(words)) == len(words), f"{uid}: duplicate word")
        check(len(u["vocab"]) >= 8, f"{uid}: needs 8+ words")
        g = u["grammar"]; d = u["dialogue"]
        for q in g["choice"] + d["questions"]:
            check(0 <= q[2] < len(q[1]), f"{uid}: bad answer index {q}")
            check(len(set(q[1])) == len(q[1]), f"{uid}: duplicate options {q}")
        for q in g["choice"]:
            check("___" in q[0], f"{uid}: blank missing {q[0]}")
        for s in g["order"]:
            check(3 <= len(s.split()) <= 9, f"{uid}: order length {s}")
        units.append({
            "id": uid, "emoji": u["emoji"], "title": t3(u["title"]),
            "vocab": [{"en": w[0], "pic": w[1], "ar": w[2], "fr": w[3]} for w in u["vocab"]],
            "grammar": {
                "title": t3(g["title"]), "explain": t3(g["explain"]),
                "examples": [{"en": e[0], "ar": e[1], "fr": e[2]} for e in g["examples"]],
                "choice": [{"q": c[0], "options": c[1], "answer": c[2]} for c in g["choice"]],
                "order": g["order"],
            },
            "dialogue": {
                "title": t3(d["title"]), "scene": d["scene"],
                "lines": [{"who": l[0], "en": l[1], "ar": l[2], "fr": l[3]} for l in d["lines"]],
                "questions": [{"q": c[0], "options": c[1], "answer": c[2]} for c in d["questions"]],
            },
        })
    levels.append({"id": L["id"], "emoji": L["emoji"], "title": t3(L["title"]), "units": units})

if errors:
    print("\n".join(errors)); sys.exit(1)
out = os.path.join(ROOT, "app", "src", "main", "assets", "content.json")
with open(out, "w", encoding="utf-8") as f:
    json.dump({"version": 1, "levels": levels}, f, ensure_ascii=False, separators=(",", ":"))
n_units = sum(len(l["units"]) for l in levels)
n_words = sum(len(u["vocab"]) for l in levels for u in l["units"])
print(f"OK: {len(levels)} levels, {n_units} units, {n_words} words -> {os.path.getsize(out)} bytes")
