# -*- coding: utf-8 -*-
"""高级美甲贴花素材生成器：30 个透明 PNG（480×480）。
风格：韩系/日系 nail art decal——极细线稿、低饱和、金属点缀；无卡通描边、无 emoji 感。
输出：backend/uploads/seed/pat_*.png（同名替换，数据库无需变更，设计台图案面板即时生效）。
用法：python frontend/scripts/gen-decals.py
"""
import os
import math

try:
    import resvg_py
except ImportError:
    raise SystemExit("需要 resvg-py：pip install resvg-py")

HERE = os.path.dirname(__file__)
OUT = os.path.join(HERE, '..', '..', 'backend', 'uploads', 'seed')
SIZE = 480

# ---- 低饱和高级色板 ----
GOLD = '#C9A25E'; GOLD_L = '#E3CB97'; GOLD_D = '#A98545'
SILVER = '#B9BFCB'; SILVER_L = '#DEE1E7'
ROSE = '#D9A7A0'; ROSE_D = '#C08A85'; BLUSH = '#EBCFC9'
MOCHA = '#8A6F5A'; TAUPE = '#A78D77'
SAGE = '#9DAF98'; SAGE_D = '#7E947A'
POWDER = '#A9BCCB'; CREAM = '#F3EDE3'; IVORY = '#F7F2E9'

def svg(body, defs=''):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {SIZE} {SIZE}">'
            f'<defs>{defs}</defs>{body}</svg>')

def g_def(gid, stops, x1=0, y1=0, x2=1, y2=1):
    s = ''.join(f'<stop offset="{o}" stop-color="{c}" stop-opacity="{a}"/>' for o, c, a in stops)
    return f'<linearGradient id="{gid}" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">{s}</linearGradient>'

GOLD_G = g_def('gg', [(0, GOLD_L, 1), (0.45, GOLD, 1), (1, GOLD_D, 1)])
SILVER_G = g_def('sg', [(0, SILVER_L, 1), (0.5, SILVER, 1), (1, '#9AA0AC', 1)])
PEARL_G = g_def('pg', [(0, '#FFFFFF', 1), (0.55, '#F6EDEA', 1), (1, '#E3CDC6', 1)], x1=0.3, y1=0.25)


def butterfly():
    """极细线稿蝴蝶：对称双翼细脉 + 金色触角"""
    b = ''
    # 左翼（右翼镜像）
    wing = ('M 228 250 C 170 190 96 170 66 206 C 40 238 66 286 118 296 '
            'C 74 306 62 344 88 366 C 116 390 178 372 224 306')
    wing_in = ('M 214 262 C 168 222 116 210 92 232 M 118 296 C 90 300 80 326 96 342 '
               'M 224 306 C 186 328 140 344 108 348')
    for sx in (1, -1):
        b += (f'<g transform="translate(240,0) scale({sx},1) translate(-240,0)">'
              f'<path d="{wing}" fill="{ROSE}" fill-opacity="0.13" stroke="{MOCHA}" '
              f'stroke-width="3.2" stroke-linecap="round"/>'
              f'<path d="{wing_in}" fill="none" stroke="{MOCHA}" stroke-width="2.2" '
              f'stroke-opacity="0.75" stroke-linecap="round"/>'
              f'<circle cx="86" cy="212" r="4" fill="{GOLD}"/>'
              f'<circle cx="94" cy="352" r="3.4" fill="{GOLD}" fill-opacity="0.85"/></g>')
    # 身体 + 触角
    b += (f'<path d="M 240 244 C 247 258 249 288 245 318 C 243 332 237 332 235 318 '
          f'C 231 288 233 258 240 244 Z" fill="{MOCHA}" fill-opacity="0.85"/>'
          f'<path d="M 234 250 C 216 226 200 214 184 212" fill="none" stroke="{GOLD_D}" '
          f'stroke-width="2.6" stroke-linecap="round"/>'
          f'<path d="M 246 250 C 264 226 280 214 296 212" fill="none" stroke="{GOLD_D}" '
          f'stroke-width="2.6" stroke-linecap="round"/>'
          f'<circle cx="184" cy="212" r="4.5" fill="{GOLD}"/>'
          f'<circle cx="296" cy="212" r="4.5" fill="{GOLD}"/>')
    return svg(b)


def bow():
    """缎带蝴蝶结：双层翼 + 缎面渐变 + 金扣"""
    b = (f'<path d="M 226 240 C 168 196 92 184 74 220 C 58 252 88 296 150 302 '
         f'C 106 318 100 356 130 370 C 162 384 208 344 230 288 Z" '
         f'fill="{ROSE}" fill-opacity="0.20" stroke="{ROSE_D}" stroke-width="4" stroke-linejoin="round"/>'
         f'<path d="M 254 240 C 312 196 388 184 406 220 C 422 252 392 296 330 302 '
         f'C 374 318 380 356 350 370 C 318 384 272 344 250 288 Z" '
         f'fill="{ROSE}" fill-opacity="0.20" stroke="{ROSE_D}" stroke-width="4" stroke-linejoin="round"/>'
         f'<path d="M 228 262 C 180 262 132 300 118 352 L 150 366 C 172 318 204 292 240 286" '
         f'fill="{BLUSH}" fill-opacity="0.30" stroke="{ROSE_D}" stroke-width="3.4" stroke-linejoin="round"/>'
         f'<path d="M 252 262 C 300 262 348 300 362 352 L 330 366 C 308 318 276 292 240 286" '
         f'fill="{BLUSH}" fill-opacity="0.30" stroke="{ROSE_D}" stroke-width="3.4" stroke-linejoin="round"/>'
         f'<ellipse cx="240" cy="264" rx="22" ry="19" fill="{ROSE}" fill-opacity="0.55" '
         f'stroke="{ROSE_D}" stroke-width="3.4"/>'
         f'<circle cx="240" cy="264" r="6.5" fill="{GOLD}"/>'
         f'<path d="M 200 226 C 216 214 232 210 240 222" fill="none" stroke="#FFFFFF" '
         f'stroke-opacity="0.65" stroke-width="3" stroke-linecap="round"/>')
    return svg(b)


