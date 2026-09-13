# -*- coding: utf-8 -*-
"""自提门店 + 演示商家种子数据：结算页"到店自提"与商家后台登录的演示环境。
- 写入本地 MySQL（root，密码取 MYSQL_PASSWORD 环境变量，默认 1234），脚本幂等
  1) store 表 3 家营业中门店（字段以 schema.sql 的 store 表为准，无经纬度字段）
  2) 演示商家账号 13900009999（角色 MERCHANT，昵称"演示商家"，密码与顾客测试账号 13800001234 相同）
     —— 复用其 BCrypt 哈希，因此密码即 Abc123456；若顾客测试账号不存在则跳过并提示
用法：python frontend/scripts/seed-stores.py（需先执行 schema.sql 完成建库）
"""
import os
import pymysql

STORES = [
    dict(name='NailDIY 旗舰店（江汉路店）', address='湖北省武汉市江汉区江汉路步行街 88 号 1 层 102 室',
         phone='027-82888888', business_hours='周一至周日 10:00-21:00', sort=1),
    dict(name='NailDIY 光谷体验店', address='湖北省武汉市洪山区珞瑜路 726 号光谷广场 3 层 B 区',
         phone='027-87654321', business_hours='周一至周五 10:30-20:30，周末 10:00-21:30', sort=2),
    dict(name='NailDIY 汉口江滩自提点', address='湖北省武汉市江岸区沿江大道 1626 号沿江商务区 2 号楼 12 层',
         phone='027-82996666', business_hours='工作日 09:30-18:30（节假日休息）', sort=3),
]

MERCHANT_PHONE = '13900009999'
CUSTOMER_PHONE = '13800001234'

def seed_merchant(cur):
    cur.execute('SELECT password FROM user WHERE phone = %s', (CUSTOMER_PHONE,))
    row = cur.fetchone()
    if not row:
        print('skip merchant seed: 顾客测试账号', CUSTOMER_PHONE, '不存在（请先注册或手工插入）')
        return
    cur.execute('SELECT COUNT(*) FROM user WHERE phone = %s', (MERCHANT_PHONE,))
    if cur.fetchone()[0]:
        return
    cur.execute(
        'INSERT INTO user (phone, password, nickname, avatar, role, status) VALUES (%s, %s, %s, %s, %s, 1)',
        (MERCHANT_PHONE, row[0], '演示商家', '', 'MERCHANT'))
    print('seeded merchant', MERCHANT_PHONE, '(role=MERCHANT, 密码同顾客测试账号)')

def main():
    conn = pymysql.connect(host='localhost', port=3306, user='root',
                           password=os.environ.get('MYSQL_PASSWORD', '1234'), database='nail_diy')
    cur = conn.cursor()
    seed_merchant(cur)
    for s in STORES:
        cur.execute(
            'INSERT INTO store (name, address, phone, business_hours, status, sort, deleted) '
            'VALUES (%s, %s, %s, %s, 1, %s, 0)',
            (s['name'], s['address'], s['phone'], s['business_hours'], s['sort']))
        print('seeded', s['name'])
    # 幂等：同名门店保留最新一条
    cur.execute('DELETE t1 FROM store t1, store t2 WHERE t1.name = t2.name AND t1.id < t2.id')
    conn.commit()
    cur.execute('SELECT COUNT(*) FROM store WHERE deleted = 0 AND status = 1')
    print('open store rows now:', cur.fetchone()[0])
    conn.close()

if __name__ == '__main__':
    main()

