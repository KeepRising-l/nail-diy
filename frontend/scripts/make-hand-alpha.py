# 由 public/hands/hand-model.png（RGB 白底原图，保持不动）生成 public/hands/hand-model-alpha.png
# （白背景转透明，供设计台手模视图在米色舞台上使用，消除手图外围白色矩形）。
# 判据：手部/甲面像素均为暖色（r-b 大），背景与淡阴影为中性亮色（r≈g≈b 且偏亮）。
# 用法：python frontend/scripts/make-hand-alpha.py
from PIL import Image
import os

HERE = os.path.dirname(__file__)
SRC = os.path.join(HERE, '..', 'public', 'hands', 'hand-model.png')
DST = os.path.join(HERE, '..', 'public', 'hands', 'hand-model-alpha.png')

img = Image.open(SRC).convert('RGB')
W, H = img.size
src = img.load()
out = Image.new('RGBA', (W, H))
dst = out.load()

for y in range(H):
    for x in range(W):
        r, g, b = src[x, y]
        warm = r - b
        if warm >= 14:                      # 手部/甲面：暖色，不透明
            a = 255
        elif warm <= 5 and min(r, g, b) >= 235:
            a = 0                           # 白背景与轮廓外淡晕：全透明
        else:
            a = max(0, min(255, int((warm - 5) / 9 * 255)))   # 轮廓过渡带：按暖度半透明
            if min(r, g, b) >= 250 and warm < 8:
                a = 0
        dst[x, y] = (r, g, b, a)

out.save(DST)
print('saved', DST)
