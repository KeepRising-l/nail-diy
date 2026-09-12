# -*- coding: utf-8 -*-
"""款式灵感库预设模板生成器：6 款可直接套用编辑的示例设计。
- designParams 与设计台 defaultFinger 结构一致，套用后可继续编辑（applyParams → mergeFinger）
- 封面用 resvg 按真实甲型轮廓程序化绘制（五指扇形排布 + 每指底色/法式/贴花点缀），无手模图
- 直接写入本地 MySQL 的 template 表（root，密码取 MYSQL_PASSWORD 环境变量，默认 1234）
用法：python frontend/scripts/gen-templates.py
"""
import os
import json
import math
import urllib.request

try:
    import resvg_py
    import pymysql
except ImportError as e:
    raise SystemExit(f'缺少依赖：{e}（pip install resvg-py pymysql）')

HERE = os.path.dirname(__file__)
SEED = os.path.join(HERE, '..', '..', 'backend', 'uploads', 'seed')

# ---- 素材目录（公共接口实时取，保证 id 与库中一致）----
def api_materials(t):
    with urllib.request.urlopen(f'http://localhost:5199/api/materials?type={t}') as r:
        return json.load(r)['data'] or []

SHAPES = {m['name']: m['id'] for m in api_materials('SHAPE')}
COLORS = {m['name']: (m['id'], m.get('colorCode')) for m in api_materials('COLOR')}
PATTERNS = {m['name']: m['id'] for m in api_materials('PATTERN_IMG')}

def color_id(name): return COLORS[name][0]
def color_hex(name): return COLORS[name][1]

# ---- 甲型轮廓（与 NailArt.vue CONTOURS 同源，画布 60 宽、根缘 y≈4-8、尖端 y=76k+4）----
def f1(v): return round(float(v), 1)
def contour(code, k=1.0):
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

GOLD, GOLD_D, WHITE, ROSE = '#C9A25E', '#A98545', '#FFFFFF', '#E8B7B0'

def accent_svg(kind, u):
    """指甲面上的装饰点缀（局部坐标：30 为甲面中轴，t 尖端 y）——按 1 单位=u px 缩放前先按单位画"""
    if kind == 'french':
        t = 80
        return (f'<path d="M 13 {t-13} Q 30 {t-19} 47 {t-13} L 47 {t-4} Q 30 {t+3} 13 {t-4} Z" '
                f'fill="{WHITE}" fill-opacity="0.92"/>')
    if kind == 'dots':
        return ''.join(f'<circle cx="{30+dx}" cy="{34+dy}" r="2.6" fill="{GOLD}"/>'
                       for dx, dy in [(-7, -6), (0, 2), (7, -2), (-3, 12), (6, 12)])
    if kind == 'sakura':
        cx, cy = 30, 40
        p = ''.join(f'<circle cx="{cx+math.cos(math.radians(a))*6:.1f}" cy="{cy+math.sin(math.radians(a))*6:.1f}" '
                    f'r="3.4" fill="#F6CBD6"/>' for a in range(0, 360, 72))
        return p + f'<circle cx="{cx}" cy="{cy}" r="2.2" fill="{GOLD}"/>'
    if kind == 'moon':
        return (f'<path d="M 30 30 a 9 9 0 1 0 7 15 a 7.2 7.2 0 1 1 -7 -15 Z" fill="{GOLD}"/>'
                f'<path d="M 38 44 l 0 6 m -3 -3 l 6 0" stroke="{GOLD}" stroke-width="1.6" stroke-linecap="round"/>')
    if kind == 'star':
        cx, cy = 30, 38
        return (f'<path d="M {cx} {cy-9} C {cx+1.4} {cy-2.4} {cx+2.4} {cy-1.4} {cx+9} {cy} '
                f'C {cx+2.4} {cy+1.4} {cx+1.4} {cy+2.4} {cx} {cy+9} '
                f'C {cx-1.4} {cy+2.4} {cx-2.4} {cy+1.4} {cx-9} {cy} '
                f'C {cx-2.4} {cy-1.4} {cx-1.4} {cy-2.4} {cx} {cy-9} Z" fill="{GOLD}"/>'
                f'<circle cx="{cx+11}" cy="{cy-11}" r="1.8" fill="{GOLD}"/>')
    if kind == 'foil':
        return ''.join(f'<polygon points="{x:.0f},{y-8:.0f} {x+3:.0f},{y-2:.0f} {x:.0f},{y+7:.0f} {x-3:.0f},{y-1:.0f}" '
                       f'fill="{GOLD}" fill-opacity="{op}" transform="rotate({r} {x:.0f} {y:.0f})"/>'
                       for x, y, r, op in [(22, 32, -20, 0.95), (36, 44, 26, 0.8), (30, 22, 8, 0.7)])
    if kind == 'rose':
        cx, cy = 30, 38
        return (f'<circle cx="{cx}" cy="{cy}" r="8.5" fill="#EFB9C4" fill-opacity="0.55"/>'
                f'<path d="M {cx} {cy} c 3 -2 6 0 5.5 3 c -0.6 3.4 -5 4.4 -7.6 2 c -3.4 -2.9 -2 -8.4 2 -10 '
                f'c 5.2 -2.2 11 1 11.2 6.4" fill="none" stroke="{GOLD_D}" stroke-width="1.5" stroke-linecap="round"/>')
    if kind == 'heart':
        return (f'<path d="M 30 34 c -3.4 -3.2 -8 -2.4 -8 1 c 0 2.8 4 5.8 8 8.6 c 4 -2.8 8 -5.8 8 -8.6 '
                f'c 0 -3.4 -4.6 -4.2 -8 -1 Z" fill="none" stroke="{GOLD_D}" stroke-width="1.5"/>')
    if kind == 'bow':
        return (f'<path d="M 26 36 L 20 32 L 21.5 39 Z M 34 36 L 40 32 L 38.5 39 Z" '
                f'fill="{ROSE}" fill-opacity="0.9"/>'
                f'<circle cx="30" cy="36.5" r="2.6" fill="{GOLD}"/>')
    if kind == 'stripe':
        return (f'<line x1="24" y1="20" x2="24" y2="72" stroke="{GOLD}" stroke-width="1.4" stroke-opacity="0.75"/>'
                f'<line x1="36" y1="20" x2="36" y2="72" stroke="{GOLD}" stroke-width="1.4" stroke-opacity="0.45"/>')
    return ''