def heart():
    """极细线心形 + 内廓 + 金点"""
    b = (f'<path d="M 240 388 C 130 306 76 240 84 180 C 90 130 140 104 184 124 '
         f'C 208 135 228 158 240 184 C 252 158 272 135 296 124 C 340 104 390 130 396 180 '
         f'C 404 240 350 306 240 388 Z" '
         f'fill="{ROSE}" fill-opacity="0.10" stroke="{MOCHA}" stroke-width="3.6" stroke-linejoin="round"/>'
         f'<path d="M 240 352 C 156 288 112 236 118 190 C 122 154 158 136 190 152 '
         f'C 210 162 228 180 240 202 C 252 180 270 162 290 152 C 322 136 358 154 362 190 '
         f'C 368 236 324 288 240 352 Z" fill="none" stroke="{ROSE_D}" stroke-width="2.2" '
         f'stroke-opacity="0.7" stroke-linejoin="round"/>'
         f'<circle cx="372" cy="140" r="5" fill="{GOLD}"/>'
         f'<circle cx="118" cy="286" r="3.5" fill="{GOLD}" fill-opacity="0.8"/>')
    return svg(b)


def candy():
    """扭糖纸糖果细线稿"""
    b = (f'<ellipse cx="240" cy="240" rx="86" ry="62" fill="{BLUSH}" fill-opacity="0.28" '
         f'stroke="{ROSE_D}" stroke-width="3.4"/>'
         f'<path d="M 158 226 C 172 214 172 266 158 254 M 178 216 C 196 200 196 280 178 264" '
         f'fill="none" stroke="{ROSE_D}" stroke-width="2.6" stroke-opacity="0.7" stroke-linecap="round"/>'
         f'<path d="M 322 226 C 308 214 308 266 322 254 M 302 216 C 284 200 284 280 302 264" '
         f'fill="none" stroke="{ROSE_D}" stroke-width="2.6" stroke-opacity="0.7" stroke-linecap="round"/>'
         f'<path d="M 120 216 C 96 200 76 202 62 216 C 76 230 76 250 62 264 C 76 278 96 280 120 264" '
         f'fill="{BLUSH}" fill-opacity="0.2" stroke="{ROSE_D}" stroke-width="3" stroke-linejoin="round"/>'
         f'<path d="M 360 216 C 384 200 404 202 418 216 C 404 230 404 250 418 264 C 404 278 384 280 360 264" '
         f'fill="{BLUSH}" fill-opacity="0.2" stroke="{ROSE_D}" stroke-width="3" stroke-linejoin="round"/>'
         f'<path d="M 196 214 C 226 202 258 202 286 216" fill="none" stroke="#FFFFFF" '
         f'stroke-opacity="0.7" stroke-width="4" stroke-linecap="round"/>')
    return svg(b)


def strawberry():
    """手绘线稿草莓：金点籽 + 锯齿萼"""
    b = (f'<path d="M 240 396 C 168 352 118 296 116 240 C 114 196 152 168 200 172 '
         f'C 218 174 232 180 240 188 C 248 180 262 174 280 172 C 328 168 366 196 364 240 '
         f'C 362 296 312 352 240 396 Z" '
         f'fill="{ROSE}" fill-opacity="0.16" stroke="{MOCHA}" stroke-width="3.4" stroke-linejoin="round"/>'
         f'<path d="M 240 190 L 226 172 M 240 190 L 254 172 M 240 190 L 240 166" '
         f'stroke="{SAGE_D}" stroke-width="2.6" stroke-linecap="round"/>'
         f'<path d="M 196 172 C 210 186 230 192 240 190 C 250 192 270 186 284 172 '
         f'C 272 166 250 164 240 170 C 230 164 208 166 196 172 Z" '
         f'fill="{SAGE}" fill-opacity="0.55" stroke="{SAGE_D}" stroke-width="2.6" stroke-linejoin="round"/>'
         f'<path d="M 238 168 C 236 152 238 138 246 128" fill="none" stroke="{SAGE_D}" '
         f'stroke-width="3" stroke-linecap="round"/>')
    seeds = ''
    for sx, sy in [(196, 236), (232, 226), (272, 234), (296, 272), (258, 268), (218, 280),
                   (188, 288), (240, 312), (274, 310), (210, 330), (262, 348), (240, 356)]:
        seeds += f'<ellipse cx="{sx}" cy="{sy}" rx="4" ry="6" fill="{GOLD_L}" stroke="{GOLD_D}" stroke-width="1.2" transform="rotate({(sx-240)//6} {sx} {sy})"/>'
    b += seeds
    return svg(b)


