#!/usr/bin/env python3
"""Plume language packs: lang/<code>.txt, one "key<TAB>text" line per string, plurals as "name:quantity".

  tools/lang.py check [--strict]   lists, for each pack, the keys that are missing and the keys that are unknown
  tools/lang.py import <code>      writes lang/<code>.txt from app/src/main/res/values-<code>/strings.xml (one time)

The English text lives in app/src/main/res/values/strings.xml. A key that a pack lacks shows in English.
"""
import re, sys, glob, os
import xml.etree.ElementTree as ET

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
RES = os.path.join(ROOT, 'app/src/main/res')


def unescape_android(t):
    out, i = [], 0
    while i < len(t):
        c = t[i]
        if c == '\\' and i + 1 < len(t):
            n = t[i + 1]
            if n == 'n': out.append('\n')
            elif n == 'u' and re.match(r'[0-9a-fA-F]{4}', t[i + 2:i + 6]):
                out.append(chr(int(t[i + 2:i + 6], 16))); i += 4
            else: out.append(n)
            i += 2
        else:
            out.append(c); i += 1
    return ''.join(out)


def escape_pack(t):
    return t.replace('\\', '\\\\').replace('\n', '\\n').replace('\t', ' ')


def read_values(path):
    """key -> text for the strings and plurals of one strings.xml (Android escapes removed)."""
    out = {}
    root = ET.parse(path).getroot()
    for e in root.findall('string'):
        if e.get('translatable') == 'false': continue
        out[e.get('name')] = unescape_android(''.join(e.itertext()))
    for p in root.findall('plurals'):
        for i in p.findall('item'):
            out[p.get('name') + ':' + i.get('quantity')] = unescape_android(''.join(i.itertext()))
    return out


def read_pack(path):
    out = {}
    for n, line in enumerate(open(path, encoding='utf-8').read().split('\n'), 1):
        if not line or line.startswith('#'): continue
        k, sep, v = line.partition('\t')
        if not sep: raise SystemExit(f'{path}:{n}: no tab')
        out[k] = v
    return out


def check(strict):
    en = read_values(os.path.join(RES, 'values/strings.xml'))
    en_plural_names = {k.split(':')[0] for k in en if ':' in k}
    bad = 0
    for path in sorted(glob.glob(os.path.join(ROOT, 'lang/*.txt'))):
        pack = read_pack(path)
        name = os.path.basename(path)
        known = set(en) | {k for k in pack if ':' in k and k.split(':')[0] in en_plural_names}
        missing = [k for k in en if k not in pack and ':' not in k]
        unknown = [k for k in pack if k not in known]
        print(f'{name}: {len(pack)} keys, {len(missing)} missing, {len(unknown)} unknown')
        if missing: print('   missing:', ', '.join(missing))
        if unknown: print('   unknown:', ', '.join(unknown))
        if unknown: bad += 1
    if strict and bad: sys.exit(1)


def do_import(code):
    src = os.path.join(RES, f'values-{code}/strings.xml')
    vals = read_values(src)
    with open(os.path.join(ROOT, f'lang/{code}.txt'), 'w', encoding='utf-8') as f:
        for k, v in vals.items(): f.write(f'{k}\t{escape_pack(v)}\n')
    print(code, len(vals), 'keys')


if __name__ == '__main__':
    a = sys.argv[1:]
    if a[:1] == ['check']: check('--strict' in a)
    elif a[:1] == ['import'] and len(a) == 2: do_import(a[1])
    else: print(__doc__); sys.exit(2)
