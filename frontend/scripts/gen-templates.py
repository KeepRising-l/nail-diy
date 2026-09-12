# -*- coding: utf-8 -*-
"""款式灵感库预设模板生成器：6 款可直接套用编辑的示例设计。
- designParams 与设计台 defaultFinger 结构一致，套用后可继续编辑（applyParams → mergeFinger）
- 封面 = 透明手模图 + 按模板 designParams 程序化上甲（复刻设计台的锚点/长度映射/甲缘遮罩/贴花几何），
  与设计台预览所见一致；渲染后裁 4:5
- 直接写入本地 MySQL 的 template 表（root，密码取 MYSQL_PASSWORD 环境变量，默认 1234），脚本幂等
用法：python frontend/scripts/gen-templates.py
"""
import os
import json
import math
import base64
import io
import urllib.request

try:
    import resvg_py
    import pymysql
    from PIL import Image
except ImportError as e:
    raise SystemExit(f'缺少依赖：{e}（pip install resvg-py pymysql pillow）')

HERE = os.path.dirname(__file__)
SEED = os.path.join(HERE, '..', '..', 'backend', 'uploads', 'seed')
HAND_PNG = os.path.join(HERE, '..', 'public', 'hands', 'hand-model-alpha.png')
W, H = 1278, 1230
CROP_X0, CROP_X1 = 147, 1131   # 4:5 裁剪（984×1230），以手部为中心
PATTERN_U = 28                 # 与 NailArt.vue PATTERN_U 一致
GOLD, GOLD_D, WHITE = '#C9A25E', '#A98545', '#FFFFFF'

def b64(path):
    with open(path, 'rb') as fh:
        return base64.b64encode(fh.read()).decode()

def api_materials(t):
    with urllib.request.urlopen(f'http://localhost:5199/api/materials?type={t}') as r:
        return json.load(r)['data'] or []

SHAPES = {m['name']: m['id'] for m in api_materials('SHAPE')}
COLORS = {m['name']: (m['id'], m.get('colorCode')) for m in api_materials('COLOR')}
PATTERNS = {m['name']: m['id'] for m in api_materials('PATTERN_IMG')}

def color_id(name): return COLORS[name][0]
def color_hex(name): return COLORS[name][1]

# ---- 手模逐指几何（与 HandModel.vue FINGERS 实测值同源；按 fingers 数组序：小指/无名指/中指/食指/拇指）----
GEOM = [
    dict(x=315.8, y=356, bedW=40, bedL=79,  angle=-5.5, tone='#F7D5C5'),   # 小指 idx0
    dict(x=876.0, y=262, bedW=57, bedL=98,  angle=18.9, tone='#F7D6C5'),   # 无名指 idx1
    dict(x=708.8, y=150, bedW=58, bedL=117, angle=11.4, tone='#F6D2C0'),   # 中指 idx2
    dict(x=535.0, y=206, bedW=50, bedL=93,  angle=9.5,  tone='#F7D5C4'),   # 食指 idx3
    dict(x=983.3, y=714, bedW=70, bedL=90,  angle=38.8, tone='#EBBFAC'),   # 拇指 idx4
]
NAIL_LAT_W = 44
ROOT_CENTER_Y = 7
DEFAULT_SLOTS = [(21, 26), (30, 40), (18, 48)]

def f1(v): return round(float(v), 1)

def contour(code, k=1.0):
    """与 NailArt.vue CONTOURS 同源；t=76k+4"""
    t = 76 * k + 4
    m = f1((4 + t) / 2)
    if code == 'round':
        return f'M 9 4 C 7.5 {m} 9.5 {f1(t-14)} 16 {f1(t-6)} Q 30 {f1(t+7)} 44 {f1(t-6)} C 50.5 {f1(t-14)} 52.5 {m} 51 4 C 41 7.5 19 7.5 9 4 Z'
    if code == 'squoval':
        return f'M 9.5 4 C 8 {m} 10 {f1(t-12)} 17 {f1(t-5)} Q 30 {f1(t+4)} 43 {f1(t-5)} C 50 {f1(t-12)} 52 {m} 50.5 4 C 41 7.5 19 7.5 9.5 4 Z'
    if code == 'square':
        return f'M 9.5 4 L 9.5 {f1(t-7)} Q 9.5 {t} 15.5 {t} L 44.5 {t} Q 50.5 {t} 50.5 {f1(t-7)} L 50.5 4 C 41 7.5 19 7.5 9.5 4 Z'
    if code == 'oval':
        return f'M 10 4 C 8 {m} 11.5 {f1(t-10)} 19 {f1(t-3)} Q 30 {f1(t+5)} 41 {f1(t-3)} C 48.5 {f1(t-10)} 52 {m} 50 4 C 40.5 7.5 19.5 7.5 10 4 Z'
    if code == 'almond':
        return f'M 10.5 4 C 8.5 {m} 15 {f1(t-12)} 25 {f1(t-4)} Q 30 {t} 35 {f1(t-4)} C 45 {f1(t-12)} 51.5 {m} 49.5 4 C 40 7.5 20 7.5 10.5 4 Z'
    if code == 'ballerina':
        return (f'M 9 4 C 8 {m} 11 {f1(t-13)} 17 {f1(t-5.5)} L 21.5 {f1(t-1.5)} Q 23 {t} 25 {t} L 35 {t} '
                f'Q 37 {t} 38.5 {f1(t-1.5)} L 43 {f1(t-5.5)} C 49 {f1(t-13)} 52 {m} 51 4 C 41 7.5 19 7.5 9 4 Z')
    raise ValueError(code)