def cherry():
    """双梗樱桃细线稿"""
    b = (f'<path d="M 178 268 C 176 216 196 168 244 138" fill="none" stroke="{SAGE_D}" '
         f'stroke-width="3.4" stroke-linecap="round"/>'
         f'<path d="M 310 276 C 316 224 292 172 244 138" fill="none" stroke="{SAGE_D}" '
         f'stroke-width="3.4" stroke-linecap="round"/>'
         f'<circle cx="172" cy="322" r="58" fill="{ROSE_D}" fill-opacity="0.22" stroke="{MOCHA}" stroke-width="3.2"/>'
         f'<circle cx="318" cy="330" r="52" fill="{ROSE_D}" fill-opacity="0.22" stroke="{MOCHA}" stroke-width="3.2"/>'
         f'<path d="M 148 296 C 156 286 168 280 180 280" fill="none" stroke="#FFFFFF" '
         f'stroke-opacity="0.8" stroke-width="4" stroke-linecap="round"/>'
         f'<path d="M 298 304 C 304 296 314 291 324 291" fill="none" stroke="#FFFFFF" '
         f'stroke-opacity="0.8" stroke-width="3.6" stroke-linecap="round"/>'
         f'<path d="M 238 136 C 262 118 296 112 322 122 C 306 134 286 142 264 142 C 254 142 244 140 238 136 Z" '
         f'fill="{SAGE}" fill-opacity="0.6" stroke="{SAGE_D}" stroke-width="2.6" stroke-linejoin="round"/>'
         f'<circle cx="238" cy="137" r="5" fill="{GOLD}"/>')
    return svg(b)


def rose():
    """日式螺旋线稿玫瑰"""
    b = (f'<circle cx="240" cy="240" r="132" fill="{BLUSH}" fill-opacity="0.22" stroke="{MOCHA}" stroke-width="3"/>')
    spiral = ('M 240 240 c 16 -10 34 -2 32 14 c -3 20 -30 26 -46 12 c -20 -17 -12 -50 12 -60 '
              'c 30 -13 64 6 66 38 c 3 38 -34 66 -72 60 c -46 -7 -74 -50 -62 -92 '
              'c 13 -47 64 -72 108 -58 c 12 4 23 10 32 18')
    b += f'<path d="{spiral}" fill="none" stroke="{MOCHA}" stroke-width="3.4" stroke-linecap="round"/>'
    petals = ('M 118 196 C 130 150 172 116 220 110 M 132 306 C 102 270 96 222 114 184 '
              'M 232 368 C 182 372 140 346 122 306 M 356 320 C 344 356 306 380 262 376 '
              'M 366 210 C 388 248 382 296 350 324 M 320 130 C 352 146 372 178 374 214')
    b += f'<path d="{petals}" fill="none" stroke="{MOCHA}" stroke-width="3" stroke-opacity="0.85" stroke-linecap="round"/>'
    b += (f'<path d="M 150 150 C 178 118 220 100 262 102" fill="none" stroke="{SAGE_D}" '
          f'stroke-width="3.2" stroke-linecap="round"/>'
          f'<path d="M 128 322 C 116 348 118 376 136 396 C 152 380 158 352 150 328" '
          f'fill="{SAGE}" fill-opacity="0.5" stroke="{SAGE_D}" stroke-width="2.6" stroke-linejoin="round"/>'
          f'<circle cx="240" cy="240" r="5.5" fill="{GOLD}"/>')
    return svg(b)


def flower_sprig():
    """小碎花枝：三朵五瓣小花 + 叶"""
    def bloom(cx, cy, r, petal_fill, petal_op, stroke):
        p = ''
        for i in range(5):
            a = math.radians(i * 72 - 90)
            px_, py_ = cx + math.cos(a) * r * 0.95, cy + math.sin(a) * r * 0.95
            p += (f'<ellipse cx="{px_:.0f}" cy="{py_:.0f}" rx="{r*0.62:.0f}" ry="{r*0.44:.0f}" '
                  f'fill="{petal_fill}" fill-opacity="{petal_op}" stroke="{stroke}" stroke-width="2.4" '
                  f'transform="rotate({i*72-90:.0f} {px_:.0f} {py_:.0f})"/>')
        p += f'<circle cx="{cx}" cy="{cy}" r="{r*0.28:.0f}" fill="{GOLD}" fill-opacity="0.9"/>'
        return p
    b = (f'<path d="M 96 396 C 150 340 210 300 268 268 C 316 242 358 210 388 168" '
         f'fill="none" stroke="{SAGE_D}" stroke-width="3.4" stroke-linecap="round"/>'
         f'<path d="M 176 330 C 158 306 158 282 172 262 C 188 276 194 300 188 322" '
         f'fill="{SAGE}" fill-opacity="0.5" stroke="{SAGE_D}" stroke-width="2.4" stroke-linejoin="round"/>'
         f'<path d="M 300 250 C 314 228 338 218 360 222 C 350 244 328 258 304 256" '
         f'fill="{SAGE}" fill-opacity="0.5" stroke="{SAGE_D}" stroke-width="2.4" stroke-linejoin="round"/>'
         + bloom(150, 250, 46, CREAM, 0.9, TAUPE)
         + bloom(300, 330, 52, BLUSH, 0.85, ROSE_D)
         + bloom(392, 128, 38, CREAM, 0.9, TAUPE)
         + bloom(240, 180, 30, BLUSH, 0.8, ROSE_D))
    return svg(b)


