import React, {useState} from 'react'
import { useNavigate } from 'react-router-dom'
import {Modal, Input} from 'antd'
import {ExpandOutlined, SearchOutlined, SettingOutlined} from "@ant-design/icons";
import screenfull from "screenfull";
import { useMenuRouteStore } from '@/store/menuRouteStore.ts'
import { fuzzySearchMenuWithPath, RouteItem, MatchResult } from '@/utils/utils.ts'

const BaseOption :React.FC = () => {
    const { menus } = useMenuRouteStore();
    const navigate = useNavigate()
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [searchMenus, setSearchMenus] = useState<MatchResult[]>([])

    const handleClose = () => {
        setIsModalOpen(false);
    }

    const search = (e :React.ChangeEvent<HTMLInputElement>) => {
        const { value } = e.target;

        setSearchMenus(fuzzySearchMenuWithPath(menus as RouteItem[], value))
    }

    const toPage = (menu :MatchResult) => {
        setIsModalOpen(false);
        navigate({
            pathname: menu.fullPath
        })
    }

    return (
        <div className="base-option">
            <div
                className="base-option-item"
                onClick={() => setIsModalOpen(true)}
            >
                <SearchOutlined />
            </div>

            <div
                className="base-option-item"
                onClick={async () => await screenfull.toggle()}
            >
                <ExpandOutlined />
            </div>

            <div className="base-option-item">
                <SettingOutlined />
            </div>

            <Modal width="70%" title="搜索" open={isModalOpen} onCancel={handleClose}>
                <Input
                    style={{margin: '16px 0 24px'}}
                    placeholder="搜索菜单"
                    onChange={search}
                />

                { searchMenus.map((menu) => (
                    <p className="menu-item" onClick={() => toPage(menu)}>{ menu.menu.meta?.title }</p>
                ))}
            </Modal>
        </div>
    )
}

export default BaseOption