def len_fit(length, bedL, s):
    """与 HandModel.vue slotLenFit 同源的分段映射"""
    l = min(1.6, max(0.6, length))
    d_bed = (1.02 + 0.35 * (l - 0.6)) if l <= 1 else (1.16 + 2.0667 * (l - 1.0))
    return min(2.8, max(0.6, (d_bed * bedL / s + 3) / 76))

def nail_markup(fi, spec, clip_id, decal_b64s):
    """单指甲片 SVG（复刻设计台渲染：轮廓/长度/法式/贴花），坐标为 60×H 画布单位"""
    code, k, hexc = spec['code'], spec['k'], spec['hex']
    t = 76 * k + 4
    path = contour(code, k)
    g = (f'<clipPath id="{clip_id}"><path d="{path}"/></clipPath>')
    body = f'<path d="{path}" fill="{hexc}"/>'
    inner = ''
    if spec.get('french'):
        h = 26
        top_y, tip_y, lift = t - h, t, 72 / 100 * 16
        inner += (f'<path d="M -2 {f1(top_y)} Q 30 {f1(top_y - lift)} 62 {f1(top_y)} '
                  f'L 62 {f1(tip_y + 6)} L -2 {f1(tip_y + 6)} Z" fill="{WHITE}" fill-opacity="0.92"/>')
    body += f'<g clip-path="url(#{clip_id})">{inner}</g>'
    # 贴花：位置百分比与 NailArt patXY 同规则（默认槽位 j%3）
    for j, pid in enumerate(spec.get('patterns', [])):
        data = decal_b64s.get(pid)
        if not data:
            continue
        px, py = DEFAULT_SLOTS[j % 3]
        x = px / 100 * 60 - PATTERN_U / 2
        y = py / 100 * (t + 4) - PATTERN_U / 2
        body += (f'<image href="data:image/png;base64,{data}" x="{f1(x)}" y="{f1(y)}" '
                 f'width="{PATTERN_U}" height="{PATTERN_U}" preserveAspectRatio="none"/>')
    return g, body