def sakura():
    """樱花枝：两朵带缺刻五瓣樱 + 弧形枝"""
    def petal_path(cx, cy, r, a_deg):
        a = math.radians(a_deg)
        tx, ty = cx + math.cos(a) * r, cy + math.sin(a) * r
        n = a - math.pi / 2
        # 花瓣：两侧贝塞尔 + 顶端小缺刻
        bx, by = cx + math.cos(a - 0.62) * r * 1.02, cy + math.sin(a - 0.62) * r * 1.02
        cx2, cy2 = cx + math.cos(a + 0.62) * r * 1.02, cy + math.sin(a + 0.62) * r * 1.02
        nx, ny = math.cos(n) * r * 0.10, math.sin(n) * r * 0.10
        return (f'M {cx:.0f} {cy:.0f} '
                f'C {cx + (bx-cx)*0.5:.0f} {cy + (by-cy)*0.72:.0f} {bx:.0f} {by:.0f} {tx - nx*1.6:.0f} {ty - ny*1.6:.0f} '
                f'L {tx + nx*1.6:.0f} {ty + ny*1.6:.0f} '
                f'C {cx2:.0f} {cy2:.0f} {cx + (cx2-cx)*0.5:.0f} {cy + (cy2-cy)*0.72:.0f} {cx:.0f} {cy:.0f} Z')
    def blossom(cx, cy, r, base_a):
        p = ''
        for i in range(5):
            p += (f'<path d="{petal_path(cx, cy, r, base_a + i*72)}" fill="{BLUSH}" '
                  f'fill-opacity="0.75" stroke="{ROSE_D}" stroke-width="2.6" stroke-linejoin="round"/>')
        for i in range(5):
            a = math.radians(base_a + i * 72)
            p += (f'<line x1="{cx + math.cos(a)*r*0.14:.0f}" y1="{cy + math.sin(a)*r*0.14:.0f}" '
                  f'x2="{cx + math.cos(a)*r*0.5:.0f}" y2="{cy + math.sin(a)*r*0.5:.0f}" '
                  f'stroke="{ROSE_D}" stroke-width="2"/>'
                  f'<circle cx="{cx + math.cos(a)*r*0.56:.0f}" cy="{cy + math.sin(a)*r*0.56:.0f}" '
                  f'r="3.6" fill="{GOLD}"/>')
        p += f'<circle cx="{cx}" cy="{cy}" r="5" fill="{GOLD_D}"/>'
        return p
    b = (f'<path d="M 64 420 C 140 380 220 344 292 300 C 340 270 380 236 408 190" '
         f'fill="none" stroke="{MOCHA}" stroke-width="5" stroke-linecap="round"/>'
         f'<path d="M 250 322 C 258 336 258 352 250 366 M 340 262 C 352 268 360 278 362 292" '
         f'fill="none" stroke="{MOCHA}" stroke-width="3.6" stroke-linecap="round"/>'
         + blossom(196, 300, 62, -90)
         + blossom(356, 158, 48, -70)
         + blossom(296, 404, 36, -100))
    return svg(b)


def leaf_branch():
    """对生叶枝细线稿"""
    b = f'<path d="M 84 408 C 160 340 250 280 340 232 C 372 216 404 204 432 198" fill="none" stroke="{SAGE_D}" stroke-width="3.6" stroke-linecap="round"/>'
    leaves = [
        (128, 366, -52, 44), (176, 330, -50, 38), (226, 292, -48, 42), (278, 258, -46, 36),
        (326, 230, -44, 32), (158, 396, 40, 34), (210, 352, 42, 40), (262, 312, 44, 36),
        (312, 276, 46, 30), (358, 244, 48, 26),
    ]
    for x, y, ang, ln in leaves:
        a = math.radians(ang)
        tipx, tipy = x + math.cos(a) * ln * 1.7, y + math.sin(a) * ln * 1.7
        p = (f'M {x} {y} '
             f'C {x + math.cos(a)*ln*0.4 - math.sin(a)*ln*0.5:.0f} {y + math.sin(a)*ln*0.4 + math.cos(a)*ln*0.5:.0f} '
             f'{tipx - math.cos(a)*ln*0.5:.0f} {tipy - math.sin(a)*ln*0.5:.0f} {tipx:.0f} {tipy:.0f} '
             f'C {tipx - math.cos(a)*ln*0.5:.0f} {tipy - math.sin(a)*ln*0.5:.0f} '
             f'{x + math.cos(a)*ln*0.4 + math.sin(a)*ln*0.5:.0f} {y + math.sin(a)*ln*0.4 - math.cos(a)*ln*0.5:.0f} '
             f'{x} {y} Z')
        b += (f'<path d="{p}" fill="{SAGE}" fill-opacity="0.45" stroke="{SAGE_D}" stroke-width="2.4" stroke-linejoin="round"/>'
              f'<line x1="{x + math.cos(a)*ln*0.2:.0f}" y1="{y + math.sin(a)*ln*0.2:.0f}" '
              f'x2="{x + math.cos(a)*ln*1.45:.0f}" y2="{y + math.sin(a)*ln*1.45:.0f}" '
              f'stroke="{SAGE_D}" stroke-width="1.6" stroke-opacity="0.8"/>')
    return svg(b)


def cloud():
    """单线连绵云 + 金星"""
    b = (f'<path d="M 96 300 C 68 300 52 278 62 256 C 70 238 90 230 108 236 '
         f'C 110 206 140 186 172 194 C 184 168 222 158 250 174 C 276 156 316 162 334 188 '
         f'C 366 178 400 196 404 228 C 428 232 442 252 434 274 C 428 292 408 300 390 298" '
         f'fill="none" stroke="{TAUPE}" stroke-width="3.6" stroke-linecap="round"/>'
         f'<path d="M 96 300 C 160 308 300 308 390 298" fill="none" stroke="{TAUPE}" '
         f'stroke-width="3.6" stroke-linecap="round"/>'
         f'<path d="M 356 132 l 0 24 m -12 -12 l 24 0" stroke="{GOLD}" stroke-width="2.6" stroke-linecap="round"/>'
         f'<circle cx="120" cy="160" r="3.6" fill="{GOLD}"/>')
    return svg(b)