def nail_group(code, k, fill, u, x, y, rot, accent=None):
    """一根指甲：<g> 平移/旋转到封面坐标；u=px/单位，根缘中心放在 (x,y)"""
    body = (f'<path d="{contour(code, k)}" fill="{fill}" stroke="rgba(150,105,70,0.35)" stroke-width="0.8"/>'
            + (f'<g>{accent_svg(accent, u)}</g>' if accent else ''))
    # 根缘中心 (30,7) 对齐锚点
    return (f'<g transform="translate({x} {y}) rotate({rot}) scale({u}) translate(-30 -7)">{body}</g>')

def cover_svg(name, fingers_spec):
    """600×750 款式卡：米色底 + 五指扇形排布"""
    fan = [  # (甲型, 长度k, u, x, y, 旋转, 底色hex, 点缀)
    ]
    layouts = [(-16, 96, 508, 1.62), (-8, 214, 546, 1.82), (0, 316, 566, 1.96), (11, 414, 544, 1.90), (42, 522, 490, 2.34)]
    defs = ('<defs><linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">'
            '<stop offset="0" stop-color="#F7F0E5"/><stop offset="1" stop-color="#EAE0CE"/></linearGradient>'
            '<filter id="sh" x="-60%" y="-60%" width="220%" height="220%">'
            '<feGaussianBlur stdDeviation="6"/></filter></defs>')
    body = '<rect width="600" height="750" fill="url(#bg)"/>'
    for (rot, x, y, u), spec in zip(layouts, fingers_spec):
        code, k, color, accent = spec
        hexc = color_hex(color)
        # 根部投影
        body += f'<ellipse cx="{x}" cy="{y + 16*u:.0f}" rx="{30*u:.0f}" ry="{7*u:.0f}" fill="rgba(120,90,60,0.20)" filter="url(#sh)"/>'
        body += nail_group(code, k, hexc, u, x, y, rot, accent)
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 750">{defs}{body}</svg>'

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

