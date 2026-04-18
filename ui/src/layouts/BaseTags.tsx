import React, { useState, useEffect, useRef } from "react";
import { useLocation, useNavigate } from 'react-router-dom'
import {useMenuRouteStore} from "@/store/menuRouteStore.ts";
import {findPathDetail, RouteItem} from '@/utils/utils.ts'
import TagView from "@/layouts/TagView.tsx";

const BaseTags :React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const isFirstRender = useRef(true);
    const { menus } = useMenuRouteStore()

    const [tags, setTags] = useState(() :RouteItem[] => {
        return [{
            path: '/',
            name: 'Home',
            meta: {title: '仪表盘'},
            hidden: false,
            component: ''
        }]
    })
    const [activeTag, setActiveTag] = useState(tags[0]);

    const onClose = (tag :RouteItem) => {
        const newTags = tags.filter(t => t.path !== tag.path);
        setTags(newTags);

        if (activeTag.path === tag.path) {
            const lastTag = newTags[newTags.length - 1];
            if (lastTag) {
                setActiveTag(lastTag);
                navigate(lastTag.path);
            } else {
                navigate('/')
            }
        }
    }

    const onCloseOther = (tag:RouteItem) => {
        const newTags = tags.filter(t => t.path === tag.path || t.path === '/');
        console.log('关闭其他', newTags, tag);
        setTags(newTags);
        setActiveTag(tag)
        navigate(tag.path);
    }

    const onCloseAll = () => {
        const newTags = tags.filter(t => t.path === '/');
        setTags(newTags);
        navigate('/')
    }

    useEffect(() => {
        // 跳过第一次执行
        if (isFirstRender.current) {
            isFirstRender.current = false;
            return;
        }
        const currentPath = location.pathname;
        const existingTag = tags.find(tag => tag.path === currentPath);

        const menuInfo = findPathDetail(menus as RouteItem[], currentPath)
        console.log('1111',existingTag, tags)
        if (!existingTag && menuInfo) {
            menuInfo.menu.path = menuInfo.fullPath
            setTags(prevState => {
                return [...prevState, menuInfo.menu]
            })
            setActiveTag(menuInfo.menu)
        } else if (menuInfo) {
            setActiveTag(menuInfo.menu)
        }
    }, [location.pathname, menus, tags])

    return (
        <div className="base-tags">
            <TagView
                tags={tags}
                activeTag={activeTag}
                onClose={onClose}
                onCloseOther={onCloseOther}
                onCloseAll={onCloseAll}
            />
        </div>
    )
}

export default BaseTags;