def moon_star():
    """星月组合：金线弯月 + 双星"""
    b = (f'<path d="M 300 96 C 226 118 178 186 186 262 C 193 332 248 388 318 398 '
         f'C 258 372 218 314 216 248 C 214 186 248 128 300 96 Z" '
         f'fill="{GOLD}" fill-opacity="0.14" stroke="{GOLD}" stroke-width="3.6" stroke-linejoin="round"/>'
         f'<path d="M 288 140 C 244 164 218 212 224 262 C 229 308 260 348 304 362 '
         f'C 268 332 248 288 250 242 C 252 198 270 160 300 136 Z" '
         f'fill="none" stroke="{GOLD}" stroke-width="2" stroke-opacity="0.65"/>'
         # 四芒星两颗
         f'<path d="M 150 150 C 156 176 162 182 188 188 C 162 194 156 200 150 226 '
         f'C 144 200 138 194 112 188 C 138 182 144 176 150 150 Z" '
         f'fill="{GOLD}" fill-opacity="0.85"/>'
         f'<path d="M 176 300 C 180 316 184 320 200 324 C 184 328 180 332 176 348 '
         f'C 172 332 168 328 152 324 C 168 320 172 316 176 300 Z" fill="{GOLD}" fill-opacity="0.7"/>'
         f'<circle cx="352" cy="120" r="3.6" fill="{GOLD}"/>')
    return svg(b)


def star_sparkle():
    """五角星细线稿 + 小闪星（与四芒星光区分）"""
    def star5(cx, cy, r, rot=-90):
        pts = []
        for i in range(5):
            a = math.radians(rot + i * 72)
            pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
            a2 = math.radians(rot + i * 72 + 36)
            pts.append((cx + math.cos(a2) * r * 0.42, cy + math.sin(a2) * r * 0.42))
        p = 'M ' + ' L '.join(f'{x:.0f} {y:.0f}' for x, y in pts) + ' Z'
        return (f'<path d="{p}" fill="{GOLD}" fill-opacity="0.14" stroke="{GOLD}" '
                f'stroke-width="3.2" stroke-linejoin="round"/>')
    def spark(cx, cy, r, op=1):
        return (f'<path d="M {cx} {cy - r} '
                f'C {cx + r*0.14} {cy - r*0.3} {cx + r*0.3} {cy - r*0.14} {cx + r} {cy} '
                f'C {cx + r*0.3} {cy + r*0.14} {cx + r*0.14} {cy + r*0.3} {cx} {cy + r} '
                f'C {cx - r*0.14} {cy + r*0.3} {cx - r*0.3} {cy + r*0.14} {cx - r} {cy} '
                f'C {cx - r*0.3} {cy - r*0.14} {cx - r*0.14} {cy - r*0.3} {cx} {cy - r} Z" '
                f'fill="url(#gg)" fill-opacity="{op}" stroke="{GOLD_D}" stroke-width="1.4"/>')
    b = (star5(232, 232, 128)
         + f'<path d="M 232 160 C 236 178 242 184 260 188 C 242 192 236 198 232 216 '
         f'C 228 198 222 192 204 188 C 222 184 228 178 232 160 Z" fill="{GOLD}" fill-opacity="0.9"/>'
         + spark(388, 132, 34, 0.85) + f'<circle cx="130" cy="360" r="3.6" fill="{GOLD}"/>')
    return svg(b, GOLD_G)


def snowflake():
    """六向细线雪花"""
    arms = ''
    for i in range(6):
        a = math.radians(i * 60)
        dx, dy = math.cos(a), math.sin(a)
        x2, y2 = 240 + dx * 150, 240 + dy * 150
        arms += f'<line x1="240" y1="240" x2="{x2:.0f}" y2="{y2:.0f}" stroke="{POWDER}" stroke-width="3.4" stroke-linecap="round"/>'
        for t in (0.55, 0.8):
            bx, by = 240 + dx * 150 * t, 240 + dy * 150 * t
            for s in (-1, 1):
                a2 = a + s * math.radians(38)
                arms += (f'<line x1="{bx:.0f}" y1="{by:.0f}" '
                         f'x2="{bx + math.cos(a2)*26:.0f}" y2="{by + math.sin(a2)*26:.0f}" '
                         f'stroke="{POWDER}" stroke-width="2.6" stroke-linecap="round"/>')
        arms += f'<circle cx="{x2:.0f}" cy="{y2:.0f}" r="4" fill="{POWDER}"/>'
    b = (arms + f'<circle cx="240" cy="240" r="10" fill="none" stroke="{POWDER}" stroke-width="2.6"/>'
         f'<circle cx="240" cy="240" r="4" fill="{POWDER}"/>')
    return svg(b)


