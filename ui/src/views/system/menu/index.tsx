import React, {useEffect, useRef} from "react";
import { menuList } from '@/api/menu.ts'
const Menu:React.FC = () => {
    const isInitialized = useRef(false);

    const getMenuList = async () => {
        const response = await menuList({
            menuName: '',
            state: ''
        })

        console.log(response)
    }

    useEffect(() => {
        if (!isInitialized.current) {
            isInitialized.current = true;
            getMenuList()
        }

    }, []);
    return (
        <></>
    )
}
export default Menu