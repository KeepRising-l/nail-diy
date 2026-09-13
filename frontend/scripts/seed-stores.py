# -*- coding: utf-8 -*-
"""自提门店种子数据：3 家营业中门店，供结算页"到店自提"选择。
- 直接写入本地 MySQL 的 store 表（root，密码取 MYSQL_PASSWORD 环境变量，默认 1234），脚本幂等（同名门店保留一条）
- 字段以 schema.sql 的 store 表为准（无经纬度字段）
用法：python frontend/scripts/seed-stores.py
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

def main():
    conn = pymysql.connect(host='localhost', port=3306, user='root',
                           password=os.environ.get('MYSQL_PASSWORD', '1234'), database='nail_diy')
    cur = conn.cursor()
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