def rainbow():
    """极简细线彩虹拱 + 金点端"""
    b = ''
    cols = [(ROSE, 150), (GOLD, 122), (SAGE, 94)]
    for c, r in cols:
        b += (f'<path d="M {240 - r} 320 A {r} {r} 0 0 1 {240 + r} 320" '
              f'fill="none" stroke="{c}" stroke-width="6" stroke-linecap="round" stroke-opacity="0.8"/>')
    for r in (150, 122, 94):
        b += f'<circle cx="{240 - r}" cy="320" r="4.5" fill="{GOLD}"/><circle cx="{240 + r}" cy="320" r="4.5" fill="{GOLD}"/>'
    b += (f'<path d="M 176 332 C 160 332 148 320 148 306 M 304 332 C 320 332 332 320 332 306" '
          f'fill="none" stroke="{TAUPE}" stroke-width="2.6" stroke-linecap="round" stroke-opacity="0.7"/>')
    return svg(b)


def melon():
    """极简线稿西瓜切片"""
    b = (f'<path d="M 88 320 A 168 168 0 0 1 392 320 L 366 320 A 142 142 0 0 0 114 320 Z" '
         f'fill="{SAGE}" fill-opacity="0.5" stroke="{SAGE_D}" stroke-width="3"/>'
         f'<path d="M 114 320 A 142 142 0 0 1 366 320 Z" fill="{BLUSH}" fill-opacity="0.55" '
         f'stroke="{ROSE_D}" stroke-width="2.6"/>'
         f'<path d="M 148 320 A 106 106 0 0 1 332 320" fill="none" stroke="{SAGE_D}" '
         f'stroke-width="2.4" stroke-opacity="0.8"/>')
    for sx, sy in [(200, 250), (240, 230), (280, 250), (222, 284), (258, 284)]:
        b += f'<ellipse cx="{sx}" cy="{sy}" rx="5" ry="8" fill="{MOCHA}"/>'
    return svg(b)


def wave():
    """三段细波浪线（金属金）"""
    w = ''
    for i, (op, wd) in enumerate([(1, 4.2), (0.75, 3.4), (0.5, 2.8)]):
        y = 180 + i * 56
        w += (f'<path d="M 70 {y} C 108 {y - 34} 146 {y - 34} 184 {y} '
              f'C 222 {y + 34} 260 {y + 34} 298 {y} C 328 {y - 27} 358 {y - 27} 386 {y + 6}" '
              f'fill="none" stroke="{GOLD}" stroke-width="{wd}" stroke-opacity="{op}" stroke-linecap="round"/>')
    b = w + f'<circle cx="404" cy="128" r="4" fill="{GOLD}"/><circle cx="88" cy="356" r="3.4" fill="{GOLD}" fill-opacity="0.8"/>'
    return svg(b)


def dots():
    """金点阵（大小/透明度交错）"""
    b = ''
    for row in range(5):
        for col in range(5):
            x, y = 108 + col * 66, 108 + row * 66
            r = 6.5 if (row + col) % 2 == 0 else 4
            op = 0.92 if (row + col) % 2 == 0 else 0.55
            b += f'<circle cx="{x}" cy="{y}" r="{r}" fill="{GOLD}" fill-opacity="{op}"/>'
    return svg(b)


def gingham():
    """低饱和格纹（玫瑰+鼠尾草半透明交织，纯透明底）"""
    b = ''
    for i in range(4):
        x = 40 + i * 106
        b += (f'<rect x="{x}" y="0" width="44" height="{SIZE}" fill="{ROSE}" fill-opacity="0.30"/>'
              f'<rect y="{x}" x="0" width="{SIZE}" height="44" fill="{SAGE}" fill-opacity="0.26"/>')
    for i in range(5):
        x = 84 + i * 106
        b += (f'<rect x="{x}" y="0" width="14" height="{SIZE}" fill="{GOLD}" fill-opacity="0.16"/>'
              f'<rect y="{x}" x="0" width="{SIZE}" height="14" fill="{GOLD}" fill-opacity="0.16"/>')
    return svg(b)


def stripes():
    """细金线条纹（疏密相间，低透明度）"""
    b = ''
    x = 52
    widths = [(3, 0.7), (8, 0.24)] * 5
    for wd, op in widths:
        b += f'<rect x="{x}" y="0" width="{wd}" height="{SIZE}" fill="{GOLD}" fill-opacity="{op}"/>'
        x += wd + 34
    return svg(b)


def ring():
    """细双圆环 + 轨道金星"""
    b = (f'<circle cx="240" cy="240" r="140" fill="none" stroke="{GOLD}" stroke-width="3"/>'
         f'<circle cx="252" cy="228" r="112" fill="none" stroke="{SILVER}" stroke-width="2.2" stroke-opacity="0.9"/>'
         f'<circle cx="380" cy="150" r="7" fill="url(#gg)" stroke="{GOLD_D}" stroke-width="1.4"/>'
         f'<circle cx="118" cy="352" r="5" fill="{SILVER}"/>'
         f'<path d="M 240 118 C 243 130 246 133 258 136 C 246 139 243 142 240 154 '
         f'C 237 142 234 139 222 136 C 234 133 237 130 240 118 Z" fill="{GOLD}" fill-opacity="0.85"/>')
    return svg(b, GOLD_G)