def hand_cover_svg(template):
    """手模合成封面：底色渐变 + 手 + 逐指甲片 + 甲缘遮罩（与设计台同几何）"""
    fingers = template['fingers']
    decal_ids = sorted({pid for f in fingers for pid in f['patternIds']})
    id2file = {m['id']: os.path.basename(m['imageUrl']) for m in api_materials('PATTERN_IMG')}
    decal_b64s = {pid: b64(os.path.join(SEED, id2file[pid])) for pid in decal_ids if pid in id2file}
    hand_b64 = b64(HAND_PNG)
    defs = (f'<linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">'
            f'<stop offset="0" stop-color="#E8DFD0"/><stop offset="1" stop-color="#D9CEBA"/></linearGradient>'
            f'<filter id="nsh" x="-40%" y="-40%" width="180%" height="180%">'
            f'<feDropShadow dx="0" dy="2" stdDeviation="1.6" flood-color="#785032" flood-opacity="0.35"/></filter>'
            f'<filter id="soft" x="-80%" y="-80%" width="260%" height="260%">'
            f'<feGaussianBlur stdDeviation="2.4"/></filter>')
    body = (f'<rect width="{W}" height="{H}" fill="url(#bg)"/>'
            f'<image href="data:image/png;base64,{hand_b64}" x="0" y="0" width="{W}" height="{H}"/>')
    clips = ''
    for fi, spec in enumerate(template['specs']):
        g = GEOM[fi]
        rad = math.radians(g['angle'])
        s = (g['bedW'] + 3) / NAIL_LAT_W
        k = len_fit(spec['length'], g['bedL'], s)
        back = ROOT_CENTER_Y * s
        ax, ay = g['x'] - math.sin(rad) * back, g['y'] + math.cos(rad) * back
        clip_id = f'nc{fi}'
        clip, nail = nail_markup(fi, dict(code=spec['shape_code'], k=k, hex=spec['hex'],
                                          french=spec['french'], patterns=spec['pattern_ids']),
                                 clip_id, decal_b64s)
        clips += clip
        body += (f'<g transform="translate({ax:.1f} {ay:.1f}) rotate({g["angle"] + 180}) scale({s:.3f}) '
                 f'translate(-30 0)" filter="url(#nsh)">{nail}</g>')
    # 甲缘遮罩（z4，盖住甲根）
    for fi, spec in enumerate(fingers):
        g = GEOM[fi]
        u = (g['bedW'] + 3) / NAIL_LAT_W
        half = u * (NAIL_LAT_W / 2 + 0.2)
        end, tip_c, palm_c = -2.5 * u, 4.5 * u, -9.5 * u
        body += (f'<g transform="translate({g["x"]} {g["y"]}) rotate({g["angle"] + 180})">'
                 f'<path d="M {-half} {end} Q 0 {tip_c} {half} {end} Q 0 {palm_c} {-half} {end} Z" '
                 f'fill="{g["tone"]}" filter="url(#soft)"/>'
                 f'<path d="M {-half*0.9} {-1.6*u} Q 0 {5.4*u} {half*0.9} {-1.6*u}" fill="none" '
                 f'stroke="rgba(163,102,72,0.20)" stroke-width="{u*1.2}" filter="url(#soft)"/></g>')
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" '
            f'xmlns:xlink="http://www.w3.org/1999/xlink"><defs>{clips}{defs}</defs>{body}</svg>')

def finger(shape, length, color, patterns=None, french=False, style=None):
    return {
        'shapeId': SHAPES[shape], 'length': length, 'colorId': color_id(color),
        'overlays': [], 'patternIds': [PATTERNS[p] for p in (patterns or [])],
        'gradient': {'enabled': False, 'color2': '', 'position': 50},
        'catEye': {'enabled': False, 'angle': 25, 'width': 34, 'brightness': 72},
        'adjust': {'hue': 0, 'sat': 0, 'light': 0},
        'french': {'enabled': french, 'color': '#FFFFFF', 'height': 26, 'curvature': 72},
        'dots': {'enabled': False, 'kind': 'dots', 'density': 7, 'size': 2.5, 'color': ''},
        'patternStyle': ({"code": style, "params": {}} if style else None),
        'decorations': [], 'patternPos': {}
    }

# ---- 6 款预设（ fingers 序 = 小指/无名指/中指/食指/拇指，与 GEOM 对齐）----
def spec_of(shape, length, color, french=False, pattern_names=None):
    """生成封面渲染需要的 spec（形状代码/长度/色值/法式/贴花 id 列表）"""
    return dict(
        shape_code=shape, length=length, hex=color_hex(color), french=french,
        pattern_ids=[PATTERNS[p] for p in (pattern_names or [])]
    )

