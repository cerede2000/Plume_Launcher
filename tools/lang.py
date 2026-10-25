#!/usr/bin/env python3
"""Plume translations.

  tools/lang.py check [--strict] [--table]  what each app/src/main/res/values-<code>/strings.xml lacks, has by mistake, or writes with other
                                            markers than English (the way translations are sent upstream)
  tools/lang.py packs [--strict]            the same for this fork's language packs lang/<code>.txt ("key<TAB>text", plurals as
                                            "name:quantity"): the keys that are missing and the keys that are unknown
  tools/lang.py new <code> <Name>           starts a pack: an empty lang/<code>.txt and its line in lang/index.txt
  tools/lang.py import <code>               writes lang/<code>.txt from app/src/main/res/values-<code>/strings.xml

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


def read_index():
    out = {}
    path = os.path.join(ROOT, 'lang/index.txt')
    if os.path.exists(path):
        for line in open(path, encoding='utf-8').read().split('\n'):
            if line and not line.startswith('#'):
                code, sep, name = line.partition('\t')
                if sep and name.strip(): out[code.strip()] = name.strip()
    return out


def do_new(code, name):
    if not re.fullmatch(r'[a-z]{2,3}(-[A-Za-z0-9]{2,8})*', code): raise SystemExit(f'{code!r} is not a language code (fr, pt-BR…)')
    pack = os.path.join(ROOT, f'lang/{code}.txt')
    if os.path.exists(pack) or code in read_index(): raise SystemExit(f'{code} exists already')
    open(pack, 'w', encoding='utf-8').write(f'# {name}: one "key<TAB>text" line per string, keys as in values/strings.xml. A key left out shows in English.\n')
    with open(os.path.join(ROOT, 'lang/index.txt'), 'a', encoding='utf-8') as f: f.write(f'{code}\t{name}\n')
    print(f'lang/{code}.txt started; add the texts, then run: tools/lang.py check')


def check_packs(strict):
    en = read_values(os.path.join(RES, 'values/strings.xml'))
    en_plural_names = {k.split(':')[0] for k in en if ':' in k}
    bad = 0
    index = read_index()
    files = {os.path.basename(p)[:-4] for p in glob.glob(os.path.join(ROOT, 'lang/*.txt')) if not p.endswith('index.txt')}
    for code in sorted(files - set(index)): print(f'lang/index.txt has no line for {code}'); bad += 1
    for code in sorted(set(index) - files): print(f'lang/index.txt lists {code}, which has no file'); bad += 1
    for path in sorted(p for p in glob.glob(os.path.join(ROOT, 'lang/*.txt')) if not p.endswith('index.txt')):
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


MARKER = re.compile(r'%(?:\d+\$)?[sdfx]')


def read_raw(path):
    """key -> raw text for the strings and plurals of one strings.xml; plurals as name:quantity."""
    out = {}
    root = ET.parse(path).getroot()
    for e in root.findall('string'):
        if e.get('translatable') != 'false':
            out[e.get('name')] = ''.join(e.itertext())
    for p in root.findall('plurals'):
        for i in p.findall('item'):
            out[p.get('name') + ':' + i.get('quantity')] = ''.join(i.itertext())
    return out


def wrong_markers(key, text, en):
    """A string must have the markers of its English text. A plural form, whatever its quantity (zero, two, few, many…),
    is held to English's "other" of the same plural: it may leave the number out ("one photo"), but must not add a marker."""
    if ':' in key:
        other = en.get(key.split(':')[0] + ':other')
        return other is not None and bool(set(MARKER.findall(text)) - set(MARKER.findall(other)))
    return key in en and sorted(MARKER.findall(text)) != sorted(MARKER.findall(en[key]))


def check_res(strict, table):
    en = read_raw(os.path.join(RES, 'values', 'strings.xml'))
    # A language may use other plural quantities than English: only the strings are required.
    required = [k for k in en if ':' not in k]
    plural_names = sorted({k.split(':')[0] for k in en if ':' in k})
    rows, bad = [], 0
    for path in sorted(glob.glob(os.path.join(RES, 'values-*', 'strings.xml'))):
        code = os.path.basename(os.path.dirname(path))[len('values-'):]
        tr = read_raw(path)
        # A plural counts as missing when the translation has none of its forms (English has one and other; a language
        # may have more, or other quantities).
        missing = [k for k in required if k not in tr] + [n for n in plural_names if not any(k.startswith(n + ':') for k in tr)]
        unknown = [k for k in tr if k not in en and k.split(':')[0] not in plural_names]
        markers = [k for k in tr if wrong_markers(k, tr[k], en)]
        rows.append((code, len(tr), missing, unknown, markers))
        if unknown or markers:
            bad += 1
    if table:
        print('| Language | Keys | Missing | Unknown | Wrong markers |')
        print('|---|---|---|---|---|')
        for code, n, missing, unknown, markers in rows:
            cell = lambda ks: ', '.join(f'`{k}`' for k in ks) or '-'
            print(f'| {code} | {n} | {cell(missing)} | {cell(unknown)} | {cell(markers)} |')
    else:
        for code, n, missing, unknown, markers in rows:
            print(f'values-{code}: {n} keys, {len(missing)} missing, {len(unknown)} unknown, {len(markers)} with other markers')
            for label, ks in (('missing', missing), ('unknown', unknown), ('markers', markers)):
                if ks:
                    print(f'   {label}: ' + ', '.join(ks))
    if strict and bad:
        sys.exit(1)


if __name__ == '__main__':
    a = sys.argv[1:]
    if a[:1] == ['check']: check_res('--strict' in a, '--table' in a)
    elif a[:1] == ['packs']: check_packs('--strict' in a)
    elif a[:1] == ['import'] and len(a) == 2: do_import(a[1])
    elif a[:1] == ['new'] and len(a) == 3: do_new(a[1], a[2])
    else: print(__doc__); sys.exit(2)