# ---- 6 款预设：每款 5 指（小指/无名指/中指/食指/拇指），跳色与点缀错落，贴花 id 取自实时素材目录 ----
TEMPLATES = [
    dict(name='奶杏圆舞', tag='简约', price=39, file='tpl_01.png',
         fingers=[finger('圆形', 1.0, '奶霜白') for _ in range(5)],
         cover=[('round', 1.0, '奶霜白', None)] * 5),
    dict(name='樱花物语', tag='甜美', price=45, file='tpl_02.png',
         fingers=[finger('椭圆形', 1.05, '芭蕾粉', ['叶子']),
                  finger('椭圆形', 1.05, '芭蕾粉'),
                  finger('椭圆形', 1.05, '芭蕾粉', ['樱花']),
                  finger('椭圆形', 1.05, '樱花粉', ['樱花', '小花']),
                  finger('椭圆形', 1.05, '芭蕾粉')],
         cover=[('oval', 1.05, '芭蕾粉', 'sakura'), ('oval', 1.05, '芭蕾粉', None),
                ('oval', 1.05, '芭蕾粉', 'sakura'), ('oval', 1.05, '樱花粉', 'sakura'),
                ('oval', 1.05, '芭蕾粉', None)]),
    dict(name='金色月影', tag='轻奢', price=49, file='tpl_03.png',
         fingers=[finger('杏仁形', 1.25, '裸杏'),
                  finger('杏仁形', 1.25, '裸杏', ['星星']),
                  finger('杏仁形', 1.25, '裸杏', ['月亮']),
                  finger('杏仁形', 1.25, '裸杏', ['金箔']),
                  finger('杏仁形', 1.25, '裸杏')],
         cover=[('almond', 1.25, '裸杏', None), ('almond', 1.25, '裸杏', 'star'),
                ('almond', 1.25, '裸杏', 'moon'), ('almond', 1.25, '裸杏', 'foil'),
                ('almond', 1.25, '裸杏', None)]),
    dict(name='法式奶盖', tag='经典', price=42, file='tpl_04.png',
         fingers=[finger('方圆形', 1.0, '奶霜白', french=True) for _ in range(5)],
         cover=[('squoval', 1.0, '奶霜白', 'french')] * 5),
    dict(name='玫瑰心事', tag='甜美', price=45, file='tpl_05.png',
         fingers=[finger('圆形', 1.0, '蜜桃粉', ['蝴蝶结']),
                  finger('圆形', 1.0, '蜜桃粉', ['爱心']),
                  finger('圆形', 1.0, '蜜桃粉'),
                  finger('圆形', 1.0, '樱花粉', ['玫瑰']),
                  finger('圆形', 1.0, '蜜桃粉', ['玫瑰'])],
         cover=[('round', 1.0, '蜜桃粉', 'bow'), ('round', 1.0, '蜜桃粉', 'heart'),
                ('round', 1.0, '蜜桃粉', None), ('round', 1.0, '樱花粉', 'rose'),
                ('round', 1.0, '蜜桃粉', 'rose')]),
    dict(name='芭蕾极简', tag='轻奢', price=55, file='tpl_06.png',
         fingers=[finger('芭蕾形', 1.5, '焦糖奶咖', ['条纹']),
                  finger('芭蕾形', 1.5, '焦糖奶咖'),
                  finger('芭蕾形', 1.5, '焦糖奶咖', ['金箔']),
                  finger('芭蕾形', 1.5, '焦糖奶咖', ['波点贴']),
                  finger('芭蕾形', 1.5, '焦糖奶咖')],
         cover=[('ballerina', 1.5, '焦糖奶咖', 'stripe'), ('ballerina', 1.5, '焦糖奶咖', None),
                ('ballerina', 1.5, '焦糖奶咖', 'foil'), ('ballerina', 1.5, '焦糖奶咖', 'dots'),
                ('ballerina', 1.5, '焦糖奶咖', None)]),
]

def main():
    conn = pymysql.connect(host='localhost', port=3306, user='root',
                           password=os.environ.get('MYSQL_PASSWORD', '1234'), database='nail_diy')
    cur = conn.cursor()
    for i, t in enumerate(TEMPLATES):
        png = resvg_py.svg_to_bytes(svg_string=cover_svg(t['name'], t['cover']), width=600, height=750)
        cover_path = os.path.join(SEED, t['file'])
        with open(cover_path, 'wb') as fh:
            fh.write(bytes(png))
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