TEMPLATES = [
    dict(name='奶杏圆舞', tag='简约', price=39, file='tpl_01.png',
         fingers=[finger('圆形', 1.0, '奶霜白') for _ in range(5)],
         specs=[spec_of('round', 1.0, '奶霜白')] * 5),
    dict(name='樱花物语', tag='甜美', price=45, file='tpl_02.png',
         fingers=[finger('椭圆形', 1.05, '芭蕾粉', ['叶子']),
                  finger('椭圆形', 1.05, '芭蕾粉'),
                  finger('椭圆形', 1.05, '芭蕾粉', ['樱花']),
                  finger('椭圆形', 1.05, '樱花粉', ['樱花', '小花']),
                  finger('椭圆形', 1.05, '芭蕾粉')],
         specs=[spec_of('oval', 1.05, '芭蕾粉', pattern_names=['叶子']),
                spec_of('oval', 1.05, '芭蕾粉'),
                spec_of('oval', 1.05, '芭蕾粉', pattern_names=['樱花']),
                spec_of('oval', 1.05, '樱花粉', pattern_names=['樱花', '小花']),
                spec_of('oval', 1.05, '芭蕾粉')]),
    dict(name='金色月影', tag='轻奢', price=49, file='tpl_03.png',
         fingers=[finger('杏仁形', 1.25, '裸杏'),
                  finger('杏仁形', 1.25, '裸杏', ['星星']),
                  finger('杏仁形', 1.25, '裸杏', ['月亮']),
                  finger('杏仁形', 1.25, '裸杏', ['金箔']),
                  finger('杏仁形', 1.25, '裸杏')],
         specs=[spec_of('almond', 1.25, '裸杏'),
                spec_of('almond', 1.25, '裸杏', pattern_names=['星星']),
                spec_of('almond', 1.25, '裸杏', pattern_names=['月亮']),
                spec_of('almond', 1.25, '裸杏', pattern_names=['金箔']),
                spec_of('almond', 1.25, '裸杏')]),
    dict(name='法式奶盖', tag='经典', price=42, file='tpl_04.png',
         fingers=[finger('方圆形', 1.0, '奶霜白', french=True) for _ in range(5)],
         specs=[spec_of('squoval', 1.0, '奶霜白', french=True)] * 5),
    dict(name='玫瑰心事', tag='甜美', price=45, file='tpl_05.png',
         fingers=[finger('圆形', 1.0, '蜜桃粉', ['蝴蝶结']),
                  finger('圆形', 1.0, '蜜桃粉', ['爱心']),
                  finger('圆形', 1.0, '蜜桃粉'),
                  finger('圆形', 1.0, '樱花粉', ['玫瑰']),
                  finger('圆形', 1.0, '蜜桃粉', ['玫瑰'])],
         specs=[spec_of('round', 1.0, '蜜桃粉', pattern_names=['蝴蝶结']),
                spec_of('round', 1.0, '蜜桃粉', pattern_names=['爱心']),
                spec_of('round', 1.0, '蜜桃粉'),
                spec_of('round', 1.0, '樱花粉', pattern_names=['玫瑰']),
                spec_of('round', 1.0, '蜜桃粉', pattern_names=['玫瑰'])]),
    dict(name='芭蕾极简', tag='轻奢', price=55, file='tpl_06.png',
         fingers=[finger('芭蕾形', 1.5, '焦糖奶咖', ['条纹']),
                  finger('芭蕾形', 1.5, '焦糖奶咖'),
                  finger('芭蕾形', 1.5, '焦糖奶咖', ['金箔']),
                  finger('芭蕾形', 1.5, '焦糖奶咖', ['波点贴']),
                  finger('芭蕾形', 1.5, '焦糖奶咖')],
         specs=[spec_of('ballerina', 1.5, '焦糖奶咖', pattern_names=['条纹']),
                spec_of('ballerina', 1.5, '焦糖奶咖'),
                spec_of('ballerina', 1.5, '焦糖奶咖', pattern_names=['金箔']),
                spec_of('ballerina', 1.5, '焦糖奶咖', pattern_names=['波点贴']),
                spec_of('ballerina', 1.5, '焦糖奶咖')]),
]

def render_cover(t):
    png = resvg_py.svg_to_bytes(svg_string=hand_cover_svg(t), width=W, height=H)
    im = Image.open(io.BytesIO(bytes(png))).convert('RGB')
    im = im.crop((CROP_X0, 0, CROP_X1, H))
    im.save(os.path.join(SEED, t['file']))

def main():
    conn = pymysql.connect(host='localhost', port=3306, user='root',
                           password=os.environ.get('MYSQL_PASSWORD', '1234'), database='nail_diy')
    cur = conn.cursor()
    for i, t in enumerate(TEMPLATES):
        render_cover(t)
        params = json.dumps({'fingers': t['fingers'], 'remark': '灵感模板，可自由修改'}, ensure_ascii=False)
        cur.execute(
            'INSERT INTO template (name, style_tag, cover_url, design_params, price, sort, status, deleted) '
            'VALUES (%s, %s, %s, %s, %s, %s, 1, 0)',
            (t['name'], t['tag'], f"/uploads/seed/{t['file']}", params, t['price'], i))
        print('seeded', t['name'], '->', t['file'])
    # 幂等：同名模板保留最新一条
    cur.execute('DELETE t1 FROM template t1, template t2 WHERE t1.name = t2.name AND t1.id < t2.id')
    conn.commit()
    cur.execute('SELECT COUNT(*) FROM template')
    print('template rows now:', cur.fetchone()[0])
    conn.close()

if __name__ == '__main__':
    main()
