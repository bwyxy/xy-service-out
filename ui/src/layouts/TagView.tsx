import React, {useEffect} from "react";
import { useNavigate } from 'react-router-dom'
import {RouteItem} from "@/utils/utils.ts";
import {CloseCircleOutlined, CloseOutlined} from "@ant-design/icons";
import {Dropdown} from "antd";
import type { MenuProps } from 'antd'

interface TagViewProps {
    tags?: RouteItem[]
    activeTag?: RouteItem,
    onClose: (tag : RouteItem) => void
    onCloseOther: (tag : RouteItem) => void
    onCloseAll: () => void
}

const TagView: React.FC<TagViewProps> = ({ tags, activeTag, onClose, onCloseOther, onCloseAll }) => {
    const navigate = useNavigate();

    const handleTagClick = (tag: RouteItem) => {
        navigate(tag.path)
    }

    const handleClose = (tag :RouteItem, event :any) => {
        event.stopPropagation();

        onClose(tag)
    }

    useEffect(() => {
        console.log(tags)
    }, [tags]);

    const getMenuItems = (tag :RouteItem) :MenuProps['items'] => [
        {
            key: 'close',
            label: '关闭',
            icon: <CloseOutlined />,
            onClick: () =>  onClose(tag)
        },
        {
            key: 'closeOthers',
            label: '关闭其他',
            icon: <CloseCircleOutlined />,
            onClick: () => onCloseOther(tag)
        },
        {
            key: 'closeAll',
            label: '关闭所有',
            icon: <CloseCircleOutlined />,
            onClick: () => onCloseAll()
        }
    ]

    return (
        <div className="tag-view-container">
            {tags?.map((tag: RouteItem) => (
                <Dropdown
                    key={tag.path}
                    menu={{ items: tag.path === '/' ? [] : getMenuItems(tag) }}
                    disabled={true}
                    trigger={['contextMenu']}
                >
                    <div
                        className={ tag.path === activeTag?.path ? 'tag-view_active' : 'tag-view' }
                         onClick={() => handleTagClick(tag)}
                    >
                        <span>{tag.meta?.title}</span>
                        {
                            (tag.path === activeTag?.path || tag.path === '/') ? <div className="tag-view-box"></div>
                                : <div
                                    className="tag-view-close"
                                    onClick={(e) => handleClose(tag, e)}
                                >
                                    <CloseOutlined />
                                </div>
                        }
                    </div>
                </Dropdown>
            ))}
        </div>
    )
}

export default TagView;
