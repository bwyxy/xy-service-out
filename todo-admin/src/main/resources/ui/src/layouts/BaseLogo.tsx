import {FC} from "react";
import YdbLogo from "@/assets/image/ydb_logo.png"

const BaseLogo :FC = () => {
    return (
        <div className="base-logo">
            <img src={YdbLogo} alt="logo" />
            <div className="base-logo-title">
                <span>忆待办</span>
                <span>RECALL TO-DO</span>
            </div>
        </div>
    )
}

export default BaseLogo