def flags():
    """极简细线三角旗弧带"""
    b = (f'<path d="M 56 170 C 150 240 330 240 424 170" fill="none" stroke="{TAUPE}" '
         f'stroke-width="2.8" stroke-linecap="round"/>'
         f'<path d="M 56 170 C 150 240 330 240 424 170" fill="none" stroke="{TAUPE}" '
         f'stroke-width="1.8" stroke-opacity="0.6" transform="translate(0 14)"/>')
    # 五面小旗沿贝塞尔取点
    ts = [0.08, 0.29, 0.5, 0.71, 0.92]
    cols = [BLUSH, CREAM, SAGE, BLUSH, CREAM]
    strokes = [ROSE_D, TAUPE, SAGE_D, ROSE_D, TAUPE]
    for t, cf, cs in zip(ts, cols, strokes):
        # 二次贝塞尔 B(t): P0(56,170) P1(240,295) P2(424,170)
        x = (1-t)**2 * 56 + 2*(1-t)*t * 240 + t*t * 424
        y = (1-t)**2 * 170 + 2*(1-t)*t * 295 + t*t * 170
        w, h = 26, 34
        b += (f'<path d="M {x-w/2:.0f} {y:.0f} L {x+w/2:.0f} {y:.0f} L {x:.0f} {y+h:.0f} Z" '
              f'fill="{cf}" fill-opacity="0.85" stroke="{cs}" stroke-width="2.2" stroke-linejoin="round"/>')
    return svg(b)


def foil():
    """金箔碎片组（不规则细长 kite 形，错落）"""
    shards = [
        (116, 116, 58, -34, 0.9), (298, 84, 44, 18, 0.8), (392, 214, 54, -48, 0.95),
        (196, 232, 40, 26, 0.75), (110, 328, 50, 10, 0.9), (296, 348, 58, -16, 0.85),
        (394, 402, 34, 36, 0.7), (226, 424, 40, -28, 0.8),
    ]
    b = ''
    for cx, cy, s, rot, op in shards:
        # 细长 kite：长轴 2s、横轴 0.55s，上尖下钝
        pts = (f'{cx:.0f},{cy - s:.0f} {cx + s*0.27:.0f},{cy - s*0.18:.0f} '
               f'{cx + s*0.16:.0f},{cy + s*0.55:.0f} {cx:.0f},{cy + s*0.34:.0f} '
               f'{cx - s*0.16:.0f},{cy + s*0.55:.0f} {cx - s*0.27:.0f},{cy - s*0.18:.0f}')
        b += (f'<polygon points="{pts}" fill="url(#gg)" fill-opacity="{op}" '
              f'stroke="{GOLD_D}" stroke-width="1.1" transform="rotate({rot} {cx} {cy})"/>')
    for cx, cy in [(200, 160), (350, 300), (150, 250), (300, 200)]:
        b += f'<circle cx="{cx}" cy="{cy}" r="3" fill="{GOLD}" fill-opacity="0.75"/>'
    return svg(b, GOLD_G)


def pearl():
    """珍珠串弧（渐变球 + 高光）"""
    b = f'<path d="M 92 330 C 150 250 330 250 388 330" fill="none" stroke="{GOLD}" stroke-width="1.8" stroke-opacity="0.7"/>'
    pearls = [(100, 322, 26), (148, 276, 30), (206, 246, 33), (240, 238, 35),
              (274, 246, 33), (332, 276, 30), (380, 322, 26)]
    for cx, cy, r in pearls:
        b += (f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="url(#pg)" stroke="#D9C2BB" stroke-width="1.4"/>'
              f'<circle cx="{cx - r*0.32}" cy="{cy - r*0.38}" r="{r*0.2:.0f}" fill="#FFFFFF" fill-opacity="0.95"/>')
    b += f'<circle cx="240" cy="150" r="4" fill="{GOLD}"/><path d="M 320 140 l 0 18 m -9 -9 l 18 0" stroke="{GOLD}" stroke-width="2.4" stroke-linecap="round"/>'
    return svg(b, PEARL_G)


def stud():
    """金属铆钉点组"""
    b = ''
    studs = [(150, 160), (260, 140), (360, 190), (140, 300), (250, 280), (356, 330), (210, 400), (330, 430)]
    for cx, cy in studs:
        b += (f'<circle cx="{cx}" cy="{cy}" r="26" fill="url(#sg)" stroke="#8A9099" stroke-width="1.6"/>'
              f'<circle cx="{cx}" cy="{cy}" r="15" fill="none" stroke="#FFFFFF" stroke-opacity="0.55" stroke-width="1.8"/>'
              f'<circle cx="{cx - 8}" cy="{cy - 9}" r="5" fill="#FFFFFF" fill-opacity="0.9"/>')
    return svg(b, SILVER_G)


def sparkle():
    """星光组合（大小双星 + 微光点）"""
    def sp(cx, cy, r):
        return (f'<path d="M {cx} {cy - r} C {cx + r*0.16} {cy - r*0.26} {cx + r*0.26} {cy - r*0.16} {cx + r} {cy} '
                f'C {cx + r*0.26} {cy + r*0.16} {cx + r*0.16} {cy + r*0.26} {cx} {cy + r} '
                f'C {cx - r*0.16} {cy + r*0.26} {cx - r*0.26} {cy + r*0.16} {cx - r} {cy} '
                f'C {cx - r*0.26} {cy - r*0.16} {cx - r*0.16} {cy - r*0.26} {cx} {cy - r} Z" '
                f'fill="url(#gg)"/>')
    b = (sp(228, 210, 120) + sp(356, 330, 62) + sp(140, 356, 40)
         + f'<circle cx="228" cy="210" r="11" fill="#FFFDF6"/>'
         + f'<path d="M 320 108 v 22 m -11 -11 h 22" stroke="{GOLD}" stroke-width="3" stroke-linecap="round"/>'
         + f'<circle cx="120" cy="150" r="3.6" fill="{GOLD}"/>')
    return svg(b, GOLD_G)


