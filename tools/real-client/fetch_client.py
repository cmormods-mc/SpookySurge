#!/usr/bin/env python3
"""Downloads Minecraft 1.21.1's client jar, libraries and asset index into a directory."""
import json, os, sys, urllib.request

def get(url):
    with urllib.request.urlopen(url, timeout=60) as r:
        return r.read()

def allowed(rules):
    if not rules:
        return True
    ok = False
    for r in rules:
        if r.get('os', {}).get('name') in (None, 'linux'):
            ok = (r['action'] == 'allow')
    return ok

def main(dest):
    os.makedirs(os.path.join(dest, 'libs'), exist_ok=True)
    manifest = json.loads(get('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
    entry = next(v for v in manifest['versions'] if v['id'] == '1.21.1')
    version = json.loads(get(entry['url']))
    paths = []
    for lib in version['libraries']:
        if not allowed(lib.get('rules')):
            continue
        art = lib['downloads'].get('artifact')
        if art:
            p = os.path.join('libs', art['path'].replace('/', '__'))
            if not os.path.exists(os.path.join(dest, p)):
                open(os.path.join(dest, p), 'wb').write(get(art['url']))
            paths.append(p)
    # One path per line, with a trailing newline so the launcher can join them safely.
    open(os.path.join(dest, 'libs.txt'), 'w').write('\n'.join(paths) + '\n')
    jar = os.path.join(dest, 'client.jar')
    if not os.path.exists(jar):
        open(jar, 'wb').write(get(version['downloads']['client']['url']))
    index = version['assetIndex']
    os.makedirs(os.path.join(dest, 'assets', 'indexes'), exist_ok=True)
    open(os.path.join(dest, 'assets', 'indexes', index['id'] + '.json'), 'wb').write(get(index['url']))
    print('ok:', len(paths), 'libraries, asset index', index['id'])

if __name__ == '__main__':
    main(sys.argv[1] if len(sys.argv) > 1 else 'client')
