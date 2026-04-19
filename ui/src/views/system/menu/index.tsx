import React, {useEffect, useRef, useState} from "react";
import { menuList } from '@/api/menu.ts'
import { handleTree } from '@/utils/utils.ts'
import {Button, Form, Input, Select, Switch, Table, TableColumnsType} from "antd";
import type { Menu } from '@/types/menu.ts'

const Menu:React.FC = () => {
    const isInitialized = useRef(false);
    const [menus, setMenus] = useState([])
    const params = {
        menuName: '',
        status: ''
    }

    const columns: TableColumnsType<Menu> = [
        { title: '菜单名称', dataIndex: 'menuName', align: 'center', ellipsis: true, key: 'menuName' },
        {
            title: '菜单图标',
            dataIndex: 'icon',
            align: 'center',
            ellipsis: true,
            key: 'icon'
        },
        {
            title: '菜单状态',
            dataIndex: 'status',
            align: 'center',
            ellipsis: true,
            key: 'status',
            render: (_text, record) => (
                <Switch checked={record.status === '0'} />
            )
        },
        { title: '路由名称', dataIndex: 'routeName', align: 'center', ellipsis: true, key: 'routeName' },
        { title: '路由地址', dataIndex: 'path', align: 'center', ellipsis: true, key: 'path' },
        { title: '组件路径', dataIndex: 'component', align: 'center', ellipsis: true, key: 'component' },
        { title: '权限标识', dataIndex: 'perms', align: 'center', ellipsis: true, key: 'perms' },
        { title: '创建时间', dataIndex: 'createTime', align: 'center', ellipsis: true, key: 'createTime' },
        { title: '更新时间', dataIndex: 'updateTime', align: 'center', ellipsis: true, key: 'updateTime' },
        { title: 'Action', key: 'operation', align: 'center', ellipsis: true, render: () => <a>Publish</a> },
    ];

    const getMenuList = async () => {
        const menus :any[] = await menuList(params as any)
        // @ts-ignore
        setMenus(handleTree(menus, "menuId"))
    }

    useEffect(() => {
        if (!isInitialized.current) {
            isInitialized.current = true;
            getMenuList()
        }

    }, []);
    return (
        <>
            <Form layout="inline" initialValues={params} onFinish={() => console.log('success')}>
                <Form.Item label="菜单名称" name="menuName">
                    <Input placeholder="查询菜单名称" />
                </Form.Item>
                <Form.Item label="菜单状态" name="status">
                    <Select
                        style={{ width: 120 }}
                        defaultValue={params.status}
                        placeholder="菜单状态"
                        options={[
                            { value: '0', label: '启用' },
                            { value: '1', label: '关闭' }
                        ]}
                    />
                </Form.Item>
                <Form.Item>
                    <Button type="primary">查询</Button>
                    <Button>重置</Button>
                </Form.Item>
            </Form>

          <Table
            columns={columns}
            dataSource={menus}
          />
        </>
    )
}
export default Menu