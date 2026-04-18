import React from "react";
import {Avatar, Button, Popover, Modal} from "antd";
import {RightOutlined} from "@ant-design/icons";
import { useAuth } from '@/store/authStore'

const BaseUser: React.FC = () => {
    const [modal, contextHolder] = Modal.useModal()
    const { logout } = useAuth()

    const handleOutLogin = async () => {

        const confirmed = await modal.warning({
            type: 'warning',
            title: '是否退出登录?',
            okText: '退出登录',
            closable: true,
            cancelText: '取消',
            centered: true
        })
        if (confirmed) {
            logout()
        }
    }

    const UserPopover = (
        <div className="user-popover">
            <Avatar size={"large"} />
            <div className="base-user-info">
                <span>admin</span>
            </div>
            <div className="user-popover-cell">
                <span>个人资料</span>
                <RightOutlined />
            </div>
            <div className="user-popover-cell">
                <span>修改密码</span>
                <RightOutlined />
            </div>
            <div style={{height: '30px'}}></div>
            <Button type="primary" onClick={handleOutLogin}>退出登录</Button>
        </div>
    )

    return (
        <>
            <Popover content={UserPopover} trigger="click" placement="right">
                <div className="base-user">
                    <Avatar size={"large"} />
                    <div className="base-user-info">
                        <span>admin</span>
                    </div>
                    <RightOutlined />
                </div>
            </Popover>

            {contextHolder}
        </>
    )
}

export default BaseUser