def bolt():
    """细长金线闪电（非卡通）"""
    b = (f'<path d="M 268 60 L 172 254 L 232 254 L 196 420 L 316 216 L 250 216 Z" '
         f'fill="{GOLD}" fill-opacity="0.22" stroke="{GOLD}" stroke-width="3.4" stroke-linejoin="round"/>'
         f'<path d="M 252 92 L 186 238 L 240 238" fill="none" stroke="{GOLD_L}" '
         f'stroke-width="2" stroke-opacity="0.9"/>'
         f'<circle cx="342" cy="130" r="3.6" fill="{GOLD}"/><circle cx="150" cy="360" r="3" fill="{GOLD}" fill-opacity="0.8"/>')
    return svg(b)


def crown():
    """极简线稿皇冠 + 金珠"""
    b = (f'<path d="M 116 300 L 96 168 L 168 232 L 240 132 L 312 232 L 384 168 L 364 300 Z" '
         f'fill="{GOLD}" fill-opacity="0.13" stroke="{GOLD}" stroke-width="3.4" stroke-linejoin="round"/>'
         f'<path d="M 122 336 L 358 336 M 116 300 L 364 300" stroke="{GOLD}" stroke-width="3" stroke-linecap="round"/>'
         f'<path d="M 138 268 L 342 268" stroke="{GOLD}" stroke-width="1.8" stroke-opacity="0.6"/>'
         f'<circle cx="96" cy="158" r="7" fill="url(#gg)" stroke="{GOLD_D}" stroke-width="1.4"/>'
         f'<circle cx="240" cy="120" r="8" fill="url(#gg)" stroke="{GOLD_D}" stroke-width="1.4"/>'
         f'<circle cx="384" cy="158" r="7" fill="url(#gg)" stroke="{GOLD_D}" stroke-width="1.4"/>'
         f'<circle cx="240" cy="318" r="4.5" fill="{ROSE_D}"/>'
         f'<circle cx="180" cy="318" r="3.4" fill="{SILVER}"/><circle cx="300" cy="318" r="3.4" fill="{SILVER}"/>')
    return svg(b, GOLD_G)


def note():
    """双连八分音符细线稿"""
    b = (f'<ellipse cx="164" cy="352" rx="34" ry="26" fill="{MOCHA}" transform="rotate(-18 164 352)"/>'
         f'<ellipse cx="330" cy="322" rx="34" ry="26" fill="{GOLD}" transform="rotate(-18 330 322)"/>'
         f'<path d="M 196 340 L 196 148 C 232 138 300 128 362 148 L 362 310" '
         f'fill="none" stroke="{MOCHA}" stroke-width="7" stroke-linecap="round"/>'
         f'<path d="M 196 196 C 240 184 300 178 362 192" fill="none" stroke="{MOCHA}" '
         f'stroke-width="6" stroke-linecap="round"/>'
         f'<circle cx="404" cy="120" r="4" fill="{GOLD}"/><circle cx="120" cy="180" r="3" fill="{GOLD}" fill-opacity="0.8"/>')
    return svg(b)


def diamond():
    """线稿钻石：切面细线"""
    b = (f'<path d="M 120 190 L 168 130 L 312 130 L 360 190 L 240 396 Z" '
         f'fill="{GOLD_L}" fill-opacity="0.14" stroke="{GOLD}" stroke-width="3.2" stroke-linejoin="round"/>'
         f'<path d="M 120 190 L 360 190 M 168 130 L 200 190 L 240 396 M 312 130 L 280 190 L 240 396 '
         f'M 200 190 L 240 130 M 280 190 L 240 130" '
         f'fill="none" stroke="{GOLD}" stroke-width="2" stroke-opacity="0.8"/>'
         f'<path d="M 168 130 L 240 190 L 312 130" fill="none" stroke="{GOLD}" stroke-width="2" stroke-opacity="0.8"/>'
         f'<path d="M 134 178 L 178 142 M 322 176 L 284 142" stroke="{GOLD_L}" stroke-width="4" stroke-linecap="round"/>'
         f'<circle cx="240" cy="130" r="4" fill="{GOLD}"/>')
    return svg(b)


BUILDERS = {
    'pat_butterfly': butterfly, 'pat_bow': bow, 'pat_heart': heart, 'pat_candy': candy,
    'pat_strawberry': strawberry, 'pat_cherry': cherry, 'pat_rose': rose,
    'pat_flower': flower_sprig, 'pat_sakura': sakura, 'pat_leaf': leaf_branch,
    'pat_cloud': cloud, 'pat_moon': moon_star, 'pat_star': star_sparkle,
    'pat_snow': snowflake, 'pat_rainbow': rainbow, 'pat_melon': melon,
    'pat_wave': wave, 'pat_dots': dots, 'pat_gingham': gingham, 'pat_stripes': stripes,
    'pat_ring': ring, 'pat_flags': flags, 'pat_foil': foil, 'pat_pearl': pearl,
    'pat_stud': stud, 'pat_sparkle': sparkle, 'pat_bolt': bolt, 'pat_crown': crown,
    'pat_note': note, 'pat_diamond': diamond,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    # 通用渐变 defs 由各 builder 自带
    for name, fn in BUILDERS.items():
        markup = fn()
        png = resvg_py.svg_to_bytes(svg_string=markup, width=SIZE, height=SIZE)
        path = os.path.join(OUT, f'{name}.png')
        with open(path, 'wb') as fh:
            fh.write(bytes(png))
        print('rendered', name)
    print('done:', len(BUILDERS), 'decals ->', os.path.abspath(OUT))


if __name__ == '__main__':
    main()
